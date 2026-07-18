package com.gui.kline.controller.form;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gui.kline.data.LocalCatalogRepository;
import com.gui.kline.data.LocalInvoiceRepository;
import com.gui.kline.models.InvoiceDetail;
import com.gui.kline.models.InvoiceRow;
import com.gui.kline.models.LineItem;
import com.gui.kline.models.Product;
import com.gui.kline.utils.JsonUtil;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AddInvoiceController {

    @FXML private Label              lblInvoiceId;
    @FXML private ComboBox<String>   cmbCustomerName;
    @FXML private TextField          txtPhone;
    @FXML private TextField          txtVehicleNumber;
    @FXML private ComboBox<String>   cmbInvoiceType;
    @FXML private Label              lblDynamicField;
    @FXML private ComboBox<Product>   cmbProduct;
    @FXML private TextField           txtProductSearch;
    @FXML private TextField          txtServiceDesc;
    @FXML private TextField          txtQuantity;
    @FXML private TextField          txtUnitPrice;
    @FXML private Label              lblProductStock;
    @FXML private Label              lblStockError;
    @FXML private TextField          txtLabour;
    @FXML private TextField          txtParts;
    @FXML private TextField          txtDiscount;
    @FXML private Label              lblTotal;
    @FXML private VBox               vboxLineItems;
    @FXML private Button             btnAddItem;
    @FXML private Button             btnCancel;
    @FXML private Button             btnSave;    private final LocalCatalogRepository catalogRepository = new LocalCatalogRepository();
    private final LocalInvoiceRepository invoiceRepository = new LocalInvoiceRepository();
    private final List<LineItem> lineItems = new ArrayList<>();
    private final ObservableList<Product> allProducts = FXCollections.observableArrayList();
    private final FilteredList<Product> filteredProducts = new FilteredList<>(allProducts, product -> true);
    private String editInvoiceId = null;
    private InvoiceDetail originalDetail = null;

    @FXML
    public void initialize() {
        lblInvoiceId.setText("#INV-" + System.currentTimeMillis() % 100000);
        cmbInvoiceType.getItems().addAll("Sale", "Service", "Both");
        cmbInvoiceType.getSelectionModel().selectFirst();
        cmbCustomerName.setEditable(true);
        cmbCustomerName.getItems().setAll(catalogRepository.getCustomerNames());
        loadProductData();
        showSaleField();
        
        txtQuantity.setText("1");
        txtUnitPrice.setText("0");
        
        // Update lines
        cmbInvoiceType.valueProperty().addListener((o, old, v) -> handleTypeChange());
        cmbProduct.valueProperty().addListener((o, old, v) -> updateProductDetails());
        txtProductSearch.textProperty().addListener((o, old, value) -> filterProducts(value));
        txtQuantity.textProperty().addListener((o, old, value) -> updateStockWarning());
        txtLabour.textProperty().addListener((o, old, v) -> recalculate());
        txtParts.textProperty().addListener((o, old, v) -> recalculate());
        txtDiscount.textProperty().addListener((o, old, v) -> recalculate());
    }

    private void loadProductData() {
        allProducts.setAll(catalogRepository.loadProducts());
        cmbProduct.setItems(filteredProducts);
        cmbProduct.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatProductLabel(item));
            }
        });
        cmbProduct.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Select a product code" : formatProductLabel(item));
            }
        });
    }

    private void filterProducts(String searchText) {
        String query = searchText == null ? "" : searchText.trim().toLowerCase();
        filteredProducts.setPredicate(product -> query.isEmpty() ||
                containsIgnoreCase(product.getCode(), query) ||
                containsIgnoreCase(product.getName(), query) ||
                containsIgnoreCase(product.getCategory(), query) ||
                containsIgnoreCase(product.getBrand(), query));
        if (!filteredProducts.contains(cmbProduct.getValue())) {
            cmbProduct.getSelectionModel().clearSelection();
        }
        if (!filteredProducts.isEmpty() && !query.isEmpty()) {
            cmbProduct.show();
        }
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private void updateStockWarning() {
        Product product = cmbProduct.getValue();
        int quantity = parseQuantity(txtQuantity.getText());
        if (product == null || quantity <= 0) {
            clearStockWarning();
            return;
        }
        int available = availableQuantityForAdditionalItem(product.getId(), product.getStock());
        if (quantity > available) {
            showStockWarning("Stock exceeded. Only " + Math.max(0, available) + " unit(s) available.");
        } else {
            clearStockWarning();
        }
    }

    private int availableQuantityForAdditionalItem(String productId, int currentStock) {
        int existingQuantity = quantityForProduct(lineItems, productId);
        int originalQuantity = originalDetail == null ? 0
                : quantityForProduct(originalDetail.getLineItems(), productId);
        return currentStock + originalQuantity - existingQuantity;
    }

    private int quantityForProduct(List<LineItem> items, String productId) {
        return items.stream()
                .filter(item -> productId != null && productId.equals(item.getProductId()))
                .mapToInt(LineItem::getQty)
                .sum();
    }

    private int parseQuantity(String value) {
        try {
            return value == null || value.isBlank() ? 0 : Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void showStockWarning(String message) {
        lblStockError.setText(message);
        lblStockError.setVisible(true);
        lblStockError.setManaged(true);
    }

    private void clearStockWarning() {
        lblStockError.setText("");
        lblStockError.setVisible(false);
        lblStockError.setManaged(false);
    }

    private void updateProductDetails() {
        Product selected = cmbProduct.getValue();
        if (selected != null) {
            Product p = selected;
            lblProductStock.setText("Stock: " + p.getStock() + " units");
            txtUnitPrice.setText(String.format("%.2f", p.getSellPrice()));
        } else {
            lblProductStock.setText("Stock: —");
        }
        updateStockWarning();
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
        txtProductSearch.clear();
    }

    @FXML
    private void handleAddItem() {
        Product productForPrice = cmbProduct.getValue();
        if (productForPrice != null) {
            txtUnitPrice.setText(String.format("%.2f", productForPrice.getSellPrice()));
        }
        if (!validateLineItem()) return;

        String type = cmbInvoiceType.getValue();
        String description;
        String productId = null;
        int qty = 1;
        double price = 0;

        try {
            if ("Sale".equals(type) || "Both".equals(type)) {
                Product selectedProduct = cmbProduct.getValue();
                if (selectedProduct != null) {
                    description = formatProductLabel(selectedProduct);
                    productId = selectedProduct.getId();

                    qty = Integer.parseInt(txtQuantity.getText().trim());
                    // Inventory products must always use their configured selling price.
                    // This prevents a quotation from accidentally adding a part at Rs. 0.
                    price = selectedProduct.getSellPrice();
                    if (price <= 0) {
                        alert("This product has no selling price. Update its price in Inventory before adding it.");
                        return;
                    }
                    txtUnitPrice.setText(String.format("%.2f", price));

                    int available = availableQuantityForAdditionalItem(
                            selectedProduct.getId(), selectedProduct.getStock());
                    if (qty > available) {
                        showStockWarning("Stock exceeded. Only " + Math.max(0, available) + " unit(s) available.");
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
            updateStockWarning();
        });

        row.getChildren().addAll(description, spacer, amount, btnRemove);
        vboxLineItems.getChildren().add(row);
    }

    private void clearItemInputs() {
        cmbProduct.getSelectionModel().clearSelection();
        txtServiceDesc.clear();
        txtQuantity.setText("1");
        txtUnitPrice.clear();
        clearStockWarning();
    }

    private void updateLineItemsTotals() {
        double sum = lineItems.stream().mapToDouble(LineItem::getTotal).sum()
                + parse(txtLabour.getText())
                + parse(txtParts.getText());
        double discount = parse(txtDiscount.getText());
        double total = sum - discount;
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
        detail.setStatus("quotation");
        detail.setDiscountAmount(parse(txtDiscount.getText()));

        for (LineItem item : lineItems) {
            detail.addLineItem(item);
        }
        addBillingCharges(detail);

        try {
            String displayType = lineItems.isEmpty() ? "Mixed" :
                    lineItems.stream().map(LineItem::getType).distinct().count() == 1 ?
                    lineItems.get(0).getType() : "Mixed";

            double grandTotal = detail.getGrandTotal();

            InvoiceRow row = new InvoiceRow(invoiceId, LocalDate.now().toString(),
                    customerName, displayType, lineItems.size(), grandTotal, "quotation");

            invoiceRepository.saveInvoice(detail, row);
            updateStockForSavedQuotation(detail);

            // Enqueue
            enqueueInvoice(row, detail);

            catalogRepository.saveCustomer(customerName, txtPhone.getText().trim());

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
                            JsonUtil.field("productCode", product.getCode()),
                            JsonUtil.field("name", product.getName()),
                            JsonUtil.field("category", product.getCategory()),
                            JsonUtil.field("buyPrice", product.getBuyPrice()),
                            JsonUtil.field("sellPrice", product.getSellPrice()),
                            JsonUtil.field("stock", product.getStock())
                    );                }
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
        );    }

    private boolean validate() {
        if (getCustomerName().isBlank()) {
            alert("Customer name required");
            return false;
        }

        if (editInvoiceId == null && txtVehicleNumber.getText().isBlank()) {
            alert("Vehicle number required");
            return false;
        }
        double labour = parse(txtLabour.getText());
        double extraParts = parse(txtParts.getText());
        if (lineItems.isEmpty() && labour <= 0 && extraParts <= 0) {
            alert("Add at least one item");
            return false;
        }
        if (labour < 0 || extraParts < 0 || parse(txtDiscount.getText()) < 0) {
            alert("Labour, parts, and discount amounts cannot be negative");
            return false;
        }
        double subtotal = lineItems.stream().mapToDouble(LineItem::getTotal).sum()
                + labour + extraParts;
        if (parse(txtDiscount.getText()) > subtotal) {
            alert("Discount cannot be greater than the quotation subtotal");
            return false;
        }
        return validateStockForSave();
    }

    private boolean validateStockForSave() {
        Map<String, Integer> requestedQuantities = new HashMap<>();
        for (LineItem item : lineItems) {
            if (item.getProductId() != null) {
                requestedQuantities.merge(item.getProductId(), item.getQty(), Integer::sum);
            }
        }
        for (Map.Entry<String, Integer> entry : requestedQuantities.entrySet()) {
            Product product = catalogRepository.findProductById(entry.getKey());
            if (product == null) {
                showStockWarning("A selected product is no longer available.");
                return false;
            }
            int originalQuantity = originalDetail == null ? 0
                    : quantityForProduct(originalDetail.getLineItems(), product.getId());
            int available = product.getStock() + originalQuantity;
            if (entry.getValue() > available) {
                showStockWarning("Stock exceeded for " + product.getName() + ". Only "
                        + Math.max(0, available) + " unit(s) available.");
                return false;
            }
        }
        clearStockWarning();
        return true;
    }

    private void updateStockForSavedQuotation(InvoiceDetail detail) {
        if (originalDetail != null) {
            restoreInventory(originalDetail);
        }
        deductInventoryForLineItems(detail);
    }

    /** Adds manual labour and extra parts charges as quote line items. */
    private void addBillingCharges(InvoiceDetail detail) {
        double labour = parse(txtLabour.getText());
        if (labour > 0) {
            detail.addLineItem(new LineItem("Labour", "Service", 1, labour, null));
        }
        double extraParts = parse(txtParts.getText());
        if (extraParts > 0) {
            detail.addLineItem(new LineItem("Additional parts", "Service", 1, extraParts, null));
        }
    }

    public void setEditMode(String invoiceId, InvoiceDetail detail) {
        this.editInvoiceId = invoiceId;
        this.originalDetail = detail;
        lblInvoiceId.setText("#" + invoiceId);

        if (detail != null) {
            cmbCustomerName.setValue(detail.getCustomer());
            cmbCustomerName.getEditor().setText(detail.getCustomer());
            txtPhone.setText(catalogRepository.getCustomerPhone(detail.getCustomer()));
            txtDiscount.setText(String.format("%.2f", detail.getDiscountAmount()));
            lineItems.clear();
            vboxLineItems.getChildren().clear();
            for (LineItem item : detail.getLineItems()) {
                if ("Labour".equals(item.getDescription()) && "Service".equals(item.getType())) {
                    txtLabour.setText(String.format("%.2f", item.getTotal()));
                } else if ("Additional parts".equals(item.getDescription()) && "Service".equals(item.getType())) {
                    txtParts.setText(String.format("%.2f", item.getTotal()));
                } else {
                    lineItems.add(item);
                    addLineItemToDisplay(item);
                }
            }
            updateLineItemsTotals();
            updateStockWarning();
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

    private String formatProductLabel(Product product) {
        String code = product.getCode();
        if (code == null || code.isBlank()) {
            return product.getName();
        }
        return code + " - " + product.getName();
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void alert(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        initOwner(a);
        a.showAndWait();
    }

    private void showSuccess(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        initOwner(a);
        a.showAndWait();
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        initOwner(a);
        a.showAndWait();
    }

    private void initOwner(Alert alert) {
        if (btnCancel != null && btnCancel.getScene() != null
                && btnCancel.getScene().getWindow() != null) {
            alert.initOwner(btnCancel.getScene().getWindow());
        }
    }

    private void closeDialog() {
        ((Stage) btnCancel.getScene().getWindow()).close();
    }
}
