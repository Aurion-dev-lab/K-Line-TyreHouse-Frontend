package com.gui.kline.data;

import com.gui.kline.models.Product;

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
        String sql = "SELECT DISTINCT name FROM customers ORDER BY name";
        List<String> names = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                names.add(rs.getString("name"));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to read customers", ex);
        }
        return names;
    }

    public void saveCustomer(String name, String phone) {
        if (name == null || name.isBlank()) {
            return;
        }
        // Try insert; if duplicate (name+phone), update phone
        String sql = "INSERT INTO customers (id, name, phone, created_at) VALUES (?, ?, ?, datetime('now')) " +
                "ON CONFLICT(name, phone) DO UPDATE SET phone = excluded.phone";
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

      public List<Product> loadProducts() {
          String sql = "SELECT id, product_code, name, category, buy_price, sell_price, stock, minimum_stock_alert, " +
                      "brand, description, vehicle_type, material, supplier_name, created_at FROM products ORDER BY product_code, name";
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
                          new java.util.ArrayList<>()
                  );
                  products.add(product);
              }
              
              // Load images for all products
              loadProductImages(connection, products);
              
          } catch (SQLException ex) {
              throw new IllegalStateException("Failed to load products", ex);
          }
          return products;
      }
      
      private void loadProductImages(Connection connection, List<Product> products) throws SQLException {
          if (products.isEmpty()) return;
          
          // Build list of product IDs
          StringBuilder idsBuilder = new StringBuilder();
          for (Product p : products) {
              if (idsBuilder.length() > 0) idsBuilder.append(",");
              idsBuilder.append("'").append(p.getId()).append("'");
          }
          
          String sql = "SELECT product_id, image_path FROM product_images WHERE product_id IN (" + idsBuilder + ")";
          try (PreparedStatement statement = connection.prepareStatement(sql);
               ResultSet rs = statement.executeQuery()) {
              while (rs.next()) {
                  String productId = rs.getString("product_id");
                  String imagePath = rs.getString("image_path");
                  
                  // Find product and add image
                  for (Product p : products) {
                      if (p.getId().equals(productId)) {
                          p.addImagePath(imagePath);
                          break;
                      }
                  }
              }
          }
      }

    public Product findProductByCode(String productCode) {
        String sql = "SELECT id, product_code, name, category, buy_price, sell_price, stock, minimum_stock_alert, " +
                    "brand, description, vehicle_type, material, supplier_name, created_at, image_path FROM products WHERE product_code = ?";
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
                            rs.getString("image_path") != null ? java.util.Collections.singletonList(rs.getString("image_path")) : new java.util.ArrayList<>()
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
                      "brand, description, vehicle_type, material, supplier_name, created_at, image_path FROM products WHERE id = ?";
          try (Connection connection = DatabaseManager.getConnection();
               PreparedStatement statement = connection.prepareStatement(sql)) {
              statement.setString(1, productId);
              try (ResultSet rs = statement.executeQuery()) {
                  if (rs.next()) {
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
                              rs.getString("image_path") != null ? java.util.Collections.singletonList(rs.getString("image_path")) : new java.util.ArrayList<>()
                      );
                      return product;
                  }
              }
          } catch (SQLException ex) {
              throw new IllegalStateException("Failed to load product", ex);
          }
          return null;
      }

      public List<Product> getProductsByCategory(String category) {
          String sql = "SELECT id, product_code, name, category, buy_price, sell_price, stock, minimum_stock_alert, " +
                      "brand, description, vehicle_type, material, supplier_name, created_at, image_path FROM products WHERE category = ? ORDER BY product_code, name";
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
                              rs.getString("image_path") != null ? java.util.Collections.singletonList(rs.getString("image_path")) : new java.util.ArrayList<>()
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
          String sql = "UPDATE products SET stock = stock + ?, updated_at = datetime('now') WHERE id = ?";
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
        // SQLite: INSERT OR REPLACE handles upsert via the UNIQUE index on name.
        // We preserve created_at by reading it first if the row exists.
        String sql = "INSERT INTO products (id, product_code, name, category, buy_price, sell_price, stock, minimum_stock_alert, " +
                "brand, description, vehicle_type, material, supplier_name, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, COALESCE(?, datetime('now')), datetime('now')) " +
                "ON CONFLICT(name) DO UPDATE SET " +
                "product_code = excluded.product_code, category = excluded.category, buy_price = excluded.buy_price, " +
                "sell_price = excluded.sell_price, stock = excluded.stock, minimum_stock_alert = excluded.minimum_stock_alert, " +
                "brand = excluded.brand, description = excluded.description, vehicle_type = excluded.vehicle_type, " +
                "material = excluded.material, supplier_name = excluded.supplier_name, updated_at = datetime('now')";
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
            
            // Handle empty created_date - use null so COALESCE kicks in
            String createdDate = product.getCreatedDate();
            if (createdDate == null || createdDate.isEmpty() || createdDate.equals("null")) {
                statement.setString(14, null);
            } else {
                statement.setString(14, createdDate);
            }
            
            statement.executeUpdate();
            
            // Save product images
            saveProductImages(connection, product);
            
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
    
    private void saveProductImages(Connection connection, Product product) throws SQLException {
        // Delete existing images for this product
        String deleteSql = "DELETE FROM product_images WHERE product_id = ?";
        try (PreparedStatement deleteStmt = connection.prepareStatement(deleteSql)) {
            deleteStmt.setString(1, product.getId());
            deleteStmt.executeUpdate();
        }
        
        // Insert new images
        String insertSql = "INSERT INTO product_images (id, product_id, image_path, created_at) VALUES (?, ?, ?, datetime('now'))";
        try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
            for (String imagePath : product.getImagePaths()) {
                insertStmt.setString(1, java.util.UUID.randomUUID().toString());
                insertStmt.setString(2, product.getId());
                insertStmt.setString(3, imagePath);
                insertStmt.addBatch();
            }
            insertStmt.executeBatch();
        }
    }

    public void deleteProduct(Product product) {
        if (product == null) {
            return;
        }
        try (Connection connection = DatabaseManager.getConnection()) {
            // Delete product images first
            String deleteImagesSql = "DELETE FROM product_images WHERE product_id = ?";
            try (PreparedStatement deleteImagesStmt = connection.prepareStatement(deleteImagesSql)) {
                deleteImagesStmt.setString(1, product.getId());
                deleteImagesStmt.executeUpdate();
            }
            
            // Delete product
            String deleteProductSql = "DELETE FROM products WHERE id = ?";
            try (PreparedStatement deleteProductStmt = connection.prepareStatement(deleteProductSql)) {
                deleteProductStmt.setString(1, product.getId());
                deleteProductStmt.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to delete product", ex);
        }
    }

    private String formatProductLabel(String code, String name) {
        if (code == null || code.isBlank()) {
            return name;
        }
        return code + " - " + name;
    }
}
