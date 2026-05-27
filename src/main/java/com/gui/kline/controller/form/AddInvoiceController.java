package com.gui.kline.controller.form;

import com.gui.kline.data.LocalCatalogRepository;
import com.gui.kline.data.LocalInvoiceRepository;
import com.gui.kline.data.SyncQueueRepository;
import com.gui.kline.models.InvoiceDetail;
import com.gui.kline.models.InvoiceRow;
import com.gui.kline.models.LineItem;
import com.gui.kline.models.Product;
import com.gui.kline.utils.JsonUtil;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddInvoiceController {

    @FXML private Label              lblInvoiceId;
    @FXML private ComboBox<String>   cmbCustomerName;
    @FXML private TextField          txtPhone;
    @FXML private TextField          txtVehicleNumber;
    @FXML private ComboBox<String>   cmbInvoiceType;
    @FXML private Label              lblDynamicField;
    @FXML private ComboBox<String>   cmbProduct;
    @FXML private TextField          txtServiceDesc;
    @FXML private TextField          txtQuantity;
    @FXML private TextField          txtUnitPrice;
    @FXML private Label              lblProductStock;
    @FXML private TextField          txtLabour;
    @FXML private TextField          txtParts;
    @FXML private TextField          txtDiscount;
    @FXML private Label              lblTotal;
    @FXML private VBox               vboxLineItems;
    @FXML private Button             btnAddItem;
    @FXML private Button             btnCancel;
    @FXML private Button             btnSave;

    private final SyncQueueRepository syncQueueRepository = new SyncQueueRepository();
    private final LocalCatalogRepository catalogRepository = new LocalCatalogRepository();
    private final LocalInvoiceRepository invoiceRepository = new LocalInvoiceRepository();
    private final Map<String, Product> productMap = new HashMap<>();
    private final List<LineItem> lineItems = new ArrayList<>();
    private String editInvoiceId = null;
    private InvoiceDetail originalDetail = null;

    @FXML
    public void initialize() {
        lblInvoiceId.setText("#INV-" + System.currentTimeMillis() % 100000);
        cmbInvoiceType.getItems().addAll("Sale", "Service", "Both");
        cmbInvoiceType.getSelectionModel().selectFirst();
        cmbCustomerName.setEditable(true);
        cmbCustomerName.getItems().setAll(catalogRepository.getCustomerNames());
        cmbProduct.getItems().setAll(catalogRepository.getProductNames());
        
        loadProductData();
        showSaleField();
        
        txtQuantity.setText("1");
        txtUnitPrice.setText("0");
        
        // Update lines
        cmbInvoiceType.valueProperty().addListener((o, old, v) -> handleTypeChange());
        cmbProduct.valueProperty().addListener((o, old, v) -> updateProductStock());
        txtLabour.textProperty().addListener((o, old, v) -> recalculate());
        txtParts.textProperty().addListener((o, old, v) -> recalculate());
        txtDiscount.textProperty().addListener((o, old, v) -> recalculate());
    }

    private void loadProductData() {
        productMap.clear();
        for (Product p : catalogRepository.loadProducts()) {
            productMap.put(p.getName(), p);
        }
    }

    private void updateProductStock() {
        String selected = cmbProduct.getValue();
        if (selected != null && productMap.containsKey(selected)) {
            Product p = productMap.get(selected);
            lblProductStock.setText("Stock: " + p.getStock() + " units");
        } else {
            lblProductStock.setText("Stock: —");
        }
    }

    @FXML
    private void handleTypeChange() {
        String selected = cmbInvoiceType.getValue();
        if ("Sale".equals(selected)) {
            showSaleField();
        } else if ("Service".equals(selected)) {
            showServiceField();
        } else if ("Both".equals(selected)) {
            // Show both options
            cmbProduct.setVisible(true);
            cmbProduct.setManaged(true);
            lblProductStock.setVisible(true);
            lblProductStock.setManaged(true);
            txtServiceDesc.setVisible(true);
            txtServiceDesc.setManaged(true);
        }
    }

    private void showSaleField() {
        lblDynamicField.setText("SELECT PRODUCT");
        cmbProduct.setVisible(true);
        cmbProduct.setManaged(true);
        lblProductStock.setVisible(true);
        lblProductStock.setManaged(true);
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
        lblProductStock.setVisible(false);
        lblProductStock.setManaged(false);
        cmbProduct.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleAddItem() {
        if (!validateLineItem()) return;

        String type = cmbInvoiceType.getValue();
        String description;
        String productId = null;
        int qty = 1;
        double price = 0;

        try {
            if ("Sale".equals(type) || "Both".equals(type)) {
                if (cmbProduct.getValue() != null && !cmbProduct.getValue().isBlank()) {
                    String productName = cmbProduct.getValue();
                    description = productName;
                    Product p = productMap.get(productName);
                    productId = p != null ? p.getId() : null;

                    qty = Integer.parseInt(txtQuantity.getText().trim());
                    price = Double.parseDouble(txtUnitPrice.getText().trim());

                    if (p != null && p.getStock() < qty) {
                        alert("Insufficient stock. Available: " + p.getStock());
                        return;
                    }

                    LineItem item = new LineItem(description, "Sale", qty, price, productId);
                    lineItems.add(item);
                    addLineItemToDisplay(item);
                }
            }

            if ("Service".equals(type) || ("Both".equals(type) && !txtServiceDesc.getText().isBlank())) {
                String serviceDesc = txtServiceDesc.getText().trim();
                if (!serviceDesc.isBlank()) {
                    double servicePrice = Double.parseDouble(txtUnitPrice.getText().trim());
                    LineItem item = new LineItem(serviceDesc, "Service", 1, servicePrice, null);
                    lineItems.add(item);
                    addLineItemToDisplay(item);
                }
            }

            clearItemInputs();
            updateLineItemsTotals();
        } catch (NumberFormatException e) {
            alert("Invalid quantity or price");
        }
    }

    private void addLineItemToDisplay(LineItem item) {
        HBox row = new HBox(10);
        row.setPadding(new Insets(8));
        row.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 6; -fx-background-color: #f9f9f9;");

        VBox description = new VBox(2);
        Label title = new Label(item.getDescription());
        title.setStyle("-fx-font-weight: bold;");
        Label detail = new Label(item.getType() + " • Qty: " + item.getQty() + " @ Rs. " + String.format("%.2f", item.getUnitPrice()));
        detail.setStyle("-fx-font-size: 10; -fx-text-fill: #666;");
        description.getChildren().addAll(title, detail);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label amount = new Label("Rs. " + String.format("%.2f", item.getTotal()));
        amount.setStyle("-fx-font-weight: bold; -fx-text-fill: #00c853;");

        Button btnRemove = new Button("Remove");
        btnRemove.setStyle("-fx-padding: 4 8 4 8; -fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-font-size: 10;");
        btnRemove.setOnAction(e -> {
            lineItems.remove(item);
            vboxLineItems.getChildren().remove(row);
            updateLineItemsTotals();
        });

        row.getChildren().addAll(description, spacer, amount, btnRemove);
        vboxLineItems.getChildren().add(row);
    }

    private void clearItemInputs() {
        cmbProduct.getSelectionModel().clearSelection();
        txtServiceDesc.clear();
        txtQuantity.setText("1");
        txtUnitPrice.clear();
    }

    private void updateLineItemsTotals() {
        double sum = lineItems.stream().mapToDouble(LineItem::getTotal).sum();
        double discount = parse(txtDiscount.getText());
        double total = sum - (sum * discount / 100.0);
        lblTotal.setText(String.format("%.2f", Math.max(0, total)));
    }

    private void recalculate() {
        updateLineItemsTotals();
    }

    private boolean validateLineItem() {
        try {
            int qty = Integer.parseInt(txtQuantity.getText().trim());
            double price = Double.parseDouble(txtUnitPrice.getText().trim());
            if (qty <= 0 || price < 0) {
                alert("Quantity must be > 0 and price >= 0");
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            alert("Invalid quantity or price");
            return false;
        }
    }

    @FXML
    private void handleSave() {
        if (!validate()) return;

        String invoiceId = lblInvoiceId.getText().replace("#", "");
        String customerName = getCustomerName();

        InvoiceDetail detail = new InvoiceDetail();
        detail.setInvoiceId(invoiceId);
        detail.setCustomer(customerName);
        detail.setDate(LocalDate.now().toString());
        detail.setType("Mixed");

        for (LineItem item : lineItems) {
            detail.addLineItem(item);
        }

        try {
            // If editing, restore old inventory
            if (editInvoiceId != null && originalDetail != null) {
                restoreInventory(originalDetail);
            }

            // Save invoice
            String displayType = lineItems.isEmpty() ? "Mixed" :
                    lineItems.stream().map(LineItem::getType).distinct().count() == 1 ?
                    lineItems.get(0).getType() : "Mixed";

            double grandTotal = lineItems.stream().mapToDouble(LineItem::getTotal).sum();
            double discount = parse(txtDiscount.getText());
            grandTotal = grandTotal - (grandTotal * discount / 100.0);

            InvoiceRow row = new InvoiceRow(invoiceId, LocalDate.now().toString(),
                    customerName, displayType, lineItems.size(), grandTotal);

            invoiceRepository.saveInvoice(detail, row);

            // Deduct inventory
            deductInventoryForLineItems(detail);

            // Enqueue
            enqueueInvoice(row, detail);

            catalogRepository.saveCustomer(customerName, txtPhone.getText().trim());

            showSuccess("Invoice " + (editInvoiceId != null ? "updated" : "created") + " successfully");
            closeDialog();
        } catch (Exception ex) {
            showError("Error: " + ex.getMessage());
        }
    }

    private void deductInventoryForLineItems(InvoiceDetail detail) {
        for (LineItem item : detail.getLineItems()) {
            if ("Sale".equals(item.getType()) && item.getProductId() != null) {
                Product product = catalogRepository.findProductById(item.getProductId());
                if (product != null) {
                    int newStock = product.getStock() - item.getQty();
                    if (newStock < 0) {
                        throw new IllegalStateException("Insufficient stock for " + item.getDescription());
                    }
                    product.setStock(newStock);
                    catalogRepository.saveProduct(product);

                    String payload = JsonUtil.obj(
                            JsonUtil.field("operation", "update"),
                            JsonUtil.field("productId", product.getId()),
                            JsonUtil.field("name", product.getName()),
                            JsonUtil.field("category", product.getCategory()),
                            JsonUtil.field("buyPrice", product.getBuyPrice()),
                            JsonUtil.field("sellPrice", product.getSellPrice()),
                            JsonUtil.field("stock", product.getStock())
                    );
                    syncQueueRepository.enqueue("product", payload);
                }
            }
        }
    }

    private void restoreInventory(InvoiceDetail detail) {
        for (LineItem item : detail.getLineItems()) {
            if ("Sale".equals(item.getType()) && item.getProductId() != null) {
                Product product = catalogRepository.findProductById(item.getProductId());
                if (product != null) {
                    product.setStock(product.getStock() + item.getQty());
                    catalogRepository.saveProduct(product);
                }
            }
        }
    }

    private void enqueueInvoice(InvoiceRow row, InvoiceDetail detail) {
        String[] items = detail.getLineItems().stream()
                .map(item -> JsonUtil.obj(
                        JsonUtil.field("description", item.getDescription()),
                        JsonUtil.field("type", item.getType()),
                        JsonUtil.field("qty", item.getQty()),
                        JsonUtil.field("unitPrice", item.getUnitPrice()),
                        JsonUtil.field("total", item.getTotal())
                ))
                .toArray(String[]::new);

        String payload = JsonUtil.obj(
                JsonUtil.field("invoiceId", row.getInvoiceId()),
                JsonUtil.field("date", row.getDate()),
                JsonUtil.field("customer", detail.getCustomer()),
                JsonUtil.field("phone", txtPhone.getText().trim()),
                JsonUtil.field("vehicle", txtVehicleNumber.getText().trim()),
                JsonUtil.field("type", row.getType()),
                JsonUtil.field("itemCount", detail.getLineItems().size()),
                JsonUtil.field("subtotal", detail.getSubtotal()),
                JsonUtil.field("tax", detail.getTax()),
                JsonUtil.field("grandTotal", detail.getGrandTotal()),
                JsonUtil.fieldRaw("items", JsonUtil.array(items))
        );
        syncQueueRepository.enqueue("invoice", payload);
    }

    private boolean validate() {
        if (getCustomerName().isBlank()) {
            alert("Customer name required");
            return false;
        }
        if (txtPhone.getText().isBlank()) {
            alert("Phone number required");
            return false;
        }
        if (txtVehicleNumber.getText().isBlank()) {
            alert("Vehicle number required");
            return false;
        }
        if (lineItems.isEmpty()) {
            alert("Add at least one item");
            return false;
        }
        return true;
    }

    public void setEditMode(String invoiceId, InvoiceDetail detail) {
        this.editInvoiceId = invoiceId;
        this.originalDetail = detail;
        lblInvoiceId.setText("#" + invoiceId);

        if (detail != null) {
            for (LineItem item : detail.getLineItems()) {
                lineItems.add(item);
                addLineItemToDisplay(item);
            }
            updateLineItemsTotals();
        }
    }

    private double parse(String text) {
        try {
            return text == null || text.isBlank() ? 0.0 : Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private String getCustomerName() {
        String value = cmbCustomerName.getValue();
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        String typed = cmbCustomerName.getEditor().getText();
        return typed == null ? "" : typed.trim();
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void alert(String msg) {
        new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait();
    }

    private void showSuccess(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    private void closeDialog() {
        ((Stage) btnCancel.getScene().getWindow()).close();
    }
}

