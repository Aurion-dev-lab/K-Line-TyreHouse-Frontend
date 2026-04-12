package com.gui.kline.controller.form;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

public class SettleCreditDialogController {

    @FXML
    private Button btnCancel;

    @FXML
    private Button btnRecord;

    @FXML
    private ComboBox<?> cmbWorker;

    @FXML
    private DatePicker datePicker;

    @FXML
    private TextField txtAmount;

    @FXML
    private TextField txtNote;

    @FXML
    void onCancel(ActionEvent event) {
        closeDialog();
    }

    @FXML
    void onRecord(ActionEvent event) {

    }

    private void closeDialog() {
        btnCancel.getScene().getWindow().hide();
    }

}

