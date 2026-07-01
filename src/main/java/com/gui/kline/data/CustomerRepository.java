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
                "sync_id, device_id, synced_at, sync_status, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW()) " +
                "ON DUPLICATE KEY UPDATE name = VALUES(name), phone = VALUES(phone), " +
                "company_name = VALUES(company_name), alternate_phone = VALUES(alternate_phone), " +
                "email = VALUES(email), address = VALUES(address), city = VALUES(city), " +
                "state = VALUES(state), country = VALUES(country), postal_code = VALUES(postal_code), " +
                "tax_id = VALUES(tax_id), category = VALUES(category), credit_limit = VALUES(credit_limit), " +
                "current_credit = VALUES(current_credit), active = VALUES(active), " +
                "date_of_birth = VALUES(date_of_birth), notes = VALUES(notes), " +
                "loyalty_program_id = VALUES(loyalty_program_id), loyalty_points = VALUES(loyalty_points), " +
                "member_since = VALUES(member_since), sync_id = VALUES(sync_id), " +
                "device_id = VALUES(device_id), synced_at = VALUES(synced_at), " +
                "sync_status = VALUES(sync_status), updated_at = NOW()";
        
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
            statement.setBoolean(16, customer.isActive());
            statement.setTimestamp(17, customer.getDateOfBirth() != null ? 
                Timestamp.valueOf(customer.getDateOfBirth().toLocalDate().atStartOfDay()) : null);
            statement.setString(18, customer.getNotes());
            statement.setString(19, customer.getLoyaltyProgramId());
            statement.setDouble(20, customer.getLoyaltyPoints());
            statement.setTimestamp(21, customer.getMemberSince() != null ? 
                Timestamp.valueOf(customer.getMemberSince()) : null);
            statement.setString(22, customer.getSyncId());
            statement.setString(23, customer.getDeviceId());
            statement.setTimestamp(24, customer.getSyncedAt() != null ? 
                Timestamp.valueOf(customer.getSyncedAt()) : null);
            statement.setBoolean(25, customer.isSyncStatus());
            
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
        String sql = "UPDATE customers SET sync_status = true, synced_at = NOW() WHERE id = ?";
        
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