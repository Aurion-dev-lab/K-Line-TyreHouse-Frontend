package com.gui.kline.controller;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import com.gui.kline.controller.form.NewExportDialogController;
import com.gui.kline.controller.form.PaymentHistoryDialogController;

import com.gui.kline.data.TyreExportRepository;
import com.gui.kline.models.ui.ExportRecord;
import com.gui.kline.models.dto.InvoiceDetail;
import com.gui.kline.models.dto.LineItem;
import com.gui.kline.models.dto.TyreExport;
import com.gui.kline.models.ViewModel;
import com.gui.kline.utils.Utils;

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
import javafx.scene.layout.Region;
import javafx.stage.Stage;

public class TyreExportsController implements Initializable {

    @FXML private TextField  txtFilter;
    @FXML private DatePicker dpFrom;
    @FXML private DatePicker dpTo;
    @FXML private VBox       cardContainer;

    @FXML private Label lblShipments;
    @FXML private Label lblTyres;
    @FXML private Label lblGain;
    @FXML private Label lblLoss;
    @FXML private Label lblPending;

    private final ObservableList<ExportRecord> masterList   = FXCollections.observableArrayList();
    private       FilteredList<ExportRecord>   filteredList;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");    private final TyreExportRepository tyreExportRepository = new TyreExportRepository();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dpFrom.setValue(LocalDate.now());
        dpTo.setValue(LocalDate.now());
        
