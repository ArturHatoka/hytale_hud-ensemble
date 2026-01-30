package com.example.hudensemble.internal;

import com.example.hudensemble.hudens.MultipleCustomUIHud;
import com.example.hudensemble.api.HudEnsembleClient;
import com.example.hudensemble.api.HudEnsembleService;
import com.example.hudensemble.api.HudEnsembleValidation;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

import javax.annotation.Nonnull;
import java.lang.ref.Cleaner;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default implementation of {@link HudEnsembleService}.
 */
public final class HudEnsembleServiceImpl implements HudEnsembleService {

    /**
     * Tracks players that have had their CustomUIHud wrapped in {@link MultipleCustomUIHud}.
     *
     * <p>This is used only for HudEnsemble plugin shutdown cleanup, to ensure we detach the
     * wrapper (which lives in this plugin's classloader) from any still-connected players.
     */
    private final java.util.Map<Player, PlayerRef> touchedPlayers = new java.util.WeakHashMap<>();

    @Override
    public int getApiVersion() {
        return com.example.hudensemble.api.HudEnsembleVersion.API_VERSION;
    }

    @Override
    public void setLayer(
            @Nonnull Player player,
            @Nonnull PlayerRef playerRef,
            @Nonnull String layerId,
            @Nonnull CustomUIHud hud
    ) {
        HudEnsembleValidation.requireValidLayerId(layerId);

        // Remember that this player was touched by HudEnsemble so we can detach the wrapper on shutdown.
        synchronized (touchedPlayers) {
            touchedPlayers.put(player, playerRef);
        }

        // If the reflection bridge is unavailable, we can't stack HUDs safely.
        if (!MultipleCustomUIHud.isCompositionSupported()) {
            player.getHudManager().setCustomHud(playerRef, hud);
            return;
        }

        CustomUIHud currentCustomHud = player.getHudManager().getCustomHud();
        if (currentCustomHud instanceof MultipleCustomUIHud multipleCustomUIHud) {
            multipleCustomUIHud.add(layerId, hud);
            return;
        }

        // Wrap the existing HUD (if any) and replace it with a multiplexed HUD.
        MultipleCustomUIHud mchud = new MultipleCustomUIHud(playerRef);
        player.getHudManager().setCustomHud(playerRef, mchud);
        mchud.add(layerId, hud);

        if (currentCustomHud != null) {
            mchud.add(MultipleCustomUIHud.PRESERVED_BASE_HUD_LAYER_ID, currentCustomHud);
        }
    }

    @Override
    public void removeLayer(@Nonnull Player player, @Nonnull String layerId) {
        HudEnsembleValidation.requireValidLayerId(layerId);

        CustomUIHud currentCustomHud = player.getHudManager().getCustomHud();
        if (currentCustomHud instanceof MultipleCustomUIHud multipleCustomUIHud) {
            multipleCustomUIHud.remove(layerId);
        }
    }

    @Nonnull
    @Override
    public HudEnsembleClient createClient(@Nonnull String ownerNamespace) {
        String ns = HudEnsembleValidation.requireValidOwnerNamespace(ownerNamespace);
        return new NamespacedHudEnsembleClient(this, ns);
    }

    @Override
    public boolean isCompositionSupported() {
        return MultipleCustomUIHud.isCompositionSupported();
    }

    /**
     * Detaches {@link MultipleCustomUIHud} from any tracked players.
     *
     * <p>Important for hot-reloading HudEnsemble: leaving a {@link MultipleCustomUIHud} instance
     * attached would keep this plugin's classes reachable from the server.
     */
    public void cleanupOnPluginShutdown() {
        java.util.Map<Player, PlayerRef> snapshot;
        synchronized (touchedPlayers) {
            snapshot = new java.util.HashMap<>(touchedPlayers);
            touchedPlayers.clear();
        }

        for (var entry : snapshot.entrySet()) {
            Player player = entry.getKey();
            PlayerRef playerRef = entry.getValue();
            if (player == null || playerRef == null) continue;

            try {
                CustomUIHud current = player.getHudManager().getCustomHud();
                if (!(current instanceof MultipleCustomUIHud multiple)) {
                    continue;
                }

                CustomUIHud preserved = multiple.getLayerOrNull(MultipleCustomUIHud.PRESERVED_BASE_HUD_LAYER_ID);
                // The HUD system supports clearing custom HUDs by passing null.
                player.getHudManager().setCustomHud(playerRef, preserved);
            } catch (Throwable ignored) {
                // Best-effort cleanup only.
            }
        }
    }

    /**
     * Default client implementation namespaces layer ids and tracks created layers for cleanup.
     *
     * <p>Clients are expected to be closed explicitly. However, to mitigate leaks when a plugin
     * forgets to close a client instance that becomes unreachable, this implementation registers
     * a {@link Cleaner} action that schedules removal of the client's layers on the owning
     * player's world thread.
     */
    private static final class NamespacedHudEnsembleClient implements HudEnsembleClient {
        private static final Cleaner CLEANER = Cleaner.create();

