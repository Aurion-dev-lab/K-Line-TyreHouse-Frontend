package com.gui.kline.data;

import com.gui.kline.models.Worker;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LocalWorkerRepository {
    public List<Worker> loadWorkers() {
        String sql = "SELECT id, name, phone, role, rate, salary_type FROM workers ORDER BY created_at DESC";
        List<Worker> workers = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                workers.add(new Worker(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("phone"),
                        rs.getString("role"),
                        rs.getString("rate"),
                        rs.getString("salary_type")
                ));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load workers", ex);
        }
        return workers;
    }

    public void saveWorker(String name, String phone, String role, String rate, String salaryType) {
        if (name == null || name.isBlank()) {
            return;
        }
        String sql = "INSERT INTO workers (id, name, phone, role, rate, salary_type, created_at) " +
                "VALUES (UUID(), ?, ?, ?, ?, ?, NOW())";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name.trim());
            statement.setString(2, phone == null ? null : phone.trim());
            statement.setString(3, role == null ? null : role.trim());
            statement.setString(4, rate == null ? null : rate.trim());
            statement.setString(5, salaryType == null ? null : salaryType.trim());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to save worker", ex);
        }
    }

    public void updateWorker(String id, String name, String phone, String role, String rate, String salaryType) {
        if (id == null || id.isBlank()) {
            return;
        }
        String sql = "UPDATE workers SET name = ?, phone = ?, role = ?, rate = ?, salary_type = ? WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name == null ? null : name.trim());
            statement.setString(2, phone == null ? null : phone.trim());
            statement.setString(3, role == null ? null : role.trim());
            statement.setString(4, rate == null ? null : rate.trim());
            statement.setString(5, salaryType == null ? null : salaryType.trim());
            statement.setString(6, id);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to update worker", ex);
        }
    }

    public void deleteWorker(String id) {
        if (id == null || id.isBlank()) {
            return;
        }
        String deleteAttendance = "DELETE FROM worker_attendance WHERE worker_id = ?";
        String deleteWorker = "DELETE FROM workers WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(deleteAttendance)) {
                statement.setString(1, id);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(deleteWorker)) {
                statement.setString(1, id);
                statement.executeUpdate();
                DatabaseManager.logDeletion("workers", id);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to delete worker", ex);
        }
    }
}
