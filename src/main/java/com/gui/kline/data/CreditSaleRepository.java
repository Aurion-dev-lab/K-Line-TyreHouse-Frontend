package com.gui.kline.data;

import com.gui.kline.models.CreditSale;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for CreditSale operations with sync support.
 */
public class CreditSaleRepository {
    
    /**
     * Get all credit sales
     */
    public List<CreditSale> getAllCreditSales() {
        List<CreditSale> creditSales = new ArrayList<>();
        String sql = "SELECT * FROM credit_sales ORDER BY sale_date DESC";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            
            while (rs.next()) {
                creditSales.add(mapCreditSale(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load credit sales: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        return creditSales;
    }
    
    /**
     * Get unsynced credit sales (credit sales that haven't been synced yet)
     */
    public List<CreditSale> getUnsyncedCreditSales() {
        List<CreditSale> creditSales = new ArrayList<>();
        String sql = "SELECT * FROM credit_sales WHERE sync_status = false OR sync_status IS NULL OR synced_at IS NULL";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            
            while (rs.next()) {
                creditSales.add(mapCreditSale(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load unsynced credit sales: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        return creditSales;
    }
    
    /**
     * Get credit sale by ID
     */
    public CreditSale getCreditSaleById(String id) {
        String sql = "SELECT * FROM credit_sales WHERE id = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapCreditSale(rs);
                }
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load credit sale: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Save or update a credit sale
     */
    public String saveCreditSale(CreditSale creditSale) {
        String id = creditSale.getId() != null ? creditSale.getId() : java.util.UUID.randomUUID().toString();
        
        String sql = "INSERT INTO credit_sales (id, credit_id, customer_id, customer_name, customer_phone, " +
                "sale_date, due_date, subtotal, tax, discount, grand_total, amount, paid_amount, " +
                "balance_due, status, notes, terms_and_conditions, payment_method, payment_reference, " +
                "created_by, updated_by, cancelled_at, cancellation_reason, " +
                "sync_id, device_id, synced_at, sync_status, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now')) " +
                "ON CONFLICT(id) DO UPDATE SET credit_id = excluded.credit_id, customer_id = excluded.customer_id, " +
                "customer_name = excluded.customer_name, customer_phone = excluded.customer_phone, " +
                "sale_date = excluded.sale_date, due_date = excluded.due_date, subtotal = excluded.subtotal, " +
                "tax = excluded.tax, discount = excluded.discount, grand_total = excluded.grand_total, " +
                "amount = excluded.amount, paid_amount = excluded.paid_amount, balance_due = excluded.balance_due, " +
                "status = excluded.status, notes = excluded.notes, terms_and_conditions = excluded.terms_and_conditions, " +
                "payment_method = excluded.payment_method, payment_reference = excluded.payment_reference, " +
                "created_by = excluded.created_by, updated_by = excluded.updated_by, " +
                "cancelled_at = excluded.cancelled_at, cancellation_reason = excluded.cancellation_reason, " +
                "sync_id = excluded.sync_id, device_id = excluded.device_id, " +
                "synced_at = excluded.synced_at, sync_status = excluded.sync_status";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, id);
            statement.setString(2, creditSale.getCreditSaleNumber());
            statement.setString(3, creditSale.getCustomerId());
            statement.setString(4, creditSale.getCustomerName());
            statement.setString(5, creditSale.getCustomerPhone());
            statement.setString(6, creditSale.getSaleDate() != null ? creditSale.getSaleDate().toString() : null);
            statement.setString(7, creditSale.getDueDate() != null ? creditSale.getDueDate().toString() : null);
            statement.setDouble(8, creditSale.getSubtotal());
            statement.setDouble(9, creditSale.getTax());
            statement.setDouble(10, creditSale.getDiscount());
            statement.setDouble(11, creditSale.getGrandTotal());
            statement.setDouble(12, creditSale.getAmount());
            statement.setDouble(13, creditSale.getPaidAmount());
            statement.setDouble(14, creditSale.getBalanceDue());
            statement.setString(15, creditSale.getStatus());
            statement.setString(16, creditSale.getNotes());
            statement.setString(17, creditSale.getTermsAndConditions());
            statement.setString(18, creditSale.getPaymentMethod());
            statement.setString(19, creditSale.getPaymentReference());
            statement.setString(20, creditSale.getCreatedBy());
            statement.setString(21, creditSale.getUpdatedBy());
            statement.setString(22, creditSale.getCancelledAt() != null ? creditSale.getCancelledAt().toString() : null);
            statement.setString(23, creditSale.getCancellationReason());
            statement.setString(24, creditSale.getSyncId());
            statement.setString(25, creditSale.getDeviceId());
            statement.setString(26, creditSale.getSyncedAt() != null ? creditSale.getSyncedAt().toString() : null);
            statement.setInt(27, creditSale.isSyncStatus() ? 1 : 0);
            
            statement.executeUpdate();
            
            return id;
            
        } catch (SQLException ex) {
            System.err.println("Failed to save credit sale: " + ex.getMessage());
            ex.printStackTrace();
            throw new RuntimeException("Failed to save credit sale", ex);
        }
    }
    
    /**
     * Mark a credit sale as synced
     */
    public void markAsSynced(String creditSaleId) {
        String sql = "UPDATE credit_sales SET sync_status = 1, synced_at = datetime('now') WHERE id = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, creditSaleId);
            statement.executeUpdate();
            
        } catch (SQLException ex) {
            System.err.println("Failed to mark credit sale as synced: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * Delete a credit sale
     */
    public void deleteCreditSale(String id) {
        String sql = "DELETE FROM credit_sales WHERE id = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, id);
            statement.executeUpdate();
            
        } catch (SQLException ex) {
            System.err.println("Failed to delete credit sale: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * Map ResultSet to CreditSale object
     */
    private CreditSale mapCreditSale(ResultSet rs) throws SQLException {
        CreditSale creditSale = new CreditSale();
        creditSale.setId(rs.getString("id"));
        creditSale.setCreditSaleNumber(rs.getString("credit_id"));
        creditSale.setCustomerId(rs.getString("customer"));
        creditSale.setCustomerName(rs.getString("customer_name"));
        
        Date saleDate = rs.getDate("sale_date");
        if (saleDate != null) {
            creditSale.setSaleDate(saleDate.toLocalDate());
        }
        
        Date dueDate = rs.getDate("due_date");
        if (dueDate != null) {
            creditSale.setDueDate(dueDate.toLocalDate());
        }
        
        creditSale.setSubtotal(rs.getDouble("subtotal"));
        creditSale.setTax(rs.getDouble("tax"));
        creditSale.setDiscount(rs.getDouble("discount"));
        creditSale.setGrandTotal(rs.getDouble("grand_total"));
        creditSale.setAmount(rs.getDouble("amount"));
        creditSale.setPaidAmount(rs.getDouble("paid_amount"));
        creditSale.setBalanceDue(rs.getDouble("balance_due"));
        creditSale.setStatus(rs.getString("status"));
        creditSale.setNotes(rs.getString("notes"));
        creditSale.setTermsAndConditions(rs.getString("terms_and_conditions"));
        creditSale.setPaymentMethod(rs.getString("payment_method"));
        creditSale.setPaymentReference(rs.getString("payment_reference"));
        creditSale.setCreatedBy(rs.getString("created_by"));
        creditSale.setUpdatedBy(rs.getString("updated_by"));
        
        Timestamp cancelledAt = rs.getTimestamp("cancelled_at");
        if (cancelledAt != null) {
            creditSale.setCancelledAt(cancelledAt.toLocalDateTime());
        }
        
        creditSale.setCancellationReason(rs.getString("cancellation_reason"));
        
        // Sync fields
        creditSale.setSyncId(rs.getString("sync_id"));
        creditSale.setDeviceId(rs.getString("device_id"));
        creditSale.setSyncedAt(rs.getTimestamp("synced_at") != null ? 
            rs.getTimestamp("synced_at").toLocalDateTime() : null);
        creditSale.setSyncStatus(rs.getBoolean("sync_status"));
        creditSale.setCreatedAt(rs.getTimestamp("created_at") != null ? 
            rs.getTimestamp("created_at").toLocalDateTime() : null);
        creditSale.setUpdatedAt(rs.getTimestamp("updated_at") != null ? 
            rs.getTimestamp("updated_at").toLocalDateTime() : null);
        
        return creditSale;
    }
}