package com.gui.kline.controller;

import com.gui.kline.data.DatabaseManager;
import com.gui.kline.models.QuickServicePreset;
import com.gui.kline.models.ViewModel;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class QuickActionController {

    @FXML private TableView<QuickServicePreset> tblPresets;
    @FXML private TableColumn<QuickServicePreset, String> colIcon;
    @FXML private TableColumn<QuickServicePreset, String> colService;
    @FXML private TableColumn<QuickServicePreset, String> colPrice;
    @FXML private TableColumn<QuickServicePreset, String> colStatus;

    @FXML private TextField txtService;
    @FXML private TextField txtPrice;
    @FXML private ComboBox<String> cmbIcon;

    @FXML private Button btnAdd;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;

    @FXML private Label lblMessage;
    @FXML private Label lblFormTitle;
    @FXML private Label lblActiveCount;
    @FXML private Label lblTotalCount;

    private final ObservableList<QuickServicePreset> presets = FXCollections.observableArrayList();
    private PauseTransition pendingHide;

    private final String[] ICON_CHOICES = {
            "fas-bolt",             // lightning / quick
            "fas-wrench",           // general repair
            "fas-tools",            // tools
            "fas-cog",              // settings
            "fas-cogs",             // multiple cogs
            "fas-oil-can",          // oil
            "fas-tint",             // coolant / fluid
            "fas-water",            // water / wash
            "fas-wind",             // air / tyre
            "fas-car",              // car
            "fas-truck",            // truck
            "fas-fire",             // exhaust
            "fas-fan",              // AC / cooling
            "fas-broom",            // cleaning
            "fas-shield-alt",       // brake / safety
            "fas-battery-full",     // battery
            "fas-temperature-high", // engine temp
            "fas-charging-station", // charging
            "fas-filter"            // oil filter
    };

    @FXML
    public void initialize() {
        colIcon.setCellValueFactory(data -> data.getValue().iconProperty());
        colIcon.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String iconLiteral, boolean empty) {
                super.updateItem(iconLiteral, empty);
                if (empty || iconLiteral == null) {
                    setGraphic(null);
                    setStyle("-fx-alignment: CENTER;");
                } else {
                    FontIcon icon = new FontIcon(iconLiteral);
                    icon.setIconSize(18);
                    setGraphic(icon);
                    setStyle("-fx-alignment: CENTER;");
                    setText(null);
                }
            }
        });

        colService.setCellValueFactory(data -> data.getValue().serviceProperty());
        colPrice.setCellValueFactory(data -> data.getValue().priceLabelProperty());
        colStatus.setCellValueFactory(data -> data.getValue().statusProperty());

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if (status.equalsIgnoreCase("Active")) {
                        setStyle("-fx-text-fill: #059669; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    } else {
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    }
                }
            }
        });

        colService.setStyle("-fx-alignment: CENTER;");
        colPrice.setStyle("-fx-alignment: CENTER;");
        colIcon.setStyle("-fx-alignment: CENTER;");
        colStatus.setStyle("-fx-alignment: CENTER;");

        tblPresets.setItems(presets);

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
                    Label label = new Label(formatIconName(item));
                    label.setStyle("-fx-text-fill: black;");
                    HBox box = new HBox(8, icon, label);
                    box.setPadding(new Insets(2, 0, 2, 0));
                    setGraphic(box);
                    setText(null);
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
                    Label label = new Label(formatIconName(item));
                    label.setStyle("-fx-text-fill: black;");
                    HBox box = new HBox(8, icon, label);
                    setGraphic(box);
                    setText(null);
                }
            }
        });
        cmbIcon.setItems(FXCollections.observableArrayList(ICON_CHOICES));
        cmbIcon.getSelectionModel().select(0);

        loadPresets();
    }


    @FXML
    private void handleAddPreset() {
        String service = trimmed(txtService.getText());
        String priceText = trimmed(txtPrice.getText());
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

        String sql = "INSERT INTO quick_service_presets (id, service, price, active, icon, created_at) VALUES (?, ?, ?, 1, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, service);
            ps.setDouble(3, price);
            ps.setString(4, icon != null ? icon : "fas-bolt");
            ps.executeUpdate();
        } catch (SQLException ex) {
            showMessage("Failed to add: " + ex.getMessage(), true);
            return;
        }

        clearForm();
        loadPresets();
        notifyLayout();
        showMessage("✓ Quick action added successfully", false);
    }

    @FXML
    private void handleEditPreset() {
        QuickServicePreset selected = requireSelection();
        if (selected == null) return;

        txtService.setText(selected.getService());
        txtPrice.setText(String.format("%.0f", selected.getPrice()));

        for (int i = 0; i < ICON_CHOICES.length; i++) {
            if (ICON_CHOICES[i].equals(selected.getIcon())) {
                cmbIcon.getSelectionModel().select(i);
                break;
            }
        }

        lblFormTitle.setText("Edit Quick Action");
        btnAdd.setVisible(false);
        btnAdd.setManaged(false);
        btnSave.setVisible(true);
        btnSave.setManaged(true);
        btnCancel.setVisible(true);
        btnCancel.setManaged(true);
    }

    @FXML
    private void handleSavePreset() {
        QuickServicePreset selected = tblPresets.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("Select a preset first.", true);
            return;
        }

        String service = trimmed(txtService.getText());
        String priceText = trimmed(txtPrice.getText());
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
            showMessage("Failed to update: " + ex.getMessage(), true);
            return;
        }

        clearForm();
        loadPresets();
        notifyLayout();
        showMessage("✓ Successfully updated", false);
    }

    @FXML
    private void handleCancelEdit() {
        clearForm();
    }

    @FXML
    private void handleTogglePreset() {
        QuickServicePreset selected = requireSelection();
        if (selected == null) return;

        String sql = "UPDATE quick_service_presets SET active = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, selected.isActive() ? 0 : 1);
            ps.setString(2, selected.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            showMessage("Failed to update: " + ex.getMessage(), true);
            return;
        }

        loadPresets();
        notifyLayout();
        showMessage(selected.isActive() ? "✓ Disabled successfully" : "✓ Enabled successfully", false);
    }

    @FXML
    private void handleDeletePreset() {
        QuickServicePreset selected = requireSelection();
        if (selected == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Quick Action");
        alert.setHeaderText(null);
        alert.setContentText("Remove \"" + selected.getService() + "\" from quick actions?");
        if (tblPresets.getScene() != null && tblPresets.getScene().getWindow() != null) {
            alert.initOwner(tblPresets.getScene().getWindow());
            alert.initModality(javafx.stage.Modality.WINDOW_MODAL);
        }
        boolean confirmed = alert.showAndWait()
                .filter(btn -> btn == ButtonType.OK)
                .isPresent();
        if (!confirmed) return;

        String sql = "DELETE FROM quick_service_presets WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, selected.getId());
            ps.executeUpdate();
            DatabaseManager.logDeletion("quick_service_presets", selected.getId());
        } catch (SQLException ex) {
            showMessage("Failed to delete: " + ex.getMessage(), true);
            return;
        }

        loadPresets();
        notifyLayout();
        showMessage("✓ Deleted successfully", false);
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
        updateStats();
    }

    private void updateStats() {
        long active = presets.stream().filter(QuickServicePreset::isActive).count();
        if (lblActiveCount != null) lblActiveCount.setText(String.valueOf(active));
        if (lblTotalCount != null) lblTotalCount.setText(String.valueOf(presets.size()));
    }

    private void notifyLayout() {
        ViewModel.INSTANCE.getViewsFactory().refreshDashboardQuickActions();
        ViewModel.INSTANCE.getViewsFactory().refreshServices();
        LayoutController lc = ViewModel.INSTANCE.getViewsFactory().getLayoutController();
        if (lc != null) {
            lc.loadQuickActionsPanel();
            lc.loadQuickStats();
        }
    }

    private void clearForm() {
        txtService.clear();
        txtPrice.clear();
        cmbIcon.getSelectionModel().select(0);
        tblPresets.getSelectionModel().clearSelection();
        lblFormTitle.setText("Add New Quick Action");
        btnAdd.setVisible(true);
        btnAdd.setManaged(true);
        btnSave.setVisible(false);
        btnSave.setManaged(false);
        btnCancel.setVisible(false);
        btnCancel.setManaged(false);
    }

    private QuickServicePreset requireSelection() {
        QuickServicePreset selected = tblPresets.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("Select a quick action from the table first.", true);
        }
        return selected;
    }

    private void showMessage(String text, boolean isError) {
        if (pendingHide != null) pendingHide.stop();
        lblMessage.setText(text);
        if (isError) {
            lblMessage.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 13px; -fx-font-weight: bold;"
                    + "-fx-background-color: #fff1f2; -fx-background-radius: 6; -fx-padding: 8 12 8 12;");
        } else {
            lblMessage.setStyle("-fx-text-fill: #059669; -fx-font-size: 13px; -fx-font-weight: bold;"
                    + "-fx-background-color: #ecfdf5; -fx-background-radius: 6; -fx-padding: 8 12 8 12;");
        }
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

    private String trimmed(String s) {
        return s == null ? "" : s.trim();
    }

    private String formatIconName(String iconLiteral) {
        if (iconLiteral == null) return "";
        String name = iconLiteral.replace("fas-", "").replace("far-", "");
        name = name.replace("-", " ");
        return name.isEmpty() ? name : name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}
