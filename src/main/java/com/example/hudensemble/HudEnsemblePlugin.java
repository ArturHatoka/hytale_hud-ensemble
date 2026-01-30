package com.example.hudensemble;

import com.example.hudensemble.commands.HudEnsembleCommand;
import com.example.hudensemble.listeners.PlayerListener;
import com.example.hudensemble.api.HudEnsembleApi;
import com.example.hudensemble.api.HudEnsembleService;
import com.example.hudensemble.internal.HudEnsembleServiceImpl;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import java.util.logging.Level;

/**
 * HudEnsemble - a Hytale server mod that allows multiple CustomUIHud overlays to coexist.
 */
public class HudEnsemblePlugin extends JavaPlugin implements HudEnsembleApi {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static HudEnsemblePlugin instance;

    private final HudEnsembleServiceImpl service = new HudEnsembleServiceImpl();

    public static HudEnsemblePlugin getInstance() {
        return instance;
    }

    /**
     * Public API entry point.
     */
    @Nonnull
    @Override
    public HudEnsembleService getHudEnsembleService() {
        return service;
    }

    public HudEnsemblePlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        LOGGER.at(Level.INFO).log("[HudEnsemble] Setting up...");

        registerCommands();
        registerListeners();

        LOGGER.at(Level.INFO).log("[HudEnsemble] Setup complete!");
    }

    private void registerCommands() {
        try {
            getCommandRegistry().registerCommand(new HudEnsembleCommand());
            LOGGER.at(Level.INFO).log("[HudEnsemble] Registered /hudens command");
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log("[HudEnsemble] Failed to register commands");
        }
    }

    private void registerListeners() {
        EventRegistry eventBus = getEventRegistry();

        try {
            new PlayerListener().register(eventBus);
            LOGGER.at(Level.INFO).log("[HudEnsemble] Registered player event listeners");
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log("[HudEnsemble] Failed to register listeners");
        }
    }

    @Override
    protected void start() {
        LOGGER.at(Level.INFO).log("[HudEnsemble] Started!");
        LOGGER.at(Level.INFO).log("[HudEnsemble] Use /hudens demo (and /hudens clean) to test");
    }

    @Override
    protected void shutdown() {
        LOGGER.at(Level.INFO).log("[HudEnsemble] Shutting down...");

        try {
            service.cleanupOnPluginShutdown();
            LOGGER.at(Level.INFO).log("[HudEnsemble] Detached multiplexed HUDs from tracked players");
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log("[HudEnsemble] Shutdown cleanup failed");
        }

        instance = null;
    }

    /**
     * Adds (or replaces) a named HUD layer for the player.
     *
     * If the player currently has a non-multiplexed CustomUIHud set, it will be wrapped and preserved
     * under the identifier "Unknown".
     */
    public void setCustomHud(
            @Nonnull Player player,
            @Nonnull PlayerRef playerRef,
            @Nonnull String hudIdentifier,
            @Nonnull CustomUIHud customHud
    ) {
        // Backward-compatible wrapper around the public service API.
        service.setLayer(player, playerRef, hudIdentifier, customHud);
    }

    /** Removes a previously added HUD layer by its identifier. */
    public void hideCustomHud(@Nonnull Player player, @Nonnull String hudIdentifier) {
        // Backward-compatible wrapper around the public service API.
        service.removeLayer(player, hudIdentifier);
    }
}
