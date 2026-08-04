package com.gui.kline.data;

import com.gui.kline.models.dto.LedgerEntry;
import com.gui.kline.models.reports.SalaryPayment;
import com.gui.kline.models.ui.WorkerSalary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class LocalSalaryRepository {
    private final LocalSalaryAdvanceRepository advanceRepository = new LocalSalaryAdvanceRepository();
    private final LocalWorkerCreditRepository creditRepository = new LocalWorkerCreditRepository();

    public List<WorkerSalary> loadWorkerSalaries(LocalDate from, LocalDate to) {
        Map<String, Double> advancesById = advanceRepository.sumAdvancesByWorkerId(from, to);
        Map<String, Double> advancesByName = advanceRepository.sumAdvancesByWorkerName(from, to);
        Map<String, Double> creditsById = creditRepository.balanceByWorkerId(from, to);
        Map<String, Double> creditsByName = creditRepository.balanceByWorkerName(from, to);
        Map<String, Double> paidAmountsByWorkerId = loadPaidAmountsByWorkerId(from, to);
        Map<String, Double> payrollSettledById = loadPayrollSettledCreditsByWorkerId(from, to);
        Map<String, Double> payrollSettledByName = loadPayrollSettledCreditsByWorkerName(from, to);

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
            statement.setString(1, from.toString());
            statement.setString(2, to.toString());
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
                    creditBalance = Math.max(0, creditBalance);

                    double payrollSettled = payrollSettledById.getOrDefault(workerId, 0.0);
                    if (payrollSettled == 0.0) {
                        payrollSettled = payrollSettledByName.getOrDefault(name, 0.0);
                    }

                    double gross = (present + (halfDay * 0.5)) * rate;
                    double paidAmount = paidAmountsByWorkerId.getOrDefault(workerId, 0.0);
                    double netPayable = Math.max(0, gross - advances - creditBalance - payrollSettled);
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
                            payrollSettled,
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

    private Map<String, Double> loadPayrollSettledCreditsByWorkerId(LocalDate from, LocalDate to) {
        String sql = "SELECT worker_id, SUM(amount) AS total FROM worker_credits " +
                "WHERE credit_type = 'SETTLEMENT' AND note LIKE 'Auto-settled via payroll%' " +
                "AND credit_date BETWEEN ? AND ? GROUP BY worker_id";
        Map<String, Double> totals = new HashMap<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, from.toString());
            statement.setString(2, to.toString());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    totals.put(rs.getString("worker_id"), rs.getDouble("total"));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load payroll credit settlements", ex);
        }
        return totals;
    }

    private Map<String, Double> loadPayrollSettledCreditsByWorkerName(LocalDate from, LocalDate to) {
        String sql = "SELECT worker, SUM(amount) AS total FROM worker_credits " +
                "WHERE credit_type = 'SETTLEMENT' AND note LIKE 'Auto-settled via payroll%' " +
                "AND credit_date BETWEEN ? AND ? GROUP BY worker";
        Map<String, Double> totals = new HashMap<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, from.toString());
            statement.setString(2, to.toString());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    totals.put(rs.getString("worker"), rs.getDouble("total"));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load payroll credit settlements", ex);
        }
        return totals;
    }

    /** Saves a partial or complete payment for a worker and payroll period. */
    public String paySalary(String workerId, String workerName, LocalDate from, LocalDate to,
                            double paymentAmount, double totalPayable) {
        return paySalary(workerId, workerName, from, to, paymentAmount, totalPayable, 0.0);
    }

    public String paySalary(String workerId, String workerName, LocalDate from, LocalDate to,
                            double paymentAmount, double totalPayable, double creditSettlementAmount) {
        if (workerId == null || workerId.isBlank() || from == null || to == null ||
                paymentAmount <= 0 || totalPayable <= 0 || from.isAfter(to)) {
            throw new IllegalArgumentException("A worker, valid payroll period, and positive payment amount are required.");
        }
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                double alreadyPaid = 0;
                String selectSql = "SELECT amount FROM salary_payments WHERE worker_id = ? AND period_from = ? AND period_to = ?";
                try (PreparedStatement select = connection.prepareStatement(selectSql)) {
                    select.setString(1, workerId);
                    select.setString(2, from.toString());
                    select.setString(3, to.toString());
                    try (ResultSet rs = select.executeQuery()) {
                        while (rs.next()) {
                            alreadyPaid += rs.getDouble("amount");
                        }
                    }
                }
                if (alreadyPaid + paymentAmount > totalPayable + 0.0001) {
                    throw new IllegalArgumentException(String.format("The payment exceeds the remaining balance of Rs. %,.2f.", totalPayable - alreadyPaid));
                }

                String paymentId = com.gui.kline.utils.Utils.generateId("PAY-", 8);
                String insertSql = "INSERT INTO salary_payments (id, worker_id, worker, period_from, period_to, amount, paid_at) VALUES (?, ?, ?, ?, ?, ?, strftime('%Y-%m-%dT%H:%M:%S', 'now'))";
                try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
                    insert.setString(1, paymentId);
                    insert.setString(2, workerId);
                    insert.setString(3, workerName);
                    insert.setString(4, from.toString());
                    insert.setString(5, to.toString());
                    insert.setDouble(6, paymentAmount);
                    insert.executeUpdate();
                }

                if (creditSettlementAmount > 0) {
                    String creditId = com.gui.kline.utils.Utils.generateId("CRD-", 8);
                    String creditSql = "INSERT INTO worker_credits (id, worker_id, worker, amount, credit_type, credit_date, note, created_at) " +
                            "VALUES (?, ?, ?, ?, 'SETTLEMENT', ?, ?, strftime('%Y-%m-%dT%H:%M:%S', 'now'))";
                    try (PreparedStatement insertCredit = connection.prepareStatement(creditSql)) {
                        insertCredit.setString(1, creditId);
                        insertCredit.setString(2, workerId);
                        insertCredit.setString(3, workerName);
                        insertCredit.setDouble(4, creditSettlementAmount);
                        insertCredit.setString(5, LocalDate.now().toString());
                        insertCredit.setString(6, "Auto-settled via payroll payout:" + paymentId);
                        insertCredit.executeUpdate();
                    }
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
            statement.setString(1, from.toString());
            statement.setString(2, to.toString());
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
        if (totalPayable <= 0) return "NO PAYABLE";
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

    /**
     * Delete a specific salary payment by its ID.
     */
    public void deleteSalaryPayment(String paymentId) {
        String sql = "DELETE FROM salary_payments WHERE id = ?";
        String creditSql = "DELETE FROM worker_credits WHERE note LIKE '%:' || ? OR note = 'Auto-settled via payroll payout'";
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, paymentId);
                    statement.executeUpdate();
                }
                try (PreparedStatement creditStatement = connection.prepareStatement(creditSql)) {
                    creditStatement.setString(1, paymentId);
                    creditStatement.executeUpdate();
                }
                connection.commit();
                DatabaseManager.logDeletion("salary_payments", paymentId);
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to delete salary payment", ex);
        }
    }

    /**
     * Get individual salary payments for a worker in a period.
     */
    public List<SalaryPayment> loadSalaryPayments(String workerId, LocalDate from, LocalDate to) {
        String sql = "SELECT id, worker, amount, paid_at FROM salary_payments WHERE worker_id = ? AND period_from = ? AND period_to = ? ORDER BY paid_at DESC";
        List<SalaryPayment> payments = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, workerId);
            statement.setString(2, from.toString());
            statement.setString(3, to.toString());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    payments.add(new SalaryPayment(
                            rs.getString("id"),
                            rs.getString("worker"),
                            rs.getDouble("amount"),
                            rs.getTimestamp("paid_at").toLocalDateTime()
                    ));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load salary payments", ex);
        }
        return payments;
    }

    /**
     * Loads unified payouts and advances ledger entries for a period.
     */
    public List<LedgerEntry> loadPayoutLedger(LocalDate from, LocalDate to) {
        String sql = "SELECT id, entry_date, worker, entry_type, entry_note, amount FROM (" +
                "  SELECT id, advance_date AS entry_date, worker, 'ADVANCE' AS entry_type, " +
                "  CASE WHEN note IS NULL OR TRIM(note) = '' THEN 'Salary advance' ELSE note END AS entry_note, amount " +
                "  FROM salary_advances " +
                "  WHERE advance_date BETWEEN ? AND ? " +
                "  UNION ALL " +
                "  SELECT id, DATE(paid_at) AS entry_date, worker, 'PAYOUT' AS entry_type, " +
                "  ('Salary payout for period ' || period_from || ' to ' || period_to) AS entry_note, amount " +
                "  FROM salary_payments " +
                "  WHERE DATE(paid_at) BETWEEN ? AND ? " +
                ") ORDER BY entry_date DESC";

        List<LedgerEntry> entries = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, from.toString());
            statement.setString(2, to.toString());
            statement.setString(3, from.toString());
            statement.setString(4, to.toString());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String dateStr = rs.getString("entry_date");
                    LocalDate date = dateStr != null ? LocalDate.parse(dateStr) : from;
                    entries.add(new LedgerEntry(
                            rs.getString("id"),
                            date,
                            rs.getString("worker"),
                            rs.getString("entry_type"),
                            rs.getString("entry_note"),
                            rs.getDouble("amount")
                    ));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load payout ledger", ex);
        }
        return entries;
    }


}
