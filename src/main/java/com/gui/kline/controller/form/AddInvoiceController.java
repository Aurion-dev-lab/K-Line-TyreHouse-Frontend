package com.gui.kline.controller.form;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class AddInvoiceController {

    @FXML private Label              lblInvoiceId;
    @FXML private TextField          txtCustomerName;
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

    @FXML
    public void initialize() {
        lblInvoiceId.setText("#INV-" + String.format("%04d", (int)(Math.random() * 9000 + 1000)));
        cmbInvoiceType.getItems().addAll("Sale", "Service");
        cmbInvoiceType.getSelectionModel().selectFirst();
        showSaleField();
        cmbProduct.getItems().addAll(
                "Engine Oil 5W-30 (1L)",
                "Air Filter",
                "Brake Pads (Set)",
                "Spark Plugs (Set of 4)",
                "Timing Belt Kit",
                "Coolant (1L)",
                "Wiper Blades (Pair)"
        );

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

        System.out.printf("%s Invoice — Customer: %s | Phone: %s | Vehicle: %s | Detail: %s | Total: %s RS.%n",
                type, txtCustomerName.getText(), txtPhone.getText(),
                txtVehicleNumber.getText(), detail, lblTotal.getText());

        closeDialog();
    }

    @FXML
    private void handleCancel() { closeDialog(); }

    private boolean validate() {
        if (txtCustomerName.getText().isBlank())  { alert("Customer name is required.");   return false; }
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

    private void alert(String msg) {
        new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait();
    }

    private void closeDialog() {
        ((Stage) btnCancel.getScene().getWindow()).close();
    }
}