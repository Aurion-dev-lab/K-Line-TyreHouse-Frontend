package com.gui.kline.controller;

import com.gui.kline.data.DatabaseManager;
import com.gui.kline.models.QuickServicePreset;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class QuickServicePresetsController {

    @FXML private TableView<QuickServicePreset> tblPresets;
    @FXML private TableColumn<QuickServicePreset, String> colIcon;
    @FXML private TableColumn<QuickServicePreset, String> colService;
    @FXML private TableColumn<QuickServicePreset, String> colPrice;
    @FXML private TableColumn<QuickServicePreset, String> colStatus;
    @FXML private TextField txtService;
    @FXML private TextField txtPrice;
    @FXML private ComboBox<String> cmbIcon;
    @FXML private Button btnSave;
    @FXML private Label lblMessage;

    private final ObservableList<QuickServicePreset> presets = FXCollections.observableArrayList();

    // Curated list of service-relevant FontAwesome icons
    private final String[] ICON_CHOICES = {
            "fas-bolt",       // lightning / quick
            "fas-wrench",     // general repair
            "fas-tools",      // tools
            "fas-cog",        // settings
            "fas-cogs",       // multiple cogs
            "fas-oil-can",    // oil
            "fas-tint",       // coolant / fluid
            "fas-water",      // water / wash
            "fas-wind",       // air / tyre
            "fas-car",        // car
            "fas-truck",      // truck
            "fas-fire",       // exhaust
            "fas-fan",        // AC / cooling
            "fas-broom",      // cleaning
            "fas-shield-alt", // brake / safety
            "fas-battery-full", // battery
            "fas-temperature-high", // engine temp
            "fas-charging-station", // charging
            "fas-filter"      // oil filter
    };

    @FXML
    public void initialize() {
        // Set up icon column with visual preview
        colIcon.setCellValueFactory(data -> data.getValue().iconProperty());
        colIcon.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String iconLiteral, boolean empty) {
                super.updateItem(iconLiteral, empty);
                if (empty || iconLiteral == null) {
                    setGraphic(null);
                } else {
                    FontIcon icon = new FontIcon(iconLiteral);
                    icon.setIconSize(18);
                    setGraphic(icon);
                }
            }
        });

        colService.setCellValueFactory(data -> data.getValue().serviceProperty());
        colPrice.setCellValueFactory(data -> data.getValue().priceLabelProperty());
        colStatus.setCellValueFactory(data -> data.getValue().statusProperty());
        tblPresets.setItems(presets);

        // Populate icon combo box with visual previews
        cmbIcon.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    FontIcon icon = new FontIcon(item);
                    icon.setIconSize(16);
                    HBox box = new HBox(8, icon, new Label(formatIconName(item)));
                    box.setPadding(new Insets(2, 0, 2, 0));
                    setGraphic(box);
                }
            }
        });
        cmbIcon.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    FontIcon icon = new FontIcon(item);
                    icon.setIconSize(16);
                    HBox box = new HBox(8, icon, new Label(formatIconName(item)));
                    setGraphic(box);
                }
            }
        });
        cmbIcon.setItems(FXCollections.observableArrayList(ICON_CHOICES));
        cmbIcon.getSelectionModel().select(0); // default to first

        loadPresets();
    }

    private String formatIconName(String iconLiteral) {
        if (iconLiteral == null) return "";
        String name = iconLiteral.replace("fas-", "").replace("far-", "").replace("fal-", "");
        name = name.replace("-", " ");
        if (name.length() > 0) {
            name = name.substring(0, 1).toUpperCase() + name.substring(1);
        }
        return name;
    }

    /** Show an inline message. For success (isError=false) it auto-hides after 2.5s. */
    private void showMessage(String text, boolean isError) {
        // Cancel any pending auto-hide
        if (pendingHide != null) {
            pendingHide.stop();
        }
        lblMessage.setText(text);
        lblMessage.setStyle(isError
                ? "-fx-text-fill: #dc2626; -fx-font-size: 13px; -fx-font-weight: bold;"
                : "-fx-text-fill: #059669; -fx-font-size: 13px; -fx-font-weight: bold;");
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);

        if (!isError) {
            pendingHide = new PauseTransition(Duration.seconds(2.5));
            pendingHide.setOnFinished(e -> {
                lblMessage.setVisible(false);
                lblMessage.setManaged(false);
            });
            pendingHide.play();
        }
    }

    private PauseTransition pendingHide;

    @FXML
    private void handleAddPreset() {
        String service = txtService.getText() == null ? "" : txtService.getText().trim();
        String priceText = txtPrice.getText() == null ? "" : txtPrice.getText().trim();
        String icon = cmbIcon.getValue();

        if (service.isEmpty()) {
            showMessage("Service name is required.", true);
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceText);
            if (price < 0) {
                showMessage("Price must be zero or greater.", true);
                return;
            }
        } catch (NumberFormatException ex) {
            showMessage("Price must be a valid number.", true);
            return;
        }

        String sql = "INSERT INTO quick_service_presets (id, service, price, active, icon, created_at) VALUES (?, ?, ?, 1, ?, NOW())";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, service);
            ps.setDouble(3, price);
            ps.setString(4, icon != null ? icon : "fas-bolt");
            ps.executeUpdate();
        } catch (SQLException ex) {
            showMessage("Failed to add preset: " + ex.getMessage(), true);
            return;
        }

        txtService.clear();
        txtPrice.clear();
        loadPresets();
        showMessage("✓ Service added successfully", false);
    }

    @FXML
    private void handleTogglePreset() {
        QuickServicePreset selected = tblPresets.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("Select a preset to enable or disable.", true);
            return;
        }

        String sql = "UPDATE quick_service_presets SET active = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, selected.isActive() ? 0 : 1);
            ps.setString(2, selected.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            showMessage("Failed to update preset: " + ex.getMessage(), true);
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

        // Populate fields with the selected item's data
        txtService.setText(selected.getService());
        txtPrice.setText(String.format("%.0f", selected.getPrice()));
        // Select the current icon in combo
        for (int i = 0; i < ICON_CHOICES.length; i++) {
            if (ICON_CHOICES[i].equals(selected.getIcon())) {
                cmbIcon.getSelectionModel().select(i);
                break;
            }
        }

        // Show the Save button
        btnSave.setVisible(true);
        btnSave.setManaged(true);
    }

    @FXML
    private void handleSavePreset() {
        QuickServicePreset selected = tblPresets.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("Select a preset first.", true);
            return;
        }

        String service = txtService.getText() == null ? "" : txtService.getText().trim();
        String priceText = txtPrice.getText() == null ? "" : txtPrice.getText().trim();
        String icon = cmbIcon.getValue();

        if (service.isEmpty()) {
            showMessage("Service name is required.", true);
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceText);
            if (price < 0) {
                showMessage("Price must be zero or greater.", true);
                return;
            }
        } catch (NumberFormatException ex) {
            showMessage("Price must be a valid number.", true);
            return;
        }

        String sql = "UPDATE quick_service_presets SET service = ?, price = ?, icon = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, service);
            ps.setDouble(2, price);
            ps.setString(3, icon != null ? icon : "fas-bolt");
            ps.setString(4, selected.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            showMessage("Failed to update preset: " + ex.getMessage(), true);
            return;
        }

        // Clear fields, reload table, hide save button
        txtService.clear();
        txtPrice.clear();
        btnSave.setVisible(false);
        btnSave.setManaged(false);
        loadPresets();

        showMessage("✓ Successfully updated", false);
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
            showMessage("Failed to delete preset: " + ex.getMessage(), true);
            return;
        }

        loadPresets();
        showMessage("✓ Service deleted successfully", false);
    }

    private QuickServicePreset requireSelection() {
        QuickServicePreset selected = tblPresets.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("Select a preset first.", true);
            return null;
        }
        return selected;
    }

    private boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Quick Service Presets");
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        // Set owner window to make it modal to the main application
        if (tblPresets.getScene() != null && tblPresets.getScene().getWindow() != null) {
            alert.initOwner(tblPresets.getScene().getWindow());
            alert.initModality(javafx.stage.Modality.WINDOW_MODAL);
        }
        
        return alert.showAndWait().filter(btn -> btn == ButtonType.OK).isPresent();
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) tblPresets.getScene().getWindow();
        stage.close();
    }

    private void loadPresets() {
        presets.clear();
        String sql = "SELECT id, service, price, active, icon FROM quick_service_presets ORDER BY service";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                presets.add(new QuickServicePreset(
                        rs.getString("id"),
                        rs.getString("service"),
                        rs.getDouble("price"),
                        rs.getInt("active") == 1,
                        rs.getString("icon")
                ));
            }
        } catch (SQLException ex) {
            showMessage("Failed to load presets: " + ex.getMessage(), true);
        }
    }
}