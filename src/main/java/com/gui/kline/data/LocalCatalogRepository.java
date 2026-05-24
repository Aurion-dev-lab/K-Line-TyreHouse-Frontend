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
        String sql = "SELECT name FROM products ORDER BY name";
        List<String> names = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                names.add(rs.getString("name"));
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
        String sql = "SELECT id, name, category, buy_price, sell_price, stock FROM products ORDER BY name";
        List<Product> products = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                Product product = new Product(
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getDouble("buy_price"),
                        rs.getDouble("sell_price"),
                        rs.getInt("stock")
                );
                product.setId(rs.getString("id"));
                products.add(product);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load products", ex);
        }
        return products;
    }

    public void saveProduct(Product product) {
        if (product == null) {
            return;
        }
        String sql = "INSERT INTO products (id, name, category, buy_price, sell_price, stock, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, NOW()) " +
                "ON DUPLICATE KEY UPDATE category = VALUES(category), buy_price = VALUES(buy_price), " +
                "sell_price = VALUES(sell_price), stock = VALUES(stock), updated_at = NOW()";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, product.getId());
            statement.setString(2, product.getName());
            statement.setString(3, product.getCategory());
            statement.setDouble(4, product.getBuyPrice());
            statement.setDouble(5, product.getSellPrice());
            statement.setInt(6, product.getStock());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to save product", ex);
        }
    }
}
