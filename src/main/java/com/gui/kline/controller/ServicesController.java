package com.gui.kline.controller;

import com.gui.kline.models.ViewModel;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

public class ServicesController {

    @FXML
    private Button btnRecordService;

    @FXML
    private TableColumn<?, ?> colDate;

    @FXML
    private TableColumn<?, ?> colPrice;

    @FXML
    private TableColumn<?, ?> colRemark;

    @FXML
    private TableColumn<?, ?> colService;

    @FXML
    private DatePicker dpFrom;

    @FXML
    private DatePicker dpTo;

    @FXML
    private FlowPane flowCommonServices;

    @FXML
    private Label lblTotalRevenue;

    @FXML
    private Label lblTotalServices;

    @FXML
    private TableView<?> tblServices;

    @FXML
    private TextField txtFilter;

    @FXML
    void handleDateFilter(ActionEvent event) {

    }

    @FXML
    void handleFilter(KeyEvent event) {

    }

    @FXML
    void handleRecordNewService(ActionEvent event) {
        Stage ownerStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        ViewModel.INSTANCE.getViewsFactory().getForm("form/record-service-dialog", ownerStage);
    }

}
