package com.gui.kline.data;

import com.gui.kline.models.ServiceRecord;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for ServiceRecord operations with sync support.
 */
public class ServiceRecordRepository {
    
    /**
     * Get all service records
     */
    public List<ServiceRecord> getAllServices() {
        List<ServiceRecord> services = new ArrayList<>();
        String sql = "SELECT * FROM services ORDER BY service_date DESC";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            
            while (rs.next()) {
                services.add(mapServiceRecord(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load service records: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        return services;
    }
    
    /**
     * Get unsynced service records (services that haven't been synced yet)
     */
    public List<ServiceRecord> getUnsyncedServices() {
        List<ServiceRecord> services = new ArrayList<>();
        String sql = "SELECT * FROM services WHERE sync_status = false OR sync_status IS NULL OR synced_at IS NULL";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            
            while (rs.next()) {
                services.add(mapServiceRecord(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load unsynced service records: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        return services;
    }
    
    /**
     * Get service record by ID
     */
    public ServiceRecord getServiceRecordById(String id) {
        String sql = "SELECT * FROM services WHERE id = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapServiceRecord(rs);
                }
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load service record: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Save or update a service record
     */
    public String saveServiceRecord(ServiceRecord serviceRecord) {
        String id = serviceRecord.getId() != null ? serviceRecord.getId() : java.util.UUID.randomUUID().toString();
        
        String sql = "INSERT INTO services (id, name, price, service_date, remark, " +
                "sync_id, device_id, synced_at, sync_status, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now')) " +
                "ON CONFLICT(id) DO UPDATE SET name = excluded.name, price = excluded.price, " +
                "service_date = excluded.service_date, remark = excluded.remark, " +
                "sync_id = excluded.sync_id, device_id = excluded.device_id, " +
                "synced_at = excluded.synced_at, sync_status = excluded.sync_status";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, id);
            statement.setString(2, serviceRecord.getService());
            statement.setDouble(3, serviceRecord.getPrice());
            statement.setString(4, serviceRecord.getDate() != null ? serviceRecord.getDate().toString() : null);
            statement.setString(5, serviceRecord.getRemark());
            statement.setString(6, serviceRecord.getSyncId());
            statement.setString(7, serviceRecord.getDeviceId());
            statement.setString(8, serviceRecord.getSyncedAt() != null ? serviceRecord.getSyncedAt().toString() : null);
            statement.setInt(9, serviceRecord.isSyncStatus() ? 1 : 0);
            
            statement.executeUpdate();
            
            return id;
            
        } catch (SQLException ex) {
            System.err.println("Failed to save service record: " + ex.getMessage());
            ex.printStackTrace();
            throw new RuntimeException("Failed to save service record", ex);
        }
    }
    
    /**
     * Mark a service record as synced
     */
    public void markAsSynced(String serviceRecordId) {
        String sql = "UPDATE services SET sync_status = 1, synced_at = datetime('now') WHERE id = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, serviceRecordId);
            statement.executeUpdate();
            
        } catch (SQLException ex) {
            System.err.println("Failed to mark service record as synced: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * Delete a service record
     */
    public void deleteServiceRecord(String id) {
        String sql = "DELETE FROM services WHERE id = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, id);
            statement.executeUpdate();
            
        } catch (SQLException ex) {
            System.err.println("Failed to delete service record: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * Map ResultSet to ServiceRecord object
     */
    private ServiceRecord mapServiceRecord(ResultSet rs) throws SQLException {
        LocalDate serviceDate = rs.getString("service_date") != null ? 
            LocalDate.parse(rs.getString("service_date")) : LocalDate.now();
        
        ServiceRecord serviceRecord = new ServiceRecord(
            serviceDate, 
            rs.getString("name"), 
            rs.getString("remark"), 
            rs.getDouble("price")
        );
        
        serviceRecord.setId(rs.getString("id"));
        
        // Sync fields
        serviceRecord.setSyncId(rs.getString("sync_id"));
        serviceRecord.setDeviceId(rs.getString("device_id"));
        serviceRecord.setSyncedAt(rs.getTimestamp("synced_at") != null ? 
            rs.getTimestamp("synced_at").toLocalDateTime() : null);
        serviceRecord.setSyncStatus(rs.getBoolean("sync_status"));
        serviceRecord.setCreatedAt(rs.getTimestamp("created_at") != null ? 
            rs.getTimestamp("created_at").toLocalDateTime() : null);
        serviceRecord.setUpdatedAt(rs.getTimestamp("updated_at") != null ? 
            rs.getTimestamp("updated_at").toLocalDateTime() : null);
        
        return serviceRecord;
    }
}