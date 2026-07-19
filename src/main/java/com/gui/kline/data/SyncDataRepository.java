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
     * Gets all unsynced rows across all tables as a JSON string.
     */
    public String getUnsyncedRowsAsJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            return mapper.writeValueAsString(getUnsyncedRows());
        } catch (Exception e) {
            e.printStackTrace();
            return "{}";
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
