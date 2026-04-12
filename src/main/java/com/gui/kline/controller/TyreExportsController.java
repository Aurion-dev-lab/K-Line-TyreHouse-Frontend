package com.gui.kline.controller;

import com.gui.kline.models.ExportRecord;
import com.gui.kline.models.ViewModel;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class TyreExportsController implements Initializable {

    @FXML private TextField  txtFilter;
    @FXML private DatePicker dpFrom;
    @FXML private DatePicker dpTo;
    @FXML private VBox       cardContainer;

    @FXML private Label lblShipments;
    @FXML private Label lblTyres;
    @FXML private Label lblProfit;
    @FXML private Label lblPending;

    private final ObservableList<ExportRecord> masterList   = FXCollections.observableArrayList();
    private       FilteredList<ExportRecord>   filteredList;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        filteredList = new FilteredList<>(masterList, p -> true);
        filteredList.addListener((ListChangeListener<ExportRecord>) c -> {
            rebuildCards();
            refreshStats();
        });
        loadSampleData();
    }

    private void loadSampleData() {
        masterList.addAll(
                new ExportRecord("Lanka Tyre Traders",  20, 520000, 440000, 10000, LocalDate.of(2026, 3, 29), "DELIVERED"),
                new ExportRecord("Colombo Auto Parts",  12, 336000, 288000,  6000, LocalDate.of(2026, 3, 28), "IN TRANSPORT"),
                new ExportRecord("Southern Motors",      8, 200000, 168000,  4000, LocalDate.of(2026, 3, 27), "PAID"),
                new ExportRecord("Kandy Wheels Hub",    15, 412500, 345000,  7500, LocalDate.of(2026, 3, 29), "PENDING")
        );
    }

    @FXML
    private void handleNewExport(ActionEvent e) {
        Stage owner = (Stage) ((Node) e.getSource()).getScene().getWindow();
        ViewModel.INSTANCE.getViewsFactory().getForm("form/new-export-dialog", owner);
    }

    @FXML
    private void handleFilter() { applyFilters(); }

    @FXML
    private void handleDateFilter() { applyFilters(); }

    private void applyFilters() {
        String    keyword = txtFilter.getText().toLowerCase().trim();
        LocalDate from    = dpFrom.getValue();
        LocalDate to      = dpTo.getValue();

        filteredList.setPredicate(r -> {
            boolean matchText = keyword.isEmpty() || r.getCompany().toLowerCase().contains(keyword);
            boolean matchDate = (from == null || !r.getDate().isBefore(from))
                    && (to   == null || !r.getDate().isAfter(to));
            return matchText && matchDate;
        });
    }

    private void refreshStats() {
        int    shipments = filteredList.size();
        int    tyres     = filteredList.stream().mapToInt(ExportRecord::getTyres).sum();
        double profit    = filteredList.stream().mapToDouble(this::calcProfit).sum();
        long   pending   = filteredList.stream().filter(r -> "PENDING".equals(r.getStatus())).count();

        lblShipments.setText(String.valueOf(shipments));
        lblTyres.setText(String.valueOf(tyres));
        lblProfit.setText(String.format("Rs. %,.0f", profit));
        lblPending.setText(String.valueOf(pending));
    }

    private double calcProfit(ExportRecord r) {
        return (r.getCustPrice() - r.getCompPrice()) * r.getTyres() + r.getServiceCharge();
    }

    private void rebuildCards() {
        cardContainer.getChildren().clear();
        filteredList.forEach(r -> cardContainer.getChildren().add(buildCard(r)));
    }

    private HBox buildCard(ExportRecord r) {
        Label avatar = new Label(initials(r.getCompany()));
        avatar.setPrefSize(44, 44);
        avatar.setMinSize(44, 44);
        avatar.setMaxSize(44, 44);
        avatar.setAlignment(Pos.CENTER);
        avatar.setStyle(
                "-fx-background-color: #EEF2FF; -fx-background-radius: 22;" +
                        "-fx-text-fill: #4F46E5; -fx-font-size: 14px; -fx-font-weight: bold;"
        );

        Label name = new Label(r.getCompany());
        name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label tyresLbl   = chip(r.getTyres() + " tyres");
        Label custLbl    = chip("Cust  Rs. " + String.format("%,.0f", r.getCustPrice()));
        Label compLbl    = chip("Comp  Rs. " + String.format("%,.0f", r.getCompPrice()));
        Label serviceLbl = new Label("Service  Rs. " + String.format("%,.0f", r.getServiceCharge()));
        serviceLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #D97706;");
        Label dateLbl = new Label(r.getDate().format(DATE_FMT));
        dateLbl.setStyle(
                "-fx-font-size: 11px; -fx-text-fill: #6B7280;" +
                        "-fx-background-color: #F3F4F6; -fx-background-radius: 20;" +
                        "-fx-padding: 2 8;"
        );

        HBox sep1 = separator();
        HBox sep2 = separator();

        HBox meta = new HBox(8, tyresLbl, sep1, custLbl, compLbl, sep2, serviceLbl, dateLbl);
        meta.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(5, name, meta);
        info.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(info, Priority.ALWAYS);

        double profit = calcProfit(r);
        Label profitAmt = new Label(String.format("Rs. %,.0f", profit));
        profitAmt.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #059669;");
        Label profitLbl = new Label("net profit");
        profitLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");
        VBox profitBox = new VBox(2, profitAmt, profitLbl);
        profitBox.setAlignment(Pos.CENTER_RIGHT);

        Label badge = new Label(r.getStatus());
        badge.setStyle(statusStyle(r.getStatus()));

        Node actionNode = buildActionNode(r);

        VBox rightCol = new VBox(6, profitBox, badge, actionNode);
        rightCol.setAlignment(Pos.CENTER_RIGHT);
        rightCol.setMinWidth(160);

        HBox card = new HBox(14, avatar, info, rightCol);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(14, 18, 14, 18));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #E5E7EB;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 12;"
        );
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle()
                .replace("#E5E7EB", "#D1D5DB")));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle()
                .replace("#D1D5DB", "#E5E7EB")));

        return card;
    }

    private Node buildActionNode(ExportRecord r) {
        String label = switch (r.getStatus()) {
            case "PENDING"      -> "Mark as transport";
            case "IN TRANSPORT" -> "Mark as delivered";
            case "DELIVERED"    -> "Mark as paid";
            default             -> null;
        };

        if (label == null) {
            Label done = new Label("✔  Done");
            done.setStyle("-fx-font-size: 13px; -fx-text-fill: #10B981;");
            return done;
        }

        Button btn = new Button(label);
        btn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: #E5E7EB; -fx-border-width: 1;" +
                        "-fx-border-radius: 8; -fx-background-radius: 8;" +
                        "-fx-font-size: 12px; -fx-text-fill: #4F46E5;" +
                        "-fx-font-weight: bold; -fx-padding: 5 10; -fx-cursor: hand;"
        );
        btn.setOnAction(e -> advanceStatus(r));
        return btn;
    }

    private void advanceStatus(ExportRecord r) {
        String next = switch (r.getStatus()) {
            case "PENDING"      -> "IN TRANSPORT";
            case "IN TRANSPORT" -> "DELIVERED";
            case "DELIVERED"    -> "PAID";
            default             -> r.getStatus();
        };
        r.setStatus(next);
        rebuildCards();
        refreshStats();
    }

    private String initials(String name) {
        String[] words = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) if (!w.isEmpty()) sb.append(w.charAt(0));
        return sb.toString().toUpperCase().substring(0, Math.min(2, sb.length()));
    }

    private Label chip(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151;");
        return l;
    }

    private HBox separator() {
        Label sep = new Label("|");
        sep.setStyle("-fx-text-fill: #D1D5DB; -fx-font-size: 13px;");
        HBox box = new HBox(sep);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private String statusStyle(String status) {
        String colors = switch (status) {
            case "DELIVERED"    -> "-fx-background-color: #D1FAE5; -fx-text-fill: #065F46;";
            case "IN TRANSPORT" -> "-fx-background-color: #DBEAFE; -fx-text-fill: #1E40AF;";
            case "PAID"         -> "-fx-background-color: #EDE9FE; -fx-text-fill: #5B21B6;";
            case "PENDING"      -> "-fx-background-color: #FEF9C3; -fx-text-fill: #92400E;";
            default             -> "-fx-background-color: #F3F4F6; -fx-text-fill: #374151;";
        };
        return colors + " -fx-background-radius: 20; -fx-padding: 3 10;" +
                " -fx-font-size: 11px; -fx-font-weight: bold;";
    }
}