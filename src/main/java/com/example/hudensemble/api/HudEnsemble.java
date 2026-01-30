package com.example.hudensemble.api;

import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.PluginManager;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Stable entry point for third-party plugins.
 *
 * <p>Pattern follows the official plugin manager lookup style used by other public
 * plugins (e.g. fetching a plugin by {@link PluginIdentifier} and casting to an API interface).</p>
 */
public final class HudEnsemble {

    /** Matches {@code manifest.json -> Group}. */
    public static final String PLUGIN_GROUP = "com.example";
    /** Matches {@code manifest.json -> Name}. */
    public static final String PLUGIN_NAME = "HUD Ensemble";

    public static final PluginIdentifier IDENTIFIER = new PluginIdentifier(PLUGIN_GROUP, PLUGIN_NAME);

    private HudEnsemble() {
    }

    /**
     * Attempts to resolve the HudEnsemble service via Hytale's {@link PluginManager}.
     */
    @Nonnull
    public static Optional<HudEnsembleService> getService() {
        Object plugin = PluginManager.get().getPlugin(IDENTIFIER);
        if (plugin instanceof HudEnsembleApi api) {
            return Optional.of(api.getHudEnsembleService());
        }
        return Optional.empty();
    }

    /**
     * Resolves the HudEnsemble service or throws an {@link IllegalStateException} with a
     * clear diagnostic message.
     *
     * <p>Use this when your plugin <em>requires</em> HudEnsemble to function and you prefer
     * a fail-fast error over silent degradation.</p>
     */
    @Nonnull
    public static HudEnsembleService getServiceOrThrow() {
        Object plugin = PluginManager.get().getPlugin(IDENTIFIER);
        if (plugin == null) {
            throw new IllegalStateException(
                    "HudEnsemble plugin is not installed or not loaded. Expected PluginIdentifier: "
                            + PLUGIN_GROUP + ":" + PLUGIN_NAME
            );
        }
        if (!(plugin instanceof HudEnsembleApi api)) {
            throw new IllegalStateException(
                    "A plugin was found for PluginIdentifier " + PLUGIN_GROUP + ":" + PLUGIN_NAME
                            + " but it does not implement HudEnsembleApi."
                            + " This usually means you have an incompatible/old HudEnsemble build."
            );
        }
        HudEnsembleService service = api.getHudEnsembleService();
        if (service == null) {
            throw new IllegalStateException(
                    "HudEnsembleApi.getHudEnsembleService() returned null (unexpected)."
                            + " Please report this as a bug."
            );
        }
        return service;
    }

    /** Convenience: resolves the service and creates a namespaced client for the given plugin. */
    @Nonnull
    public static Optional<HudEnsembleClient> openClient(@Nonnull JavaPlugin owner) {
        return getService().map(s -> s.createClient(owner));
    }

    /** Convenience: like {@link #openClient(JavaPlugin)} but throws with diagnostics if unavailable. */
    @Nonnull
    public static HudEnsembleClient openClientOrThrow(@Nonnull JavaPlugin owner) {
        return getServiceOrThrow().createClient(owner);
    }
}
