package com.example.hudensemble.api;

import javax.annotation.Nonnull;

/** Validation helpers for public API inputs. */
public final class HudEnsembleValidation {

    /** Upper bound to prevent accidentally unbounded identifiers. */
    public static final int MAX_LAYER_ID_LENGTH = 1024;

    /** Upper bound for owner namespaces used for namespacing layer ids. */
    public static final int MAX_OWNER_NAMESPACE_LENGTH = 256;

    private HudEnsembleValidation() {
    }

    /**
     * Ensures the supplied layer id is usable.
     *
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

    /**
     * Ensures the supplied owner namespace is usable.
     *
     * <p>The owner namespace is used to prefix your layer ids to avoid collisions with other plugins.
     * It may contain spaces and punctuation, but control characters are rejected.</p>
     */
    @Nonnull
    public static String requireValidOwnerNamespace(@Nonnull String ownerNamespace) {
        if (ownerNamespace == null) {
            throw new IllegalArgumentException("ownerNamespace must not be null");
        }

        String trimmed = ownerNamespace.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("ownerNamespace must not be blank");
        }

        int len = trimmed.length();
        if (len > MAX_OWNER_NAMESPACE_LENGTH) {
            throw new IllegalArgumentException(
                    "ownerNamespace length must be <= " + MAX_OWNER_NAMESPACE_LENGTH + " (was " + len + ")"
            );
        }

        requireNoControlChars(trimmed, "ownerNamespace");
        return trimmed;
    }

    private static void requireNoControlChars(@Nonnull String value, @Nonnull String paramName) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            // Reject C0 controls and DEL.
            if (c < 0x20 || c == 0x7F) {
                throw new IllegalArgumentException(paramName + " must not contain control characters");
            }
        }
    }
}
