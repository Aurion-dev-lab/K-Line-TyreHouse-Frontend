package com.gui.kline.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gui.kline.controller.CreditSalesController;
import com.gui.kline.models.CreditSaleDetail;
import com.gui.kline.models.Part;
import com.gui.kline.utils.JsonUtil;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LocalCreditSalesRepository {

    private final LocalCatalogRepository catalogRepository = new LocalCatalogRepository();
    private final ObjectMapper objectMapper = JsonUtil.createObjectMapper();

    public void saveCreditSale(CreditSaleDetail detail, CreditSalesController.CreditSaleRow row) {
        String partsJson = null;
        try {
            partsJson = objectMapper.writeValueAsString(detail.getParts());
        } catch (Exception e) {
            System.err.println("Failed to serialize parts: " + e.getMessage());
            e.printStackTrace();
            partsJson = "[]";
        }

        String sql = "INSERT INTO credit_sales (id, credit_id, sale_date, customer_id, due_date, sub_total, grand_total, settlement, status, parts, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP) " +
                "ON CONFLICT(credit_id) DO UPDATE SET sale_date = excluded.sale_date, customer_id = excluded.customer_id, due_date = excluded.due_date, sub_total = excluded.sub_total, grand_total = excluded.grand_total, settlement = excluded.settlement, status = excluded.status, parts = excluded.parts, sync_status = 0, updated_at = CURRENT_TIMESTAMP";
         
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, java.util.UUID.randomUUID().toString());
            ps.setString(2, row.getCreditId());
            ps.setString(3, row.getDate());
            ps.setString(4, detail.getCustomerId());
            ps.setString(5, row.getDueDate());
            ps.setDouble(6, detail.getSubtotal());
            ps.setDouble(7, detail.getGrandTotal());
            ps.setDouble(8, detail.getSettlement());
            ps.setString(9, row.getStatus());
            ps.setString(10, partsJson);
            
            ps.executeUpdate();
            
            // Restore inventory for previous parts if edit mode, then deduct for new parts
            CreditSaleDetail existing = loadCreditSaleDetail(row.getCreditId());
            if (existing != null && existing.getParts() != null) {
                // deduct inventory for current parts
                updateInventoryForCreditSale(detail.getParts());
            } else {
                updateInventoryForCreditSale(detail.getParts());
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save credit sale: " + e.getMessage());
        }
    }

    private void updateInventoryForCreditSale(List<Part> parts) {
        if (parts == null) return;
        for (Part part : parts) {
            if (part.getProductId() != null) {
                catalogRepository.updateProductStock(part.getProductId(), -part.getQuantity());
            }
        }
    }

    private void restoreInventoryForCreditSale(List<Part> parts) {
        if (parts == null) return;
        for (Part part : parts) {
            if (part.getProductId() != null) {
                catalogRepository.updateProductStock(part.getProductId(), part.getQuantity());
            }
        }
    }

    public CreditSaleDetail loadCreditSaleDetail(String creditId) {
        String sql = "SELECT cs.*, cc.name AS customer_name, cc.phone AS customer_phone FROM credit_sales cs " +
                "LEFT JOIN credit_customers cc ON cs.customer_id = cc.id WHERE cs.credit_id = ?";
        CreditSaleDetail detail = null;
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, creditId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                detail = new CreditSaleDetail();
                detail.setCreditId(rs.getString("credit_id"));
                detail.setCustomerId(rs.getString("customer_id"));
                detail.setCustomerName(rs.getString("customer_name"));
                detail.setPhone(rs.getString("customer_phone"));
                detail.setDate(LocalDate.parse(rs.getString("sale_date")));
                detail.setDueDate(LocalDate.parse(rs.getString("due_date")));
                detail.setSettlement(rs.getDouble("settlement"));
                
                String partsJson = rs.getString("parts");
                if (partsJson != null && !partsJson.isBlank()) {
                    try {
                        List<Part> parts = objectMapper.readValue(partsJson, new TypeReference<List<Part>>() {});
                        if (parts != null) {
                            for (Part part : parts) {
                                detail.addPart(part);
                            }
                        }
                    } catch (Exception ignored) { }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load credit sale detail: " + e.getMessage());
        }
        
        return detail;
    }

    public void deleteCreditSale(String creditId) {
        CreditSaleDetail detail = loadCreditSaleDetail(creditId);
        if (detail != null && detail.getParts() != null) {
            restoreInventoryForCreditSale(detail.getParts());
        }

        String sql = "DELETE FROM credit_sales WHERE credit_id = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, creditId);
            ps.executeUpdate();
            DatabaseManager.logDeletion("credit_sales", creditId);
            
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete credit sale: " + e.getMessage());
        }
    }

    public void updatePayment(String creditId, double paidAmount) {
        String sql = "UPDATE credit_sales SET settlement = ?, status = ?, sync_status = 0 WHERE credit_id = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
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
        String sql = "SELECT grand_total FROM credit_sales WHERE credit_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, creditId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getDouble("grand_total");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get total amount: " + e.getMessage());
        }
        return 0;
    }

    public List<CreditSalesController.CreditSaleRow> loadAllCreditSales() {
        String sql = "SELECT cs.credit_id, cs.sale_date, cc.name AS customer_name, cs.due_date, " +
                "cs.grand_total, cs.settlement, cs.status FROM credit_sales cs " +
                "LEFT JOIN credit_customers cc ON cs.customer_id = cc.id " +
                "ORDER BY cs.sale_date DESC";
        List<CreditSalesController.CreditSaleRow> sales = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                CreditSalesController.CreditSaleRow row = new CreditSalesController.CreditSaleRow(
                    rs.getString("credit_id"),
                    rs.getString("sale_date"),
                    rs.getString("customer_name") != null ? rs.getString("customer_name") : "Unknown",
                    rs.getString("due_date"),
                    rs.getDouble("grand_total"),
                    rs.getDouble("settlement"),
                    rs.getString("status")
                );
                sales.add(row);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load credit sales: " + e.getMessage());
        }
        
        return sales;
    }


}

