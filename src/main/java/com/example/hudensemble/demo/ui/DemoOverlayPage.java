package com.example.hudensemble.demo.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Small interactive overlay used by {@code /hudens demo}.
 */
public final class DemoOverlayPage extends InteractiveCustomUIPage<DemoOverlayPage.UIEventData> {

    /** Path relative to Common/UI/Custom/ */
    public static final String LAYOUT = "hudensemble/DemoOverlay.ui";

    public DemoOverlayPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, UIEventData.CODEC);
    }

    @Override
    public void build(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder evt,
            @Nonnull Store<EntityStore> store
    ) {
        cmd.append(LAYOUT);

        evt.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#DemoOverlayHelloButton",
                new EventData().append("Action", "hello"),
                false
        );

        evt.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#DemoOverlayCloseButton",
                new EventData().append("Action", "close"),
                false
        );
    }

    @Override
    public void handleDataEvent(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull UIEventData data
    ) {
        if (data.action == null) return;

        switch (data.action) {
            case "hello" -> {
                Player player = store.getComponent(ref, Player.getComponentType());
                if (player != null) {
                    player.sendMessage(Message.raw("Hello, hud ensemble!"));
                }

                UICommandBuilder cmd = new UICommandBuilder();
                cmd.set("#DemoOverlayStatusText.Text", "Sent to chat ✅");
                this.sendUpdate(cmd, false);
            }
            case "close" -> this.close();
        }
    }

    /** Allows external code (e.g. /hudens clean) to close this page safely. */
    public void requestClose() {
        this.close();
    }

    public static class UIEventData {
        public static final BuilderCodec<UIEventData> CODEC = BuilderCodec.builder(UIEventData.class, UIEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (e, v) -> e.action = v, e -> e.action)
                .add()
                .build();

        private String action;

        public UIEventData() {}
    }
}
