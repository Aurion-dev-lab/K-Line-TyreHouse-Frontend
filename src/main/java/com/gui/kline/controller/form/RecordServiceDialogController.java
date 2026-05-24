package com.gui.kline.controller.form;

import com.gui.kline.data.SyncQueueRepository;
import com.gui.kline.utils.JsonUtil;

import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

import java.time.LocalDate;

public class RecordServiceDialogController {

    @FXML private TextField    txtRemark;
    @FXML private TextField   txtServiceName;
    @FXML private TextField   txtPrice;
    @FXML private DatePicker  datePicker;

    private final SyncQueueRepository syncQueueRepository = new SyncQueueRepository();

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

        String payload = JsonUtil.obj(
                JsonUtil.field("service", name),
                JsonUtil.field("remark", remark),
                JsonUtil.field("price", price),
                JsonUtil.field("date", date != null ? date.toString() : LocalDate.now().toString())
        );
        syncQueueRepository.enqueue("service", payload);


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