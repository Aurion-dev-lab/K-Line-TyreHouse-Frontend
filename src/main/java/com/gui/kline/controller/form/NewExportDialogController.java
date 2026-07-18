package com.gui.kline.controller.form;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.function.Consumer;

import com.gui.kline.models.ExportRecord;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class NewExportDialogController implements Initializable {

    @FXML private TextField        txtSerialNumber;
    @FXML private TextField        txtCompany;
    @FXML private TextField        txtTyreCount;
    @FXML private TextField        txtServiceFee;
    @FXML private TextField        txtCustomerPrice;
    @FXML private TextField        txtPaidNow;
    @FXML private TextField        txtCompanyPrice;
    @FXML private Label            lblTotalAmount;
    @FXML private Label            lblPaidAmount;
    @FXML private Label            lblBalanceAmount;
    @FXML private Label            lblPaymentStatus;
    @FXML private DatePicker       dpDateSent;
    @FXML private ComboBox<String> cmbStatus;
    @FXML private Button           btnCancel;
    @FXML private Button           btnSave;

    private final String exportId = "EXP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    private Consumer<ExportResult> onSave;

    public void setOnSave(Consumer<ExportResult> onSave) {
        this.onSave = onSave;
    }

    public void setEditMode(ExportRecord record) {
        txtSerialNumber.setText(record.getSerialNumber() != null ? record.getSerialNumber() : "");
        txtCompany.setText(record.getCompany());
        txtTyreCount.setText(String.valueOf(record.getTyres()));
        txtServiceFee.setText(String.valueOf(record.getServiceCharge()));
        txtCustomerPrice.setText(String.valueOf(record.getCustPrice()));
        txtCompanyPrice.setText(String.valueOf(record.getCompPrice()));
        txtPaidNow.setText(String.valueOf(record.getPaidAmount()));
        dpDateSent.setValue(record.getDate());
        cmbStatus.setValue(record.getStatus());
        refreshSummary();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cmbStatus.setItems(FXCollections.observableArrayList(
                "PENDING", "IN TRANSPORT", "DELIVERED", "PAID"
        ));
        cmbStatus.getSelectionModel().selectFirst();
        dpDateSent.setValue(LocalDate.now());

        // Auto-generate next serial number
        txtSerialNumber.setText(generateNextSerialNumber());

        enforceNumeric(txtTyreCount);
        enforceNumeric(txtServiceFee);
        enforceNumeric(txtCustomerPrice);
        enforceNumeric(txtPaidNow);
        enforceNumeric(txtCompanyPrice);

        txtTyreCount.textProperty().addListener((obs, oldVal, newVal) -> refreshSummary());
        txtServiceFee.textProperty().addListener((obs, oldVal, newVal) -> refreshSummary());
        txtCustomerPrice.textProperty().addListener((obs, oldVal, newVal) -> refreshSummary());
        txtPaidNow.textProperty().addListener((obs, oldVal, newVal) -> refreshSummary());
        txtCompanyPrice.textProperty().addListener((obs, oldVal, newVal) -> refreshSummary());

        refreshSummary();
    }

    @FXML
    private void handleSave(ActionEvent e) {
        double totalAmount = calculateTotalAmount();
        double paidAmount = Math.max(0.0, Math.min(parseDouble(txtPaidNow), totalAmount));
        double balanceAmount = Math.max(0.0, totalAmount - paidAmount);
        String paymentStatus = paymentStatusFor(balanceAmount, paidAmount);

        ExportResult result = new ExportResult(
            exportId,
                txtSerialNumber.getText().trim(),
                txtCompany.getText().trim(),
                parseTyres(),
                parseDouble(txtServiceFee),
                parseDouble(txtCustomerPrice),
                parseDouble(txtCompanyPrice),
            paidAmount,
            totalAmount,
            balanceAmount,
            paymentStatus,
                dpDateSent.getValue() != null ? dpDateSent.getValue() : LocalDate.now(),
                cmbStatus.getValue()
        );
        if (onSave != null) onSave.accept(result);
        closeStage();
    }

    @FXML
    private void handleCancel(ActionEvent e) {
        closeStage();
    }

    private void refreshSummary() {
        double totalAmount = calculateTotalAmount();
        double paidAmount = Math.max(0.0, Math.min(parseDouble(txtPaidNow), totalAmount));
        double balanceAmount = Math.max(0.0, totalAmount - paidAmount);
        String paymentStatus = paymentStatusFor(balanceAmount, paidAmount);

        if (lblTotalAmount != null) {
            lblTotalAmount.setText(String.format("Rs. %,.0f", totalAmount));
        }
        if (lblPaidAmount != null) {
            lblPaidAmount.setText(String.format("Rs. %,.0f", paidAmount));
        }
        if (lblBalanceAmount != null) {
            lblBalanceAmount.setText(String.format("Rs. %,.0f", balanceAmount));
        }
        if (lblPaymentStatus != null) {
            lblPaymentStatus.setText(paymentStatus);
        }
    }

    private double calculateTotalAmount() {
        return parseTyres() * parseDouble(txtCustomerPrice) + parseDouble(txtServiceFee);
    }

    private String paymentStatusFor(double balanceAmount, double paidAmount) {
        if (balanceAmount <= 0.0 && paidAmount > 0.0) {
            return "PAID";
        }
        if (paidAmount > 0.0) {
            return "PARTIAL";
        }
        return "CREDIT";
    }

    private int    parseTyres()              { try { return Integer.parseInt(txtTyreCount.getText().trim()); } catch (Exception ex) { return 0; } }
    private double parseDouble(TextField f)  { try { return Double.parseDouble(f.getText().trim()); }          catch (Exception ex) { return 0.0; } }
    private void   enforceNumeric(TextField field) { field.textProperty().addListener((obs, oldVal, newVal) -> { if (!newVal.matches("\\d*\\.?\\d*")) field.setText(oldVal); }); }
    private void   closeStage()              { ((Stage) btnCancel.getScene().getWindow()).close(); }

    public record ExportResult(
            String    exportId,
            String    serialNumber,
            String    company,
            int       tyres,
            double    serviceFee,
            double    custPrice,
            double    compPrice,
            double    paidAmount,
            double    totalAmount,
            double    balanceAmount,
            String    paymentStatus,
            LocalDate date,
            String    status
    ) {}

    private String generateNextSerialNumber() {
        try (Connection conn = com.gui.kline.data.DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT MAX(CAST(serial_number AS UNSIGNED)) AS max_num FROM tyre_exports WHERE serial_number REGEXP '^[0-9]+$'");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int max = rs.getInt("max_num");
                return String.format("%03d", max + 1);
            }
        } catch (Exception ex) {
            System.err.println("Failed to generate serial number: " + ex.getMessage());
        }
        return "001";
    }
}