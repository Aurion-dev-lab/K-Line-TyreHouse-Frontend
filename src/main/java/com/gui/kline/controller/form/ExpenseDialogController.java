package com.gui.kline.controller.form;

import com.gui.kline.data.DatabaseManager;
import com.gui.kline.utils.JsonUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Window;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;

public class ExpenseDialogController {

    @FXML
    private Button btnCancel;

    @FXML
    private Button btnSave;

    @FXML
    private ComboBox<String> cmbCategory;

    @FXML
    private DatePicker dpDate;

    @FXML
    private TextField txtAmount;

    @FXML
    private TextField txtDescription;    private Runnable onSaved;

    @FXML
    public void initialize() {
        dpDate.setValue(LocalDate.now());
        // Set up category combo box items
        if (cmbCategory != null) {
            cmbCategory.getItems().addAll("Rent", "Utilities", "Salaries", "Supplies", "Equipment", "Maintenance", "Transport", "Other");
        }
    }

    @FXML
    void handleCancel(ActionEvent event) {
        closeDialog();
    }

    @FXML
    void handleSave(ActionEvent event) {
        LocalDate date = dpDate.getValue();
        String description = txtDescription.getText() != null ? txtDescription.getText().trim() : "";
        String category = cmbCategory.getValue() != null ? cmbCategory.getValue() : "Other";
        String amountStr = txtAmount.getText() != null ? txtAmount.getText().trim() : "0";

        if (date == null) {
            showError("Please select a date.");
            return;
        }

        if (description.isEmpty()) {
            showError("Please enter a description.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr.replace(",", ""));
        } catch (NumberFormatException ex) {
            showError("Please enter a valid amount.");
            return;
        }

        if (amount <= 0) {
            showError("Amount must be greater than zero.");
            return;
        }

        try {
            // Save to database
            String id = saveExpense(date, description, category, amount);

            // Enqueue for sync
            enqueueExpense(id, date, description, category, amount);

            if (onSaved != null) {
                onSaved.run();
            }
            closeDialog();
        } catch (Exception ex) {
            showError("Failed to save expense: " + ex.getMessage());
        }
    }

    private String saveExpense(LocalDate date, String description, String category, double amount) throws SQLException {
        String id = "EXP" + System.currentTimeMillis();

        String sql = "INSERT INTO expenses (id, expense_date, description, category, amount, created_at) VALUES (?, ?, ?, ?, ?, NOW())";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setDate(2, Date.valueOf(date));
            ps.setString(3, description);
            ps.setString(4, category);
            ps.setDouble(5, amount);
            ps.executeUpdate();
        }

        return id;
    }

    private void enqueueExpense(String id, LocalDate date, String description, String category, double amount) {
        String payload = JsonUtil.obj(
                JsonUtil.field("operation", "create"),
                JsonUtil.field("id", id),
                JsonUtil.field("expenseDate", date.toString()),
                JsonUtil.field("description", description),
                JsonUtil.field("category", category),
                JsonUtil.field("amount", amount)
        );    }

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    private void closeDialog() {
        if (btnCancel != null && btnCancel.getScene() != null) {
            btnCancel.getScene().getWindow().hide();
        }
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        Window owner = getOwnerWindow();
        if (owner != null) {
            alert.initOwner(owner);
            alert.initModality(javafx.stage.Modality.WINDOW_MODAL);
        }
        alert.showAndWait();
    }

    private Window getOwnerWindow() {
        if (btnCancel != null && btnCancel.getScene() != null) {
            return btnCancel.getScene().getWindow();
        }
        return null;
    }
}