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
        String sql = "INSERT INTO customers (id, name, phone, created_at) VALUES (UUID(), ?, ?, NOW()) " +
                "ON DUPLICATE KEY UPDATE phone = VALUES(phone)";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name.trim());
            statement.setString(2, phone == null ? null : phone.trim());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to save customer", ex);
        }
    }

     public List<Product> loadProducts() {
         String sql = "SELECT id, product_code, name, category, buy_price, sell_price, stock FROM products ORDER BY product_code, name";
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
                         rs.getInt("stock")
                 );
                 products.add(product);
             }
         } catch (SQLException ex) {
             throw new IllegalStateException("Failed to load products", ex);
         }
         return products;
     }

    public Product findProductByCode(String productCode) {
        String sql = "SELECT id, product_code, name, category, buy_price, sell_price, stock FROM products WHERE product_code = ?";
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
                            rs.getInt("stock")
                    );
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load product by code", ex);
        }
        return null;
    }

      public Product findProductById(String productId) {
          String sql = "SELECT id, product_code, name, category, buy_price, sell_price, stock FROM products WHERE id = ?";
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
                              rs.getInt("stock")
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
          String sql = "SELECT id, product_code, name, category, buy_price, sell_price, stock FROM products WHERE category = ? ORDER BY product_code, name";
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
                              rs.getInt("stock")
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
          String sql = "UPDATE products SET stock = stock + ?, updated_at = NOW() WHERE id = ?";
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
        String sql = "INSERT INTO products (id, product_code, name, category, buy_price, sell_price, stock, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, NOW()) " +
                "ON DUPLICATE KEY UPDATE product_code = VALUES(product_code), category = VALUES(category), buy_price = VALUES(buy_price), " +
                "sell_price = VALUES(sell_price), stock = VALUES(stock), updated_at = NOW()";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, product.getId());
            statement.setString(2, product.getCode());
            statement.setString(3, product.getName());
            statement.setString(4, product.getCategory());
            statement.setDouble(5, product.getBuyPrice());
            statement.setDouble(6, product.getSellPrice());
            statement.setInt(7, product.getStock());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to save product", ex);
        }
    }

    private String formatProductLabel(String code, String name) {
        if (code == null || code.isBlank()) {
            return name;
        }
        return code + " - " + name;
    }
}
