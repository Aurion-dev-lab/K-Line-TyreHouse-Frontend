package com.gui.kline.controller;

import com.gui.kline.data.ReportsRepository;
import com.gui.kline.data.ReportsRepository.DailySummary;
import com.gui.kline.data.ReportsRepository.ExpenseItem;
import com.gui.kline.data.ReportsRepository.FinancialSummary;
import com.gui.kline.data.ReportsRepository.TopProduct;
import com.gui.kline.data.ReportsRepository.CustomerSummary;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Enhanced Reports Controller with comprehensive reporting capabilities.
 * Handles financial reports, sales analysis, service revenue, expenses, and more.
 */
public class ReportsController implements Initializable {

    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Button exportBtn;
    @FXML private Button refreshBtn;

    @FXML private Label totalSalesLabel;
    @FXML private Label grossProfitLabel;
    @FXML private Label workerCostsLabel;
    @FXML private Label netIncomeLabel;

    @FXML private VBox salesBreakdownContainer;
    @FXML private VBox serviceRevenueContainer;
    @FXML private VBox expensesContainer;
    @FXML private VBox reportsContainer;

    @FXML private ComboBox<String> reportTypeComboBox;
    @FXML private TextField searchField;

    // Additional UI elements for enhanced reports
    @FXML private TabPane reportsTabPane;
    @FXML private Tab summaryTab;
    @FXML private Tab salesTab;
    @FXML private Tab servicesTab;
    @FXML private Tab expensesTab;
    @FXML private Tab analyticsTab;

    // Summary tab elements
    @FXML private VBox summaryContent;
    
    // Sales tab elements
    @FXML private VBox topProductsContainer;
    @FXML private VBox dailySalesContainer;
    
    // Analytics elements
    @FXML private VBox customerSummaryContainer;

    private final ReportsRepository reportsRepository = new ReportsRepository();
    private final ObservableList<SaleItem> allSales = FXCollections.observableArrayList();
    private final ObservableList<ServiceItem> allServices = FXCollections.observableArrayList();
    private final ObservableList<ExpenseItem> allExpenses = FXCollections.observableArrayList();

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final NumberFormat NF = NumberFormat.getInstance();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public record SaleItem(
            String name,
            LocalDate date,
            int qty,
            double revenue,
            double profit
    ) {}

