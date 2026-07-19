package com.gui.kline.data;

import com.gui.kline.models.InvoiceDetail;
import com.gui.kline.models.InvoiceRow;
import com.gui.kline.models.LineItem;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LocalInvoiceRepository {

    /**
     * Save or update an invoice with all its line items
     */
    public String saveInvoice(InvoiceDetail detail, InvoiceRow row) {
        String internalId = null;
        String sql = "INSERT INTO invoices (id, invoice_id, customer, invoice_date, type, status, subtotal, tax, grand_total, created_at) " +
                "VALUES (UUID(), ?, ?, ?, ?, ?, ?, ?, ?, NOW()) " +
                "ON DUPLICATE KEY UPDATE customer = VALUES(customer), type = VALUES(type), status = VALUES(status), subtotal = VALUES(subtotal), tax = VALUES(tax), grand_total = VALUES(grand_total), updated_at = NOW()";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, row.getInvoiceId());
            statement.setString(2, detail.getCustomer());
            statement.setString(3, row.getDate());
            statement.setString(4, row.getType());
            statement.setString(5, detail.getStatus());
            statement.setDouble(6, detail.getSubtotal());
            statement.setDouble(7, detail.getTax());
            statement.setDouble(8, detail.getGrandTotal());
            statement.executeUpdate();

            // Retrieve internal id for the invoice
            String lookup = "SELECT id FROM invoices WHERE invoice_id = ? LIMIT 1";
            try (PreparedStatement ps = connection.prepareStatement(lookup)) {
                ps.setString(1, row.getInvoiceId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        internalId = rs.getString("id");
                    }
                }
            }

            if (internalId != null) {
                // Clear old line items and save new ones
                deleteInvoiceLineItems(connection, row.getInvoiceId());
                for (LineItem item : detail.getLineItems()) {
                    saveInvoiceLineItemWithRef(connection, internalId, row.getInvoiceId(), item, item.getProductId());
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to save invoice", ex);
        }
        return internalId;
    }

    /**
     * Save a single line item to an existing invoice
     */
    public void saveInvoiceLineItem(String invoiceId, LineItem item, String productId) {
        try (Connection connection = DatabaseManager.getConnection()) {
            String lookup = "SELECT id FROM invoices WHERE invoice_id = ? LIMIT 1";
            try (PreparedStatement ps = connection.prepareStatement(lookup)) {
                ps.setString(1, invoiceId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String internalId = rs.getString("id");
                        saveInvoiceLineItemWithRef(connection, internalId, invoiceId, item, productId);
                    }
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to save invoice line item", ex);
        }
    }

    /**
     * Internal method to save line item with foreign key reference
     */
    private void saveInvoiceLineItemWithRef(Connection connection, String invoiceRef, String invoiceId, LineItem item, String productId) throws SQLException {
        String sql = "INSERT INTO invoice_line_items (id, invoice_id, invoice_ref, product_id, description, type, qty, unit_price, total, created_at) " +
                "VALUES (UUID(), ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, invoiceId);
            statement.setString(2, invoiceRef);
            statement.setString(3, productId);
            statement.setString(4, item.getDescription());
            statement.setString(5, item.getType());
            statement.setInt(6, item.getQty());
            statement.setDouble(7, item.getUnitPrice());
            statement.setDouble(8, item.getTotal());
            statement.executeUpdate();
        }
    }

    /**
     * Load all invoices with summary information
     */
    public List<InvoiceRow> loadInvoices() {
        String sql = "SELECT i.id, i.invoice_id, i.customer, i.invoice_date, i.type, i.status, COUNT(DISTINCT ili.id) as item_count, i.grand_total " +
                "FROM invoices i LEFT JOIN invoice_line_items ili ON i.id = ili.invoice_ref " +
                "GROUP BY i.id ORDER BY i.invoice_date DESC";
        List<InvoiceRow> invoices = new ArrayList<>();
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                InvoiceRow row = new InvoiceRow(
                        rs.getString("invoice_id"),
                        rs.getString("invoice_date"),
                        rs.getString("customer"),
                        rs.getString("type"),
                        rs.getInt("item_count"),
                        rs.getDouble("grand_total"),
                        rs.getString("status")
                );
                invoices.add(row);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load invoices", ex);
        }
        return invoices;
    }

    /**
     * Load complete invoice details including all line items
     */
    public InvoiceDetail loadInvoiceDetail(String invoiceId) {
        String sql = "SELECT invoice_id, customer, invoice_date, type, subtotal, tax, grand_total, status " +
                "FROM invoices WHERE invoice_id = ? LIMIT 1";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, invoiceId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    InvoiceDetail detail = new InvoiceDetail();
                    detail.setInvoiceId(rs.getString("invoice_id"));
                    detail.setCustomer(rs.getString("customer"));
                    detail.setDate(rs.getString("invoice_date"));
                    detail.setType(rs.getString("type"));
                    detail.setStatus(rs.getString("status"));
                    detail.setTaxRate(rs.getDouble("tax") > 0 ? rs.getDouble("tax") / rs.getDouble("subtotal") : 0.0);
                    detail.setDiscountAmount(Math.max(0, rs.getDouble("subtotal") + rs.getDouble("tax") - rs.getDouble("grand_total")));

                    // Load line items
                    String lineItemsSql = "SELECT description, type, qty, unit_price, product_id FROM invoice_line_items WHERE invoice_id = ?";
                    try (PreparedStatement ps = connection.prepareStatement(lineItemsSql)) {
                        ps.setString(1, invoiceId);
                        try (ResultSet itemsRs = ps.executeQuery()) {
                            while (itemsRs.next()) {
                                LineItem item = new LineItem(
                                        itemsRs.getString("description"),
                                        itemsRs.getString("type"),
                                        itemsRs.getInt("qty"),
                                        itemsRs.getDouble("unit_price"),
                                        itemsRs.getString("product_id")
                                );
                                detail.addLineItem(item);
                            }
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
        String sql = "UPDATE invoices SET status = ?, updated_at = NOW() WHERE invoice_id = ?";
        
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
     * Delete an invoice and all its line items
     */
    public void deleteInvoice(String invoiceId) {
        String delItems = "DELETE FROM invoice_line_items WHERE invoice_id = ?";
        String delInvoice = "DELETE FROM invoices WHERE invoice_id = ?";
        
        try (Connection connection = DatabaseManager.getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(delItems)) {
                stmt.setString(1, invoiceId);
                stmt.executeUpdate();
            }
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
     * Delete line items for an invoice
     */
    private void deleteInvoiceLineItems(Connection connection, String invoiceId) throws SQLException {
        String sql = "DELETE FROM invoice_line_items WHERE invoice_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, invoiceId);
            stmt.executeUpdate();
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
