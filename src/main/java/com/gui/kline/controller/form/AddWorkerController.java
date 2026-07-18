package com.gui.kline.controller.form;

import com.gui.kline.data.LocalWorkerRepository;
import com.gui.kline.utils.JsonUtil;
import com.gui.kline.models.Worker;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class AddWorkerController {

    @FXML
    private Button btnCancel;

    @FXML
    private Button btnSave;

    @FXML
    private ComboBox<String> cmbSalaryType;

    @FXML
    private TextField txtName;

    @FXML
    private TextField txtPhone;

    @FXML
    private TextField txtRate;

    @FXML
    private TextField txtRole;

    @FXML
    private Label lblTitle;    private final LocalWorkerRepository workerRepository = new LocalWorkerRepository();
    private Runnable onSaved;

    private String workerId;
    private boolean editMode = false;

    @FXML
    void handleCancel(ActionEvent event) {
        closeDialog();
    }

    @FXML
    public void initialize() {
        cmbSalaryType.setItems(FXCollections.observableArrayList("Daily", "Monthly"));
    }

    @FXML
    void handleSave(ActionEvent event) {
        String name = txtName.getText().trim();
        String phone = txtPhone.getText().trim();
        String role = txtRole.getText().trim();
        String rate = txtRate.getText().trim();
        String salaryType = cmbSalaryType.getValue();
        String payload = JsonUtil.obj(
                JsonUtil.field("id", workerId),
                JsonUtil.field("name", name),
                JsonUtil.field("phone", phone),
                JsonUtil.field("role", role),
                JsonUtil.field("rate", rate),
                JsonUtil.field("salaryType", salaryType),
                JsonUtil.field("op", editMode ? "update" : "create")
        );
        if (editMode) {
            workerRepository.updateWorker(workerId, name, phone, role, rate, salaryType);
        } else {
            workerRepository.saveWorker(name, phone, role, rate, salaryType);
        }        if (onSaved != null) {
            onSaved.run();
        }
        closeDialog();
    }

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    public void setEditMode(Worker worker) {
        if (worker == null) {
            return;
        }
        this.workerId = worker.getId();
        this.editMode = true;
        txtName.setText(worker.getName());
        txtPhone.setText(worker.getPhone());
        txtRole.setText(worker.getRole());
        txtRate.setText(worker.getRate());
        cmbSalaryType.setValue(worker.getSalaryType());
        lblTitle.setText("Edit Worker");
        btnSave.setText("Update Worker");
    }

    private void closeDialog() {
        btnCancel.getScene().getWindow().hide();
    }
}
