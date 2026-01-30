package com.example.hudensemble.api;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

/**
 * Per-consumer handle for interacting with {@link HudEnsembleService}.
 *
 * <p>Implementations are expected to:
 * <ul>
 *   <li>Namespace layer identifiers to avoid collisions between plugins.</li>
 *   <li>Track all created layers so they can be cleaned up reliably via {@link #close()}.</li>
 * </ul>
 */
public interface HudEnsembleClient extends AutoCloseable {

    void setLayer(
            @Nonnull Player player,
            @Nonnull PlayerRef playerRef,
            @Nonnull String layerId,
            @Nonnull CustomUIHud hud
    );

    void removeLayer(@Nonnull Player player, @Nonnull String layerId);

    /** Removes all layers created by this client for the given player. */
    void clear(@Nonnull Player player);

    /**
     * Removes all layers created by this client across all players it touched.
     *
     * <p>Call this in your plugin's {@code shutdown()}.</p>
     */
    @Override
    void close();
}
