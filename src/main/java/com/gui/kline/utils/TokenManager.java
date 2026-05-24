package com.gui.kline.utils;

public class TokenManager {
    private static String accessToken = null;

    public static synchronized void setAccessToken(String token) {
        accessToken = token;
    }

    public static synchronized String getAccessToken() {
        return accessToken;
    }

    public static synchronized void clearAccessToken() {
        accessToken = null;
    }
}