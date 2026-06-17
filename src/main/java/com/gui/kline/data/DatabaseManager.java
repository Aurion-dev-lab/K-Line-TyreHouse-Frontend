package com.gui.kline.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class DatabaseManager {
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/kline_local?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";
    private static volatile boolean initialized = false;

    private DatabaseManager() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        try {
            // Test connection first
            testDatabaseConnection();
            
            Connection connection = getConnection();
            Statement statement = connection.createStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS app_sync_state (" +
                    "id TINYINT PRIMARY KEY," +
                    "device_id VARCHAR(64) NOT NULL," +
                    "last_sync_at DATETIME NULL" +
                    ")");
            statement.execute("CREATE TABLE IF NOT EXISTS sync_queue (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "entity_type VARCHAR(64) NOT NULL," +
                    "payload JSON NOT NULL," +
                    "status VARCHAR(16) NOT NULL," +
                    "created_at DATETIME NOT NULL," +
                    "last_error TEXT" +
                    ")");
            statement.execute("CREATE TABLE IF NOT EXISTS products (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "name VARCHAR(255) NOT NULL UNIQUE," +
                    "category VARCHAR(128)," +
                    "buy_price DECIMAL(12,2) NOT NULL DEFAULT 0," +
                    "sell_price DECIMAL(12,2) NOT NULL DEFAULT 0," +
                    "stock INT NOT NULL DEFAULT 0," +
                    "updated_at DATETIME NOT NULL" +
                    ")");
            statement.execute("CREATE TABLE IF NOT EXISTS customers (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "name VARCHAR(255) NOT NULL," +
                    "phone VARCHAR(32)," +
                    "created_at DATETIME NOT NULL," +
                    "UNIQUE KEY uk_customer_name_phone (name, phone)" +
                    ")");
             statement.execute("CREATE TABLE IF NOT EXISTS invoices (" +
                     "id VARCHAR(36) PRIMARY KEY," +
                     "invoice_id VARCHAR(64) UNIQUE," +
                     "customer VARCHAR(255)," +
                     "invoice_date DATE," +
                     "type VARCHAR(32)," +
                     "status VARCHAR(32)," +
                     "subtotal DECIMAL(12,2) NOT NULL DEFAULT 0," +
                     "tax DECIMAL(12,2) NOT NULL DEFAULT 0," +
                     "grand_total DECIMAL(12,2) NOT NULL DEFAULT 0," +
                     "created_at DATETIME NOT NULL," +
                     "updated_at DATETIME" +
                     ") ENGINE=InnoDB");
             statement.execute("CREATE TABLE IF NOT EXISTS invoice_line_items (" +
                     "id VARCHAR(36) PRIMARY KEY," +
                     "invoice_id VARCHAR(64)," +
                     "invoice_ref VARCHAR(36) NOT NULL," +
                     "product_id VARCHAR(36)," +
                     "description VARCHAR(255)," +
                     "type VARCHAR(32)," +
                     "qty INT," +
                     "unit_price DECIMAL(12,2)," +
                     "total DECIMAL(12,2)," +
                     "created_at DATETIME NOT NULL," +
                     "FOREIGN KEY (invoice_ref) REFERENCES invoices(id) ON DELETE CASCADE" +
                     ") ENGINE=InnoDB");
             statement.execute("CREATE TABLE IF NOT EXISTS credit_sales (" +
                     "id VARCHAR(36) PRIMARY KEY," +
                     "credit_id VARCHAR(64)," +
                     "customer VARCHAR(255)," +
                     "customer_name VARCHAR(255)," +
                     "sale_date DATE," +
                     "due_date DATE," +
                     "subtotal DECIMAL(12,2) NOT NULL DEFAULT 0," +
                     "paid_amount DECIMAL(12,2) NOT NULL DEFAULT 0," +
                     "amount DECIMAL(12,2) NOT NULL DEFAULT 0," +
                     "status VARCHAR(32)," +
                     "created_at DATETIME NOT NULL," +
                     "updated_at DATETIME" +
                     ")");
             statement.execute("CREATE TABLE IF NOT EXISTS credit_sale_parts (" +
                      "id VARCHAR(36) PRIMARY KEY," +
                      "credit_sale_id VARCHAR(36) NOT NULL," +
                      "product_id VARCHAR(36)," +
                      "description VARCHAR(255)," +
                      "quantity INT," +
                      "unit_price DECIMAL(12,2)," +
                      "total DECIMAL(12,2)," +
                      "created_at DATETIME NOT NULL," +
                      "FOREIGN KEY (credit_sale_id) REFERENCES credit_sales(id) ON DELETE CASCADE," +
                      "FOREIGN KEY (product_id) REFERENCES products(id)," +
                      "INDEX idx_credit_sale_id (credit_sale_id)" +
                      ") ENGINE=InnoDB");
            statement.execute("CREATE TABLE IF NOT EXISTS services (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "name VARCHAR(255)," +
                    "price DECIMAL(12,2) NOT NULL DEFAULT 0," +
                    "service_date DATE," +
                    "remark VARCHAR(255)" +
                    ")");
            statement.execute("CREATE TABLE IF NOT EXISTS tyre_exports (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "company VARCHAR(255)," +
                    "tyres INT NOT NULL DEFAULT 0," +
                    "status VARCHAR(32)," +
                    "export_date DATE" +
                    ")");
            statement.execute("CREATE TABLE IF NOT EXISTS workers (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "name VARCHAR(255) NOT NULL," +
                    "phone VARCHAR(32)," +
                    "role VARCHAR(128)," +
                    "rate VARCHAR(64)," +
                    "created_at DATETIME NOT NULL" +
                    ")");
            statement.execute("CREATE TABLE IF NOT EXISTS worker_attendance (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "worker_id VARCHAR(36) NOT NULL," +
                    "attendance_date DATE NOT NULL," +
                    "status VARCHAR(16) NOT NULL," +
                    "created_at DATETIME NOT NULL," +
                    "updated_at DATETIME NOT NULL," +
                    "UNIQUE KEY uk_worker_date (worker_id, attendance_date)" +
                    ")");
            statement.execute("CREATE TABLE IF NOT EXISTS salary_advances (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "worker VARCHAR(255)," +
                    "amount DECIMAL(12,2) NOT NULL DEFAULT 0," +
                    "advance_date DATE" +
                    ")");
            statement.execute("CREATE TABLE IF NOT EXISTS worker_credits (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "worker VARCHAR(255)," +
                    "amount DECIMAL(12,2) NOT NULL DEFAULT 0," +
                    "credit_type VARCHAR(16)," +
                    "credit_date DATE" +
                    ")");
            statement.execute("CREATE TABLE IF NOT EXISTS quick_services (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "service VARCHAR(255)," +
                    "price DECIMAL(12,2) NOT NULL DEFAULT 0," +
                    "service_date DATE" +
                    ")");
            statement.execute("CREATE TABLE IF NOT EXISTS quick_service_presets (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "service VARCHAR(255) NOT NULL," +
                    "price DECIMAL(12,2) NOT NULL DEFAULT 0," +
                    "active TINYINT(1) NOT NULL DEFAULT 1," +
                    "created_at DATETIME NOT NULL" +
                    ")");
            statement.execute("INSERT IGNORE INTO app_sync_state (id, device_id, last_sync_at) " +
                    "VALUES (1, UUID(), NULL)");
            // Ensure new invoice columns exist on older databases
            ensureColumnExists(connection, "invoices", "invoice_id", "VARCHAR(64)");
            ensureColumnExists(connection, "invoices", "customer", "VARCHAR(255)");
            ensureColumnExists(connection, "invoices", "invoice_date", "DATE");
            ensureColumnExists(connection, "invoices", "type", "VARCHAR(32)");
            ensureColumnExists(connection, "invoices", "status", "VARCHAR(32)");
            ensureColumnExists(connection, "invoices", "subtotal", "DECIMAL(12,2)");
            ensureColumnExists(connection, "invoices", "tax", "DECIMAL(12,2)");
            ensureColumnExists(connection, "invoices", "grand_total", "DECIMAL(12,2)");
            ensureColumnExists(connection, "invoices", "created_at", "DATETIME");
            ensureColumnExists(connection, "invoices", "updated_at", "DATETIME");
            ensureColumnExists(connection, "workers", "salary_type", "VARCHAR(32)");
            ensureColumnExists(connection, "salary_advances", "worker_id", "VARCHAR(36)");
            ensureColumnExists(connection, "salary_advances", "note", "VARCHAR(255)");
            ensureColumnExists(connection, "salary_advances", "created_at", "DATETIME");
            ensureColumnExists(connection, "worker_credits", "worker_id", "VARCHAR(36)");
            ensureColumnExists(connection, "worker_credits", "note", "VARCHAR(255)");
            ensureColumnExists(connection, "worker_credits", "created_at", "DATETIME");
            ensureColumnExists(connection, "services", "remark", "VARCHAR(255)");
             ensureColumnExists(connection, "credit_sales", "customer_name", "VARCHAR(255)");
             ensureColumnExists(connection, "credit_sales", "sale_date", "DATE");
             ensureColumnExists(connection, "credit_sales", "due_date", "DATE");
             ensureColumnExists(connection, "credit_sales", "subtotal", "DECIMAL(12,2)");
             ensureColumnExists(connection, "credit_sales", "paid_amount", "DECIMAL(12,2)");
             ensureColumnExists(connection, "credit_sales", "updated_at", "DATETIME");
             ensureColumnExists(connection, "credit_sales", "amount", "DECIMAL(12,2)");
             ensureColumnExists(connection, "credit_sales", "customer", "VARCHAR(255)");
             ensureColumnExists(connection, "credit_sale_parts", "product_id", "VARCHAR(36)");
             validateSchema(connection);
            initialized = true;
        } catch (SQLException ex) {
            System.err.println("=== DATABASE INITIALIZATION ERROR ===");
            System.err.println("Database URL: " + DEFAULT_URL);
            System.err.println("Database User: " + DEFAULT_USER);
            System.err.println("Error Message: " + ex.getMessage());
            ex.printStackTrace();
            throw new IllegalStateException("Failed to initialize local database: " + ex.getMessage(), ex);
        }
    }

    public static Connection getConnection() throws SQLException {
        String url = getJdbcUrl();
        return DriverManager.getConnection(url, getJdbcUser(), getJdbcPassword());
    }

    /**
     * Quick connection test used during init to provide a clearer error earlier.
     * Throws SQLException if connection cannot be established.
     */
    public static void testDatabaseConnection() throws SQLException {
        String url = getJdbcUrl();
        String user = getJdbcUser();
        String pass = getJdbcPassword();
        try (Connection conn = DriverManager.getConnection(url, user, pass);
             PreparedStatement stmt = conn.prepareStatement("SELECT 1");
             ResultSet rs = stmt.executeQuery()) {
            // successful connection
        } catch (SQLException ex) {
            System.err.println("=== DATABASE CONNECTION TEST FAILED ===");
            System.err.println("JDBC URL: " + url);
            System.err.println("JDBC USER: " + user);
            System.err.println("ERROR: " + ex.getMessage());
            throw ex;
        }
    }

    public static String getJdbcUrl() {
        return getEnvOrProp("KLINE_DB_URL", "kline.dbUrl", DEFAULT_URL);
    }

    public static String getJdbcUser() {
        return getEnvOrProp("KLINE_DB_USER", "kline.dbUser", DEFAULT_USER);
    }

    public static String getJdbcPassword() {
        return getEnvOrProp("KLINE_DB_PASSWORD", "kline.dbPassword", DEFAULT_PASSWORD);
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

    private static void ensureColumnExists(Connection connection, String table, String column, String definition)
            throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next() && rs.getInt("total") == 0) {
                    try (Statement alter = connection.createStatement()) {
                        alter.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
                    }
                }
            }
        }
    }

     private static void validateSchema(Connection connection) throws SQLException {
          List<String> required = List.of(
                  "app_sync_state", "sync_queue", "products", "customers",
                  "invoices", "invoice_line_items", "credit_sales", "credit_sale_parts", "services", "tyre_exports",
                  "workers", "worker_attendance", "salary_advances", "worker_credits", "quick_services",
                  "quick_service_presets"
          );
        String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()";
        List<String> existing = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                existing.add(rs.getString("table_name"));
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
}
