package com.gui.kline.controller.form;

import com.gui.kline.data.DatabaseManager;
import com.gui.kline.utils.JsonUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;

public class RecordServiceDialogController {

    @FXML private TextField    txtRemark;
    @FXML private TextField   txtServiceName;
    @FXML private TextField   txtPrice;
    @FXML private DatePicker  datePicker;
    @FXML
    public void initialize() {
        datePicker.setValue(LocalDate.now());
    }

    @FXML
    private void onSave() {
        String remark  = txtRemark.getText().trim();
        String name    = txtServiceName.getText().trim();
        String priceText   = txtPrice.getText().trim();
        LocalDate date = datePicker.getValue();

        if (name.isEmpty()) {
            showError("Service name is required.");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceText);
            if (price < 0) {
                showError("Price must be zero or greater.");
                return;
            }
        } catch (NumberFormatException ex) {
            showError("Price must be a valid number.");
            return;
        }

        LocalDate serviceDate = date != null ? date : LocalDate.now();
        String insert = "INSERT INTO services (id, name, price, service_date, remark) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(insert)) {
            ps.setString(1, java.util.UUID.randomUUID().toString());
            ps.setString(2, name);
            ps.setDouble(3, price);
            ps.setString(4, serviceDate.toString());
            ps.setString(5, remark);
            ps.executeUpdate();
        } catch (SQLException ex) {
            showError("Failed to save service: " + ex.getMessage());
            return;
        }

        String payload = JsonUtil.obj(
                JsonUtil.field("service", name),
                JsonUtil.field("remark", remark),
                JsonUtil.field("price", price),
                JsonUtil.field("date", serviceDate.toString())
        );
        closeDialog();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Record Service");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void onCancel() {
        closeDialog();
    }

    private void closeDialog() {
        txtRemark.getScene().getWindow().hide();
    }
}