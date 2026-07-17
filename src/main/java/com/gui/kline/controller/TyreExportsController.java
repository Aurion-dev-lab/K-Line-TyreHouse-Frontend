package com.gui.kline.controller;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import com.gui.kline.controller.form.NewExportDialogController;
import com.gui.kline.data.SyncQueueReader;
import com.gui.kline.data.SyncQueueRepository;
import com.gui.kline.models.ExportRecord;
import com.gui.kline.models.ViewModel;
import com.gui.kline.utils.JsonUtil;

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
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

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
    private final SyncQueueRepository syncQueueRepository = new SyncQueueRepository();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        filteredList = new FilteredList<>(masterList, p -> true);
        filteredList.addListener((ListChangeListener<ExportRecord>) c -> {
            rebuildCards();
            refreshStats();
        });
        loadFromLocal();
    }

    private void loadFromLocal() {
        SyncQueueReader reader = new SyncQueueReader();
        List<ExportRecord> local = reader.loadTyreExports();
        masterList.setAll(local);
    }

    @FXML
    private void handleNewExport(ActionEvent e) {
        Stage owner = (Stage) ((Node) e.getSource()).getScene().getWindow();
        NewExportDialogController form =
                ViewModel.INSTANCE.getViewsFactory().getForm("form/new-export-dialog", owner);
        if (form == null) {
            return;
        }
        form.setOnSave(result -> {
            ExportRecord record = new ExportRecord(
                result.exportId(),
                    result.company(),
                    result.tyres(),
                    result.custPrice(),
                    result.compPrice(),
                    result.serviceFee(),
                result.totalAmount(),
                result.paidAmount(),
                result.balanceAmount(),
                result.paymentStatus(),
                    result.date(),
                    result.status()
            );
            masterList.add(0, record);
            enqueueExport(record);
        });
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
            boolean matchText = keyword.isEmpty()
                    || r.getCompany().toLowerCase().contains(keyword)
                    || r.getStatus().toLowerCase().contains(keyword)
                    || r.getPaymentStatus().toLowerCase().contains(keyword);
            boolean matchDate = (from == null || !r.getDate().isBefore(from))
                    && (to   == null || !r.getDate().isAfter(to));
            return matchText && matchDate;
        });
    }

    private void refreshStats() {
        int    shipments = filteredList.size();
        int    tyres     = filteredList.stream().mapToInt(ExportRecord::getTyres).sum();
        double profit    = filteredList.stream().mapToDouble(this::calcProfit).sum();
        long   pending   = filteredList.stream().filter(r -> r.getBalanceAmount() > 0).count();

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

        Label exportId = chip(r.getExportId().isBlank() ? "draft export" : r.getExportId());

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

        HBox meta = new HBox(8, exportId, tyresLbl, sep1, custLbl, compLbl, sep2, serviceLbl, dateLbl);
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

        Label shipmentBadge = new Label(r.getStatus());
        shipmentBadge.setStyle(statusStyle(r.getStatus()));

        Label paymentBadge = new Label(r.getPaymentStatus() + "  " + (r.getBalanceAmount() > 0 ? "Due Rs. " + String.format("%,.0f", r.getBalanceAmount()) : "Settled"));
        paymentBadge.setStyle(paymentStatusStyle(r.getPaymentStatus(), r.getBalanceAmount()));

        Node actionNode = buildActionNode(r);

        VBox rightCol = new VBox(6, profitBox, shipmentBadge, paymentBadge, actionNode);
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
        String shipmentLabel = switch (r.getStatus()) {
            case "PENDING"      -> "Mark as transport";
            case "IN TRANSPORT" -> "Mark as delivered";
            case "DELIVERED"    -> r.getBalanceAmount() > 0 ? null : "Mark as paid";
            default             -> null;
        };

        VBox box = new VBox(6);
        box.setAlignment(Pos.CENTER_RIGHT);

        if (shipmentLabel != null) {
            Button btn = new Button(shipmentLabel);
            btn.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-border-color: #E5E7EB; -fx-border-width: 1;" +
                            "-fx-border-radius: 8; -fx-background-radius: 8;" +
                            "-fx-font-size: 12px; -fx-text-fill: #4F46E5;" +
                            "-fx-font-weight: bold; -fx-padding: 5 10; -fx-cursor: hand;"
            );
            btn.setOnAction(e -> advanceStatus(r));
            box.getChildren().add(btn);
        }

        if (r.getBalanceAmount() > 0) {
            Button payBtn = new Button("Receive payment");
            payBtn.setStyle(
                    "-fx-background-color: #ecfeff;" +
                            "-fx-border-color: #22c55e; -fx-border-width: 1;" +
                            "-fx-border-radius: 8; -fx-background-radius: 8;" +
                            "-fx-font-size: 12px; -fx-text-fill: #166534;" +
                            "-fx-font-weight: bold; -fx-padding: 5 10; -fx-cursor: hand;"
            );
            payBtn.setOnAction(e -> collectPayment(r));
            box.getChildren().add(payBtn);
        }

        // Edit and Delete buttons
        HBox editDeleteBox = new HBox(6);
        editDeleteBox.setAlignment(Pos.CENTER_RIGHT);

        Button editBtn = new Button("Edit");
        editBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: #E5E7EB; -fx-border-width: 1;" +
                        "-fx-border-radius: 8; -fx-background-radius: 8;" +
                        "-fx-font-size: 12px; -fx-text-fill: #F59E0B;" +
                        "-fx-font-weight: bold; -fx-padding: 5 10; -fx-cursor: hand;"
        );
        editBtn.setOnAction(e -> onEditExport(r));
        editDeleteBox.getChildren().add(editBtn);

        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: #E5E7EB; -fx-border-width: 1;" +
                        "-fx-border-radius: 8; -fx-background-radius: 8;" +
                        "-fx-font-size: 12px; -fx-text-fill: #EF4444;" +
                        "-fx-font-weight: bold; -fx-padding: 5 10; -fx-cursor: hand;"
        );
        deleteBtn.setOnAction(e -> onDeleteExport(r));
        editDeleteBox.getChildren().add(deleteBtn);

        box.getChildren().add(editDeleteBox);

        if (box.getChildren().isEmpty()) {
            Label done = new Label("✔  Done");
            done.setStyle("-fx-font-size: 13px; -fx-text-fill: #10B981;");
            return done;
        }
        return box;
    }

     private void advanceStatus(ExportRecord r) {
         if ("DELIVERED".equals(r.getStatus()) && r.getBalanceAmount() > 0) {
             return;
         }
         String next = switch (r.getStatus()) {
             case "PENDING"      -> "IN TRANSPORT";
             case "IN TRANSPORT" -> "DELIVERED";
             case "DELIVERED"    -> "PAID";
             default             -> r.getStatus();
         };
         r.setStatus(next);
         enqueueExportUpdate(r, "update_status");
         rebuildCards();
         refreshStats();
     }

    private void collectPayment(ExportRecord r) {
        TextInputDialog dialog = new TextInputDialog(String.format("%,.0f", r.getBalanceAmount()));
        dialog.setTitle("Receive Payment");
        dialog.setHeaderText("Record payment for " + r.getCompany());
        dialog.setContentText("Payment amount (balance Rs. " + String.format("%,.0f", r.getBalanceAmount()) + "):");
        if (cardContainer.getScene() != null && cardContainer.getScene().getWindow() != null) {
            dialog.initOwner(cardContainer.getScene().getWindow());
            dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
        }

        Optional<String> value = dialog.showAndWait();
        if (value.isEmpty()) {
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(value.get().replace(",", "").trim());
        } catch (NumberFormatException ex) {
            return;
        }

        if (amount <= 0) {
            return;
        }

        double newPaid = r.getPaidAmount() + amount;
        if (newPaid >= r.getTotalAmount()) {
            newPaid = r.getTotalAmount();
        }
        double newBalance = Math.max(0.0, r.getTotalAmount() - newPaid);
        String paymentStatus = newBalance == 0.0 ? "PAID" : "PARTIAL";

        r.setPaidAmount(newPaid);
        r.setBalanceAmount(newBalance);
        r.setPaymentStatus(paymentStatus);

        enqueueExportUpdate(r, "record_payment");
        rebuildCards();
        refreshStats();
    }

     private void enqueueExportUpdate(ExportRecord r, String operation) {
         String payload = JsonUtil.obj(
                 JsonUtil.field("operation", operation),
                 JsonUtil.field("exportId", r.getExportId()),
                 JsonUtil.field("company", r.getCompany()),
                 JsonUtil.field("tyres", r.getTyres()),
                 JsonUtil.field("custPrice", r.getCustPrice()),
                 JsonUtil.field("compPrice", r.getCompPrice()),
                 JsonUtil.field("serviceFee", r.getServiceCharge()),
                 JsonUtil.field("paidAmount", r.getPaidAmount()),
                 JsonUtil.field("totalAmount", r.getTotalAmount()),
                 JsonUtil.field("balanceAmount", r.getBalanceAmount()),
                 JsonUtil.field("paymentStatus", r.getPaymentStatus()),
                 JsonUtil.field("date", r.getDate().toString()),
                 JsonUtil.field("status", r.getStatus())
         );
         syncQueueRepository.enqueue("tyre_export", payload);
     }

    private void enqueueExport(ExportRecord r) {
        String payload = JsonUtil.obj(
                JsonUtil.field("operation", "create"),
                JsonUtil.field("exportId", r.getExportId()),
                JsonUtil.field("company", r.getCompany()),
                JsonUtil.field("tyres", r.getTyres()),
                JsonUtil.field("custPrice", r.getCustPrice()),
                JsonUtil.field("compPrice", r.getCompPrice()),
                JsonUtil.field("serviceFee", r.getServiceCharge()),
                JsonUtil.field("paidAmount", r.getPaidAmount()),
                JsonUtil.field("totalAmount", r.getTotalAmount()),
                JsonUtil.field("balanceAmount", r.getBalanceAmount()),
                JsonUtil.field("paymentStatus", r.getPaymentStatus()),
                JsonUtil.field("date", r.getDate().toString()),
                JsonUtil.field("status", r.getStatus())
        );
        syncQueueRepository.enqueue("tyre_export", payload);
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

    private String paymentStatusStyle(String paymentStatus, double balanceAmount) {
        if (balanceAmount <= 0.0 || "PAID".equals(paymentStatus)) {
            return "-fx-background-color: #DCFCE7; -fx-text-fill: #166534; -fx-background-radius: 20; -fx-padding: 3 10; -fx-font-size: 11px; -fx-font-weight: bold;";
        }
        if ("PARTIAL".equals(paymentStatus)) {
            return "-fx-background-color: #DBEAFE; -fx-text-fill: #1E40AF; -fx-background-radius: 20; -fx-padding: 3 10; -fx-font-size: 11px; -fx-font-weight: bold;";
        }
        return "-fx-background-color: #FEF3C7; -fx-text-fill: #92400E; -fx-background-radius: 20; -fx-padding: 3 10; -fx-font-size: 11px; -fx-font-weight: bold;";
    }

    private void onEditExport(ExportRecord r) {
        Stage owner = (Stage) cardContainer.getScene().getWindow();
        NewExportDialogController form =
                ViewModel.INSTANCE.getViewsFactory().getForm("form/new-export-dialog", owner);
        if (form == null) {
            return;
        }

        form.setEditMode(r);
        form.setOnSave(result -> {
            r.setCompany(result.company());
            r.setTyres(result.tyres());
            r.setCustPrice(result.custPrice());
            r.setCompPrice(result.compPrice());
            r.setServiceCharge(result.serviceFee());
            r.setPaidAmount(result.paidAmount());
            r.setTotalAmount(result.totalAmount());
            r.setBalanceAmount(result.balanceAmount());
            r.setPaymentStatus(result.paymentStatus());
            r.setDate(result.date());
            r.setStatus(result.status());

            enqueueExportUpdate(r, "update");
            rebuildCards();
            refreshStats();
        });
    }

    private void onDeleteExport(ExportRecord r) {
        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Export");
        confirm.setHeaderText("Are you sure?");
        confirm.setContentText("This will permanently delete export #" + r.getExportId() + " for " + r.getCompany());
        if (cardContainer.getScene() != null && cardContainer.getScene().getWindow() != null) {
            confirm.initOwner(cardContainer.getScene().getWindow());
            confirm.initModality(javafx.stage.Modality.WINDOW_MODAL);
        }

        if (confirm.showAndWait().orElse(javafx.scene.control.ButtonType.CANCEL) == javafx.scene.control.ButtonType.OK) {
            masterList.remove(r);
            enqueueExportUpdate(r, "delete");
            rebuildCards();
            refreshStats();
        }
    }
}
