package com.example.hudensemble.hudens;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.CRC32;

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

        // Always include a stable suffix derived from the full identifier.
        // This makes the mapping deterministic (independent of insertion order) and prevents collisions
        // when different identifiers normalize to the same base.
        String suffix = crc32Hex8(identifier);

        // Keep the final id strictly alphanumeric to be as compatible as possible with UI selector rules.
        String candidate = base + suffix;
        int i = 1;
        while (usedNormalizedIds.contains(candidate)) {
            candidate = base + suffix + i;
            i++;
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

    /**
     * Stable 8-hex-digit CRC32 suffix for deterministic ID allocation.
     */
    private static String crc32Hex8(@Nonnull String s) {
        CRC32 crc = new CRC32();
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        crc.update(bytes, 0, bytes.length);
        long value = crc.getValue();
        String hex = Long.toHexString(value);
        // left-pad to 8 chars
        if (hex.length() >= 8) {
            return hex.substring(hex.length() - 8);
        }
        StringBuilder sb = new StringBuilder(8);
        for (int i = hex.length(); i < 8; i++) {
            sb.append('0');
        }
        sb.append(hex);
        return sb.toString();
    }
}
