package com.mycompany.myapp.security;

/**
 * Screen permission level, mirroring FE rbac.ts: Y = read+write, R = read-only, N = no access.
 */
public enum ScreenPerm {
    N,
    R,
    Y;

    public boolean canRead() {
        return this != N;
    }

    public boolean canWrite() {
        return this == Y;
    }

    public static ScreenPerm fromCode(String code) {
        if (code == null || code.isBlank()) {
            return N;
        }
        return switch (code.trim().toUpperCase()) {
            case "Y" -> Y;
            case "R" -> R;
            default -> N;
        };
    }
}
