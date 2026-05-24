package com.gui.kline.utils;

import java.io.InputStream;
import java.util.Properties;

public final class ApiConfig {
    private static final String BASE_URL;

    static {
        String url = "http://localhost:8080/api/v1"; // Default fallback
        try (InputStream input = ApiConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                Properties prop = new Properties();
                prop.load(input);
                String val = prop.getProperty("kline.api.base-url");
                if (val != null && !val.isBlank()) url = val.trim();
            }
        } catch (Exception ignored) {
            // Fallback used seamlessly
        }
        BASE_URL = url;
    }

    public static String getBaseUrl() {
        return BASE_URL;
    }
}