package com.gui.kline.controller.form;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import com.gui.kline.models.Product;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ProductFormController implements Initializable {

    @FXML private Label            lblTitle;
    @FXML private Label            lblBadge;
    @FXML private TextField        txtProductName;
    @FXML private TextField        txtProductCode;
    @FXML private ComboBox<String> cmbCategory;
    @FXML private TextField        txtBuyPrice;
    @FXML private TextField        txtSellPrice;
    @FXML private TextField        txtQuantity;
    @FXML private Button           btnSubmit;
    @FXML private Button           btnCancel;

    private Product           editingProduct = null;
    private Consumer<Product> onSave;

    public void setProduct(Product product) {
        this.editingProduct = product;
        applyEditMode();
    }

    public void setOnSave(Consumer<Product> onSave) {
        this.onSave = onSave;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cmbCategory.setItems(FXCollections.observableArrayList(
                "Lubricants", "Tyres", "Spare Parts",
                "Batteries", "Filters", "Accessories", "Other"
        ));
        enforceNumeric(txtBuyPrice);
        enforceNumeric(txtSellPrice);
        enforceNumeric(txtQuantity);
    }

    private void applyEditMode() {
        lblTitle.setText("Edit Product");

        lblBadge.setVisible(true);
        lblBadge.setManaged(true);

        btnSubmit.setText("Update Product");
        btnSubmit.setStyle(
                "-fx-background-color: #f97316; -fx-background-radius: 14; " +
                        "-fx-text-fill: white; -fx-font-weight: bold; " +
                        "-fx-font-size: 14px; -fx-cursor: hand;"
        );

        txtProductName.setText(editingProduct.getName());
        txtProductCode.setText(editingProduct.getCode());
        txtProductCode.setDisable(true);
        cmbCategory.setValue(editingProduct.getCategory());
        txtBuyPrice.setText(String.format("%.2f", editingProduct.getBuyPrice()));
        txtSellPrice.setText(String.format("%.2f", editingProduct.getSellPrice()));
        txtQuantity.setText(String.valueOf(editingProduct.getStock()));
    }

    @FXML
    private void handleSubmit() {
        if (!validate()) return;

        if (editingProduct == null) {
            Product newProduct = new Product(
                    txtProductName.getText().trim(),
                    cmbCategory.getValue(),
                    parseDouble(txtBuyPrice),
                    parseDouble(txtSellPrice),
                    parseInt(txtQuantity)
            );
            newProduct.setCode(txtProductCode.getText().trim());
            if (onSave != null) onSave.accept(newProduct);

        } else {
            editingProduct.setName(txtProductName.getText().trim());
            editingProduct.setCode(txtProductCode.getText().trim());
            editingProduct.setCategory(cmbCategory.getValue());
            editingProduct.setBuyPrice(parseDouble(txtBuyPrice));
            editingProduct.setSellPrice(parseDouble(txtSellPrice));
            editingProduct.setStock(parseInt(txtQuantity));
            if (onSave != null) onSave.accept(editingProduct);
        }

        closeStage();
    }

    @FXML
    private void handleCancel() {
        closeStage();
    }

    private boolean validate() {
        StringBuilder errors = new StringBuilder();
        if (txtProductName.getText().trim().isEmpty())
            errors.append("• Product name is required.\n");
        if (txtProductCode.getText().trim().isEmpty())
            errors.append("• Product code is required.\n");
        if (cmbCategory.getValue() == null)
            errors.append("• Please select a category.\n");
        if (txtBuyPrice.getText().trim().isEmpty())
            errors.append("• Buying price is required.\n");
        if (txtSellPrice.getText().trim().isEmpty())
            errors.append("• Selling price is required.\n");
        if (txtQuantity.getText().trim().isEmpty())
            errors.append("• Quantity is required.\n");

        if (!errors.isEmpty()) {
            showError("Validation Error", errors.toString().trim());
            return false;
        }
        if (parseDouble(txtSellPrice) < parseDouble(txtBuyPrice)) {
            showError("Price Error", "Selling price must be greater than buying price.");
            return false;
        }
        return true;
    }

    private void enforceNumeric(TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*\\.?\\d*")) field.setText(oldVal);
        });
    }

    private double parseDouble(TextField field) {
        try { return Double.parseDouble(field.getText().trim()); }
        catch (NumberFormatException e) { return 0.0; }
    }

    private int parseInt(TextField field) {
        try { return Integer.parseInt(field.getText().trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    private void closeStage() {
        ((Stage) btnCancel.getScene().getWindow()).close();
    }

    private void showError(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}