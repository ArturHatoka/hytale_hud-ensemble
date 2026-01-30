package com.example.hudensemble.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

/**
 * Main command collection for HudEnsemble.
 *
 * Usage:
 * - /hudens demo  - Show demo layered HUDs
 * - /hudens clean - Remove demo layered HUDs
 */
public class HudEnsembleCommand extends AbstractCommandCollection {

    public HudEnsembleCommand() {
        super("hudens", "HudEnsemble commands");

        this.addSubCommand(new DemoSubCommand());
        this.addSubCommand(new CleanSubCommand());
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }
}
