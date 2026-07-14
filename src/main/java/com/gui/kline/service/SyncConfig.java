package com.gui.kline.service;

public final class SyncConfig {
    private static final String DEFAULT_SYNC_URL = "http://localhost:8080/api/sync/batch";
    private static final String DEFAULT_API_KEY = "change-me";

    private SyncConfig() {
    }

    public static String getSyncUrl() {
        String env = System.getenv("KLINE_SYNC_URL");
        if (env != null && !env.isBlank()) {
            return env;
        }
        String prop = System.getProperty("kline.syncUrl");
        if (prop != null && !prop.isBlank()) {
            return prop;
        }
        return DEFAULT_SYNC_URL;
    }

    public static String getApiKey() {
        String env = System.getenv("KLINE_SYNC_API_KEY");
        if (env != null && !env.isBlank()) {
            return env;
        }
        String prop = System.getProperty("kline.syncApiKey");
        if (prop != null && !prop.isBlank()) {
            return prop;
        }
        return DEFAULT_API_KEY;
    }
}
