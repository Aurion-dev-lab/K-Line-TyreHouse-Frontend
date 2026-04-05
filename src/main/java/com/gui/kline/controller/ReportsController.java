package com.gui.kline.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ReportsController implements Initializable {


    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Button     exportBtn;

    @FXML private Label totalSalesLabel;
    @FXML private Label grossProfitLabel;
    @FXML private Label workerCostsLabel;
    @FXML private Label netIncomeLabel;

    @FXML private VBox salesBreakdownContainer;
    @FXML private VBox serviceRevenueContainer;

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
            String assignedTo,   // null → "Unassigned"
            double fee
    ) {}


    private final List<SaleItem>    allSales    = new ArrayList<>();
    private final List<ServiceItem> allServices = new ArrayList<>();

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final NumberFormat      NF = NumberFormat.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        endDatePicker.setValue(LocalDate.now());
        startDatePicker.setValue(LocalDate.now().minusDays(7));

        startDatePicker.valueProperty().addListener((obs, o, n) -> refresh());
        endDatePicker.valueProperty()  .addListener((obs, o, n) -> refresh());
        loadSampleData();
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
        LocalDate to   = endDatePicker.getValue();
        if (from == null || to == null) return;

        List<SaleItem>    sales    = allSales   .stream().filter(s -> !s.date().isBefore(from) && !s.date().isAfter(to)).toList();
        List<ServiceItem> services = allServices.stream().filter(s -> !s.date().isBefore(from) && !s.date().isAfter(to)).toList();

        double totalSales   = sales.stream().mapToDouble(SaleItem::revenue).sum()
                + services.stream().mapToDouble(ServiceItem::fee).sum();
        double grossProfit  = sales.stream().mapToDouble(SaleItem::profit).sum();
        double workerCosts  = computeWorkerCosts(from, to);   // plug in your logic
        double netIncome    = grossProfit - workerCosts;

        totalSalesLabel  .setText("Rs. " + fmt(totalSales));
        grossProfitLabel .setText("Rs. " + fmt(grossProfit));
        workerCostsLabel .setText("Rs. " + fmt(workerCosts));
        netIncomeLabel   .setText("Rs. " + fmt(netIncome));

        buildSalesBreakdown(sales);
        buildServiceRevenue(services);
    }

    private void buildSalesBreakdown(List<SaleItem> sales) {
        salesBreakdownContainer.getChildren().clear();
        if (sales.isEmpty()) {
            salesBreakdownContainer.getChildren().add(emptyLabel("No sales in this period"));
            return;
        }
        for (SaleItem item : sales) {
            salesBreakdownContainer.getChildren().add(buildSaleRow(item));
        }
    }

    private void buildServiceRevenue(List<ServiceItem> services) {
        serviceRevenueContainer.getChildren().clear();
        if (services.isEmpty()) {
            serviceRevenueContainer.getChildren().add(emptyLabel("No services in this period"));
            return;
        }
        for (ServiceItem item : services) {
            serviceRevenueContainer.getChildren().add(buildServiceRow(item));
        }
    }

    private HBox buildSaleRow(SaleItem item) {
        Label name = new Label(item.name());
        name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label sub = new Label(item.date().format(DF) + " • " + item.qty() + " unit" + (item.qty() != 1 ? "s" : ""));
        sub.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");

        VBox left = new VBox(3, name, sub);
        HBox.setHgrow(left, Priority.ALWAYS);

        Label rev = new Label("Rs. " + fmt(item.revenue()));
        rev.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        rev.setAlignment(Pos.CENTER_RIGHT);

        Label profit = new Label("PROFIT: RS. " + fmt(item.profit()));
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

        Label fee = new Label("Rs. " + fmt(item.fee()));
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

    private Label emptyLabel(String msg) {
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 13px; -fx-padding: 20 0 0 0;");
        return lbl;
    }


    @FXML
    private void handleExportPDF() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "PDF export not yet implemented.\nPlug in your PDF library here.",
                ButtonType.OK);
        alert.setHeaderText("Export PDF");
        alert.showAndWait();
    }

    private double computeWorkerCosts(LocalDate from, LocalDate to) {
        return 4_700.0;
    }

    private static String fmt(double value) {
        NF.setMaximumFractionDigits(0);
        return NF.format(value);
    }


    private void loadSampleData() {
        LocalDate d = LocalDate.of(2026, 3, 29);

        allSales.add(new SaleItem("Engine Oil 5W-30 (4L)", d, 2, 21_000, 4_000));
        allSales.add(new SaleItem("Oil Filter - Toyota",   d, 1,  1_850,   650));

        allServices.add(new ServiceItem("Full Service",    d, null, 5_500));
        allServices.add(new ServiceItem("Wheel Alignment", d, null, 2_500));
    }
}