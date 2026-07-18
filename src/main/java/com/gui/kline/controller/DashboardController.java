package com.gui.kline.controller;

import com.gui.kline.controller.form.QuickActionsPopupController;
import com.gui.kline.data.*;
import com.gui.kline.models.Product;
import com.gui.kline.models.ExportRecord;
import com.gui.kline.models.ViewModel;
import com.gui.kline.service.NavigationService;
import com.gui.kline.utils.BackgroundTask;
import com.gui.kline.utils.JsonUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class DashboardController implements Initializable {

    @FXML private Label periodSalesLabel;
    @FXML private Label periodProfitLabel;
    @FXML private Label periodServicesLabel;
    @FXML private Label activeWorkersLabel;

    @FXML private Label salesTrendLabel;
    @FXML private Label profitTrendLabel;
    @FXML private Label servicesTrendLabel;
    @FXML private Label workersTrendLabel;

    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;

    @FXML private AreaChart<String, Number> revenueChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private ComboBox<String> chartRangeCombo;

    @FXML private VBox   stockAlertBox;
    @FXML private Label  stockAlertLabel;
    @FXML private Hyperlink viewInventoryLink;

    @FXML private Label quickServiceCountLabel;
    @FXML private Label quickServiceRevenueLabel;

    @FXML private Button newSaleBtn;
    @FXML private Button addServiceBtn;
    @FXML private Button logWorkBtn;
    @FXML private Button newExportBtn;
    @FXML private GridPane quickActionsGrid;

    private final LocalCatalogRepository catalogRepository = new LocalCatalogRepository();
    private final SyncQueueRepository syncQueueRepository = new SyncQueueRepository();
    
    private List<QuickService> quickServices = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupDatePickers();
        setupChartRangeCombo();
        applyChartStyles();

        // Load data-heavy sections on background threads to prevent UI freeze
        BackgroundTask.run(this::loadKpiDataSync, this::applyKpiData);
        BackgroundTask.run(this::loadQuickServiceStatsSync, this::applyQuickServiceStats);
        BackgroundTask.run(this::loadQuickServicesSync, services -> {
            quickServices = services;
            populateQuickActionsGrid();
        });

        loadChartData("Last 7 Days");
        loadStockAlerts();

        // Register with ViewFactory for cross-controller refresh
        ViewModel.INSTANCE.getViewsFactory().setDashboardController(this);
    }

    // Holder for background-loaded KPI data
    private static class KPIMetricsData {
        KPIMetrics current;
        KPIMetrics previous;
    }

    private KPIMetricsData loadKpiDataSync() {
        LocalDate startDate = startDatePicker.getValue() != null ? startDatePicker.getValue() : LocalDate.now().minusDays(30);
        LocalDate endDate = endDatePicker.getValue() != null ? endDatePicker.getValue() : LocalDate.now();

        KPIMetricsData data = new KPIMetricsData();
        data.current = calculateMetrics(startDate, endDate);

        long daysDiff = ChronoUnit.DAYS.between(startDate, endDate);
        LocalDate prevStart = startDate.minusDays(daysDiff + 1);
        LocalDate prevEnd = startDate.minusDays(1);
        data.previous = calculateMetrics(prevStart, prevEnd);
        return data;
    }

    private void applyKpiData(KPIMetricsData data) {
        // Called on FX thread - takes the pre-computed metrics and updates labels
        try {
            KPIMetrics current = data.current;
            KPIMetrics previous = data.previous;

            periodSalesLabel.setText("Rs. " + formatAmount(current.sales));
            periodProfitLabel.setText("Rs. " + formatAmount(current.profit));
            periodServicesLabel.setText(String.valueOf(current.services));
            activeWorkersLabel.setText(String.valueOf(current.workers));

            double salesTrend = calculateTrendPercent(previous.sales, current.sales);
            double profitTrend = calculateTrendPercent(previous.profit, current.profit);
            double servicesTrend = calculateTrendPercent(previous.services, current.services);
            double workersTrend = calculateTrendPercent(previous.workers, current.workers);

            setSalesTrend(salesTrend);
            setProfitTrend(profitTrend);
            setServicesTrend(servicesTrend);
            setWorkersTrend(workersTrend);
        } catch (Exception ex) {
            System.err.println("Error loading KPI data: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private QuickServiceStats loadQuickServiceStatsSync() {
        if (quickServiceCountLabel == null || quickServiceRevenueLabel == null) {
            return new QuickServiceStats(0, 0);
        }
        LocalDate startDate = startDatePicker.getValue() != null ? startDatePicker.getValue() : LocalDate.now().minusDays(30);
        LocalDate endDate = endDatePicker.getValue() != null ? endDatePicker.getValue() : LocalDate.now();
        try (Connection conn = DatabaseManager.getConnection()) {
            int count = countRows(conn,
                    "SELECT COUNT(*) FROM quick_services WHERE service_date BETWEEN ? AND ?",
                    startDate, endDate);
            double revenue = sumAmount(conn,
                    "SELECT COALESCE(SUM(price),0) FROM quick_services WHERE service_date BETWEEN ? AND ?",
                    startDate, endDate);
            return new QuickServiceStats(count, revenue);
        } catch (SQLException ex) {
            System.err.println("Error loading quick service stats: " + ex.getMessage());
            return new QuickServiceStats(0, 0);
        }
    }

    private void applyQuickServiceStats(QuickServiceStats stats) {
        if (quickServiceCountLabel != null && quickServiceRevenueLabel != null) {
            quickServiceCountLabel.setText(String.valueOf(stats.count));
            quickServiceRevenueLabel.setText("Rs. " + formatAmount(stats.revenue));
        }
    }

    private static class QuickServiceStats {
        final int count;
        final double revenue;
        QuickServiceStats(int count, double revenue) { this.count = count; this.revenue = revenue; }
    }

    private List<QuickService> loadQuickServicesSync() {
        List<QuickService> services = new ArrayList<>();
        String sql = "SELECT id, service, price, icon FROM quick_service_presets WHERE active = 1 ORDER BY service";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                services.add(new QuickService(
                        rs.getString("id"),
                        rs.getString("service"),
                        rs.getDouble("price"),
                        rs.getString("icon")
                ));
            }
        } catch (SQLException ex) {
            System.err.println("Error loading quick services: " + ex.getMessage());
        }
        return services;
    }

    private void setupDatePickers() {
        startDatePicker.setValue(LocalDate.now().minusDays(30));
        endDatePicker.setValue(LocalDate.now());

        startDatePicker.valueProperty().addListener((obs, o, n) -> refreshData());
        endDatePicker.valueProperty().addListener((obs, o, n) -> refreshData());
    }

    private void setupChartRangeCombo() {
        ObservableList<String> ranges = FXCollections.observableArrayList(
                "Last 7 Days", "Last 30 Days", "Last 3 Months", "Last Year"
        );
        chartRangeCombo.setItems(ranges);
        chartRangeCombo.setValue("Last 7 Days");
        chartRangeCombo.valueProperty().addListener((obs, o, n) -> loadChartData(n));
    }

    private void loadKpiData() {
        try {
            LocalDate startDate = startDatePicker.getValue() != null ? startDatePicker.getValue() : LocalDate.now().minusDays(30);
            LocalDate endDate = endDatePicker.getValue() != null ? endDatePicker.getValue() : LocalDate.now();

            KPIMetrics current = calculateMetrics(startDate, endDate);

            long daysDiff = ChronoUnit.DAYS.between(startDate, endDate);
            LocalDate prevStart = startDate.minusDays(daysDiff + 1);
            LocalDate prevEnd = startDate.minusDays(1);
            KPIMetrics previous = calculateMetrics(prevStart, prevEnd);

            periodSalesLabel.setText("Rs. " + formatAmount(current.sales));
            periodProfitLabel.setText("Rs. " + formatAmount(current.profit));
            periodServicesLabel.setText(String.valueOf(current.services));
            activeWorkersLabel.setText(String.valueOf(current.workers));

            double salesTrend = calculateTrendPercent(previous.sales, current.sales);
            double profitTrend = calculateTrendPercent(previous.profit, current.profit);
            double servicesTrend = calculateTrendPercent(previous.services, current.services);
            double workersTrend = calculateTrendPercent(previous.workers, current.workers);

            setSalesTrend(salesTrend);
            setProfitTrend(profitTrend);
            setServicesTrend(servicesTrend);
            setWorkersTrend(workersTrend);
        } catch (Exception ex) {
            System.err.println("Error loading KPI data: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    private KPIMetrics calculateMetrics(LocalDate startDate, LocalDate endDate) {
        KPIMetrics metrics = new KPIMetrics();

        try (Connection conn = DatabaseManager.getConnection()) {
            metrics.sales = sumRevenue(conn, startDate, endDate);
            metrics.profit = sumProfit(conn, startDate, endDate);
            metrics.services = countServices(conn, startDate, endDate);
            metrics.workers = countActiveWorkers(conn, startDate, endDate);
        } catch (SQLException ex) {
            System.err.println("Error calculating metrics: " + ex.getMessage());
        }

        return metrics;
    }
    
    private double calculateTrendPercent(double previous, double current) {
        if (previous == 0 && current == 0) return 0;
        if (previous == 0) return 100;
        return ((current - previous) / previous) * 100;
    }

    private void loadChartData(String range) {
        revenueChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenue");

        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(6);
            String groupBy = "day";
            boolean useDayNames = true;

            switch (range) {
                case "Last 7 Days" -> {
                    startDate = endDate.minusDays(6);
                    useDayNames = true;
                }
                case "Last 30 Days" -> {
                    startDate = endDate.minusDays(29);
                    useDayNames = false;
                }
                case "Last 3 Months" -> {
                    startDate = endDate.minusMonths(3);
                    groupBy = "month";
                    useDayNames = false;
                }
                case "Last Year" -> {
                    startDate = endDate.minusYears(1);
                    groupBy = "month";
                    useDayNames = false;
                }
            }

            Map<LocalDate, Double> totals = loadRevenueTotals(startDate, endDate);
            populateChartData(series, totals, startDate, endDate, groupBy, useDayNames);
        } catch (Exception ex) {
            System.err.println("Error loading chart data: " + ex.getMessage());
        }

        revenueChart.getData().add(series);

        series.getNode().setStyle(
                "-fx-stroke: #22c55e; -fx-stroke-width: 2.5px;"
        );
    }

    private void populateChartData(XYChart.Series<String, Number> series,
                                   Map<LocalDate, Double> totals,
                                   LocalDate startDate,
                                   LocalDate endDate,
                                   String groupBy,
                                   boolean useDayNames) {
        Map<String, Double> groupedData = new LinkedHashMap<>();

        if ("day".equals(groupBy)) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM");
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                String label = useDayNames ? current.getDayOfWeek().name().substring(0, 3) : current.format(formatter);
                groupedData.put(label, 0.0);
                current = current.plusDays(1);
            }

            totals.forEach((date, value) -> {
                if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                    String label = useDayNames ? date.getDayOfWeek().name().substring(0, 3) : date.format(formatter);
                    groupedData.merge(label, value, Double::sum);
                }
            });
        } else if ("month".equals(groupBy)) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM");
            YearMonth current = YearMonth.from(startDate);
            YearMonth end = YearMonth.from(endDate);
            while (!current.isAfter(end)) {
                groupedData.put(current.format(formatter), 0.0);
                current = current.plusMonths(1);
            }

            totals.forEach((date, value) -> {
                YearMonth month = YearMonth.from(date);
                String label = month.format(formatter);
                groupedData.merge(label, value, Double::sum);
            });
        }

        groupedData.forEach((key, value) -> series.getData().add(new XYChart.Data<>(key, value)));
    }

    private void loadStockAlerts() {
        try {
            List<Product> products = catalogRepository.loadProducts();
            List<Product> lowStockItems = products.stream()
                    .filter(Product::isLowStock)
                    .collect(Collectors.toList());
            
            int lowStockCount = lowStockItems.size();
            if (lowStockCount > 0) {
                stockAlertBox.setVisible(true);
                stockAlertBox.setManaged(true);
                stockAlertLabel.setText(lowStockCount + " item" +
                        (lowStockCount > 1 ? "s are" : " is") + " running low on stock.");
            } else {
                stockAlertBox.setVisible(false);
                stockAlertBox.setManaged(false);
            }
        } catch (Exception ex) {
            System.err.println("Error loading stock alerts: " + ex.getMessage());
            stockAlertBox.setVisible(false);
            stockAlertBox.setManaged(false);
        }
    }

    private void applyChartStyles() {
        revenueChart.setStyle("-fx-background-color: transparent;");
        revenueChart.lookup(".chart-plot-background")
                .setStyle("-fx-background-color: transparent;");
    }


    private void setSalesTrend(double pct) {
        salesTrendLabel.setText(formatTrend(pct));
        salesTrendLabel.setStyle(trendStyle(pct));
    }

    private void setProfitTrend(double pct) {
        profitTrendLabel.setText(formatTrend(pct));
        profitTrendLabel.setStyle(trendStyle(pct));
    }

    private void setServicesTrend(double pct) {
        servicesTrendLabel.setText(formatTrend(pct));
        servicesTrendLabel.setStyle(trendStyle(pct));
    }

    private void setWorkersTrend(double pct) {
        if (pct == 0) {
            workersTrendLabel.setText("Stable ↑");
            workersTrendLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 12px; -fx-font-weight: bold;");
        } else {
            workersTrendLabel.setText(formatTrend(pct));
            workersTrendLabel.setStyle(trendStyle(pct));
        }
    }

    private String formatTrend(double pct) {
        return String.format("%+.1f%% %s", pct, pct >= 0 ? "↑" : "↓");
    }

    private String trendStyle(double pct) {
        String color = pct >= 0 ? "#22c55e" : "#ef4444";
        return "-fx-text-fill: " + color + "; -fx-font-size: 12px; -fx-font-weight: bold;";
    }


    private String formatAmount(double amount) {
        if (amount == 0) return "0";
        if (amount >= 1_000_000) return String.format("%.1fM", amount / 1_000_000);
        if (amount >= 1_000)     return String.format("%.1fK", amount / 1_000);
        return String.format("%.2f", amount);
    }

    public void refreshQuickActions() {
        loadQuickServicesSync();
    }

    private void refreshData() {
        LocalDate start = startDatePicker.getValue();
        LocalDate end   = endDatePicker.getValue();
        if (start != null && end != null && !start.isAfter(end)) {
            loadKpiData();
            loadQuickServiceStats();
            loadChartData(chartRangeCombo.getValue());
        }
    }

    private void loadQuickServiceStats() {
        if (quickServiceCountLabel == null || quickServiceRevenueLabel == null) {
            return;
        }
        LocalDate startDate = startDatePicker.getValue() != null ? startDatePicker.getValue() : LocalDate.now().minusDays(30);
        LocalDate endDate = endDatePicker.getValue() != null ? endDatePicker.getValue() : LocalDate.now();
        try (Connection conn = DatabaseManager.getConnection()) {
            int count = countRows(conn,
                    "SELECT COUNT(*) FROM quick_services WHERE service_date BETWEEN ? AND ?",
                    startDate, endDate);
            double revenue = sumAmount(conn,
                    "SELECT COALESCE(SUM(price),0) FROM quick_services WHERE service_date BETWEEN ? AND ?",
                    startDate, endDate);

            quickServiceCountLabel.setText(String.valueOf(count));
            quickServiceRevenueLabel.setText("Rs. " + formatAmount(revenue));
        } catch (SQLException ex) {
            System.err.println("Error loading quick service stats: " + ex.getMessage());
        }
    }

    @FXML
    private void handleViewInventory() {
        navigateTo("inventory");
    }

    @FXML
    private void handleNewSale() {
        showInfo("New Sale", "Opening New Sale form…");
    }

    @FXML
    private void handleAddService() {
        navigateTo("services");
    }

    @FXML
    private void handleLogWork() {
        showInfo("Log Work", "Opening Log Work form…");
    }

    @FXML
    private void handleNewExport() {
        showInfo("New Export", "Opening Export wizard…");
    }
    
    private void loadQuickServices() {
        try {
            quickServices.clear();
            String sql = "SELECT id, service, price, icon FROM quick_service_presets WHERE active = 1 ORDER BY service";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                
                while (rs.next()) {
                    QuickService qs = new QuickService(
                            rs.getString("id"),
                            rs.getString("service"),
                            rs.getDouble("price"),
                            rs.getString("icon")
                    );
                    quickServices.add(qs);
                }
            }
            populateQuickActionsGrid();
        } catch (Exception ex) {
            System.err.println("Error loading quick services: " + ex.getMessage());
            quickServices.clear();
            populateQuickActionsGrid();
        }
    }
    
    private void populateQuickActionsGrid() {
        if (quickActionsGrid == null) return;

        quickActionsGrid.getChildren().clear();

        if (quickServices.isEmpty()) {
            Label emptyState = new Label("No quick services yet");
            emptyState.setStyle("-fx-text-fill: #e5e7eb; -fx-font-size: 12px;");
            GridPane.setRowIndex(emptyState, 0);
            GridPane.setColumnIndex(emptyState, 0);
            GridPane.setColumnSpan(emptyState, 2);
            quickActionsGrid.getChildren().add(emptyState);
            return;
        }

        int row = 0;
        int col = 0;

        for (QuickService service : quickServices) {
            Button btn = createQuickActionButton(service);
            GridPane.setColumnIndex(btn, col);
            GridPane.setRowIndex(btn, row);
            quickActionsGrid.getChildren().add(btn);

            col++;
            if (col >= 2) {
                col = 0;
                row++;
            }
        }
    }
    
    private String faIconToEmoji(String iconLiteral) {
        if (iconLiteral == null) return "⚡";
        switch (iconLiteral) {
            case "fas-bolt": return "⚡";
            case "fas-wrench": return "🔧";
            case "fas-tools": return "🛠";
            case "fas-cog": case "fas-cogs": return "⚙";
            case "fas-oil-can": return "🛢";
            case "fas-tint": return "💧";
            case "fas-water": return "🌊";
            case "fas-wind": return "💨";
            case "fas-car": return "🚗";
            case "fas-truck": return "🚛";
            case "fas-fire": return "🔥";
            case "fas-fan": return "🌀";
            case "fas-broom": return "🧹";
            case "fas-shield-alt": return "🛡";
            case "fas-battery-full": return "🔋";
            case "fas-temperature-high": return "🌡";
            case "fas-charging-station": return "⚡";
            case "fas-filter": return "🔽";
            default: return "⚡";
        }
    }

    private Button createQuickActionButton(QuickService service) {
        Button btn = new Button();
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setMaxHeight(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color: #22303c; -fx-background-radius: 10; " +
                "-fx-text-fill: white; -fx-font-size: 12px; -fx-cursor: hand;");
        btn.setUserData("quick-service");
        
        VBox content = new VBox();
        content.setAlignment(Pos.CENTER);
        content.setSpacing(6.0);
        
        Label iconLabel = new Label(faIconToEmoji(service.icon));
        iconLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: #f59e0b;");
        
        Label name = new Label(service.name);
        name.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
        
        Label price = new Label("Rs. " + String.format("%.0f", service.price));
        price.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 11px; -fx-font-weight: bold;");
        
        content.getChildren().addAll(iconLabel, name, price);
        btn.setGraphic(content);
        
        btn.setOnAction(e -> handleQuickServiceAction(service));
        
        return btn;
    }
    
    private void handleQuickServiceAction(QuickService service) {
        // Log a quick service entry without a full invoice (no confirmation dialog)
        logQuickService(service);
        refreshData();
        // Update sidebar quick stats
        ViewModel.INSTANCE.getViewsFactory().updateQuickStats();
    }

    @FXML
    private void handleExpandQuickActions() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/gui/kline/view/form/quick-actions-popup.fxml"));
            javafx.scene.Parent root = loader.load();
            QuickActionsPopupController controller = loader.getController();

            // Convert inner QuickService list to popup's QuickService list
            List<QuickActionsPopupController.QuickService> popupServices = new ArrayList<>();
            for (QuickService qs : quickServices) {
                popupServices.add(new QuickActionsPopupController.QuickService(
                        qs.id, qs.name, qs.price, qs.icon));
            }
            controller.setServices(popupServices);
            controller.setOnActionLogged(this::refreshData);

            Stage stage = new Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initOwner(quickActionsGrid.getScene().getWindow());
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.setScene(scene);
            stage.show();
        } catch (Exception ex) {
            System.err.println("Error opening quick actions popup: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleQuickInvoice() {
        navigateTo("invoices");
    }

    @FXML
    private void handleQuickService() {
        navigateTo("services");
    }

    @FXML
    private void handleQuickCustomer() {
        navigateTo("workers");
    }

    @FXML
    private void handleQuickInventory() {
        navigateTo("inventory");
    }

    private void navigateTo(String page) {
        Pane container = getContentPane();
        if (container != null) {
            NavigationService.navigate(container, page);
            return;
        }

        ViewModel.INSTANCE.getViewsFactory().getView(page);
    }

    private Pane getContentPane() {
        if (periodSalesLabel == null || periodSalesLabel.getScene() == null) return null;
        Node root = periodSalesLabel.getScene().getRoot();
        Node target = root.lookup("#contentPane");
        if (target instanceof Pane pane) {
            return pane;
        }
        return null;
    }

    private Map<LocalDate, Double> loadRevenueTotals(LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Double> totals = new HashMap<>();
        try (Connection conn = DatabaseManager.getConnection()) {
            collectTotalsByDate(conn,
                    "SELECT COALESCE(invoice_date, DATE(created_at)) AS d, SUM(grand_total) AS total " +
                            "FROM invoices WHERE status = 'completed' AND COALESCE(invoice_date, DATE(created_at)) BETWEEN ? AND ? GROUP BY d",
                    startDate, endDate, totals);
            collectTotalsByDate(conn,
                    "SELECT COALESCE(sale_date, DATE(created_at)) AS d, SUM(COALESCE(subtotal, amount)) AS total " +
                            "FROM credit_sales WHERE COALESCE(sale_date, DATE(created_at)) BETWEEN ? AND ? GROUP BY d",
                    startDate, endDate, totals);
            collectTotalsByDate(conn,
                    "SELECT service_date, SUM(price) AS total FROM services WHERE service_date BETWEEN ? AND ? GROUP BY service_date",
                    startDate, endDate, totals);
            collectTotalsByDate(conn,
                    "SELECT service_date, SUM(price) AS total FROM quick_services WHERE service_date BETWEEN ? AND ? GROUP BY service_date",
                    startDate, endDate, totals);
            collectTotalsByDate(conn,
                    "SELECT export_date, SUM(total_amount) AS total FROM tyre_exports WHERE export_date BETWEEN ? AND ? GROUP BY export_date",
                    startDate, endDate, totals);
        } catch (SQLException ex) {
            System.err.println("Error loading revenue totals: " + ex.getMessage());
        }
        return totals;
    }

    private void collectTotalsByDate(Connection conn,
                                     String sql,
                                     LocalDate startDate,
                                     LocalDate endDate,
                                     Map<LocalDate, Double> totals) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(startDate));
            ps.setDate(2, java.sql.Date.valueOf(endDate));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.sql.Date date = rs.getDate(1);
                    double total = rs.getDouble(2);
                    if (date != null) {
                        totals.merge(date.toLocalDate(), total, Double::sum);
                    }
                }
            }
        }
    }

    private double sumRevenue(Connection conn, LocalDate startDate, LocalDate endDate) throws SQLException {
        double invoices = sumAmount(conn,
                "SELECT COALESCE(SUM(grand_total),0) FROM invoices " +
                        "WHERE status = 'completed' AND COALESCE(invoice_date, DATE(created_at)) BETWEEN ? AND ?",
                startDate, endDate);
        double creditSales = sumAmount(conn,
                "SELECT COALESCE(SUM(COALESCE(subtotal, amount)),0) FROM credit_sales " +
                        "WHERE COALESCE(sale_date, DATE(created_at)) BETWEEN ? AND ?",
                startDate, endDate);
        double services = sumAmount(conn,
                "SELECT COALESCE(SUM(price),0) FROM services WHERE service_date BETWEEN ? AND ?",
                startDate, endDate);
        double quickServicesTotal = sumAmount(conn,
                "SELECT COALESCE(SUM(price),0) FROM quick_services WHERE service_date BETWEEN ? AND ?",
                startDate, endDate);
        double tyreExports = sumAmount(conn,
                "SELECT COALESCE(SUM(total_amount),0) FROM tyre_exports WHERE export_date BETWEEN ? AND ?",
                startDate, endDate);
        return invoices + creditSales + services + quickServicesTotal + tyreExports;
    }

     private double sumProfit(Connection conn, LocalDate startDate, LocalDate endDate) throws SQLException {
         double invoiceProfit = sumAmount(conn,
                 "SELECT COALESCE(SUM(il.qty * (il.unit_price - COALESCE(p.buy_price,0))),0) " +
                         "FROM invoice_line_items il " +
                         "LEFT JOIN products p ON p.id = il.product_id " +
                         "JOIN invoices i ON i.id = il.invoice_ref " +
                         "WHERE i.status = 'completed' AND COALESCE(i.invoice_date, DATE(i.created_at)) BETWEEN ? AND ?",
                 startDate, endDate);
         double creditSalesProfit = sumAmount(conn,
                 "SELECT COALESCE(SUM(COALESCE(subtotal, amount)),0) FROM credit_sales " +
                         "WHERE COALESCE(sale_date, DATE(created_at)) BETWEEN ? AND ?",
                 startDate, endDate);
         double servicesProfit = sumAmount(conn,
                 "SELECT COALESCE(SUM(price),0) FROM services WHERE service_date BETWEEN ? AND ?",
                 startDate, endDate);
         double quickServicesProfit = sumAmount(conn,
                 "SELECT COALESCE(SUM(price),0) FROM quick_services WHERE service_date BETWEEN ? AND ?",
                 startDate, endDate);
         
         // Add tyre exports profit from sync_queue (calculated from JSON payload)
         double tyreExportsProfit = calculateTyreExportsProfit(startDate, endDate);
         double paidSalaries = sumAmount(conn,
                 "SELECT COALESCE(SUM(amount),0) FROM salary_payments WHERE DATE(paid_at) BETWEEN ? AND ?",
                 startDate, endDate);
         
         // Add expenses from the expenses table
         double totalExpenses = sumAmount(conn,
                 "SELECT COALESCE(SUM(amount),0) FROM expenses WHERE expense_date BETWEEN ? AND ?",
                 startDate, endDate);
         
         return invoiceProfit + creditSalesProfit + servicesProfit + quickServicesProfit + tyreExportsProfit - paidSalaries - totalExpenses;
     }

    private double calculateTyreExportsProfit(LocalDate startDate, LocalDate endDate) {
        TyreExportRepository repository = new TyreExportRepository();
        List<com.gui.kline.models.TyreExport> exports = repository.getAllExports();
        return exports.stream()
                .filter(e -> {
                    LocalDate date = e.getExportDate();
                    return date != null && !date.isBefore(startDate) && !date.isAfter(endDate);
                })
                .mapToDouble(e -> (e.getCustPrice() - e.getCompPrice()) * e.getTyres() + e.getServiceFee())
                .sum();
    }

     private int countServices(Connection conn, LocalDate startDate, LocalDate endDate) throws SQLException {
         int services = countRows(conn,
                 "SELECT COUNT(*) FROM services WHERE service_date BETWEEN ? AND ?",
                 startDate, endDate);
         int quick = countRows(conn,
                 "SELECT COUNT(*) FROM quick_services WHERE service_date BETWEEN ? AND ?",
                 startDate, endDate);
         int exports = countRows(conn,
                 "SELECT COUNT(*) FROM tyre_exports WHERE export_date BETWEEN ? AND ?",
                 startDate, endDate);
         return services + quick + exports;
     }

    private int countActiveWorkers(Connection conn, LocalDate startDate, LocalDate endDate) throws SQLException {
        int active = countRows(conn,
                "SELECT COUNT(DISTINCT worker_id) FROM worker_attendance WHERE attendance_date BETWEEN ? AND ? AND status = 'present'",
                startDate, endDate);
        if (active > 0) {
            return active;
        }
        return countRows(conn, "SELECT COUNT(*) FROM workers", null, null);
    }

    private double sumAmount(Connection conn, String sql, LocalDate startDate, LocalDate endDate) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (startDate != null && endDate != null) {
                ps.setDate(1, java.sql.Date.valueOf(startDate));
                ps.setDate(2, java.sql.Date.valueOf(endDate));
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        }
    }

    private int countRows(Connection conn, String sql, LocalDate startDate, LocalDate endDate) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (startDate != null && endDate != null) {
                ps.setDate(1, java.sql.Date.valueOf(startDate));
                ps.setDate(2, java.sql.Date.valueOf(endDate));
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private void logQuickService(QuickService service) {
        String insert = "INSERT INTO quick_services (id, service, price, service_date) VALUES (UUID(), ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(insert)) {
            LocalDate today = LocalDate.now();
            ps.setString(1, service.name);
            ps.setDouble(2, service.price);
            ps.setDate(3, java.sql.Date.valueOf(today));
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Error logging quick service: " + ex.getMessage());
            return;
        }

        String payload = JsonUtil.obj(
                JsonUtil.field("service", service.name),
                JsonUtil.field("price", service.price),
                JsonUtil.field("date", LocalDate.now().toString())
        );
        syncQueueRepository.enqueue("quick_service", payload);
    }

    private int getMaxRowIndex(GridPane grid) {
        int max = -1;
        for (Node node : grid.getChildren()) {
            Integer rowIndex = GridPane.getRowIndex(node);
            int row = rowIndex == null ? 0 : rowIndex;
            if (row > max) {
                max = row;
            }
        }
        return Math.max(max, 0);
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Inner classes for data modeling
    private static class KPIMetrics {
        double sales = 0;
        double profit = 0;
        int services = 0;
        int workers = 0;
    }
    
    private static class QuickService {
        String id;
        String name;
        double price;
        String icon;
        
        QuickService(String id, String name, double price, String icon) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.icon = icon;
        }
    }
}
