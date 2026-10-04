package com.finalproject.utils;

import java.net.URI;

public final class SafeRedirect {
    private SafeRedirect() {}
    public static boolean isWithin(String value, String... allowedPaths) {
        if (value == null || !value.startsWith("/") || value.contains("\\") || value.chars().anyMatch(Character::isISOControl)) return false;
        try {
            URI uri = URI.create(value);
            if (uri.getScheme() != null || uri.getRawAuthority() != null || uri.getFragment() != null || !uri.normalize().equals(uri)) return false;
            for (String prefix : allowedPaths) if (uri.getPath().equals(prefix) || uri.getPath().startsWith(prefix + "/")) return true;
        } catch (IllegalArgumentException ignored) {}
        return false;
    }
}
