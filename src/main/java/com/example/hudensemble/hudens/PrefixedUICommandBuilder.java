package com.example.hudensemble.hudens;

import com.hypixel.hytale.protocol.packets.interface_.CustomUICommand;
import com.hypixel.hytale.protocol.packets.interface_.CustomUICommandType;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * UICommandBuilder wrapper that prefixes selectors so a child HUD renders under a dedicated group.
 */
final class PrefixedUICommandBuilder extends UICommandBuilder {

    private final String prefixSelector;
    private final ArrayList<CustomUICommand> out = new ArrayList<>();
    private final List<CustomUICommand> internalCommands;

    PrefixedUICommandBuilder(@Nonnull String normalizedId) {
        this.prefixSelector = HudEnsembleUi.ROOT_SELECTOR + " #" + normalizedId;
        this.internalCommands = UiCommandListAccess.tryGet(this);
    }

    String getPrefixSelector() {
        return prefixSelector;
    }

    void addSynthetic(@Nonnull CustomUICommandType type, @Nonnull String selector, String document) {
        out.add(new CustomUICommand(type, selector, null, document));
    }

    /**
     * Called by CustomUIHud.update(...) internally. We must prefix before returning commands.
     */
    @Override
    @Nonnull
    public CustomUICommand[] getCommands() {
        collectAndPrefixIntoOut();
        CustomUICommand[] result = out.toArray(new CustomUICommand[0]);
        out.clear();
        return result;
    }

    void appendTo(@Nonnull UICommandBuilder target) {
        collectAndPrefixIntoOut();

        List<CustomUICommand> targetList = UiCommandListAccess.tryGet(target);
        if (targetList == null) return;

        targetList.addAll(out);
        out.clear();
    }

    private void collectAndPrefixIntoOut() {
        List<CustomUICommand> commands = this.internalCommands;
        if (commands == null || commands.isEmpty()) return;

        for (int i = 0, n = commands.size(); i < n; i++) {
            CustomUICommand cmd = commands.get(i);
            if (cmd.selector == null) {
                cmd.selector = prefixSelector;
            } else {
                cmd.selector = prefixSelector + ' ' + cmd.selector;
            }
            out.add(cmd);
        }

        commands.clear();
    }
}
