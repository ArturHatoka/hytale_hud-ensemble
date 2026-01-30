package com.example.hudensemble.hudens;

import com.hypixel.hytale.protocol.packets.interface_.CustomUICommand;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reflection bridge to access {@link UICommandBuilder}'s internal command list.
 *
 * <p>HudEnsemble needs this to prefix selectors for child HUDs. This is inherently
 * version-sensitive; if the upstream API changes, HudEnsemble degrades gracefully
 * by skipping multi-HUD composition.</p>
 */
final class UiCommandListAccess {

    private static final Logger LOG = Logger.getLogger(UiCommandListAccess.class.getName());

    private static final Field COMMANDS_FIELD = resolveCommandsField();

    private static Field resolveCommandsField() {
        try {
            Field f = UICommandBuilder.class.getDeclaredField("commands");
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            LOG.log(Level.SEVERE, "Could not find field 'commands' in UICommandBuilder", e);
            return null;
        }
    }

    static boolean isAvailable() {
        return COMMANDS_FIELD != null;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    static List<CustomUICommand> tryGet(@Nonnull UICommandBuilder builder) {
        if (COMMANDS_FIELD == null) return null;
        try {
            return (List<CustomUICommand>) COMMANDS_FIELD.get(builder);
        } catch (IllegalAccessException e) {
            LOG.log(Level.SEVERE, "Failed to access UICommandBuilder.commands", e);
            return null;
        }
    }
}
