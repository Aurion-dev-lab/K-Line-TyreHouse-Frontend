package com.gui.kline.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.gui.kline.data.DatabaseManager;
import com.gui.kline.data.LocalCatalogRepository;
import com.gui.kline.utils.Utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

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
                    "credit_payments", "tyre_export_payments",
                    "credit_sales", "invoices",
                    "worker_attendance", "salary_advances", "salary_payments",
                    "worker_credits", "tyre_exports", "quick_services", "services", "credit_customers", "workers", "products", "expenses", "quick_service_presets",
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

                restoreProducts(conn,              getNode(data, "products"));
                restoreCustomers(conn,             getNode(data, "creditCustomers", "credit_customers", "customers"));
                restoreWorkers(conn,               getNode(data, "workers"));
                restoreExpenses(conn,              getNode(data, "expenses"));
                restoreQuickServicePresets(conn,   getNode(data, "quickServicePresets", "quick_service_presets"));

                restoreInvoices(conn,              getNode(data, "invoices"));
                restoreCreditSales(conn,           getNode(data, "creditSales", "credit_sales"));
                restoreCreditPayments(conn,        getNode(data, "creditPayments", "credit_payments"));
                restoreServices(conn,              getNode(data, "services"));
                restoreTyreExports(conn,           getNode(data, "tyreExports", "tyre_exports"));
                restoreTyreExportPayments(conn,    getNode(data, "tyreExportPayments", "tyre_export_payments"));
                restoreWorkerAttendance(conn,      getNode(data, "workerAttendance", "worker_attendance"));
                restoreSalaryAdvances(conn,        getNode(data, "salaryAdvances", "salary_advances"));
                restoreSalaryPayments(conn,        getNode(data, "salaryPayments", "salary_payments"));
                restoreWorkerCredits(conn,         getNode(data, "workerCredits", "worker_credits"));
                restoreQuickServices(conn,         getNode(data, "quickServices", "quick_services"));

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

    private JsonNode getNode(JsonNode data, String primaryKey, String... alternateKeys) {
        JsonNode n = data.path(primaryKey);
        if (!n.isMissingNode() && !n.isNull() && n.isArray() && n.size() > 0) {
            return n;
        }
        if (alternateKeys != null) {
            for (String altKey : alternateKeys) {
                if (altKey != null) {
                    JsonNode alt = data.path(altKey);
                    if (!alt.isMissingNode() && !alt.isNull() && alt.isArray() && alt.size() > 0) {
                        return alt;
                    }
                }
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
        String[] keys = {"products", "creditCustomers", "credit_customers", "workers", "invoices", "creditSales", "credit_sales", "creditPayments", "credit_payments", "expenses", "tyreExports", "tyre_exports", "tyreExportPayments", "tyre_export_payments"};
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

    private double dblVal(JsonNode n, String field, double def) {
        Double val = dbl(n, field);
        return val != null ? val : def;
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

    private int numVal(JsonNode n, String field, int def) {
        Integer val = num(n, field);
        return val != null ? val : def;
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
                    "brand, description, vehicle_type, material, supplier_name, image_paths, created_at, updated_at, sync_status) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,1)";
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
                    ps.setString(14, str(r, "imagePaths"));
                    ps.setString(15, str(r, "createdAt"));
                    ps.setString(16, str(r, "updatedAt"));
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
            String sql = "INSERT OR IGNORE INTO credit_customers (id, name, phone, email, address, created_at, updated_at, sync_status) VALUES (?,?,?,?,?,?,?,1)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (JsonNode r : arr) {
                    ps.setString(1, str(r, "id"));
                    ps.setString(2, str(r, "name"));
                    ps.setString(3, str(r, "phone"));
                    ps.setString(4, str(r, "email"));
                    ps.setString(5, str(r, "address"));
                    ps.setString(6, str(r, "createdAt"));
                    ps.setString(7, str(r, "updatedAt"));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
        log("  Restored: credit_customers (" + count + " rows)");
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
                    "(id, invoice_id, customer, phone, description, vehicle_number, invoice_date, type, status, subtotal, grand_total, line_items, created_at, updated_at, sync_status) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,1)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (JsonNode r : arr) {
                    ps.setString(1,  str(r, "id"));
                    ps.setString(2,  str(r, "invoiceId"));
                    ps.setString(3,  str(r, "customer"));
                    ps.setString(4,  str(r, "phone"));
                    ps.setString(5,  str(r, "description"));
                    ps.setString(6,  str(r, "vehicleNumber"));
                    ps.setString(7,  str(r, "invoiceDate"));
                    ps.setString(8,  str(r, "type"));
                    ps.setString(9,  str(r, "status"));
                    ps.setObject(10, dbl(r, "subtotal"));
                    ps.setObject(11, dbl(r, "grandTotal"));
                    ps.setString(12, str(r, "lineItems"));
                    ps.setString(13, str(r, "createdAt"));
                    ps.setString(14, str(r, "updatedAt"));
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
            LocalCatalogRepository catalogRepository = new LocalCatalogRepository();
            String sql = "INSERT OR IGNORE INTO credit_sales " +
                    "(id, credit_id, customer_id, sale_date, due_date, sub_total, grand_total, settlement, status, parts, created_at, updated_at, sync_status) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,1)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (JsonNode r : arr) {
                    ps.setString(1,  str(r, "id"));
                    ps.setString(2,  str(r, "creditId"));
                    
                    String customerId = str(r, "customerId");
                    if (customerId == null || customerId.isBlank()) {
                        String customerName = str(r, "customerName");
                        if (customerName == null || customerName.isBlank()) {
                            customerName = str(r, "customer");
                        }
                        if (customerName != null && !customerName.isBlank()) {
                            customerId = catalogRepository.getCustomerIdByName(customerName);
                            if (customerId == null) {
                                customerId = Utils.generateId("CST-", 8);
                                String insCustomer = "INSERT OR IGNORE INTO credit_customers (id, name, created_at) VALUES (?, ?, strftime('%Y-%m-%dT%H:%M:%S', 'now'))";
                                try (PreparedStatement ins = conn.prepareStatement(insCustomer)) {
                                    ins.setString(1, customerId);
                                    ins.setString(2, customerName.trim());
                                    ins.executeUpdate();
                                }
                            }
                        }
                    }
                    ps.setString(3,  customerId);
                    ps.setString(4,  str(r, "saleDate"));
                    ps.setString(5,  str(r, "dueDate"));
                    
                    double subTotal = dblVal(r, "subTotal", dblVal(r, "sub_total", 0.0));
                    double grandTotal = dblVal(r, "grandTotal", dblVal(r, "grand_total", dblVal(r, "amount", 0.0)));
                    if (subTotal == 0.0) {
                        subTotal = grandTotal;
                    }
                    double settlement = dblVal(r, "settlement", dblVal(r, "paidAmount", dblVal(r, "paid_amount", 0.0)));
                    
                    ps.setDouble(6,  subTotal);
                    ps.setDouble(7,  grandTotal);
                    ps.setDouble(8,  settlement);
                    ps.setString(9,  str(r, "status"));
                    ps.setString(10, str(r, "parts"));
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
            String sql = "INSERT OR IGNORE INTO services (id, invoice_id, name, price, service_date, remark, sync_status) VALUES (?,?,?,?,?,?,1)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (JsonNode r : arr) {
                    ps.setString(1, str(r, "id"));
                    ps.setString(2, str(r, "invoiceId"));
                    ps.setString(3, str(r, "name"));
                    ps.setObject(4, dbl(r, "price"));
                    ps.setString(5, str(r, "serviceDate"));
                    ps.setString(6, str(r, "remark"));
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
                    "(id, export_id, serial_number, company, tyre_size, tyre_make, tyres, cust_price, comp_price, service_fee, sub_total, grand_total, initial_payment, settlement, status, export_date, remark, created_at, updated_at, sync_status) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,1)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (JsonNode r : arr) {
                    double custPrice = dblVal(r, "custPrice", dblVal(r, "cust_price", 0.0));
                    int tyres = numVal(r, "tyres", 0);
                    double serviceFee = dblVal(r, "serviceFee", dblVal(r, "service_fee", 0.0));
                    double paidAmount = dblVal(r, "paidAmount", dblVal(r, "initialPayment", dblVal(r, "settlement", 0.0)));
                    double subTotal = dblVal(r, "subTotal", dblVal(r, "sub_total", custPrice * tyres));
                    double grandTotal = dblVal(r, "grandTotal", dblVal(r, "grand_total", subTotal + serviceFee));

                    ps.setString(1,  str(r, "id"));
                    ps.setString(2,  str(r, "exportId"));
                    ps.setString(3,  str(r, "serialNumber"));
                    ps.setString(4,  str(r, "company"));
                    ps.setString(5,  str(r, "tyreSize"));
                    ps.setString(6,  str(r, "tyreMake"));
                    ps.setInt(7,     tyres);
                    ps.setDouble(8,  custPrice);
                    ps.setDouble(9,  dblVal(r, "compPrice", dblVal(r, "comp_price", 0.0)));
                    ps.setDouble(10, serviceFee);
                    ps.setDouble(11, subTotal);
                    ps.setDouble(12, grandTotal);
                    ps.setDouble(13, paidAmount);
                    ps.setDouble(14, dblVal(r, "settlement", paidAmount));
                    ps.setString(15, str(r, "status"));
                    ps.setString(16, str(r, "exportDate"));
                    ps.setString(17, str(r, "remark") != null ? str(r, "remark") : str(r, "notes"));
                    ps.setString(18, str(r, "createdAt"));
                    ps.setString(19, str(r, "updatedAt"));
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

    private void restoreCreditPayments(Connection conn, JsonNode arr) throws SQLException {
        int count = (arr != null && arr.isArray()) ? arr.size() : 0;
        if (count > 0) {
            String sql = "INSERT OR IGNORE INTO credit_payments (id, credit_id, customer_id, payment_date, amount, payment_method, notes, created_at, sync_status) VALUES (?,?,?,?,?,?,?,?,1)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (JsonNode r : arr) {
                    ps.setString(1, str(r, "id"));
                    ps.setString(2, str(r, "creditId"));
                    ps.setString(3, str(r, "customerId"));
                    ps.setString(4, str(r, "paymentDate"));
                    ps.setObject(5, dbl(r, "amount"));
                    ps.setString(6, str(r, "paymentMethod"));
                    ps.setString(7, str(r, "notes"));
                    ps.setString(8, str(r, "createdAt"));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
        log("  Restored: credit_payments (" + count + " rows)");
    }

    private void restoreTyreExportPayments(Connection conn, JsonNode arr) throws SQLException {
        int count = (arr != null && arr.isArray()) ? arr.size() : 0;
        if (count > 0) {
            String sql = "INSERT OR IGNORE INTO tyre_export_payments (id, export_id, company, payment_date, amount, payment_method, notes, created_at, sync_status) VALUES (?,?,?,?,?,?,?,?,1)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (JsonNode r : arr) {
                    ps.setString(1, str(r, "id"));
                    ps.setString(2, str(r, "exportId"));
                    ps.setString(3, str(r, "company"));
                    ps.setString(4, str(r, "paymentDate"));
                    ps.setObject(5, dbl(r, "amount"));
                    ps.setString(6, str(r, "paymentMethod"));
                    ps.setString(7, str(r, "notes"));
                    ps.setString(8, str(r, "createdAt"));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
        log("  Restored: tyre_export_payments (" + count + " rows)");
    }
}
