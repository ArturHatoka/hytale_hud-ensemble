package com.example.hudensemble.hudens;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reflection bridge to invoke {@code CustomUIHud#build(UICommandBuilder)}.
 *
 * <p>This is required because child HUDs may have protected visibility for {@code build}.
 * If the method cannot be resolved (API change), HudEnsemble will skip composing child HUDs.
 */
final class CustomHudBuildBridge {

    private static final Logger LOG = Logger.getLogger(CustomHudBuildBridge.class.getName());

    private static final Method BUILD_METHOD = resolveBuildMethod();

    private static Method resolveBuildMethod() {
        try {
            Method m = CustomUIHud.class.getDeclaredMethod("build", UICommandBuilder.class);
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException e) {
            LOG.log(Level.SEVERE, "Could not find method 'build(UICommandBuilder)' in CustomUIHud", e);
            return null;
        }
    }

    static boolean isAvailable() {
        return BUILD_METHOD != null;
    }

    static void invokeBuild(@Nonnull CustomUIHud hud, @Nonnull UICommandBuilder builder) {
        if (BUILD_METHOD == null) return;

        try {
            BUILD_METHOD.invoke(hud, builder);
        } catch (IllegalAccessException e) {
            LOG.log(Level.SEVERE, "Unable to invoke CustomUIHud#build via reflection", e);
        } catch (InvocationTargetException e) {
            // Child HUD threw an exception in build(). Log and ignore so we don't hard-crash.
            LOG.log(Level.SEVERE, "Child HUD threw inside build()", e.getTargetException());
        }
    }
}
