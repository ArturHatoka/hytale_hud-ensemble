package com.example.hudensemble.commands;

import com.example.hudensemble.HudEnsemblePlugin;
import com.example.hudensemble.api.HudEnsembleService;
import com.example.hudensemble.demo.DemoInstaller;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * /hudens demo - Show several demo HUD layers simultaneously.
 */
public class DemoSubCommand extends AbstractPlayerCommand {

    public DemoSubCommand() {
        super("demo", "Show demo layered HUDs");
        this.setPermissionGroup(null);
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    ) {
        HudEnsemblePlugin plugin = CommandUtil.getPluginOrReply(context);
        if (plugin == null) return;

        Player player = CommandUtil.getPlayerOrReply(context, store, ref);
        if (player == null) return;

        HudEnsembleService service = plugin.getHudEnsembleService();
        DemoInstaller.enable(service, player, playerRef, ref, store);

        context.sendMessage(Message.raw("Demo HUD layers enabled. Use /hudens clean to remove."));
    }
}
