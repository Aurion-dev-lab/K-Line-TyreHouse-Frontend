package com.gui.kline.controller;

import com.gui.kline.data.DatabaseManager;
import com.gui.kline.models.ServiceRecord;
import com.gui.kline.models.ViewModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ServicesController implements Initializable {

    @FXML
    private Button btnRecordService;

    @FXML
    private TableColumn<ServiceRecord, String> colDate;

    @FXML
    private TableColumn<ServiceRecord, String> colPrice;

    @FXML
    private TableColumn<ServiceRecord, String> colRemark;

    @FXML
    private TableColumn<ServiceRecord, String> colService;

    @FXML
    private DatePicker dpFrom;

    @FXML
    private DatePicker dpTo;

    @FXML
    private FlowPane flowCommonServices;

    @FXML
    private Label lblTotalRevenue;

    @FXML
    private Label lblTotalProfit;

    @FXML
    private Label lblTotalServices;

    @FXML
    private TableView<ServiceRecord> tblServices;

    @FXML
    private TextField txtFilter;

    private final ObservableList<ServiceRecord> services = FXCollections.observableArrayList();
    private FilteredList<ServiceRecord> filteredServices;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        colDate.setCellValueFactory(data -> data.getValue().dateLabelProperty());
        colService.setCellValueFactory(data -> data.getValue().serviceProperty());
        colRemark.setCellValueFactory(data -> data.getValue().remarkProperty());
        colPrice.setCellValueFactory(data -> data.getValue().priceLabelProperty());

        filteredServices = new FilteredList<>(services, item -> true);
        tblServices.setItems(filteredServices);

        dpFrom.setValue(LocalDate.now().minusDays(30));
        dpTo.setValue(LocalDate.now());

        loadServices();
        applyFilters();
        refreshTotals();
        populateCommonServices();
    }

    @FXML
    void handleDateFilter(ActionEvent event) {
        applyFilters();
        refreshTotals();
    }

    @FXML
    void handleFilter(KeyEvent event) {
        applyFilters();
        refreshTotals();
    }

    @FXML
    void handleRecordNewService(ActionEvent event) {
        Stage ownerStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Stage formStage = ViewModel.INSTANCE.getViewsFactory().getForm("form/record-service-dialog", ownerStage);
        if (formStage != null) {
            formStage.setOnHidden(e -> {
                loadServices();
                applyFilters();
                refreshTotals();
                populateCommonServices();
            });
        }
    }

    private void loadServices() {
        services.clear();
        try (Connection conn = DatabaseManager.getConnection()) {
            loadServiceRows(conn,
                    "SELECT service_date, name, remark, price FROM services",
                    false);
            loadServiceRows(conn,
                    "SELECT service_date, service, NULL AS remark, price FROM quick_services",
                    true);
            loadInvoiceServiceRows(conn);
        } catch (SQLException ex) {
            System.err.println("Error loading services: " + ex.getMessage());
        }

        services.sort(Comparator.comparing(ServiceRecord::getDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
    }

    private void loadServiceRows(Connection conn, String sql, boolean markQuick) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                java.sql.Date date = rs.getDate(1);
                String name = rs.getString(2);
                String remark = rs.getString(3);
                double price = rs.getDouble(4);

                LocalDate serviceDate = date != null ? date.toLocalDate() : null;
                String finalRemark = remark != null ? remark : "";
                if (markQuick) {
                    finalRemark = finalRemark.isEmpty() ? "Quick service" : finalRemark + " (Quick)";
                }
                services.add(new ServiceRecord(serviceDate, name, finalRemark, price));
            }
        }
    }

    private void loadInvoiceServiceRows(Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(i.invoice_date, DATE(i.created_at)) AS d, " +
                "il.description, COALESCE(il.total, il.qty * il.unit_price) AS total " +
                "FROM invoice_line_items il " +
                "JOIN invoices i ON i.id = il.invoice_ref " +
                "WHERE il.type = 'Service'";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                java.sql.Date date = rs.getDate(1);
                String name = rs.getString(2);
                double price = rs.getDouble(3);
                LocalDate serviceDate = date != null ? date.toLocalDate() : null;
                services.add(new ServiceRecord(serviceDate, name, "Invoiced service", price));
            }
        }
    }

    private void applyFilters() {
        String query = txtFilter.getText() == null ? "" : txtFilter.getText().trim().toLowerCase();
        LocalDate from = dpFrom.getValue();
        LocalDate to = dpTo.getValue();

        filteredServices.setPredicate(record -> {
            if (record == null) {
                return false;
            }

            boolean matchesQuery = query.isEmpty()
                    || record.getService().toLowerCase().contains(query)
                    || record.getRemark().toLowerCase().contains(query);

            LocalDate date = record.getDate();
            boolean matchesDate = true;
            if (from != null && date != null && date.isBefore(from)) {
                matchesDate = false;
            }
            if (to != null && date != null && date.isAfter(to)) {
                matchesDate = false;
            }

            return matchesQuery && matchesDate;
        });
    }

    private void refreshTotals() {
        int count = filteredServices.size();
        double revenue = filteredServices.stream()
                .collect(Collectors.summingDouble(ServiceRecord::getPrice));

        lblTotalServices.setText(String.valueOf(count));
        lblTotalRevenue.setText("Rs. " + String.format("%.2f", revenue));
        lblTotalProfit.setText("Rs. " + String.format("%.2f", revenue));
    }

    private void populateCommonServices() {
        flowCommonServices.getChildren().clear();

        Map<String, Long> counts = services.stream()
                .collect(Collectors.groupingBy(ServiceRecord::getService, HashMap::new, Collectors.counting()));

        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8)
                .forEach(entry -> {
                    Label label = new Label(entry.getKey());
                    label.getStyleClass().add("service-tag");
                    flowCommonServices.getChildren().add(label);
                });
    }
}
