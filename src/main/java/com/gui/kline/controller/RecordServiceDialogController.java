package com.gui.kline.controller;

import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

import java.time.LocalDate;

public class RecordServiceDialogController {

    @FXML private TextField    txtRemark;
    @FXML private TextField   txtServiceName;
    @FXML private TextField   txtPrice;
    @FXML private DatePicker  datePicker;

    @FXML
    public void initialize() {
        datePicker.setValue(LocalDate.now());
    }

    @FXML
    private void onSave() {
        String remark  = txtRemark.getText().trim();
        String name    = txtServiceName.getText().trim();
        String price   = txtPrice.getText().trim();
        LocalDate date = datePicker.getValue();

        System.out.println("Saving service: " + name + " | Rs." + price + " | " + date);

        closeDialog();
    }

    @FXML
    private void onCancel() {
        closeDialog();
    }

    private void closeDialog() {
        txtRemark.getScene().getWindow().hide();
    }
}