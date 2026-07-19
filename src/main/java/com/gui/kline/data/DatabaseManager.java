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
            statement.execute("CREATE TABLE IF NOT EXISTS sync_tombstones (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "table_name VARCHAR(255) NOT NULL," +
                    "record_id VARCHAR(36) NOT NULL," +
                    "client_deleted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                    ")");
            statement.execute("CREATE TABLE IF NOT EXISTS products (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                       "product_code VARCHAR(64)," +
                    "name VARCHAR(255) NOT NULL UNIQUE," +

                    "category VARCHAR(128)," +
                    "buy_price DECIMAL(12,2) NOT NULL DEFAULT 0," +
                    "sell_price DECIMAL(12,2) NOT NULL DEFAULT 0," +
                    "stock INT NOT NULL DEFAULT 0," +
                    "minimum_stock_alert INT NOT NULL DEFAULT 5," +
                    "brand VARCHAR(128)," +
                    "description TEXT," +
                    "vehicle_type VARCHAR(128)," +
                    "material VARCHAR(128)," +
                    "supplier_name VARCHAR(255)," +
                    "created_at DATETIME NOT NULL," +
                    "updated_at DATETIME NOT NULL," +
                    "sync_status BOOLEAN NOT NULL DEFAULT false" +
                    ")");
            statement.execute("CREATE TABLE IF NOT EXISTS product_images (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "product_id VARCHAR(36) NOT NULL," +
                    "image_path VARCHAR(255) NOT NULL," +
                    "created_at DATETIME NOT NULL," +
                    "sync_status BOOLEAN NOT NULL DEFAULT false," +
                    "INDEX idx_product_id (product_id)" +
                    ")");
            statement.execute("CREATE TABLE IF NOT EXISTS customers (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "name VARCHAR(255) NOT NULL," +
                    "phone VARCHAR(32)," +
                    "created_at DATETIME NOT NULL," +
                    "sync_status BOOLEAN NOT NULL DEFAULT false," +
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
                     "updated_at DATETIME," +
                    "sync_status BOOLEAN NOT NULL DEFAULT false" +
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
                    "sync_status BOOLEAN NOT NULL DEFAULT false," +
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
                     "updated_at DATETIME," +
                    "sync_status BOOLEAN NOT NULL DEFAULT false" +
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
                    "sync_status BOOLEAN NOT NULL DEFAULT false," +
                      "FOREIGN KEY (credit_sale_id) REFERENCES credit_sales(id) ON DELETE CASCADE," +
                      "FOREIGN KEY (product_id) REFERENCES products(id)," +
                      "INDEX idx_credit_sale_id (credit_sale_id)" +
                      ") ENGINE=InnoDB");
            statement.execute("CREATE TABLE IF NOT EXISTS services (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "name VARCHAR(255)," +
                    "price DECIMAL(12,2) NOT NULL DEFAULT 0," +
                    "service_date DATE," +
                    "remark VARCHAR(255)," +
                    "sync_status BOOLEAN NOT NULL DEFAULT false" +
                    ")");
            statement.execute("CREATE TABLE IF NOT EXISTS tyre_exports (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "export_id VARCHAR(64)," +
                    "operation VARCHAR(32)," +
                    "company VARCHAR(255)," +
                    "tyres INT NOT NULL DEFAULT 0," +
                    "cust_price DECIMAL(12,2) NOT NULL DEFAULT 0," +
                    "comp_price DECIMAL(12,2) NOT NULL DEFAULT 0," +
                    "service_fee DECIMAL(12,2) NOT NULL DEFAULT 0," +
                    "paid_amount DECIMAL(12,2) NOT NULL DEFAULT 0," +
                    "total_amount DECIMAL(12,2) NOT NULL DEFAULT 0," +
                    "balance_amount DECIMAL(12,2) NOT NULL DEFAULT 0," +
                    "payment_status VARCHAR(32)," +
                    "status VARCHAR(32)," +
                    "export_date DATE," +
                    "updated_at DATETIME," +
                    "sync_status BOOLEAN NOT NULL DEFAULT false" +
                    ")");
            statement.execute("CREATE TABLE IF NOT EXISTS workers (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "name VARCHAR(255) NOT NULL," +
                    "phone VARCHAR(32)," +
                    "role VARCHAR(128)," +
                    "rate VARCHAR(64)," +
                    "created_at DATETIME NOT NULL," +
                    "sync_status BOOLEAN NOT NULL DEFAULT false" +
                    ")");
            statement.execute("CREATE TABLE IF NOT EXISTS worker_attendance (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "worker_id VARCHAR(36) NOT NULL," +
                    "attendance_date DATE NOT NULL," +
                    "status VARCHAR(16) NOT NULL," +
                    "created_at DATETIME NOT NULL," +
                    "updated_at DATETIME NOT NULL," +
                    "sync_status BOOLEAN NOT NULL DEFAULT false," +
                    "UNIQUE KEY uk_worker_date (worker_id, attendance_date)" +
                    ")");
            statement.execute("CREATE TABLE IF NOT EXISTS salary_advances (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "worker VARCHAR(255)," +
                    "amount DECIMAL(12,2) NOT NULL DEFAULT 0," +
                    "advance_date DATE," +
                    "sync_status BOOLEAN NOT NULL DEFAULT false" +
                    ")");
            statement.execute("CREATE TABLE IF NOT EXISTS salary_payments (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "worker_id VARCHAR(36) NOT NULL," +
                    "worker VARCHAR(255) NOT NULL," +
                    "period_from DATE NOT NULL," +
                    "period_to DATE NOT NULL," +
                    "amount DECIMAL(12,2) NOT NULL DEFAULT 0," +
                    "paid_at DATETIME NOT NULL," +
                    "sync_status BOOLEAN NOT NULL DEFAULT false," +
                    "INDEX idx_salary_payment_period (worker_id, period_from, period_to)" +
                    ")");
            statement.execute("CREATE TABLE IF NOT EXISTS worker_credits (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "worker VARCHAR(255)," +
                    "amount DECIMAL(12,2) NOT NULL DEFAULT 0," +
                    "credit_type VARCHAR(16)," +
                    "credit_date DATE," +
                    "sync_status BOOLEAN NOT NULL DEFAULT false" +
                    ")");
            statement.execute("CREATE TABLE IF NOT EXISTS quick_services (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "service VARCHAR(255)," +
                    "price DECIMAL(12,2) NOT NULL DEFAULT 0," +
                    "service_date DATE," +
                    "sync_status BOOLEAN NOT NULL DEFAULT false" +
                    ")");
             statement.execute("CREATE TABLE IF NOT EXISTS quick_service_presets (" +
                     "id VARCHAR(36) PRIMARY KEY," +
                     "service VARCHAR(255) NOT NULL," +
                     "price DECIMAL(12,2) NOT NULL DEFAULT 0," +
                     "active TINYINT(1) NOT NULL DEFAULT 1," +
                     "icon VARCHAR(50) DEFAULT 'fas-bolt'," +
                     "created_at DATETIME NOT NULL," +
                     "sync_status BOOLEAN NOT NULL DEFAULT false" +
                     ")");
             statement.execute("CREATE TABLE IF NOT EXISTS expenses (" +
                     "id VARCHAR(36) PRIMARY KEY," +
                     "expense_date DATE NOT NULL," +
                     "description VARCHAR(255) NOT NULL," +
                     "category VARCHAR(100)," +
                     "amount DECIMAL(12,2) NOT NULL," +
                     "created_at DATETIME NOT NULL," +
                    "sync_status BOOLEAN NOT NULL DEFAULT false" +
                     ")");
            // Ensure new invoice columns exist on older databases
            ensureColumnExists(connection, "invoices", "invoice_id", "VARCHAR(64)");
            ensureColumnExists(connection, "invoices", "customer", "VARCHAR(255)");
            ensureColumnExists(connection, "invoices", "invoice_date", "DATE");
            ensureColumnExists(connection, "quick_service_presets", "sync_status", "BOOLEAN NOT NULL DEFAULT false");
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
            // Salary payments are individual transactions so partial payments can be
            // reported on the date they were actually made.
            dropIndexIfExists(connection, "salary_payments", "uk_salary_payment_period");
            ensureColumnExists(connection, "worker_credits", "worker_id", "VARCHAR(36)");
            ensureColumnExists(connection, "worker_credits", "note", "VARCHAR(255)");
            ensureColumnExists(connection, "worker_credits", "created_at", "DATETIME");
            ensureColumnExists(connection, "services", "remark", "VARCHAR(255)");
            ensureColumnExists(connection, "tyre_exports", "export_id", "VARCHAR(64)");
            ensureColumnExists(connection, "tyre_exports", "operation", "VARCHAR(32)");
            ensureColumnExists(connection, "tyre_exports", "cust_price", "DECIMAL(12,2)");
            ensureColumnExists(connection, "tyre_exports", "comp_price", "DECIMAL(12,2)");
            ensureColumnExists(connection, "tyre_exports", "service_fee", "DECIMAL(12,2)");
            ensureColumnExists(connection, "tyre_exports", "paid_amount", "DECIMAL(12,2)");
            ensureColumnExists(connection, "tyre_exports", "total_amount", "DECIMAL(12,2)");
            ensureColumnExists(connection, "tyre_exports", "balance_amount", "DECIMAL(12,2)");
            ensureColumnExists(connection, "tyre_exports", "payment_status", "VARCHAR(32)");
            ensureColumnExists(connection, "tyre_exports", "updated_at", "DATETIME");
             ensureColumnExists(connection, "credit_sales", "customer_name", "VARCHAR(255)");
             ensureColumnExists(connection, "credit_sales", "sale_date", "DATE");
             ensureColumnExists(connection, "credit_sales", "due_date", "DATE");
             ensureColumnExists(connection, "credit_sales", "subtotal", "DECIMAL(12,2)");
             ensureColumnExists(connection, "credit_sales", "paid_amount", "DECIMAL(12,2)");
             ensureColumnExists(connection, "credit_sales", "updated_at", "DATETIME");
              ensureColumnExists(connection, "credit_sales", "amount", "DECIMAL(12,2)");
              ensureColumnExists(connection, "credit_sales", "customer", "VARCHAR(255)");
              ensureColumnExists(connection, "credit_sales", "labour", "DECIMAL(12,2) DEFAULT 0.00");
              ensureColumnExists(connection, "credit_sales", "parts_cost", "DECIMAL(12,2) DEFAULT 0.00");
              ensureColumnExists(connection, "credit_sales", "discount", "DECIMAL(12,2) DEFAULT 0.00");
              ensureColumnExists(connection, "credit_sales", "labour_cost", "DECIMAL(12,2) DEFAULT 0.00");
              ensureColumnExists(connection, "credit_sales", "extra_parts", "DECIMAL(12,2) DEFAULT 0.00");
              ensureColumnExists(connection, "credit_sales", "discount_amount", "DECIMAL(12,2) DEFAULT 0.00");
             ensureColumnExists(connection, "credit_sale_parts", "product_id", "VARCHAR(36)");
              ensureColumnExists(connection, "products", "product_code", "VARCHAR(64)");
             ensureColumnExists(connection, "products", "minimum_stock_alert", "INT DEFAULT 5");
             ensureColumnExists(connection, "products", "image_path", "VARCHAR(255)");
             ensureColumnExists(connection, "products", "brand", "VARCHAR(128)");
             ensureColumnExists(connection, "products", "description", "TEXT");
             ensureColumnExists(connection, "products", "vehicle_type", "VARCHAR(128)");
             ensureColumnExists(connection, "products", "material", "VARCHAR(128)");
             ensureColumnExists(connection, "products", "supplier_name", "VARCHAR(255)");
             ensureColumnExists(connection, "products", "created_at", "DATETIME");
             
             
             
             
             
             
             
             
             
             
             
             
             
             
             backfillProductCodes(connection);
             ensureUniqueIndex(connection, "products", "uk_products_product_code", "product_code");
             ensureUniqueIndex(connection, "tyre_exports", "uk_tyre_exports_export_id", "export_id");
             validateSchema(connection);
            initialized = true;
        } catch (SQLException ex) {
            System.err.println("=== DATABASE INITIALIZATION ERROR ===");
            System.err.println("Database URL: " + DEFAULT_URL);
            System.err.println("Database User: " + DEFAULT_USER);
            System.err.println("Error Message: " + ex.getMessage());
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
             PreparedStatement stmt = conn.prepareStatement("SELECT 1")) {
            stmt.executeQuery();
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
                  "products", "customers",
                  "invoices", "invoice_line_items", "credit_sales", "credit_sale_parts", "services", "tyre_exports",
                  "workers", "worker_attendance", "salary_advances", "salary_payments", "worker_credits", "quick_services",
                  "quick_service_presets", "expenses"
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

    private static void backfillProductCodes(Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE products SET product_code = CONCAT('PRD-', UPPER(SUBSTRING(REPLACE(id, '-', ''), 1, 8))) " +
                        "WHERE product_code IS NULL OR product_code = ''")) {
            ps.executeUpdate();
        }
    }

    private static void ensureUniqueIndex(Connection connection, String table, String indexName, String column)
            throws SQLException {
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?";
        try (PreparedStatement check = connection.prepareStatement(sql)) {
            check.setString(1, table);
            check.setString(2, indexName);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    try (Statement create = connection.createStatement()) {
                        create.execute("ALTER TABLE " + table + " ADD UNIQUE INDEX " + indexName + " (" + column + ")");
                    }
                }
            }
        }
    }

    private static void dropIndexIfExists(Connection connection, String table, String indexName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?";
        try (PreparedStatement check = connection.prepareStatement(sql)) {
            check.setString(1, table);
            check.setString(2, indexName);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    try (Statement drop = connection.createStatement()) {
                        drop.execute("DROP INDEX " + indexName + " ON " + table);
                    }
                }
            }
        }
    }
    
    private static void addSyncColumns(Connection connection, String table) throws SQLException {
        // Add sync_id column if not exists
        ensureColumnExists(connection, table, "sync_id", "VARCHAR(36)");
        // Add device_id column if not exists  
        ensureColumnExists(connection, table, "device_id", "VARCHAR(64)");
        // Add synced_at column if not exists
        ensureColumnExists(connection, table, "synced_at", "DATETIME");
        // Add sync_status column if not exists
        ensureColumnExists(connection, table, "sync_status", "BOOLEAN NOT NULL DEFAULT false");
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
