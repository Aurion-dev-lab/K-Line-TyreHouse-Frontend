package com.gui.kline.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gui.kline.data.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Performs the cloud-to-local database restore operation.
 *
 * <p>Execution runs inside a single atomic JDBC transaction. If any error occurs,
 * the entire local state rolls back automatically.
 */
public class LocalRestoreService {

    private final Consumer<String> logger;

    public LocalRestoreService(Consumer<String> logger) {
        this.logger = logger;
    }

    private void log(String msg) {
        if (logger != null) logger.accept(msg);
    }

    /**
     * Parses the cloud database snapshot, purges local tables, and restores
     * data in FK-safe insertion order.
     */
    public void wipeAndRestore(String snapshotJson) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        JsonNode root = mapper.readTree(snapshotJson);
        JsonNode data = root.path("data");

        if (data.isMissingNode() || data.isNull()) {
            throw new RuntimeException("Snapshot payload is missing the 'data' field. Restore aborted.");
        }

        if (isEmpty(data)) {
            throw new RuntimeException("Cloud snapshot returned no records. Restore aborted to prevent data loss.");
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Step 1: Temporarily disable foreign key constraints
                exec(conn, "PRAGMA foreign_keys = OFF;");

                // Step 2: Wipe local tables in reverse dependency order
                log("Purging local tables...");
                String[] wipeOrder = {
                    "credit_sale_parts", "invoice_line_items",
                    "credit_sales", "invoices",
                    "worker_attendance", "salary_advances", "salary_payments",
                    "worker_credits", "tyre_exports", "quick_services", "services",
                    "customers", "workers", "products", "expenses", "quick_service_presets",
                    "sync_tombstones"
                };
                for (String table : wipeOrder) {
                    exec(conn, "DELETE FROM " + table);
                    log("  Wiped: " + table);
                }

                // Step 3: Re-enable foreign key constraints
                exec(conn, "PRAGMA foreign_keys = ON;");

                // Step 4: Populate tables from snapshot in FK-safe order
                log("Restoring data from cloud snapshot...");

                restoreProducts(conn,              getNode(data, "products", null));
                restoreCustomers(conn,             getNode(data, "customers", null));
                restoreWorkers(conn,               getNode(data, "workers", null));
                restoreExpenses(conn,              getNode(data, "expenses", null));
                restoreQuickServicePresets(conn,   getNode(data, "quickServicePresets", "quick_service_presets"));

                restoreInvoices(conn,              getNode(data, "invoices", null));
                restoreCreditSales(conn,           getNode(data, "creditSales", "credit_sales"));
                restoreServices(conn,              getNode(data, "services", null));
                restoreTyreExports(conn,           getNode(data, "tyreExports", "tyre_exports"));
                restoreWorkerAttendance(conn,      getNode(data, "workerAttendance", "worker_attendance"));
                restoreSalaryAdvances(conn,        getNode(data, "salaryAdvances", "salary_advances"));
                restoreSalaryPayments(conn,        getNode(data, "salaryPayments", "salary_payments"));
                restoreWorkerCredits(conn,         getNode(data, "workerCredits", "worker_credits"));
                restoreQuickServices(conn,         getNode(data, "quickServices", "quick_services"));

                restoreInvoiceLineItems(conn,      getNode(data, "invoiceLineItems", "invoice_line_items"), getNode(data, "invoices", null));
                restoreCreditSaleParts(conn,       getNode(data, "creditSaleParts", "credit_sale_parts"),  getNode(data, "creditSales", "credit_sales"));

                conn.commit();
                log("All tables restored successfully from cloud snapshot.");

            } catch (Exception ex) {
                conn.rollback();
                exec(conn, "PRAGMA foreign_keys = ON;");
                throw ex;
            }
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private JsonNode getNode(JsonNode data, String camelCaseKey, String snakeCaseKey) {
        JsonNode n = data.path(camelCaseKey);
        if ((n.isMissingNode() || n.isNull() || !n.isArray() || n.size() == 0) && snakeCaseKey != null) {
            JsonNode alt = data.path(snakeCaseKey);
            if (!alt.isMissingNode() && !alt.isNull() && alt.isArray()) {
                return alt;
            }
        }
        return n;
    }

