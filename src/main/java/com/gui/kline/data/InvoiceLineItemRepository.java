package com.gui.kline.data;

import com.gui.kline.models.LineItem;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for Invoice Line Item operations with sync support.
 */
public class InvoiceLineItemRepository {
    
    /**
     * Get all line items
     */
    public List<LineItem> getAllLineItems() {
        List<LineItem> lineItems = new ArrayList<>();
        String sql = "SELECT * FROM invoice_line_items ORDER BY created_at DESC";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            
            while (rs.next()) {
                lineItems.add(mapLineItem(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load line items: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        return lineItems;
    }
    
    /**
     * Get unsynced line items (line items that haven't been synced yet)
     */
    public List<LineItem> getUnsyncedLineItems() {
        List<LineItem> lineItems = new ArrayList<>();
        String sql = "SELECT * FROM invoice_line_items WHERE sync_status = false OR sync_status IS NULL OR synced_at IS NULL";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            
            while (rs.next()) {
                lineItems.add(mapLineItem(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load unsynced line items: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        return lineItems;
    }
    
    /**
     * Get line item by ID
     */
    public LineItem getLineItemById(String id) {
        String sql = "SELECT * FROM invoice_line_items WHERE id = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapLineItem(rs);
                }
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load line item: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Save or update a line item
     */
    public String saveLineItem(LineItem lineItem) {
        String id = lineItem.getId() != null ? lineItem.getId() : java.util.UUID.randomUUID().toString();
        
        String sql = "INSERT INTO invoice_line_items (id, invoice_id, invoice_ref, product_id, description, type, " +
                "qty, unit_price, total, created_at, sync_status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), ?) " +
                "ON DUPLICATE KEY UPDATE invoice_id = VALUES(invoice_id), invoice_ref = VALUES(invoice_ref), " +
                "product_id = VALUES(product_id), description = VALUES(description), type = VALUES(type), " +
                "qty = VALUES(qty), unit_price = VALUES(unit_price), total = VALUES(total), " +
                "" +
                "sync_status = VALUES(sync_status)";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, id);
            statement.setString(2, lineItem.getInvoiceId());
            statement.setString(3, lineItem.getInvoiceRef());
            statement.setString(4, lineItem.getProductId());
            statement.setString(5, lineItem.getDescription());
            statement.setString(6, lineItem.getType());
            statement.setInt(7, lineItem.getQty());
            statement.setDouble(8, lineItem.getUnitPrice());
            statement.setDouble(9, lineItem.getTotal());
            statement.setBoolean(10, lineItem.isSyncStatus());
            
            statement.executeUpdate();
            
            return id;
            
        } catch (SQLException ex) {
            System.err.println("Failed to save line item: " + ex.getMessage());
            ex.printStackTrace();
            throw new RuntimeException("Failed to save line item", ex);
        }
    }
    
    /**
     * Mark a line item as synced
     */
    public void markAsSynced(String lineItemId) {
        String sql = "UPDATE invoice_line_items SET sync_status = true WHERE id = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, lineItemId);
            statement.executeUpdate();
            
        } catch (SQLException ex) {
            System.err.println("Failed to mark line item as synced: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * Delete a line item
     */
    public void deleteLineItem(String id) {
        String sql = "DELETE FROM invoice_line_items WHERE id = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, id);
            statement.executeUpdate();
            
        } catch (SQLException ex) {
            System.err.println("Failed to delete line item: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * Map ResultSet to LineItem object
     */
    private LineItem mapLineItem(ResultSet rs) throws SQLException {
        LineItem lineItem = new LineItem("", "", 0, 0.0);
        lineItem.setId(rs.getString("id"));
        lineItem.setInvoiceId(rs.getString("invoice_id"));
        lineItem.setInvoiceRef(rs.getString("invoice_ref"));
        lineItem.setProductId(rs.getString("product_id"));
        lineItem.setDescription(rs.getString("description"));
        lineItem.setType(rs.getString("type"));
        lineItem.setQty(rs.getInt("qty"));
        lineItem.setUnitPrice(rs.getDouble("unit_price"));
        lineItem.setTotal(rs.getDouble("total"));
        
        // Sync fields
        lineItem.setSyncStatus(rs.getBoolean("sync_status"));
        lineItem.setCreatedAt(rs.getTimestamp("created_at") != null ? 
            rs.getTimestamp("created_at").toLocalDateTime() : null);
        
        return lineItem;
    }
}