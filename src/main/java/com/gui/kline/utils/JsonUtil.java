package com.gui.kline.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.StringJoiner;

public final class JsonUtil {
    private JsonUtil() {
    }

    public static String quote(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + escape(value) + "\"";
    }

    public static String field(String key, String value) {
        return quote(key) + ":" + quote(value);
    }

    public static String field(String key, int value) {
        return quote(key) + ":" + value;
    }

    public static String field(String key, double value) {
        return quote(key) + ":" + value;
    }

    public static String field(String key, boolean value) {
        return quote(key) + ":" + value;
    }

    public static String fieldRaw(String key, String rawJson) {
        return quote(key) + ":" + (rawJson == null ? "null" : rawJson);
    }

    public static String obj(String... fields) {
        StringJoiner joiner = new StringJoiner(",");
        for (String f : fields) {
            if (f != null && !f.isBlank()) {
                joiner.add(f);
            }
        }
        return "{" + joiner + "}";
    }

    public static String array(String... items) {
        StringJoiner joiner = new StringJoiner(",");
        for (String item : items) {
            if (item != null && !item.isBlank()) {
                joiner.add(item);
            }
        }
        return "[" + joiner + "]";
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
    
    /**
     * Creates and configures an ObjectMapper for JSON serialization/deserialization
     */
    public static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}

