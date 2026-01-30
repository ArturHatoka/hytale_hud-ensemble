package com.example.hudensemble.api;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Per-consumer handle for interacting with {@link HudEnsembleService}.
 *
 * <p>Implementations are expected to:
 * <ul>
 *   <li>Namespace layer identifiers to avoid collisions between plugins.</li>
 *   <li>Track all created layers so they can be cleaned up reliably via {@link #close()}.</li>
 * </ul>
 */
public interface HudEnsembleClient extends AutoCloseable {

    void setLayer(
            @Nonnull Player player,
            @Nonnull PlayerRef playerRef,
            @Nonnull String layerId,
            @Nonnull CustomUIHud hud
    );

    /**
     * Convenience helper: applies {@link #setLayer(Player, PlayerRef, String, CustomUIHud)} to all
     * connected players that are currently spawned in a world.
     *
     * <p>Because most HUD implementations need a per-player {@link PlayerRef}, this method uses a
     * factory to create a fresh {@link CustomUIHud} instance for each target player.
     *
     * <p>The HUD installation is scheduled on each player's world thread.
     *
     * @return number of players for which the operation was scheduled (best-effort).
     */
    default int setLayerForAllOnline(
            @Nonnull String layerId,
            @Nonnull Function<PlayerRef, CustomUIHud> hudFactory
    ) {
        HudEnsembleValidation.requireValidLayerId(layerId);
        if (hudFactory == null) {
            throw new IllegalArgumentException("hudFactory must not be null");
        }

        Universe universe = Universe.get();
        List<PlayerRef> players = universe.getPlayers();
        int scheduled = 0;

        for (PlayerRef playerRef : players) {
            if (playerRef == null) continue;

            Ref<EntityStore> ref;
            try {
                ref = playerRef.getReference();
            } catch (Throwable ignored) {
                continue;
            }

            if (ref == null || !ref.isValid()) {
                // The player is not currently spawned in a world.
                continue;
            }

            Store<EntityStore> store;
            try {
                store = ref.getStore();
            } catch (Throwable ignored) {
                continue;
            }

            World world;
            try {
                world = store.getExternalData().getWorld();
            } catch (Throwable ignored) {
                continue;
            }

            if (world == null || !world.isAlive()) {
                continue;
            }

            Runnable work = () -> {
                try {
                    Player player = store.getComponent(ref, Player.getComponentType());
                    if (player == null) return;

                    CustomUIHud hud = hudFactory.apply(playerRef);
                    if (hud == null) return;

                    setLayer(player, playerRef, layerId, hud);
                } catch (Throwable ignored) {
                    // Best-effort only.
                }
            };

            try {
                if (world.isInThread()) {
                    work.run();
                } else {
                    world.execute(work);
                }
                scheduled++;
            } catch (Throwable ignored) {
                // Best-effort only.
            }
        }

        return scheduled;
    }

    /**
     * Convenience helper: removes {@link #removeLayer(Player, String)} for all
     * connected players that are currently spawned in a world.
     *
     * <p>The removal is scheduled on each player's world thread.
     *
     * @return number of players for which the operation was scheduled (best-effort).
     */
    default int removeLayerForAllOnline(@Nonnull String layerId) {
        HudEnsembleValidation.requireValidLayerId(layerId);

        Universe universe = Universe.get();
        List<PlayerRef> players = universe.getPlayers();
        int scheduled = 0;

        for (PlayerRef playerRef : players) {
            if (playerRef == null) continue;

            Ref<EntityStore> ref;
            try {
                ref = playerRef.getReference();
            } catch (Throwable ignored) {
                continue;
            }

            if (ref == null || !ref.isValid()) {
                continue;
            }

            Store<EntityStore> store;
            try {
                store = ref.getStore();
            } catch (Throwable ignored) {
                continue;
            }

            World world;
            try {
                world = store.getExternalData().getWorld();
            } catch (Throwable ignored) {
                continue;
            }

            if (world == null || !world.isAlive()) {
                continue;
            }

            Runnable work = () -> {
                try {
                    Player player = store.getComponent(ref, Player.getComponentType());
                    if (player == null) return;
                    removeLayer(player, layerId);
                } catch (Throwable ignored) {
                    // Best-effort only.
                }
            };

            try {
                if (world.isInThread()) {
                    work.run();
                } else {
                    world.execute(work);
                }
                scheduled++;
            } catch (Throwable ignored) {
                // Best-effort only.
            }
        }

        return scheduled;
    }

    void removeLayer(@Nonnull Player player, @Nonnull String layerId);

    /**
     * Applies incremental UI commands to an existing layer owned by this client.
     *
     * <p>The provided {@code updater} receives a {@link UICommandBuilder} whose selectors are
     * automatically scoped to the target layer.</p>
     *
     * <p>If the layer doesn't exist, this is a no-op.</p>
     */
    void updateLayer(
            @Nonnull Player player,
            @Nonnull String layerId,
            @Nonnull Consumer<UICommandBuilder> updater
    );

    /**
     * Convenience helper: removes all layers created by this client for all
     * connected players that are currently spawned in a world.
     *
     * <p>The cleanup is scheduled on each player's world thread.
     *
     * @return number of players for which the operation was scheduled (best-effort).
     */
    default int clearForAllOnline() {
        Universe universe = Universe.get();
        List<PlayerRef> players = universe.getPlayers();
        int scheduled = 0;

        for (PlayerRef playerRef : players) {
            if (playerRef == null) continue;

            Ref<EntityStore> ref;
            try {
                ref = playerRef.getReference();
            } catch (Throwable ignored) {
                continue;
            }

            if (ref == null || !ref.isValid()) {
                continue;
            }

            Store<EntityStore> store;
            try {
                store = ref.getStore();
            } catch (Throwable ignored) {
                continue;
            }

            World world;
            try {
                world = store.getExternalData().getWorld();
            } catch (Throwable ignored) {
                continue;
            }

            if (world == null || !world.isAlive()) {
                continue;
            }

            Runnable work = () -> {
                try {
                    Player player = store.getComponent(ref, Player.getComponentType());
                    if (player == null) return;
                    clear(player);
                } catch (Throwable ignored) {
                    // Best-effort only.
                }
            };

            try {
                if (world.isInThread()) {
                    work.run();
                } else {
                    world.execute(work);
                }
                scheduled++;
            } catch (Throwable ignored) {
                // Best-effort only.
            }
        }

        return scheduled;
    }

    /** Removes all layers created by this client for the given player. */
    void clear(@Nonnull Player player);

    /**
     * Removes all layers created by this client across all players it touched.
     *
     * <p>Call this in your plugin's {@code shutdown()}.</p>
     */
    @Override
    void close();
}
