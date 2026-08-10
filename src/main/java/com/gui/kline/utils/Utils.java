package com.gui.kline.utils;

import java.security.SecureRandom;

public class Utils {
    private static final char[] CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateId(String prefix, int length) {
        StringBuilder sb = new StringBuilder(prefix != null ? prefix : "");

        for (int i = 0; i < length; i++) {
            char c = CHAR_POOL[RANDOM.nextInt(CHAR_POOL.length)];
            sb.append(c);
        }

        return sb.toString();
    }
}
