package com.example.hudensemble.internal;

import com.example.hudensemble.hudens.MultipleCustomUIHud;
import com.example.hudensemble.api.HudEnsembleClient;
import com.example.hudensemble.api.HudEnsembleService;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

/**
 * Default implementation of {@link HudEnsembleService}.
 */
public final class HudEnsembleServiceImpl implements HudEnsembleService {

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
            mchud.add("Unknown", currentCustomHud);
        }
    }

    @Override
    public void removeLayer(@Nonnull Player player, @Nonnull String layerId) {
        CustomUIHud currentCustomHud = player.getHudManager().getCustomHud();
        if (currentCustomHud instanceof MultipleCustomUIHud multipleCustomUIHud) {
            multipleCustomUIHud.remove(layerId);
        }
    }

    @Nonnull
    @Override
    public HudEnsembleClient createClient(@Nonnull String ownerNamespace) {
        return new NamespacedHudEnsembleClient(this, ownerNamespace);
    }

    @Override
    public boolean isCompositionSupported() {
        return MultipleCustomUIHud.isCompositionSupported();
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
            this.namespace = ownerNamespace;
        }

        @Override
        public void setLayer(
                @Nonnull Player player,
                @Nonnull PlayerRef playerRef,
                @Nonnull String layerId,
                @Nonnull CustomUIHud hud
        ) {
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
