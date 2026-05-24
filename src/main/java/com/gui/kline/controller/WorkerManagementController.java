package com.gui.kline.controller;

import com.gui.kline.models.ViewModel;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDate;

public class WorkerManagementController {

    @FXML private VBox attendanceRowsContainer;
    @FXML private VBox historyRowsContainer;
    @FXML private VBox monthlySummaryContainer;
    @FXML private Label attendanceDateLabel;

    @FXML
    public void initialize() {
        attendanceDateLabel.setText("Daily Attendance - " + LocalDate.now());

        attendanceRowsContainer.getChildren().clear();
        historyRowsContainer.getChildren().clear();
        monthlySummaryContainer.getChildren().clear();

        loadSampleData();
    }

    private void loadSampleData() {
        addAttendanceRow("Kasun Perera", "Senior Mechanic", "Rs. 2,500");
        addAttendanceRow("Nuwan Silva", "Tyre Specialist", "Rs. 2,200");

        addHistoryRow("2026-03-29", "Kasun Perera", "PRESENT", "#22c55e");
        addHistoryRow("2026-03-29", "Nuwan Silva", "PRESENT", "#22c55e");
        addHistoryRow("2026-03-28", "Kasun Perera", "PRESENT", "#22c55e");
        addHistoryRow("2026-03-28", "Nuwan Silva", "HALF DAY", "#f97316");
        addHistoryRow("2026-04-05", "Kasun Perera", "PRESENT", "#22c55e");

        addSummaryRow("Kasun Perera", "1 Full, 0 Half Days", "Rs. 2,500", "#22c55e");
        addSummaryRow("Nuwan Silva", "0 Full, 0 Half Days", "Rs. 0", "#22c55e");
    }

    private void addAttendanceRow(String name, String role, String rate) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 20, 12, 20));
        row.setStyle("-fx-border-color: transparent transparent #f0f2f5 transparent;");

        VBox nameBox = new VBox(2);
        nameBox.setMinWidth(200); nameBox.setPrefWidth(200);
        Label lblName = new Label(name); lblName.setStyle("-fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
        Label lblRole = new Label(role); lblRole.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");
        nameBox.getChildren().addAll(lblName, lblRole);

        Label lblRate = new Label(rate);
        lblRate.setMinWidth(150); lblRate.setPrefWidth(150);
        lblRate.setStyle("-fx-font-weight: 500;");

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(actions, Priority.ALWAYS);
        Button p = createStatusBtn("✔", "#22c55e");
        Button h = createStatusBtn("🕐", "#f97316");
        Button a = createStatusBtn("✖", "#ef4444");

        p.setOnAction(e -> mark(p, h, a, "#22c55e"));
        h.setOnAction(e -> mark(h, p, a, "#f97316"));
        a.setOnAction(e -> mark(a, p, h, "#ef4444"));
        actions.getChildren().addAll(p, h, a);

        HBox editDel = new HBox(15);
        editDel.setMinWidth(100); editDel.setPrefWidth(100);
        editDel.setAlignment(Pos.CENTER_RIGHT);
        Label edit = new Label("✎"); edit.setStyle("-fx-text-fill: #3b82f6; -fx-cursor: hand; -fx-font-size: 14px;");
        Label del = new Label("🗑"); del.setStyle("-fx-text-fill: #ef4444; -fx-cursor: hand; -fx-font-size: 14px;");
        editDel.getChildren().addAll(edit, del);

        row.getChildren().addAll(nameBox, lblRate, actions, editDel);
        attendanceRowsContainer.getChildren().add(row);
    }

    private void addHistoryRow(String date, String worker, String status, String color) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 20, 12, 20));
        row.setStyle("-fx-border-color: transparent transparent #f0f2f5 transparent; -fx-background-color: white;");

        // 1. DATE COLUMN - Must be exactly 250 to match FXML Header
        Label lblDate = new Label(date);
        lblDate.setMinWidth(250);
        lblDate.setPrefWidth(250);
        lblDate.setMaxWidth(250);
        lblDate.setStyle("-fx-text-fill: #666666; -fx-font-size: 13px;");

        // 2. WORKER COLUMN - Must grow to fill space
        Label lblWorker = new Label(worker);
        lblWorker.setStyle("-fx-font-weight: bold; -fx-text-fill: #1a1a2e; -fx-font-size: 13px;");
        HBox.setHgrow(lblWorker, Priority.ALWAYS);
        lblWorker.setMaxWidth(Double.MAX_VALUE);

        // 3. STATUS COLUMN - Container to right-align the badge
        StackPane statusContainer = new StackPane();
        statusContainer.setMinWidth(100);
        statusContainer.setPrefWidth(100);
        statusContainer.setMaxWidth(100);
        statusContainer.setAlignment(Pos.CENTER_RIGHT);

        Label lblStatus = new Label(status);
        lblStatus.setStyle(
                "-fx-background-color: #f8f9fa; " +
                        "-fx-text-fill: " + color + "; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 10px; " +
                        "-fx-padding: 5 12 5 12; " +
                        "-fx-background-radius: 5; " +
                        "-fx-border-color: #eeeeee; " +
                        "-fx-border-radius: 5;"
        );

        statusContainer.getChildren().add(lblStatus);

        row.getChildren().addAll(lblDate, lblWorker, statusContainer);
        historyRowsContainer.getChildren().add(row);
    }
    private void addSummaryRow(String name, String days, String amount, String color) {
        HBox row = new HBox();
        row.setPadding(new Insets(10, 0, 10, 0));
        VBox v1 = new VBox(2);
        Label n = new Label(name); n.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        Label d = new Label(days); d.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");
        v1.getChildren().addAll(n, d);

        Region s = new Region(); HBox.setHgrow(s, Priority.ALWAYS);

        VBox v2 = new VBox(2); v2.setAlignment(Pos.TOP_RIGHT);
        Label a = new Label(amount); a.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 15px;");
        Label np = new Label("NET PAYABLE"); np.setStyle("-fx-text-fill: #555555; -fx-font-size: 9px;");
        v2.getChildren().addAll(a, np);

        row.getChildren().addAll(v1, s, v2);
        monthlySummaryContainer.getChildren().add(row);
    }

    private Button createStatusBtn(String text, String color) {
        Button b = new Button(text);
        b.setPrefSize(32, 32);
        b.setStyle("-fx-background-radius: 50; -fx-background-color: #f8f9fa; -fx-border-color: #e0e0e0; -fx-text-fill: #aaaaaa; -fx-cursor: hand;");
        return b;
    }

    private void mark(Button sel, Button o1, Button o2, String color) {
        o1.setStyle("-fx-background-radius: 50; -fx-background-color: #f8f9fa; -fx-border-color: #e0e0e0; -fx-text-fill: #aaaaaa;");
        o2.setStyle("-fx-background-radius: 50; -fx-background-color: #f8f9fa; -fx-border-color: #e0e0e0; -fx-text-fill: #aaaaaa;");
        sel.setStyle("-fx-background-radius: 50; -fx-background-color: white; -fx-border-color: " + color + "; -fx-text-fill: " + color + "; -fx-font-weight: bold;");
    }

    @FXML void handleAddWorker(ActionEvent event) {
        Stage ownerStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        ViewModel.INSTANCE.getViewsFactory().getForm("form/add-worker-dialog", ownerStage);
    }
}