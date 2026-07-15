package com.gui.kline.data;

import com.gui.kline.models.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for Product operations with sync support.
 */
public class ProductRepository {
    
    /**
     * Get all products
     */
    public List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products ORDER BY name ASC";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            
            while (rs.next()) {
                Product product = mapProduct(rs);
                products.add(product);
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load products: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        return products;
    }
    
    /**
     * Get unsynced products (products that haven't been synced yet)
     */
    public List<Product> getUnsyncedProducts() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE sync_status = false OR sync_status IS NULL OR synced_at IS NULL";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            
            while (rs.next()) {
                Product product = mapProduct(rs);
                products.add(product);
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load unsynced products: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        return products;
    }
    
    /**
     * Get product by ID
     */
    public Product getProductById(String id) {
        String sql = "SELECT * FROM products WHERE id = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapProduct(rs);
                }
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load product: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Save or update a product
     */
    public String saveProduct(Product product) {
        String id = product.getId();
        if (id == null || id.isBlank()) {
            id = java.util.UUID.randomUUID().toString();
        }
        
        String sql = "INSERT INTO products (id, product_code, name, category, buy_price, sell_price, stock) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(id) DO UPDATE SET product_code = excluded.product_code, name = excluded.name, " +
                "category = excluded.category, buy_price = excluded.buy_price, " +
                "sell_price = excluded.sell_price, stock = excluded.stock";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, id);
            statement.setString(2, product.getCode());
            statement.setString(3, product.getName());
            statement.setString(4, product.getCategory());
            statement.setDouble(5, product.getBuyPrice());
            statement.setDouble(6, product.getSellPrice());
            statement.setInt(7, product.getStock());
            
            statement.executeUpdate();
            
            return id;
            
        } catch (SQLException ex) {
            System.err.println("Failed to save product: " + ex.getMessage());
            ex.printStackTrace();
            throw new RuntimeException("Failed to save product", ex);
        }
    }
    
    /**
     * Mark a product as synced
     */
    public void markAsSynced(String productId) {
        String sql = "UPDATE products SET sync_status = 1, synced_at = datetime('now') WHERE id = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, productId);
            statement.executeUpdate();
            
        } catch (SQLException ex) {
            System.err.println("Failed to mark product as synced: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * Delete a product
     */
    public void deleteProduct(String id) {
        String sql = "DELETE FROM products WHERE id = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, id);
            statement.executeUpdate();
            
        } catch (SQLException ex) {
            System.err.println("Failed to delete product: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * Map ResultSet to Product object
     */
    private Product mapProduct(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String code = rs.getString("product_code");
        String name = rs.getString("name");
        String category = rs.getString("category");
        double buyPrice = rs.getDouble("buy_price");
        double sellPrice = rs.getDouble("sell_price");
        int stock = rs.getInt("stock");
        
        Product product = new Product(id, code, name, category, buyPrice, sellPrice, stock);
        
        return product;
    }
}