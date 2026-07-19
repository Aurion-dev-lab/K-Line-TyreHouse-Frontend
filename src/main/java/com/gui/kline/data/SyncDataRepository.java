package com.gui.kline.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Centralized repository for retrieving unsynced data across all tables.
 */
public class SyncDataRepository {

    private static final String[] SYNC_TABLES = {
        "credit_sale_parts", "credit_sales", "customers", "expenses",
        "invoice_line_items", "invoices", "products", "quick_service_presets",
        "quick_services", "salary_advances", "salary_payments", "services",
        "tyre_exports", "worker_attendance", "worker_credits", "workers"
    };

    /**
     * Executes a generic query to fetch all unsynced rows from a specific table.
     */
    private List<Map<String, Object>> fetchUnsyncedRowsForTable(String tableName) {
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "SELECT * FROM " + tableName + " WHERE sync_status = 0";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
             
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                rows.add(row);
            }
        } catch (SQLException ex) {
            System.err.println("Failed to fetch unsynced rows for table " + tableName + ": " + ex.getMessage());
            ex.printStackTrace();
        }
        
        return rows;
    }

    /**
     * Executes a generic query to get the count of unsynced rows for a specific table.
     */
    private int getUnsyncedCountForTable(String tableName) {
        String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE sync_status = 0";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            System.err.println("Failed to fetch unsynced count for table " + tableName + ": " + ex.getMessage());
            ex.printStackTrace();
        }
        return 0;
    }

    /**
     * Gets all unsynced rows across all tables as a Map.
     */
    public Map<String, List<Map<String, Object>>> getUnsyncedRows() {
        Map<String, List<Map<String, Object>>> allData = new HashMap<>();
        for (String table : SYNC_TABLES) {
            allData.put(table, fetchUnsyncedRowsForTable(table));
        }
        return allData;
    }

    /**
     * Gets pending deletions from the tombstone table.
     */
    public List<Map<String, String>> getPendingDeletions() {
        List<Map<String, String>> deletions = new ArrayList<>();
        String sql = "SELECT table_name, record_id FROM sync_tombstones";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                Map<String, String> deletion = new HashMap<>();
                deletion.put("tableName", rs.getString("table_name"));
                deletion.put("recordId", rs.getString("record_id"));
                deletions.add(deletion);
            }
        } catch (SQLException ex) {
            System.err.println("Failed to fetch pending deletions: " + ex.getMessage());
        }
        return deletions;
    }

    /**
     * Gets all unsynced data (inserts/updates and deletions) as a JSON string payload.
     */
    public String getSyncPayloadAsJson() {
        try {
            Map<String, Object> rootEnvelope = new HashMap<>();
            rootEnvelope.put("data", getUnsyncedRows());
            rootEnvelope.put("deletions", getPendingDeletions());
            
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            return mapper.writeValueAsString(rootEnvelope);
        } catch (Exception e) {
            e.printStackTrace();
            return "{}";
        }
    }

    /**
     * Marks all unsynced rows as synced and clears processed tombstones.
     */
    public void markAsSynced(String syncInitiationTimestamp) {
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try (java.sql.Statement stmt = connection.createStatement()) {
                // Update sync_status for all tables
                for (String table : SYNC_TABLES) {
                    stmt.executeUpdate("UPDATE " + table + " SET sync_status = 1 WHERE sync_status = 0");
                }
                
                // Clear tombstones up to the sync initiation timestamp
                if (syncInitiationTimestamp != null && !syncInitiationTimestamp.isEmpty()) {
                    try (PreparedStatement pstmt = connection.prepareStatement("DELETE FROM sync_tombstones WHERE client_deleted_at <= ?")) {
                        pstmt.setString(1, syncInitiationTimestamp);
                        pstmt.executeUpdate();
                    }
                }
                
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            System.err.println("Failed to mark data as synced: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Gets the total count of unsynced items across all sync-enabled tables.
     */
    public int getTotalUnsyncedCount() {
        int total = 0;
        for (String table : SYNC_TABLES) {
            total += getUnsyncedCountForTable(table);
        }
        return total;
    }
}
