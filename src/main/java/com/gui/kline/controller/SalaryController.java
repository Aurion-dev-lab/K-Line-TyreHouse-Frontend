package com.gui.kline.controller;


import com.gui.kline.models.dto.LedgerEntry;
import com.gui.kline.models.reports.SalaryPayment;
import com.gui.kline.models.ViewModel;
import com.gui.kline.models.ui.WorkerSalary;
import com.gui.kline.data.LocalSalaryRepository;
import com.gui.kline.data.LocalSalaryAdvanceRepository;
import com.gui.kline.data.LocalWorkerCreditRepository;
import com.gui.kline.utils.JsonUtil;
import com.gui.kline.controller.form.GiveCreditDialogController;
import com.gui.kline.controller.form.PaymentHistoryDialogController;
import com.gui.kline.controller.form.SalaryAdvanceController;
import com.gui.kline.controller.form.SettleCreditDialogController;
import com.gui.kline.utils.AlertUtil;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.FileChooser;

import java.net.URL;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public class SalaryController implements Initializable {

    @FXML private ComboBox<YearMonth> cmbSalaryMonth;
    private LocalDate rangeFrom, rangeTo;
    @FXML private Button btnRecordAdvance, btnGiveCredit, btnSettleCredit, btnExportPayroll;

    @FXML private Label lblNetPayout, lblGross;
    @FXML private Label lblPaidSalary;
    @FXML private Label lblTotalAdvances;
    @FXML private Label lblCreditBalance;
    @FXML private Label lblActiveWorkers, lblWorkersSubtitle;

    @FXML private TableView<WorkerSalary>                    tblSalary;
    @FXML private TableColumn<WorkerSalary, WorkerSalary>    colWorker;
    @FXML private TableColumn<WorkerSalary, WorkerSalary>    colAttendance;
    @FXML private TableColumn<WorkerSalary, Double>          colGrossSalary;
    @FXML private TableColumn<WorkerSalary, Double>          colAdvances;
    @FXML private TableColumn<WorkerSalary, Double>          colCreditBalance;
    @FXML private TableColumn<WorkerSalary, WorkerSalary>    colDeductCredit;
    @FXML private TableColumn<WorkerSalary, WorkerSalary>    colNetPayable;
    @FXML private TableColumn<WorkerSalary, WorkerSalary>    colRemaining;
    @FXML private TableColumn<WorkerSalary, WorkerSalary>    colStatus;
    @FXML private TableColumn<WorkerSalary, WorkerSalary>    colSalaryActions;

    private final Map<String, Boolean> deductCreditMap = new HashMap<>();

    @FXML private TableView<LedgerEntry>                     tblPayoutLedger;
    @FXML private TableColumn<LedgerEntry, String>           colPayoutDate;
    @FXML private TableColumn<LedgerEntry, String>           colPayoutWorker;
    @FXML private TableColumn<LedgerEntry, LedgerEntry>      colPayoutType;
    @FXML private TableColumn<LedgerEntry, String>           colPayoutNote;
    @FXML private TableColumn<LedgerEntry, LedgerEntry>      colPayoutAmount;
    @FXML private TableColumn<LedgerEntry, LedgerEntry>      colPayoutActions;
    @FXML private HBox hboxPayoutSummary;

    @FXML private TableView<LedgerEntry>                     tblLedger;
    @FXML private TableColumn<LedgerEntry, String>           colLedgerDate;
    @FXML private TableColumn<LedgerEntry, String>           colLedgerWorker;
    @FXML private TableColumn<LedgerEntry, LedgerEntry>      colLedgerType;
    @FXML private TableColumn<LedgerEntry, String>           colLedgerNote;
    @FXML private TableColumn<LedgerEntry, LedgerEntry>      colLedgerAmount;
    @FXML private TableColumn<LedgerEntry, LedgerEntry>      colLedgerActions;
    @FXML private HBox hboxCreditSummary;

    private final ObservableList<WorkerSalary> salaryList = FXCollections.observableArrayList();
    private final ObservableList<LedgerEntry>  ledgerList = FXCollections.observableArrayList();
    private final ObservableList<LedgerEntry>  payoutLedgerList = FXCollections.observableArrayList();
    private final LocalSalaryRepository salaryRepository = new LocalSalaryRepository();
    private final LocalSalaryAdvanceRepository advanceRepository = new LocalSalaryAdvanceRepository();
    private final LocalWorkerCreditRepository creditRepository = new LocalWorkerCreditRepository();
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        YearMonth currentMonth = YearMonth.now();
        setupMonthComboBox(currentMonth);
        
        // Set initial date range based on current month
        rangeFrom = currentMonth.atDay(1);
        rangeTo = currentMonth.atEndOfMonth();

        setupSalaryTable();
        setupPayoutLedgerTable();
        setupLedgerTable();
        reloadData();
    }

    private void setupMonthComboBox(YearMonth currentMonth) {
        if (cmbSalaryMonth != null) {
            // Populate with last 12 months
            for (int i = 0; i < 12; i++) {
                cmbSalaryMonth.getItems().add(currentMonth.minusMonths(i));
            }
            cmbSalaryMonth.setValue(currentMonth);
            cmbSalaryMonth.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    rangeFrom = newVal.atDay(1);
                    rangeTo = newVal.atEndOfMonth();
                    reloadData();
                }
            });
        }
    }

    private void reloadData() {
        if (rangeFrom == null || rangeTo == null) {
            return;
        }
        List<WorkerSalary> loaded = salaryRepository.loadWorkerSalaries(rangeFrom, rangeTo);
        for (WorkerSalary w : loaded) {
            deductCreditMap.putIfAbsent(w.getWorkerId(), w.getCreditBalance() > 0);
        }
        salaryList.setAll(loaded);
        payoutLedgerList.setAll(salaryRepository.loadPayoutLedger(rangeFrom, rangeTo));
        ledgerList.setAll(creditRepository.loadLedger(rangeFrom, rangeTo));
        refreshSummary();
        refreshPayoutSummary();
        refreshCreditSummary();
    }

    private boolean canDeductCredit(WorkerSalary w) {
        if (w == null || w.getCreditBalance() <= 0) return false;
        double remainingWithoutCredit = w.getRemainingPayable(false);
        return remainingWithoutCredit >= w.getCreditBalance();
    }

    private boolean isDeductCreditSelected(WorkerSalary w) {
        if (!canDeductCredit(w)) return false;
        return deductCreditMap.getOrDefault(w.getWorkerId(), true);
    }

    private void setupSalaryTable() {

        colWorker.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colWorker.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(WorkerSalary worker, boolean empty) {
                super.updateItem(worker, empty);
                if (empty || worker == null) { setGraphic(null); return; }
                Label name = new Label(worker.getName());
                name.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #111827;");
                Label role = new Label(worker.getRole() != null ? worker.getRole() : "");
                role.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");
                VBox box = new VBox(2, name, role);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box); setText(null);
                setStyle("-fx-background-color: transparent;");
            }
        });

        colAttendance.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colAttendance.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(WorkerSalary w, boolean empty) {
                super.updateItem(w, empty);
                if (empty || w == null) { setGraphic(null); return; }

                Label p = new Label(String.valueOf(w.getPresent()));
                p.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #065f46; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 12px; -fx-padding: 3 8 3 8;");

                Label l = new Label(String.valueOf(w.getLate()));
                l.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 12px; -fx-padding: 3 8 3 8;");

                Label a = new Label(String.valueOf(w.getAbsent()));
                a.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 12px; -fx-padding: 3 8 3 8;");

                HBox box = new HBox(4, p, l, a);
                box.setAlignment(Pos.CENTER);
                setGraphic(box); setText(null);
                setStyle("-fx-background-color: transparent;");
                setAlignment(Pos.CENTER);
            }
        });

        colGrossSalary.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getGrossSalary()));
        colGrossSalary.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                setText(String.format("Rs. %,.0f", v));
                setStyle("-fx-font-size: 13px; -fx-text-fill: #111827; -fx-background-color: transparent; -fx-alignment: center;");
                setAlignment(Pos.CENTER);
            }
        });

        colAdvances.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getAdvances()));
        colAdvances.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                setText(String.format("Rs. %,.0f", v));
                setStyle("-fx-font-size: 13px; -fx-text-fill: #f59e0b; -fx-font-weight: bold; -fx-background-color: transparent; -fx-alignment: center;");
                setAlignment(Pos.CENTER);
            }
        });

        colCreditBalance.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getCreditBalance()));
        colCreditBalance.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                setText(String.format("Rs. %,.0f", v));
                setStyle("-fx-font-size: 13px; -fx-text-fill: #e11d48; -fx-font-weight: bold; -fx-background-color: transparent; -fx-alignment: center;");
                setAlignment(Pos.CENTER);
            }
        });

        colNetPayable.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colNetPayable.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(WorkerSalary w, boolean empty) {
                super.updateItem(w, empty);
                if (empty || w == null) { setText(null); setGraphic(null); return; }
                boolean deduct = isDeductCreditSelected(w);
                setText(String.format("Rs. %,.0f", w.getNetPayable(deduct)));
                setStyle("-fx-font-size: 13px; -fx-text-fill: #059669; -fx-font-weight: bold; -fx-background-color: transparent; -fx-alignment: center;");
                setAlignment(Pos.CENTER);
            }
        });

        colDeductCredit.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colDeductCredit.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(WorkerSalary w, boolean empty) {
                super.updateItem(w, empty);
                if (empty || w == null) { setGraphic(null); return; }
                if (!canDeductCredit(w)) { setGraphic(null); return; }
                CheckBox chk = new CheckBox();
                chk.setSelected(deductCreditMap.getOrDefault(w.getWorkerId(), true));
                chk.setStyle("-fx-cursor: hand;");
                chk.selectedProperty().addListener((obs, oldVal, newVal) -> {
                    deductCreditMap.put(w.getWorkerId(), newVal);
                    tblSalary.refresh();
                    refreshSummary();
                });
                HBox box = new HBox(chk);
                box.setAlignment(Pos.CENTER);
                setGraphic(box); setText(null);
                setStyle("-fx-background-color: transparent;");
                setAlignment(Pos.CENTER);
            }
        });

        colRemaining.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colRemaining.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(WorkerSalary w, boolean empty) {
                super.updateItem(w, empty);
                if (empty || w == null) { setText(null); setGraphic(null); return; }
                boolean deduct = isDeductCreditSelected(w);
                double remaining = w.getRemainingPayable(deduct);
                if (remaining <= 0 && w.getPaidAmount() > 0) {
                    Label badge = new Label("✓ Paid");
                    badge.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #065f46; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 3 10 3 10;");
                    HBox box = new HBox(badge); box.setAlignment(Pos.CENTER);
                    setGraphic(box); setText(null);
                } else {
                    setText(remaining > 0 ? String.format("Rs. %,.0f", remaining) : "—");
                    setGraphic(null);
                    setStyle("-fx-font-size: 13px; -fx-text-fill: " + (remaining > 0 ? "#f59e0b" : "#6b7280") + "; -fx-font-weight: bold; -fx-background-color: transparent; -fx-alignment: center;");
                }
                setAlignment(Pos.CENTER);
            }
        });

        colStatus.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(WorkerSalary w, boolean empty) {
                super.updateItem(w, empty);
                if (empty || w == null) { setGraphic(null); return; }
                boolean deduct = isDeductCreditSelected(w);
                String v = w.getStatus(deduct);
                boolean paid = "PAID".equalsIgnoreCase(v);
                boolean partiallyPaid = "PARTIALLY PAID".equalsIgnoreCase(v);
                boolean noData = "NO DATA".equalsIgnoreCase(v) || "NO PAYABLE".equalsIgnoreCase(v);
                Label badge = new Label(v);
                badge.setStyle(
                        "-fx-background-color: " + (paid ? "#d1fae5" : partiallyPaid ? "#dbeafe" : noData ? "#f3f4f6" : "#fef3c7") + ";" +
                                "-fx-text-fill: " + (paid ? "#065f46" : partiallyPaid ? "#1d4ed8" : noData ? "#6b7280" : "#92400e") + ";" +
                                "-fx-font-size: 11px; -fx-font-weight: bold;" +
                                "-fx-background-radius: 20px; -fx-padding: 4 14 4 14;"
                );
                HBox wrap = new HBox(badge);
                wrap.setAlignment(Pos.CENTER);
                setGraphic(wrap); setText(null);
                setStyle("-fx-background-color: transparent;");
                setAlignment(Pos.CENTER);
            }
        });

        colSalaryActions.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colSalaryActions.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(WorkerSalary worker, boolean empty) {
                super.updateItem(worker, empty);
                if (empty || worker == null) { setGraphic(null); return; }

                boolean deduct = isDeductCreditSelected(worker);
                boolean canPay = worker.getRemainingPayable(deduct) > 0 &&
                        !"NO DATA".equalsIgnoreCase(worker.getStatus(deduct)) &&
                        !"NO PAYABLE".equalsIgnoreCase(worker.getStatus(deduct));
                boolean hasPayments = worker.getPaidAmount() > 0;

                HBox wrap = new HBox(8);
                wrap.setAlignment(Pos.CENTER);

                if (hasPayments) {
                    Button del = new Button("🗑");
                    del.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-font-size: 15px; -fx-cursor: hand;");
                    del.setOnMouseEntered(ev -> del.setStyle("-fx-background-color: transparent; -fx-text-fill: #dc2626; -fx-font-size: 15px; -fx-cursor: hand;"));
                    del.setOnMouseExited(ev -> del.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-font-size: 15px; -fx-cursor: hand;"));
                    del.setOnAction(ev -> showPaymentHistory(worker));
                    wrap.getChildren().add(del);
                }

                Button pay = new Button(canPay && hasPayments ? "Pay Balance" : "Pay");
                pay.setDisable(!canPay);
                pay.setStyle("-fx-background-color: " + (canPay ? "#059669" : "#d1d5db") + ";" +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;" +
                        "-fx-background-radius: 8px; -fx-padding: 6 14 6 14; -fx-cursor: hand;");
                pay.setOnAction(event -> showPaymentEditor(worker));
                wrap.getChildren().add(pay);

                setGraphic(wrap); setText(null);
                setStyle("-fx-background-color: transparent;");
                setAlignment(Pos.CENTER);
            }

            private void showPaymentEditor(WorkerSalary worker) {
                boolean canDeduct = canDeductCredit(worker);
                boolean currentDeduct = isDeductCreditSelected(worker);

                // ── Dialog shell ──────────────────────────────────────
                Dialog<ButtonType> dialog = new Dialog<>();
                dialog.setTitle("Record Payment");
                dialog.setHeaderText(null);
                if (tblSalary.getScene() != null) {
                    dialog.initOwner(tblSalary.getScene().getWindow());
                    dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
                }

                // ── Content ───────────────────────────────────────────
                VBox root = new VBox(16);
                root.setStyle("-fx-padding: 24 28 8 28; -fx-min-width: 340px;");

                // Worker name + role header
                Label workerName = new Label(worker.getName());
                workerName.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #111827;");
                Label workerRole = new Label(worker.getRole() != null ? worker.getRole() : "Worker");
                workerRole.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
                VBox workerHeader = new VBox(2, workerName, workerRole);

                // ── Summary strip ──────────────────────────────────────
                VBox summaryBox = new VBox(6);
                summaryBox.setStyle("-fx-background-color: #f9fafb; -fx-background-radius: 10px; -fx-padding: 12 14 12 14;");

                HBox grossRow = summaryRow("Gross salary", String.format("Rs. %,.0f", worker.getGrossSalary()), "#111827");
                HBox advRow   = summaryRow("Advances deducted", String.format("- Rs. %,.0f", worker.getAdvances()), "#f59e0b");

                summaryBox.getChildren().addAll(grossRow, advRow);

                // ── Credit deduction toggle ────────────────────────────
                CheckBox chkDeductCredit = null;
                if (canDeduct) {
                    chkDeductCredit = new CheckBox(String.format("Deduct outstanding credit  (Rs. %,.0f)", worker.getCreditBalance()));
                    chkDeductCredit.setSelected(currentDeduct);
                    chkDeductCredit.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151; -fx-cursor: hand;");
                    HBox creditToggleRow = new HBox(chkDeductCredit);
                    creditToggleRow.setStyle("-fx-background-color: #fef9c3; -fx-background-radius: 8px; -fx-padding: 8 12 8 12;");
                    summaryBox.getChildren().add(new javafx.scene.control.Separator());
                    summaryBox.getChildren().add(creditToggleRow);
                }

                // ── Net payable label (updates live) ───────────────────
                Label netLabel = new Label("Net payable");
                netLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
                Label netValue = new Label(String.format("Rs. %,.0f", worker.getNetPayable(currentDeduct)));
                netValue.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #059669;");
                Label alreadyPaidLbl = new Label(String.format("Already paid: Rs. %,.0f", worker.getPaidAmount()));
                alreadyPaidLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");

                HBox netRow = new HBox();
                netRow.setAlignment(Pos.CENTER_LEFT);
                Region netSpacer = new Region(); HBox.setHgrow(netSpacer, Priority.ALWAYS);
                netRow.getChildren().addAll(netLabel, netSpacer, netValue);

                summaryBox.getChildren().addAll(new javafx.scene.control.Separator(), netRow);
                if (worker.getPaidAmount() > 0) summaryBox.getChildren().add(alreadyPaidLbl);

                // ── Amount input ───────────────────────────────────────
                Label amtLabel = new Label("Payment amount");
                amtLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #374151;");

                TextField amount = new TextField(String.format("%.0f", worker.getRemainingPayable(currentDeduct)));
                amount.setPromptText("Enter amount to pay");
                amount.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 8px; " +
                        "-fx-border-color: #d1d5db; -fx-border-radius: 8px; -fx-padding: 8 12 8 12;");
                amount.setPrefWidth(260);

                Label error = new Label();
                error.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 11px; -fx-font-weight: bold;");
                error.setWrapText(true);

                VBox amtSection = new VBox(6, amtLabel, amount, error);

                // ── Wire credit toggle → update net + amount ───────────
                final CheckBox finalChk = chkDeductCredit;
                if (chkDeductCredit != null) {
                    chkDeductCredit.selectedProperty().addListener((obs, old, newVal) -> {
                        deductCreditMap.put(worker.getWorkerId(), newVal);
                        tblSalary.refresh();
                        refreshSummary();
                        netValue.setText(String.format("Rs. %,.0f", worker.getNetPayable(newVal)));
                        amount.setText(String.format("%.0f", worker.getRemainingPayable(newVal)));
                    });
                }

                root.getChildren().addAll(workerHeader, summaryBox, amtSection);
                dialog.getDialogPane().setContent(root);
                dialog.getDialogPane().setStyle("-fx-background-color: white; -fx-background-radius: 14px;");

                // ── Buttons ────────────────────────────────────────────
                ButtonType payBtn = new ButtonType("✓  Confirm Payment", ButtonBar.ButtonData.OK_DONE);
                dialog.getDialogPane().getButtonTypes().addAll(payBtn, ButtonType.CANCEL);

                javafx.scene.control.Button confirmBtn =
                        (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(payBtn);
                confirmBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; " +
                        "-fx-font-size: 13px; -fx-background-radius: 8px; -fx-padding: 8 20 8 20; -fx-cursor: hand;");

                javafx.scene.control.Button cancelBtn =
                        (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
                cancelBtn.setStyle("-fx-background-color: #f3f4f6; -fx-text-fill: #374151; -fx-font-weight: bold; " +
                        "-fx-font-size: 13px; -fx-background-radius: 8px; -fx-padding: 8 16 8 16; -fx-cursor: hand;");

                // Prevent dialog from closing on pay if there's a validation error
                confirmBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
                    boolean deduct = finalChk != null && finalChk.isSelected();
                    String msg = paySalary(worker, amount.getText(), deduct);
                    if (msg != null) {
                        error.setText(msg);
                        ev.consume(); // stay open
                    }
                });

                amount.setOnAction(e -> confirmBtn.fire());
                amount.requestFocus();
                amount.selectAll();
                dialog.showAndWait();
            }

            // Helper: builds a left/right label row for the summary strip
            private HBox summaryRow(String label, String value, String valueColor) {
                Label l = new Label(label);
                l.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
                Label v = new Label(value);
                v.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + valueColor + ";");
                Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
                HBox row = new HBox(l, sp, v);
                row.setAlignment(Pos.CENTER_LEFT);
                return row;
            }

            private void showPaymentHistory(WorkerSalary worker) {
                if (rangeFrom == null || rangeTo == null) return;
                LocalDate from = rangeFrom;
                LocalDate to = rangeTo;

                List<SalaryPayment> payments = salaryRepository.loadSalaryPayments(worker.getWorkerId(), from, to);

                Stage ownerStage = null;
                if (tblSalary.getScene() != null && tblSalary.getScene().getWindow() instanceof Stage) {
                    ownerStage = (Stage) tblSalary.getScene().getWindow();
                }

                PaymentHistoryDialogController controller =
                        ViewModel.INSTANCE.getViewsFactory().getForm("form/payment-history-dialog", ownerStage);
                if (controller != null) {
                    controller.setSalaryData(worker, payments, (p) -> {
                        deletePayment(p.getId());
                        List<SalaryPayment> updatedPayments = salaryRepository.loadSalaryPayments(worker.getWorkerId(), from, to);
                        WorkerSalary updatedWorker = salaryList.stream()
                                .filter(w -> w.getWorkerId().equals(worker.getWorkerId()))
                                .findFirst()
                                .orElse(worker);
                        controller.setSalaryData(updatedWorker, updatedPayments, null);
                    });
                }
            }

            private void deletePayment(String paymentId) {
                salaryRepository.deleteSalaryPayment(paymentId);
                String payload = JsonUtil.obj(
                        JsonUtil.field("id", paymentId),
                        JsonUtil.field("op", "delete")
                );                reloadData();
            }
        });

        tblSalary.setItems(salaryList);
        tblSalary.setRowFactory(tv -> {
            TableRow<WorkerSalary> row = new TableRow<>();
            row.setPrefHeight(68);
            row.setStyle("-fx-background-color: white; -fx-border-color: transparent transparent #f3f4f6 transparent;");
            return row;
        });
    }


    private void setupLedgerTable() {

        colLedgerDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDate().toString()));
        colLedgerDate.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v);
                setStyle("-fx-text-fill: #374151; -fx-font-size: 13px; -fx-background-color: transparent; -fx-alignment: center;");
                setAlignment(Pos.CENTER);
            }
        });

        colLedgerWorker.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getWorker()));
        colLedgerWorker.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v);
                setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #111827; -fx-background-color: transparent; -fx-alignment: center;");
                setAlignment(Pos.CENTER);
            }
        });

        colLedgerType.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colLedgerType.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LedgerEntry e, boolean empty) {
                super.updateItem(e, empty);
                if (empty || e == null) { setGraphic(null); return; }
                boolean isSettle = e.getType().equals("SETTLEMENT");
                Label badge = new Label((isSettle ? "✓ " : "+ ") + e.getType());
                badge.setStyle(
                        "-fx-background-color: " + (isSettle ? "#d1fae5" : "#fee2e2") + ";" +
                                "-fx-text-fill: "         + (isSettle ? "#059669" : "#e11d48") + ";" +
                                "-fx-font-size: 11px; -fx-font-weight: bold;" +
                                "-fx-background-radius: 20px; -fx-padding: 4 12 4 12;"
                );
                HBox wrap = new HBox(badge);
                wrap.setAlignment(Pos.CENTER);
                setGraphic(wrap); setText(null);
                setStyle("-fx-background-color: transparent;");
                setAlignment(Pos.CENTER);
            }
        });

        colLedgerNote.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNote()));
        colLedgerNote.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v);
                setStyle("-fx-text-fill: #9ca3af; -fx-font-style: italic; -fx-font-size: 13px; -fx-background-color: transparent; -fx-alignment: center-left;");
                setAlignment(Pos.CENTER_LEFT);
            }
        });

        colLedgerAmount.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colLedgerAmount.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LedgerEntry e, boolean empty) {
                super.updateItem(e, empty);
                if (empty || e == null) { setText(null); return; }
                boolean isSettle = e.getType().equals("SETTLEMENT");
                setText(String.format("%s Rs. %,.0f", isSettle ? "-" : "+", e.getAmount()));
                setStyle(
                        "-fx-font-weight: bold; -fx-font-size: 13px; -fx-background-color: transparent;" +
                                "-fx-text-fill: " + (isSettle ? "#059669" : "#e11d48") + "; -fx-alignment: center;"
                );
                setAlignment(Pos.CENTER);
            }
        });

        colLedgerActions.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colLedgerActions.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LedgerEntry e, boolean empty) {
                super.updateItem(e, empty);
                if (empty || e == null) { setGraphic(null); return; }
                Button del = new Button("🗑");
                del.setStyle("-fx-background-color: transparent; -fx-text-fill: #fca5a5; -fx-font-size: 15px; -fx-cursor: hand;");
                del.setOnMouseEntered(ev -> del.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-font-size: 15px; -fx-cursor: hand;"));
                del.setOnMouseExited(ev  -> del.setStyle("-fx-background-color: transparent; -fx-text-fill: #fca5a5; -fx-font-size: 15px; -fx-cursor: hand;"));
                Button edit = new Button("✎");
                edit.setStyle("-fx-background-color: transparent; -fx-text-fill: #60a5fa; -fx-font-size: 15px; -fx-cursor: hand;");
                edit.setOnAction(ev -> {
                    Stage ownerStage = (Stage) ((Node) ev.getSource()).getScene().getWindow();
                    if (e.getType().equalsIgnoreCase("SETTLEMENT")) {
                        SettleCreditDialogController controller = ViewModel.INSTANCE.getViewsFactory()
                                .getForm("form/settle-credit-dialog", ownerStage);
                        if (controller != null) controller.setEditMode(e);
                    } else {
                        GiveCreditDialogController controller = ViewModel.INSTANCE.getViewsFactory()
                                .getForm("form/give-credit-dialog", ownerStage);
                        if (controller != null) controller.setEditMode(e);
                    }
                });

                del.setOnAction(ev -> {
                    creditRepository.deleteCredit(e.getId());
                    String payload = JsonUtil.obj(
                            JsonUtil.field("id", e.getId()),
                            JsonUtil.field("op", "delete")
                    );                    reloadData();
                });
                HBox actionsBox = new HBox(8, edit, del);
                actionsBox.setAlignment(Pos.CENTER);
                HBox wrap = new HBox(actionsBox);
                wrap.setAlignment(Pos.CENTER);
                setGraphic(wrap); setText(null);
                setStyle("-fx-background-color: transparent;");
                setAlignment(Pos.CENTER);
            }
        });

        tblLedger.setItems(ledgerList);
        tblLedger.setRowFactory(tv -> {
            TableRow<LedgerEntry> row = new TableRow<>();
            row.setPrefHeight(60);
            row.setStyle("-fx-background-color: white; -fx-border-color: transparent transparent #f3f4f6 transparent;");
            return row;
        });
    }

    private void refreshCreditSummary() {
        hboxCreditSummary.getChildren().clear();

        Map<String, double[]> summary = new LinkedHashMap<>();
        for (LedgerEntry e : ledgerList) {
            summary.putIfAbsent(e.getWorker(), new double[]{0, 0});
            if (e.getType().equals("SETTLEMENT")) summary.get(e.getWorker())[1] += e.getAmount();
            else                                  summary.get(e.getWorker())[0] += e.getAmount();
        }

        for (Map.Entry<String, double[]> entry : summary.entrySet()) {
            double given     = entry.getValue()[0];
            double paid      = entry.getValue()[1];
            double remaining = given - paid;

            if (remaining <= 0) continue;

            VBox card = new VBox(8);
            card.setStyle(
                    "-fx-background-color: white; -fx-background-radius: 12px;" +
                            "-fx-border-color: #f3f4f6; -fx-border-width: 1; -fx-border-radius: 12px;" +
                            "-fx-padding: 16; -fx-pref-width: 240;"
            );

            Label nameOwes = new Label(entry.getKey());
            nameOwes.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #111827;");
            Label owesBadge = new Label("Owes");
            owesBadge.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #e11d48; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 3 10 3 10;");
            Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox topRow = new HBox(nameOwes, spacer, owesBadge);
            topRow.setAlignment(Pos.CENTER_LEFT);

            Label givenLbl = new Label(String.format("Given: Rs. %,.0f", given));
            givenLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
            Label paidLbl = new Label(String.format("Paid: Rs. %,.0f", paid));
            paidLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
            HBox givenPaid = new HBox(givenLbl, new Region(), paidLbl);
            HBox.setHgrow(givenPaid.getChildren().get(1), Priority.ALWAYS);

            Label remLabel = new Label("Remaining");
            remLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #e11d48; -fx-font-weight: bold;");
            Label remAmt = new Label(String.format("Rs. %,.0f", remaining));
            remAmt.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #e11d48;");
            Region remSpacer = new Region(); HBox.setHgrow(remSpacer, Priority.ALWAYS);
            HBox remRow = new HBox(remLabel, remSpacer, remAmt);
            remRow.setAlignment(Pos.CENTER_LEFT);

            card.getChildren().addAll(topRow, givenPaid, remRow);
            hboxCreditSummary.getChildren().add(card);
        }
    }


    private void refreshSummary() {
        double gross   = salaryList.stream().mapToDouble(WorkerSalary::getGrossSalary).sum();
        double advances= salaryList.stream().mapToDouble(WorkerSalary::getAdvances).sum();
        double net     = salaryList.stream().mapToDouble(w -> {
            boolean deduct = isDeductCreditSelected(w);
            return w.getNetPayable(deduct);
        }).sum();
        double paid    = salaryList.stream().mapToDouble(WorkerSalary::getPaidAmount).sum();
        double credit  = ledgerList.stream()
                .mapToDouble(e -> e.getType().equals("SETTLEMENT") ? -e.getAmount() : e.getAmount()).sum();

        lblNetPayout.setText(String.format("Rs. %,.0f", net));
        lblGross.setText(String.format("Gross: Rs. %,.0f", gross));
        lblPaidSalary.setText(String.format("Rs. %,.0f", paid));
        lblTotalAdvances.setText(String.format("Rs. %,.0f", advances));
        lblCreditBalance.setText(String.format("Rs. %,.0f", credit));
        lblActiveWorkers.setText(String.valueOf(salaryList.size()));
        lblWorkersSubtitle.setText("Out of " + salaryList.size() + " registered");
    }

    /**
     * Records an inline payment. A null return value means the payment was saved;
     * otherwise the message is displayed in the table rather than in a new window.
     */
    private String paySalary(WorkerSalary worker, String enteredAmount, boolean deductCredit) {
        if (rangeFrom == null || rangeTo == null || rangeFrom.isAfter(rangeTo)) {
            return "Select a valid date range.";
        }
        LocalDate from = rangeFrom;
        LocalDate to = rangeTo;

        double paymentAmount;
        try {
            paymentAmount = Double.parseDouble(enteredAmount.replace(",", "").trim());
        } catch (NumberFormatException ex) {
            return "Enter a valid amount.";
        }
        double remainingPayable = worker.getRemainingPayable(deductCredit);
        if (paymentAmount <= 0 || paymentAmount > remainingPayable + 0.0001) {
            return String.format("Maximum: Rs. %,.2f", remainingPayable);
        }

        try {
            double totalPayable = worker.getNetPayable(deductCredit);
            double creditSettlementAmount = (deductCredit && worker.getCreditBalance() > 0) ? worker.getCreditBalance() : 0.0;
            String paymentId = salaryRepository.paySalary(worker.getWorkerId(), worker.getName(), from, to,
                    paymentAmount, totalPayable, creditSettlementAmount);
            reloadData();
            return null;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ex.getMessage();
        }
    }



    @FXML private void handleDateFilter(ActionEvent e)  {
        reloadData();
    }
    @FXML private void handleRecordAdvance(ActionEvent e){
        Stage ownerStage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        SalaryAdvanceController controller = ViewModel.INSTANCE.getViewsFactory()
                .getForm("form/salary-advance-dialog", ownerStage);
        if (controller != null) {
            controller.setOnSaved(this::reloadData);
        }
    }
    @FXML private void handleGiveCredit(ActionEvent e)  {
        Stage ownerStage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        GiveCreditDialogController controller = ViewModel.INSTANCE.getViewsFactory()
                .getForm("form/give-credit-dialog", ownerStage);
        if (controller != null) {
            controller.setOnSaved(this::reloadData);
        }
    }
    @FXML private void handleSettleCredit(ActionEvent e){
        Stage ownerStage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        SettleCreditDialogController controller = ViewModel.INSTANCE.getViewsFactory()
                .getForm("form/settle-credit-dialog", ownerStage);
        if (controller != null) {
            controller.setOnSaved(this::reloadData);
        }
    }

    @FXML private void handleExportPayroll(ActionEvent e){
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Payroll");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        Stage ownerStage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        java.io.File file = chooser.showSaveDialog(ownerStage);
        if (file == null) return;

        try (java.io.PrintWriter pw = new java.io.PrintWriter(file, "UTF-8")) {
            pw.println("Worker,Role,Present,Late,Absent,Gross,Advances,CreditBalance,NetPayable,Status");
            for (WorkerSalary w : salaryList) {
                pw.printf("%s,%s,%d,%d,%d,%.0f,%.0f,%.0f,%.0f,%s\n",
                        quoteCsv(w.getName()), quoteCsv(w.getRole()), w.getPresent(), w.getLate(), w.getAbsent(),
                        w.getGrossSalary(), w.getAdvances(), w.getCreditBalance(), w.getNetPayable(), quoteCsv(w.getStatus())
                );
            }
        } catch (Exception ex) {
            com.gui.kline.utils.AlertUtil.showError("Export failed", ex.getMessage());
        }
    }

    private String quoteCsv(String v) {
        if (v == null) return "";
        String s = v.replace("\"", "\"\"");
        if (s.contains(",") || s.contains("\n") || s.contains("\"")) return "\"" + s + "\"";
        return s;
    }

    private Label styledBadge(String text, String bg, String fg) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + fg +
                "; -fx-font-size:11px; -fx-font-weight:bold;" +
                " -fx-background-radius:20px; -fx-padding:3 8 3 8;");
        return l;
    }

    private void setupPayoutLedgerTable() {
        if (tblPayoutLedger == null) return;

        colPayoutDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDate().toString()));
        colPayoutDate.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v);
                setStyle("-fx-text-fill: #374151; -fx-font-size: 13px; -fx-background-color: transparent; -fx-alignment: center;");
                setAlignment(Pos.CENTER);
            }
        });

        colPayoutWorker.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getWorker()));
        colPayoutWorker.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v);
                setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #111827; -fx-background-color: transparent; -fx-alignment: center;");
                setAlignment(Pos.CENTER);
            }
        });

        colPayoutType.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colPayoutType.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LedgerEntry e, boolean empty) {
                super.updateItem(e, empty);
                if (empty || e == null) { setGraphic(null); return; }
                boolean isAdvance = "ADVANCE".equalsIgnoreCase(e.getType());
                Label badge = new Label((isAdvance ? "⏱ " : "✓ ") + e.getType());
                badge.getStyleClass().add("badge");
                badge.getStyleClass().add(isAdvance ? "badge-advance" : "badge-payout");
                HBox wrap = new HBox(badge);
                wrap.setAlignment(Pos.CENTER);
                setGraphic(wrap); setText(null);
                setStyle("-fx-background-color: transparent;");
                setAlignment(Pos.CENTER);
            }
        });

        colPayoutNote.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNote()));
        colPayoutNote.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v);
                setStyle("-fx-text-fill: #6b7280; -fx-font-style: italic; -fx-font-size: 13px; -fx-background-color: transparent; -fx-alignment: center-left;");
                setAlignment(Pos.CENTER_LEFT);
            }
        });

        colPayoutAmount.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colPayoutAmount.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LedgerEntry e, boolean empty) {
                super.updateItem(e, empty);
                if (empty || e == null) { setText(null); return; }
                boolean isAdvance = "ADVANCE".equalsIgnoreCase(e.getType());
                setText(String.format("Rs. %,.0f", e.getAmount()));
                setStyle(
                        "-fx-font-weight: bold; -fx-font-size: 13px; -fx-background-color: transparent;" +
                                "-fx-text-fill: " + (isAdvance ? "#d97706" : "#059669") + "; -fx-alignment: center;"
                );
                setAlignment(Pos.CENTER);
            }
        });

        colPayoutActions.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colPayoutActions.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LedgerEntry e, boolean empty) {
                super.updateItem(e, empty);
                if (empty || e == null) { setGraphic(null); return; }
                Button del = new Button("🗑");
                del.setStyle("-fx-background-color: transparent; -fx-text-fill: #fca5a5; -fx-font-size: 15px; -fx-cursor: hand;");
                del.setOnMouseEntered(ev -> del.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-font-size: 15px; -fx-cursor: hand;"));
                del.setOnMouseExited(ev  -> del.setStyle("-fx-background-color: transparent; -fx-text-fill: #fca5a5; -fx-font-size: 15px; -fx-cursor: hand;"));

                del.setOnAction(ev -> {
                    if ("ADVANCE".equalsIgnoreCase(e.getType())) {
                        advanceRepository.deleteAdvance(e.getId());
                    } else {
                        salaryRepository.deleteSalaryPayment(e.getId());
                    }
                    reloadData();
                });

                HBox actionsBox = new HBox(del);
                actionsBox.setAlignment(Pos.CENTER);
                setGraphic(actionsBox); setText(null);
                setStyle("-fx-background-color: transparent;");
                setAlignment(Pos.CENTER);
            }
        });

        tblPayoutLedger.setItems(payoutLedgerList);
        tblPayoutLedger.setRowFactory(tv -> {
            TableRow<LedgerEntry> row = new TableRow<>();
            row.setPrefHeight(50);
            row.setStyle("-fx-background-color: white; -fx-border-color: transparent transparent #f3f4f6 transparent;");
            return row;
        });
    }

    private void refreshPayoutSummary() {
        if (hboxPayoutSummary == null) return;
        hboxPayoutSummary.getChildren().clear();

        Map<String, double[]> summary = new LinkedHashMap<>();
        for (LedgerEntry e : payoutLedgerList) {
            summary.putIfAbsent(e.getWorker(), new double[]{0, 0});
            if ("ADVANCE".equalsIgnoreCase(e.getType())) {
                summary.get(e.getWorker())[0] += e.getAmount();
            } else {
                summary.get(e.getWorker())[1] += e.getAmount();
            }
        }

        if (summary.isEmpty()) {
            Label emptyLbl = new Label("No payout or advance transactions recorded for this period.");
            emptyLbl.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 13px; -fx-font-style: italic;");
            hboxPayoutSummary.getChildren().add(emptyLbl);
            return;
        }

        for (Map.Entry<String, double[]> entry : summary.entrySet()) {
            double advances = entry.getValue()[0];
            double payouts  = entry.getValue()[1];
            double total    = advances + payouts;

            VBox card = new VBox(6);
            card.setStyle(
                    "-fx-background-color: white; -fx-background-radius: 12px;" +
                            "-fx-border-color: #f3f4f6; -fx-border-width: 1; -fx-border-radius: 12px;" +
                            "-fx-padding: 14 18 14 18; -fx-min-width: 210px;"
            );

            Label name = new Label(entry.getKey());
            name.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #111827;");

            Label advancesLbl = new Label(String.format("Advances: Rs. %,.0f", advances));
            advancesLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #d97706;");

            Label payoutsLbl = new Label(String.format("Salary Paid: Rs. %,.0f", payouts));
            payoutsLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #059669;");

            Label totalLbl = new Label(String.format("Total Handed Out: Rs. %,.0f", total));
            totalLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #111827;");

            card.getChildren().addAll(name, advancesLbl, payoutsLbl, totalLbl);
            hboxPayoutSummary.getChildren().add(card);
        }
    }

    private double computeWorkerCosts(LocalDate from, LocalDate to) {
        return 0.0;
    }

}
