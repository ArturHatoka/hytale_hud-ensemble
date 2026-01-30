package com.example.hudensemble.internal;

import com.example.hudensemble.hudens.MultipleCustomUIHud;
import com.example.hudensemble.api.HudEnsembleClient;
import com.example.hudensemble.api.HudEnsembleService;
import com.example.hudensemble.api.HudEnsembleValidation;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

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
     */
    private static final class NamespacedHudEnsembleClient implements HudEnsembleClient {
        private final HudEnsembleService service;
        private final String namespace;

        /**
         * Weak keys avoid retaining disconnected players if a client is kept around longer than intended.
         */
        private final java.util.Map<Player, java.util.Set<String>> layersByPlayer = new java.util.WeakHashMap<>();

        private NamespacedHudEnsembleClient(@Nonnull HudEnsembleService service, @Nonnull String ownerNamespace) {
            this.service = service;
            this.namespace = HudEnsembleValidation.requireValidOwnerNamespace(ownerNamespace);
        }

        @Override
        public void setLayer(
                @Nonnull Player player,
                @Nonnull PlayerRef playerRef,
                @Nonnull String layerId,
                @Nonnull CustomUIHud hud
        ) {
            HudEnsembleValidation.requireValidLayerId(layerId);

            String key = namespaced(layerId);
            synchronized (layersByPlayer) {
                layersByPlayer
                        .computeIfAbsent(player, p -> new java.util.HashSet<>())
                        .add(key);
            }
            service.setLayer(player, playerRef, key, hud);
        }

        @Override
        public void removeLayer(@Nonnull Player player, @Nonnull String layerId) {
            HudEnsembleValidation.requireValidLayerId(layerId);

            String key = namespaced(layerId);
            synchronized (layersByPlayer) {
                var set = layersByPlayer.get(player);
                if (set != null) {
                    set.remove(key);
                    if (set.isEmpty()) {
                        layersByPlayer.remove(player);
                    }
                }
            }
            service.removeLayer(player, key);
        }

        @Override
        public void clear(@Nonnull Player player) {
            java.util.Set<String> toRemove;
            synchronized (layersByPlayer) {
                toRemove = layersByPlayer.remove(player);
            }
            if (toRemove == null || toRemove.isEmpty()) {
                return;
            }
            for (String id : toRemove) {
                service.removeLayer(player, id);
            }
        }

        @Override
        public void close() {
            java.util.Map<Player, java.util.Set<String>> snapshot;
            synchronized (layersByPlayer) {
                snapshot = new java.util.HashMap<>(layersByPlayer);
                layersByPlayer.clear();
            }

            for (var entry : snapshot.entrySet()) {
                Player player = entry.getKey();
                for (String id : entry.getValue()) {
                    service.removeLayer(player, id);
                }
            }
        }

        private String namespaced(@Nonnull String layerId) {
            return namespace + ":" + layerId;
        }
    }
}
