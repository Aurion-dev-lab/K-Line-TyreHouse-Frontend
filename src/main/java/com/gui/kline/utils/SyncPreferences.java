package com.gui.kline.utils;

import java.util.prefs.Preferences;

public class SyncPreferences {

    private static final Preferences prefs = Preferences.userNodeForPackage(SyncPreferences.class);

    private static final String KEY_SYNC_API_URL = "SYNC_API_URL";
    private static final String KEY_SYNC_API_KEY = "SYNC_API_KEY";

    private static final String DEFAULT_URL = System.getenv().getOrDefault(
            "SYNC_API_URL", "http://localhost:8080"
    );
    private static final String DEFAULT_KEY = System.getenv().getOrDefault(
            "SYNC_API_KEY", "sync_qPoGFyE00i74X__qkFuCyKEuLYZNwq0ShyXdNe5t4og60LkWZSDTgeedtrihyTFwQm1Gt7VyeXkmQ2AQ3fSlnQ"
    );

    public static String getBaseUrl() {
        String url = prefs.get(KEY_SYNC_API_URL, DEFAULT_URL).trim();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (url.endsWith("/api/sync")) {
            url = url.substring(0, url.length() - "/api/sync".length());
        }
        return url;
    }

    public static String getSyncApiUrl() {
        return getBaseUrl() + "/api/sync";
    }

    public static String getGenerateKeyUrl() {
        return getBaseUrl() + "/api/sync/keys";
    }

    public static void setSyncApiUrl(String url) {
        if (url != null && !url.trim().isEmpty()) {
            prefs.put(KEY_SYNC_API_URL, url.trim());
        }
    }

    public static String getSyncApiKey() {
        return prefs.get(KEY_SYNC_API_KEY, DEFAULT_KEY);
    }

    public static void setSyncApiKey(String key) {
        if (key != null && !key.trim().isEmpty()) {
            prefs.put(KEY_SYNC_API_KEY, key.trim());
        }
    }

    public static void saveSyncSettings(String url, String key) {
        setSyncApiUrl(url);
        setSyncApiKey(key);
    }
}
