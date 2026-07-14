package com.gui.kline.data;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LocalSalaryAdvanceRepository {
    public String saveAdvance(String workerId, String workerName, LocalDate date, double amount, String note) {
        String id = UUID.randomUUID().toString();
        String sql = "INSERT INTO salary_advances (id, worker_id, worker, amount, advance_date, note, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, NOW())";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.setString(2, workerId);
            statement.setString(3, workerName);
            statement.setDouble(4, amount);
            statement.setDate(5, Date.valueOf(date));
            statement.setString(6, note == null ? null : note.trim());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to save salary advance", ex);
        }
        return id;
    }

    public void deleteAdvance(String id) {
        if (id == null || id.isBlank()) {
            return;
        }
        String sql = "DELETE FROM salary_advances WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to delete salary advance", ex);
        }
    }

    public Map<String, Double> sumAdvancesByWorkerId(LocalDate from, LocalDate to) {
        String sql = "SELECT worker_id, SUM(amount) AS total FROM salary_advances " +
                "WHERE advance_date BETWEEN ? AND ? AND worker_id IS NOT NULL " +
                "GROUP BY worker_id";
        Map<String, Double> totals = new HashMap<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(from));
            statement.setDate(2, Date.valueOf(to));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    totals.put(rs.getString("worker_id"), rs.getDouble("total"));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load salary advances", ex);
        }
        return totals;
    }

    public Map<String, Double> sumAdvancesByWorkerName(LocalDate from, LocalDate to) {
        String sql = "SELECT worker, SUM(amount) AS total FROM salary_advances " +
                "WHERE advance_date BETWEEN ? AND ? AND worker IS NOT NULL " +
                "GROUP BY worker";
        Map<String, Double> totals = new HashMap<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(from));
            statement.setDate(2, Date.valueOf(to));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    totals.put(rs.getString("worker"), rs.getDouble("total"));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load salary advances", ex);
        }
        return totals;
    }
}

