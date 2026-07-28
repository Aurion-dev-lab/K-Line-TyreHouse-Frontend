package com.gui.kline.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseManager {
    private static final String DEFAULT_URL = "jdbc:sqlite:kline.db";
    private static volatile boolean initialized = false;

    private DatabaseManager() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        try {
            Connection connection = getConnection();
            Statement statement = connection.createStatement();
            
            // Enable Foreign Key support in SQLite
            statement.execute("PRAGMA foreign_keys = ON;");

            statement.execute("CREATE TABLE IF NOT EXISTS sync_tombstones (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "table_name TEXT NOT NULL," +
                    "record_id TEXT NOT NULL," +
                    "client_deleted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                    ")");

            statement.execute("CREATE TABLE IF NOT EXISTS products (" +
                    "id TEXT PRIMARY KEY," +
                    "product_code TEXT UNIQUE," +
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
                    "created_at DATETIME," +
                    "updated_at DATETIME NOT NULL," +
                    "sync_status INTEGER NOT NULL DEFAULT 0" +
                    ")");

            statement.execute("CREATE TABLE IF NOT EXISTS product_images (" +
                    "id TEXT PRIMARY KEY," +
                    "product_id TEXT NOT NULL," +
                    "image_path TEXT NOT NULL," +
                    "created_at DATETIME NOT NULL," +
                    "sync_status INTEGER NOT NULL DEFAULT 0" +
                    ")");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_product_id ON product_images (product_id)");

            statement.execute("CREATE TABLE IF NOT EXISTS credit_customers (" +
                    "id TEXT PRIMARY KEY," +
                    "name TEXT NOT NULL," +
                    "phone TEXT," +
                    "email TEXT," +
                    "address TEXT," +
                    "created_at DATETIME NOT NULL," +
                    "updated_at DATETIME," +
                    "sync_status INTEGER NOT NULL DEFAULT 0," +
                    "UNIQUE (name, phone)" +
                    ")");

            statement.execute("CREATE TABLE IF NOT EXISTS invoices (" +
                    "id TEXT PRIMARY KEY," +
                    "invoice_id TEXT UNIQUE," +
                    "customer TEXT," +
                    "phone TEXT," +
                    "description TEXT," +
                    "vehicle_number TEXT," +
                    "invoice_date DATE," +
                    "type TEXT," +
                    "status TEXT," +
                    "subtotal REAL NOT NULL DEFAULT 0," +
                    "grand_total REAL NOT NULL DEFAULT 0," +
                    "line_items TEXT," +
                    "created_at DATETIME NOT NULL," +
                    "updated_at DATETIME," +
                    "sync_status INTEGER NOT NULL DEFAULT 0" +
                    ")");


            statement.execute("CREATE TABLE IF NOT EXISTS credit_sales (" +
                    "id TEXT PRIMARY KEY," +
                    "credit_id TEXT UNIQUE," +
                    "customer_id TEXT," +
                    "sale_date DATE," +
                    "due_date DATE," +
                    "sub_total REAL NOT NULL DEFAULT 0," +
                    "grand_total REAL NOT NULL DEFAULT 0," +
                    "settlement REAL NOT NULL DEFAULT 0," +
                    "status TEXT," +
                    "parts TEXT," +
                    "created_at DATETIME NOT NULL," +
                    "updated_at DATETIME," +
                    "sync_status INTEGER NOT NULL DEFAULT 0," +
                    "FOREIGN KEY(customer_id) REFERENCES credit_customers(id)" +
                    ")");

            statement.execute("DROP TABLE IF EXISTS invoice_line_items");
            statement.execute("DROP TABLE IF EXISTS credit_sale_parts");

            statement.execute("CREATE TABLE IF NOT EXISTS services (" +
                    "id TEXT PRIMARY KEY," +
                    "invoice_id TEXT," +
                    "name TEXT," +
                    "price REAL NOT NULL DEFAULT 0," +
                    "service_date DATE," +
                    "remark TEXT," +
                    "sync_status INTEGER NOT NULL DEFAULT 0" +
                    ")");

            statement.execute("CREATE TABLE IF NOT EXISTS tyre_exports (" +
                    "id TEXT PRIMARY KEY," +
                    "export_id TEXT UNIQUE," +
                    "serial_number TEXT UNIQUE," +
                    "company TEXT," +
                    "tyre_size TEXT," +
                    "tyre_make TEXT," +
                    "tyres INTEGER NOT NULL DEFAULT 0," +
                    "cust_price REAL NOT NULL DEFAULT 0," +
                    "comp_price REAL NOT NULL DEFAULT 0," +
                    "service_fee REAL NOT NULL DEFAULT 0," +
                    "sub_total REAL NOT NULL DEFAULT 0," +
                    "grand_total REAL NOT NULL DEFAULT 0," +
                    "initial_payment REAL NOT NULL DEFAULT 0," +
                    "settlement REAL NOT NULL DEFAULT 0," +
                    "status TEXT," +
                    "export_date DATE," +
                    "remark TEXT," +
                    "created_at DATETIME," +
                    "updated_at DATETIME," +
                    "sync_status INTEGER NOT NULL DEFAULT 0" +
                    ")");

            statement.execute("CREATE TABLE IF NOT EXISTS workers (" +
                    "id TEXT PRIMARY KEY," +
                    "name TEXT NOT NULL," +
                    "phone TEXT," +
                    "role TEXT," +
                    "rate TEXT," +
                    "salary_type TEXT," +
                    "created_at DATETIME NOT NULL," +
                    "sync_status INTEGER NOT NULL DEFAULT 0" +
                    ")");

            statement.execute("CREATE TABLE IF NOT EXISTS worker_attendance (" +
                    "id TEXT PRIMARY KEY," +
                    "worker_id TEXT NOT NULL," +
                    "attendance_date DATE NOT NULL," +
                    "status TEXT NOT NULL," +
                    "created_at DATETIME NOT NULL," +
                    "updated_at DATETIME NOT NULL," +
                    "sync_status INTEGER NOT NULL DEFAULT 0," +
                    "UNIQUE (worker_id, attendance_date)" +
                    ")");

            statement.execute("CREATE TABLE IF NOT EXISTS salary_advances (" +
                    "id TEXT PRIMARY KEY," +
                    "worker TEXT," +
                    "worker_id TEXT," +
                    "amount REAL NOT NULL DEFAULT 0," +
                    "advance_date DATE," +
                    "note TEXT," +
                    "created_at DATETIME," +
                    "sync_status INTEGER NOT NULL DEFAULT 0" +
                    ")");

            statement.execute("CREATE TABLE IF NOT EXISTS salary_payments (" +
                    "id TEXT PRIMARY KEY," +
                    "worker_id TEXT NOT NULL," +
                    "worker TEXT NOT NULL," +
                    "period_from DATE NOT NULL," +
                    "period_to DATE NOT NULL," +
                    "amount REAL NOT NULL DEFAULT 0," +
                    "paid_at DATETIME NOT NULL," +
                    "sync_status INTEGER NOT NULL DEFAULT 0" +
                    ")");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_salary_payment_period ON salary_payments (worker_id, period_from, period_to)");

            statement.execute("CREATE TABLE IF NOT EXISTS worker_credits (" +
                    "id TEXT PRIMARY KEY," +
                    "worker TEXT," +
                    "worker_id TEXT," +
                    "amount REAL NOT NULL DEFAULT 0," +
                    "credit_type TEXT," +
                    "credit_date DATE," +
                    "note TEXT," +
                    "created_at DATETIME," +
                    "sync_status INTEGER NOT NULL DEFAULT 0" +
                    ")");

            statement.execute("CREATE TABLE IF NOT EXISTS quick_services (" +
                    "id TEXT PRIMARY KEY," +
                    "service TEXT," +
                    "price REAL NOT NULL DEFAULT 0," +
                    "service_date DATE," +
                    "sync_status INTEGER NOT NULL DEFAULT 0" +
                    ")");

            statement.execute("CREATE TABLE IF NOT EXISTS quick_service_presets (" +
                    "id TEXT PRIMARY KEY," +
                    "service TEXT NOT NULL," +
                    "price REAL NOT NULL DEFAULT 0," +
                    "active INTEGER NOT NULL DEFAULT 1," +
                    "icon TEXT DEFAULT 'fas-bolt'," +
                    "created_at DATETIME NOT NULL," +
                    "sync_status INTEGER NOT NULL DEFAULT 0" +
                    ")");

            statement.execute("CREATE TABLE IF NOT EXISTS expenses (" +
                    "id TEXT PRIMARY KEY," +
                    "expense_date DATE NOT NULL," +
                    "description TEXT NOT NULL," +
                    "category TEXT," +
                    "amount REAL NOT NULL," +
                    "created_at DATETIME NOT NULL," +
                    "sync_status INTEGER NOT NULL DEFAULT 0" +
                    ")");

            backfillProductCodes(connection);
            initialized = true;
        } catch (SQLException ex) {
            System.err.println("=== DATABASE INITIALIZATION ERROR ===");
            System.err.println("Database URL: " + DEFAULT_URL);
            System.err.println("Error Message: " + ex.getMessage());
            throw new IllegalStateException("Failed to initialize local database: " + ex.getMessage(), ex);
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found on classpath", e);
        }
        String url = getJdbcUrl();
        Connection conn = DriverManager.getConnection(url);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    public static void testDatabaseConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found on classpath", e);
        }
        String url = getJdbcUrl();
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement stmt = conn.prepareStatement("SELECT 1")) {
            stmt.executeQuery();
        } catch (SQLException ex) {
            System.err.println("=== DATABASE CONNECTION TEST FAILED ===");
            System.err.println("JDBC URL: " + url);
            System.err.println("ERROR: " + ex.getMessage());
            throw ex;
        }
    }

    public static String getJdbcUrl() {
        return getEnvOrProp("KLINE_DB_URL", "kline.dbUrl", DEFAULT_URL);
    }

    private static String getEnvOrProp(String envKey, String propKey, String fallback) {
        String env = System.getenv(envKey);
        if (env != null && !env.isBlank()) {
            return env;
        }
        String prop = System.getProperty(propKey);
        if (prop != null && !prop.isBlank()) {
            return prop;
        }
        return fallback;
    }

    private static void backfillProductCodes(Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE products SET product_code = 'PRD-' || UPPER(SUBSTR(REPLACE(id, '-', ''), 1, 8)), sync_status = 0 " +
                        "WHERE product_code IS NULL OR product_code = ''")) {
            ps.executeUpdate();
        }
    }

    public static void logDeletion(String tableName, String recordId) {
        String sql = "INSERT INTO sync_tombstones (table_name, record_id) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tableName);
            pstmt.setString(2, recordId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to log deletion for table " + tableName + ": " + e.getMessage());
        }
    }
}
