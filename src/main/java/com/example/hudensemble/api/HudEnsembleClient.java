package com.example.hudensemble.api;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

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

    /**
     * Applies incremental UI commands to an existing layer owned by this client.
     *
     * <p>The provided {@code updater} receives a {@link UICommandBuilder} whose selectors are
     * automatically scoped to the target layer.</p>
     *
     * <p>If the layer doesn't exist, this is a no-op.</p>
     */
    void updateLayer(
            @Nonnull Player player,
            @Nonnull String layerId,
            @Nonnull Consumer<UICommandBuilder> updater
    );

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
