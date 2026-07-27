package com.gui.kline.controller;

import com.gui.kline.controller.form.CreditCustomerFormController;
import com.gui.kline.data.LocalCatalogRepository;
import com.gui.kline.models.CreditCustomer;
import com.gui.kline.models.ViewModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

public class CreditCustomersController {

    @FXML private TableView<CreditCustomer> tblCustomers;
    @FXML private TableColumn<CreditCustomer, String> colName;
    @FXML private TableColumn<CreditCustomer, String> colPhone;
    @FXML private TableColumn<CreditCustomer, String> colEmail;
    @FXML private TableColumn<CreditCustomer, String> colAddress;
    @FXML private TableColumn<CreditCustomer, String> colUpdatedAt;

    @FXML private FlowPane flowCards;
    @FXML private Label lblTotalCredit;
    @FXML private Label lblTotalSettled;
    @FXML private Label lblTotalDue;

    @FXML private TextField txtSearch;

    private final LocalCatalogRepository catalogRepository = new LocalCatalogRepository();
    private final ObservableList<CreditCustomer> masterList = FXCollections.observableArrayList();
    private final FilteredList<CreditCustomer> filteredList = new FilteredList<>(masterList, p -> true);

    @FXML
    public void initialize() {
        // Tab 1 bindings
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        colUpdatedAt.setCellValueFactory(new PropertyValueFactory<>("updatedAt"));

        tblCustomers.setItems(filteredList);

        loadData();
        ViewModel.INSTANCE.getViewsFactory().setCreditCustomersController(this);
    }

    public void loadData() {
        List<CreditCustomer> list = catalogRepository.loadCreditCustomers();
        for (CreditCustomer customer : list) {
            catalogRepository.loadCustomerStats(customer);
        }
        masterList.setAll(list);
        buildOverviewCards(list);
    }

    private void buildOverviewCards(List<CreditCustomer> list) {
        if (flowCards == null) return;
        flowCards.getChildren().clear();

        double totalCredit = 0.0;
        double totalSettled = 0.0;
        double totalDue = 0.0;

        for (CreditCustomer c : list) {
            totalCredit += c.getTotalAmount();
            totalSettled += c.getSettleAmount();
            totalDue += c.getDueAmount();

            // Create Card Container
            VBox card = new VBox();
            card.getStyleClass().add("customer-credit-card");

            // Customer Name Title
            Label nameLbl = new Label(c.getName() != null ? c.getName() : "Unknown");
            nameLbl.getStyleClass().add("customer-card-title");
            card.getChildren().add(nameLbl);

            // Total Credit Row
            HBox creditRow = new HBox();
            creditRow.getStyleClass().add("customer-card-row");
            Label creditLabel = new Label("Total Credit Extended: ");
            creditLabel.getStyleClass().add("customer-card-label");
            Label creditVal = new Label(String.format("Rs. %,.2f", c.getTotalAmount()));
            creditVal.getStyleClass().add("customer-card-value");
            creditRow.getChildren().addAll(creditLabel, creditVal);
            card.getChildren().add(creditRow);

            // Settled Row
            HBox settledRow = new HBox();
            settledRow.getStyleClass().add("customer-card-row");
            Label settledLabel = new Label("Amount Settled: ");
            settledLabel.getStyleClass().add("customer-card-label");
            Label settledVal = new Label(String.format("Rs. %,.2f", c.getSettleAmount()));
            settledVal.getStyleClass().add("customer-card-value");
            settledVal.getStyleClass().add("customer-card-settled-value");
            settledRow.getChildren().addAll(settledLabel, settledVal);
            card.getChildren().add(settledRow);

            // Due Row (Highlighted Box)
            VBox dueBox = new VBox();
            dueBox.getStyleClass().add("customer-card-due-box");
            Label dueLabel = new Label("OUTSTANDING DUE");
            dueLabel.getStyleClass().add("customer-card-due-label");
            Label dueVal = new Label(String.format("Rs. %,.2f", c.getDueAmount()));
            dueVal.getStyleClass().add("customer-card-due-value");
            dueBox.getChildren().addAll(dueLabel, dueVal);
            card.getChildren().add(dueBox);

            flowCards.getChildren().add(card);
        }

        // Set Top KPI Card Labels
        lblTotalCredit.setText(String.format("Rs. %,.2f", totalCredit));
        lblTotalSettled.setText(String.format("Rs. %,.2f", totalSettled));
        lblTotalDue.setText(String.format("Rs. %,.2f", totalDue));
    }

    @FXML
    private void handleSearch() {
        String query = txtSearch.getText().toLowerCase().trim();
        filteredList.setPredicate(customer -> {
            if (query.isEmpty()) {
                return true;
            }
            if (customer.getName() != null && customer.getName().toLowerCase().contains(query)) {
                return true;
            }
            if (customer.getPhone() != null && customer.getPhone().toLowerCase().contains(query)) {
                return true;
            }
            if (customer.getEmail() != null && customer.getEmail().toLowerCase().contains(query)) {
                return true;
            }
            if (customer.getAddress() != null && customer.getAddress().toLowerCase().contains(query)) {
                return true;
            }
            return false;
        });
    }

    @FXML
    private void handleAddCustomer() {
        Stage owner = (Stage) tblCustomers.getScene().getWindow();
        CreditCustomerFormController form = ViewModel.INSTANCE.getViewsFactory()
                .getForm("form/credit-customer-form", owner);
        if (form != null) {
            form.setOnSaved(this::loadData);
        }
    }

    @FXML
    private void handleEdit() {
        CreditCustomer selected = tblCustomers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a customer to edit.");
            return;
        }

        Stage owner = (Stage) tblCustomers.getScene().getWindow();
        CreditCustomerFormController form = ViewModel.INSTANCE.getViewsFactory()
                .getForm("form/credit-customer-form", owner);
        if (form != null) {
            form.setEditMode(selected);
            form.setOnSaved(this::loadData);
        }
    }

    @FXML
    private void handleDelete() {
        CreditCustomer selected = tblCustomers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a customer to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, 
                "Are you sure you want to delete credit customer '" + selected.getName() + "'?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);
        confirm.initOwner(tblCustomers.getScene().getWindow());

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    catalogRepository.deleteCreditCustomer(selected.getId());
                    loadData();
                } catch (Exception ex) {
                    showAlert("Error", "Failed to delete customer: " + ex.getMessage());
                }
            }
        });
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING, content, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.initOwner(tblCustomers.getScene().getWindow());
        alert.showAndWait();
    }
}
