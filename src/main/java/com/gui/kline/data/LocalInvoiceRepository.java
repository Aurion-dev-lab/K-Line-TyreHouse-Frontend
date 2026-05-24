package com.gui.kline.data;

import com.gui.kline.models.InvoiceDetail;
import com.gui.kline.models.InvoiceRow;
import com.gui.kline.models.LineItem;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LocalInvoiceRepository {

    public void saveInvoice(InvoiceDetail detail, InvoiceRow row) {
        String sql = "INSERT INTO invoices (id, invoice_id, customer, invoice_date, type, status, subtotal, tax, grand_total, created_at) " +
                "VALUES (UUID(), ?, ?, ?, ?, ?, ?, ?, ?, NOW()) " +
                "ON DUPLICATE KEY UPDATE subtotal = VALUES(subtotal), tax = VALUES(tax), grand_total = VALUES(grand_total), updated_at = NOW()";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, row.getInvoiceId());
            statement.setString(2, detail.getCustomer());
            statement.setString(3, row.getDate());
            statement.setString(4, row.getType());
            statement.setString(5, "completed");
            statement.setDouble(6, detail.getSubtotal());
            statement.setDouble(7, detail.getTax());
            statement.setDouble(8, detail.getGrandTotal());
            statement.executeUpdate();

            // retrieve internal id for the invoice we just inserted/updated
            String lookup = "SELECT id FROM invoices WHERE invoice_id = ? LIMIT 1";
            try (PreparedStatement ps = connection.prepareStatement(lookup)) {
                ps.setString(1, row.getInvoiceId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String internalId = rs.getString("id");
                        // save line items if any
                        for (LineItem item : detail.getLineItems()) {
                            saveInvoiceLineItemWithRef(connection, internalId, row.getInvoiceId(), item, item.getProductId());
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to save invoice", ex);
        }
    }

    public void saveInvoiceLineItem(String invoiceId, LineItem item, String productId) {
        try (Connection connection = DatabaseManager.getConnection()) {
            // find internal id for invoice
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

    public List<InvoiceRow> loadInvoices() {
        String sql = "SELECT invoice_id, customer, invoice_date, type, COUNT(*) as item_count, grand_total " +
                "FROM invoices GROUP BY invoice_id ORDER BY invoice_date DESC";
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
                        rs.getDouble("grand_total")
                );
                invoices.add(row);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load invoices", ex);
        }
        return invoices;
    }

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
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to delete invoice", ex);
        }
    }
}

