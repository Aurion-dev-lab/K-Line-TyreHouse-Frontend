package com.gui.kline.controller;

import com.gui.kline.data.DatabaseManager;
import com.gui.kline.models.ViewModel;
import com.gui.kline.utils.JsonUtil;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class QuickActionsUseController {

    @FXML private GridPane actionsGrid;
    @FXML private Label lblSubtitle;
    @FXML private Label lblTodayCount;
    @FXML private Label lblTodayRevenue;
    @FXML private Label lblStatusMessage;
    @FXML private Label lblLastLogged;

    private PauseTransition lastLoggedHide;

    private static class QuickService {
        final String id;
        final String name;
        final double price;
        final String icon;

        QuickService(String id, String name, double price, String icon) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.icon = icon;
        }
    }

    @FXML
    public void initialize() {
        loadAndRender();
    }

    public void refresh() {
        loadAndRender();
    }

    private void loadAndRender() {
        List<QuickService> services = new ArrayList<>();
        String sql = "SELECT id, service, price, icon FROM quick_service_presets WHERE active = 1 ORDER BY service";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                services.add(new QuickService(
                        rs.getString("id"),
                        rs.getString("service"),
                        rs.getDouble("price"),
                        rs.getString("icon")
                ));
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load quick service presets: " + ex.getMessage());
        }

        populateGrid(services);
        loadTodayStats();
    }

    private void populateGrid(List<QuickService> services) {
        actionsGrid.getChildren().clear();

        if (services.isEmpty()) {
            Label empty = new Label("No active quick services.\nGo to Quick Action Management (⚙) to add some.");
            empty.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 14px; -fx-text-alignment: center;");
            empty.setWrapText(true);
            GridPane.setColumnSpan(empty, 4);
            actionsGrid.getChildren().add(empty);
            if (lblSubtitle != null) lblSubtitle.setText("No services configured yet");
            return;
        }

        if (lblSubtitle != null) {
            lblSubtitle.setText(services.size() + " service" + (services.size() > 1 ? "s" : "") + " available — tap to log");
        }

        int cols = 4;
        int row = 0, col = 0;
        for (QuickService s : services) {
            Button card = buildCard(s);
            GridPane.setColumnIndex(card, col);
            GridPane.setRowIndex(card, row);
            actionsGrid.getChildren().add(card);
            col++;
            if (col >= cols) {
                col = 0;
                row++;
            }
        }
    }

    private Button buildCard(QuickService service) {
        Button btn = new Button();
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setMaxHeight(Double.MAX_VALUE);
        btn.setPrefHeight(130);
        btn.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #e2e8f0;" +
                "-fx-border-radius: 12;" +
                "-fx-border-width: 1;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 2);"
        );

        FontIcon icon = new FontIcon(service.icon != null ? service.icon : "fas-bolt");
        icon.setIconSize(32);
        icon.setIconColor(javafx.scene.paint.Color.web("#f59e0b"));

        Label nameLabel = new Label(service.name);
        nameLabel.setStyle("-fx-text-fill: #1e293b; -fx-font-size: 13px; -fx-font-weight: bold;");
        nameLabel.setWrapText(true);
        nameLabel.setAlignment(Pos.CENTER);

        Label priceLabel = new Label("Rs. " + String.format("%.0f", service.price));
        priceLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 14px; -fx-font-weight: bold;");

        VBox content = new VBox(8, icon, nameLabel, priceLabel);
        content.setAlignment(Pos.CENTER);
        btn.setGraphic(content);

        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: #f1f5f9;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #cbd5e1;" +
                "-fx-border-radius: 12;" +
                "-fx-border-width: 1;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 4);"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #e2e8f0;" +
                "-fx-border-radius: 12;" +
                "-fx-border-width: 1;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 2);"
        ));

        btn.setOnAction(e -> logService(service, btn));
        return btn;
    }

    @FXML
    private void handleManageQuickActions() {
        LayoutController lc = ViewModel.INSTANCE.getViewsFactory().getLayoutController();
        if (lc != null) {
            lc.onQuickActions();
        }
    }

    private void logService(QuickService service, Button sourceBtn) {
        flashCard(sourceBtn);

        String insert = "INSERT INTO quick_services (id, service, price, service_date) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(insert)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, service.name);
            ps.setDouble(3, service.price);
            ps.setString(4, LocalDate.now().toString());
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Failed to log quick service: " + ex.getMessage());
            return;
        }

        JsonUtil.obj(
                JsonUtil.field("service", service.name),
                JsonUtil.field("price", service.price),
                JsonUtil.field("date", LocalDate.now().toString())
        );

        loadTodayStats();
        ViewModel.INSTANCE.getViewsFactory().updateQuickStats();

        showLastLogged("✓ Logged: " + service.name + "  (Rs. " + String.format("%.0f", service.price) + ")");
    }

    private void flashCard(Button btn) {
        String originalStyle = btn.getStyle();
        btn.setStyle(
                "-fx-background-color: #f59e0b;" +
                "-fx-background-radius: 14;" +
                "-fx-border-color: #f59e0b;" +
                "-fx-border-radius: 14;" +
                "-fx-border-width: 1;" +
                "-fx-cursor: hand;"
        );
        PauseTransition flash = new PauseTransition(Duration.millis(200));
        flash.setOnFinished(e -> btn.setStyle(originalStyle));
        flash.play();
    }

    private void loadTodayStats() {
        String sql = "SELECT COUNT(*) AS cnt, COALESCE(SUM(price), 0) AS total " +
                     "FROM quick_services WHERE service_date = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt("cnt");
                    double total = rs.getDouble("total");
                    Platform.runLater(() -> {
                        if (lblTodayCount != null) lblTodayCount.setText(String.valueOf(count));
                        if (lblTodayRevenue != null) lblTodayRevenue.setText("Rs. " + String.format("%.0f", total));
                    });
                }
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load today stats: " + ex.getMessage());
        }
    }

    private void showLastLogged(String message) {
        if (lastLoggedHide != null) lastLoggedHide.stop();
        if (lblLastLogged != null) {
            lblLastLogged.setText(message);
        }
        if (lblStatusMessage != null) {
            lblStatusMessage.setText("Service logged successfully!");
            lblStatusMessage.setStyle("-fx-font-size: 12px; -fx-text-fill: #22c55e;");
        }
        lastLoggedHide = new PauseTransition(Duration.seconds(3));
        lastLoggedHide.setOnFinished(e -> {
            if (lblLastLogged != null) lblLastLogged.setText("");
            if (lblStatusMessage != null) {
                lblStatusMessage.setText("Tap any card above to log that service for today");
                lblStatusMessage.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
            }
        });
        lastLoggedHide.play();
    }
}
