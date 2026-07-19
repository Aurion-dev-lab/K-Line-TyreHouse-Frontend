package com.gui.kline.data;

import com.gui.kline.models.WorkerAttendance;
import com.gui.kline.models.WorkerAttendanceHistory;
import com.gui.kline.models.WorkerMonthlySummary;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class LocalWorkerAttendanceRepository {
    public List<WorkerAttendance> loadAttendanceForDate(LocalDate date) {
        String sql = "SELECT w.id, w.name, w.role, w.rate, w.salary_type, a.attendance_date, a.status " +
                "FROM workers w " +
                "LEFT JOIN worker_attendance a ON a.worker_id = w.id AND a.attendance_date = ? " +
                "ORDER BY w.created_at DESC";
        List<WorkerAttendance> rows = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(date));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    LocalDate attendanceDate = rs.getDate("attendance_date") == null
                            ? date
                            : rs.getDate("attendance_date").toLocalDate();
                    rows.add(new WorkerAttendance(
                            rs.getString("id"),
                            rs.getString("name"),
                            rs.getString("role"),
                            rs.getString("rate"),
                            rs.getString("salary_type"),
                            attendanceDate,
                            rs.getString("status")
                    ));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load attendance", ex);
        }
        return rows;
    }

    public void upsertAttendance(String workerId, LocalDate date, String status) {
        String sql = "INSERT INTO worker_attendance (id, worker_id, attendance_date, status, created_at, updated_at) " +
                "VALUES (UUID(), ?, ?, ?, NOW(), NOW()) " +
                "ON DUPLICATE KEY UPDATE status = VALUES(status), sync_status = 0, updated_at = NOW()";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, workerId);
            statement.setDate(2, Date.valueOf(date));
            statement.setString(3, status);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to save attendance", ex);
        }
    }

    public List<WorkerAttendanceHistory> loadHistory(LocalDate from, LocalDate to, String nameFilter) {
        String sql = "SELECT a.attendance_date, w.name, a.status " +
                "FROM worker_attendance a " +
                "JOIN workers w ON w.id = a.worker_id " +
                "WHERE a.attendance_date BETWEEN ? AND ? AND w.name LIKE ? " +
                "ORDER BY a.attendance_date DESC";
        List<WorkerAttendanceHistory> rows = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(from));
            statement.setDate(2, Date.valueOf(to));
            statement.setString(3, "%" + (nameFilter == null ? "" : nameFilter.trim()) + "%");
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new WorkerAttendanceHistory(
                            rs.getDate("attendance_date").toLocalDate(),
                            rs.getString("name"),
                            rs.getString("status")
                    ));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load attendance history", ex);
        }
        return rows;
    }

    public List<WorkerMonthlySummary> loadMonthlySummary(YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        String sql = "SELECT w.name, w.rate, " +
                "SUM(CASE a.status WHEN 'PRESENT' THEN 1 WHEN 'HALF_DAY' THEN 0.5 ELSE 0 END) AS days " +
                "FROM worker_attendance a " +
                "JOIN workers w ON w.id = a.worker_id " +
                "WHERE a.attendance_date BETWEEN ? AND ? " +
                "GROUP BY w.id, w.name, w.rate " +
                "ORDER BY w.name";
        List<WorkerMonthlySummary> rows = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(start));
            statement.setDate(2, Date.valueOf(end));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    double days = rs.getDouble("days");
                    double rate = parseRate(rs.getString("rate"));
                    rows.add(new WorkerMonthlySummary(
                            rs.getString("name"),
                            days,
                            days * rate
                    ));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load monthly summary", ex);
        }
        return rows;
    }

    private double parseRate(String rateText) {
        if (rateText == null || rateText.isBlank()) {
            return 0;
        }
        try {
            return Double.parseDouble(rateText.replace(",", ""));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    /**
     * Delete a single attendance record for a worker on a specific date
     */
    public void deleteAttendance(String workerId, LocalDate date) {
        String sql = "DELETE FROM worker_attendance WHERE worker_id = ? AND attendance_date = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, workerId);
            statement.setDate(2, Date.valueOf(date));
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to delete attendance", ex);
        }
    }

    /**
     * Delete all attendance records within a date range
     */
    public void deleteAttendanceForPeriod(LocalDate from, LocalDate to) {
        String sql = "DELETE FROM worker_attendance WHERE attendance_date BETWEEN ? AND ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(from));
            statement.setDate(2, Date.valueOf(to));
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to delete attendance for period", ex);
        }
    }
}

