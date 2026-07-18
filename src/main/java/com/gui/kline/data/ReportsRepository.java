package com.gui.kline.data;

import com.gui.kline.controller.ReportsController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository class for generating comprehensive reports from the database.
 * Handles data retrieval for sales, services, expenses, and financial reports.
 */
public class ReportsRepository {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Get sales data for the reports
     */
    public List<ReportsController.SaleItem> getSalesData(LocalDate startDate, LocalDate endDate) {
        List<ReportsController.SaleItem> sales = new ArrayList<>();
        
        // Get data from invoices and line items
        String sql = "SELECT " +
                "    i.invoice_date as sale_date, " +
                "    COALESCE(p.name, il.description) as product_name, " +
                "    il.qty as quantity, " +
                "    il.total as revenue, " +
                "    (il.unit_price - COALESCE(p.buy_price, 0)) * il.qty as profit " +
                "FROM invoice_line_items il " +
                "LEFT JOIN invoices i ON il.invoice_id = i.invoice_id " +
                "LEFT JOIN products p ON il.product_id = p.id " +
                "WHERE i.status = 'completed' AND i.invoice_date BETWEEN ? AND ? " +
                "ORDER BY i.invoice_date DESC, product_name";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setDate(1, Date.valueOf(startDate));
            statement.setDate(2, Date.valueOf(endDate));
            
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    LocalDate date = rs.getDate("sale_date") != null ? 
                        rs.getDate("sale_date").toLocalDate() : LocalDate.now();
                    String productName = rs.getString("product_name");
                    if (productName == null || productName.isBlank()) {
                        productName = "Unknown Product";
                    }
                    int qty = rs.getInt("quantity");
                    double revenue = rs.getDouble("revenue");
                    double profit = rs.getDouble("profit");
                    
                    sales.add(new ReportsController.SaleItem(
                        productName, date, qty, revenue, profit
                    ));
                }
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load sales data for reports: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        // Also include credit sales data
        String creditSql = "SELECT " +
                "    cs.sale_date, " +
                "    cs.customer_name as product_name, " +
                "    cs.amount as revenue, " +
                "    cs.paid_amount as paid_amount, " +
                "    (cs.amount - cs.paid_amount) as balance " +
                "FROM credit_sales cs " +
                "WHERE cs.sale_date BETWEEN ? AND ? " +
                "ORDER BY cs.sale_date DESC";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(creditSql)) {
            
            statement.setDate(1, Date.valueOf(startDate));
            statement.setDate(2, Date.valueOf(endDate));
            
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    LocalDate date = rs.getDate("sale_date") != null ? 
                        rs.getDate("sale_date").toLocalDate() : LocalDate.now();
                    String customerName = rs.getString("product_name");
                    if (customerName == null || customerName.isBlank()) {
                        customerName = "Credit Sale";
                    }
                    double revenue = rs.getDouble("revenue");
                    double paidAmount = rs.getDouble("paid_amount");
                    double balance = rs.getDouble("balance");
                    
                    // Estimate profit margin (assuming 30% for credit sales)
                    double profit = revenue * 0.3;
                    
                    sales.add(new ReportsController.SaleItem(
                        customerName + " (Credit)", date, 1, revenue, profit
                    ));
                }
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load credit sales data: " + ex.getMessage());
        }
        
        return sales;
    }

    /**
     * Get service data for the reports
     */
    public List<ReportsController.ServiceItem> getServiceData(LocalDate startDate, LocalDate endDate) {
        List<ReportsController.ServiceItem> services = new ArrayList<>();
        
        // Get regular services
        String sql = "SELECT s.service_date, s.name, s.price, NULL as assigned_to " +
                "FROM services s " +
                "WHERE s.service_date BETWEEN ? AND ? " +
                "ORDER BY s.service_date DESC, s.name";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setDate(1, Date.valueOf(startDate));
            statement.setDate(2, Date.valueOf(endDate));
            
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    LocalDate date = rs.getDate("service_date") != null ? 
                        rs.getDate("service_date").toLocalDate() : LocalDate.now();
                    String name = rs.getString("name");
                    String assignedTo = rs.getString("assigned_to");
                    double fee = rs.getDouble("price");
                    
                    services.add(new ReportsController.ServiceItem(name, date, assignedTo, fee));
                }
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load services data: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        // Get quick services
        String quickSql = "SELECT qs.service_date, qs.service as name, qs.price " +
                "FROM quick_services qs " +
                "WHERE qs.service_date BETWEEN ? AND ? " +
                "ORDER BY qs.service_date DESC";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(quickSql)) {
            
            statement.setDate(1, Date.valueOf(startDate));
            statement.setDate(2, Date.valueOf(endDate));
            
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    LocalDate date = rs.getDate("service_date") != null ? 
                        rs.getDate("service_date").toLocalDate() : LocalDate.now();
                    String name = rs.getString("name");
                    double fee = rs.getDouble("price");
                    
                    services.add(new ReportsController.ServiceItem(name, date, null, fee));
                }
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load quick services data: " + ex.getMessage());
        }
        
        return services;
    }

    /**
     * Calculate total worker costs for the given period
     */
    public double getWorkerCosts(LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT COALESCE(SUM(amount), 0) AS total FROM salary_payments " +
                "WHERE DATE(paid_at) BETWEEN ? AND ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(startDate));
            statement.setDate(2, Date.valueOf(endDate));
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getDouble("total") : 0.0;
            }
        } catch (SQLException ex) {
            System.err.println("Failed to calculate paid worker salaries: " + ex.getMessage());
            return 0.0;
        }
    }

    /**
     * Get expense data for reports
     */
    public List<ExpenseItem> getExpenses(LocalDate startDate, LocalDate endDate) {
        List<ExpenseItem> expenses = new ArrayList<>();
        
        // Get tyre export costs (these are expenses - the cost to purchase tyres)
        String tyreExportsSql = "SELECT " +
                "    te.export_date, " +
                "    te.company as description, " +
                "    (te.comp_price * te.tyres) as amount, " +
                "    'Tyre Purchase' as category " +
                "FROM tyre_exports te " +
                "WHERE te.export_date BETWEEN ? AND ? " +
                "ORDER BY te.export_date DESC";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(tyreExportsSql)) {
            
            statement.setDate(1, Date.valueOf(startDate));
            statement.setDate(2, Date.valueOf(endDate));
            
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    LocalDate date = rs.getDate("export_date") != null ? 
                        rs.getDate("export_date").toLocalDate() : LocalDate.now();
                    String description = rs.getString("description");
                    double amount = rs.getDouble("amount");
                    String category = rs.getString("category");
                    
                    expenses.add(new ExpenseItem(date, description, amount, category));
                }
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load tyre export expenses: " + ex.getMessage());
        }
        
        // Note: Service fees from tyre exports are NOT added as expenses here
        // because they are already included in the total_amount (revenue).
        // The profit from tyre exports is calculated as:
        // total_amount - (comp_price * tyres)
        // This avoids double-counting the service fee.

        String salaryPaymentsSql = "SELECT DATE(paid_at) AS payment_date, worker, amount " +
                "FROM salary_payments WHERE DATE(paid_at) BETWEEN ? AND ? ORDER BY paid_at DESC";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(salaryPaymentsSql)) {
            statement.setDate(1, Date.valueOf(startDate));
            statement.setDate(2, Date.valueOf(endDate));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    LocalDate date = rs.getDate("payment_date").toLocalDate();
                    String worker = rs.getString("worker");
                    expenses.add(new ExpenseItem(date, "Salary payment - " + worker,
                            rs.getDouble("amount"), "Worker Salary"));
                }
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load salary payment expenses: " + ex.getMessage());
        }

        // Get expenses from the expenses table
        String expensesTableSql = "SELECT expense_date, description, amount, category " +
                "FROM expenses WHERE expense_date BETWEEN ? AND ? ORDER BY expense_date DESC";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(expensesTableSql)) {
            statement.setDate(1, Date.valueOf(startDate));
            statement.setDate(2, Date.valueOf(endDate));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    LocalDate date = rs.getDate("expense_date") != null ?
                        rs.getDate("expense_date").toLocalDate() : LocalDate.now();
                    String description = rs.getString("description");
                    double amount = rs.getDouble("amount");
                    String category = rs.getString("category");
                    if (category == null) category = "Other";
                    expenses.add(new ExpenseItem(date, description, amount, category));
                }
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load expenses from expenses table: " + ex.getMessage());
        }
        
        return expenses;
    }

    /**
     * Get comprehensive financial summary for the period
     */
    public FinancialSummary getFinancialSummary(LocalDate startDate, LocalDate endDate) {
        FinancialSummary summary = new FinancialSummary();
        
        // Total sales revenue
        String salesSql = "SELECT COALESCE(SUM(il.total), 0) as total_sales " +
                "FROM invoice_line_items il " +
                "LEFT JOIN invoices i ON il.invoice_id = i.invoice_id " +
                "WHERE i.status = 'completed' AND i.invoice_date BETWEEN ? AND ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(salesSql)) {
            
            statement.setDate(1, Date.valueOf(startDate));
            statement.setDate(2, Date.valueOf(endDate));
            
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    summary.setTotalSales(rs.getDouble("total_sales"));
                }
            }
        } catch (SQLException ex) {
            System.err.println("Failed to calculate total sales: " + ex.getMessage());
        }
        
        // Total credit sales
        String creditSalesSql = "SELECT COALESCE(SUM(amount), 0) as total_credit " +
                "FROM credit_sales " +
                "WHERE sale_date BETWEEN ? AND ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(creditSalesSql)) {
            
            statement.setDate(1, Date.valueOf(startDate));
            statement.setDate(2, Date.valueOf(endDate));
            
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    summary.setCreditSales(rs.getDouble("total_credit"));
                }
            }
        } catch (SQLException ex) {
            System.err.println("Failed to calculate credit sales: " + ex.getMessage());
        }
        
        // Total service revenue
        String servicesSql = "SELECT COALESCE(SUM(price), 0) as total_services " +
                "FROM services " +
                "WHERE service_date BETWEEN ? AND ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(servicesSql)) {
            
            statement.setDate(1, Date.valueOf(startDate));
            statement.setDate(2, Date.valueOf(endDate));
            
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    summary.setServiceRevenue(rs.getDouble("total_services"));
                }
            }
        } catch (SQLException ex) {
            System.err.println("Failed to calculate service revenue: " + ex.getMessage());
        }
        
        // Quick services revenue
        String quickServicesSql = "SELECT COALESCE(SUM(price), 0) as total_quick " +
                "FROM quick_services " +
                "WHERE service_date BETWEEN ? AND ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(quickServicesSql)) {
            
            statement.setDate(1, Date.valueOf(startDate));
            statement.setDate(2, Date.valueOf(endDate));
            
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    summary.setQuickServiceRevenue(rs.getDouble("total_quick"));
                }
            }
        } catch (SQLException ex) {
            System.err.println("Failed to calculate quick service revenue: " + ex.getMessage());
        }
        
        // Tyre exports revenue (total_amount from tyre exports)
        String tyreExportRevenueSql = "SELECT COALESCE(SUM(total_amount), 0) as total_revenue " +
                "FROM tyre_exports " +
                "WHERE export_date BETWEEN ? AND ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(tyreExportRevenueSql)) {
            
            statement.setDate(1, Date.valueOf(startDate));
            statement.setDate(2, Date.valueOf(endDate));
            
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    summary.setTyreExportRevenue(rs.getDouble("total_revenue"));
                }
            }
        } catch (SQLException ex) {
            System.err.println("Failed to calculate tyre export revenue: " + ex.getMessage());
        }

        // Get expenses from the expenses table
        String generalExpensesSql = "SELECT COALESCE(SUM(amount), 0) as total_expenses " +
                "FROM expenses " +
                "WHERE expense_date BETWEEN ? AND ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(generalExpensesSql)) {
            
            statement.setDate(1, Date.valueOf(startDate));
            statement.setDate(2, Date.valueOf(endDate));
            
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    summary.setTotalExpenses(summary.getTotalExpenses() + rs.getDouble("total_expenses"));
                }
            }
        } catch (SQLException ex) {
            System.err.println("Failed to calculate general expenses: " + ex.getMessage());
        }

        // Product cost is incurred when a quotation is generated as a completed
        // invoice. Service and labour lines have no inventory cost, so they add
        // their full amount to profit.
        summary.setTotalExpenses(summary.getTotalExpenses()
                + getCompletedInvoiceProductCost(startDate, endDate));
        
        summary.setWorkerCosts(getWorkerCosts(startDate, endDate));
        
        // Calculate net profit
        // Total revenue from all sources (sales + credit sales + services + quick services + tyre exports)
        double totalRevenue = summary.getTotalSales() + summary.getCreditSales() + 
                             summary.getServiceRevenue() + summary.getQuickServiceRevenue() +
                             summary.getTyreExportRevenue();
        
        // Tyre export costs (comp_price * tyres) - cost of purchasing tyres
        double tyreExportCosts = getTyreExportCosts(startDate, endDate);
        
        // Total costs: general expenses + tyre export costs + worker costs
        double totalCosts = summary.getTotalExpenses() + tyreExportCosts + summary.getWorkerCosts();
        
        // Net profit = total revenue - total costs
        summary.setNetProfit(totalRevenue - totalCosts);
        
        return summary;
    }

    private double getCompletedInvoiceProductCost(LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT COALESCE(SUM(il.qty * COALESCE(p.buy_price, 0)), 0) " +
                "FROM invoice_line_items il " +
                "JOIN invoices i ON i.id = il.invoice_ref " +
                "LEFT JOIN products p ON p.id = il.product_id " +
                "WHERE i.status = 'completed' AND i.invoice_date BETWEEN ? AND ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(startDate));
            statement.setDate(2, Date.valueOf(endDate));
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        } catch (SQLException ex) {
            System.err.println("Failed to calculate completed invoice product cost: " + ex.getMessage());
            return 0.0;
        }
    }

    private double getTyreExportCosts(LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT COALESCE(SUM(comp_price * tyres), 0) " +
                "FROM tyre_exports " +
                "WHERE export_date BETWEEN ? AND ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(startDate));
            statement.setDate(2, Date.valueOf(endDate));
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        } catch (SQLException ex) {
            System.err.println("Failed to calculate tyre export costs: " + ex.getMessage());
            return 0.0;
        }
    }

    /**
     * Get top selling products
     */
    public ObservableList<TopProduct> getTopSellingProducts(LocalDate startDate, LocalDate endDate, int limit) {
        ObservableList<TopProduct> topProducts = FXCollections.observableArrayList();
        
        String sql = "SELECT " +
                "    p.name as product_name, " +
                "    SUM(il.qty) as total_quantity, " +
                "    SUM(il.total) as total_revenue " +
                "FROM invoice_line_items il " +
                "LEFT JOIN invoices i ON il.invoice_id = i.invoice_id " +
                "LEFT JOIN products p ON il.product_id = p.id " +
                "WHERE i.status = 'completed' AND i.invoice_date BETWEEN ? AND ? " +
                "GROUP BY p.id, p.name " +
                "ORDER BY total_revenue DESC " +
                "LIMIT ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setDate(1, Date.valueOf(startDate));
            statement.setDate(2, Date.valueOf(endDate));
            statement.setInt(3, limit);
            
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String productName = rs.getString("product_name");
                    int quantity = rs.getInt("total_quantity");
                    double revenue = rs.getDouble("total_revenue");
                    
                    topProducts.add(new TopProduct(productName, quantity, revenue));
                }
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load top selling products: " + ex.getMessage());
        }
        
        return topProducts;
    }

    /**
     * Get daily sales summary
     */
    public ObservableList<DailySummary> getDailySalesSummary(LocalDate startDate, LocalDate endDate) {
        ObservableList<DailySummary> dailySummaries = FXCollections.observableArrayList();
        
        String sql = "SELECT " +
                "    i.invoice_date as sale_date, " +
                "    COUNT(DISTINCT il.invoice_id) as invoice_count, " +
                "    SUM(il.qty) as total_items, " +
                "    SUM(il.total) as total_revenue " +
                "FROM invoice_line_items il " +
                "LEFT JOIN invoices i ON il.invoice_id = i.invoice_id " +
                "WHERE i.status = 'completed' AND i.invoice_date BETWEEN ? AND ? " +
                "GROUP BY i.invoice_date " +
                "ORDER BY i.invoice_date";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setDate(1, Date.valueOf(startDate));
            statement.setDate(2, Date.valueOf(endDate));
            
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    LocalDate date = rs.getDate("sale_date") != null ? 
                        rs.getDate("sale_date").toLocalDate() : LocalDate.now();
                    int invoiceCount = rs.getInt("invoice_count");
                    int totalItems = rs.getInt("total_items");
                    double totalRevenue = rs.getDouble("total_revenue");
                    
                    dailySummaries.add(new DailySummary(date, invoiceCount, totalItems, totalRevenue));
                }
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load daily sales summary: " + ex.getMessage());
        }
        
        return dailySummaries;
    }

    /**
     * Get customer purchase summary (for credit sales)
     */
    public ObservableList<CustomerSummary> getCustomerPurchaseSummary(LocalDate startDate, LocalDate endDate) {
        ObservableList<CustomerSummary> customerSummaries = FXCollections.observableArrayList();
        
        String sql = "SELECT " +
                "    customer_name as customer, " +
                "    COUNT(*) as purchase_count, " +
                "    SUM(amount) as total_amount, " +
                "    SUM(paid_amount) as total_paid " +
                "FROM credit_sales " +
                "WHERE sale_date BETWEEN ? AND ? " +
                "GROUP BY customer_name " +
                "ORDER BY total_amount DESC";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setDate(1, Date.valueOf(startDate));
            statement.setDate(2, Date.valueOf(endDate));
            
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String customer = rs.getString("customer");
                    int purchaseCount = rs.getInt("purchase_count");
                    double totalAmount = rs.getDouble("total_amount");
                    double totalPaid = rs.getDouble("total_paid");
                    double outstanding = totalAmount - totalPaid;
                    
                    customerSummaries.add(new CustomerSummary(customer, purchaseCount, totalAmount, totalPaid, outstanding));
                }
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load customer purchase summary: " + ex.getMessage());
        }
        
        return customerSummaries;
    }

    // Model classes for report data

    public static class ExpenseItem {
        private final LocalDate date;
        private final String description;
        private final double amount;
        private final String category;

        public ExpenseItem(LocalDate date, String description, double amount, String category) {
            this.date = date;
            this.description = description;
            this.amount = amount;
            this.category = category;
        }

        public LocalDate getDate() { return date; }
        public String getDescription() { return description; }
        public double getAmount() { return amount; }
        public String getCategory() { return category; }
    }

    public static class FinancialSummary {
        private double totalSales;
        private double creditSales;
        private double serviceRevenue;
        private double quickServiceRevenue;
        private double tyreExportRevenue;
        private double totalExpenses;
        private double workerCosts;
        private double netProfit;

        // Getters and setters
        public double getTotalSales() { return totalSales; }
        public void setTotalSales(double totalSales) { this.totalSales = totalSales; }
        
        public double getCreditSales() { return creditSales; }
        public void setCreditSales(double creditSales) { this.creditSales = creditSales; }
        
        public double getServiceRevenue() { return serviceRevenue; }
        public void setServiceRevenue(double serviceRevenue) { this.serviceRevenue = serviceRevenue; }
        
        public double getQuickServiceRevenue() { return quickServiceRevenue; }
        public void setQuickServiceRevenue(double quickServiceRevenue) { this.quickServiceRevenue = quickServiceRevenue; }
        
        public double getTyreExportRevenue() { return tyreExportRevenue; }
        public void setTyreExportRevenue(double tyreExportRevenue) { this.tyreExportRevenue = tyreExportRevenue; }
        
        public double getTotalExpenses() { return totalExpenses; }
        public void setTotalExpenses(double totalExpenses) { this.totalExpenses = totalExpenses; }
        
        public double getWorkerCosts() { return workerCosts; }
        public void setWorkerCosts(double workerCosts) { this.workerCosts = workerCosts; }
        
        public double getNetProfit() { return netProfit; }
        public void setNetProfit(double netProfit) { this.netProfit = netProfit; }
        
        public double getTotalRevenue() {
            return totalSales + creditSales + serviceRevenue + quickServiceRevenue + tyreExportRevenue;
        }
        
        public double getTotalCosts() {
            return totalExpenses + workerCosts;
        }
    }

    public static class TopProduct {
        private final String productName;
        private final int quantity;
        private final double revenue;

        public TopProduct(String productName, int quantity, double revenue) {
            this.productName = productName;
            this.quantity = quantity;
            this.revenue = revenue;
        }

        public String getProductName() { return productName; }
        public int getQuantity() { return quantity; }
        public double getRevenue() { return revenue; }
    }

    public static class DailySummary {
        private final LocalDate date;
        private final int invoiceCount;
        private final int totalItems;
        private final double totalRevenue;

        public DailySummary(LocalDate date, int invoiceCount, int totalItems, double totalRevenue) {
            this.date = date;
            this.invoiceCount = invoiceCount;
            this.totalItems = totalItems;
            this.totalRevenue = totalRevenue;
        }

        public LocalDate getDate() { return date; }
        public int getInvoiceCount() { return invoiceCount; }
        public int getTotalItems() { return totalItems; }
        public double getTotalRevenue() { return totalRevenue; }
    }

    public static class CustomerSummary {
        private final String customer;
        private final int purchaseCount;
        private final double totalAmount;
        private final double totalPaid;
        private final double outstanding;

        public CustomerSummary(String customer, int purchaseCount, double totalAmount, 
                              double totalPaid, double outstanding) {
            this.customer = customer;
            this.purchaseCount = purchaseCount;
            this.totalAmount = totalAmount;
            this.totalPaid = totalPaid;
            this.outstanding = outstanding;
        }

        public String getCustomer() { return customer; }
        public int getPurchaseCount() { return purchaseCount; }
        public double getTotalAmount() { return totalAmount; }
        public double getTotalPaid() { return totalPaid; }
        public double getOutstanding() { return outstanding; }
    }
}