        filteredList = new FilteredList<>(masterList, p -> true);
        filteredList.addListener((ListChangeListener<ExportRecord>) c -> {
            rebuildCards();
            refreshStats();
        });
        loadFromLocal();
    }

    private void loadFromLocal() {
        // Load from local database using TyreExportRepository
        List<com.gui.kline.models.dto.TyreExport> exports = tyreExportRepository.getAllExports();
        masterList.clear();
        for (com.gui.kline.models.dto.TyreExport export : exports) {
            ExportRecord record = new ExportRecord(
                export.getExportId() != null ? export.getExportId() : export.getId(),
                export.getSerialNumber() != null ? export.getSerialNumber() : "",
                export.getCompany(),
                export.getTyreSize() != null ? export.getTyreSize() : "",
                export.getTyreMake() != null ? export.getTyreMake() : "",
                export.getTyres(),
                export.getCustPrice(),
                export.getCompPrice(),
                export.getServiceFee(),
                export.getSubTotal(),
                export.getGrandTotal(),
                export.getInitialPayment(),
                export.getSettlement(),
                export.getExportDate() != null ? export.getExportDate() : LocalDate.now(),
                export.getStatus() != null ? export.getStatus() : "PENDING",
                export.getRemark() != null ? export.getRemark() : ""
            );
            masterList.add(record);
        }
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
            // Create TyreExport for local database
            com.gui.kline.models.dto.TyreExport tyreExport = new com.gui.kline.models.dto.TyreExport();
            tyreExport.setId(Utils.generateId("EXP-PK-", 8));
            tyreExport.setExportId(result.exportId());
            tyreExport.setSerialNumber(result.serialNumber());
            tyreExport.setCompany(result.company());
            tyreExport.setTyreSize(result.tyreSize());
            tyreExport.setTyreMake(result.tyreMake());
            tyreExport.setTyres(result.tyres());
            tyreExport.setCustPrice(result.custPrice());
            tyreExport.setCompPrice(result.compPrice());
            tyreExport.setServiceFee(result.serviceFee());
            
            double subTotal = result.tyres() * result.custPrice();
            double grandTotal = subTotal + result.serviceFee();
            tyreExport.setSubTotal(subTotal);
            tyreExport.setGrandTotal(grandTotal);
            tyreExport.setInitialPayment(result.paidAmount());
            tyreExport.setSettlement(result.paidAmount());
            tyreExport.setRemark(result.remark());
            tyreExport.setExportDate(result.date());
            tyreExport.setStatus(result.status());
            
            // Save to local database
            tyreExportRepository.saveTyreExport(tyreExport);
            
            // Record payment transaction into tyre_export_payments table if paid amount > 0
            if (result.paidAmount() > 0) {
                tyreExportRepository.recordExportPayment(
                        result.exportId(),
                        result.paidAmount(),
                        "Cash",
                        "Initial payment balance",
                        result.date() != null ? result.date() : LocalDate.now()
                );
            }
            
            // Refresh analytics and dashboard
            ViewModel.INSTANCE.getViewsFactory().refreshReports();
            ViewModel.INSTANCE.getViewsFactory().refreshDashboard();
            
            // Create ExportRecord for UI
            ExportRecord record = new ExportRecord(
                    result.exportId(),
                    result.serialNumber(),
                    result.company(),
                    result.tyreSize(),
                    result.tyreMake(),
                    result.tyres(),
                    result.custPrice(),
                    result.compPrice(),
                    result.serviceFee(),
                    subTotal,
                    grandTotal,
                    result.paidAmount(),
                    result.paidAmount(),
                    result.date(),
                    result.status(),
                    result.remark()
            );
            masterList.add(0, record);
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
        double gains     = filteredList.stream().mapToDouble(this::calcProfit).filter(p -> p > 0).sum();
        double losses    = Math.abs(filteredList.stream().mapToDouble(this::calcProfit).filter(p -> p < 0).sum());
        long   pending   = filteredList.stream().filter(r -> r.getBalanceAmount() > 0).count();

        lblShipments.setText(String.valueOf(shipments));
        lblTyres.setText(String.valueOf(tyres));
        lblGain.setText(String.format("Rs. %,.0f", gains));
        lblLoss.setText(String.format("Rs. %,.0f", losses));
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
        // ── Row 1: Company name, meta chips, gain/loss, status badges ──────
        Label name = new Label(r.getCompany());
        name.getStyleClass().add("export-company-name");

        Label exportId = chip(r.getExportId().isBlank() ? "Draft" : r.getExportId());
        exportId.getStyleClass().add("export-chip");

        Label serialLbl = new Label(r.getSerialNumber().isBlank() ? "" : "S/N: " + r.getSerialNumber());
        serialLbl.getStyleClass().add("export-chip");

        Label tyresLbl = new Label(r.getTyres() + " tyres");
        tyresLbl.getStyleClass().add("export-chip");

        Label dateLbl = new Label(r.getDate().format(DATE_FMT));
        dateLbl.getStyleClass().add("export-chip");

        HBox meta = new HBox(6);
        meta.setAlignment(Pos.CENTER_LEFT);
        meta.getChildren().add(exportId);
        if (!r.getSerialNumber().isBlank()) meta.getChildren().add(serialLbl);
        if (!r.getTyreSize().isBlank()) {
            Label sizeLbl = new Label(r.getTyreSize());
            sizeLbl.getStyleClass().add("export-chip");
            meta.getChildren().add(sizeLbl);
        }
        if (!r.getTyreMake().isBlank()) {
            Label makeLbl = new Label(r.getTyreMake());
            makeLbl.getStyleClass().add("export-chip");
            meta.getChildren().add(makeLbl);
        }
        meta.getChildren().addAll(tyresLbl, dateLbl);

        VBox nameCol = new VBox(4, name, meta);
        if (r.getRemark() != null && !r.getRemark().isBlank()) {
            Label remarkLbl = new Label("Remark: " + r.getRemark());
            remarkLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280; -fx-font-style: italic;");
            nameCol.getChildren().add(remarkLbl);
        }
        nameCol.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nameCol, Priority.ALWAYS);

        // Gain / Loss
        double profit = calcProfit(r);
        Label profitAmt = new Label();
        Label profitTag = new Label();
        if (profit > 0) {
            profitAmt.setText("+ Rs. " + String.format("%,.0f", profit));
            profitAmt.getStyleClass().add("export-gain-amount");
            profitTag.setText("gain");
            profitTag.getStyleClass().add("export-gain-tag");
        } else if (profit < 0) {
            profitAmt.setText("- Rs. " + String.format("%,.0f", Math.abs(profit)));
            profitAmt.getStyleClass().add("export-loss-amount");
            profitTag.setText("loss");
            profitTag.getStyleClass().add("export-loss-tag");
        } else {
            profitAmt.setText("Rs. 0");
            profitAmt.getStyleClass().add("export-neutral-amount");
            profitTag.setText("–");
        }
        HBox profitBox = new HBox(5, profitAmt, profitTag);
        profitBox.setAlignment(Pos.CENTER_RIGHT);

        // Status badges
        Label shipBadge = new Label(r.getStatus());
        shipBadge.getStyleClass().add(shipmentBadgeClass(r.getStatus()));

        String payText = r.getPaymentStatus() +
                (r.getBalanceAmount() > 0 ? "   Due Rs. " + String.format("%,.0f", r.getBalanceAmount()) : "  Settled");
        Label payBadge = new Label(payText);
        payBadge.getStyleClass().add(paymentBadgeClass(r.getPaymentStatus(), r.getBalanceAmount()));

        HBox badgesBox = new HBox(6, shipBadge, payBadge);
        badgesBox.setAlignment(Pos.CENTER_RIGHT);

        VBox rightCol = new VBox(4, profitBox, badgesBox);
        rightCol.setAlignment(Pos.TOP_RIGHT);

        HBox topRow = new HBox(12, nameCol, rightCol);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // ── Divider 1 ─────────────────────────────────────────────────────
        Region div1 = new Region();
        div1.getStyleClass().add("card-divider");
        VBox.setMargin(div1, new Insets(8, 0, 8, 0));

        // ── Row 2: Pricing ────────────────────────────────────────────────
        Label cLabel = new Label("Customer Price: ");
        cLabel.getStyleClass().add("pricing-label");
        Label cValue = new Label("Rs. " + String.format("%,.0f", r.getCustPrice()));
        cValue.getStyleClass().add("pricing-cust");

        Label compLabel = new Label("Company Cost: ");
        compLabel.getStyleClass().add("pricing-label");
        Label compValue = new Label("Rs. " + String.format("%,.0f", r.getCompPrice()));
        compValue.getStyleClass().add("pricing-comp");

        Label fLabel = new Label("Service Fee: ");
        fLabel.getStyleClass().add("pricing-label");
        Label fValue = new Label("Rs. " + String.format("%,.0f", r.getServiceCharge()));
        fValue.getStyleClass().add("pricing-fee");

        Label tLabel = new Label("Grand Total: ");
        tLabel.getStyleClass().add("pricing-label");
        Label tValue = new Label("Rs. " + String.format("%,.0f", r.getGrandTotal()));
        tValue.getStyleClass().add("pricing-total");

        Label sLabel = new Label("Settled: ");
        sLabel.getStyleClass().add("pricing-label");
        Label sValue = new Label("Rs. " + String.format("%,.0f", r.getSettlement()));
        sValue.getStyleClass().add("pricing-settled");

        HBox pricingRow = new HBox(24,
                new HBox(cLabel, cValue),
                new HBox(compLabel, compValue),
                new HBox(fLabel, fValue),
                new HBox(tLabel, tValue),
                new HBox(sLabel, sValue));
        pricingRow.setAlignment(Pos.CENTER_LEFT);

        // ── Divider 2 ─────────────────────────────────────────────────────
        Region div2 = new Region();
        div2.getStyleClass().add("card-divider");
        VBox.setMargin(div2, new Insets(8, 0, 8, 0));

        // ── Row 3: Actions ────────────────────────────────────────────────
        HBox actionRow = (HBox) buildActionNode(r);

        VBox cardContent = new VBox(topRow, div1, pricingRow, div2, actionRow);
        cardContent.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(cardContent, Priority.ALWAYS);

        HBox card = new HBox(cardContent);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(14, 18, 14, 18));
        card.getStyleClass().add("export-card");

        return card;
    }

    private Node buildActionNode(ExportRecord r) {
        String shipmentLabel = switch (r.getStatus()) {
            case "PENDING"      -> "Mark as In Transport";
            case "IN TRANSPORT" -> "Mark as Delivered";
            case "DELIVERED"    -> r.getBalanceAmount() > 0 ? null : "Mark as Paid";
            default             -> null;
        };

        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);

        if (shipmentLabel != null) {
            Button btn = new Button(shipmentLabel);
            btn.getStyleClass().add("btn-action-outline-blue");
            btn.setOnAction(e -> advanceStatus(r));
            box.getChildren().add(btn);
        }

        if (r.getBalanceAmount() > 0) {
            Button payBtn = new Button("Settle Credit");
            payBtn.getStyleClass().add("btn-action-outline-green");
            payBtn.setOnAction(e -> collectPayment(r));
            box.getChildren().add(payBtn);
        }

        Button historyBtn = new Button("Payment History");
        historyBtn.getStyleClass().add("btn-action-outline-blue");
        historyBtn.setOnAction(e -> onViewPaymentHistory(r));
        box.getChildren().add(historyBtn);

        Button downloadPdfBtn = new Button("Download PDF");
        downloadPdfBtn.getStyleClass().add("btn-action-blue");
        downloadPdfBtn.setOnAction(e -> onDownloadInvoiceForRecord(r));
        box.getChildren().add(downloadPdfBtn);

        if (r.getBalanceAmount() > 0) {
            Button editBtn = new Button("Edit");
            editBtn.getStyleClass().add("btn-action-outline-amber");
            editBtn.setOnAction(e -> onEditExport(r));
            box.getChildren().add(editBtn);
        }

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("btn-action-outline-red");
        deleteBtn.setOnAction(e -> onDeleteExport(r));
        box.getChildren().add(deleteBtn);

        return box;
    }

    private String shipmentBadgeClass(String status) {
        return switch (status) {
            case "PENDING"      -> "badge-pending";
            case "IN TRANSPORT" -> "badge-transport";
            case "DELIVERED"    -> "badge-delivered";
            case "PAID"         -> "badge-paid";
            default             -> "badge-pending";
        };
    }

    private String paymentBadgeClass(String payStatus, double balance) {
        if (balance <= 0) return "badge-paid";
        return switch (payStatus) {
            case "PAID"    -> "badge-paid";
            case "PARTIAL" -> "badge-partial";
            default        -> "badge-credit";
        };
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
        
        // Save to database
        com.gui.kline.models.dto.TyreExport tyreExport = tyreExportRepository.getTyreExportByExportId(r.getExportId());
        if (tyreExport != null) {
            tyreExport.setStatus(next);
            tyreExportRepository.saveTyreExport(tyreExport);
        }
        
        // Reload data from database to ensure UI is in sync
        loadFromLocal();
        
        // Refresh analytics and dashboard
        ViewModel.INSTANCE.getViewsFactory().refreshReports();
        ViewModel.INSTANCE.getViewsFactory().refreshDashboard();
        
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

        double newPaid = r.getSettlement() + amount;
        if (newPaid >= r.getGrandTotal()) {
            newPaid = r.getGrandTotal();
        }

        r.setSettlement(newPaid);

        // Record payment transaction in local database table tyre_export_payments
        tyreExportRepository.recordExportPayment(
                r.getExportId(),
                amount,
                "Cash",
                "Tyre Export Settlement",
                LocalDate.now()
        );
        
        // Reload data from database to ensure UI is in sync
        loadFromLocal();
        
        // Refresh analytics and dashboard
        ViewModel.INSTANCE.getViewsFactory().refreshReports();
        ViewModel.INSTANCE.getViewsFactory().refreshDashboard();
        
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
            r.setSerialNumber(result.serialNumber());
            r.setCompany(result.company());
            r.setTyreSize(result.tyreSize());
            r.setTyreMake(result.tyreMake());
            r.setTyres(result.tyres());
            r.setCustPrice(result.custPrice());
            r.setCompPrice(result.compPrice());
            r.setServiceCharge(result.serviceFee());
            
            double subTotal = result.tyres() * result.custPrice();
            double grandTotal = subTotal + result.serviceFee();
            r.setSubTotal(subTotal);
            r.setGrandTotal(grandTotal);
            r.setInitialPayment(result.paidAmount());
            r.setSettlement(result.paidAmount());
            r.setDate(result.date());
            r.setStatus(result.status());
            r.setRemark(result.remark());

            // Update in local database
            com.gui.kline.models.dto.TyreExport tyreExport = tyreExportRepository.getTyreExportByExportId(r.getExportId());
            if (tyreExport != null) {
                tyreExport.setSerialNumber(result.serialNumber());
                tyreExport.setCompany(result.company());
                tyreExport.setTyreSize(result.tyreSize());
                tyreExport.setTyreMake(result.tyreMake());
                tyreExport.setTyres(result.tyres());
                tyreExport.setCustPrice(result.custPrice());
                tyreExport.setCompPrice(result.compPrice());
                tyreExport.setServiceFee(result.serviceFee());
                tyreExport.setSubTotal(subTotal);
                tyreExport.setGrandTotal(grandTotal);
                tyreExport.setInitialPayment(result.paidAmount());
                tyreExport.setSettlement(result.paidAmount());
                tyreExport.setExportDate(result.date());
                tyreExport.setStatus(result.status());
                tyreExport.setRemark(result.remark());
                tyreExportRepository.saveTyreExport(tyreExport);
            }

        rebuildCards();
            refreshStats();
        });
    }

    private void onViewPaymentHistory(ExportRecord r) {
        Stage owner = (Stage) cardContainer.getScene().getWindow();
        PaymentHistoryDialogController dlg =
                ViewModel.INSTANCE.getViewsFactory().getForm("form/payment-history-dialog", owner);
        if (dlg != null) {
            dlg.setData(
                "EXPORT",
                r.getExportId(),
                r.getCompany(),
                r.getGrandTotal()
            );
        }
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
            // Delete from local database by export_id
            if (!r.getExportId().isBlank()) {
                tyreExportRepository.deleteTyreExportByExportId(r.getExportId());
            }
            
            // Refresh analytics and dashboard
            ViewModel.INSTANCE.getViewsFactory().refreshReports();
            ViewModel.INSTANCE.getViewsFactory().refreshDashboard();
            
            // Remove from UI list
            masterList.remove(r);
            
            rebuildCards();
            refreshStats();
        }
    }

    private void onDownloadInvoiceForRecord(ExportRecord r) {
        try {
            // Create a temporary invoice for PDF generation
            String invoiceId = r.getExportId().isBlank() ? "Export_" + System.currentTimeMillis() : r.getExportId();
            String type = "Tyre Export";
            String dateStr = LocalDate.now().toString();

            // Fetch payment history for this tyre export
            List<com.gui.kline.models.dto.PaymentRecord> payments = tyreExportRepository.getPaymentsForExport(r.getExportId());

            // Create invoice detail
            com.gui.kline.models.dto.InvoiceDetail invoiceDetail = new com.gui.kline.models.dto.InvoiceDetail();
            invoiceDetail.setInvoiceId(invoiceId);
            invoiceDetail.setSerialNumber(r.getSerialNumber());
            invoiceDetail.setCustomer(r.getCompany());
            invoiceDetail.setDate(r.getDate() != null ? r.getDate().toString() : dateStr);
            invoiceDetail.setType(type);
            invoiceDetail.setStatus(r.getPaymentStatus());
            invoiceDetail.setInitialPayment(r.getInitialPayment());
            invoiceDetail.setTyreSize(r.getTyreSize());
            invoiceDetail.setTyreMake(r.getTyreMake());
            invoiceDetail.setRemark(r.getRemark());
            invoiceDetail.setPaymentHistory(payments);

            // Construct detailed item description for tyres
            StringBuilder tyreDesc = new StringBuilder();
            tyreDesc.append(r.getTyres()).append(" Tyres");
            if (!r.getTyreSize().isBlank() || !r.getTyreMake().isBlank()) {
                tyreDesc.append(" (");
                if (!r.getTyreSize().isBlank()) tyreDesc.append(r.getTyreSize());
                if (!r.getTyreMake().isBlank()) {
                    if (!r.getTyreSize().isBlank()) tyreDesc.append(" ");
                    tyreDesc.append(r.getTyreMake());
                }
                tyreDesc.append(")");
            }

            com.gui.kline.models.dto.LineItem tyreItem = new com.gui.kline.models.dto.LineItem(
                    tyreDesc.toString(),
                    "Sale",
                    r.getTyres(),
                    r.getCustPrice(),
                    null
            );
            invoiceDetail.addLineItem(tyreItem);

            // Add line item for service fee if applicable
            if (r.getServiceCharge() > 0) {
                com.gui.kline.models.dto.LineItem serviceItem = new com.gui.kline.models.dto.LineItem(
                        "Service Charge & Handling",
                        "Service",
                        1,
                        r.getServiceCharge(),
                        "Service Charge"
                );
                invoiceDetail.addLineItem(serviceItem);
            }

            // Open file chooser
            Stage ownerStage = (Stage) cardContainer.getScene().getWindow();
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Save Invoice PDF");
            fileChooser.setInitialFileName("Invoice_" + invoiceId + ".pdf");
            fileChooser.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("PDF files (*.pdf)", "*.pdf"));

            java.io.File file = fileChooser.showSaveDialog(ownerStage);
            if (file == null) {
                return; // user cancelled
            }

            // Generate PDF
            new com.gui.kline.service.InvoicePdfService().export(invoiceDetail, file);
            showSuccess("Invoice PDF saved to:\n" + file.getAbsolutePath());
        } catch (Exception ex) {
            showError("Failed to generate PDF: " + ex.getMessage());
        }
    }

    private ExportRecord getSelectedRecord() {
        return null; // No selection mechanism in current implementation
    }

    private void showError(String msg) {
        javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText(null);
        a.setContentText(msg);
        if (cardContainer.getScene() != null && cardContainer.getScene().getWindow() != null) {
            a.initOwner(cardContainer.getScene().getWindow());
            a.initModality(javafx.stage.Modality.WINDOW_MODAL);
        }
        a.showAndWait();
    }

    private void showSuccess(String msg) {
        javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        a.setTitle("Success");
        a.setHeaderText(null);
        a.setContentText(msg);
        if (cardContainer.getScene() != null && cardContainer.getScene().getWindow() != null) {
            a.initOwner(cardContainer.getScene().getWindow());
            a.initModality(javafx.stage.Modality.WINDOW_MODAL);
        }
        a.showAndWait();
    }
}

