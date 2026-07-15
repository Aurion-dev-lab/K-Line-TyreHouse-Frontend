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
                "sync_id, device_id, synced_at, sync_status) " +
                "VALUES (?, ?, ?, ?, ?, datetime('now'), ?, ?, ?, ?, ?) " +
                "ON CONFLICT(id) DO UPDATE SET name = excluded.name, phone = excluded.phone, " +
                "role = excluded.role, rate = excluded.rate, salary_type = excluded.salary_type, " +
                "sync_id = excluded.sync_id, device_id = excluded.device_id, " +
                "synced_at = excluded.synced_at, sync_status = excluded.sync_status";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, id);
            statement.setString(2, worker.getName());
            statement.setString(3, worker.getPhone());
            statement.setString(4, worker.getRole());
            statement.setString(5, worker.getRate());
            statement.setString(6, worker.getSalaryType());
            statement.setString(7, worker.getSyncId());
            statement.setString(8, worker.getDeviceId());
            statement.setString(9, worker.getSyncedAt() != null ? worker.getSyncedAt().toString() : null);
            statement.setInt(10, worker.isSyncStatus() ? 1 : 0);
            
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
        String sql = "UPDATE workers SET sync_status = 1, synced_at = datetime('now') WHERE id = ?";
        
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
        worker.setSyncId(rs.getString("sync_id"));
        worker.setDeviceId(rs.getString("device_id"));
        worker.setSyncedAt(rs.getTimestamp("synced_at") != null ? 
            rs.getTimestamp("synced_at").toLocalDateTime() : null);
        worker.setSyncStatus(rs.getBoolean("sync_status"));
        worker.setCreatedAt(rs.getTimestamp("created_at") != null ? 
            rs.getTimestamp("created_at").toLocalDateTime() : null);
        worker.setUpdatedAt(rs.getTimestamp("updated_at") != null ? 
            rs.getTimestamp("updated_at").toLocalDateTime() : null);
        
        return worker;
    }
}