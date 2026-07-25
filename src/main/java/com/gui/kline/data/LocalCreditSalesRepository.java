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

        String sql = "INSERT INTO credit_sales (id, credit_id, sale_date, customer_name, due_date, subtotal, paid_amount, status, labour, parts_cost, discount, parts, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP) " +
                "ON CONFLICT(credit_id) DO UPDATE SET sale_date = excluded.sale_date, customer_name = excluded.customer_name, due_date = excluded.due_date, subtotal = excluded.subtotal, paid_amount = excluded.paid_amount, status = excluded.status, labour = excluded.labour, parts_cost = excluded.parts_cost, discount = excluded.discount, parts = excluded.parts, sync_status = 0, updated_at = CURRENT_TIMESTAMP";
         
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, java.util.UUID.randomUUID().toString());
            ps.setString(2, row.getCreditId());
            ps.setString(3, row.getDate());
            ps.setString(4, row.getCustomer());
            ps.setString(5, row.getDueDate());
            ps.setDouble(6, row.getAmount());
            ps.setDouble(7, detail.getPaid());
            ps.setString(8, row.getStatus());
            ps.setDouble(9, detail.getLabour());
            ps.setDouble(10, detail.getPartsCost());
            ps.setDouble(11, detail.getDiscount());
            ps.setString(12, partsJson);
            
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
        String sql = "SELECT * FROM credit_sales WHERE credit_id = ?";
        CreditSaleDetail detail = null;
        
        try (Connection conn = DatabaseManager.getConnection();
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
                try { detail.setLabour(rs.getDouble("labour")); } catch (SQLException e) { detail.setLabour(0); }
                try { detail.setPartsCost(rs.getDouble("parts_cost")); } catch (SQLException e) { detail.setPartsCost(0); }
                try { detail.setDiscount(rs.getDouble("discount")); } catch (SQLException e) { detail.setDiscount(0); }
                
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
        String sql = "UPDATE credit_sales SET paid_amount = ?, status = ?, sync_status = 0 WHERE credit_id = ?";
        
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
        String sql = "SELECT subtotal, labour, parts_cost, discount FROM credit_sales WHERE credit_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, creditId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                double subtotal = rs.getDouble("subtotal");
                double labour = 0;
                double partsCost = 0;
                double discount = 0;
                try { labour = rs.getDouble("labour"); } catch (SQLException e) {}
                try { partsCost = rs.getDouble("parts_cost"); } catch (SQLException e) {}
                try { discount = rs.getDouble("discount"); } catch (SQLException e) {}
                return subtotal + labour + partsCost - discount;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get total amount: " + e.getMessage());
        }
        return 0;
    }

    public List<CreditSalesController.CreditSaleRow> loadAllCreditSales() {
        String sql = "SELECT * FROM credit_sales ORDER BY sale_date DESC";
        List<CreditSalesController.CreditSaleRow> sales = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                CreditSalesController.CreditSaleRow row = new CreditSalesController.CreditSaleRow(
                    rs.getString("credit_id"),
                    rs.getString("sale_date"),
                    rs.getString("customer_name"),
                    rs.getString("due_date"),
                    rs.getDouble("subtotal"),
                    rs.getDouble("paid_amount"),
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

