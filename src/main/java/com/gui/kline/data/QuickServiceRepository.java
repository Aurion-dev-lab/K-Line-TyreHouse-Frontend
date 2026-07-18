package com.gui.kline.data;

import com.gui.kline.models.QuickService;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for QuickService operations with sync support.
 */
public class QuickServiceRepository {
    
    /**
     * Get all quick services
     */
    public List<QuickService> getAllQuickServices() {
        List<QuickService> quickServices = new ArrayList<>();
        String sql = "SELECT * FROM quick_services ORDER BY service_date DESC";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            
            while (rs.next()) {
                quickServices.add(mapQuickService(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load quick services: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        return quickServices;
    }
    
    /**
     * Get unsynced quick services (services that haven't been synced yet)
     */
    public List<QuickService> getUnsyncedQuickServices() {
        List<QuickService> quickServices = new ArrayList<>();
        String sql = "SELECT * FROM quick_services WHERE sync_status = false OR sync_status IS NULL OR synced_at IS NULL";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            
            while (rs.next()) {
                quickServices.add(mapQuickService(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load unsynced quick services: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        return quickServices;
    }
    
    /**
     * Get quick service by ID
     */
    public QuickService getQuickServiceById(String id) {
        String sql = "SELECT * FROM quick_services WHERE id = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapQuickService(rs);
                }
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load quick service: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Save or update a quick service
     */
    public String saveQuickService(QuickService quickService) {
        String id = quickService.getId() != null ? quickService.getId() : java.util.UUID.randomUUID().toString();
        
        String sql = "INSERT INTO quick_services (id, service, price, service_date, notes, created_by, updated_by, " +
                "sync_status, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW()) " +
                "ON DUPLICATE KEY UPDATE service = VALUES(service), price = VALUES(price), " +
                "service_date = VALUES(service_date), notes = VALUES(notes), " +
                "created_by = VALUES(created_by), updated_by = VALUES(updated_by), " +
                "" +
                "sync_status = VALUES(sync_status), " +
                "updated_at = NOW()";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, id);
            statement.setString(2, quickService.getService());
            statement.setDouble(3, quickService.getPrice());
            statement.setDate(4, quickService.getServiceDate() != null ? Date.valueOf(quickService.getServiceDate()) : null);
            statement.setString(5, quickService.getNotes());
            statement.setString(6, quickService.getCreatedBy());
            statement.setString(7, quickService.getUpdatedBy());
            statement.setBoolean(8, quickService.isSyncStatus());
            
            statement.executeUpdate();
            
            return id;
            
        } catch (SQLException ex) {
            System.err.println("Failed to save quick service: " + ex.getMessage());
            ex.printStackTrace();
            throw new RuntimeException("Failed to save quick service", ex);
        }
    }
    
    /**
     * Mark a quick service as synced
     */
    public void markAsSynced(String quickServiceId) {
        String sql = "UPDATE quick_services SET sync_status = true WHERE id = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, quickServiceId);
            statement.executeUpdate();
            
        } catch (SQLException ex) {
            System.err.println("Failed to mark quick service as synced: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * Delete a quick service
     */
    public void deleteQuickService(String id) {
        String sql = "DELETE FROM quick_services WHERE id = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, id);
            statement.executeUpdate();
            
        } catch (SQLException ex) {
            System.err.println("Failed to delete quick service: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * Map ResultSet to QuickService object
     */
    private QuickService mapQuickService(ResultSet rs) throws SQLException {
        QuickService quickService = new QuickService();
        quickService.setId(rs.getString("id"));
        quickService.setService(rs.getString("service"));
        quickService.setPrice(rs.getDouble("price"));
        
        Date serviceDate = rs.getDate("service_date");
        if (serviceDate != null) {
            quickService.setServiceDate(serviceDate.toLocalDate());
        }
        
        quickService.setNotes(rs.getString("notes"));
        quickService.setCreatedBy(rs.getString("created_by"));
        quickService.setUpdatedBy(rs.getString("updated_by"));
        
        // Sync fields
        quickService.setSyncStatus(rs.getBoolean("sync_status"));
        quickService.setCreatedAt(rs.getTimestamp("created_at") != null ? 
            rs.getTimestamp("created_at").toLocalDateTime() : null);
        quickService.setUpdatedAt(rs.getTimestamp("updated_at") != null ? 
            rs.getTimestamp("updated_at").toLocalDateTime() : null);
        
        return quickService;
    }
}