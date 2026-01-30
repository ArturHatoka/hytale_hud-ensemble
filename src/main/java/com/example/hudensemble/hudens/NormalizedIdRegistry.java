package com.example.hudensemble.hudens;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Allocates stable UI-safe ids for HUD layer identifiers.
 */
final class NormalizedIdRegistry {

    private final Map<String, String> normalizedIds = new HashMap<>();
    private final Set<String> usedNormalizedIds = new HashSet<>();

    @Nonnull
    String getOrCreate(@Nonnull String identifier) {
        String existing = normalizedIds.get(identifier);
        if (existing != null) return existing;

        String base = normalizeToAlphaNum(identifier);
        if (base.isEmpty()) base = "hud";

        String candidate = base;
        if (usedNormalizedIds.contains(candidate)) {
            String suffix = Integer.toHexString(identifier.hashCode());
            candidate = base + "_" + suffix;
            int i = 1;
            while (usedNormalizedIds.contains(candidate)) {
                candidate = base + "_" + suffix + "_" + i;
                i++;
            }
        }

        normalizedIds.put(identifier, candidate);
        usedNormalizedIds.add(candidate);
        return candidate;
    }

    @Nullable
    String getIfPresent(@Nonnull String identifier) {
        return normalizedIds.get(identifier);
    }

    /**
     * Releases a normalized id when the layer is removed.
     */
    void release(@Nonnull String identifier) {
        String normalized = normalizedIds.remove(identifier);
        if (normalized != null) usedNormalizedIds.remove(normalized);
    }

    private static String normalizeToAlphaNum(@Nonnull String s) {
        int n = s.length();
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
