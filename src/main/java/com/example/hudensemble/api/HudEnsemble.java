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
    public static final String PLUGIN_NAME = "HudEnsemble";

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

    /** Convenience: resolves the service and creates a namespaced client for the given plugin. */
    @Nonnull
    public static Optional<HudEnsembleClient> openClient(@Nonnull JavaPlugin owner) {
        return getService().map(s -> s.createClient(owner));
    }
}
