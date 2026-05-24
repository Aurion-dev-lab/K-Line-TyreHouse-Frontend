package com.gui.kline.data;

import com.gui.kline.models.WorkerSalary;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
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
                    String status = gross > 0 ? "READY" : "NO DATA";

                    salaries.add(new WorkerSalary(
                            name,
                            role,
                            avatarColor(name),
                            present,
                            halfDay,
                            absent,
                            gross,
                            advances,
                            creditBalance,
                            status
                    ));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load salaries", ex);
        }
        return salaries;
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

