package com.gui.kline.controller;

import com.gui.kline.data.ReportsRepository;
import com.gui.kline.models.reports.CustomerSummary;
import com.gui.kline.models.reports.DailySummary;
import com.gui.kline.models.reports.ExpenseItem;
import com.gui.kline.models.reports.FinancialSummary;
import com.gui.kline.models.reports.TopProduct;
import com.gui.kline.models.ViewModel;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import com.gui.kline.service.PDFExportService;

import java.io.File;
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
    @FXML private VBox paymentHistoryContainer;
    
    // Services tab elements
    @FXML private VBox servicesTabContent;
    
    // Expenses tab elements
    @FXML private VBox expensesTabContent;

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

    public record PaymentItem(
            String id,
            String customerOrCompany,
            String refId,
            String type,
            LocalDate date,
            double amount,
            String method,
            String notes
    ) {}

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUI();
        initializeDatePickers();
        setupEventHandlers();
        loadInitialData();
        // Register with ViewFactory for cross-controller refresh
        ViewModel.INSTANCE.getViewsFactory().setReportsController(this);
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

    public void refresh() {
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
        buildExpensesTabContent(expenses);
        buildServicesTabContent(services);
        buildTopProductsSection(from, to);
        buildDailySalesSummary(from, to);
        buildCustomerAnalysis(from, to);
        buildPaymentHistory(from, to);
    }

    private void updateSummaryMetrics(FinancialSummary summary) {
        double totalRevenue = summary.getTotalRevenue();
        // Gross profit = Total Revenue - Cost of Goods Sold (Product Costs)
        double grossProfit = totalRevenue - summary.getProductCosts();
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

    private void buildServicesTabContent(List<ServiceItem> services) {
        if (servicesTabContent == null) return;
        
        servicesTabContent.getChildren().clear();
        if (services.isEmpty()) {
            servicesTabContent.getChildren().add(emptyLabel("No services in this period"));
            return;
        }
        
        // Group by date
        Map<LocalDate, List<ServiceItem>> byDate = new LinkedHashMap<>();
        for (ServiceItem service : services) {
            byDate.computeIfAbsent(service.date(), k -> new ArrayList<>()).add(service);
        }
        
        for (Map.Entry<LocalDate, List<ServiceItem>> entry : byDate.entrySet()) {
            VBox dateBox = new VBox(8);
            dateBox.setPadding(new Insets(0, 0, 16, 0));
            
            // Date header with total
            double dateTotal = entry.getValue().stream().mapToDouble(ServiceItem::fee).sum();
            Label dateHeader = new Label(entry.getKey().format(DF) + " (Rs. " + formatCurrency(dateTotal) + ")");
            dateHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #111827;");
            dateBox.getChildren().add(dateHeader);
            
            // Service items
            for (ServiceItem service : entry.getValue()) {
                dateBox.getChildren().add(buildServiceRowForTab(service));
            }
            
            servicesTabContent.getChildren().add(dateBox);
        }
    }

    private HBox buildServiceRowForTab(ServiceItem item) {
        Label name = new Label(item.name());
        name.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        String assignee = (item.assignedTo() == null || item.assignedTo().isBlank()) ? "Standard Service" : item.assignedTo();
        Label sub = new Label("Type/Worker: " + assignee);
        sub.setStyle("-fx-font-size: 10px; -fx-text-fill: #6B7280;");

        VBox left = new VBox(2, name, sub);
        HBox.setHgrow(left, Priority.ALWAYS);

        Label fee = new Label("Rs. " + formatCurrency(item.fee()));
        fee.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2563EB;");
        fee.setAlignment(Pos.CENTER_RIGHT);

        VBox right = new VBox(fee);
        right.setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox(left, right);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("report-row");
        return row;
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) return "Other";
        String lower = category.trim().toLowerCase();
        if (lower.contains("worker") || lower.contains("salary") || lower.contains("payroll")) {
            return "Worker Salary";
        }
        if (lower.contains("transport") || lower.contains("freight") || lower.contains("delivery")) {
            return "Transport";
        }
        if (lower.contains("tyre purchase") || lower.contains("export")) {
            return "Tyre Purchase";
        }
        return category.trim().substring(0, 1).toUpperCase() + category.trim().substring(1);
    }

    private void buildExpensesTabContent(List<ExpenseItem> expenses) {
        if (expensesTabContent == null) return;
        
        expensesTabContent.getChildren().clear();
        if (expenses.isEmpty()) {
            expensesTabContent.getChildren().add(emptyLabel("No expenses in this period"));
            return;
        }
        
        // Group by normalized category
        Map<String, List<ExpenseItem>> byCategory = new LinkedHashMap<>();
        for (ExpenseItem expense : expenses) {
            String categoryKey = normalizeCategory(expense.getCategory());
            byCategory.computeIfAbsent(categoryKey, k -> new ArrayList<>()).add(expense);
        }
        
        for (Map.Entry<String, List<ExpenseItem>> entry : byCategory.entrySet()) {
            VBox categoryBox = new VBox(8);
            categoryBox.setPadding(new Insets(0, 0, 16, 0));
            
            // Category header with total
            double categoryTotal = entry.getValue().stream().mapToDouble(ExpenseItem::getAmount).sum();
            Label categoryHeader = new Label(entry.getKey() + " Expenses (Rs. " + formatCurrency(categoryTotal) + ")");
            categoryHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #111827;");
            categoryBox.getChildren().add(categoryHeader);
            
            // Expense items
            for (ExpenseItem expense : entry.getValue()) {
                categoryBox.getChildren().add(buildExpenseRow(expense));
            }
            
            expensesTabContent.getChildren().add(categoryBox);
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
        
        // FXML already has the header
        
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
        
        // FXML already has the header
        
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
        
        // FXML already has the header
        
        for (CustomerSummary customer : customerSummaries) {
            customerSummaryContainer.getChildren().add(buildCustomerSummaryRow(customer));
        }
    }

    private void buildPaymentHistory(LocalDate from, LocalDate to) {
        if (paymentHistoryContainer == null) return;
        
        paymentHistoryContainer.getChildren().clear();
        
        List<PaymentItem> payments = reportsRepository.getPaymentTransactions(from, to);
        if (payments.isEmpty()) {
            paymentHistoryContainer.getChildren().add(emptyLabel("No payment transactions in this period"));
            return;
        }
        
        // FXML already has the header
        
        for (PaymentItem payment : payments) {
            paymentHistoryContainer.getChildren().add(buildPaymentRow(payment));
        }
    }

    private HBox buildPaymentRow(PaymentItem item) {
        Label name = new Label(item.customerOrCompany());
        name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        String method = (item.method() != null && !item.method().isBlank()) ? item.method() : "Cash";
        Label sub = new Label(item.date().format(DF) + " • " + item.type() + " (" + item.refId() + ") • Method: " + method);
        sub.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;");

        VBox left = new VBox(3, name, sub);
        HBox.setHgrow(left, Priority.ALWAYS);

        Label amount = new Label("Rs. " + formatCurrency(item.amount()));
        amount.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #16A34A;");
        amount.setAlignment(Pos.CENTER_RIGHT);

        String note = (item.notes() != null && !item.notes().isBlank()) ? item.notes() : "Settlement";
        Label noteLabel = new Label(note);
        noteLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #9CA3AF;");
        noteLabel.setAlignment(Pos.CENTER_RIGHT);

        VBox right = new VBox(3, amount, noteLabel);
        right.setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox(left, right);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("report-row");
        return row;
    }

    private HBox buildSaleRow(SaleItem item) {
        // Detect item type from name suffix
        boolean isCredit = item.name().endsWith("(Credit)");
        boolean isTyreExport = item.name().startsWith("Tyre Export -");

        String displayName = item.name();
        String typeTag;
        String typeColor;
        if (isCredit) {
            typeTag = "CREDIT SALE";
            typeColor = "#9333EA";
        } else if (isTyreExport) {
            typeTag = "TYRE EXPORT";
            typeColor = "#EA580C";
        } else {
            typeTag = "INVOICE";
            typeColor = "#2563EB";
        }

        Label name = new Label(displayName);
        name.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        String qtyText = item.qty() > 0 ? item.qty() + " unit" + (item.qty() != 1 ? "s" : "") : "—";
        Label sub = new Label(item.date().format(DF) + " • Qty: " + qtyText);
        sub.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;");

        Label badge = new Label(typeTag);
        badge.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: " + typeColor +
                "; -fx-background-color: transparent; -fx-border-color: " + typeColor +
                "; -fx-border-radius: 4; -fx-padding: 1 5 1 5;");

        VBox left = new VBox(2, new HBox(6, name, badge), sub);
        HBox.setHgrow(left, Priority.ALWAYS);

        Label rev = new Label("Rs. " + formatCurrency(item.revenue()));
        rev.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        rev.setAlignment(Pos.CENTER_RIGHT);

        Label profit = new Label("PROFIT: RS. " + formatCurrency(item.profit()));
        profit.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #16A34A;");
        profit.setAlignment(Pos.CENTER_RIGHT);

        VBox right = new VBox(3, rev, profit);
        right.setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox(left, right);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("report-row");
        return row;
    }

    private HBox buildServiceRow(ServiceItem item) {
        Label name = new Label(item.name());
        name.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        String assignee = (item.assignedTo() == null || item.assignedTo().isBlank()) ? "Standard Service" : item.assignedTo();
        Label sub = new Label(item.date().format(DF) + " • " + assignee);
        sub.setStyle("-fx-font-size: 10px; -fx-text-fill: #6B7280;");

        VBox left = new VBox(2, name, sub);
        HBox.setHgrow(left, Priority.ALWAYS);

        Label fee = new Label("Rs. " + formatCurrency(item.fee()));
        fee.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2563EB;");
        fee.setAlignment(Pos.CENTER_RIGHT);

        VBox right = new VBox(fee);
        right.setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox(left, right);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("report-row");
        return row;
    }

    private HBox buildExpenseRow(ExpenseItem expense) {
        Label description = new Label(expense.getDescription());
        description.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #111827;");

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
        row.getStyleClass().add("report-row");
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
        row.getStyleClass().add("report-row");

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
        row.getStyleClass().add("report-row");

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
        row.getStyleClass().add("report-row");

        return row;
    }

    private Label emptyLabel(String msg) {
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 13px; -fx-padding: 20 0 0 0;");
        return lbl;
    }

    private void filterReports(String searchText) {
        String query = searchText == null ? "" : searchText.trim().toLowerCase();
        if (query.isEmpty()) {
            buildSalesBreakdown(allSales);
            buildServiceRevenue(allServices);
            buildExpensesSection(allExpenses);
            buildExpensesTabContent(allExpenses);
            buildServicesTabContent(allServices);
            return;
        }

        List<SaleItem> filteredSales = allSales.stream()
                .filter(s -> s.name() != null && s.name().toLowerCase().contains(query))
                .toList();

        List<ServiceItem> filteredServices = allServices.stream()
                .filter(s -> (s.name() != null && s.name().toLowerCase().contains(query)) ||
                             (s.assignedTo() != null && s.assignedTo().toLowerCase().contains(query)))
                .toList();

        List<ExpenseItem> filteredExpenses = allExpenses.stream()
                .filter(e -> (e.getDescription() != null && e.getDescription().toLowerCase().contains(query)) ||
                             (e.getCategory() != null && e.getCategory().toLowerCase().contains(query)))
                .toList();

        buildSalesBreakdown(filteredSales);
        buildServiceRevenue(filteredServices);
        buildExpensesSection(filteredExpenses);
        buildExpensesTabContent(filteredExpenses);
        buildServicesTabContent(filteredServices);
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
        LocalDate to   = endDatePicker.getValue();

        if (from == null || to == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING,
                    "Please select a valid date range first.", ButtonType.OK);
            alert.setHeaderText("Date Range Required");
            alert.showAndWait();
            return;
        }

        String selectedReportType = reportTypeComboBox != null && reportTypeComboBox.getValue() != null 
                ? reportTypeComboBox.getValue() : "Summary Report";

        // Open file save dialog
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Save " + selectedReportType + " PDF");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        String safeTypeName = selectedReportType.replaceAll("[^a-zA-Z0-9]", "_");
        fc.setInitialFileName("KLine_" + safeTypeName + "_" + from + "_to_" + to + ".pdf");

        File file = fc.showSaveDialog(exportBtn.getScene().getWindow());
        if (file == null) return; // user cancelled

        // Lock button during generation
        exportBtn.setDisable(true);
        exportBtn.setText("Generating...");

        PDFExportService pdfService = new PDFExportService(reportsRepository);
        executorService.submit(() -> {
            boolean success;
            switch (selectedReportType) {
                case "Sales Analysis":
                    success = pdfService.exportSalesReportToPDF(from, to, file);
                    break;
                case "Service Revenue":
                    success = pdfService.exportServiceReportToPDF(from, to, file);
                    break;
                case "Expense Report":
                    success = pdfService.exportExpenseReportToPDF(from, to, file);
                    break;
                case "Customer Analysis":
                    success = pdfService.exportCustomerReportToPDF(from, to, file);
                    break;
                case "Daily Summary":
                    success = pdfService.exportDailySummaryReportToPDF(to, to, file);
                    break;
                case "Top Products":
                    success = pdfService.exportTopProductsReportToPDF(from, to, file);
                    break;
                case "Summary Report":
                default:
                    success = pdfService.exportSummaryReportToPDF(from, to, file);
                    break;
            }

            Platform.runLater(() -> {
                exportBtn.setDisable(false);
                exportBtn.setText("Export PDF");
                if (success) {
                    Alert ok = new Alert(Alert.AlertType.INFORMATION,
                            "Report saved to:\n" + file.getAbsolutePath(), ButtonType.OK);
                    ok.setHeaderText("PDF Report Generated");
                    ok.showAndWait();
                } else {
                    Alert err = new Alert(Alert.AlertType.ERROR,
                            "Failed to generate the PDF report. Please check the logs.", ButtonType.OK);
                    err.setHeaderText("Export Failed");
                    err.showAndWait();
                }
            });
        });
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
        
        report.append("--- EXPENSES & COSTS ---\n");
        report.append(String.format("Product Costs (COGS): Rs. %,.0f\n", summary.getProductCosts()));
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
