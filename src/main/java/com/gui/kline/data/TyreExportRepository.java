package com.gui.kline.data;

import com.gui.kline.models.TyreExport;
import com.gui.kline.utils.Utils;

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
        String id = tyreExport.getId() != null ? tyreExport.getId() : Utils.generateId("EXP-PK-", 8);
        
        String sql = "INSERT INTO tyre_exports (id, export_id, serial_number, company, tyre_size, tyre_make, tyres, cust_price, " +
                "comp_price, service_fee, sub_total, grand_total, initial_payment, settlement, " +
                "status, export_date, remark, " +
                "sync_status, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, strftime('%Y-%m-%dT%H:%M:%S', 'now'), strftime('%Y-%m-%dT%H:%M:%S', 'now')) " +
                "ON CONFLICT(id) DO UPDATE SET export_id = excluded.export_id, " +
                "serial_number = excluded.serial_number, company = excluded.company, " +
                "tyre_size = excluded.tyre_size, tyre_make = excluded.tyre_make, tyres = excluded.tyres, " +
                "cust_price = excluded.cust_price, comp_price = excluded.comp_price, service_fee = excluded.service_fee, " +
                "sub_total = excluded.sub_total, grand_total = excluded.grand_total, " +
                "initial_payment = excluded.initial_payment, settlement = excluded.settlement, " +
                "status = excluded.status, export_date = excluded.export_date, remark = excluded.remark, " +
                "sync_status = 0, " +
                "updated_at = strftime('%Y-%m-%dT%H:%M:%S', 'now')";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, id);
            statement.setString(2, tyreExport.getExportId());
            statement.setString(3, tyreExport.getSerialNumber());
            statement.setString(4, tyreExport.getCompany());
            statement.setString(5, tyreExport.getTyreSize());
            statement.setString(6, tyreExport.getTyreMake());
            statement.setInt(7, tyreExport.getTyres());
            statement.setDouble(8, tyreExport.getCustPrice());
            statement.setDouble(9, tyreExport.getCompPrice());
            statement.setDouble(10, tyreExport.getServiceFee());
            statement.setDouble(11, tyreExport.getSubTotal());
            statement.setDouble(12, tyreExport.getGrandTotal());
            statement.setDouble(13, tyreExport.getInitialPayment());
            statement.setDouble(14, tyreExport.getSettlement());
            statement.setString(15, tyreExport.getStatus());
            statement.setString(16, tyreExport.getExportDate() != null ? tyreExport.getExportDate().toString() : null);
            statement.setString(17, tyreExport.getRemark());
            statement.setBoolean(18, tyreExport.isSyncStatus());
            
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
        tyreExport.setSerialNumber(rs.getString("serial_number"));
        tyreExport.setCompany(rs.getString("company"));
        tyreExport.setTyreSize(rs.getString("tyre_size"));
        tyreExport.setTyreMake(rs.getString("tyre_make"));
        tyreExport.setTyres(rs.getInt("tyres"));
        tyreExport.setCustPrice(rs.getDouble("cust_price"));
        tyreExport.setCompPrice(rs.getDouble("comp_price"));
        tyreExport.setServiceFee(rs.getDouble("service_fee"));
        tyreExport.setSubTotal(rs.getDouble("sub_total"));
        tyreExport.setGrandTotal(rs.getDouble("grand_total"));
        tyreExport.setInitialPayment(rs.getDouble("initial_payment"));
        tyreExport.setSettlement(rs.getDouble("settlement"));
        tyreExport.setStatus(rs.getString("status"));
        
        String exportDateStr = rs.getString("export_date");
        if (exportDateStr != null && !exportDateStr.isBlank()) {
            tyreExport.setExportDate(com.gui.kline.utils.SqliteUtil.getLocalDate(rs, "export_date"));
        }
        
        tyreExport.setRemark(rs.getString("remark"));
        
        // Sync fields
        tyreExport.setSyncStatus(rs.getBoolean("sync_status"));
        tyreExport.setCreatedAt(com.gui.kline.utils.SqliteUtil.getLocalDateTime(rs, "created_at"));
        tyreExport.setUpdatedAt(com.gui.kline.utils.SqliteUtil.getLocalDateTime(rs, "updated_at"));
        
        return tyreExport;
    }
}