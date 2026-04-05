package com.gui.kline.controller;

import com.gui.kline.models.ExportRecord;
import com.gui.kline.models.ViewModel;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class TyreExportsController implements Initializable {

    @FXML private Button    btnNewExport;
    @FXML private TextField txtFilter;
    @FXML private DatePicker dpFrom, dpTo;

    @FXML private TableView<ExportRecord>                tblExports;
    @FXML private TableColumn<ExportRecord, String>      colCompany;
    @FXML private TableColumn<ExportRecord, Integer>     colTyres;
    @FXML private TableColumn<ExportRecord, String>      colPrices;
    @FXML private TableColumn<ExportRecord, Double>      colService;
    @FXML private TableColumn<ExportRecord, String>      colProfit;
    @FXML private TableColumn<ExportRecord, String>      colStatus;
    @FXML private TableColumn<ExportRecord, ExportRecord> colActions;

    private final ObservableList<ExportRecord> masterList   = FXCollections.observableArrayList();
    private FilteredList<ExportRecord>         filteredList;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        setupFilteredList();
        loadSampleData();
    }

    private void setupColumns() {

        colCompany.setCellValueFactory(new PropertyValueFactory<>("company"));

        colTyres.setCellValueFactory(new PropertyValueFactory<>("tyres"));
        colTyres.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setGraphic(null); return; }
                Label lbl = new Label("⬡ " + v);
                lbl.setStyle("-fx-text-fill: #374151; -fx-font-weight: bold;");
                setGraphic(lbl); setText(null);
            }
        });

        colPrices.setCellValueFactory(new PropertyValueFactory<>("pricesDisplay"));
        colPrices.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setGraphic(null); return; }
                String[] parts = v.split("\\|");
                Label c = new Label(parts[0]);
                Label p = new Label(parts.length > 1 ? parts[1] : "");
                c.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold; -fx-font-size: 12px;");
                p.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");
                javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(2, c, p);
                setGraphic(box); setText(null);
            }
        });

        colService.setCellValueFactory(new PropertyValueFactory<>("serviceCharge"));
        colService.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                setText(String.format("Rs. %,.0f", v));
                setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
            }
        });

        colProfit.setCellValueFactory(new PropertyValueFactory<>("profitDisplay"));
        colProfit.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setGraphic(null); return; }
                String[] parts = v.split("\\|");
                Label amt  = new Label(parts[0]);
                Label date = new Label(parts.length > 1 ? parts[1] : "");
                amt.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold; -fx-font-size: 13px;");
                date.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11px;");
                javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(2, amt, date);
                setGraphic(box); setText(null);
            }
        });

        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setGraphic(null); return; }
                Label badge = new Label(v);
                String style = switch (v) {
                    case "DELIVERED"    -> "-fx-background-color: #d1fae5; -fx-text-fill: #065f46;";
                    case "IN TRANSPORT" -> "-fx-background-color: #dbeafe; -fx-text-fill: #1e40af;";
                    case "PAID"         -> "-fx-background-color: #ede9fe; -fx-text-fill: #5b21b6;";
                    case "PENDING"      -> "-fx-background-color: #fef9c3; -fx-text-fill: #92400e;";
                    default             -> "-fx-background-color: #f3f4f6; -fx-text-fill: #374151;";
                };
                badge.setStyle(style + " -fx-background-radius: 20px; -fx-padding: 4 12 4 12;"
                        + " -fx-font-size: 11px; -fx-font-weight: bold;");
                HBox wrap = new HBox(badge);
                wrap.setAlignment(Pos.CENTER);
                setGraphic(wrap); setText(null);
            }
        });

        colActions.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue()));
        colActions.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(ExportRecord record, boolean empty) {
                super.updateItem(record, empty);
                if (empty || record == null) { setGraphic(null); return; }

                String actionLabel = switch (record.getStatus()) {
                    case "PENDING"      -> "Mark as Transport";
                    case "IN TRANSPORT" -> "Mark as Delivered";
                    case "DELIVERED"    -> "Mark as Paid";
                    case "PAID"         -> "✔";
                    default             -> "-";
                };

                if (actionLabel.equals("✔")) {
                    Label done = new Label("✔");
                    done.setStyle("-fx-text-fill: #10b981; -fx-font-size: 16px;");
                    HBox wrap = new HBox(done);
                    wrap.setAlignment(Pos.CENTER);
                    setGraphic(wrap);
                } else {
                    Button btn = new Button(actionLabel);
                    btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #4f46e5;"
                            + " -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 12px;");
                    btn.setOnAction(e -> handleStatusAdvance(record));
                    HBox wrap = new HBox(btn);
                    wrap.setAlignment(Pos.CENTER);
                    setGraphic(wrap);
                }
                setText(null);
            }
        });
    }

    private void setupFilteredList() {
        filteredList = new FilteredList<>(masterList, p -> true);
        tblExports.setItems(filteredList);
    }

    private void loadSampleData() {
        masterList.addAll(
                new ExportRecord("Lanka Tyre Traders",  20, 520000, 440000, 10000, LocalDate.of(2026,3,29), "DELIVERED"),
                new ExportRecord("Colombo Auto Parts",  12, 336000, 288000,  6000, LocalDate.of(2026,3,28), "IN TRANSPORT"),
                new ExportRecord("Southern Motors",      8, 200000, 168000,  4000, LocalDate.of(2026,3,27), "PAID"),
                new ExportRecord("Kandy Wheels Hub",    15, 412500, 345000,  7500, LocalDate.of(2026,3,29), "PENDING")
        );
    }

    @FXML
    private void handleNewExport(ActionEvent event) {
        Stage ownerStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        ViewModel.INSTANCE.getViewsFactory().getForm("form/new-export-dialog", ownerStage);
    }

    @FXML
    private void handleFilter(KeyEvent event) {
        applyFilters();
    }

    @FXML
    private void handleDateFilter(ActionEvent event) {
        applyFilters();
    }

    private void applyFilters() {
        String keyword = txtFilter.getText().toLowerCase().trim();
        LocalDate from = dpFrom.getValue();
        LocalDate to   = dpTo.getValue();

        filteredList.setPredicate(record -> {
            boolean matchText = keyword.isEmpty()
                    || record.getCompany().toLowerCase().contains(keyword);
            boolean matchDate = (from == null || !record.getDate().isBefore(from))
                    && (to == null || !record.getDate().isAfter(to));
            return matchText && matchDate;
        });
    }

    private void handleStatusAdvance(ExportRecord record) {
        String next = switch (record.getStatus()) {
            case "PENDING"      -> "IN TRANSPORT";
            case "IN TRANSPORT" -> "DELIVERED";
            case "DELIVERED"    -> "PAID";
            default             -> record.getStatus();
        };
        record.setStatus(next);
        tblExports.refresh();
    }
}