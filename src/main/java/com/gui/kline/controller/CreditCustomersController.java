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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.util.List;

public class CreditCustomersController {

    @FXML private TableView<CreditCustomer> tblCustomers;
    @FXML private TableColumn<CreditCustomer, String> colName;
    @FXML private TableColumn<CreditCustomer, String> colPhone;
    @FXML private TableColumn<CreditCustomer, String> colEmail;
    @FXML private TableColumn<CreditCustomer, String> colAddress;
    @FXML private TableColumn<CreditCustomer, String> colUpdatedAt;
    @FXML private TableColumn<CreditCustomer, String> colAmount;
    @FXML private TableColumn<CreditCustomer, String> colSettleAmount;
    @FXML private TableColumn<CreditCustomer, String> colDueAmount;
    @FXML private TextField txtSearch;

    private final LocalCatalogRepository catalogRepository = new LocalCatalogRepository();
    private final ObservableList<CreditCustomer> masterList = FXCollections.observableArrayList();
    private final FilteredList<CreditCustomer> filteredList = new FilteredList<>(masterList, p -> true);

    @FXML
    public void initialize() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        colUpdatedAt.setCellValueFactory(new PropertyValueFactory<>("updatedAt"));

        colAmount.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(String.format("Rs. %.2f", cellData.getValue().getTotalAmount())));
        colSettleAmount.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(String.format("Rs. %.2f", cellData.getValue().getSettleAmount())));
        colDueAmount.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(String.format("Rs. %.2f", cellData.getValue().getDueAmount())));

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
