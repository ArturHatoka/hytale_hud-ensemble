package com.example.hudensemble.demo.huds;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

/**
 * Minimal HUD that only appends a UI layout under {@code Common/UI/Custom/Pages/}.
 */
public final class LayoutHud extends CustomUIHud {

    private final String pageFile;

    public LayoutHud(@Nonnull PlayerRef playerRef, @Nonnull String pageFile) {
        super(playerRef);
        this.pageFile = pageFile;
    }

    @Override
    protected void build(@Nonnull UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("Pages/" + pageFile);
    }
}
