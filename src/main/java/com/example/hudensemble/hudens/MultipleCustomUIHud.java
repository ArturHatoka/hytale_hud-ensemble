package com.example.hudensemble.hudens;

import com.hypixel.hytale.protocol.packets.interface_.CustomUICommandType;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A {@link CustomUIHud} wrapper that can host multiple HUDs at once.
 *
 * <p>Implementation notes:
 * <ul>
 *   <li>Uses reflection to call protected {@code CustomUIHud#build(UICommandBuilder)} on child HUDs.</li>
 *   <li>Prefixes selectors so each child HUD renders in an isolated subtree: {@code #HudEnsemble #<layerId> ...}</li>
 *   <li>If upstream API internals change, the wrapper degrades gracefully and simply won't compose children.</li>
 * </ul>
 */
public final class MultipleCustomUIHud extends CustomUIHud {

    private static final boolean CAN_COMPOSE_LAYERS =
            CustomHudBuildBridge.isAvailable() && UiCommandListAccess.isAvailable();

    /**
     * @return true if this build can compose multiple HUD layers (reflection bridge resolved).
     */
    public static boolean isCompositionSupported() {
        return CAN_COMPOSE_LAYERS;
    }

    private final NormalizedIdRegistry normalizedIds = new NormalizedIdRegistry();

    /** Maintains insertion order so demo layers are stable. */
    private final Map<String, CustomUIHud> layers = new LinkedHashMap<>();

    public MultipleCustomUIHud(@Nonnull PlayerRef playerRef) {
        super(playerRef);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder uiCommandBuilder) {
        // Root UI template contains: Group #HudEnsemble {}
        uiCommandBuilder.append("HUD/HudEnsemble.ui");
    }

    @Override
    public void show() {
        UICommandBuilder commandBuilder = new UICommandBuilder();
        this.build(commandBuilder);

        for (Map.Entry<String, CustomUIHud> entry : layers.entrySet()) {
            String identifier = entry.getKey();
            CustomUIHud hud = entry.getValue();
            String normalizedId = normalizedIds.getOrCreate(identifier);
            buildLayer(commandBuilder, normalizedId, hud, false);
        }

        this.update(true, commandBuilder);
    }

    /**
     * Adds or replaces a named HUD layer.
     */
    public void add(@Nonnull String identifier, @Nonnull CustomUIHud hud) {
        UICommandBuilder commandBuilder = new UICommandBuilder();

        String normalizedId = normalizedIds.getOrCreate(identifier);
        boolean existed = layers.put(identifier, hud) != null;

        buildLayer(commandBuilder, normalizedId, hud, existed);
        update(false, commandBuilder);
    }

    /**
     * Removes a previously added HUD layer by its identifier.
     */
    public void remove(@Nonnull String identifier) {
        String normalizedId = normalizedIds.getIfPresent(identifier);
        if (normalizedId == null) return;

        if (layers.remove(identifier) == null) return;
        normalizedIds.release(identifier);

        UICommandBuilder commandBuilder = new UICommandBuilder();
        commandBuilder.remove(HudEnsembleUi.ROOT_SELECTOR + " #" + normalizedId);
        update(false, commandBuilder);
    }

    private static void buildLayer(
            @Nonnull UICommandBuilder target,
            @Nonnull String normalizedId,
            @Nonnull CustomUIHud hud,
            boolean layerAlreadyExists
    ) {
        if (!CAN_COMPOSE_LAYERS) return;

        PrefixedUICommandBuilder layerBuilder = new PrefixedUICommandBuilder(normalizedId);

        if (layerAlreadyExists) {
            // Clear previous contents under this layer group.
            layerBuilder.addSynthetic(CustomUICommandType.Clear, layerBuilder.getPrefixSelector(), null);
        } else {
            // Create the group under the root container.
            layerBuilder.addSynthetic(
                    CustomUICommandType.AppendInline,
                    HudEnsembleUi.ROOT_SELECTOR,
                    "Group #" + normalizedId + " {}"
            );
        }

        CustomHudBuildBridge.invokeBuild(hud, layerBuilder);
        layerBuilder.appendTo(target);
    }
}
