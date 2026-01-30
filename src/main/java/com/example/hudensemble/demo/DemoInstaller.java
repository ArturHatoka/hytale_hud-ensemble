package com.example.hudensemble.demo;

import com.example.hudensemble.api.HudEnsembleClient;
import com.example.hudensemble.api.HudEnsembleService;
import com.example.hudensemble.demo.huds.LayoutHud;
import com.example.hudensemble.demo.ui.DemoOverlayPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Demo module for {@code /hudens demo}.
 */
public final class DemoInstaller {

    private DemoInstaller() {}

    private static final String UI_HELLO_BUTTON = "HudEns_DemoHelloButton.ui";
    private static final String UI_STATUS_CARD = "HudEns_DemoStatusCard.ui";
    private static final String UI_TIPS_CARD = "HudEns_DemoTipsCard.ui";
    private static final String UI_WATERMARK = "HudEns_DemoWatermark.ui";

    private static final String DEMO_NAMESPACE = "com.example:HudEnsemble:demo";

    public static void enable(
            @Nonnull HudEnsembleService service,
            @Nonnull Player player,
            @Nonnull PlayerRef playerRef,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store
    ) {
        HudEnsembleClient demo = service.createClient(DEMO_NAMESPACE);
        // Visual HUD layers.
        demo.setLayer(player, playerRef, DemoLayerIds.HELLO_BUTTON, new LayoutHud(playerRef, UI_HELLO_BUTTON));
        demo.setLayer(player, playerRef, DemoLayerIds.STATUS_CARD, new LayoutHud(playerRef, UI_STATUS_CARD));
        demo.setLayer(player, playerRef, DemoLayerIds.TIPS_CARD, new LayoutHud(playerRef, UI_TIPS_CARD));
        demo.setLayer(player, playerRef, DemoLayerIds.WATERMARK, new LayoutHud(playerRef, UI_WATERMARK));

        // Interactive overlay page (HUDs are not interactable).
        player.getPageManager().openCustomPage(ref, store, new DemoOverlayPage(playerRef));
    }

    public static void disable(@Nonnull HudEnsembleService service, @Nonnull Player player) {
        HudEnsembleClient demo = service.createClient(DEMO_NAMESPACE);
        demo.removeLayer(player, DemoLayerIds.HELLO_BUTTON);
        demo.removeLayer(player, DemoLayerIds.STATUS_CARD);
        demo.removeLayer(player, DemoLayerIds.TIPS_CARD);
        demo.removeLayer(player, DemoLayerIds.WATERMARK);

        // Close the demo overlay page if it's open.
        CustomUIPage page = player.getPageManager().getCustomPage();
        if (page instanceof DemoOverlayPage demoOverlay) {
            demoOverlay.requestClose();
        }
    }
}
