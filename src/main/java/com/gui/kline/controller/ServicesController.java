package com.gui.kline.controller;

import com.gui.kline.controller.form.RecordServiceDialogController;
import com.gui.kline.data.DatabaseManager;
import com.gui.kline.models.ServiceRecord;
import com.gui.kline.models.ViewModel;
import com.gui.kline.utils.JsonUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

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
    private TableColumn<ServiceRecord, String> colActions;

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
        
        // Set up actions column with delete button
        colActions.setCellFactory(col -> new javafx.scene.control.TableCell<ServiceRecord, String>() {
            private final Button deleteBtn = new Button();
            {
                deleteBtn.setGraphic(new FontIcon("fas-trash"));
                deleteBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                deleteBtn.setOnAction(e -> {
                    ServiceRecord record = getTableView().getItems().get(getIndex());
                    deleteService(record);
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(deleteBtn);
                }
            }
        });

        filteredServices = new FilteredList<>(services, item -> true);
        tblServices.setItems(filteredServices);

        dpFrom.setValue(LocalDate.now().minusDays(30));
        dpTo.setValue(LocalDate.now());

        loadServices();
        applyFilters();
        refreshTotals();
        populateCommonServices();

        // Register with ViewFactory for cross-controller refresh
        ViewModel.INSTANCE.getViewsFactory().setServicesController(this);
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

    public void refreshData() {
        loadServices();
        applyFilters();
        refreshTotals();
        populateCommonServices();

        // Register with ViewFactory for cross-controller refresh
        ViewModel.INSTANCE.getViewsFactory().setServicesController(this);
    }

    @FXML
    void handleRecordNewService(ActionEvent event) {
        Stage ownerStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        RecordServiceDialogController controller = ViewModel.INSTANCE.getViewsFactory()
                .getForm("form/record-service-dialog", ownerStage);

        Stage formStage = ViewModel.INSTANCE.getViewsFactory().getLastDialogStage();
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
                    "SELECT id, service_date, name, remark, price FROM services",
                    "services");
            loadQuickServiceRows(conn);
            loadInvoiceServiceRows(conn);
        } catch (SQLException ex) {
            System.err.println("Error loading services: " + ex.getMessage());
        }

        services.sort(Comparator.comparing(ServiceRecord::getDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
    }

    private void loadServiceRows(Connection conn, String sql, String sourceTable) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString(1);
                java.sql.Date date = rs.getDate(2);
                String name = rs.getString(3);
                String remark = rs.getString(4);
                double price = rs.getDouble(5);

                LocalDate serviceDate = date != null ? date.toLocalDate() : null;
                String finalRemark = remark != null ? remark : "";
                
                ServiceRecord record = new ServiceRecord(serviceDate, name, finalRemark, price);
                record.setId(id);
                record.setSourceTable(sourceTable);
                record.setIsQuickService("quick_services".equals(sourceTable));
                services.add(record);
            }
        }
    }

    private void loadQuickServiceRows(Connection conn) throws SQLException {
        String sql = "SELECT id, service_date, service, NULL AS remark, price FROM quick_services";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString(1);
                java.sql.Date date = rs.getDate(2);
                String name = rs.getString(3);
                String remark = rs.getString(4);
                double price = rs.getDouble(5);

                LocalDate serviceDate = date != null ? date.toLocalDate() : null;
                String finalRemark = remark != null ? remark : "Quick service";
                
                ServiceRecord record = new ServiceRecord(serviceDate, name, finalRemark, price);
                record.setId(id);
                record.setSourceTable("quick_services");
                record.setIsQuickService(true);
                services.add(record);
            }
        }
    }

    private void loadInvoiceServiceRows(Connection conn) throws SQLException {
        String sql = "SELECT i.id, COALESCE(i.invoice_date, DATE(i.created_at)) AS d, " +
                "il.description, COALESCE(il.total, il.qty * il.unit_price) AS total " +
                "FROM invoice_line_items il " +
                "JOIN invoices i ON i.id = il.invoice_ref " +
                "WHERE il.type = 'Service'";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString(1);
                java.sql.Date date = rs.getDate(2);
                String name = rs.getString(3);
                double price = rs.getDouble(4);
                LocalDate serviceDate = date != null ? date.toLocalDate() : null;
                
                ServiceRecord record = new ServiceRecord(serviceDate, name, "Invoiced service", price);
                record.setId(id);
                record.setSourceTable("invoice_line_items");
                record.setIsQuickService(false);
                services.add(record);
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

    private void deleteService(ServiceRecord record) {
        if (record == null) {
            return;
        }

        String id = record.getId();
        String sourceTable = record.getSourceTable();
        boolean isQuickService = record.getIsQuickService();

        // Get owner window to prevent alert from opening as separate window in full-screen mode
        javafx.stage.Window owner = null;
        if (lblTotalServices != null && lblTotalServices.getScene() != null) {
            owner = lblTotalServices.getScene().getWindow();
        }

        // Confirm deletion
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Service");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to delete this service?");
        if (owner != null) {
            alert.initOwner(owner);
            alert.initModality(javafx.stage.Modality.WINDOW_MODAL);
        }

        if (alert.showAndWait().orElse(javafx.scene.control.ButtonType.CANCEL) 
                != javafx.scene.control.ButtonType.OK) {
            return;
        }

        // Delete from appropriate table
        try (Connection conn = DatabaseManager.getConnection()) {
            if ("quick_services".equals(sourceTable)) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM quick_services WHERE id = ?")) {
                    ps.setString(1, id);
                    ps.executeUpdate();
                    DatabaseManager.logDeletion("quick_services", id);
                }
            } else if ("services".equals(sourceTable)) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM services WHERE id = ?")) {
                    ps.setString(1, id);
                    ps.executeUpdate();
                    DatabaseManager.logDeletion("services", id);
                }
            } else if ("invoice_line_items".equals(sourceTable)) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM invoice_line_items WHERE id = ?")) {
                    ps.setString(1, id);
                    ps.executeUpdate();
                    DatabaseManager.logDeletion("invoice_line_items", id);
                }
            }
        } catch (SQLException ex) {
            System.err.println("Error deleting service: " + ex.getMessage());
            return;
        }

        // Remove from list
        services.remove(record);
        applyFilters();
        refreshTotals();
        populateCommonServices();

        // Update sidebar quick stats if it was a quick service
        if (isQuickService) {
            ViewModel.INSTANCE.getViewsFactory().updateQuickStats();
        }
    }
}

