package com.gui.kline.data;

import com.gui.kline.models.LedgerEntry;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LocalWorkerCreditRepository {
    public String saveCredit(String workerId, String workerName, LocalDate date, double amount, String note, String type) {
        String id = UUID.randomUUID().toString();
        String sql = "INSERT INTO worker_credits (id, worker_id, worker, amount, credit_type, credit_date, note, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.setString(2, workerId);
            statement.setString(3, workerName);
            statement.setDouble(4, amount);
            statement.setString(5, type);
            statement.setDate(6, Date.valueOf(date));
            statement.setString(7, note == null ? null : note.trim());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to save worker credit", ex);
        }
        return id;
    }

    public void deleteCredit(String id) {
        if (id == null || id.isBlank()) {
            return;
        }
        String sql = "DELETE FROM worker_credits WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.executeUpdate();
            DatabaseManager.logDeletion("worker_credits", id);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to delete worker credit", ex);
        }
    }

    public void updateCredit(String id, String workerId, String workerName, LocalDate date, double amount, String note, String type) {
        if (id == null || id.isBlank()) return;
        String sql = "UPDATE worker_credits SET worker_id = ?, worker = ?, amount = ?, credit_type = ?, credit_date = ?, note = ?, created_at = NOW() WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, workerId);
            statement.setString(2, workerName);
            statement.setDouble(3, amount);
            statement.setString(4, type);
            statement.setDate(5, Date.valueOf(date));
            statement.setString(6, note == null ? null : note.trim());
            statement.setString(7, id);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to update worker credit", ex);
        }
    }

    public LedgerEntry loadById(String id) {
        String sql = "SELECT id, worker, credit_date, credit_type, note, amount FROM worker_credits WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    String type = rs.getString("credit_type");
                    return new LedgerEntry(
                            rs.getString("id"),
                            rs.getDate("credit_date").toLocalDate(),
                            rs.getString("worker"),
                            type == null ? "CREDIT" : type,
                            rs.getString("note"),
                            rs.getDouble("amount")
                    );
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load worker credit", ex);
        }
        return null;
    }

    public List<LedgerEntry> loadLedger(LocalDate from, LocalDate to) {
        String sql = "SELECT id, worker, credit_date, credit_type, note, amount " +
                "FROM worker_credits WHERE credit_date BETWEEN ? AND ? ORDER BY credit_date DESC";
        List<LedgerEntry> entries = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(from));
            statement.setDate(2, Date.valueOf(to));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String type = rs.getString("credit_type");
                    entries.add(new LedgerEntry(
                            rs.getString("id"),
                            rs.getDate("credit_date").toLocalDate(),
                            rs.getString("worker"),
                            type == null ? "CREDIT" : type,
                            rs.getString("note"),
                            rs.getDouble("amount")
                    ));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load credit ledger", ex);
        }
        return entries;
    }

    public Map<String, Double> balanceByWorkerId(LocalDate from, LocalDate to) {
        String sql = "SELECT worker_id, credit_type, SUM(amount) AS total FROM worker_credits " +
                "WHERE credit_date <= ? AND worker_id IS NOT NULL " +
                "GROUP BY worker_id, credit_type";
        Map<String, Double> totals = new HashMap<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(to));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String workerId = rs.getString("worker_id");
                    double amount = rs.getDouble("total");
                    String type = rs.getString("credit_type");
                    double signed = "SETTLEMENT".equalsIgnoreCase(type) ? -amount : amount;
                    totals.put(workerId, totals.getOrDefault(workerId, 0.0) + signed);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load credit balances", ex);
        }
        return totals;
    }

    public Map<String, Double> balanceByWorkerName(LocalDate from, LocalDate to) {
        String sql = "SELECT worker, credit_type, SUM(amount) AS total FROM worker_credits " +
                "WHERE credit_date <= ? AND worker IS NOT NULL " +
                "GROUP BY worker, credit_type";
        Map<String, Double> totals = new HashMap<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(to));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String worker = rs.getString("worker");
                    double amount = rs.getDouble("total");
                    String type = rs.getString("credit_type");
                    double signed = "SETTLEMENT".equalsIgnoreCase(type) ? -amount : amount;
                    totals.put(worker, totals.getOrDefault(worker, 0.0) + signed);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load credit balances", ex);
        }
        return totals;
    }
}
