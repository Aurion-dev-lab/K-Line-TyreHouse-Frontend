package com.gui.kline.data;

import com.gui.kline.models.Invoice;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for Invoice operations with sync support.
 */
public class InvoiceRepository {
    
    /**
     * Get all invoices
     */
    public List<Invoice> getAllInvoices() {
        List<Invoice> invoices = new ArrayList<>();
        String sql = "SELECT * FROM invoices ORDER BY invoice_date DESC";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            
            while (rs.next()) {
                invoices.add(mapInvoice(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load invoices: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        return invoices;
    }
    
    /**
     * Get unsynced invoices (invoices that haven't been synced yet)
     */
    public List<Invoice> getUnsyncedInvoices() {
        List<Invoice> invoices = new ArrayList<>();
        String sql = "SELECT * FROM invoices WHERE sync_status = false OR sync_status IS NULL OR synced_at IS NULL";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            
            while (rs.next()) {
                invoices.add(mapInvoice(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load unsynced invoices: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        return invoices;
    }
    
    /**
     * Get invoice by ID
     */
    public Invoice getInvoiceById(String id) {
        String sql = "SELECT * FROM invoices WHERE id = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapInvoice(rs);
                }
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load invoice: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Save or update an invoice
     */
    public String saveInvoice(Invoice invoice) {
        String id = invoice.getId() != null ? invoice.getId() : java.util.UUID.randomUUID().toString();
        
        String sql = "INSERT INTO invoices (id, invoice_id, customer, invoice_date, type, status, " +
                "subtotal, tax, grand_total, created_at, customer_name, customer_phone, " +
                "payment_method, payment_reference, amount_paid, balance_due, notes, terms_and_conditions, " +
                "created_by, updated_by, cancelled_at, cancellation_reason, discount, shipping, " +
                "sync_id, device_id, synced_at, sync_status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(id) DO UPDATE SET invoice_id = excluded.invoice_id, customer = excluded.customer, " +
                "invoice_date = excluded.invoice_date, type = excluded.type, status = excluded.status, " +
                "subtotal = excluded.subtotal, tax = excluded.tax, grand_total = excluded.grand_total, " +
                "customer_name = excluded.customer_name, customer_phone = excluded.customer_phone, " +
                "payment_method = excluded.payment_method, payment_reference = excluded.payment_reference, " +
                "amount_paid = excluded.amount_paid, balance_due = excluded.balance_due, " +
                "notes = excluded.notes, terms_and_conditions = excluded.terms_and_conditions, " +
                "created_by = excluded.created_by, updated_by = excluded.updated_by, " +
                "cancelled_at = excluded.cancelled_at, cancellation_reason = excluded.cancellation_reason, " +
                "discount = excluded.discount, shipping = excluded.shipping, " +
                "sync_id = excluded.sync_id, device_id = excluded.device_id, " +
                "synced_at = excluded.synced_at, sync_status = excluded.sync_status";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, id);
            statement.setString(2, invoice.getInvoiceNumber());
            statement.setString(3, invoice.getCustomerName());
            statement.setString(4, invoice.getInvoiceDate() != null ? invoice.getInvoiceDate().toString() : null);
            statement.setString(5, invoice.getType());
            statement.setString(6, invoice.getStatus());
            statement.setDouble(7, invoice.getSubtotal());
            statement.setDouble(8, invoice.getTax());
            statement.setDouble(9, invoice.getGrandTotal());
            statement.setString(10, invoice.getCustomerName());
            statement.setString(11, invoice.getCustomerPhone());
            statement.setString(12, invoice.getPaymentMethod());
            statement.setString(13, invoice.getPaymentReference());
            statement.setDouble(14, invoice.getAmountPaid());
            statement.setDouble(15, invoice.getBalanceDue());
            statement.setString(16, invoice.getNotes());
            statement.setString(17, invoice.getTermsAndConditions());
            statement.setString(18, invoice.getCreatedBy());
            statement.setString(19, invoice.getUpdatedBy());
            statement.setString(20, invoice.getCancelledAt() != null ? invoice.getCancelledAt().toString() : null);
            statement.setString(21, invoice.getCancellationReason());
            statement.setDouble(22, invoice.getDiscount());
            statement.setDouble(23, invoice.getShipping());
            statement.setString(24, invoice.getSyncId());
            statement.setString(25, invoice.getDeviceId());
            statement.setString(26, invoice.getSyncedAt() != null ? invoice.getSyncedAt().toString() : null);
            statement.setInt(27, invoice.isSyncStatus() ? 1 : 0);
            
            statement.executeUpdate();
            
            return id;
            
        } catch (SQLException ex) {
            System.err.println("Failed to save invoice: " + ex.getMessage());
            ex.printStackTrace();
            throw new RuntimeException("Failed to save invoice", ex);
        }
    }
    
    /**
     * Mark an invoice as synced
     */
    public void markAsSynced(String invoiceId) {
        String sql = "UPDATE invoices SET sync_status = 1, synced_at = datetime('now') WHERE id = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, invoiceId);
            statement.executeUpdate();
            
        } catch (SQLException ex) {
            System.err.println("Failed to mark invoice as synced: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * Delete an invoice
     */
    public void deleteInvoice(String id) {
        String sql = "DELETE FROM invoices WHERE id = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, id);
            statement.executeUpdate();
            
        } catch (SQLException ex) {
            System.err.println("Failed to delete invoice: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * Map ResultSet to Invoice object
     */
    private Invoice mapInvoice(ResultSet rs) throws SQLException {
        Invoice invoice = new Invoice();
        invoice.setId(rs.getString("id"));
        invoice.setInvoiceNumber(rs.getString("invoice_id"));
        invoice.setCustomer(rs.getString("customer"));
        invoice.setCustomerName(rs.getString("customer_name"));
        invoice.setCustomerPhone(rs.getString("customer_phone"));
        
        Date invoiceDate = rs.getDate("invoice_date");
        if (invoiceDate != null) {
            invoice.setInvoiceDate(invoiceDate.toLocalDate());
        }
        
        invoice.setType(rs.getString("type"));
        invoice.setStatus(rs.getString("status"));
        invoice.setSubtotal(rs.getDouble("subtotal"));
        invoice.setTax(rs.getDouble("tax"));
        invoice.setDiscount(rs.getDouble("discount"));
        invoice.setShipping(rs.getDouble("shipping"));
        invoice.setGrandTotal(rs.getDouble("grand_total"));
        invoice.setAmountPaid(rs.getDouble("amount_paid"));
        invoice.setBalanceDue(rs.getDouble("balance_due"));
        invoice.setNotes(rs.getString("notes"));
        invoice.setTermsAndConditions(rs.getString("terms_and_conditions"));
        invoice.setPaymentMethod(rs.getString("payment_method"));
        invoice.setPaymentReference(rs.getString("payment_reference"));
        invoice.setCreatedBy(rs.getString("created_by"));
        invoice.setUpdatedBy(rs.getString("updated_by"));
        
        Timestamp cancelledAt = rs.getTimestamp("cancelled_at");
        if (cancelledAt != null) {
            invoice.setCancelledAt(cancelledAt.toLocalDateTime());
        }
        
        invoice.setCancellationReason(rs.getString("cancellation_reason"));
        
        // Sync fields
        invoice.setSyncId(rs.getString("sync_id"));
        invoice.setDeviceId(rs.getString("device_id"));
        invoice.setSyncedAt(rs.getTimestamp("synced_at") != null ? 
            rs.getTimestamp("synced_at").toLocalDateTime() : null);
        invoice.setSyncStatus(rs.getBoolean("sync_status"));
        invoice.setCreatedAt(rs.getTimestamp("created_at") != null ? 
            rs.getTimestamp("created_at").toLocalDateTime() : null);
        invoice.setUpdatedAt(rs.getTimestamp("updated_at") != null ? 
            rs.getTimestamp("updated_at").toLocalDateTime() : null);
        
        return invoice;
    }
}