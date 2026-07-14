package com.gui.kline.data;

import com.gui.kline.models.WorkerAttendance;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for WorkerAttendance operations with sync support.
 */
public class WorkerAttendanceRepository {
    
    /**
     * Get all worker attendances
     */
    public List<WorkerAttendance> getAllAttendances() {
        List<WorkerAttendance> attendances = new ArrayList<>();
        String sql = "SELECT * FROM worker_attendance ORDER BY attendance_date DESC";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            
            while (rs.next()) {
                attendances.add(mapWorkerAttendance(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load worker attendances: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        return attendances;
    }
    
    /**
     * Get unsynced worker attendances (attendances that haven't been synced yet)
     */
    public List<WorkerAttendance> getUnsyncedAttendances() {
        List<WorkerAttendance> attendances = new ArrayList<>();
        String sql = "SELECT * FROM worker_attendance WHERE sync_status = false OR sync_status IS NULL OR synced_at IS NULL";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            
            while (rs.next()) {
                attendances.add(mapWorkerAttendance(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load unsynced worker attendances: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        return attendances;
    }
    
    /**
     * Get worker attendance by ID
     */
    public WorkerAttendance getWorkerAttendanceById(String id) {
        String sql = "SELECT * FROM worker_attendance WHERE id = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapWorkerAttendance(rs);
                }
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load worker attendance: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Save or update a worker attendance
     */
    public String saveWorkerAttendance(WorkerAttendance attendance) {
        String id = attendance.getId() != null ? attendance.getId() : java.util.UUID.randomUUID().toString();
        
        String sql = "INSERT INTO worker_attendance (id, worker_id, attendance_date, status, created_at, updated_at, " +
                "sync_id, device_id, synced_at, sync_status) " +
                "VALUES (?, ?, ?, ?, NOW(), NOW(), ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE worker_id = VALUES(worker_id), attendance_date = VALUES(attendance_date), " +
                "status = VALUES(status), sync_id = VALUES(sync_id), device_id = VALUES(device_id), " +
                "synced_at = VALUES(synced_at), sync_status = VALUES(sync_status), " +
                "updated_at = NOW()";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, id);
            statement.setString(2, attendance.getWorkerId());
            statement.setDate(3, attendance.getDate() != null ? Date.valueOf(attendance.getDate()) : null);
            statement.setString(4, attendance.getStatus());
            statement.setString(5, attendance.getSyncId());
            statement.setString(6, attendance.getDeviceId());
            statement.setTimestamp(7, attendance.getSyncedAt() != null ? Timestamp.valueOf(attendance.getSyncedAt()) : null);
            statement.setBoolean(8, attendance.isSyncStatus());
            
            statement.executeUpdate();
            
            return id;
            
        } catch (SQLException ex) {
            System.err.println("Failed to save worker attendance: " + ex.getMessage());
            ex.printStackTrace();
            throw new RuntimeException("Failed to save worker attendance", ex);
        }
    }
    
    /**
     * Mark a worker attendance as synced
     */
    public void markAsSynced(String attendanceId) {
        String sql = "UPDATE worker_attendance SET sync_status = true, synced_at = NOW() WHERE id = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, attendanceId);
            statement.executeUpdate();
            
        } catch (SQLException ex) {
            System.err.println("Failed to mark worker attendance as synced: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * Delete a worker attendance
     */
    public void deleteWorkerAttendance(String id) {
        String sql = "DELETE FROM worker_attendance WHERE id = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, id);
            statement.executeUpdate();
            
        } catch (SQLException ex) {
            System.err.println("Failed to delete worker attendance: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * Map ResultSet to WorkerAttendance object
     */
    private WorkerAttendance mapWorkerAttendance(ResultSet rs) throws SQLException {
        WorkerAttendance attendance = new WorkerAttendance();
        attendance.setId(rs.getString("id"));
        attendance.setWorkerId(rs.getString("worker_id"));
        
        Date attendanceDate = rs.getDate("attendance_date");
        if (attendanceDate != null) {
            attendance.setDate(attendanceDate.toLocalDate());
        }
        
        attendance.setStatus(rs.getString("status"));
        
        // Sync fields
        attendance.setSyncId(rs.getString("sync_id"));
        attendance.setDeviceId(rs.getString("device_id"));
        attendance.setSyncedAt(rs.getTimestamp("synced_at") != null ? 
            rs.getTimestamp("synced_at").toLocalDateTime() : null);
        attendance.setSyncStatus(rs.getBoolean("sync_status"));
        attendance.setCreatedAt(rs.getTimestamp("created_at") != null ? 
            rs.getTimestamp("created_at").toLocalDateTime() : null);
        attendance.setUpdatedAt(rs.getTimestamp("updated_at") != null ? 
            rs.getTimestamp("updated_at").toLocalDateTime() : null);
        
        return attendance;
    }
}