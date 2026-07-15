package com.gui.kline.data;

import com.gui.kline.models.Customer;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for Customer operations with sync support.
 */
public class CustomerRepository {
    
    /**
     * Get all customers
     */
    public List<Customer> getAllCustomers() {
        List<Customer> customers = new ArrayList<>();
        String sql = "SELECT * FROM customers ORDER BY name ASC";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            
            while (rs.next()) {
                customers.add(mapCustomer(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load customers: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        return customers;
    }
    
    /**
     * Get unsynced customers (customers that haven't been synced yet)
     */
    public List<Customer> getUnsyncedCustomers() {
        List<Customer> customers = new ArrayList<>();
        String sql = "SELECT * FROM customers WHERE sync_status = false OR sync_status IS NULL OR synced_at IS NULL";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            
            while (rs.next()) {
                customers.add(mapCustomer(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load unsynced customers: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        return customers;
    }
    
    /**
     * Get customer by ID
     */
    public Customer getCustomerById(String id) {
        String sql = "SELECT * FROM customers WHERE id = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapCustomer(rs);
                }
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load customer: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Save or update a customer
     */
    public String saveCustomer(Customer customer) {
        String id = customer.getId() != null ? customer.getId() : java.util.UUID.randomUUID().toString();
        
        String sql = "INSERT INTO customers (id, name, phone, company_name, alternate_phone, email, address, " +
                "city, state, country, postal_code, tax_id, category, credit_limit, current_credit, " +
                "active, date_of_birth, notes, loyalty_program_id, loyalty_points, member_since, " +
                "sync_id, device_id, synced_at, sync_status, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now')) " +
                "ON CONFLICT(id) DO UPDATE SET name = excluded.name, phone = excluded.phone, " +
                "company_name = excluded.company_name, alternate_phone = excluded.alternate_phone, " +
                "email = excluded.email, address = excluded.address, city = excluded.city, " +
                "state = excluded.state, country = excluded.country, postal_code = excluded.postal_code, " +
                "tax_id = excluded.tax_id, category = excluded.category, credit_limit = excluded.credit_limit, " +
                "current_credit = excluded.current_credit, active = excluded.active, " +
                "date_of_birth = excluded.date_of_birth, notes = excluded.notes, " +
                "loyalty_program_id = excluded.loyalty_program_id, loyalty_points = excluded.loyalty_points, " +
                "member_since = excluded.member_since, sync_id = excluded.sync_id, " +
                "device_id = excluded.device_id, synced_at = excluded.synced_at, " +
                "sync_status = excluded.sync_status";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, id);
            statement.setString(2, customer.getName());
            statement.setString(3, customer.getPhone());
            statement.setString(4, customer.getCompanyName());
            statement.setString(5, customer.getAlternatePhone());
            statement.setString(6, customer.getEmail());
            statement.setString(7, customer.getAddress());
            statement.setString(8, customer.getCity());
            statement.setString(9, customer.getState());
            statement.setString(10, customer.getCountry());
            statement.setString(11, customer.getPostalCode());
            statement.setString(12, customer.getTaxId());
            statement.setString(13, customer.getCategory());
            statement.setDouble(14, customer.getCreditLimit());
            statement.setDouble(15, customer.getCurrentCredit());
            statement.setInt(16, customer.isActive() ? 1 : 0);
            statement.setString(17, customer.getDateOfBirth() != null ?
                customer.getDateOfBirth().toLocalDate().toString() : null);
            statement.setString(18, customer.getNotes());
            statement.setString(19, customer.getLoyaltyProgramId());
            statement.setDouble(20, customer.getLoyaltyPoints());
            statement.setString(21, customer.getMemberSince() != null ?
                customer.getMemberSince().toString() : null);
            statement.setString(22, customer.getSyncId());
            statement.setString(23, customer.getDeviceId());
            statement.setString(24, customer.getSyncedAt() != null ?
                customer.getSyncedAt().toString() : null);
            statement.setInt(25, customer.isSyncStatus() ? 1 : 0);
            
            statement.executeUpdate();
            
            return id;
            
        } catch (SQLException ex) {
            System.err.println("Failed to save customer: " + ex.getMessage());
            ex.printStackTrace();
            throw new RuntimeException("Failed to save customer", ex);
        }
    }
    
    /**
     * Mark a customer as synced
     */
    public void markAsSynced(String customerId) {
        String sql = "UPDATE customers SET sync_status = 1, synced_at = datetime('now') WHERE id = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, customerId);
            statement.executeUpdate();
            
        } catch (SQLException ex) {
            System.err.println("Failed to mark customer as synced: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * Delete a customer
     */
    public void deleteCustomer(String id) {
        String sql = "DELETE FROM customers WHERE id = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, id);
            statement.executeUpdate();
            
        } catch (SQLException ex) {
            System.err.println("Failed to delete customer: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * Map ResultSet to Customer object
     */
    private Customer mapCustomer(ResultSet rs) throws SQLException {
        Customer customer = new Customer();
        customer.setId(rs.getString("id"));
        customer.setName(rs.getString("name"));
        customer.setPhone(rs.getString("phone"));
        customer.setCompanyName(rs.getString("company_name"));
        customer.setAlternatePhone(rs.getString("alternate_phone"));
        customer.setEmail(rs.getString("email"));
        customer.setAddress(rs.getString("address"));
        customer.setCity(rs.getString("city"));
        customer.setState(rs.getString("state"));
        customer.setCountry(rs.getString("country"));
        customer.setPostalCode(rs.getString("postal_code"));
        customer.setTaxId(rs.getString("tax_id"));
        customer.setCategory(rs.getString("category"));
        customer.setCreditLimit(rs.getDouble("credit_limit"));
        customer.setCurrentCredit(rs.getDouble("current_credit"));
        customer.setActive(rs.getBoolean("active"));
        customer.setNotes(rs.getString("notes"));
        customer.setLoyaltyProgramId(rs.getString("loyalty_program_id"));
        customer.setLoyaltyPoints(rs.getDouble("loyalty_points"));
        
        Timestamp dobTimestamp = rs.getTimestamp("date_of_birth");
        if (dobTimestamp != null) {
            customer.setDateOfBirth(dobTimestamp.toLocalDateTime());
        }
        
        Timestamp memberSinceTimestamp = rs.getTimestamp("member_since");
        if (memberSinceTimestamp != null) {
            customer.setMemberSince(memberSinceTimestamp.toLocalDateTime());
        }
        
        // Sync fields
        customer.setSyncId(rs.getString("sync_id"));
        customer.setDeviceId(rs.getString("device_id"));
        customer.setSyncedAt(rs.getTimestamp("synced_at") != null ? 
            rs.getTimestamp("synced_at").toLocalDateTime() : null);
        customer.setSyncStatus(rs.getBoolean("sync_status"));
        customer.setCreatedAt(rs.getTimestamp("created_at") != null ? 
            rs.getTimestamp("created_at").toLocalDateTime() : null);
        customer.setUpdatedAt(rs.getTimestamp("updated_at") != null ? 
            rs.getTimestamp("updated_at").toLocalDateTime() : null);
        
        return customer;
    }
}