package com.gui.kline.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gui.kline.controller.CreditSalesController;
import com.gui.kline.models.ExportRecord;
import com.gui.kline.models.InvoiceRow;
import com.gui.kline.models.Product;

public class SyncQueueReader {
    public List<InvoiceRow> loadInvoices() {
        List<InvoiceRow> rows = new ArrayList<>();
        for (JsonObject payload : loadPayloads("invoice")) {
            String id = getText(payload, "invoiceId", "INV-");
            String date = getText(payload, "date", LocalDate.now().toString());
            String customer = getText(payload, "customer", "Customer");
            String type = getText(payload, "type", "Sales");
            double total = getNumber(payload, "grandTotal", 0.0);
            if (total == 0.0) {
                total = getNumber(payload, "total", 0.0);
            }
            int itemCount = getArraySize(payload, "items");
            rows.add(new InvoiceRow(id, date, customer, type, itemCount, total));
        }
        return rows;
    }

    public List<CreditSalesController.CreditSaleRow> loadCreditSales() {
        List<CreditSalesController.CreditSaleRow> rows = new ArrayList<>();
        for (JsonObject payload : loadPayloads("credit_sale")) {
            String id = getText(payload, "creditId", "CS-");
            String date = getText(payload, "date", LocalDate.now().toString());
            String customer = getText(payload, "customer", "Customer");
            String dueDate = getText(payload, "dueDate", LocalDate.now().toString());
            double amount = getNumber(payload, "amount", 0.0);
            String status = getText(payload, "status", "PENDING");
            rows.add(new CreditSalesController.CreditSaleRow(id, date, customer, dueDate, amount, status));
        }
        return rows;
    }

    public List<Product> loadProducts() {
        List<Product> products = new ArrayList<>();
        for (JsonObject payload : loadPayloads("product")) {
            String code = getText(payload, "productCode", "");
            String name = getText(payload, "name", "Product");
            String category = getText(payload, "category", "Other");
            double buy = getNumber(payload, "buyPrice", 0.0);
            double sell = getNumber(payload, "sellPrice", 0.0);
            int stock = (int) getNumber(payload, "stock", 0.0);
            Product product = new Product(name, category, buy, sell, stock);
            product.setCode(code);
            products.add(product);
        }
        return products;
    }

    public List<ExportRecord> loadTyreExports() {
        java.util.Map<String, ExportRecord> records = new java.util.LinkedHashMap<>();
        for (JsonObject payload : loadPayloads("tyre_export")) {
            String exportId = getText(payload, "exportId", resolveExportKey(payload));
            String operation = getText(payload, "operation", "create");
            String company = getText(payload, "company", "Company");
            int tyres = (int) getNumber(payload, "tyres", 0.0);
            double cust = getNumber(payload, "custPrice", 0.0);
            double comp = getNumber(payload, "compPrice", 0.0);
            double service = getNumber(payload, "serviceFee", 0.0);
            double total = getNumber(payload, "totalAmount", cust * tyres + service);
            double paid = getNumber(payload, "paidAmount", 0.0);
            double balance = getNumber(payload, "balanceAmount", Math.max(0.0, total - paid));
            String paymentStatus = getText(payload, "paymentStatus", paymentStatusFor(balance, paid));
            String dateStr = getText(payload, "date", LocalDate.now().toString());
            LocalDate date = LocalDate.parse(dateStr);
            String status = getText(payload, "status", "PENDING");

            ExportRecord current = records.get(exportId);
            if (current == null || "create".equalsIgnoreCase(operation)) {
                records.put(exportId, new ExportRecord(exportId, company, tyres, cust, comp, service,
                        total, paid, balance, paymentStatus, date, status));
            } else {
                current = mergeExport(current, company, tyres, cust, comp, service, total, paid, balance,
                        paymentStatus, date, status);
                records.put(exportId, current);
            }
        }
        return new ArrayList<>(records.values());
    }

    private List<JsonObject> loadPayloads(String entityType) {
        List<JsonObject> payloads = new ArrayList<>();
        String sql = "SELECT payload FROM sync_queue WHERE entity_type = ? ORDER BY created_at";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, entityType);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                String payload = rs.getString("payload");
                JsonElement element = JsonParser.parseString(payload == null ? "{}" : payload);
                if (element.isJsonObject()) {
                    payloads.add(element.getAsJsonObject());
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read sync payloads", ex);
        }
        return payloads;
    }

    private String getText(JsonObject obj, String key, String fallback) {
        JsonElement el = obj.get(key);
        return el != null && !el.isJsonNull() ? el.getAsString() : fallback;
    }

    private double getNumber(JsonObject obj, String key, double fallback) {
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) return fallback;
        try {
            return el.getAsDouble();
        } catch (Exception ex) {
            return fallback;
        }
    }

    private int getArraySize(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull() || !el.isJsonArray()) return 0;
        JsonArray array = el.getAsJsonArray();
        return array.size();
    }

    private String resolveExportKey(JsonObject payload) {
        return getText(payload, "company", "Company") + "|"
                + getText(payload, "date", LocalDate.now().toString()) + "|"
                + (int) getNumber(payload, "tyres", 0.0) + "|"
                + getNumber(payload, "custPrice", 0.0) + "|"
                + getNumber(payload, "compPrice", 0.0) + "|"
                + getNumber(payload, "serviceFee", 0.0);
    }

    private String paymentStatusFor(double balance, double paid) {
        if (balance <= 0.0 && paid > 0.0) return "PAID";
        if (paid > 0.0) return "PARTIAL";
        return "CREDIT";
    }

    private ExportRecord mergeExport(ExportRecord record,
                                     String company,
                                     int tyres,
                                     double cust,
                                     double comp,
                                     double service,
                                     double total,
                                     double paid,
                                     double balance,
                                     String paymentStatus,
                                     LocalDate date,
                                     String status) {
        record.setExportId(record.getExportId().isBlank() ? resolveExportKeyFromFields(company, date, tyres, cust, comp, service) : record.getExportId());
        record.companyProperty().set(company);
        record.tyresProperty().set(tyres);
        record.serviceChargeProperty().set(service);
        record.setPaidAmount(paid);
        record.setTotalAmount(total);
        record.setBalanceAmount(balance);
        record.setPaymentStatus(paymentStatus);
        record.setStatus(status);
        return record;
    }

    private String resolveExportKeyFromFields(String company, LocalDate date, int tyres, double cust, double comp, double service) {
        return company + "|" + date + "|" + tyres + "|" + cust + "|" + comp + "|" + service;
    }
}

