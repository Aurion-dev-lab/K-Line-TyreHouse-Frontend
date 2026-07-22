package com.gui.kline.utils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utility class for safely reading date/time values from SQLite ResultSets.
 *
 * Handles ISO date strings ("yyyy-MM-dd"), ISO timestamps ("yyyy-MM-dd HH:mm:ss"),
 * ISO 8601 ("yyyy-MM-dd'T'HH:mm:ss"), and Unix epoch timestamps (seconds or millis).
 */
public final class SqliteUtil {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATETIME_MILLIS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private SqliteUtil() {}

    /**
     * Safely reads a date column from a SQLite ResultSet by column name.
     */
    public static LocalDate getLocalDate(ResultSet rs, String column) throws SQLException {
        String raw = rs.getString(column);
        return parseLocalDate(raw);
    }

    /**
     * Safely reads a date column by column index (1-based).
     */
    public static LocalDate getLocalDate(ResultSet rs, int columnIndex) throws SQLException {
        String raw = rs.getString(columnIndex);
        return parseLocalDate(raw);
    }

    /**
     * Parses a string representation into a LocalDate.
     */
    public static LocalDate parseLocalDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        raw = raw.trim();
        
        // Handle numeric epoch timestamps (seconds or milliseconds)
        if (raw.matches("^\\d+$")) {
            try {
                long val = Long.parseLong(raw);
                if (raw.length() <= 10) {
                    return Instant.ofEpochSecond(val).atZone(ZoneId.systemDefault()).toLocalDate();
                } else {
                    return Instant.ofEpochMilli(val).atZone(ZoneId.systemDefault()).toLocalDate();
                }
            } catch (Exception e) {
                // Fallback to text parsing
            }
        }

        try {
            if (raw.contains("T")) {
                return LocalDateTime.parse(raw).toLocalDate();
            }
            if (raw.length() > 10) {
                raw = raw.substring(0, 10);
            }
            return LocalDate.parse(raw, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            System.err.println("SqliteUtil: Could not parse date: " + raw);
            return null;
        }
    }

    /**
     * Safely reads a datetime/timestamp column from a SQLite ResultSet by column name.
     */
    public static LocalDateTime getLocalDateTime(ResultSet rs, String column) throws SQLException {
        String raw = rs.getString(column);
        return parseLocalDateTime(raw);
    }

    /**
     * Parses a string representation into a LocalDateTime.
     */
    public static LocalDateTime parseLocalDateTime(String raw) {
        if (raw == null || raw.isBlank()) return null;
        raw = raw.trim();

        // Handle numeric epoch timestamps
        if (raw.matches("^\\d+$")) {
            try {
                long val = Long.parseLong(raw);
                if (raw.length() <= 10) {
                    return Instant.ofEpochSecond(val).atZone(ZoneId.systemDefault()).toLocalDateTime();
                } else {
                    return Instant.ofEpochMilli(val).atZone(ZoneId.systemDefault()).toLocalDateTime();
                }
            } catch (Exception e) {
                // Fallback
            }
        }

        try {
            if (raw.contains("T")) {
                return LocalDateTime.parse(raw);
            }
            if (raw.length() > 19) {
                raw = raw.substring(0, 23);
                return LocalDateTime.parse(raw, DATETIME_MILLIS_FORMATTER);
            }
            if (raw.length() > 10) {
                return LocalDateTime.parse(raw.substring(0, 19), DATETIME_FORMATTER);
            }
            return LocalDate.parse(raw, DATE_FORMATTER).atStartOfDay();
        } catch (DateTimeParseException e) {
            System.err.println("SqliteUtil: Could not parse datetime: " + raw);
            return null;
        }
    }
}
