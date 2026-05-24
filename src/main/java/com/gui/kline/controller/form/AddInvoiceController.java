package com.gui.kline.controller.form;

import com.gui.kline.data.LocalCatalogRepository;
import com.gui.kline.data.SyncQueueRepository;
import com.gui.kline.utils.JsonUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class AddInvoiceController {

    @FXML private Label              lblInvoiceId;
    @FXML private ComboBox<String>   cmbCustomerName;
    @FXML private TextField          txtPhone;
    @FXML private TextField          txtVehicleNumber;
    @FXML private ComboBox<String>   cmbInvoiceType;
    @FXML private Label              lblDynamicField;
    @FXML private ComboBox<String>   cmbProduct;
    @FXML private TextField          txtServiceDesc;
    @FXML private TextField          txtLabour;
    @FXML private TextField          txtParts;
    @FXML private TextField          txtDiscount;
    @FXML private Label              lblTotal;
    @FXML private Button             btnCancel;
    @FXML private Button             btnSave;

    private final SyncQueueRepository syncQueueRepository = new SyncQueueRepository();
    private final LocalCatalogRepository catalogRepository = new LocalCatalogRepository();

    @FXML
    public void initialize() {
        lblInvoiceId.setText("#INV-" + String.format("%04d", (int)(Math.random() * 9000 + 1000)));
        cmbInvoiceType.getItems().addAll("Sale", "Service");
        cmbInvoiceType.getSelectionModel().selectFirst();
        cmbCustomerName.setEditable(true);
        cmbCustomerName.getItems().setAll(catalogRepository.getCustomerNames());
        cmbProduct.getItems().setAll(catalogRepository.getProductNames());
        showSaleField();

        txtLabour.textProperty().addListener((o, old, v)   -> recalculate());
        txtParts.textProperty().addListener((o, old, v)    -> recalculate());
        txtDiscount.textProperty().addListener((o, old, v) -> recalculate());
    }

    @FXML
    private void handleTypeChange() {
        String selected = cmbInvoiceType.getValue();
        if ("Sale".equals(selected)) {
            showSaleField();
        } else if ("Service".equals(selected)) {
            showServiceField();
        }
    }

    private void showSaleField() {
        lblDynamicField.setText("SELECT PRODUCT");

        cmbProduct.setVisible(true);
        cmbProduct.setManaged(true);

        txtServiceDesc.setVisible(false);
        txtServiceDesc.setManaged(false);
        txtServiceDesc.clear();
    }

    private void showServiceField() {
        lblDynamicField.setText("SERVICE DESCRIPTION");

        txtServiceDesc.setVisible(true);
        txtServiceDesc.setManaged(true);

        cmbProduct.setVisible(false);
        cmbProduct.setManaged(false);
        cmbProduct.getSelectionModel().clearSelection();
    }

    private void recalculate() {
        try {
            double labour   = parse(txtLabour.getText());
            double parts    = parse(txtParts.getText());
            double discount = parse(txtDiscount.getText());
            double sub      = labour + parts;
            double total    = sub - (sub * discount / 100.0);
            lblTotal.setText(String.format("%.2f", Math.max(0, total)));
        } catch (NumberFormatException ignored) {
            lblTotal.setText("0.00");
        }
    }

    private double parse(String text) {
        if (text == null || text.isBlank()) return 0.0;
        return Double.parseDouble(text.trim());
    }

    @FXML
    private void handleSave() {
        if (!validate()) return;

        String type = cmbInvoiceType.getValue();
        String detail = "Sale".equals(type) ? cmbProduct.getValue() : txtServiceDesc.getText();
        String customerName = getCustomerName();

        String payload = JsonUtil.obj(
                JsonUtil.field("invoiceId", lblInvoiceId.getText().replace("#", "")),
                JsonUtil.field("date", java.time.LocalDate.now().toString()),
                JsonUtil.field("customer", customerName),
                JsonUtil.field("phone", txtPhone.getText().trim()),
                JsonUtil.field("vehicle", txtVehicleNumber.getText().trim()),
                JsonUtil.field("type", type),
                JsonUtil.field("detail", detail),
                JsonUtil.field("labour", parse(txtLabour.getText())),
                JsonUtil.field("parts", parse(txtParts.getText())),
                JsonUtil.field("discount", parse(txtDiscount.getText())),
                JsonUtil.field("total", parse(lblTotal.getText()))
        );
        syncQueueRepository.enqueue("invoice", payload);
        catalogRepository.saveCustomer(customerName, txtPhone.getText().trim());

        closeDialog();
    }

    @FXML
    private void handleCancel() { closeDialog(); }

    private boolean validate() {
        if (getCustomerName().isBlank())  { alert("Customer name is required.");   return false; }
        if (txtPhone.getText().isBlank())          { alert("Phone number is required.");    return false; }
        if (txtVehicleNumber.getText().isBlank())  { alert("Vehicle number is required.");  return false; }
        if (cmbInvoiceType.getValue() == null)    { alert("Please select an invoice type."); return false; }

        if ("Sale".equals(cmbInvoiceType.getValue()) && cmbProduct.getValue() == null) {
            alert("Please select a product."); return false;
        }
        if ("Service".equals(cmbInvoiceType.getValue()) && txtServiceDesc.getText().isBlank()) {
            alert("Please describe the service."); return false;
        }
        return true;
    }

    private String getCustomerName() {
        String value = cmbCustomerName.getValue();
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        String typed = cmbCustomerName.getEditor().getText();
        return typed == null ? "" : typed.trim();
    }

    private void alert(String msg) {
        new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait();
    }

    private void closeDialog() {
        ((Stage) btnCancel.getScene().getWindow()).close();
    }
}