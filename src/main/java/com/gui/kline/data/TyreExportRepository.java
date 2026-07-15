package com.gui.kline.data;

import com.gui.kline.models.TyreExport;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for TyreExport operations with sync support.
 */
public class TyreExportRepository {
    
    /**
     * Get all tyre exports
     */
    public List<TyreExport> getAllExports() {
        List<TyreExport> exports = new ArrayList<>();
        String sql = "SELECT * FROM tyre_exports ORDER BY export_date DESC";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            
            while (rs.next()) {
                exports.add(mapTyreExport(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load tyre exports: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        return exports;
    }
    
    /**
     * Get unsynced tyre exports (exports that haven't been synced yet)
     */
    public List<TyreExport> getUnsyncedExports() {
        List<TyreExport> exports = new ArrayList<>();
        String sql = "SELECT * FROM tyre_exports WHERE sync_status = false OR sync_status IS NULL OR synced_at IS NULL";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            
            while (rs.next()) {
                exports.add(mapTyreExport(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load unsynced tyre exports: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        return exports;
    }
    
    /**
     * Get tyre export by ID
     */
    public TyreExport getTyreExportById(String id) {
        String sql = "SELECT * FROM tyre_exports WHERE id = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapTyreExport(rs);
                }
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load tyre export: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Save or update a tyre export
     */
    public String saveTyreExport(TyreExport tyreExport) {
        String id = tyreExport.getId() != null ? tyreExport.getId() : java.util.UUID.randomUUID().toString();
        
        String sql = "INSERT INTO tyre_exports (id, export_id, operation, company, tyres, cust_price, " +
                "comp_price, service_fee, paid_amount, total_amount, balance_amount, payment_status, " +
                "status, export_date, notes, created_by, updated_by, " +
                "sync_id, device_id, synced_at, sync_status, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now')) " +
                "ON CONFLICT(id) DO UPDATE SET export_id = excluded.export_id, operation = excluded.operation, " +
                "company = excluded.company, tyres = excluded.tyres, cust_price = excluded.cust_price, " +
                "comp_price = excluded.comp_price, service_fee = excluded.service_fee, " +
                "paid_amount = excluded.paid_amount, total_amount = excluded.total_amount, " +
                "balance_amount = excluded.balance_amount, payment_status = excluded.payment_status, " +
                "status = excluded.status, export_date = excluded.export_date, notes = excluded.notes, " +
                "created_by = excluded.created_by, updated_by = excluded.updated_by, " +
                "sync_id = excluded.sync_id, device_id = excluded.device_id, " +
                "synced_at = excluded.synced_at, sync_status = excluded.sync_status";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, id);
            statement.setString(2, tyreExport.getExportId());
            statement.setString(3, tyreExport.getOperation());
            statement.setString(4, tyreExport.getCompany());
            statement.setInt(5, tyreExport.getTyres());
            statement.setDouble(6, tyreExport.getCustPrice());
            statement.setDouble(7, tyreExport.getCompPrice());
            statement.setDouble(8, tyreExport.getServiceFee());
            statement.setDouble(9, tyreExport.getPaidAmount());
            statement.setDouble(10, tyreExport.getTotalAmount());
            statement.setDouble(11, tyreExport.getBalanceAmount());
            statement.setString(12, tyreExport.getPaymentStatus());
            statement.setString(13, tyreExport.getStatus());
            statement.setString(14, tyreExport.getExportDate() != null ? tyreExport.getExportDate().toString() : null);
            statement.setString(15, tyreExport.getNotes());
            statement.setString(16, tyreExport.getCreatedBy());
            statement.setString(17, tyreExport.getUpdatedBy());
            statement.setString(18, tyreExport.getSyncId());
            statement.setString(19, tyreExport.getDeviceId());
            statement.setString(20, tyreExport.getSyncedAt() != null ? tyreExport.getSyncedAt().toString() : null);
            statement.setInt(21, tyreExport.isSyncStatus() ? 1 : 0);
            
            statement.executeUpdate();
            
            return id;
            
        } catch (SQLException ex) {
            System.err.println("Failed to save tyre export: " + ex.getMessage());
            ex.printStackTrace();
            throw new RuntimeException("Failed to save tyre export", ex);
        }
    }
    
    /**
     * Mark a tyre export as synced
     */
    public void markAsSynced(String tyreExportId) {
        String sql = "UPDATE tyre_exports SET sync_status = 1, synced_at = datetime('now') WHERE id = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, tyreExportId);
            statement.executeUpdate();
            
        } catch (SQLException ex) {
            System.err.println("Failed to mark tyre export as synced: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * Delete a tyre export
     */
    public void deleteTyreExport(String id) {
        String sql = "DELETE FROM tyre_exports WHERE id = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, id);
            statement.executeUpdate();
            
        } catch (SQLException ex) {
            System.err.println("Failed to delete tyre export: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * Map ResultSet to TyreExport object
     */
    private TyreExport mapTyreExport(ResultSet rs) throws SQLException {
        TyreExport tyreExport = new TyreExport();
        tyreExport.setId(rs.getString("id"));
        tyreExport.setExportId(rs.getString("export_id"));
        tyreExport.setOperation(rs.getString("operation"));
        tyreExport.setCompany(rs.getString("company"));
        tyreExport.setTyres(rs.getInt("tyres"));
        tyreExport.setCustPrice(rs.getDouble("cust_price"));
        tyreExport.setCompPrice(rs.getDouble("comp_price"));
        tyreExport.setServiceFee(rs.getDouble("service_fee"));
        tyreExport.setPaidAmount(rs.getDouble("paid_amount"));
        tyreExport.setTotalAmount(rs.getDouble("total_amount"));
        tyreExport.setBalanceAmount(rs.getDouble("balance_amount"));
        tyreExport.setPaymentStatus(rs.getString("payment_status"));
        tyreExport.setStatus(rs.getString("status"));
        
        Date exportDate = rs.getDate("export_date");
        if (exportDate != null) {
            tyreExport.setExportDate(exportDate.toLocalDate());
        }
        
        tyreExport.setNotes(rs.getString("notes"));
        tyreExport.setCreatedBy(rs.getString("created_by"));
        tyreExport.setUpdatedBy(rs.getString("updated_by"));
        
        // Sync fields
        tyreExport.setSyncId(rs.getString("sync_id"));
        tyreExport.setDeviceId(rs.getString("device_id"));
        tyreExport.setSyncedAt(rs.getTimestamp("synced_at") != null ? 
            rs.getTimestamp("synced_at").toLocalDateTime() : null);
        tyreExport.setSyncStatus(rs.getBoolean("sync_status"));
        tyreExport.setCreatedAt(rs.getTimestamp("created_at") != null ? 
            rs.getTimestamp("created_at").toLocalDateTime() : null);
        tyreExport.setUpdatedAt(rs.getTimestamp("updated_at") != null ? 
            rs.getTimestamp("updated_at").toLocalDateTime() : null);
        
        return tyreExport;
    }
}