package com.finalproject.utils;

public final class ImageUrls {
    private ImageUrls() {}
    public static boolean valid(String value) {
        if (value == null || value.isBlank() || value.length() > 500 || value.contains(",") || value.contains("\n") || value.contains("\r")) return false;
        String url = value.trim();
        if (url.startsWith("/images/") || url.startsWith("/upload/")) return !url.contains("..") && !url.contains("\\");
        try {
            var uri = java.net.URI.create(url);
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null;
        } catch (IllegalArgumentException ex) { return false; }
    }
}
