package com.example.hudensemble.commands;

import com.example.hudensemble.HudEnsemblePlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Small helpers for command implementations.
 */
final class CommandUtil {

    private CommandUtil() {}

    @Nullable
    static HudEnsemblePlugin getPluginOrReply(@Nonnull CommandContext context) {
        HudEnsemblePlugin plugin = HudEnsemblePlugin.getInstance();
        if (plugin == null) {
            context.sendMessage(Message.raw("Error: Plugin not loaded"));
            return null;
        }
        return plugin;
    }

    @Nullable
    static Player getPlayerOrReply(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref
    ) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            context.sendMessage(Message.raw("Error: Could not get Player component."));
            return null;
        }
        return player;
    }
}
