package com.gui.kline.controller;


import com.gui.kline.models.LedgerEntry;
import com.gui.kline.models.ViewModel;
import com.gui.kline.models.WorkerSalary;
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

import java.net.URL;
import java.time.LocalDate;
import java.util.*;

public class SalaryController implements Initializable {

    @FXML private DatePicker dpFrom, dpTo;
    @FXML private Button btnRecordAdvance, btnGiveCredit, btnSettleCredit, btnExportPayroll;

    @FXML private Label lblNetPayout, lblGross;
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

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dpFrom.setValue(LocalDate.now().minusMonths(1));
        dpTo.setValue(LocalDate.now());

        setupSalaryTable();
        setupLedgerTable();
        loadSampleData();
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
                Label badge = new Label(v);
                badge.setStyle(
                        "-fx-background-color: #d1fae5; -fx-text-fill: #065f46;" +
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
                del.setOnAction(ev -> {
                    ledgerList.remove(e);
                    refreshSummary();
                    refreshCreditSummary();
                });
                HBox wrap = new HBox(del);
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
        double credit  = ledgerList.stream()
                .mapToDouble(e -> e.getType().equals("SETTLEMENT") ? -e.getAmount() : e.getAmount()).sum();

        lblNetPayout.setText(String.format("Rs. %,.0f", net));
        lblGross.setText(String.format("Gross: Rs. %,.0f", gross));
        lblTotalAdvances.setText(String.format("Rs. %,.0f", advances));
        lblCreditBalance.setText(String.format("Rs. %,.0f", credit));
        lblActiveWorkers.setText(String.valueOf(salaryList.size()));
        lblWorkersSubtitle.setText("Out of " + salaryList.size() + " registered");
    }


    private void loadSampleData() {
        salaryList.addAll(
                new WorkerSalary("Kasun Perera", "SENIOR MECHANIC", "#10b981", 2, 0, 0, 5000, 0, 5000, "Payable"),
                new WorkerSalary("Nuwan Silva",  "TYRE SPECIALIST",  "#3b82f6", 1, 1, 0, 3300, 0, 5000, "Payable")
        );
        ledgerList.addAll(
                new LedgerEntry(LocalDate.of(2026,3,28), "Nuwan Silva",  "SETTLEMENT", "Partial settlement from work", 3000),
                new LedgerEntry(LocalDate.of(2026,3,27), "Nuwan Silva",  "CREDIT GIVEN","Tyre set on credit",          8000),
                new LedgerEntry(LocalDate.of(2026,3,28), "Kasun Perera", "CREDIT GIVEN","Spare parts for personal vehicle", 5000)
        );
    }


    @FXML private void handleDateFilter(ActionEvent e)  {  }
    @FXML private void handleRecordAdvance(ActionEvent e){
        Stage ownerStage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        ViewModel.INSTANCE.getViewsFactory().getForm("form/salary-advance-dialog", ownerStage);
    }
    @FXML private void handleGiveCredit(ActionEvent e)  {
        Stage ownerStage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        ViewModel.INSTANCE.getViewsFactory().getForm("form/give-credit-dialog", ownerStage);
    }
    @FXML private void handleSettleCredit(ActionEvent e){
        Stage ownerStage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        ViewModel.INSTANCE.getViewsFactory().getForm("form/settle-credit-dialog", ownerStage);
    }
    @FXML private void handleExportPayroll(ActionEvent e){ }

    private Label styledBadge(String text, String bg, String fg) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + fg +
                "; -fx-font-size:11px; -fx-font-weight:bold;" +
                " -fx-background-radius:20px; -fx-padding:3 8 3 8;");
        return l;
    }

}