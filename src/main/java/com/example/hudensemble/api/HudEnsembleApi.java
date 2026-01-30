package com.example.hudensemble.api;

import javax.annotation.Nonnull;

/**
 * Marker interface implemented by the provider plugin.
 *
 * <p>Consumers should cast the plugin instance to this interface (instead of
 * {@code HudEnsemblePlugin}) to avoid depending on implementation details.</p>
 */
public interface HudEnsembleApi {

    @Nonnull
    HudEnsembleService getHudEnsembleService();
}
