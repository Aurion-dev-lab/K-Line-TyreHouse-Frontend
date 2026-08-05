package com.gui.kline.controller.form;

import com.gui.kline.data.LocalCreditSalesRepository;
import com.gui.kline.data.TyreExportRepository;
import com.gui.kline.models.dto.PaymentRecord;
import com.gui.kline.models.reports.SalaryPayment;
import com.gui.kline.models.ui.WorkerSalary;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class PaymentHistoryDialogController implements Initializable {

    @FXML private Label lblTitle;
    @FXML private Label lblSubtitle;
    @FXML private Label lblTotalDue;
    @FXML private Label lblTotalPaid;
    @FXML private Label lblBalance;
    @FXML private VBox  vboxPayments;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final LocalCreditSalesRepository creditSalesRepo = new LocalCreditSalesRepository();
    private final TyreExportRepository       exportRepo      = new TyreExportRepository();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Data is loaded via setData() or setSalaryData()
    }

    /**
     * Populate the dialog for Credit Sales or Tyre Exports.
     */
    public void setData(String type, String referenceId, String entityName, double grandTotal) {
        lblSubtitle.setText(entityName != null ? entityName : referenceId);
        lblTitle.setText("Payment History");

        List<PaymentRecord> payments;
        if ("CREDIT".equals(type)) {
            payments = creditSalesRepo.getPaymentsForCredit(referenceId);
        } else {
            payments = exportRepo.getPaymentsForExport(referenceId);
        }

        double totalPaid = payments.stream().mapToDouble(PaymentRecord::getAmount).sum();
        double balance   = Math.max(0, grandTotal - totalPaid);

        lblTotalDue.setText(String.format("Rs. %,.2f", grandTotal));
        lblTotalPaid.setText(String.format("Rs. %,.2f", totalPaid));
        lblBalance.setText(String.format("Rs. %,.2f", balance));

        if (balance <= 0.0) {
            lblBalance.getStyleClass().remove("summary-value-red");
            lblBalance.getStyleClass().add("summary-value-green");
            lblBalance.setText("Settled");
        } else {
            lblBalance.getStyleClass().remove("summary-value-green");
            lblBalance.getStyleClass().add("summary-value-red");
        }

        vboxPayments.getChildren().clear();

        if (payments.isEmpty()) {
            Label empty = new Label("No payments have been recorded yet.");
            empty.getStyleClass().add("empty-state-label");
            empty.setMaxWidth(Double.MAX_VALUE);
            empty.setAlignment(Pos.CENTER);
            VBox.setMargin(empty, new Insets(24, 0, 24, 0));
            vboxPayments.getChildren().add(empty);
            return;
        }

        for (PaymentRecord p : payments) {
            vboxPayments.getChildren().add(buildPaymentRow(p));
        }
    }

    /**
     * Populate the dialog for Salary Payments.
     */
    public void setSalaryData(WorkerSalary worker, List<SalaryPayment> payments, Consumer<SalaryPayment> onDeletePayment) {
        lblTitle.setText("Payment History");
        lblSubtitle.setText(worker.getName());

        double totalDue  = worker.getNetPayable();
        double totalPaid = worker.getPaidAmount();
        double balance   = worker.getRemainingPayable();

        lblTotalDue.setText(String.format("Rs. %,.2f", totalDue));
        lblTotalPaid.setText(String.format("Rs. %,.2f", totalPaid));
        lblBalance.setText(String.format("Rs. %,.2f", balance));

        if (balance <= 0.0) {
            lblBalance.getStyleClass().remove("summary-value-red");
            lblBalance.getStyleClass().add("summary-value-green");
            lblBalance.setText("Settled");
        } else {
            lblBalance.getStyleClass().remove("summary-value-green");
            lblBalance.getStyleClass().add("summary-value-red");
        }

        vboxPayments.getChildren().clear();

        if (payments.isEmpty()) {
            Label empty = new Label("No salary payments recorded in this period.");
            empty.getStyleClass().add("empty-state-label");
            empty.setMaxWidth(Double.MAX_VALUE);
            empty.setAlignment(Pos.CENTER);
            VBox.setMargin(empty, new Insets(24, 0, 24, 0));
            vboxPayments.getChildren().add(empty);
            return;
        }

        for (SalaryPayment p : payments) {
            vboxPayments.getChildren().add(buildSalaryPaymentRow(p, onDeletePayment));
        }
    }

    private HBox buildPaymentRow(PaymentRecord p) {
        VBox left = new VBox(3);
        HBox.setHgrow(left, Priority.ALWAYS);

        HBox topLine = new HBox(8);
        topLine.setAlignment(Pos.CENTER_LEFT);

        Label date = new Label(p.getDate() != null ? p.getDate().format(DATE_FMT) : "Unknown date");
        date.getStyleClass().add("payment-date");

        Label method = new Label(p.getMethod());
        method.getStyleClass().add("payment-method-badge");

        topLine.getChildren().addAll(date, method);
        left.getChildren().add(topLine);

        if (p.getNotes() != null && !p.getNotes().isBlank()) {
            Label notes = new Label(p.getNotes());
            notes.getStyleClass().add("payment-notes");
            notes.setWrapText(true);
            left.getChildren().add(notes);
        }

        Label amount = new Label("Rs. " + String.format("%,.2f", p.getAmount()));
        amount.getStyleClass().add("payment-amount");
        amount.setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox(12, left, amount);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 12, 10, 12));
        row.getStyleClass().add("payment-row");

        return row;
    }

    private HBox buildSalaryPaymentRow(SalaryPayment p, Consumer<SalaryPayment> onDeletePayment) {
        VBox left = new VBox(3);
        HBox.setHgrow(left, Priority.ALWAYS);

        HBox topLine = new HBox(8);
        topLine.setAlignment(Pos.CENTER_LEFT);

        Label date = new Label(p.getPaidAt() != null ? p.getPaidAt().toLocalDate().format(DATE_FMT) : "—");
        date.getStyleClass().add("payment-date");

        Label method = new Label("Payout");
        method.getStyleClass().add("payment-method-badge");

        topLine.getChildren().addAll(date, method);
        left.getChildren().add(topLine);

        Label notes = new Label("Salary payout");
        notes.getStyleClass().add("payment-notes");
        left.getChildren().add(notes);

        Label amount = new Label("Rs. " + String.format("%,.2f", p.getAmount()));
        amount.getStyleClass().add("payment-amount");
        amount.setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox(12, left, amount);

        if (onDeletePayment != null) {
            Button delBtn = new Button("🗑");
            delBtn.getStyleClass().add("payment-delete-btn");
            delBtn.setOnAction(ev -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Delete Payment");
                confirm.setHeaderText(null);
                confirm.setContentText("Delete payment of Rs. " + String.format("%,.0f", p.getAmount()) +
                        " made on " + (p.getPaidAt() != null ? p.getPaidAt().toLocalDate() : "") + "?");
                
                if (vboxPayments.getScene() != null && vboxPayments.getScene().getWindow() != null) {
                    confirm.initOwner(vboxPayments.getScene().getWindow());
                    confirm.initModality(javafx.stage.Modality.WINDOW_MODAL);
                }

                if (confirm.showAndWait().filter(btn -> btn == ButtonType.OK).isPresent()) {
                    onDeletePayment.accept(p);
                }
            });
            row.getChildren().add(delBtn);
        }

        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 12, 10, 12));
        row.getStyleClass().add("payment-row");

        return row;
    }

    @FXML
    private void onClose() {
        Stage stage = (Stage) vboxPayments.getScene().getWindow();
        stage.close();
    }
}