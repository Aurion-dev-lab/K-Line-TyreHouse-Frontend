package com.gui.kline.controller.form;

import com.gui.kline.data.LocalSalaryAdvanceRepository;
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

public class SalaryAdvanceController {

    @FXML
    private Button btnCancel;

    @FXML
    private Button btnSave;

    @FXML
    private ComboBox<Worker> cmbWorker;

    @FXML
    private DatePicker datePicker;

    @FXML
    private TextField txtAmount;

    @FXML
    private TextField txtNote;    private final LocalWorkerRepository workerRepository = new LocalWorkerRepository();
    private final LocalSalaryAdvanceRepository advanceRepository = new LocalSalaryAdvanceRepository();
    private Runnable onSaved;

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
    void onSave(ActionEvent event) {
        Worker worker = cmbWorker.getValue();
        LocalDate date = datePicker.getValue();
        
        // Get owner window for proper dialog parenting
        javafx.stage.Window owner = null;
        if (btnSave != null && btnSave.getScene() != null) {
            owner = btnSave.getScene().getWindow();
        }
        
        if (worker == null || date == null) {
            AlertUtil.showError(owner, "Missing data", "Please select a worker and date.");
            return;
        }
        double amount = parseAmount(txtAmount.getText());
        if (amount <= 0) {
            AlertUtil.showError(owner, "Invalid amount", "Please enter a valid amount.");
            return;
        }
        String note = txtNote.getText().trim();
        String id = advanceRepository.saveAdvance(worker.getId(), worker.getName(), date, amount, note);
        String payload = JsonUtil.obj(
                JsonUtil.field("id", id),
                JsonUtil.field("workerId", worker.getId()),
                JsonUtil.field("worker", worker.getName()),
                JsonUtil.field("date", date.toString()),
                JsonUtil.field("amount", amount),
                JsonUtil.field("note", note),
                JsonUtil.field("op", "create")
        );        if (onSaved != null) {
            onSaved.run();
        }
        closeDialog();
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
