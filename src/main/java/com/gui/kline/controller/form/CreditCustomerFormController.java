package com.gui.kline.controller.form;

import com.gui.kline.data.LocalCatalogRepository;
import com.gui.kline.models.CreditCustomer;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class CreditCustomerFormController {

    @FXML private Label lblTitle;
    @FXML private Label lblBadge;
    @FXML private TextField txtName;
    @FXML private TextField txtPhone;
    @FXML private TextField txtEmail;
    @FXML private TextField txtAddress;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;

    private final LocalCatalogRepository catalogRepository = new LocalCatalogRepository();
    private CreditCustomer editingCustomer = null;
    private Runnable onSaved;

    @FXML
    public void initialize() {
        // Initialization if needed
    }

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    public void setEditMode(CreditCustomer customer) {
        this.editingCustomer = customer;
        if (lblTitle != null) {
            lblTitle.setText("Edit Credit Customer");
        }
        if (lblBadge != null) {
            lblBadge.setVisible(true);
            lblBadge.setManaged(true);
        }
        if (btnSave != null) {
            btnSave.setText("Update Customer");
        }

        if (customer != null) {
            txtName.setText(customer.getName());
            txtPhone.setText(customer.getPhone());
            txtEmail.setText(customer.getEmail() == null ? "" : customer.getEmail());
            txtAddress.setText(customer.getAddress() == null ? "" : customer.getAddress());
        }
    }

    @FXML
    private void handleSave() {
        String name = txtName.getText().trim();
        String phone = txtPhone.getText().trim();
        String email = txtEmail.getText().trim();
        String address = txtAddress.getText().trim();

        if (name.isEmpty()) {
            showAlert("Validation Error", "Customer name is required.");
            return;
        }
        if (name.length() > 100) {
            showAlert("Validation Error", "Customer name cannot exceed 100 characters.");
            return;
        }

        if (phone.isEmpty()) {
            showAlert("Validation Error", "Phone number is required.");
            return;
        }
        // Match digits, spaces, hyphens, and optional leading plus sign
        if (!phone.matches("^\\+?[0-9\\s\\-]{9,15}$")) {
            showAlert("Validation Error", "Please enter a valid phone number (9 to 15 digits).");
            return;
        }

        if (!email.isEmpty() && !email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
            showAlert("Validation Error", "Please enter a valid email address.");
            return;
        }

        if (!address.isEmpty() && address.length() > 250) {
            showAlert("Validation Error", "Physical address cannot exceed 250 characters.");
            return;
        }

        try {
            String id = editingCustomer != null ? editingCustomer.getId() : null;
            CreditCustomer c = new CreditCustomer(
                    id,
                    name,
                    phone,
                    email.isEmpty() ? null : email,
                    address.isEmpty() ? null : address,
                    editingCustomer != null ? editingCustomer.getCreatedAt() : null,
                    null
            );

            catalogRepository.saveCreditCustomer(c);

            if (onSaved != null) {
                onSaved.run();
            }
            close();
        } catch (Exception ex) {
            showAlert("Database Error", "Failed to save credit customer: " + ex.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private void close() {
        if (btnCancel != null && btnCancel.getScene() != null) {
            Stage stage = (Stage) btnCancel.getScene().getWindow();
            stage.close();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING, content, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        if (btnCancel != null && btnCancel.getScene() != null) {
            alert.initOwner(btnCancel.getScene().getWindow());
        }
        alert.showAndWait();
    }
}
