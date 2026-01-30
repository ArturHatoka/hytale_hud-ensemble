package com.example.hudensemble.api;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

/**
 * Public API for managing multiple {@link CustomUIHud} layers per player.
 *
 * <p>This abstraction intentionally hides implementation details (reflection bridge,
 * UI selector prefixing, root group layout) so that future versions can evolve
 * without forcing API consumers to rewrite their code.</p>
 */
public interface HudEnsembleService {

    /**
     * API version of this service. Consumers can use this for compatibility checks.
     */
    default int getApiVersion() {
        return HudEnsembleVersion.API_VERSION;
    }

    /**
     * Adds (or replaces) a named HUD layer for the player.
     *
     * @param player the target player
     * @param playerRef the player's ref (required by HudManager/CustomUIHud)
     * @param layerId stable identifier for this layer within a single player
     * @param hud the HUD to render inside this layer
     */
    void setLayer(
            @Nonnull Player player,
            @Nonnull PlayerRef playerRef,
            @Nonnull String layerId,
            @Nonnull CustomUIHud hud
    );

    /**
     * Removes a previously added HUD layer by its identifier.
     */
    void removeLayer(@Nonnull Player player, @Nonnull String layerId);

    /**
     * Creates a client handle for a specific consumer plugin (or subsystem).
     *
     * <p>The client:
     * <ul>
     *   <li>Automatically namespaces {@code layerId} values to avoid collisions with other plugins.</li>
     *   <li>Tracks all layers you created so you can reliably clean them up via {@link HudEnsembleClient#close()}.</li>
     * </ul>
     *
     * <p><b>Lifecycle contract:</b> store the returned client and call {@link HudEnsembleClient#close()} in your
     * plugin's {@code shutdown()} to remove your HUD layers when your plugin unloads.</p>
     *
     * @param ownerNamespace stable namespace for your plugin, e.g. {@code "MyGroup:MyPlugin"}
     */
    @Nonnull
    HudEnsembleClient createClient(@Nonnull String ownerNamespace);

    /**
     * Convenience overload that uses the owner's class name as a stable namespace.
     */
    @Nonnull
    default HudEnsembleClient createClient(@Nonnull JavaPlugin owner) {
        return createClient(owner.getClass().getName());
    }

    /**
     * @return {@code true} if the runtime supports composing multiple CustomUIHuds.
     */
    boolean isCompositionSupported();
}