    private void exec(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.execute();
        }
    }

    private boolean isEmpty(JsonNode data) {
        String[] keys = {"products", "customers", "workers", "invoices", "creditSales", "credit_sales", "expenses"};
        for (String k : keys) {
            JsonNode node = data.path(k);
            if (!node.isMissingNode() && node.isArray() && node.size() > 0) return false;
        }
        return true;
    }

    private static String camelToSnake(String field) {
        if (field == null) return null;
        StringBuilder sb = new StringBuilder();
        for (char c : field.toCharArray()) {
            if (Character.isUpperCase(c)) {
                sb.append('_').append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String str(JsonNode n, String field) {
        if (n == null) return null;
        JsonNode v = n.path(field);
        if (v.isNull() || v.isMissingNode()) {
            String snake = camelToSnake(field);
            if (!snake.equals(field)) {
                v = n.path(snake);
            }
        }
        return v.isNull() || v.isMissingNode() ? null : v.asText();
    }

    private Double dbl(JsonNode n, String field) {
        if (n == null) return null;
        JsonNode v = n.path(field);
        if (v.isNull() || v.isMissingNode()) {
            String snake = camelToSnake(field);
            if (!snake.equals(field)) {
                v = n.path(snake);
            }
        }
        return v.isNull() || v.isMissingNode() ? null : v.asDouble();
    }

    private Integer num(JsonNode n, String field) {
        if (n == null) return null;
        JsonNode v = n.path(field);
        if (v.isNull() || v.isMissingNode()) {
            String snake = camelToSnake(field);
            if (!snake.equals(field)) {
                v = n.path(snake);
            }
        }
        return v.isNull() || v.isMissingNode() ? null : v.asInt();
    }

    private boolean bool(JsonNode n, String field, boolean def) {
        if (n == null) return def;
        JsonNode v = n.path(field);
        if (v.isNull() || v.isMissingNode()) {
            String snake = camelToSnake(field);
            if (!snake.equals(field)) {
                v = n.path(snake);
            }
        }
        return v.isNull() || v.isMissingNode() ? def : v.asBoolean();
    }

    // ── Entity Restoration Methods ─────────────────────────────────────────

    private void restoreProducts(Connection conn, JsonNode arr) throws SQLException {
        int count = (arr != null && arr.isArray()) ? arr.size() : 0;
        if (count > 0) {
            String sql = "INSERT OR IGNORE INTO products " +
                    "(id, product_code, name, category, buy_price, sell_price, stock, minimum_stock_alert, " +
                    "brand, description, vehicle_type, material, supplier_name, created_at, updated_at, sync_status) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,1)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (JsonNode r : arr) {
                    ps.setString(1,  str(r, "id"));
                    ps.setString(2,  str(r, "productCode"));
                    ps.setString(3,  str(r, "name"));
                    ps.setString(4,  str(r, "category"));
                    ps.setObject(5,  dbl(r, "buyPrice"));
                    ps.setObject(6,  dbl(r, "sellPrice"));
                    ps.setObject(7,  num(r, "stock"));
                    ps.setObject(8,  num(r, "minimumStockAlert"));
                    ps.setString(9,  str(r, "brand"));
                    ps.setString(10, str(r, "description"));
                    ps.setString(11, str(r, "vehicleType"));
                    ps.setString(12, str(r, "material"));
                    ps.setString(13, str(r, "supplierName"));
                    ps.setString(14, str(r, "createdAt"));
                    ps.setString(15, str(r, "updatedAt"));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
        log("  Restored: products (" + count + " rows)");
    }

    private void restoreCustomers(Connection conn, JsonNode arr) throws SQLException {
        int count = (arr != null && arr.isArray()) ? arr.size() : 0;
        if (count > 0) {
            String sql = "INSERT OR IGNORE INTO customers (id, name, phone, created_at, sync_status) VALUES (?,?,?,?,1)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (JsonNode r : arr) {
                    ps.setString(1, str(r, "id"));
                    ps.setString(2, str(r, "name"));
                    ps.setString(3, str(r, "phone"));
                    ps.setString(4, str(r, "createdAt"));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
        log("  Restored: customers (" + count + " rows)");
    }

    private void restoreWorkers(Connection conn, JsonNode arr) throws SQLException {
        int count = (arr != null && arr.isArray()) ? arr.size() : 0;
        if (count > 0) {
            String sql = "INSERT OR IGNORE INTO workers (id, name, phone, role, rate, created_at, salary_type, sync_status) VALUES (?,?,?,?,?,?,?,1)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (JsonNode r : arr) {
                    ps.setString(1, str(r, "id"));
                    ps.setString(2, str(r, "name"));
                    ps.setString(3, str(r, "phone"));
                    ps.setString(4, str(r, "role"));
                    ps.setString(5, str(r, "rate"));
                    ps.setString(6, str(r, "createdAt"));
                    ps.setString(7, str(r, "salaryType"));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
        log("  Restored: workers (" + count + " rows)");
    }

    private void restoreExpenses(Connection conn, JsonNode arr) throws SQLException {
        int count = (arr != null && arr.isArray()) ? arr.size() : 0;
        if (count > 0) {
            String sql = "INSERT OR IGNORE INTO expenses (id, expense_date, description, category, amount, created_at, sync_status) VALUES (?,?,?,?,?,?,1)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (JsonNode r : arr) {
                    ps.setString(1, str(r, "id"));
                    ps.setString(2, str(r, "expenseDate"));
                    ps.setString(3, str(r, "description"));
                    ps.setString(4, str(r, "category"));
                    ps.setObject(5, dbl(r, "amount"));
                    ps.setString(6, str(r, "createdAt"));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
        log("  Restored: expenses (" + count + " rows)");
    }

    private void restoreQuickServicePresets(Connection conn, JsonNode arr) throws SQLException {
        int count = (arr != null && arr.isArray()) ? arr.size() : 0;
        if (count > 0) {
            String sql = "INSERT OR IGNORE INTO quick_service_presets (id, service, price, active, icon, created_at, sync_status) VALUES (?,?,?,?,?,?,1)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (JsonNode r : arr) {
                    ps.setString(1, str(r, "id"));
                    ps.setString(2, str(r, "service"));
                    ps.setObject(3, dbl(r, "price"));
                    ps.setInt(4, bool(r, "active", true) ? 1 : 0);
                    ps.setString(5, str(r, "icon"));
                    ps.setString(6, str(r, "createdAt"));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
        log("  Restored: quick_service_presets (" + count + " rows)");
    }

    private void restoreInvoices(Connection conn, JsonNode arr) throws SQLException {
        int count = (arr != null && arr.isArray()) ? arr.size() : 0;
        if (count > 0) {
            String sql = "INSERT OR IGNORE INTO invoices " +
                    "(id, invoice_id, customer, invoice_date, type, status, subtotal, tax, grand_total, created_at, updated_at, sync_status) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?,1)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (JsonNode r : arr) {
                    ps.setString(1,  str(r, "id"));
                    ps.setString(2,  str(r, "invoiceId"));
                    ps.setString(3,  str(r, "customer"));
                    ps.setString(4,  str(r, "invoiceDate"));
                    ps.setString(5,  str(r, "type"));
                    ps.setString(6,  str(r, "status"));
                    ps.setObject(7,  dbl(r, "subtotal"));
                    ps.setObject(8,  dbl(r, "tax"));
                    ps.setObject(9,  dbl(r, "grandTotal"));
                    ps.setString(10, str(r, "createdAt"));
                    ps.setString(11, str(r, "updatedAt"));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
        log("  Restored: invoices (" + count + " rows)");
    }

    private void restoreCreditSales(Connection conn, JsonNode arr) throws SQLException {
        int count = (arr != null && arr.isArray()) ? arr.size() : 0;
        if (count > 0) {
            String sql = "INSERT OR IGNORE INTO credit_sales " +
                    "(id, credit_id, customer, customer_name, sale_date, due_date, subtotal, paid_amount, amount, status, created_at, updated_at, sync_status) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,1)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (JsonNode r : arr) {
                    ps.setString(1,  str(r, "id"));
                    ps.setString(2,  str(r, "creditId"));
                    ps.setString(3,  str(r, "customer"));
                    ps.setString(4,  str(r, "customerName"));
                    ps.setString(5,  str(r, "saleDate"));
                    ps.setString(6,  str(r, "dueDate"));
                    ps.setObject(7,  dbl(r, "subtotal"));
                    ps.setObject(8,  dbl(r, "paidAmount"));
                    ps.setObject(9,  dbl(r, "amount"));
                    ps.setString(10, str(r, "status"));
                    ps.setString(11, str(r, "createdAt"));
                    ps.setString(12, str(r, "updatedAt"));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
        log("  Restored: credit_sales (" + count + " rows)");
    }

    private void restoreServices(Connection conn, JsonNode arr) throws SQLException {
        int count = (arr != null && arr.isArray()) ? arr.size() : 0;
        if (count > 0) {
            String sql = "INSERT OR IGNORE INTO services (id, name, price, service_date, remark, sync_status) VALUES (?,?,?,?,?,1)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (JsonNode r : arr) {
                    ps.setString(1, str(r, "id"));
                    ps.setString(2, str(r, "name"));
                    ps.setObject(3, dbl(r, "price"));
                    ps.setString(4, str(r, "serviceDate"));
                    ps.setString(5, str(r, "remark"));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
        log("  Restored: services (" + count + " rows)");
    }

    private void restoreTyreExports(Connection conn, JsonNode arr) throws SQLException {
        int count = (arr != null && arr.isArray()) ? arr.size() : 0;
        if (count > 0) {
            String sql = "INSERT OR IGNORE INTO tyre_exports " +
                    "(id, export_id, operation, company, tyres, cust_price, comp_price, service_fee, paid_amount, total_amount, balance_amount, payment_status, status, export_date, updated_at, sync_status) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,1)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (JsonNode r : arr) {
                    ps.setString(1,  str(r, "id"));
                    ps.setString(2,  str(r, "exportId"));
                    ps.setString(3,  str(r, "operation"));
                    ps.setString(4,  str(r, "company"));
                    ps.setObject(5,  num(r, "tyres"));
                    ps.setObject(6,  dbl(r, "custPrice"));
                    ps.setObject(7,  dbl(r, "compPrice"));
                    ps.setObject(8,  dbl(r, "serviceFee"));
                    ps.setObject(9,  dbl(r, "paidAmount"));
                    ps.setObject(10, dbl(r, "totalAmount"));
                    ps.setObject(11, dbl(r, "balanceAmount"));
                    ps.setString(12, str(r, "paymentStatus"));
                    ps.setString(13, str(r, "status"));
                    ps.setString(14, str(r, "exportDate"));
                    ps.setString(15, str(r, "updatedAt"));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
        log("  Restored: tyre_exports (" + count + " rows)");
    }

    private void restoreWorkerAttendance(Connection conn, JsonNode arr) throws SQLException {
        int count = (arr != null && arr.isArray()) ? arr.size() : 0;
        if (count > 0) {
            String sql = "INSERT OR IGNORE INTO worker_attendance (id, worker_id, attendance_date, status, created_at, updated_at, sync_status) VALUES (?,?,?,?,?,?,1)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (JsonNode r : arr) {
                    ps.setString(1, str(r, "id"));
                    ps.setString(2, str(r, "workerId"));
                    ps.setString(3, str(r, "attendanceDate"));
                    ps.setString(4, str(r, "status"));
                    ps.setString(5, str(r, "createdAt"));
                    ps.setString(6, str(r, "updatedAt"));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
        log("  Restored: worker_attendance (" + count + " rows)");
    }

    private void restoreSalaryAdvances(Connection conn, JsonNode arr) throws SQLException {
        int count = (arr != null && arr.isArray()) ? arr.size() : 0;
        if (count > 0) {
            String sql = "INSERT OR IGNORE INTO salary_advances (id, worker, worker_id, amount, advance_date, note, created_at, sync_status) VALUES (?,?,?,?,?,?,?,1)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (JsonNode r : arr) {
                    ps.setString(1, str(r, "id"));
                    ps.setString(2, str(r, "worker"));
                    ps.setString(3, str(r, "workerId"));
                    ps.setObject(4, dbl(r, "amount"));
                    ps.setString(5, str(r, "advanceDate"));
                    ps.setString(6, str(r, "note"));
                    ps.setString(7, str(r, "createdAt"));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
        log("  Restored: salary_advances (" + count + " rows)");
    }

    private void restoreSalaryPayments(Connection conn, JsonNode arr) throws SQLException {
        int count = (arr != null && arr.isArray()) ? arr.size() : 0;
        if (count > 0) {
            String sql = "INSERT OR IGNORE INTO salary_payments (id, worker_id, worker, period_from, period_to, amount, paid_at, sync_status) VALUES (?,?,?,?,?,?,?,1)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (JsonNode r : arr) {
                    ps.setString(1, str(r, "id"));
                    ps.setString(2, str(r, "workerId"));
                    ps.setString(3, str(r, "worker"));
                    ps.setString(4, str(r, "periodFrom"));
                    ps.setString(5, str(r, "periodTo"));
                    ps.setObject(6, dbl(r, "amount"));
                    ps.setString(7, str(r, "paidAt"));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
        log("  Restored: salary_payments (" + count + " rows)");
    }

    private void restoreWorkerCredits(Connection conn, JsonNode arr) throws SQLException {
        int count = (arr != null && arr.isArray()) ? arr.size() : 0;
        if (count > 0) {
            String sql = "INSERT OR IGNORE INTO worker_credits (id, worker, worker_id, amount, credit_type, credit_date, note, created_at, sync_status) VALUES (?,?,?,?,?,?,?,?,1)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (JsonNode r : arr) {
                    ps.setString(1, str(r, "id"));
                    ps.setString(2, str(r, "worker"));
                    ps.setString(3, str(r, "workerId"));
                    ps.setObject(4, dbl(r, "amount"));
                    ps.setString(5, str(r, "creditType"));
                    ps.setString(6, str(r, "creditDate"));
                    ps.setString(7, str(r, "note"));
                    ps.setString(8, str(r, "createdAt"));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
        log("  Restored: worker_credits (" + count + " rows)");
    }

    private void restoreQuickServices(Connection conn, JsonNode arr) throws SQLException {
        int count = (arr != null && arr.isArray()) ? arr.size() : 0;
        if (count > 0) {
            String sql = "INSERT OR IGNORE INTO quick_services (id, service, price, service_date, sync_status) VALUES (?,?,?,?,1)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (JsonNode r : arr) {
                    ps.setString(1, str(r, "id"));
                    ps.setString(2, str(r, "service"));
                    ps.setObject(3, dbl(r, "price"));
                    ps.setString(4, str(r, "serviceDate"));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
        log("  Restored: quick_services (" + count + " rows)");
    }

    private void restoreInvoiceLineItems(Connection conn, JsonNode lineItemsArr, JsonNode invoicesArr) throws SQLException {
        List<JsonNode> allItems = new ArrayList<>();
        Map<String, String> itemToParentInvoiceId = new HashMap<>();

        if (lineItemsArr != null && lineItemsArr.isArray()) {
            for (JsonNode item : lineItemsArr) {
                allItems.add(item);
            }
        }

        if (invoicesArr != null && invoicesArr.isArray()) {
            for (JsonNode inv : invoicesArr) {
                String parentUuid = str(inv, "id");
                JsonNode nestedItems = inv.path("lineItems");
                if (!nestedItems.isMissingNode() && nestedItems.isArray()) {
                    for (JsonNode item : nestedItems) {
                        allItems.add(item);
                        if (parentUuid != null) {
                            String itemId = str(item, "id");
                            if (itemId != null) {
                                itemToParentInvoiceId.put(itemId, parentUuid);
                            }
                        }
                    }
                }
            }
        }

        if (!allItems.isEmpty()) {
            String sql = "INSERT OR IGNORE INTO invoice_line_items " +
                    "(id, invoice_id, invoice_ref, product_id, description, type, qty, unit_price, total, created_at, sync_status) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?,1)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (JsonNode r : allItems) {
                    String itemId = str(r, "id");
                    String invoiceRefId = str(r, "invoiceRefId");
                    if (invoiceRefId == null) {
                        JsonNode refNode = r.path("invoiceRef");
                        if (!refNode.isMissingNode() && !refNode.isNull()) {
                            invoiceRefId = str(refNode, "id");
                        }
                    }
                    if (invoiceRefId == null) {
                        invoiceRefId = str(r, "invoice_ref");
                    }
                    if (invoiceRefId == null && itemId != null) {
                        invoiceRefId = itemToParentInvoiceId.get(itemId);
                    }

                    ps.setString(1,  itemId);
                    ps.setString(2,  str(r, "invoiceId"));
                    ps.setString(3,  invoiceRefId);
                    ps.setString(4,  str(r, "productId"));
                    ps.setString(5,  str(r, "description"));
                    ps.setString(6,  str(r, "type"));
                    ps.setObject(7,  num(r, "qty"));
                    ps.setObject(8,  dbl(r, "unitPrice"));
                    ps.setObject(9,  dbl(r, "total"));
                    ps.setString(10, str(r, "createdAt"));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
        log("  Restored: invoice_line_items (" + allItems.size() + " rows)");
    }

    private void restoreCreditSaleParts(Connection conn, JsonNode partsArr, JsonNode creditSalesArr) throws SQLException {
        List<JsonNode> allParts = new ArrayList<>();
        Map<String, String> partToParentCreditSaleId = new HashMap<>();

        if (partsArr != null && partsArr.isArray()) {
            for (JsonNode part : partsArr) {
                allParts.add(part);
            }
        }

        if (creditSalesArr != null && creditSalesArr.isArray()) {
            for (JsonNode cs : creditSalesArr) {
                String parentUuid = str(cs, "id");
                JsonNode nestedParts = cs.path("parts");
                if (!nestedParts.isMissingNode() && nestedParts.isArray()) {
                    for (JsonNode part : nestedParts) {
                        allParts.add(part);
                        if (parentUuid != null) {
                            String partId = str(part, "id");
                            if (partId != null) {
                                partToParentCreditSaleId.put(partId, parentUuid);
                            }
                        }
                    }
                }
            }
        }

        if (!allParts.isEmpty()) {
            String sql = "INSERT OR IGNORE INTO credit_sale_parts " +
                    "(id, credit_sale_id, product_id, description, quantity, unit_price, total, created_at, sync_status) " +
                    "VALUES (?,?,?,?,?,?,?,?,1)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (JsonNode r : allParts) {
                    String partId = str(r, "id");
                    String creditSaleId = str(r, "creditSaleId");
                    if (creditSaleId == null) {
                        JsonNode csNode = r.path("creditSale");
                        if (!csNode.isMissingNode() && !csNode.isNull()) {
                            creditSaleId = str(csNode, "id");
                        }
                    }
                    if (creditSaleId == null) {
                        creditSaleId = str(r, "credit_sale_id");
                    }
                    if (creditSaleId == null && partId != null) {
                        creditSaleId = partToParentCreditSaleId.get(partId);
                    }

                    ps.setString(1, partId);
                    ps.setString(2, creditSaleId);
                    ps.setString(3, str(r, "productId"));
                    ps.setString(4, str(r, "description"));
                    ps.setObject(5, num(r, "quantity"));
                    ps.setObject(6, dbl(r, "unitPrice"));
                    ps.setObject(7, dbl(r, "total"));
                    ps.setString(8, str(r, "createdAt"));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
        log("  Restored: credit_sale_parts (" + allParts.size() + " rows)");
    }
}
