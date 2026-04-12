package com.gui.kline.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

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

    @FXML private Button newSaleBtn;
    @FXML private Button addServiceBtn;
    @FXML private Button logWorkBtn;
    @FXML private Button newExportBtn;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupDatePickers();
        setupChartRangeCombo();
        loadKpiData();
        loadChartData("Last 7 Days");
        loadStockAlerts();
        applyChartStyles();
    }

    private void setupDatePickers() {
        startDatePicker.setValue(LocalDate.of(2026, 3, 30));
        endDatePicker.setValue(LocalDate.of(2026, 4, 6));

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
        double sales    = 0;
        double profit   = 0;
        int    services = 0;
        int    workers  = 0;

        periodSalesLabel.setText("Rs. " + formatAmount(sales));
        periodProfitLabel.setText("Rs. " + formatAmount(profit));
        periodServicesLabel.setText(String.valueOf(services));
        activeWorkersLabel.setText(String.valueOf(workers));

        setSalesTrend(+12.5);
        setProfitTrend(+8.2);
        setServicesTrend(-2.4);
        setWorkersTrend(0);            // 0 → "Stable"
    }

    private void loadChartData(String range) {
        revenueChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenue");

        switch (range) {
            case "Last 7 Days" -> {
                String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
                double[] values = {4000, 3200, 2100, 2700, 2000, 2600, 3500};
                for (int i = 0; i < days.length; i++) {
                    series.getData().add(new XYChart.Data<>(days[i], values[i]));
                }
            }
            case "Last 30 Days" -> {
                for (int d = 1; d <= 30; d++) {
                    series.getData().add(new XYChart.Data<>(String.valueOf(d),
                            2000 + Math.random() * 3000));
                }
            }
            case "Last 3 Months" -> {
                String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                for (int m = 0; m < 3; m++) {
                    series.getData().add(new XYChart.Data<>(months[m],
                            15000 + Math.random() * 20000));
                }
            }
            case "Last Year" -> {
                String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                for (String month : months) {
                    series.getData().add(new XYChart.Data<>(month,
                            40000 + Math.random() * 60000));
                }
            }
        }

        revenueChart.getData().add(series);

        series.getNode().setStyle(
                "-fx-stroke: #22c55e; -fx-stroke-width: 2.5px;"
        );
    }

    private void loadStockAlerts() {
        int lowStockCount = 1;
        if (lowStockCount > 0) {
            stockAlertBox.setVisible(true);
            stockAlertBox.setManaged(true);
            stockAlertLabel.setText(lowStockCount + " item" +
                    (lowStockCount > 1 ? "s are" : " is") + " running low on stock.");
        } else {
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

    private void refreshData() {
        LocalDate start = startDatePicker.getValue();
        LocalDate end   = endDatePicker.getValue();
        if (start != null && end != null && !start.isAfter(end)) {
            loadKpiData();
            loadChartData(chartRangeCombo.getValue());
        }
    }


    @FXML
    private void handleViewInventory() {
        showInfo("Inventory", "Opening inventory view…");
    }

    @FXML
    private void handleNewSale() {
        showInfo("New Sale", "Opening New Sale form…");
    }

    @FXML
    private void handleAddService() {
        showInfo("Add Service", "Opening Add Service form…");
    }

    @FXML
    private void handleLogWork() {
        showInfo("Log Work", "Opening Log Work form…");
    }

    @FXML
    private void handleNewExport() {
        showInfo("New Export", "Opening Export wizard…");
    }


    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}