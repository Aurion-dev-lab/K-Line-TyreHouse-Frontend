package com.gui.kline.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JsonDataParser {

    /**
     * Safely converts raw JSON text into a JsonObject without reflection.
     */
    public static JsonObject parse(String rawJson) {
        try {
            if (rawJson != null && !rawJson.trim().isEmpty()) {
                JsonElement element = JsonParser.parseString(rawJson);
                if (element != null && element.isJsonObject()) {
                    return element.getAsJsonObject();
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to parse raw JSON tree: " + e.getMessage());
        }
        return new JsonObject();
    }

    /**
     * Safely pulls a String value by its property key name with a fallback default.
     */
    public static String getString(JsonObject json, String key, String defaultValue) {
        if (json != null && json.has(key) && !json.get(key).isJsonNull()) {
            return json.get(key).getAsString();
        }
        return defaultValue;
    }

    public static double getDouble(JsonObject json, String key, double defaultValue) {
        if (json != null && json.has(key) && !json.get(key).isJsonNull()) {
            return json.get(key).getAsDouble();
        }
        return defaultValue;
    }

    public static int getInt(JsonObject json, String key, int defaultValue) {
        if (json != null && json.has(key) && !json.get(key).isJsonNull()) {
            return json.get(key).getAsInt();
        }
        return defaultValue;
    }
}