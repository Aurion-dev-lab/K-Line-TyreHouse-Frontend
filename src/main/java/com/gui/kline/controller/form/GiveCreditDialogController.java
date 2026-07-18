package com.gui.kline.controller.form;

import com.gui.kline.data.LocalWorkerCreditRepository;
import com.gui.kline.models.LedgerEntry;
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
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import java.time.LocalDate;

public class GiveCreditDialogController {

    @FXML
    private Button btnCancel;

    @FXML
    private Button btnRecord;

    @FXML
    private ComboBox<Worker> cmbWorker;

    @FXML
    private DatePicker datePicker;

    @FXML
    private TextField txtAmount;

    @FXML
    private TextField txtNote;    private final LocalWorkerRepository workerRepository = new LocalWorkerRepository();
    private final LocalWorkerCreditRepository creditRepository = new LocalWorkerCreditRepository();
    private Runnable onSaved;
    private boolean editMode = false;
    private String creditId;

    @FXML
    public void initialize() {
        cmbWorker.setItems(FXCollections.observableArrayList(workerRepository.loadWorkers()));
        cmbWorker.setConverter(new StringConverter<>() {
            @Override
            public String toString(Worker worker) {
                return worker == null ? "" : worker.getName();
            }

            @Override
            public Worker fromString(String string) {
                return null;
            }
        });
        datePicker.setValue(LocalDate.now());
    }

    @FXML
    void onCancel(ActionEvent event) {
        closeDialog();
    }

    @FXML
    void onRecord(ActionEvent event) {
        Worker worker = cmbWorker.getValue();
        LocalDate date = datePicker.getValue();
        if (worker == null || date == null) {
            AlertUtil.showError("Missing data", "Please select a worker and date.");
            return;
        }
        double amount = parseAmount(txtAmount.getText());
        if (amount <= 0) {
            AlertUtil.showError("Invalid amount", "Please enter a valid amount.");
            return;
        }
        String note = txtNote.getText().trim();
        String id;
        String payload;
        if (editMode) {
            creditRepository.updateCredit(creditId, worker.getId(), worker.getName(), date, amount, note, "CREDIT");
            id = creditId;
            payload = JsonUtil.obj(
                    JsonUtil.field("id", id),
                    JsonUtil.field("workerId", worker.getId()),
                    JsonUtil.field("worker", worker.getName()),
                    JsonUtil.field("date", date.toString()),
                    JsonUtil.field("amount", amount),
                    JsonUtil.field("note", note),
                    JsonUtil.field("type", "CREDIT"),
                    JsonUtil.field("op", "update")
            );        } else {
            id = creditRepository.saveCredit(worker.getId(), worker.getName(), date, amount, note, "CREDIT");
            payload = JsonUtil.obj(
                    JsonUtil.field("id", id),
                    JsonUtil.field("workerId", worker.getId()),
                    JsonUtil.field("worker", worker.getName()),
                    JsonUtil.field("date", date.toString()),
                    JsonUtil.field("amount", amount),
                    JsonUtil.field("note", note),
                    JsonUtil.field("type", "CREDIT"),
                    JsonUtil.field("op", "create")
            );        }
        if (onSaved != null) {
            onSaved.run();
        }
        closeDialog();
    }

    public void setEditMode(LedgerEntry entry) {
        if (entry == null) return;
        this.editMode = true;
        this.creditId = entry.getId();
        this.cmbWorker.setValue(new LocalWorkerRepository().loadWorkers().stream().filter(w -> w.getName().equals(entry.getWorker())).findFirst().orElse(null));
        this.datePicker.setValue(entry.getDate());
        this.txtAmount.setText(String.valueOf((int)entry.getAmount()));
        this.txtNote.setText(entry.getNote());
        this.btnRecord.setText("Update");
    }

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    private double parseAmount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        try {
            return Double.parseDouble(text.replace(",", ""));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void closeDialog() {
        btnCancel.getScene().getWindow().hide();
    }
}