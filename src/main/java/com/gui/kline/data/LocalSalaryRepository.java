package com.gui.kline.data;

import com.gui.kline.models.WorkerSalary;

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

public class LocalSalaryRepository {
    private final LocalSalaryAdvanceRepository advanceRepository = new LocalSalaryAdvanceRepository();
    private final LocalWorkerCreditRepository creditRepository = new LocalWorkerCreditRepository();

    public List<WorkerSalary> loadWorkerSalaries(LocalDate from, LocalDate to) {
        Map<String, Double> advancesById = advanceRepository.sumAdvancesByWorkerId(from, to);
        Map<String, Double> advancesByName = advanceRepository.sumAdvancesByWorkerName(from, to);
        Map<String, Double> creditsById = creditRepository.balanceByWorkerId(from, to);
        Map<String, Double> creditsByName = creditRepository.balanceByWorkerName(from, to);
        Map<String, Double> paidAmountsByWorkerId = loadPaidAmountsByWorkerId(from, to);

        String sql = "SELECT w.id, w.name, w.role, w.rate, " +
                "SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) AS present, " +
                "SUM(CASE WHEN a.status = 'HALF_DAY' THEN 1 ELSE 0 END) AS half_day, " +
                "SUM(CASE WHEN a.status = 'ABSENT' THEN 1 ELSE 0 END) AS absent " +
                "FROM workers w " +
                "LEFT JOIN worker_attendance a ON a.worker_id = w.id AND a.attendance_date BETWEEN ? AND ? " +
                "GROUP BY w.id, w.name, w.role, w.rate " +
                "ORDER BY w.name";

        List<WorkerSalary> salaries = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(from));
            statement.setDate(2, Date.valueOf(to));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String workerId = rs.getString("id");
                    String name = rs.getString("name");
                    String role = rs.getString("role");
                    double rate = parseRate(rs.getString("rate"));
                    int present = rs.getInt("present");
                    int halfDay = rs.getInt("half_day");
                    int absent = rs.getInt("absent");

                    double advances = advancesById.getOrDefault(workerId, 0.0);
                    if (advances == 0.0) {
                        advances = advancesByName.getOrDefault(name, 0.0);
                    }
                    double creditBalance = creditsById.getOrDefault(workerId, 0.0);
                    if (creditBalance == 0.0) {
                        creditBalance = creditsByName.getOrDefault(name, 0.0);
                    }

                    double gross = (present + (halfDay * 0.5)) * rate;
                    double paidAmount = paidAmountsByWorkerId.getOrDefault(workerId, 0.0);
                    double netPayable = gross - advances;
                    String status = paymentStatus(netPayable, paidAmount);

                    salaries.add(new WorkerSalary(
                            workerId,
                            name,
                            role,
                            avatarColor(name),
                            present,
                            halfDay,
                            absent,
                            gross,
                            advances,
                            creditBalance,
                            paidAmount,
                            status
                    ));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load salaries", ex);
        }
        return salaries;
    }

    /** Saves a partial or complete payment for a worker and payroll period. */
    public String paySalary(String workerId, String workerName, LocalDate from, LocalDate to,
                            double paymentAmount, double totalPayable) {
        if (workerId == null || workerId.isBlank() || from == null || to == null ||
                paymentAmount <= 0 || totalPayable <= 0 || from.isAfter(to)) {
            throw new IllegalArgumentException("A worker, valid payroll period, and positive payment amount are required.");
        }
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                double alreadyPaid = 0;
                String selectSql = "SELECT amount FROM salary_payments WHERE worker_id = ? AND period_from = ? AND period_to = ? FOR UPDATE";
                try (PreparedStatement select = connection.prepareStatement(selectSql)) {
                    select.setString(1, workerId);
                    select.setDate(2, Date.valueOf(from));
                    select.setDate(3, Date.valueOf(to));
                    try (ResultSet rs = select.executeQuery()) {
                        while (rs.next()) {
                            alreadyPaid += rs.getDouble("amount");
                        }
                    }
                }
                if (alreadyPaid + paymentAmount > totalPayable + 0.0001) {
                    throw new IllegalArgumentException(String.format("The payment exceeds the remaining balance of Rs. %,.2f.", totalPayable - alreadyPaid));
                }

                String paymentId = UUID.randomUUID().toString();
                String insertSql = "INSERT INTO salary_payments (id, worker_id, worker, period_from, period_to, amount, paid_at) VALUES (?, ?, ?, ?, ?, ?, NOW())";
                try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
                    insert.setString(1, paymentId);
                    insert.setString(2, workerId);
                    insert.setString(3, workerName);
                    insert.setDate(4, Date.valueOf(from));
                    insert.setDate(5, Date.valueOf(to));
                    insert.setDouble(6, paymentAmount);
                    insert.executeUpdate();
                }
                connection.commit();
                return paymentId;
            } catch (RuntimeException | SQLException ex) {
                connection.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to pay salary", ex);
        }
    }

    private Map<String, Double> loadPaidAmountsByWorkerId(LocalDate from, LocalDate to) {
        String sql = "SELECT worker_id, SUM(amount) AS amount FROM salary_payments WHERE period_from = ? AND period_to = ? GROUP BY worker_id";
        Map<String, Double> paidAmounts = new HashMap<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(from));
            statement.setDate(2, Date.valueOf(to));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    paidAmounts.put(rs.getString("worker_id"), rs.getDouble("amount"));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load salary payment status", ex);
        }
        return paidAmounts;
    }

    private String paymentStatus(double totalPayable, double paidAmount) {
        if (totalPayable <= 0) return "NO DATA";
        if (paidAmount >= totalPayable - 0.0001) return "PAID";
        if (paidAmount > 0) return "PARTIALLY PAID";
        return "READY";
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

    private String avatarColor(String name) {
        String[] palette = new String[]{"#60a5fa", "#34d399", "#f472b6", "#f59e0b", "#a78bfa"};
        if (name == null || name.isBlank()) {
            return palette[0];
        }
        int idx = Math.abs(name.hashCode()) % palette.length;
        return palette[idx];
    }
}
