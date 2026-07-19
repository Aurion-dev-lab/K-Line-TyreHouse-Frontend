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
        
        String sql = "INSERT INTO tyre_exports (id, export_id, operation, serial_number, company, tyres, cust_price, " +
                "comp_price, service_fee, paid_amount, total_amount, balance_amount, payment_status, " +
                "status, export_date, notes, created_by, updated_by, " +
                "sync_status, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW()) " +
                "ON DUPLICATE KEY UPDATE export_id = VALUES(export_id), operation = VALUES(operation), " +
                "serial_number = VALUES(serial_number), company = VALUES(company), tyres = VALUES(tyres), " +
                "cust_price = VALUES(cust_price), comp_price = VALUES(comp_price), service_fee = VALUES(service_fee), " +
                "paid_amount = VALUES(paid_amount), total_amount = VALUES(total_amount), " +
                "balance_amount = VALUES(balance_amount), payment_status = VALUES(payment_status), " +
                "status = VALUES(status), export_date = VALUES(export_date), notes = VALUES(notes), " +
                "created_by = VALUES(created_by), updated_by = VALUES(updated_by), " +
                "" +
                "sync_status = VALUES(sync_status), " +
                "updated_at = NOW()";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, id);
            statement.setString(2, tyreExport.getExportId());
            statement.setString(3, tyreExport.getOperation());
            statement.setString(4, tyreExport.getSerialNumber());
            statement.setString(5, tyreExport.getCompany());
            statement.setInt(6, tyreExport.getTyres());
            statement.setDouble(7, tyreExport.getCustPrice());
            statement.setDouble(8, tyreExport.getCompPrice());
            statement.setDouble(9, tyreExport.getServiceFee());
            statement.setDouble(10, tyreExport.getPaidAmount());
            statement.setDouble(11, tyreExport.getTotalAmount());
            statement.setDouble(12, tyreExport.getBalanceAmount());
            statement.setString(13, tyreExport.getPaymentStatus());
            statement.setString(14, tyreExport.getStatus());
            statement.setDate(15, tyreExport.getExportDate() != null ? Date.valueOf(tyreExport.getExportDate()) : null);
            statement.setString(16, tyreExport.getNotes());
            statement.setString(17, tyreExport.getCreatedBy());
            statement.setString(18, tyreExport.getUpdatedBy());
            statement.setBoolean(19, tyreExport.isSyncStatus());
            
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
        String sql = "UPDATE tyre_exports SET sync_status = true WHERE id = ?";
        
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
     * Delete a tyre export by id
     */
    public void deleteTyreExport(String id) {
        String sql = "DELETE FROM tyre_exports WHERE id = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, id);
            statement.executeUpdate();
            DatabaseManager.logDeletion("tyre_exports", id);
            
        } catch (SQLException ex) {
            System.err.println("Failed to delete tyre export: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * Delete a tyre export by export_id
     */
    public void deleteTyreExportByExportId(String exportId) {
        String sql = "DELETE FROM tyre_exports WHERE export_id = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, exportId);
            statement.executeUpdate();
            
        } catch (SQLException ex) {
            System.err.println("Failed to delete tyre export: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * Get tyre export by export_id
     */
    public TyreExport getTyreExportByExportId(String exportId) {
        String sql = "SELECT * FROM tyre_exports WHERE export_id = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, exportId);
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
     * Map ResultSet to TyreExport object
     */
    private TyreExport mapTyreExport(ResultSet rs) throws SQLException {
        TyreExport tyreExport = new TyreExport();
        tyreExport.setId(rs.getString("id"));
        tyreExport.setExportId(rs.getString("export_id"));
        tyreExport.setOperation(rs.getString("operation"));
        tyreExport.setSerialNumber(rs.getString("serial_number"));
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
        tyreExport.setSyncStatus(rs.getBoolean("sync_status"));
        tyreExport.setCreatedAt(rs.getTimestamp("created_at") != null ? 
            rs.getTimestamp("created_at").toLocalDateTime() : null);
        tyreExport.setUpdatedAt(rs.getTimestamp("updated_at") != null ? 
            rs.getTimestamp("updated_at").toLocalDateTime() : null);
        
        return tyreExport;
    }
}