        private final HudEnsembleService service;
        private final String namespace;

        private final CleanupState cleanupState;
        private final Cleaner.Cleanable cleanable;

        private NamespacedHudEnsembleClient(@Nonnull HudEnsembleService service, @Nonnull String ownerNamespace) {
            this.service = service;
            this.namespace = HudEnsembleValidation.requireValidOwnerNamespace(ownerNamespace);
            this.cleanupState = new CleanupState(service);
            this.cleanable = CLEANER.register(this, cleanupState);
        }

        @Override
        public void setLayer(
                @Nonnull Player player,
                @Nonnull PlayerRef playerRef,
                @Nonnull String layerId,
                @Nonnull CustomUIHud hud
        ) {
            ensureOpen();
            HudEnsembleValidation.requireValidLayerId(layerId);

            String key = namespaced(layerId);
            cleanupState.recordLayer(player, key);
            service.setLayer(player, playerRef, key, hud);
        }

        @Override
        public void removeLayer(@Nonnull Player player, @Nonnull String layerId) {
            ensureOpen();
            HudEnsembleValidation.requireValidLayerId(layerId);

            String key = namespaced(layerId);
            cleanupState.unrecordLayer(player, key);
            service.removeLayer(player, key);
        }

        @Override
        public void clear(@Nonnull Player player) {
            ensureOpen();
            java.util.Set<String> toRemove = cleanupState.removeAllForPlayer(player);
            cleanupState.scheduleRemoval(player, toRemove);
        }

        @Override
        public void close() {
            // Idempotent; schedules cleanup on the appropriate world threads.
            cleanable.clean();
        }

        private String namespaced(@Nonnull String layerId) {
            return namespace + ":" + layerId;
        }

        private void ensureOpen() {
            if (cleanupState.isCleaned()) {
                throw new IllegalStateException("HudEnsembleClient is closed");
            }
        }

        /**
         * Captures tracked layers and schedules cleanup on world threads.
         *
         * <p>This state object must not reference the owning {@link NamespacedHudEnsembleClient}
         * instance, otherwise the {@link Cleaner} action would prevent garbage collection.
         */
        private static final class CleanupState implements Runnable {

            private final HudEnsembleService service;
            private final AtomicBoolean cleaned = new AtomicBoolean(false);

            /**
             * Weak keys avoid retaining disconnected players if a client is kept around longer than intended.
             */
            private final java.util.Map<Player, java.util.Set<String>> layersByPlayer = new java.util.WeakHashMap<>();

            private CleanupState(@Nonnull HudEnsembleService service) {
                this.service = service;
            }

            private boolean isCleaned() {
                return cleaned.get();
            }

            private void recordLayer(@Nonnull Player player, @Nonnull String namespacedLayerId) {
                synchronized (layersByPlayer) {
                    layersByPlayer
                            .computeIfAbsent(player, p -> new java.util.HashSet<>())
                            .add(namespacedLayerId);
                }
            }

            private void unrecordLayer(@Nonnull Player player, @Nonnull String namespacedLayerId) {
                synchronized (layersByPlayer) {
                    var set = layersByPlayer.get(player);
                    if (set != null) {
                        set.remove(namespacedLayerId);
                        if (set.isEmpty()) {
                            layersByPlayer.remove(player);
                        }
                    }
                }
            }

            private java.util.Set<String> removeAllForPlayer(@Nonnull Player player) {
                synchronized (layersByPlayer) {
                    return layersByPlayer.remove(player);
                }
            }

            @Override
            public void run() {
                if (!cleaned.compareAndSet(false, true)) {
                    return;
                }

                java.util.Map<Player, java.util.Set<String>> snapshot;
                synchronized (layersByPlayer) {
                    snapshot = new java.util.HashMap<>(layersByPlayer);
                    layersByPlayer.clear();
                }

                for (var entry : snapshot.entrySet()) {
                    scheduleRemoval(entry.getKey(), entry.getValue());
                }
            }

            private void scheduleRemoval(Player player, java.util.Set<String> layerIds) {
                if (player == null || layerIds == null || layerIds.isEmpty()) {
                    return;
                }

                final World world;
                try {
                    world = player.getWorld();
                } catch (Throwable ignored) {
                    return;
                }

                if (world == null || !world.isAlive()) {
                    return;
                }

                Runnable work = () -> {
                    for (String id : layerIds) {
                        try {
                            service.removeLayer(player, id);
                        } catch (Throwable ignored) {
                            // Best-effort only.
                        }
                    }
                };

                try {
                    if (world.isInThread()) {
                        work.run();
                    } else {
                        world.execute(work);
                    }
                } catch (Throwable ignored) {
                    // Best-effort only.
                }
            }
        }
    }
}
