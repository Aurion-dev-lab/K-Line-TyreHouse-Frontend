package com.gui.kline.controller.form;

import com.gui.kline.data.LocalWorkerCreditRepository;
import com.gui.kline.models.dto.LedgerEntry;
import com.gui.kline.data.LocalWorkerRepository;
import com.gui.kline.models.Worker;
import com.gui.kline.utils.AlertUtil;
import com.gui.kline.utils.JsonUtil;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import java.time.LocalDate;

public class SettleCreditDialogController {

    @FXML private Button btnCancel;
    @FXML private Button btnRecord;
    @FXML private ComboBox<Worker> cmbWorker;
    @FXML private DatePicker datePicker;
    @FXML private TextField txtAmount;
    @FXML private TextField txtNote;
    @FXML private Label lblCreditBalance;   // optional hint label
    @FXML private Label lblError;           // inline error label under txtAmount

    private final LocalWorkerRepository workerRepository = new LocalWorkerRepository();
    private final LocalWorkerCreditRepository creditRepository = new LocalWorkerCreditRepository();
    private Runnable onSaved;
    private boolean editMode = false;
    private String creditId;
    private double originalAmount = 0;

    /** Outstanding balance for the currently selected worker (refreshed on selection change). */
    private double outstandingBalance = 0;

    @FXML
    public void initialize() {
        cmbWorker.setItems(FXCollections.observableArrayList(workerRepository.loadWorkers()));
        cmbWorker.setConverter(new StringConverter<>() {
            @Override public String toString(Worker worker) { return worker == null ? "" : worker.getName(); }
            @Override public Worker fromString(String string) { return null; }
        });
        datePicker.setValue(LocalDate.now());

        // Whenever the selected worker changes, look up their outstanding balance
        cmbWorker.valueProperty().addListener((obs, oldW, newW) -> {
            refreshBalance(newW);
            validateAmount();
        });

        // Validate dynamically as the user types the amount
        txtAmount.textProperty().addListener((obs, oldVal, newVal) -> validateAmount());
    }

    private void refreshBalance(Worker worker) {
        if (worker == null) {
            outstandingBalance = 0;
            updateBalanceLabel();
            return;
        }
        outstandingBalance = creditRepository.getOutstandingBalance(worker.getId());
        updateBalanceLabel();
        // Pre-fill the amount with the full outstanding balance if the field is empty / zero
        String cur = txtAmount.getText().trim();
        if (cur.isEmpty() || "0".equals(cur)) {
            txtAmount.setText(outstandingBalance > 0 ? String.format("%.0f", outstandingBalance) : "");
        }
    }

    private void updateBalanceLabel() {
        if (lblCreditBalance != null) {
            if (outstandingBalance > 0) {
                lblCreditBalance.setText(String.format("Outstanding credit: Rs. %,.0f  (max you can settle)", outstandingBalance));
                lblCreditBalance.setStyle("-fx-text-fill: #b45309; -fx-font-size: 11px; -fx-font-weight: bold;");
            } else {
                lblCreditBalance.setText("No outstanding credit for this worker.");
                lblCreditBalance.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");
            }
        }
    }

    private boolean validateAmount() {
        Worker worker = cmbWorker.getValue();
        if (worker == null) {
            hideError();
            return true;
        }
        double amount = parseAmount(txtAmount.getText());
        double maxSettleable = editMode
                ? creditRepository.getOutstandingBalance(worker.getId()) + originalAmount
                : creditRepository.getOutstandingBalance(worker.getId());

        if (amount > maxSettleable + 0.001) {
            if (lblError != null) {
                lblError.setText(String.format("Maximum: Rs. %,.2f", maxSettleable));
                lblError.setVisible(true);
                lblError.setManaged(true);
            }
            return false;
        } else {
            hideError();
            return true;
        }
    }

    private void hideError() {
        if (lblError != null) {
            lblError.setText("");
            lblError.setVisible(false);
            lblError.setManaged(false);
        }
    }

    @FXML
    void onCancel(ActionEvent event) { closeDialog(); }

    @FXML
    void onRecord(ActionEvent event) {
        Worker worker = cmbWorker.getValue();
        LocalDate date = datePicker.getValue();
        if (worker == null || date == null) {
            AlertUtil.showError(getOwnerWindow(), "Missing data", "Please select a worker and date.");
            return;
        }
        double amount = parseAmount(txtAmount.getText());
        if (amount <= 0) {
            AlertUtil.showError(getOwnerWindow(), "Invalid amount", "Please enter a valid amount greater than zero.");
            return;
        }

        // Validate amount against credit cap
        if (!validateAmount()) {
            return;
        }

        String note = txtNote.getText().trim();
        String id;
        String payload;
        if (editMode) {
            creditRepository.updateCredit(creditId, worker.getId(), worker.getName(), date, amount, note, "SETTLEMENT");
            id = creditId;
            payload = JsonUtil.obj(
                    JsonUtil.field("id", id),
                    JsonUtil.field("workerId", worker.getId()),
                    JsonUtil.field("worker", worker.getName()),
                    JsonUtil.field("date", date.toString()),
                    JsonUtil.field("amount", amount),
                    JsonUtil.field("note", note),
                    JsonUtil.field("type", "SETTLEMENT"),
                    JsonUtil.field("op", "update")
            );
        } else {
            id = creditRepository.saveCredit(worker.getId(), worker.getName(), date, amount, note, "SETTLEMENT");
            payload = JsonUtil.obj(
                    JsonUtil.field("id", id),
                    JsonUtil.field("workerId", worker.getId()),
                    JsonUtil.field("worker", worker.getName()),
                    JsonUtil.field("date", date.toString()),
                    JsonUtil.field("amount", amount),
                    JsonUtil.field("note", note),
                    JsonUtil.field("type", "SETTLEMENT"),
                    JsonUtil.field("op", "create")
            );
        }
        if (onSaved != null) onSaved.run();
        closeDialog();
    }

    public void setEditMode(LedgerEntry entry) {
        if (entry == null) return;
        this.editMode = true;
        this.creditId = entry.getId();
        this.originalAmount = entry.getAmount();
        Worker found = new LocalWorkerRepository().loadWorkers().stream()
                .filter(w -> w.getName().equals(entry.getWorker()))
                .findFirst().orElse(null);
        this.cmbWorker.setValue(found);   // triggers refreshBalance listener
        this.datePicker.setValue(entry.getDate());
        this.txtAmount.setText(String.valueOf((int) entry.getAmount()));
        this.txtNote.setText(entry.getNote());
        this.btnRecord.setText("Update");
    }

    public void setOnSaved(Runnable onSaved) { this.onSaved = onSaved; }

    private double parseAmount(String text) {
        if (text == null || text.isBlank()) return 0;
        try { return Double.parseDouble(text.replace(",", "")); }
        catch (NumberFormatException ex) { return 0; }
    }

    private void closeDialog() { btnCancel.getScene().getWindow().hide(); }

    private javafx.stage.Window getOwnerWindow() {
        if (btnCancel != null && btnCancel.getScene() != null) return btnCancel.getScene().getWindow();
        return null;
    }
}