    public record ServiceItem(
            String name,
            LocalDate date,
            String assignedTo,   // null -> "Unassigned"
            double fee
    ) {}

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUI();
        initializeDatePickers();
        setupEventHandlers();
        loadInitialData();
    }

    private void setupUI() {
        // Initialize report type combo box if available
        if (reportTypeComboBox != null) {
            reportTypeComboBox.getItems().addAll(
                "Summary Report",
                "Sales Analysis", 
                "Service Revenue",
                "Expense Report",
                "Customer Analysis",
                "Daily Summary",
                "Top Products"
            );
            reportTypeComboBox.getSelectionModel().selectFirst();
        }

        // Setup search field
        if (searchField != null) {
            searchField.setPromptText("Search reports...");
            searchField.textProperty().addListener((obs, oldVal, newVal) -> filterReports(newVal));
        }
    }

    private void initializeDatePickers() {
        endDatePicker.setValue(LocalDate.now());
        startDatePicker.setValue(LocalDate.now().minusDays(7));

        startDatePicker.valueProperty().addListener((obs, o, n) -> refresh());
        endDatePicker.valueProperty().addListener((obs, o, n) -> refresh());
    }

    private void setupEventHandlers() {
        if (refreshBtn != null) {
            refreshBtn.setOnAction(e -> refresh());
        }
    }

    private void loadInitialData() {
        refresh();
    }

    public void setData(List<SaleItem> sales, List<ServiceItem> services) {
        allSales.clear();
        allSales.addAll(sales);
        allServices.clear();
        allServices.addAll(services);
        refresh();
    }

    private void refresh() {
        LocalDate from = startDatePicker.getValue();
        LocalDate to = endDatePicker.getValue();
        if (from == null || to == null) return;

        // Load data asynchronously to prevent UI freezing
        executorService.submit(() -> {
            try {
                // Load all report data
                List<SaleItem> sales = reportsRepository.getSalesData(from, to);
                List<ServiceItem> services = reportsRepository.getServiceData(from, to);
                List<ExpenseItem> expenses = reportsRepository.getExpenses(from, to);
                FinancialSummary financialSummary = reportsRepository.getFinancialSummary(from, to);
                
                Platform.runLater(() -> {
                    updateUIWithData(sales, services, expenses, financialSummary, from, to);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Failed to load report data: " + e.getMessage());
                });
            }
        });
    }

    private void updateUIWithData(List<SaleItem> sales, List<ServiceItem> services, 
                                 List<ExpenseItem> expenses, FinancialSummary financialSummary,
                                 LocalDate from, LocalDate to) {
        // Update summary metrics
        updateSummaryMetrics(financialSummary);
        
        // Update data collections
        allSales.setAll(sales);
        allServices.setAll(services);
        allExpenses.setAll(expenses);
        
        // Build report sections
        buildSalesBreakdown(sales);
        buildServiceRevenue(services);
        buildExpensesSection(expenses);
        buildTopProductsSection(from, to);
        buildDailySalesSummary(from, to);
        buildCustomerAnalysis(from, to);
    }

    private void updateSummaryMetrics(FinancialSummary summary) {
        double totalRevenue = summary.getTotalRevenue();
        double grossProfit = totalRevenue - summary.getTotalExpenses();
        double workerCosts = summary.getWorkerCosts();
        double netIncome = summary.getNetProfit();

        totalSalesLabel.setText("Rs. " + formatCurrency(totalRevenue));
        grossProfitLabel.setText("Rs. " + formatCurrency(grossProfit));
        workerCostsLabel.setText("Rs. " + formatCurrency(workerCosts));
        netIncomeLabel.setText("Rs. " + formatCurrency(netIncome));
        
        // Update card colors based on values
        updateNetIncomeCardColor(netIncome);
    }

    private void updateNetIncomeCardColor(double netIncome) {
        // The net income card is already styled in FXML, but we can update it dynamically
        if (netIncome >= 0) {
            // Keep the existing dark theme for positive income
            netIncomeLabel.getParent().setStyle("-fx-background-color: #111827; -fx-background-radius: 14; -fx-padding: 20;");
        } else {
            // Red theme for negative income
            netIncomeLabel.getParent().setStyle("-fx-background-color: #991B1B; -fx-background-radius: 14; -fx-padding: 20;");
        }
    }

    private void buildSalesBreakdown(List<SaleItem> sales) {
        salesBreakdownContainer.getChildren().clear();
        if (sales.isEmpty()) {
            salesBreakdownContainer.getChildren().add(emptyLabel("No sales in this period"));
            return;
        }
        
        // Sort by revenue descending
        sales.stream()
                .sorted(Comparator.comparingDouble(SaleItem::revenue).reversed())
                .forEach(item -> salesBreakdownContainer.getChildren().add(buildSaleRow(item)));
    }

    private void buildServiceRevenue(List<ServiceItem> services) {
        serviceRevenueContainer.getChildren().clear();
        if (services.isEmpty()) {
            serviceRevenueContainer.getChildren().add(emptyLabel("No services in this period"));
            return;
        }
        
        services.stream()
                .sorted(Comparator.comparingDouble(ServiceItem::fee).reversed())
                .forEach(item -> serviceRevenueContainer.getChildren().add(buildServiceRow(item)));
    }

    private void buildExpensesSection(List<ExpenseItem> expenses) {
        if (expensesContainer == null) return;
        
        expensesContainer.getChildren().clear();
        if (expenses.isEmpty()) {
            expensesContainer.getChildren().add(emptyLabel("No expenses in this period"));
            return;
        }
        
        // Group by category
        Map<String, List<ExpenseItem>> byCategory = new HashMap<>();
        for (ExpenseItem expense : expenses) {
            byCategory.computeIfAbsent(expense.getCategory(), k -> new ArrayList<>()).add(expense);
        }
        
        for (Map.Entry<String, List<ExpenseItem>> entry : byCategory.entrySet()) {
            VBox categoryBox = new VBox(8);
            categoryBox.setPadding(new Insets(0, 0, 16, 0));
            
            // Category header
            Label categoryHeader = new Label(entry.getKey() + " Expenses");
            categoryHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #111827;");
            categoryBox.getChildren().add(categoryHeader);
            
            // Expense items
            for (ExpenseItem expense : entry.getValue()) {
                categoryBox.getChildren().add(buildExpenseRow(expense));
            }
            
            expensesContainer.getChildren().add(categoryBox);
        }
    }

    private void buildTopProductsSection(LocalDate from, LocalDate to) {
        if (topProductsContainer == null) return;
        
        topProductsContainer.getChildren().clear();
        
        ObservableList<TopProduct> topProducts = reportsRepository.getTopSellingProducts(from, to, 10);
        if (topProducts.isEmpty()) {
            topProductsContainer.getChildren().add(emptyLabel("No product data available"));
            return;
        }
        
        Label title = new Label("Top Selling Products");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        topProductsContainer.getChildren().add(title);
        topProductsContainer.getChildren().add(new Separator());
        
        for (TopProduct product : topProducts) {
            topProductsContainer.getChildren().add(buildTopProductRow(product));
        }
    }

    private void buildDailySalesSummary(LocalDate from, LocalDate to) {
        if (dailySalesContainer == null) return;
        
        dailySalesContainer.getChildren().clear();
        
        ObservableList<DailySummary> dailySummaries = reportsRepository.getDailySalesSummary(from, to);
        if (dailySummaries.isEmpty()) {
            dailySalesContainer.getChildren().add(emptyLabel("No daily sales data available"));
            return;
        }
        
        Label title = new Label("Daily Sales Summary");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        dailySalesContainer.getChildren().add(title);
        dailySalesContainer.getChildren().add(new Separator());
        
        for (DailySummary summary : dailySummaries) {
            dailySalesContainer.getChildren().add(buildDailySummaryRow(summary));
        }
    }

    private void buildCustomerAnalysis(LocalDate from, LocalDate to) {
        if (customerSummaryContainer == null) return;
        
        customerSummaryContainer.getChildren().clear();
        
        ObservableList<CustomerSummary> customerSummaries = reportsRepository.getCustomerPurchaseSummary(from, to);
        if (customerSummaries.isEmpty()) {
            customerSummaryContainer.getChildren().add(emptyLabel("No customer credit data available"));
            return;
        }
        
        Label title = new Label("Customer Credit Analysis");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        customerSummaryContainer.getChildren().add(title);
        customerSummaryContainer.getChildren().add(new Separator());
        
        for (CustomerSummary customer : customerSummaries) {
            customerSummaryContainer.getChildren().add(buildCustomerSummaryRow(customer));
        }
    }

    private HBox buildSaleRow(SaleItem item) {
        Label name = new Label(item.name());
        name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label sub = new Label(item.date().format(DF) + " • " + item.qty() + " unit" + (item.qty() != 1 ? "s" : ""));
        sub.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");

        VBox left = new VBox(3, name, sub);
        HBox.setHgrow(left, Priority.ALWAYS);

        Label rev = new Label("Rs. " + formatCurrency(item.revenue()));
        rev.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        rev.setAlignment(Pos.CENTER_RIGHT);

        Label profit = new Label("PROFIT: RS. " + formatCurrency(item.profit()));
        profit.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #16A34A;");
        profit.setAlignment(Pos.CENTER_RIGHT);

        VBox right = new VBox(3, rev, profit);
        right.setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox(left, right);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 0, 14, 0));

        VBox wrapper = new VBox(new Separator(), row);
        wrapper.setPadding(new Insets(0));
        HBox outer = new HBox(wrapper);
        HBox.setHgrow(wrapper, Priority.ALWAYS);
        return outer;
    }

    private HBox buildServiceRow(ServiceItem item) {
        Label name = new Label(item.name());
        name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        String assignee = (item.assignedTo() == null || item.assignedTo().isBlank()) ? "Unassigned" : item.assignedTo();
        Label sub = new Label(item.date().format(DF) + " • " + assignee);
        sub.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");

        VBox left = new VBox(3, name, sub);
        HBox.setHgrow(left, Priority.ALWAYS);

        Label fee = new Label("Rs. " + formatCurrency(item.fee()));
        fee.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2563EB;");
        fee.setAlignment(Pos.CENTER_RIGHT);

        Label feeTag = new Label("SERVICE FEE");
        feeTag.setStyle("-fx-font-size: 10px; -fx-text-fill: #9CA3AF;");
        feeTag.setAlignment(Pos.CENTER_RIGHT);

        VBox right = new VBox(3, fee, feeTag);
        right.setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox(left, right);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 0, 14, 0));

        VBox wrapper = new VBox(new Separator(), row);
        HBox outer = new HBox(wrapper);
        HBox.setHgrow(wrapper, Priority.ALWAYS);
        return outer;
    }

    private HBox buildExpenseRow(ExpenseItem expense) {
        Label description = new Label(expense.getDescription());
        description.setStyle("-fx-font-size: 13px; -fx-text-fill: #111827;");

        Label dateLabel = new Label(expense.getDate().format(DF) + " • " + expense.getCategory());
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");

        VBox left = new VBox(3, description, dateLabel);
        HBox.setHgrow(left, Priority.ALWAYS);

        Label amount = new Label("Rs. " + formatCurrency(expense.getAmount()));
        amount.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #DC2626;");
        amount.setAlignment(Pos.CENTER_RIGHT);

        VBox right = new VBox(amount);
        right.setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox(left, right);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 0, 12, 0));

        return row;
    }

    private HBox buildTopProductRow(TopProduct product) {
        Label name = new Label(product.getProductName());
        name.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label quantityLabel = new Label("Qty: " + product.getQuantity() + " units");
        quantityLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;");

        VBox left = new VBox(3, name, quantityLabel);
        HBox.setHgrow(left, Priority.ALWAYS);

        Label revenue = new Label("Rs. " + formatCurrency(product.getRevenue()));
        revenue.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #16A34A;");
        revenue.setAlignment(Pos.CENTER_RIGHT);

        VBox right = new VBox(revenue);
        right.setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox(left, right);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 0, 12, 0));

        return row;
    }

    private HBox buildDailySummaryRow(DailySummary summary) {
        Label date = new Label(summary.getDate().format(DF));
        date.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label details = new Label(summary.getInvoiceCount() + " invoices • " + 
                                 summary.getTotalItems() + " items");
        details.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;");

        VBox left = new VBox(3, date, details);
        HBox.setHgrow(left, Priority.ALWAYS);

        Label revenue = new Label("Rs. " + formatCurrency(summary.getTotalRevenue()));
        revenue.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2563EB;");
        revenue.setAlignment(Pos.CENTER_RIGHT);

        VBox right = new VBox(revenue);
        right.setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox(left, right);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 0, 12, 0));

        return row;
    }

    private HBox buildCustomerSummaryRow(CustomerSummary customer) {
        Label name = new Label(customer.getCustomer());
        name.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label details = new Label(customer.getPurchaseCount() + " purchases");
        details.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;");

        VBox left = new VBox(3, name, details);
        HBox.setHgrow(left, Priority.ALWAYS);

        Label totalAmount = new Label("Rs. " + formatCurrency(customer.getTotalAmount()));
        totalAmount.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        totalAmount.setAlignment(Pos.CENTER_RIGHT);

        Label paid = new Label("Paid: Rs. " + formatCurrency(customer.getTotalPaid()));
        paid.setStyle("-fx-font-size: 10px; -fx-text-fill: #16A34A;");
        paid.setAlignment(Pos.CENTER_RIGHT);

        Label outstanding = new Label("Due: Rs. " + formatCurrency(customer.getOutstanding()));
        outstanding.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #DC2626;");
        outstanding.setAlignment(Pos.CENTER_RIGHT);

        VBox right = new VBox(3, totalAmount, paid, outstanding);
        right.setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox(left, right);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 0, 12, 0));

        return row;
    }

    private Label emptyLabel(String msg) {
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 13px; -fx-padding: 20 0 0 0;");
        return lbl;
    }

    private void filterReports(String searchText) {
        if (searchText == null || searchText.isBlank()) {
            refresh();
            return;
        }
        
        // Filter logic would go here
        // For now, just refresh to show all data
        refresh();
    }

    private String formatCurrency(double value) {
        NF.setMaximumFractionDigits(0);
        return NF.format(value);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText("Error Loading Reports");
        alert.showAndWait();
    }

    @FXML
    private void handleExportPDF() {
        LocalDate from = startDatePicker.getValue();
        LocalDate to = endDatePicker.getValue();
        
        if (from == null || to == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, 
                    "Please select a valid date range first.", ButtonType.OK);
            alert.setHeaderText("Date Range Required");
            alert.showAndWait();
            return;
        }

        String reportText = generateBusinessReport(from, to);
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "Generating PDF report for period: " + from + " to " + to + "\n\n" +
                "PDF export functionality will be implemented with a PDF library.\n\n" + reportText,
                ButtonType.OK);
        alert.setHeaderText("PDF Export");
        alert.setTitle("Generate Report");
        alert.showAndWait();
    }

    @FXML
    private void handleRefresh() {
        refresh();
    }

    private double computeWorkerCosts(LocalDate from, LocalDate to) {
        // Use the repository method instead
        return reportsRepository.getWorkerCosts(from, to);
    }

    // Cleanup executor service when controller is no longer needed
    public void cleanup() {
        executorService.shutdown();
    }

    // Additional report generation methods

    /**
     * Generate a comprehensive business report
     */
    public String generateBusinessReport(LocalDate from, LocalDate to) {
        FinancialSummary summary = reportsRepository.getFinancialSummary(from, to);
        
        StringBuilder report = new StringBuilder();
        report.append("=== BUSINESS REPORT ===\n\n");
        report.append("Period: ").append(from).append(" to ").append(to).append("\n\n");
        
        report.append("--- FINANCIAL SUMMARY ---\n");
        report.append(String.format("Total Sales Revenue: Rs. %,.0f\n", summary.getTotalSales()));
        report.append(String.format("Credit Sales: Rs. %,.0f\n", summary.getCreditSales()));
        report.append(String.format("Service Revenue: Rs. %,.0f\n", summary.getServiceRevenue()));
        report.append(String.format("Quick Services: Rs. %,.0f\n", summary.getQuickServiceRevenue()));
        report.append(String.format("Total Revenue: Rs. %,.0f\n\n", summary.getTotalRevenue()));
        
        report.append("--- EXPENSES ---\n");
        report.append(String.format("Total Expenses: Rs. %,.0f\n", summary.getTotalExpenses()));
        report.append(String.format("Worker Costs: Rs. %,.0f\n", summary.getWorkerCosts()));
        report.append(String.format("Total Costs: Rs. %,.0f\n\n", summary.getTotalCosts()));
        
        report.append("--- PROFITABILITY ---\n");
        report.append(String.format("Net Profit: Rs. %,.0f\n", summary.getNetProfit()));
        
        return report.toString();
    }

    /**
     * Generate sales report
     */
    public String generateSalesReport(LocalDate from, LocalDate to) {
        List<SaleItem> sales = reportsRepository.getSalesData(from, to);
        
        StringBuilder report = new StringBuilder();
        report.append("=== SALES REPORT ===\n\n");
        report.append("Period: ").append(from).append(" to ").append(to).append("\n");
        report.append("Total Transactions: ").append(sales.size()).append("\n\n");
        
        double totalRevenue = sales.stream().mapToDouble(SaleItem::revenue).sum();
        double totalProfit = sales.stream().mapToDouble(SaleItem::profit).sum();
        
        report.append(String.format("Total Revenue: Rs. %,.0f\n", totalRevenue));
        report.append(String.format("Total Profit: Rs. %,.0f\n\n", totalProfit));
        
        report.append("-- Top 10 Sales --\n");
        sales.stream()
                .sorted(Comparator.comparingDouble(SaleItem::revenue).reversed())
                .limit(10)
                .forEach(item -> report.append(String.format("%-25s %10s %12s %12s\n", 
                        truncate(item.name(), 25), 
                        item.qty(), 
                        formatCurrency(item.revenue()), 
                        formatCurrency(item.profit()))));
        
        return report.toString();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 3) + "...";
    }

    // Utility method for other controllers to access report data
    public ReportsRepository getReportsRepository() {
        return reportsRepository;
    }

    public ObservableList<SaleItem> getAllSales() {
        return allSales;
    }

    public ObservableList<ServiceItem> getAllServices() {
        return allServices;
    }

    public ObservableList<ExpenseItem> getAllExpenses() {
        return allExpenses;
    }
}
