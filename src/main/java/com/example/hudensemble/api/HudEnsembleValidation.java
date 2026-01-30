package com.example.hudensemble.api;

import javax.annotation.Nonnull;

/**
 * Validation helpers for public API inputs.
 */
public final class HudEnsembleValidation {

    /**
     * Practical upper bound to prevent accidentally unbounded identifiers.
     *
     * <p>Note: the internal UI id is always normalized and hashed, so this limit exists mainly to
     * protect logs, maps, and user error cases.</p>
     */
    public static final int MAX_LAYER_ID_LENGTH = 1024;

    private HudEnsembleValidation() {}

    /**
     * Ensures the supplied layer id is usable.
     *
     * <p>Rules:
     * <ul>
     *   <li>Must not be {@code null}</li>
     *   <li>Must not be blank (after trimming)</li>
     *   <li>Length must be {@code <= MAX_LAYER_ID_LENGTH}</li>
     * </ul>
     */
    @Nonnull
    public static String requireValidLayerId(@Nonnull String layerId) {
        if (layerId == null) {
            // Even though callers annotate @Nonnull, enforce at runtime for safety.
            throw new IllegalArgumentException("layerId must not be null");
        }

        String trimmed = layerId.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("layerId must not be blank");
        }

        int len = layerId.length();
        if (len > MAX_LAYER_ID_LENGTH) {
            throw new IllegalArgumentException(
                    "layerId length must be <= " + MAX_LAYER_ID_LENGTH + " (was " + len + ")"
            );
        }

        return layerId;
    }
}
