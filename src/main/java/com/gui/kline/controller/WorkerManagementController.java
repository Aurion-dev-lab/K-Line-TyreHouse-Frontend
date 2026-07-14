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
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.gui.kline.controller.form.AddWorkerController;
import com.gui.kline.data.LocalWorkerAttendanceRepository;
import com.gui.kline.data.LocalWorkerRepository;
import com.gui.kline.data.SyncQueueRepository;
import com.gui.kline.models.Worker;
import com.gui.kline.models.WorkerAttendance;
import com.gui.kline.models.WorkerAttendanceHistory;
import com.gui.kline.models.WorkerMonthlySummary;
import com.gui.kline.utils.JsonUtil;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class WorkerManagementController {

    @FXML private VBox attendanceRowsContainer;
    @FXML private VBox historyRowsContainer;
    @FXML private VBox monthlySummaryContainer;
    @FXML private Label attendanceDateLabel;
    @FXML private DatePicker attendanceDatePicker;
    @FXML private TextField historySearchField;
    @FXML private DatePicker historyFromDate;
    @FXML private DatePicker historyToDate;
    @FXML private Label monthlySummaryMonthLabel;

    private final LocalWorkerRepository workerRepository = new LocalWorkerRepository();
    private final LocalWorkerAttendanceRepository attendanceRepository = new LocalWorkerAttendanceRepository();
    private final SyncQueueRepository syncQueueRepository = new SyncQueueRepository();
    private Map<String, Worker> workerIndex = new HashMap<>();

    @FXML
    public void initialize() {
        LocalDate today = LocalDate.now();
        attendanceDateLabel.setText("Daily Attendance - " + today);
        attendanceDatePicker.setValue(today);

        attendanceRowsContainer.getChildren().clear();
        historyRowsContainer.getChildren().clear();
        monthlySummaryContainer.getChildren().clear();

        YearMonth currentMonth = YearMonth.now();
        monthlySummaryMonthLabel.setText("CURRENT MONTH: " + currentMonth);
        historyFromDate.setValue(currentMonth.atDay(1));
        historyToDate.setValue(today);

        attendanceDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> loadAttendance(newVal));
        historyFromDate.valueProperty().addListener((obs, oldVal, newVal) -> loadHistory());
        historyToDate.valueProperty().addListener((obs, oldVal, newVal) -> loadHistory());
        historySearchField.textProperty().addListener((obs, oldVal, newVal) -> loadHistory());

        loadAttendance(today);
        loadHistory();
        loadMonthlySummary(currentMonth);
    }

    private void loadAttendance(LocalDate date) {
        if (date == null) {
            return;
        }
        attendanceDateLabel.setText("Daily Attendance - " + date);
        attendanceRowsContainer.getChildren().clear();
        workerIndex = new HashMap<>();
        for (Worker worker : workerRepository.loadWorkers()) {
            workerIndex.put(worker.getId(), worker);
        }
        for (WorkerAttendance attendance : attendanceRepository.loadAttendanceForDate(date)) {
            Worker worker = workerIndex.get(attendance.getWorkerId());
            addAttendanceRow(attendance, worker);
        }
    }

    private void loadHistory() {
        LocalDate from = historyFromDate.getValue();
        LocalDate to = historyToDate.getValue();
        if (from == null || to == null) {
            return;
        }
        historyRowsContainer.getChildren().clear();
        for (WorkerAttendanceHistory row : attendanceRepository.loadHistory(from, to, historySearchField.getText())) {
            String[] status = statusLabel(row.getStatus());
            addHistoryRow(row.getDate().toString(), row.getWorkerName(), status[0], status[1]);
        }
    }

    private void loadMonthlySummary(YearMonth month) {
        monthlySummaryMonthLabel.setText("CURRENT MONTH: " + month);
        monthlySummaryContainer.getChildren().clear();
        for (WorkerMonthlySummary row : attendanceRepository.loadMonthlySummary(month)) {
            addSummaryRow(row.getWorkerName(), String.format("%.1f DAYS", row.getDays()),
                    String.format("Rs. %.2f", row.getNetPayable()), "#22c55e");
        }
    }

    private void addAttendanceRow(WorkerAttendance attendance, Worker worker) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 20, 12, 20));
        row.setStyle("-fx-border-color: transparent transparent #f0f2f5 transparent;");

        VBox nameBox = new VBox(2);
        nameBox.setMinWidth(200); nameBox.setPrefWidth(200);
        Label lblName = new Label(attendance.getWorkerName()); lblName.setStyle("-fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
        Label lblRole = new Label(attendance.getRole()); lblRole.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");
        nameBox.getChildren().addAll(lblName, lblRole);

        Label lblRate = new Label(attendance.getRate());
        lblRate.setMinWidth(150); lblRate.setPrefWidth(150);
        lblRate.setStyle("-fx-font-weight: 500;");

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(actions, Priority.ALWAYS);
        Button p = createStatusBtn("✔", "#22c55e");
        Button h = createStatusBtn("🕐", "#f97316");
        Button a = createStatusBtn("✖", "#ef4444");

        applyStatus(p, h, a, attendance.getStatus());

        p.setOnAction(e -> saveAttendance(attendance.getWorkerId(), "PRESENT", p, h, a, "#22c55e"));
        h.setOnAction(e -> saveAttendance(attendance.getWorkerId(), "HALF_DAY", h, p, a, "#f97316"));
        a.setOnAction(e -> saveAttendance(attendance.getWorkerId(), "ABSENT", a, p, h, "#ef4444"));
        actions.getChildren().addAll(p, h, a);

        HBox editDel = new HBox(15);
        editDel.setMinWidth(100); editDel.setPrefWidth(100);
        editDel.setAlignment(Pos.CENTER_RIGHT);
        Label edit = new Label("✎"); edit.setStyle("-fx-text-fill: #3b82f6; -fx-cursor: hand; -fx-font-size: 14px;");
        Label del = new Label("🗑"); del.setStyle("-fx-text-fill: #ef4444; -fx-cursor: hand; -fx-font-size: 14px;");

        edit.setOnMouseClicked(e -> openEditWorker(worker));
        del.setOnMouseClicked(e -> deleteWorker(worker));

        editDel.getChildren().addAll(edit, del);

        row.getChildren().addAll(nameBox, lblRate, actions, editDel);
        attendanceRowsContainer.getChildren().add(row);
    }

    private void saveAttendance(String workerId, String status, Button selected, Button other1, Button other2, String color) {
        LocalDate date = attendanceDatePicker.getValue();
        if (date == null) {
            return;
        }
        attendanceRepository.upsertAttendance(workerId, date, status);
        String payload = JsonUtil.obj(
                JsonUtil.field("workerId", workerId),
                JsonUtil.field("date", date.toString()),
                JsonUtil.field("status", status),
                JsonUtil.field("op", "upsert")
        );
        syncQueueRepository.enqueue("worker_attendance", payload);
        mark(selected, other1, other2, color);
        loadHistory();
        loadMonthlySummary(YearMonth.from(date));
    }

    private void openEditWorker(Worker worker) {
        if (worker == null) {
            return;
        }
        Stage ownerStage = (Stage) attendanceRowsContainer.getScene().getWindow();
        AddWorkerController controller = ViewModel.INSTANCE.getViewsFactory()
                .getForm("form/add-worker-dialog", ownerStage);
        if (controller != null) {
            controller.setEditMode(worker);
            controller.setOnSaved(() -> {
                loadAttendance(attendanceDatePicker.getValue());
                loadHistory();
                loadMonthlySummary(YearMonth.now());
            });
        }
    }

    private void deleteWorker(Worker worker) {
        if (worker == null) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Worker");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete " + worker.getName() + " and related attendance?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
        workerRepository.deleteWorker(worker.getId());
        String payload = JsonUtil.obj(
                JsonUtil.field("id", worker.getId()),
                JsonUtil.field("op", "delete")
        );
        syncQueueRepository.enqueue("worker", payload);
        loadAttendance(attendanceDatePicker.getValue());
        loadHistory();
        loadMonthlySummary(YearMonth.now());
    }

    private void applyStatus(Button present, Button half, Button absent, String status) {
        if ("PRESENT".equalsIgnoreCase(status)) {
            mark(present, half, absent, "#22c55e");
        } else if ("HALF_DAY".equalsIgnoreCase(status)) {
            mark(half, present, absent, "#f97316");
        } else if ("ABSENT".equalsIgnoreCase(status)) {
            mark(absent, present, half, "#ef4444");
        }
    }

    private String[] statusLabel(String status) {
        if ("PRESENT".equalsIgnoreCase(status)) {
            return new String[]{"PRESENT", "#22c55e"};
        }
        if ("HALF_DAY".equalsIgnoreCase(status)) {
            return new String[]{"HALF DAY", "#f97316"};
        }
        return new String[]{"ABSENT", "#ef4444"};
    }

    private void addHistoryRow(String date, String worker, String status, String color) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 20, 12, 20));
        row.setStyle("-fx-border-color: transparent transparent #f0f2f5 transparent; -fx-background-color: white;");

        Label lblDate = new Label(date);
        lblDate.setMinWidth(250);
        lblDate.setPrefWidth(250);
        lblDate.setMaxWidth(250);
        lblDate.setStyle("-fx-text-fill: #666666; -fx-font-size: 13px;");

        Label lblWorker = new Label(worker);
        lblWorker.setStyle("-fx-font-weight: bold; -fx-text-fill: #1a1a2e; -fx-font-size: 13px;");
        HBox.setHgrow(lblWorker, Priority.ALWAYS);
        lblWorker.setMaxWidth(Double.MAX_VALUE);

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
        AddWorkerController controller = ViewModel.INSTANCE.getViewsFactory()
                .getForm("form/add-worker-dialog", ownerStage);
        if (controller != null) {
            controller.setOnSaved(() -> {
                loadAttendance(attendanceDatePicker.getValue());
                loadHistory();
                loadMonthlySummary(YearMonth.now());
            });
        }
    }
}