package com.gui.kline.controller;

import com.gui.kline.data.DatabaseManager;
import com.gui.kline.models.QuickServicePreset;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class QuickServicePresetsController {

    @FXML private TableView<QuickServicePreset> tblPresets;
    @FXML private TableColumn<QuickServicePreset, String> colService;
    @FXML private TableColumn<QuickServicePreset, String> colPrice;
    @FXML private TableColumn<QuickServicePreset, String> colStatus;
    @FXML private TextField txtService;
    @FXML private TextField txtPrice;

    private final ObservableList<QuickServicePreset> presets = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colService.setCellValueFactory(data -> data.getValue().serviceProperty());
        colPrice.setCellValueFactory(data -> data.getValue().priceLabelProperty());
        colStatus.setCellValueFactory(data -> data.getValue().statusProperty());
        tblPresets.setItems(presets);
        loadPresets();
    }

    @FXML
    private void handleAddPreset() {
        String service = txtService.getText() == null ? "" : txtService.getText().trim();
        String priceText = txtPrice.getText() == null ? "" : txtPrice.getText().trim();

        if (service.isEmpty()) {
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

        String sql = "INSERT INTO quick_service_presets (id, service, price, active, created_at) VALUES (?, ?, ?, 1, NOW())";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, service);
            ps.setDouble(3, price);
            ps.executeUpdate();
        } catch (SQLException ex) {
            showError("Failed to add preset: " + ex.getMessage());
            return;
        }

        txtService.clear();
        txtPrice.clear();
        loadPresets();
    }

    @FXML
    private void handleTogglePreset() {
        QuickServicePreset selected = tblPresets.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select a preset to enable or disable.");
            return;
        }

        String sql = "UPDATE quick_service_presets SET active = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, selected.isActive() ? 0 : 1);
            ps.setString(2, selected.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            showError("Failed to update preset: " + ex.getMessage());
            return;
        }

        loadPresets();
    }

    @FXML
    private void handleEditPreset() {
        QuickServicePreset selected = requireSelection();
        if (selected == null) {
            return;
        }

        String service = txtService.getText() == null ? "" : txtService.getText().trim();
        String priceText = txtPrice.getText() == null ? "" : txtPrice.getText().trim();

        if (service.isEmpty() && priceText.isEmpty()) {
            txtService.setText(selected.getService());
            txtPrice.setText(String.format("%.0f", selected.getPrice()));
            return;
        }

        if (service.isEmpty()) {
            showError("Service name is required.");
            return;
        }

        double price = parsePrice(priceText);
        if (price < 0) {
            return;
        }

        String sql = "UPDATE quick_service_presets SET service = ?, price = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, service);
            ps.setDouble(2, price);
            ps.setString(3, selected.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            showError("Failed to update preset: " + ex.getMessage());
            return;
        }

        txtService.clear();
        txtPrice.clear();
        loadPresets();
    }

    @FXML
    private void handleDeletePreset() {
        QuickServicePreset selected = requireSelection();
        if (selected == null) {
            return;
        }

        if (!confirm("Delete\n\nRemove \"" + selected.getService() + "\" preset?")) {
            return;
        }

        String sql = "DELETE FROM quick_service_presets WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, selected.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            showError("Failed to delete preset: " + ex.getMessage());
            return;
        }

        loadPresets();
    }

    private QuickServicePreset requireSelection() {
        QuickServicePreset selected = tblPresets.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select a preset first.");
            return null;
        }
        return selected;
    }

    private double parsePrice(String priceText) {
        try {
            double price = Double.parseDouble(priceText);
            if (price < 0) {
                showError("Price must be zero or greater.");
                return -1;
            }
            return price;
        } catch (NumberFormatException ex) {
            showError("Price must be a valid number.");
            return -1;
        }
    }

    private boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Quick Service Presets");
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().filter(btn -> btn == ButtonType.OK).isPresent();
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) tblPresets.getScene().getWindow();
        stage.close();
    }

    private void loadPresets() {
        presets.clear();
        String sql = "SELECT id, service, price, active FROM quick_service_presets ORDER BY service";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                presets.add(new QuickServicePreset(
                        rs.getString("id"),
                        rs.getString("service"),
                        rs.getDouble("price"),
                        rs.getInt("active") == 1
                ));
            }
        } catch (SQLException ex) {
            showError("Failed to load presets: " + ex.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Quick Service Presets");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
