package com.gui.kline.controller;

import com.gui.kline.data.DatabaseManager;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AnalyticsController {

    @FXML
    private AreaChart<String, Number> revenueTrendChart;
    @FXML
    private PieChart inventoryPieChart;
    @FXML
    private BarChart<String, Number> salesServicesChart;

    @FXML
    public void initialize() {
        setupRevenueChart();
        setupInventoryChart();
        setupSalesServicesChart();
    }

    private void setupRevenueChart() {
        revenueTrendChart.getData().clear();

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(29); // last 30 days

        XYChart.Series<String, Number> revenue = new XYChart.Series<>();
        revenue.setName("Revenue");

        XYChart.Series<String, Number> profit = new XYChart.Series<>();
        profit.setName("Profit");

        try (Connection conn = DatabaseManager.getConnection()) {
            Map<LocalDate, Double> revenueByDate = new LinkedHashMap<>();
            Map<LocalDate, Double> expensesByDate = new HashMap<>();
            Map<LocalDate, Double> paidSalaryByDate = new HashMap<>();

            // Initialize all dates with 0
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                revenueByDate.put(current, 0.0);
                expensesByDate.put(current, 0.0);
                current = current.plusDays(1);
            }

            // Load invoice revenue
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(invoice_date, DATE(created_at)) as d, SUM(grand_total) as total " +
                            "FROM invoices WHERE status = 'completed' AND COALESCE(invoice_date, DATE(created_at)) BETWEEN ? AND ? GROUP BY d")) {
                ps.setString(1, startDate.toString());
                ps.setString(2, endDate.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        LocalDate date = com.gui.kline.utils.SqliteUtil.getLocalDate(rs, 1);
                        if (date != null) {
                            revenueByDate.merge(date, rs.getDouble(2), Double::sum);
                        }
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(sale_date, DATE(created_at)) as d, SUM(grand_total) as total " +
                            "FROM credit_sales WHERE COALESCE(sale_date, DATE(created_at)) BETWEEN ? AND ? GROUP BY d")) {
                ps.setString(1, startDate.toString());
                ps.setString(2, endDate.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        LocalDate date = com.gui.kline.utils.SqliteUtil.getLocalDate(rs, 1);
                        if (date != null) {
                            revenueByDate.merge(date, rs.getDouble(2), Double::sum);
                        }
                    }
                }
            }

            // Load services revenue
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT service_date, SUM(price) as total FROM services " +
                            "WHERE (invoice_id IS NULL OR invoice_id = '') AND (name IS NULL OR name != 'Invoiced Service') AND service_date BETWEEN ? AND ? GROUP BY service_date")) {
                ps.setString(1, startDate.toString());
                ps.setString(2, endDate.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        LocalDate date = com.gui.kline.utils.SqliteUtil.getLocalDate(rs, 1);
                        if (date != null) {
                            revenueByDate.merge(date, rs.getDouble(2), Double::sum);
                        }
                    }
                }
            }

            // Load quick services revenue
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT service_date, SUM(price) as total FROM quick_services " +
                            "WHERE service_date BETWEEN ? AND ? GROUP BY service_date")) {
                ps.setString(1, startDate.toString());
                ps.setString(2, endDate.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        LocalDate date = com.gui.kline.utils.SqliteUtil.getLocalDate(rs, 1);
                        if (date != null) {
                            revenueByDate.merge(date, rs.getDouble(2), Double::sum);
                        }
                    }
                }
            }

            // Load product costs (COGS) for accurate profit calculation
            Map<LocalDate, Double> cogsByDate = new HashMap<>();
            Map<String, Double> productBuyPrices = new HashMap<>();
            try (java.sql.Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT id, buy_price FROM products")) {
                while (rs.next()) {
                    productBuyPrices.put(rs.getString("id"), rs.getDouble("buy_price"));
                }
            } catch (SQLException ignored) {
            }

            com.fasterxml.jackson.databind.ObjectMapper mapper = com.gui.kline.utils.JsonUtil.createObjectMapper();

            // Invoice product costs
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(invoice_date, DATE(created_at)) as d, line_items FROM invoices WHERE status = 'completed' AND COALESCE(invoice_date, DATE(created_at)) BETWEEN ? AND ?")) {
                ps.setString(1, startDate.toString());
                ps.setString(2, endDate.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        LocalDate date = com.gui.kline.utils.SqliteUtil.getLocalDate(rs, 1);
                        if (date != null) {
                            String lineItemsJson = rs.getString(2);
                            if (lineItemsJson != null && !lineItemsJson.isBlank()) {
                                try {
                                    List<com.gui.kline.models.dto.LineItem> items = mapper.readValue(lineItemsJson,
                                            new com.fasterxml.jackson.core.type.TypeReference<List<com.gui.kline.models.dto.LineItem>>() {
                                            });
                                    if (items != null) {
                                        for (com.gui.kline.models.dto.LineItem item : items) {
                                            if (item.getProductId() != null) {
                                                double buyPrice = productBuyPrices.getOrDefault(item.getProductId(),
                                                        0.0);
                                                cogsByDate.merge(date, item.getQty() * buyPrice, Double::sum);
                                            }
                                        }
                                    }
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }
                }
            }

            // Credit sale product costs
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(sale_date, DATE(created_at)) as d, parts FROM credit_sales WHERE COALESCE(sale_date, DATE(created_at)) BETWEEN ? AND ?")) {
                ps.setString(1, startDate.toString());
                ps.setString(2, endDate.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        LocalDate date = com.gui.kline.utils.SqliteUtil.getLocalDate(rs, 1);
                        if (date != null) {
                            String partsJson = rs.getString(2);
                            if (partsJson != null && !partsJson.isBlank()) {
                                try {
                                    List<com.gui.kline.models.dto.Part> items = mapper.readValue(partsJson,
                                            new com.fasterxml.jackson.core.type.TypeReference<List<com.gui.kline.models.dto.Part>>() {
                                            });
                                    if (items != null) {
                                        for (com.gui.kline.models.dto.Part item : items) {
                                            if (item.getProductId() != null) {
                                                double buyPrice = productBuyPrices.getOrDefault(item.getProductId(),
                                                        0.0);
                                                cogsByDate.merge(date, item.getQuantity() * buyPrice, Double::sum);
                                            }
                                        }
                                    }
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }
                }
            }

            // Tyre export purchase costs
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT export_date as d, SUM(comp_price * tyres) as total FROM tyre_exports WHERE export_date BETWEEN ? AND ? GROUP BY export_date")) {
                ps.setString(1, startDate.toString());
                ps.setString(2, endDate.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        LocalDate date = com.gui.kline.utils.SqliteUtil.getLocalDate(rs, 1);
                        if (date != null) {
                            cogsByDate.merge(date, rs.getDouble(2), Double::sum);
                        }
                    }
                }
            }

            // Load expenses
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT expense_date, SUM(amount) as total FROM expenses " +
                            "WHERE expense_date BETWEEN ? AND ? GROUP BY expense_date")) {
                ps.setString(1, startDate.toString());
                ps.setString(2, endDate.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        LocalDate date = com.gui.kline.utils.SqliteUtil.getLocalDate(rs, 1);
                        if (date != null) {
                            expensesByDate.merge(date, rs.getDouble(2), Double::sum);
                        }
                    }
                }
            }

            // Salary Management records payments individually, including partial payments.
            // Deduct each payment on its actual payment date for a meaningful profit trend.
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT DATE(paid_at) AS d, SUM(amount) AS total FROM salary_payments " +
                            "WHERE DATE(paid_at) BETWEEN ? AND ? GROUP BY DATE(paid_at)")) {
                ps.setString(1, startDate.toString());
                ps.setString(2, endDate.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        LocalDate date = com.gui.kline.utils.SqliteUtil.getLocalDate(rs, "d");
                        if (date != null) {
                            paidSalaryByDate.merge(date, rs.getDouble("total"), Double::sum);
                        }
                    }
                }
            }

            // Populate chart data
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM");
            revenueByDate.forEach((date, total) -> {
                String label = date.format(formatter);
                revenue.getData().add(new XYChart.Data<>(label, total));
                // Profit = Revenue - COGS - Expenses - Salaries
                double totalExpenses = expensesByDate.getOrDefault(date, 0.0);
                double totalSalaries = paidSalaryByDate.getOrDefault(date, 0.0);
                double totalCogs = cogsByDate.getOrDefault(date, 0.0);
                profit.getData().add(new XYChart.Data<>(label,
                        total - totalCogs - totalExpenses - totalSalaries));
            });

        } catch (SQLException ex) {
            System.err.println("Error loading revenue chart data: " + ex.getMessage());
        }

        revenueTrendChart.getData().addAll(revenue, profit);
    }

    private void setupInventoryChart() {
        inventoryPieChart.getData().clear();

        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT category, SUM(stock) as total FROM products " +
                                "WHERE category IS NOT NULL AND category != '' GROUP BY category ORDER BY total DESC")) {
            try (ResultSet rs = ps.executeQuery()) {
                ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
                while (rs.next()) {
                    String category = rs.getString("category");
                    int stock = rs.getInt("total");
                    pieData.add(new PieChart.Data(category, stock));
                }
                inventoryPieChart.setData(pieData);
            }
        } catch (SQLException ex) {
            System.err.println("Error loading inventory chart data: " + ex.getMessage());
        }
    }

    private void setupSalesServicesChart() {
        salesServicesChart.getData().clear();

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6); // last 7 days

        XYChart.Series<String, Number> sales = new XYChart.Series<>();
        sales.setName("Sales");

        XYChart.Series<String, Number> services = new XYChart.Series<>();
        services.setName("Services");

        try (Connection conn = DatabaseManager.getConnection()) {
            Map<LocalDate, Double> salesByDate = new LinkedHashMap<>();
            Map<LocalDate, Double> servicesByDate = new LinkedHashMap<>();

            // Initialize dates
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                salesByDate.put(current, 0.0);
                servicesByDate.put(current, 0.0);
                current = current.plusDays(1);
            }

            // Load invoice totals by date
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(invoice_date, DATE(created_at)) as d, SUM(grand_total) as total " +
                            "FROM invoices WHERE status = 'completed' AND COALESCE(invoice_date, DATE(created_at)) BETWEEN ? AND ? GROUP BY d")) {
                ps.setString(1, startDate.toString());
                ps.setString(2, endDate.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        LocalDate date = com.gui.kline.utils.SqliteUtil.getLocalDate(rs, 1);
                        if (date != null) {
                            salesByDate.merge(date, rs.getDouble(2), Double::sum);
                        }
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(sale_date, DATE(created_at)) as d, SUM(grand_total) as total " +
                            "FROM credit_sales WHERE COALESCE(sale_date, DATE(created_at)) BETWEEN ? AND ? GROUP BY d")) {
                ps.setString(1, startDate.toString());
                ps.setString(2, endDate.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        LocalDate date = com.gui.kline.utils.SqliteUtil.getLocalDate(rs, 1);
                        if (date != null) {
                            salesByDate.merge(date, rs.getDouble(2), Double::sum);
                        }
                    }
                }
            }

            // Load services by date
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT service_date, SUM(price) as total FROM services " +
                            "WHERE (invoice_id IS NULL OR invoice_id = '') AND (name IS NULL OR name != 'Invoiced Service') AND service_date BETWEEN ? AND ? GROUP BY service_date")) {
                ps.setString(1, startDate.toString());
                ps.setString(2, endDate.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        LocalDate date = com.gui.kline.utils.SqliteUtil.getLocalDate(rs, 1);
                        if (date != null) {
                            servicesByDate.merge(date, rs.getDouble(2), Double::sum);
                        }
                    }
                }
            }

            // Load quick services by date
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT service_date, SUM(price) as total FROM quick_services " +
                            "WHERE service_date BETWEEN ? AND ? GROUP BY service_date")) {
                ps.setString(1, startDate.toString());
                ps.setString(2, endDate.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        LocalDate date = com.gui.kline.utils.SqliteUtil.getLocalDate(rs, 1);
                        if (date != null) {
                            servicesByDate.merge(date, rs.getDouble(2), Double::sum);
                        }
                    }
                }
            }

            // Populate chart
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM");
            salesByDate.forEach((date, total) -> {
                String label = date.format(formatter);
                sales.getData().add(new XYChart.Data<>(label, total));
                services.getData().add(new XYChart.Data<>(label, servicesByDate.get(date)));
            });

        } catch (SQLException ex) {
            System.err.println("Error loading sales/services chart data: " + ex.getMessage());
        }

        salesServicesChart.getData().addAll(sales, services);
    }
}
