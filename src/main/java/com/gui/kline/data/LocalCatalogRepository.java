package com.gui.kline.data;

import com.gui.kline.models.Product;
import com.gui.kline.models.CreditCustomer;
import com.gui.kline.utils.ImagePathUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LocalCatalogRepository {
    public List<String> getProductNames() {
        String sql = "SELECT product_code, name FROM products ORDER BY product_code, name";
        List<String> names = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                names.add(formatProductLabel(rs.getString("product_code"), rs.getString("name")));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to read products", ex);
        }
        return names;
    }

    public List<String> getCustomerNames() {
        String sql = "SELECT DISTINCT name FROM credit_customers ORDER BY name";
        List<String> names = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                names.add(rs.getString("name"));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to read credit_customers", ex);
        }
        return names;
    }

    public void saveCustomer(String name, String phone) {
        if (name == null || name.isBlank()) {
            return;
        }
        String sql = "INSERT INTO credit_customers (id, name, phone, created_at) VALUES (?, ?, ?, strftime('%Y-%m-%dT%H:%M:%S', 'now')) " +
                "ON CONFLICT(name, phone) DO UPDATE SET phone = excluded.phone, sync_status = 0";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, java.util.UUID.randomUUID().toString());
            statement.setString(2, name.trim());
            statement.setString(3, phone == null ? null : phone.trim());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to save customer", ex);
        }
    }

    public String getCustomerIdByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String sql = "SELECT id FROM credit_customers WHERE name = ? LIMIT 1";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name.trim());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("id");
                }
            }
        } catch (SQLException ex) {
            System.err.println("Failed to find customer ID by name: " + ex.getMessage());
        }
        return null;
    }

    public String getCustomerPhone(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        String sql = "SELECT phone FROM credit_customers WHERE name = ? LIMIT 1";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name.trim());
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return "";
                }
                String phone = rs.getString("phone");
                return phone == null ? "" : phone;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to read customer phone", ex);
        }
    }

    public List<CreditCustomer> loadCreditCustomers() {
        String sql = "SELECT id, name, phone, email, address, created_at, updated_at FROM credit_customers ORDER BY name";
        List<CreditCustomer> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                CreditCustomer cc = new CreditCustomer(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        rs.getString("address"),
                        rs.getString("created_at"),
                        rs.getString("updated_at")
                );
                list.add(cc);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load credit customers", ex);
        }
        return list;
    }

    public void loadCustomerStats(CreditCustomer customer) {
        String sql = "SELECT COALESCE(SUM(grand_total), 0) as total_amount, COALESCE(SUM(settlement), 0) as settle_amount FROM credit_sales WHERE customer_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, customer.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double total = rs.getDouble("total_amount");
                    double settle = rs.getDouble("settle_amount");
                    customer.setTotalAmount(total);
                    customer.setSettleAmount(settle);
                    customer.setDueAmount(total - settle);
                }
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load stats for customer " + customer.getName() + ": " + ex.getMessage());
        }
    }

    public void saveCreditCustomer(CreditCustomer customer) {
        String sql = "INSERT INTO credit_customers (id, name, phone, email, address, created_at, updated_at, sync_status) " +
                     "VALUES (?, ?, ?, ?, ?, strftime('%Y-%m-%dT%H:%M:%S', 'now'), strftime('%Y-%m-%dT%H:%M:%S', 'now'), 0) " +
                     "ON CONFLICT(id) DO UPDATE SET " +
                     "  name = excluded.name, " +
                     "  phone = excluded.phone, " +
                     "  email = excluded.email, " +
                     "  address = excluded.address, " +
                     "  updated_at = strftime('%Y-%m-%dT%H:%M:%S', 'now'), " +
                     "  sync_status = 0";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, customer.getId() == null || customer.getId().isBlank() ? java.util.UUID.randomUUID().toString() : customer.getId());
            stmt.setString(2, customer.getName());
            stmt.setString(3, customer.getPhone());
            stmt.setString(4, customer.getEmail());
            stmt.setString(5, customer.getAddress());
            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to save credit customer", ex);
        }
    }

    public void deleteCreditCustomer(String id) {
        String sql = "DELETE FROM credit_customers WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to delete credit customer", ex);
        }
    }

    public CreditCustomer findCreditCustomerById(String id) {
        String sql = "SELECT id, name, phone, email, address, created_at, updated_at FROM credit_customers WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new CreditCustomer(
                            rs.getString("id"),
                            rs.getString("name"),
                            rs.getString("phone"),
                            rs.getString("email"),
                            rs.getString("address"),
                            rs.getString("created_at"),
                            rs.getString("updated_at")
                    );
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to find credit customer by id", ex);
        }
        return null;
    }

    private static final com.google.gson.Gson GSON = new com.google.gson.Gson();

    private List<String> parseJsonImagePaths(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<ArrayList<String>>(){}.getType();
            List<String> list = GSON.fromJson(json, listType);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            // Fallback for legacy single string paths
            List<String> fallback = new ArrayList<>();
            fallback.add(json);
            return fallback;
        }
    }

      public List<Product> loadProducts() {
          String sql = "SELECT id, product_code, name, category, buy_price, sell_price, stock, minimum_stock_alert, " +
                      "brand, description, vehicle_type, material, supplier_name, created_at, image_paths FROM products ORDER BY product_code, name";
          List<Product> products = new ArrayList<>();
          try (Connection connection = DatabaseManager.getConnection();
               PreparedStatement statement = connection.prepareStatement(sql);
               ResultSet rs = statement.executeQuery()) {
              while (rs.next()) {
                  Product product = new Product(
                          rs.getString("id"),
                          rs.getString("product_code"),
                          rs.getString("name"),
                          rs.getString("category"),
                          rs.getDouble("buy_price"),
                          rs.getDouble("sell_price"),
                          rs.getInt("stock"),
                          rs.getInt("minimum_stock_alert"),
                          rs.getString("brand"),
                          rs.getString("description"),
                          rs.getString("vehicle_type"),
                          rs.getString("material"),
                          rs.getString("supplier_name"),
                          rs.getString("created_at"),
                          parseJsonImagePaths(rs.getString("image_paths"))
                  );
                  products.add(product);
              }
          } catch (SQLException ex) {
              throw new IllegalStateException("Failed to load products", ex);
          }
          return products;
      }

    public Product findProductByCode(String productCode) {
        String sql = "SELECT id, product_code, name, category, buy_price, sell_price, stock, minimum_stock_alert, " +
                    "brand, description, vehicle_type, material, supplier_name, created_at, image_paths FROM products WHERE product_code = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, productCode);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new Product(
                            rs.getString("id"),
                            rs.getString("product_code"),
                            rs.getString("name"),
                            rs.getString("category"),
                            rs.getDouble("buy_price"),
                            rs.getDouble("sell_price"),
                            rs.getInt("stock"),
                            rs.getInt("minimum_stock_alert"),
                            rs.getString("brand"),
                            rs.getString("description"),
                            rs.getString("vehicle_type"),
                            rs.getString("material"),
                            rs.getString("supplier_name"),
                            rs.getString("created_at"),
                            parseJsonImagePaths(rs.getString("image_paths"))
                    );
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load product by code", ex);
        }
        return null;
    }

      public Product findProductById(String productId) {
          String sql = "SELECT id, product_code, name, category, buy_price, sell_price, stock, minimum_stock_alert, " +
                      "brand, description, vehicle_type, material, supplier_name, created_at, image_paths FROM products WHERE id = ?";
          try (Connection connection = DatabaseManager.getConnection();
               PreparedStatement statement = connection.prepareStatement(sql)) {
              statement.setString(1, productId);
              try (ResultSet rs = statement.executeQuery()) {
                  if (rs.next()) {
                      return new Product(
                              rs.getString("id"),
                              rs.getString("product_code"),
                              rs.getString("name"),
                              rs.getString("category"),
                              rs.getDouble("buy_price"),
                              rs.getDouble("sell_price"),
                              rs.getInt("stock"),
                              rs.getInt("minimum_stock_alert"),
                              rs.getString("brand"),
                              rs.getString("description"),
                              rs.getString("vehicle_type"),
                              rs.getString("material"),
                              rs.getString("supplier_name"),
                              rs.getString("created_at"),
                              parseJsonImagePaths(rs.getString("image_paths"))
                      );
                  }
              }
          } catch (SQLException ex) {
              throw new IllegalStateException("Failed to load product", ex);
          }
          return null;
      }

      public List<Product> getProductsByCategory(String category) {
          String sql = "SELECT id, product_code, name, category, buy_price, sell_price, stock, minimum_stock_alert, " +
                      "brand, description, vehicle_type, material, supplier_name, created_at, image_paths FROM products WHERE category = ? ORDER BY product_code, name";
          List<Product> products = new ArrayList<>();
          try (Connection connection = DatabaseManager.getConnection();
               PreparedStatement statement = connection.prepareStatement(sql)) {
              statement.setString(1, category);
              try (ResultSet rs = statement.executeQuery()) {
                  while (rs.next()) {
                      Product product = new Product(
                              rs.getString("id"),
                              rs.getString("product_code"),
                              rs.getString("name"),
                              rs.getString("category"),
                              rs.getDouble("buy_price"),
                              rs.getDouble("sell_price"),
                              rs.getInt("stock"),
                              rs.getInt("minimum_stock_alert"),
                              rs.getString("brand"),
                              rs.getString("description"),
                              rs.getString("vehicle_type"),
                              rs.getString("material"),
                              rs.getString("supplier_name"),
                              rs.getString("created_at"),
                              parseJsonImagePaths(rs.getString("image_paths"))
                      );
                      products.add(product);
                  }
              }
          } catch (SQLException ex) {
              throw new IllegalStateException("Failed to load products by category", ex);
          }
          return products;
      }

      public void updateProductStock(String productId, int quantityChange) {
          String sql = "UPDATE products SET stock = stock + ?, sync_status = 0, updated_at = strftime('%Y-%m-%dT%H:%M:%S', 'now') WHERE id = ?";
          try (Connection connection = DatabaseManager.getConnection();
               PreparedStatement statement = connection.prepareStatement(sql)) {
              statement.setInt(1, quantityChange);
              statement.setString(2, productId);
              statement.executeUpdate();
          } catch (SQLException ex) {
              throw new IllegalStateException("Failed to update product stock", ex);
          }
      }

    public void saveProduct(Product product) {
        if (product == null) {
            return;
        }
        String jsonImages = GSON.toJson(product.getImagePaths());
        String sql = "INSERT INTO products (id, product_code, name, category, buy_price, sell_price, stock, minimum_stock_alert, " +
                "brand, description, vehicle_type, material, supplier_name, image_paths, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, COALESCE(?, strftime('%Y-%m-%dT%H:%M:%S', 'now')), strftime('%Y-%m-%dT%H:%M:%S', 'now')) " +
                "ON CONFLICT(id) DO UPDATE SET product_code = excluded.product_code, name = excluded.name, category = excluded.category, buy_price = excluded.buy_price, " +
                "sell_price = excluded.sell_price, stock = excluded.stock, minimum_stock_alert = excluded.minimum_stock_alert, " +
                "brand = excluded.brand, description = excluded.description, vehicle_type = excluded.vehicle_type, " +
                "material = excluded.material, supplier_name = excluded.supplier_name, image_paths = excluded.image_paths, " +
                "created_at = COALESCE(products.created_at, strftime('%Y-%m-%dT%H:%M:%S', 'now')), sync_status = 0, updated_at = strftime('%Y-%m-%dT%H:%M:%S', 'now')";
        Connection connection = null;
        try {
            connection = DatabaseManager.getConnection();
            connection.setAutoCommit(false);  // Disable auto-commit for transaction control

            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, product.getId());
            statement.setString(2, product.getCode());
            statement.setString(3, product.getName());
            statement.setString(4, product.getCategory());
            statement.setDouble(5, product.getBuyPrice());
            statement.setDouble(6, product.getSellPrice());
            statement.setInt(7, product.getStock());
            statement.setInt(8, product.getMinimumStockAlert());
            statement.setString(9, product.getBrand());
            statement.setString(10, product.getDescription());
            statement.setString(11, product.getVehicleType());
            statement.setString(12, product.getMaterial());
            statement.setString(13, product.getSupplierName());
            statement.setString(14, jsonImages);  // image_paths JSON array string

            // Handle empty created_date - use null so COALESCE kicks in
            String createdDate = product.getCreatedDate();
            if (createdDate == null || createdDate.isEmpty() || createdDate.equals("null")) {
                statement.setString(15, null);
            } else {
                statement.setString(15, createdDate);
            }

            statement.executeUpdate();
            connection.commit();  // Explicitly commit transaction

        } catch (SQLException ex) {
            // Rollback on error
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException e) {
                    System.err.println("Failed to rollback transaction: " + e.getMessage());
                }
            }
            throw new IllegalStateException("Failed to save product", ex);
        } finally {
            // Close connection
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    System.err.println("Failed to close connection: " + e.getMessage());
                }
            }
        }
    }

    public void deleteProduct(Product product) {
        if (product == null) {
            return;
        }
        try (Connection connection = DatabaseManager.getConnection()) {
            // Delete product
            String deleteProductSql = "DELETE FROM products WHERE id = ?";
            try (PreparedStatement deleteProductStmt = connection.prepareStatement(deleteProductSql)) {
                deleteProductStmt.setString(1, product.getId());
                deleteProductStmt.executeUpdate();
                DatabaseManager.logDeletion("products", product.getId());
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to delete product", ex);
        }
        // Remove the physical image files from disk so they don't remain orphaned
        ImagePathUtil.deleteImageFiles(product.getImagePaths());
    }

    private String formatProductLabel(String code, String name) {
        if (code == null || code.isBlank()) {
            return name;
        }
        return code + " - " + name;
    }
}
