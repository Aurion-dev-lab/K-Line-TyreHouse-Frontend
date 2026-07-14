package com.gui.kline.controller;

import com.gui.kline.data.DatabaseManager;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AnalyticsController {

    @FXML private AreaChart<String, Number> revenueTrendChart;
    @FXML private PieChart inventoryPieChart;
    @FXML private BarChart<String, Number> salesServicesChart;

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
            Map<LocalDate, Double> paidSalaryByDate = new HashMap<>();
            
            // Initialize all dates with 0
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                revenueByDate.put(current, 0.0);
                current = current.plusDays(1);
            }
            
            // Load invoice revenue
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(invoice_date, DATE(created_at)) as d, SUM(grand_total) as total " +
                    "FROM invoices WHERE status = 'completed' AND COALESCE(invoice_date, DATE(created_at)) BETWEEN ? AND ? GROUP BY d")) {
                ps.setDate(1, Date.valueOf(startDate));
                ps.setDate(2, Date.valueOf(endDate));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Date d = rs.getDate(1);
                        if (d != null) {
                            LocalDate date = d.toLocalDate();
                            revenueByDate.merge(date, rs.getDouble(2), Double::sum);
                        }
                    }
                }
            }
            
            // Load credit sales revenue
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(sale_date, DATE(created_at)) as d, SUM(COALESCE(subtotal, amount)) as total " +
                    "FROM credit_sales WHERE COALESCE(sale_date, DATE(created_at)) BETWEEN ? AND ? GROUP BY d")) {
                ps.setDate(1, Date.valueOf(startDate));
                ps.setDate(2, Date.valueOf(endDate));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Date d = rs.getDate(1);
                        if (d != null) {
                            LocalDate date = d.toLocalDate();
                            revenueByDate.merge(date, rs.getDouble(2), Double::sum);
                        }
                    }
                }
            }
            
            // Load services revenue
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT service_date, SUM(price) as total FROM services " +
                    "WHERE service_date BETWEEN ? AND ? GROUP BY service_date")) {
                ps.setDate(1, Date.valueOf(startDate));
                ps.setDate(2, Date.valueOf(endDate));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Date d = rs.getDate(1);
                        if (d != null) {
                            LocalDate date = d.toLocalDate();
                            revenueByDate.merge(date, rs.getDouble(2), Double::sum);
                        }
                    }
                }
            }
            
            // Load quick services revenue
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT service_date, SUM(price) as total FROM quick_services " +
                    "WHERE service_date BETWEEN ? AND ? GROUP BY service_date")) {
                ps.setDate(1, Date.valueOf(startDate));
                ps.setDate(2, Date.valueOf(endDate));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Date d = rs.getDate(1);
                        if (d != null) {
                            LocalDate date = d.toLocalDate();
                            revenueByDate.merge(date, rs.getDouble(2), Double::sum);
                        }
                    }
                }
            }
            
            // Salary Management records payments individually, including partial payments.
            // Deduct each payment on its actual payment date for a meaningful profit trend.
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT DATE(paid_at) AS d, SUM(amount) AS total FROM salary_payments " +
                    "WHERE DATE(paid_at) BETWEEN ? AND ? GROUP BY DATE(paid_at)")) {
                ps.setDate(1, Date.valueOf(startDate));
                ps.setDate(2, Date.valueOf(endDate));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Date d = rs.getDate("d");
                        if (d != null) {
                            paidSalaryByDate.merge(d.toLocalDate(), rs.getDouble("total"), Double::sum);
                        }
                    }
                }
            }
            
            // Populate chart data
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM");
            revenueByDate.forEach((date, total) -> {
                String label = date.format(formatter);
                revenue.getData().add(new XYChart.Data<>(label, total));
                profit.getData().add(new XYChart.Data<>(label, total - paidSalaryByDate.getOrDefault(date, 0.0)));
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
                ps.setDate(1, Date.valueOf(startDate));
                ps.setDate(2, Date.valueOf(endDate));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Date d = rs.getDate(1);
                        if (d != null) {
                            LocalDate date = d.toLocalDate();
                            salesByDate.merge(date, rs.getDouble(2), Double::sum);
                        }
                    }
                }
            }
            
            // Load credit sales by date
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(sale_date, DATE(created_at)) as d, SUM(COALESCE(subtotal, amount)) as total " +
                    "FROM credit_sales WHERE COALESCE(sale_date, DATE(created_at)) BETWEEN ? AND ? GROUP BY d")) {
                ps.setDate(1, Date.valueOf(startDate));
                ps.setDate(2, Date.valueOf(endDate));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Date d = rs.getDate(1);
                        if (d != null) {
                            LocalDate date = d.toLocalDate();
                            salesByDate.merge(date, rs.getDouble(2), Double::sum);
                        }
                    }
                }
            }
            
            // Load services by date
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT service_date, SUM(price) as total FROM services " +
                    "WHERE service_date BETWEEN ? AND ? GROUP BY service_date")) {
                ps.setDate(1, Date.valueOf(startDate));
                ps.setDate(2, Date.valueOf(endDate));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Date d = rs.getDate(1);
                        if (d != null) {
                            LocalDate date = d.toLocalDate();
                            servicesByDate.merge(date, rs.getDouble(2), Double::sum);
                        }
                    }
                }
            }
            
            // Load quick services by date
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT service_date, SUM(price) as total FROM quick_services " +
                    "WHERE service_date BETWEEN ? AND ? GROUP BY service_date")) {
                ps.setDate(1, Date.valueOf(startDate));
                ps.setDate(2, Date.valueOf(endDate));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Date d = rs.getDate(1);
                        if (d != null) {
                            LocalDate date = d.toLocalDate();
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
