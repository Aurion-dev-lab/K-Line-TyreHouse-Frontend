package com.gui.kline.controller.form;

import com.gui.kline.data.DatabaseManager;
import com.gui.kline.utils.JsonUtil;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class QuickActionsPopupController {

    @FXML private GridPane actionsGrid;
    @FXML private Label lblSubtitle;

    private List<QuickService> services;
    private Runnable onActionLogged;

    public void setServices(List<QuickService> services) {
        this.services = services;
        populateGrid();
    }

    public void setOnActionLogged(Runnable callback) {
        this.onActionLogged = callback;
    }

    private void populateGrid() {
        if (actionsGrid == null || services == null) return;
        actionsGrid.getChildren().clear();

        if (services.isEmpty()) {
            Label empty = new Label("No quick services available");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
            GridPane.setColumnSpan(empty, 3);
            actionsGrid.getChildren().add(empty);
            return;
        }

        lblSubtitle.setText(services.size() + " service" + (services.size() > 1 ? "s" : "") + " available");

        int row = 0;
        int col = 0;
        for (QuickService s : services) {
            Button btn = createButton(s);
            GridPane.setColumnIndex(btn, col);
            GridPane.setRowIndex(btn, row);
            actionsGrid.getChildren().add(btn);

            col++;
            if (col >= 3) {
                col = 0;
                row++;
            }
        }
    }

    private Button createButton(QuickService service) {
        Button btn = new Button();
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setMaxHeight(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color: #22303c; -fx-background-radius: 10; " +
                "-fx-text-fill: white; -fx-font-size: 12px; -fx-cursor: hand;");
        btn.setPrefHeight(90);

        VBox content = new VBox();
        content.setAlignment(Pos.CENTER);
        content.setSpacing(6.0);

        FontIcon icon = new FontIcon(service.icon != null ? service.icon : "fas-bolt");
        icon.setIconSize(28);
        icon.setIconColor(javafx.scene.paint.Color.web("#f59e0b"));

        Label name = new Label(service.name);
        name.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");

        Label price = new Label("Rs. " + String.format("%.0f", service.price));
        price.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 11px; -fx-font-weight: bold;");

        content.getChildren().addAll(icon, name, price);
        btn.setGraphic(content);

        btn.setOnAction(e -> {
            logQuickService(service);
            if (onActionLogged != null) {
                onActionLogged.run();
            }
        });

        return btn;
    }

    private void logQuickService(QuickService service) {
        String insert = "INSERT INTO quick_services (id, service, price, service_date) VALUES (UUID(), ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(insert)) {
            ps.setString(1, service.name);
            ps.setDouble(2, service.price);
            ps.setDate(3, java.sql.Date.valueOf(java.time.LocalDate.now()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Error logging quick service: " + ex.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) actionsGrid.getScene().getWindow();
        stage.close();
    }

    public static class QuickService {
        public String id;
        public String name;
        public double price;
        public String icon;

        public QuickService(String id, String name, double price, String icon) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.icon = icon;
        }
    }
}