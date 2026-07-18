package com.gui.kline.controller;


import com.gui.kline.models.LedgerEntry;
import com.gui.kline.models.ViewModel;
import com.gui.kline.models.WorkerSalary;
import com.gui.kline.data.LocalSalaryRepository;
import com.gui.kline.data.LocalWorkerCreditRepository;
import com.gui.kline.utils.JsonUtil;
import com.gui.kline.controller.form.GiveCreditDialogController;
import com.gui.kline.controller.form.SalaryAdvanceController;
import com.gui.kline.controller.form.SettleCreditDialogController;
import com.gui.kline.data.SyncQueueRepository;
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
    @FXML private TableColumn<WorkerSalary, Double>          colNetPayable;
    @FXML private TableColumn<WorkerSalary, String>          colStatus;
    @FXML private TableColumn<WorkerSalary, WorkerSalary>    colSalaryActions;

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
    private final LocalSalaryRepository salaryRepository = new LocalSalaryRepository();
    private final LocalWorkerCreditRepository creditRepository = new LocalWorkerCreditRepository();
    private final SyncQueueRepository syncQueueRepository = new SyncQueueRepository();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        YearMonth currentMonth = YearMonth.now();
        setupMonthComboBox(currentMonth);
        
        // Set initial date range based on current month
        rangeFrom = currentMonth.atDay(1);
        rangeTo = currentMonth.atEndOfMonth();

        setupSalaryTable();
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
        salaryList.setAll(salaryRepository.loadWorkerSalaries(rangeFrom, rangeTo));
        ledgerList.setAll(creditRepository.loadLedger(rangeFrom, rangeTo));
        refreshSummary();
        refreshCreditSummary();
    }

    private void setupSalaryTable() {
        colWorker.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colWorker.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(WorkerSalary w, boolean empty) {
                super.updateItem(w, empty);
                if (empty || w == null) { setGraphic(null); return; }

                Label avatar = new Label(String.valueOf(w.getName().charAt(0)));
                avatar.setStyle(
                        "-fx-background-color: " + w.getAvatarColor() + ";" +
                                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;" +
                                "-fx-background-radius: 50%; -fx-min-width: 36px; -fx-min-height: 36px;" +
                                "-fx-alignment: center;"
                );

                Label name = new Label(w.getName());
                name.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #111827;");
                Label role = new Label(w.getRole());
                role.setStyle("-fx-font-size: 10px; -fx-text-fill: #9ca3af;");

                VBox info = new VBox(2, name, role);
                HBox box  = new HBox(10, avatar, info);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box); setText(null);
                setStyle("-fx-background-color: transparent; -fx-padding: 6 0 6 8;");
                setAlignment(Pos.CENTER_LEFT);
            }
        });

        colAttendance.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colAttendance.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(WorkerSalary w, boolean empty) {
                super.updateItem(w, empty);
                if (empty || w == null) { setGraphic(null); return; }

                Label present = styledBadge("✓ " + w.getPresent(), "#d1fae5", "#059669");
                Label late    = styledBadge("⏱ " + w.getLate(),    "#fef3c7", "#f59e0b");
                Label absent  = styledBadge("✗ " + w.getAbsent(),  "#fee2e2", "#ef4444");

                HBox box = new HBox(6, present, late, absent);
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

        colNetPayable.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getNetPayable()));
        colNetPayable.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                setText(String.format("Rs. %,.0f", v));
                setStyle("-fx-font-size: 13px; -fx-text-fill: #059669; -fx-font-weight: bold; -fx-background-color: transparent; -fx-alignment: center;");
                setAlignment(Pos.CENTER);
            }
        });

        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setGraphic(null); return; }
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

                boolean canPay = worker.getRemainingPayable() > 0 &&
                        !"NO DATA".equalsIgnoreCase(worker.getStatus()) &&
                        !"NO PAYABLE".equalsIgnoreCase(worker.getStatus());
                boolean hasPayments = worker.getPaidAmount() > 0;

                HBox wrap = new HBox(8);
                wrap.setAlignment(Pos.CENTER);

                if (hasPayments) {
                    // Show delete button if there are payments
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
                TextField amount = new TextField(String.format("%.2f", worker.getRemainingPayable()));
                amount.setPromptText("Amount");
                amount.setPrefWidth(88);
                amount.setStyle("-fx-font-size: 11px; -fx-background-radius: 7px; -fx-border-color: #9ca3af; -fx-border-radius: 7px;");

                Button save = new Button("✓");
                save.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 7px; -fx-cursor: hand;");
                Button cancel = new Button("✕");
                cancel.setStyle("-fx-background-color: #e5e7eb; -fx-text-fill: #374151; -fx-font-weight: bold; -fx-background-radius: 7px; -fx-cursor: hand;");

                Label error = new Label();
                error.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 9px;");
                HBox editor = new HBox(4, amount, save, cancel);
                editor.setAlignment(Pos.CENTER);
                VBox content = new VBox(2, editor, error);
                content.setAlignment(Pos.CENTER);
                setGraphic(content);

                save.setOnAction(event -> {
                    String errorMessage = paySalary(worker, amount.getText());
                    if (errorMessage != null) {
                        error.setText(errorMessage);
                    }
                });
                amount.setOnAction(event -> save.fire());
                cancel.setOnAction(event -> updateItem(worker, false));
                amount.requestFocus();
                amount.selectAll();
            }

            private void showPaymentHistory(WorkerSalary worker) {
                if (rangeFrom == null || rangeTo == null) return;
                LocalDate from = rangeFrom;
                LocalDate to = rangeTo;

                List<LocalSalaryRepository.SalaryPayment> payments = salaryRepository.loadSalaryPayments(worker.getWorkerId(), from, to);
                if (payments.isEmpty()) {
                    AlertUtil.showInfo("No Payments", "No payments found for " + worker.getName() + " in this period.");
                    return;
                }

                // Create dialog to show payment history
                Dialog<Void> dialog = new Dialog<>();
                dialog.setTitle("Payment History - " + worker.getName());
                dialog.setHeaderText(null);

                // Set owner window
                javafx.stage.Window owner = null;
                if (tblSalary.getScene() != null) {
                    owner = tblSalary.getScene().getWindow();
                }
                if (owner != null) {
                    dialog.initOwner(owner);
                    dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
                }

                VBox content = new VBox(8);
                content.setStyle("-fx-padding: 16;");

                for (LocalSalaryRepository.SalaryPayment p : payments) {
                    HBox row = new HBox(10);
                    row.setAlignment(Pos.CENTER_LEFT);

                    Label dateLbl = new Label(p.getPaidAt().toLocalDate().toString());
                    dateLbl.setPrefWidth(100);
                    Label amountLbl = new Label(String.format("Rs. %,.0f", p.getAmount()));
                    amountLbl.setPrefWidth(100);
                    HBox.setHgrow(amountLbl, Priority.ALWAYS);

                    Button del = new Button("🗑");
                    del.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-font-size: 14px; -fx-cursor: hand;");
                    del.setOnAction(ev -> {
                        if (confirmDeletePayment(p)) {
                            deletePayment(p.getId());
                            dialog.close();
                        }
                    });

                    row.getChildren().addAll(dateLbl, amountLbl, del);
                    content.getChildren().add(row);
                }

                dialog.getDialogPane().setContent(content);
                dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
                dialog.showAndWait();
            }

            private boolean confirmDeletePayment(LocalSalaryRepository.SalaryPayment payment) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Delete Payment");
                confirm.setHeaderText(null);
                confirm.setContentText("Delete payment of Rs. " + String.format("%,.0f", payment.getAmount()) + " made on " + payment.getPaidAt().toLocalDate() + "?");

                javafx.stage.Window owner = null;
                if (tblSalary.getScene() != null) {
                    owner = tblSalary.getScene().getWindow();
                }
                if (owner != null) {
                    confirm.initOwner(owner);
                    confirm.initModality(javafx.stage.Modality.WINDOW_MODAL);
                }

                return confirm.showAndWait().filter(btn -> btn == ButtonType.OK).isPresent();
            }

            private void deletePayment(String paymentId) {
                salaryRepository.deleteSalaryPayment(paymentId);
                String payload = JsonUtil.obj(
                        JsonUtil.field("id", paymentId),
                        JsonUtil.field("op", "delete")
                );
                syncQueueRepository.enqueue("salary_payment", payload);
                reloadData();
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
                    );
                    syncQueueRepository.enqueue("worker_credit", payload);
                    reloadData();
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
        double net     = salaryList.stream().mapToDouble(WorkerSalary::getNetPayable).sum();
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
    private String paySalary(WorkerSalary worker, String enteredAmount) {
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
        if (paymentAmount <= 0 || paymentAmount > worker.getRemainingPayable() + 0.0001) {
            return String.format("Maximum: Rs. %,.2f", worker.getRemainingPayable());
        }

        try {
            String paymentId = salaryRepository.paySalary(worker.getWorkerId(), worker.getName(), from, to,
                    paymentAmount, worker.getNetPayable());
            syncQueueRepository.enqueue("salary_payment", JsonUtil.obj(
                    JsonUtil.field("id", paymentId),
                    JsonUtil.field("workerId", worker.getWorkerId()),
                    JsonUtil.field("worker", worker.getName()),
                    JsonUtil.field("periodFrom", from.toString()),
                    JsonUtil.field("periodTo", to.toString()),
                    JsonUtil.field("amount", paymentAmount),
                    JsonUtil.field("status", paymentAmount >= worker.getRemainingPayable() - 0.0001 ? "PAID" : "PARTIALLY PAID"),
                    JsonUtil.field("op", "create")
            ));
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

    private double computeWorkerCosts(LocalDate from, LocalDate to) {
        return 0.0;
    }

}
