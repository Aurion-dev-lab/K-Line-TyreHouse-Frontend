package com.gui.kline.controller.form;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

public class ProcessSaleController {

    @FXML
    private Button btnCancel;

    @FXML
    private Button btnComplete;

    @FXML
    private ComboBox<?> cmbCategory;

    @FXML
    private ComboBox<?> cmbProduct;

    @FXML
    private DatePicker datePicker;

    @FXML
    private TextField txtQuantity;

    @FXML
    private TextField txtRemark;

    @FXML
    private TextField txtSearchProduct;

    @FXML
    void onCancel(ActionEvent event) {
        closeDialog();
    }

    @FXML
    void onSave(ActionEvent event) {

    }

    private void closeDialog() {
        btnCancel.getScene().getWindow().hide();
    }

}
