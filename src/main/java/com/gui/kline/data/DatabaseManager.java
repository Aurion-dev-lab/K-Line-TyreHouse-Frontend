package com.gui.kline.data;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class DatabaseManager {

    private static final String DEFAULT_DB_PATH = "kline.sqlite";
    private static volatile boolean initialized = false;

    private DatabaseManager() {
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        try {
            // Test connection first
            testDatabaseConnection();

            try (Connection connection = getConnection();
                 Statement statement = connection.createStatement()) {

                // Enable WAL mode for better concurrency
                statement.execute("PRAGMA journal_mode=WAL");
                // Enable foreign key enforcement
                statement.execute("PRAGMA foreign_keys=ON");

                // ── Core tables ──────────────────────────────────────────
                statement.execute("CREATE TABLE IF NOT EXISTS app_sync_state (" +
                        "id INTEGER PRIMARY KEY," +
                        "device_id TEXT NOT NULL," +
                        "last_sync_at TEXT" +
                        ")");
                statement.execute("CREATE TABLE IF NOT EXISTS sync_queue (" +
                        "id TEXT PRIMARY KEY," +
                        "entity_type TEXT NOT NULL," +
                        "payload TEXT NOT NULL," +
                        "status TEXT NOT NULL," +
                        "created_at TEXT NOT NULL," +
                        "last_error TEXT" +
                        ")");
                statement.execute("CREATE TABLE IF NOT EXISTS products (" +
                        "id TEXT PRIMARY KEY," +
                        "product_code TEXT," +
                        "name TEXT NOT NULL UNIQUE," +
                        "category TEXT," +
                        "buy_price REAL NOT NULL DEFAULT 0," +
                        "sell_price REAL NOT NULL DEFAULT 0," +
                        "stock INTEGER NOT NULL DEFAULT 0," +
                        "minimum_stock_alert INTEGER NOT NULL DEFAULT 5," +
                        "brand TEXT," +
                        "description TEXT," +
                        "vehicle_type TEXT," +
                        "material TEXT," +
                        "supplier_name TEXT," +
                        "image_path TEXT," +
                        "created_at TEXT NOT NULL," +
                        "updated_at TEXT NOT NULL," +
                        "sync_id TEXT," +
                        "device_id TEXT," +
                        "synced_at TEXT," +
                        "sync_status INTEGER DEFAULT 0" +
                        ")");
                statement.execute("CREATE TABLE IF NOT EXISTS product_images (" +
                        "id TEXT PRIMARY KEY," +
                        "product_id TEXT NOT NULL," +
                        "image_path TEXT NOT NULL," +
                        "created_at TEXT NOT NULL," +
                        "sync_id TEXT," +
                        "device_id TEXT," +
                        "synced_at TEXT," +
                        "sync_status INTEGER DEFAULT 0" +
                        ")");
                statement.execute("CREATE INDEX IF NOT EXISTS idx_product_images_product_id " +
                        "ON product_images (product_id)");

                statement.execute("CREATE TABLE IF NOT EXISTS customers (" +
                        "id TEXT PRIMARY KEY," +
                        "name TEXT NOT NULL," +
                        "phone TEXT," +
                        "company_name TEXT," +
                        "alternate_phone TEXT," +
                        "email TEXT," +
                        "address TEXT," +
                        "city TEXT," +
                        "state TEXT," +
                        "country TEXT," +
                        "postal_code TEXT," +
                        "tax_id TEXT," +
                        "category TEXT," +
                        "credit_limit REAL DEFAULT 0," +
                        "current_credit REAL DEFAULT 0," +
                        "active INTEGER DEFAULT 1," +
                        "date_of_birth TEXT," +
                        "notes TEXT," +
                        "loyalty_program_id TEXT," +
                        "loyalty_points REAL DEFAULT 0," +
                        "member_since TEXT," +
                        "created_at TEXT NOT NULL," +
                        "updated_at TEXT," +
                        "sync_id TEXT," +
                        "device_id TEXT," +
                        "synced_at TEXT," +
                        "sync_status INTEGER DEFAULT 0" +
                        ")");
                statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_customer_name_phone " +
                        "ON customers (name, phone)");

                statement.execute("CREATE TABLE IF NOT EXISTS invoices (" +
                        "id TEXT PRIMARY KEY," +
                        "invoice_id TEXT UNIQUE," +
                        "customer TEXT," +
                        "invoice_date TEXT," +
                        "type TEXT," +
                        "status TEXT," +
                        "subtotal REAL NOT NULL DEFAULT 0," +
                        "tax REAL NOT NULL DEFAULT 0," +
                        "grand_total REAL NOT NULL DEFAULT 0," +
                        "customer_name TEXT," +
                        "customer_phone TEXT," +
                        "payment_method TEXT," +
                        "payment_reference TEXT," +
                        "amount_paid REAL DEFAULT 0," +
                        "balance_due REAL DEFAULT 0," +
                        "notes TEXT," +
                        "terms_and_conditions TEXT," +
                        "created_by TEXT," +
                        "updated_by TEXT," +
                        "cancelled_at TEXT," +
                        "cancellation_reason TEXT," +
                        "discount REAL DEFAULT 0," +
                        "shipping REAL DEFAULT 0," +
                        "created_at TEXT NOT NULL," +
                        "updated_at TEXT," +
                        "sync_id TEXT," +
                        "device_id TEXT," +
                        "synced_at TEXT," +
                        "sync_status INTEGER DEFAULT 0" +
                        ")");
                statement.execute("CREATE TABLE IF NOT EXISTS invoice_line_items (" +
                        "id TEXT PRIMARY KEY," +
                        "invoice_id TEXT," +
                        "invoice_ref TEXT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE," +
                        "product_id TEXT," +
                        "description TEXT," +
                        "type TEXT," +
                        "qty INTEGER," +
                        "unit_price REAL," +
                        "total REAL," +
                        "created_at TEXT NOT NULL," +
                        "sync_id TEXT," +
                        "device_id TEXT," +
                        "synced_at TEXT," +
                        "sync_status INTEGER DEFAULT 0" +
                        ")");
                statement.execute("CREATE TABLE IF NOT EXISTS credit_sales (" +
                        "id TEXT PRIMARY KEY," +
                        "credit_id TEXT," +
                        "customer TEXT," +
                        "customer_name TEXT," +
                        "customer_id TEXT," +
                        "customer_phone TEXT," +
                        "sale_date TEXT," +
                        "due_date TEXT," +
                        "subtotal REAL NOT NULL DEFAULT 0," +
                        "tax REAL DEFAULT 0," +
                        "discount REAL DEFAULT 0," +
                        "grand_total REAL DEFAULT 0," +
                        "amount REAL NOT NULL DEFAULT 0," +
                        "paid_amount REAL NOT NULL DEFAULT 0," +
                        "balance_due REAL DEFAULT 0," +
                        "status TEXT," +
                        "notes TEXT," +
                        "terms_and_conditions TEXT," +
                        "payment_method TEXT," +
                        "payment_reference TEXT," +
                        "created_by TEXT," +
                        "updated_by TEXT," +
                        "cancelled_at TEXT," +
                        "cancellation_reason TEXT," +
                        "created_at TEXT NOT NULL," +
                        "updated_at TEXT," +
                        "sync_id TEXT," +
                        "device_id TEXT," +
                        "synced_at TEXT," +
                        "sync_status INTEGER DEFAULT 0" +
                        ")");
                statement.execute("CREATE TABLE IF NOT EXISTS credit_sale_parts (" +
                        "id TEXT PRIMARY KEY," +
                        "credit_sale_id TEXT NOT NULL REFERENCES credit_sales(id) ON DELETE CASCADE," +
                        "product_id TEXT REFERENCES products(id)," +
                        "description TEXT," +
                        "quantity INTEGER," +
                        "unit_price REAL," +
                        "total REAL," +
                        "created_at TEXT NOT NULL," +
                        "sync_id TEXT," +
                        "device_id TEXT," +
                        "synced_at TEXT," +
                        "sync_status INTEGER DEFAULT 0" +
                        ")");
                statement.execute("CREATE INDEX IF NOT EXISTS idx_credit_sale_parts_credit_sale_id " +
                        "ON credit_sale_parts (credit_sale_id)");

                statement.execute("CREATE TABLE IF NOT EXISTS services (" +
                        "id TEXT PRIMARY KEY," +
                        "name TEXT," +
                        "price REAL NOT NULL DEFAULT 0," +
                        "service_date TEXT," +
                        "remark TEXT," +
                        "sync_id TEXT," +
                        "device_id TEXT," +
                        "synced_at TEXT," +
                        "sync_status INTEGER DEFAULT 0," +
                        "created_at TEXT" +
                        ")");
                statement.execute("CREATE TABLE IF NOT EXISTS tyre_exports (" +
                        "id TEXT PRIMARY KEY," +
                        "export_id TEXT," +
                        "operation TEXT," +
                        "company TEXT," +
                        "tyres INTEGER NOT NULL DEFAULT 0," +
                        "cust_price REAL NOT NULL DEFAULT 0," +
                        "comp_price REAL NOT NULL DEFAULT 0," +
                        "service_fee REAL NOT NULL DEFAULT 0," +
                        "paid_amount REAL NOT NULL DEFAULT 0," +
                        "total_amount REAL NOT NULL DEFAULT 0," +
                        "balance_amount REAL NOT NULL DEFAULT 0," +
                        "payment_status TEXT," +
                        "status TEXT," +
                        "export_date TEXT," +
                        "updated_at TEXT," +
                        "sync_id TEXT," +
                        "device_id TEXT," +
                        "synced_at TEXT," +
                        "sync_status INTEGER DEFAULT 0" +
                        ")");
                statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_tyre_exports_export_id " +
                        "ON tyre_exports (export_id)");

                statement.execute("CREATE TABLE IF NOT EXISTS workers (" +
                        "id TEXT PRIMARY KEY," +
                        "name TEXT NOT NULL," +
                        "phone TEXT," +
                        "role TEXT," +
                        "rate TEXT," +
                        "salary_type TEXT," +
                        "created_at TEXT NOT NULL," +
                        "sync_id TEXT," +
                        "device_id TEXT," +
                        "synced_at TEXT," +
                        "sync_status INTEGER DEFAULT 0" +
                        ")");
                statement.execute("CREATE TABLE IF NOT EXISTS worker_attendance (" +
                        "id TEXT PRIMARY KEY," +
                        "worker_id TEXT NOT NULL," +
                        "attendance_date TEXT NOT NULL," +
                        "status TEXT NOT NULL," +
                        "created_at TEXT NOT NULL," +
                        "updated_at TEXT NOT NULL," +
                        "sync_id TEXT," +
                        "device_id TEXT," +
                        "synced_at TEXT," +
                        "sync_status INTEGER DEFAULT 0" +
                        ")");
                statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_worker_date " +
                        "ON worker_attendance (worker_id, attendance_date)");

                statement.execute("CREATE TABLE IF NOT EXISTS salary_advances (" +
                        "id TEXT PRIMARY KEY," +
                        "worker_id TEXT," +
                        "worker TEXT," +
                        "amount REAL NOT NULL DEFAULT 0," +
                        "advance_date TEXT," +
                        "note TEXT," +
                        "created_at TEXT," +
                        "sync_id TEXT," +
                        "device_id TEXT," +
                        "synced_at TEXT," +
                        "sync_status INTEGER DEFAULT 0" +
                        ")");
                statement.execute("CREATE TABLE IF NOT EXISTS salary_payments (" +
                        "id TEXT PRIMARY KEY," +
                        "worker_id TEXT NOT NULL," +
                        "worker TEXT NOT NULL," +
                        "period_from TEXT NOT NULL," +
                        "period_to TEXT NOT NULL," +
                        "amount REAL NOT NULL DEFAULT 0," +
                        "paid_at TEXT NOT NULL," +
                        "sync_id TEXT," +
                        "device_id TEXT," +
                        "synced_at TEXT," +
                        "sync_status INTEGER DEFAULT 0" +
                        ")");
                statement.execute("CREATE TABLE IF NOT EXISTS worker_credits (" +
                        "id TEXT PRIMARY KEY," +
                        "worker_id TEXT," +
                        "worker TEXT," +
                        "amount REAL NOT NULL DEFAULT 0," +
                        "credit_type TEXT," +
                        "credit_date TEXT," +
                        "note TEXT," +
                        "created_at TEXT," +
                        "sync_id TEXT," +
                        "device_id TEXT," +
                        "synced_at TEXT," +
                        "sync_status INTEGER DEFAULT 0" +
                        ")");
                statement.execute("CREATE TABLE IF NOT EXISTS quick_services (" +
                        "id TEXT PRIMARY KEY," +
                        "service TEXT," +
                        "price REAL NOT NULL DEFAULT 0," +
                        "service_date TEXT," +
                        "notes TEXT," +
                        "created_by TEXT," +
                        "updated_by TEXT," +
                        "sync_id TEXT," +
                        "device_id TEXT," +
                        "synced_at TEXT," +
                        "sync_status INTEGER DEFAULT 0," +
                        "created_at TEXT," +
                        "updated_at TEXT" +
                        ")");
                statement.execute("CREATE TABLE IF NOT EXISTS quick_service_presets (" +
                        "id TEXT PRIMARY KEY," +
                        "service TEXT NOT NULL," +
                        "price REAL NOT NULL DEFAULT 0," +
                        "active INTEGER NOT NULL DEFAULT 1," +
                        "icon TEXT DEFAULT 'fas-bolt'," +
                        "created_at TEXT NOT NULL" +
                        ")");

                // Seed sync state row if not present
                statement.execute("INSERT OR IGNORE INTO app_sync_state (id, device_id, last_sync_at) " +
                        "VALUES (1, lower(hex(randomblob(16))), NULL)");

                // Back-fill product codes for rows that have none
                backfillProductCodes(connection);

                validateSchema(connection);
            }

            initialized = true;
        } catch (SQLException ex) {
            System.err.println("=== DATABASE INITIALIZATION ERROR ===");
            System.err.println("Database path: " + getSqlitePath());
            System.err.println("Error Message: " + ex.getMessage());
            throw new IllegalStateException("Failed to initialize local database: " + ex.getMessage(), ex);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + getSqlitePath());
    }

    /**
     * Quick connection test used during init to provide a clearer error earlier.
     */
    public static void testDatabaseConnection() throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT 1")) {
            stmt.executeQuery();
        } catch (SQLException ex) {
            System.err.println("=== DATABASE CONNECTION TEST FAILED ===");
            System.err.println("SQLite path: " + getSqlitePath());
            System.err.println("ERROR: " + ex.getMessage());
            throw ex;
        }
    }

    public static String getJdbcUrl() {
        return "jdbc:sqlite:" + getSqlitePath();
    }

    /** Returns null – SQLite has no concept of a user. */
    public static String getJdbcUser() {
        return null;
    }

    /** Returns null – SQLite has no concept of a password. */
    public static String getJdbcPassword() {
        return null;
    }

    public static String getDeviceId() {
        init();
        String sql = "SELECT device_id FROM app_sync_state WHERE id = 1";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            return rs.next() ? rs.getString("device_id") : "";
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to read device id", ex);
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private static String getSqlitePath() {
        String env = System.getenv("KLINE_DB_SQLITE_PATH");
        if (env != null && !env.isBlank()) return env;
        String prop = System.getProperty("kline.dbSqlitePath");
        if (prop != null && !prop.isBlank()) return prop;
        return DEFAULT_DB_PATH;
    }

    /**
     * Adds a column to a table if it does not already exist.
     * Uses SQLite's PRAGMA table_info instead of information_schema.
     */
    private static void ensureColumnExists(Connection connection, String table, String column, String definition)
            throws SQLException {
        boolean exists = false;
        try (PreparedStatement ps = connection.prepareStatement("PRAGMA table_info(" + table + ")");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    exists = true;
                    break;
                }
            }
        }
        if (!exists) {
            try (Statement alter = connection.createStatement()) {
                alter.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            }
        }
    }

    private static void validateSchema(Connection connection) throws SQLException {
        List<String> required = List.of(
                "app_sync_state", "sync_queue", "products", "customers",
                "invoices", "invoice_line_items", "credit_sales", "credit_sale_parts",
                "services", "tyre_exports", "workers", "worker_attendance",
                "salary_advances", "salary_payments", "worker_credits",
                "quick_services", "quick_service_presets"
        );
        List<String> existing = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT name FROM sqlite_master WHERE type='table'");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                existing.add(rs.getString("name"));
            }
        }
        List<String> missing = new ArrayList<>();
        for (String table : required) {
            if (!existing.contains(table)) {
                missing.add(table);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Missing required tables: " + String.join(", ", missing));
        }
    }

    private static void backfillProductCodes(Connection connection) throws SQLException {
        // SQLite-compatible product code backfill using substr/upper/replace
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE products SET product_code = 'PRD-' || upper(substr(replace(id, '-', ''), 1, 8)) " +
                        "WHERE product_code IS NULL OR product_code = ''")) {
            ps.executeUpdate();
        }
    }
}
