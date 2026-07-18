package com.gui.kline.data;

import com.gui.kline.models.Worker;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for Worker operations with sync support.
 */
public class WorkerRepository {
    
    /**
     * Get all workers
     */
    public List<Worker> getAllWorkers() {
        List<Worker> workers = new ArrayList<>();
        String sql = "SELECT * FROM workers ORDER BY name ASC";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            
            while (rs.next()) {
                workers.add(mapWorker(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load workers: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        return workers;
    }
    
    /**
     * Get unsynced workers (workers that haven't been synced yet)
     */
    public List<Worker> getUnsyncedWorkers() {
        List<Worker> workers = new ArrayList<>();
        String sql = "SELECT * FROM workers WHERE sync_status = false OR sync_status IS NULL OR synced_at IS NULL";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            
            while (rs.next()) {
                workers.add(mapWorker(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load unsynced workers: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        return workers;
    }
    
    /**
     * Get worker by ID
     */
    public Worker getWorkerById(String id) {
        String sql = "SELECT * FROM workers WHERE id = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapWorker(rs);
                }
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load worker: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Save or update a worker
     */
    public String saveWorker(Worker worker) {
        String id = worker.getId() != null ? worker.getId() : java.util.UUID.randomUUID().toString();
        
        String sql = "INSERT INTO workers (id, name, phone, role, rate, created_at, salary_type, " +
                "sync_status, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, NOW(), ?, ?, NOW()) " +
                "ON DUPLICATE KEY UPDATE name = VALUES(name), phone = VALUES(phone), " +
                "role = VALUES(role), rate = VALUES(rate), salary_type = VALUES(salary_type), " +
                "" +
                "sync_status = VALUES(sync_status), " +
                "updated_at = NOW()";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, id);
            statement.setString(2, worker.getName());
            statement.setString(3, worker.getPhone());
            statement.setString(4, worker.getRole());
            statement.setString(5, worker.getRate());
            statement.setString(6, worker.getSalaryType());
            statement.setBoolean(7, worker.isSyncStatus());
            
            statement.executeUpdate();
            
            return id;
            
        } catch (SQLException ex) {
            System.err.println("Failed to save worker: " + ex.getMessage());
            ex.printStackTrace();
            throw new RuntimeException("Failed to save worker", ex);
        }
    }
    
    /**
     * Mark a worker as synced
     */
    public void markAsSynced(String workerId) {
        String sql = "UPDATE workers SET sync_status = true WHERE id = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, workerId);
            statement.executeUpdate();
            
        } catch (SQLException ex) {
            System.err.println("Failed to mark worker as synced: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * Delete a worker
     */
    public void deleteWorker(String id) {
        String sql = "DELETE FROM workers WHERE id = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, id);
            statement.executeUpdate();
            
        } catch (SQLException ex) {
            System.err.println("Failed to delete worker: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * Map ResultSet to Worker object
     */
    private Worker mapWorker(ResultSet rs) throws SQLException {
        Worker worker = new Worker();
        worker.setId(rs.getString("id"));
        worker.setName(rs.getString("name"));
        worker.setPhone(rs.getString("phone"));
        worker.setRole(rs.getString("role"));
        worker.setRate(rs.getString("rate"));
        worker.setSalaryType(rs.getString("salary_type"));
        
        // Sync fields
        worker.setSyncStatus(rs.getBoolean("sync_status"));
        worker.setCreatedAt(rs.getTimestamp("created_at") != null ? 
            rs.getTimestamp("created_at").toLocalDateTime() : null);
        worker.setUpdatedAt(rs.getTimestamp("updated_at") != null ? 
            rs.getTimestamp("updated_at").toLocalDateTime() : null);
        
        return worker;
    }
}