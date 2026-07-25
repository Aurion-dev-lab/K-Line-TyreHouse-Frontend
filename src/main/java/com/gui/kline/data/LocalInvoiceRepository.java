package com.gui.kline.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gui.kline.models.InvoiceDetail;
import com.gui.kline.models.InvoiceRow;
import com.gui.kline.models.LineItem;
import com.gui.kline.models.Product;
import com.gui.kline.utils.JsonUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LocalInvoiceRepository {

    private final ObjectMapper objectMapper = JsonUtil.createObjectMapper();

    /**
     * Save or update an invoice with all its line items stored as JSON
     */
    public String saveInvoice(InvoiceDetail detail, InvoiceRow row) {
        String internalId = null;
        String lineItemsJson = null;
        try {
            lineItemsJson = objectMapper.writeValueAsString(detail.getLineItems());
        } catch (Exception e) {
            System.err.println("Failed to serialize line items: " + e.getMessage());
            e.printStackTrace();
            lineItemsJson = "[]";
        }

        String sql = "INSERT INTO invoices (id, invoice_id, customer, phone, description, invoice_date, type, status, subtotal, grand_total, line_items, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP) " +
                "ON CONFLICT(invoice_id) DO UPDATE SET customer = excluded.customer, phone = excluded.phone, description = excluded.description, type = excluded.type, status = excluded.status, subtotal = excluded.subtotal, grand_total = excluded.grand_total, line_items = excluded.line_items, sync_status = 0, updated_at = CURRENT_TIMESTAMP";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, java.util.UUID.randomUUID().toString());
            statement.setString(2, row.getInvoiceId());
            statement.setString(3, detail.getCustomer());
            statement.setString(4, detail.getPhone());
            statement.setString(5, detail.getDescription());
            statement.setString(6, row.getDate());
            statement.setString(7, row.getType());
            statement.setString(8, detail.getStatus());
            statement.setDouble(9, detail.getSubtotal());
            statement.setDouble(10, detail.getGrandTotal());
            statement.setString(11, lineItemsJson);
            statement.executeUpdate();

            String lookup = "SELECT id FROM invoices WHERE invoice_id = ? LIMIT 1";
            try (PreparedStatement ps = connection.prepareStatement(lookup)) {
                ps.setString(1, row.getInvoiceId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        internalId = rs.getString("id");
                    }
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to save invoice", ex);
        }
        return internalId;
    }

    /**
     * Save a single line item to an existing invoice (by appending to line_items JSON)
     */
    public void saveInvoiceLineItem(String invoiceId, LineItem item, String productId) {
        InvoiceDetail detail = loadInvoiceDetail(invoiceId);
        if (detail != null) {
            if (productId != null && !productId.isBlank()) {
                item.setProductId(productId);
            }
            detail.addLineItem(item);
            InvoiceRow row = new InvoiceRow(
                    detail.getInvoiceId(),
                    detail.getDate(),
                    detail.getCustomer(),
                    detail.getType(),
                    detail.getLineItems().size(),
                    detail.getGrandTotal(),
                    detail.getStatus()
            );
            saveInvoice(detail, row);
        }
    }

    /**
     * Load all invoices with summary information
     */
    public List<InvoiceRow> loadInvoices() {
        String sql = "SELECT invoice_id, customer, phone, description, invoice_date, type, status, line_items, grand_total FROM invoices ORDER BY invoice_date DESC";
        List<InvoiceRow> invoices = new ArrayList<>();
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                String lineItemsJson = rs.getString("line_items");
                int itemCount = 0;
                if (lineItemsJson != null && !lineItemsJson.isBlank()) {
                    try {
                        List<LineItem> items = objectMapper.readValue(lineItemsJson, new TypeReference<List<LineItem>>() {});
                        itemCount = items != null ? items.size() : 0;
                    } catch (Exception ex) {
                        System.err.println("Failed to parse line_items JSON: " + ex.getMessage());
                    }
                }
                InvoiceRow row = new InvoiceRow(
                        rs.getString("invoice_id"),
                        rs.getString("invoice_date"),
                        rs.getString("customer"),
                        rs.getString("type"),
                        itemCount,
                        rs.getDouble("grand_total"),
                        rs.getString("status"),
                        rs.getString("phone"),
                        rs.getString("description")
                );
                invoices.add(row);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load invoices", ex);
        }
        return invoices;
    }

    /**
     * Load complete invoice details including line items from JSON
     */
    public InvoiceDetail loadInvoiceDetail(String invoiceId) {
        String sql = "SELECT invoice_id, customer, phone, description, invoice_date, type, subtotal, grand_total, status, line_items " +
                "FROM invoices WHERE invoice_id = ? LIMIT 1";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, invoiceId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    InvoiceDetail detail = new InvoiceDetail();
                    detail.setInvoiceId(rs.getString("invoice_id"));
                    detail.setCustomer(rs.getString("customer"));
                    detail.setPhone(rs.getString("phone"));
                    detail.setDescription(rs.getString("description"));
                    detail.setDate(rs.getString("invoice_date"));
                    detail.setType(rs.getString("type"));
                    detail.setStatus(rs.getString("status"));
                    detail.setTaxRate(0.0);
                    detail.setDiscountAmount(Math.max(0, rs.getDouble("subtotal") - rs.getDouble("grand_total")));

                    String lineItemsJson = rs.getString("line_items");
                    if (lineItemsJson != null && !lineItemsJson.isBlank()) {
                        try {
                            List<LineItem> items = objectMapper.readValue(lineItemsJson, new TypeReference<List<LineItem>>() {});
                            if (items != null) {
                                for (LineItem item : items) {
                                    detail.addLineItem(item);
                                }
                            }
                        } catch (Exception ex) {
                            System.err.println("Failed to parse line_items JSON for detail: " + ex.getMessage());
                        }
                    }
                    return detail;
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load invoice detail", ex);
        }
        return null;
    }

    /**
     * Update invoice status (completed, cancelled, etc.)
     */
    public void updateInvoiceStatus(String invoiceId, String status) {
        String sql = "UPDATE invoices SET status = ?, sync_status = 0, updated_at = CURRENT_TIMESTAMP WHERE invoice_id = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setString(2, invoiceId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to update invoice status", ex);
        }
    }

    /**
     * Delete an invoice
     */
    public void deleteInvoice(String invoiceId) {
        String delInvoice = "DELETE FROM invoices WHERE invoice_id = ?";
        
        try (Connection connection = DatabaseManager.getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(delInvoice)) {
                stmt.setString(1, invoiceId);
                stmt.executeUpdate();
                DatabaseManager.logDeletion("invoices", invoiceId);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to delete invoice", ex);
        }
    }

    /**
     * Get count of invoices
     */
    public int getInvoiceCount() {
        String sql = "SELECT COUNT(*) as total FROM invoices";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to get invoice count", ex);
        }
        return 0;
    }

    /**
     * Get total revenue
     */
    public double getTotalRevenue() {
        String sql = "SELECT SUM(grand_total) as total FROM invoices WHERE status = 'completed'";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                return rs.getDouble("total");
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to get total revenue", ex);
        }
        return 0.0;
    }
}
