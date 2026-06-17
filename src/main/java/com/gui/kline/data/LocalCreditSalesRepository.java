package com.gui.kline.data;

import com.gui.kline.controller.CreditSalesController;
import com.gui.kline.models.CreditSaleDetail;
import com.gui.kline.models.Part;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LocalCreditSalesRepository {

    private final LocalCatalogRepository catalogRepository = new LocalCatalogRepository();

    public void saveCreditSale(CreditSaleDetail detail, CreditSalesController.CreditSaleRow row) {
         String sql = "INSERT INTO credit_sales (id, credit_id, sale_date, customer_name, due_date, subtotal, paid_amount, status, created_at) " +
                 "VALUES (UUID(), ?, ?, ?, ?, ?, ?, ?, NOW()) " +
                 "ON DUPLICATE KEY UPDATE sale_date=?, customer_name=?, due_date=?, subtotal=?, paid_amount=?, status=?, updated_at=NOW()";
         
         try (Connection conn = getConnection();
              PreparedStatement ps = conn.prepareStatement(sql)) {
             
             ps.setString(1, row.getCreditId());
             ps.setString(2, row.getDate());
             ps.setString(3, row.getCustomer());
             ps.setString(4, row.getDueDate());
             ps.setDouble(5, row.getAmount());
             ps.setDouble(6, detail.getPaid());
             ps.setString(7, row.getStatus());
             
             // For update clause
             ps.setString(8, row.getDate());
             ps.setString(9, row.getCustomer());
             ps.setString(10, row.getDueDate());
             ps.setDouble(11, row.getAmount());
             ps.setDouble(12, detail.getPaid());
             ps.setString(13, row.getStatus());
             
             ps.executeUpdate();
             
             // Save parts and update inventory
             saveParts(row.getCreditId(), detail.getParts());
             updateInventoryForCreditSale(detail.getParts());
         } catch (SQLException e) {
             throw new RuntimeException("Failed to save credit sale: " + e.getMessage());
         }
     }

     private void saveParts(String creditId, List<Part> parts) {
         // First get the credit_sales.id for the credit_id
         String getIdSql = "SELECT id FROM credit_sales WHERE credit_id = ?";
         String creditSaleId = null;
         
         try (Connection conn = getConnection();
              PreparedStatement ps = conn.prepareStatement(getIdSql)) {
             ps.setString(1, creditId);
             ResultSet rs = ps.executeQuery();
             if (rs.next()) {
                 creditSaleId = rs.getString("id");
             }
         } catch (SQLException e) {
             throw new RuntimeException("Failed to get credit sale id: " + e.getMessage());
         }
         
         if (creditSaleId == null) {
             throw new RuntimeException("Credit sale not found");
         }
         
         // Delete old parts first
         String deleteSql = "DELETE FROM credit_sale_parts WHERE credit_sale_id = ?";
         try (Connection conn = getConnection();
              PreparedStatement psDelete = conn.prepareStatement(deleteSql)) {
             psDelete.setString(1, creditSaleId);
             psDelete.executeUpdate();
         } catch (SQLException e) {
             throw new RuntimeException("Failed to delete old parts: " + e.getMessage());
         }
         
         // Insert new parts
         String sql = "INSERT INTO credit_sale_parts (id, credit_sale_id, product_id, description, quantity, unit_price, total, created_at) " +
                 "VALUES (UUID(), ?, ?, ?, ?, ?, ?, NOW())";
         
         try (Connection conn = getConnection();
              PreparedStatement ps = conn.prepareStatement(sql)) {
             
             final String finalCreditSaleId = creditSaleId;
             for (Part part : parts) {
                 ps.setString(1, finalCreditSaleId);
                 ps.setString(2, part.getProductId());
                 ps.setString(3, part.getDescription());
                 ps.setInt(4, part.getQuantity());
                 ps.setDouble(5, part.getUnitPrice());
                 ps.setDouble(6, part.getTotal());
                 ps.addBatch();
             }
             ps.executeBatch();
         } catch (SQLException e) {
             throw new RuntimeException("Failed to save parts: " + e.getMessage());
         }
     }

     private void updateInventoryForCreditSale(List<Part> parts) {
         for (Part part : parts) {
             if (part.getProductId() != null) {
                 // Reduce stock for each part added
                 catalogRepository.updateProductStock(part.getProductId(), -part.getQuantity());
             }
         }
     }

    public CreditSaleDetail loadCreditSaleDetail(String creditId) {
        String sql = "SELECT * FROM credit_sales WHERE credit_id = ?";
        CreditSaleDetail detail = null;
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, creditId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                detail = new CreditSaleDetail();
                detail.setCreditId(rs.getString("credit_id"));
                detail.setCustomer(rs.getString("customer_name"));
                detail.setDate(LocalDate.parse(rs.getString("sale_date")));
                detail.setDueDate(LocalDate.parse(rs.getString("due_date")));
                detail.setPaid(rs.getDouble("paid_amount"));
                
                // Load parts
                List<Part> parts = loadParts(creditId);
                for (Part part : parts) {
                    detail.addPart(part);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load credit sale detail: " + e.getMessage());
        }
        
        return detail;
    }

     private List<Part> loadParts(String creditId) {
         // First get the credit_sales.id from credit_id
         String getIdSql = "SELECT id FROM credit_sales WHERE credit_id = ?";
         String creditSaleId = null;
         
         try (Connection conn = getConnection();
              PreparedStatement ps = conn.prepareStatement(getIdSql)) {
             ps.setString(1, creditId);
             ResultSet rs = ps.executeQuery();
             if (rs.next()) {
                 creditSaleId = rs.getString("id");
             }
         } catch (SQLException e) {
             return new ArrayList<>(); // Return empty list if credit sale not found
         }
         
         if (creditSaleId == null) {
             return new ArrayList<>();
         }
         
         String sql = "SELECT * FROM credit_sale_parts WHERE credit_sale_id = ?";
         List<Part> parts = new ArrayList<>();
         
         try (Connection conn = getConnection();
              PreparedStatement ps = conn.prepareStatement(sql)) {
             
             ps.setString(1, creditSaleId);
             ResultSet rs = ps.executeQuery();
             
             while (rs.next()) {
                 Part part = new Part(
                     rs.getString("description"),
                     "",  // category not stored in new schema
                     rs.getInt("quantity"),
                     rs.getDouble("unit_price"),
                     rs.getString("product_id")
                 );
                 parts.add(part);
             }
         } catch (SQLException e) {
             throw new RuntimeException("Failed to load parts: " + e.getMessage());
         }
         
         return parts;
     }

     public void deleteCreditSale(String creditId) {
         // First, get the credit_sales.id
         String getIdSql = "SELECT id FROM credit_sales WHERE credit_id = ?";
         String creditSaleId = null;
         
         try (Connection conn = getConnection();
              PreparedStatement ps = conn.prepareStatement(getIdSql)) {
             ps.setString(1, creditId);
             ResultSet rs = ps.executeQuery();
             if (rs.next()) {
                 creditSaleId = rs.getString("id");
             }
         } catch (SQLException e) {
             throw new RuntimeException("Failed to get credit sale id: " + e.getMessage());
         }
         
         if (creditSaleId == null) {
             throw new RuntimeException("Credit sale not found");
         }

         // Load parts to restore inventory
         List<Part> parts = loadParts(creditId);
         for (Part part : parts) {
             if (part.getProductId() != null) {
                 // Restore stock
                 catalogRepository.updateProductStock(part.getProductId(), part.getQuantity());
             }
         }

         String sql = "DELETE FROM credit_sales WHERE credit_id = ?";
         
         try (Connection conn = getConnection();
              PreparedStatement ps = conn.prepareStatement(sql)) {
             
             // Delete parts first
             String deleteParts = "DELETE FROM credit_sale_parts WHERE credit_sale_id = ?";
             try (PreparedStatement psDelete = conn.prepareStatement(deleteParts)) {
                 psDelete.setString(1, creditSaleId);
                 psDelete.executeUpdate();
             }
             
             ps.setString(1, creditId);
             ps.executeUpdate();
         } catch (SQLException e) {
             throw new RuntimeException("Failed to delete credit sale: " + e.getMessage());
         }
     }

    public void updatePayment(String creditId, double paidAmount) {
        String sql = "UPDATE credit_sales SET paid_amount = ?, status = ? WHERE credit_id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setDouble(1, paidAmount);
            double total = getTotalAmount(creditId);
            String status = paidAmount >= total ? "PAID" : (paidAmount > 0 ? "PARTIAL" : "PENDING");
            ps.setString(2, status);
            ps.setString(3, creditId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update payment: " + e.getMessage());
        }
    }

    private double getTotalAmount(String creditId) {
        String sql = "SELECT subtotal FROM credit_sales WHERE credit_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, creditId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getDouble("subtotal");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get total amount: " + e.getMessage());
        }
        return 0;
    }

    public List<CreditSalesController.CreditSaleRow> loadAllCreditSales() {
        String sql = "SELECT * FROM credit_sales ORDER BY sale_date DESC";
        List<CreditSalesController.CreditSaleRow> sales = new ArrayList<>();
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                CreditSalesController.CreditSaleRow row = new CreditSalesController.CreditSaleRow(
                    rs.getString("credit_id"),
                    rs.getString("sale_date"),
                    rs.getString("customer_name"),
                    rs.getString("due_date"),
                    rs.getDouble("subtotal"),
                    rs.getString("status")
                );
                sales.add(row);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load credit sales: " + e.getMessage());
        }
        
        return sales;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://localhost:3306/kline_local", "root", "");
    }
}

