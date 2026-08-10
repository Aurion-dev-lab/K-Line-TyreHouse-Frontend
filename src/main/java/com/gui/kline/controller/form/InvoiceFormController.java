package com.gui.kline.controller.form;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gui.kline.data.LocalCatalogRepository;
import com.gui.kline.data.LocalInvoiceRepository;
import com.gui.kline.models.dto.InvoiceDetail;
import com.gui.kline.models.dto.InvoiceRow;
import com.gui.kline.models.dto.LineItem;
import com.gui.kline.models.Product;

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

public class InvoiceFormController {

    @FXML private Label              lblTitle;
    @FXML private Label              lblInvoiceId;
    @FXML private TextField          txtCustomerName;
    @FXML private TextField          txtPhone;
    @FXML private TextField          txtVehicleNumber;
    @FXML private VBox               vboxVehicleNumber;
    @FXML private ComboBox<String>   cmbInvoiceType;
    @FXML private VBox               vboxProductSection;
    @FXML private VBox               vboxServiceSection;
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
    @FXML private VBox               vboxBillingExtras;
    @FXML private Button             btnAddItem;
    @FXML private Button             btnCancel;
    @FXML private Button             btnSave;

    private final LocalCatalogRepository catalogRepository = new LocalCatalogRepository();
    private final LocalInvoiceRepository invoiceRepository = new LocalInvoiceRepository();
    private final List<LineItem> lineItems = new ArrayList<>();
    private final ObservableList<Product> allProducts = FXCollections.observableArrayList();
    private final FilteredList<Product> filteredProducts = new FilteredList<>(allProducts, product -> true);
    private String editInvoiceId = null;
    private InvoiceDetail originalDetail = null;

    @FXML
    public void initialize() {
        lblInvoiceId.setText(com.gui.kline.utils.Utils.generateId("INV-", 8));
        cmbInvoiceType.getItems().setAll("Sale", "Service");
        cmbInvoiceType.getSelectionModel().selectFirst();
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
        if ("Service".equals(selected)) {
            showServiceField();
        } else {
            showSaleField();
        }
    }

    private void showSaleField() {
        if (vboxProductSection != null) {
            vboxProductSection.setVisible(true);
            vboxProductSection.setManaged(true);
            vboxProductSection.setDisable(false);
        }
        if (vboxServiceSection != null) {
            vboxServiceSection.setVisible(false);
            vboxServiceSection.setManaged(false);
            vboxServiceSection.setDisable(true);
        }
        // Hide Labour and Additional Parts for Sale invoices
        if (vboxBillingExtras != null) {
            vboxBillingExtras.setVisible(false);
            vboxBillingExtras.setManaged(false);
        }
        // Hide Vehicle Number for Sale invoices
        if (vboxVehicleNumber != null) {
            vboxVehicleNumber.setVisible(false);
            vboxVehicleNumber.setManaged(false);
        }
        txtServiceDesc.clear();
        txtVehicleNumber.clear();
    }

    private void showServiceField() {
        // In Service mode, show BOTH Service Description AND Product Inventory Selection
        if (vboxServiceSection != null) {
            vboxServiceSection.setVisible(true);
            vboxServiceSection.setManaged(true);
            vboxServiceSection.setDisable(false);
        }
        if (vboxProductSection != null) {
            vboxProductSection.setVisible(true);
            vboxProductSection.setManaged(true);
            vboxProductSection.setDisable(false);
        }
        // Show Labour and Additional Parts for Service invoices
        if (vboxBillingExtras != null) {
            vboxBillingExtras.setVisible(true);
            vboxBillingExtras.setManaged(true);
        }
        // Show Vehicle Number for Service invoices
        if (vboxVehicleNumber != null) {
            vboxVehicleNumber.setVisible(true);
            vboxVehicleNumber.setManaged(true);
        }
    }

    @FXML
    private void handleAddItem() {
        Product productForPrice = cmbProduct.getValue();
        if (productForPrice != null) {
            txtUnitPrice.setText(String.format("%.2f", productForPrice.getSellPrice()));
        }
        if (!validateLineItem()) return;

        String description;
        String productId = null;
        int qty = 1;
        double price = 0;

        try {
            // Product item from inventory selection
            Product selectedProduct = cmbProduct.getValue();
            if (selectedProduct != null) {
                description = formatProductLabel(selectedProduct);
                productId = selectedProduct.getId();

                qty = Integer.parseInt(txtQuantity.getText().trim());
                price = selectedProduct.getSellPrice();
                if (price <= 0) {
                    alert("This product has no selling price. Update its price in Inventory before adding it.");
                    return;
                }
                txtUnitPrice.setText(String.format("%.2f", price));

                // Check if product already exists in lineItems
                LineItem existingItem = lineItems.stream()
                        .filter(item -> selectedProduct.getId().equals(item.getProductId()))
                        .findFirst()
                        .orElse(null);

                if (existingItem != null) {
                    int newQty = existingItem.getQty() + qty;
                    int originalQty = originalDetail == null ? 0 : quantityForProduct(originalDetail.getLineItems(), selectedProduct.getId());
                    int available = selectedProduct.getStock() + originalQty;
                    if (newQty > available) {
                        showStockWarning("Stock exceeded. Only " + Math.max(0, available) + " unit(s) available.");
                        return;
                    }
                    existingItem.setQty(newQty);
                    
                    // Refresh display list
                    vboxLineItems.getChildren().clear();
                    for (LineItem item : lineItems) {
                        addLineItemToDisplay(item);
                    }
                } else {
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

        String invoiceId = lblInvoiceId.getText().trim();
        String customerName = getCustomerName();
        String selectedType = cmbInvoiceType.getValue();
        if (selectedType == null || selectedType.isBlank()) {
            selectedType = "Sale";
        }

        InvoiceDetail detail = new InvoiceDetail();
        detail.setInvoiceId(invoiceId);
        detail.setCustomer(customerName);
        detail.setPhone(txtPhone.getText() != null ? txtPhone.getText().trim() : "");
        detail.setDescription(txtServiceDesc.getText() != null ? txtServiceDesc.getText().trim() : "");
        // Save vehicle number only for Service invoice, clear for Sale
        if ("Service".equals(selectedType)) {
            detail.setVehicleNumber(txtVehicleNumber.getText() != null ? txtVehicleNumber.getText().trim() : "");
        } else {
            detail.setVehicleNumber("");
        }
        detail.setDate(LocalDate.now().toString());
        detail.setType(selectedType);
        detail.setStatus("quotation");
        detail.setDiscountAmount(parse(txtDiscount.getText()));

        for (LineItem item : lineItems) {
            detail.addLineItem(item);
        }
        addBillingCharges(detail);

        try {
            double grandTotal = detail.getGrandTotal();

            InvoiceRow row = new InvoiceRow(invoiceId, LocalDate.now().toString(),
                    customerName, selectedType, detail.getLineItems().size(), grandTotal, "quotation", detail.getPhone(), detail.getDescription(), detail.getVehicleNumber());

            invoiceRepository.saveInvoice(detail, row);
            updateStockForSavedQuotation(detail);

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

    private boolean validate() {
        if (getCustomerName().isBlank()) {
            alert("Customer name required");
            return false;
        }

        if (txtPhone.getText().trim().isBlank()) {
            alert("Phone number required");
            return false;
        }

        if ("Service".equals(cmbInvoiceType.getValue()) && txtServiceDesc.getText().trim().isBlank()) {
            alert("Service description required");
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

    private void addBillingCharges(InvoiceDetail detail) {
        double labour = parse(txtLabour.getText());
        if (labour > 0) {
            detail.addLineItem(new LineItem("Labour", "Service", 1, labour, "Labour"));
        }
        double extraParts = parse(txtParts.getText());
        if (extraParts > 0) {
            detail.addLineItem(new LineItem("Additional parts", "Service", 1, extraParts, "Additional parts"));
        }
    }

    public void setEditMode(String invoiceId, InvoiceDetail detail) {
        this.editInvoiceId = invoiceId;
        this.originalDetail = detail;
        lblInvoiceId.setText(invoiceId);
        if (lblTitle != null) {
            lblTitle.setText("Edit Quotation");
        }
        if (btnSave != null) {
            btnSave.setText("Update Quotation");
        }

        if (detail != null) {
            txtCustomerName.setText(detail.getCustomer());
            txtPhone.setText(detail.getPhone());
            txtVehicleNumber.setText(detail.getVehicleNumber());
            
            // Set invoice type from existing data
            if (detail.getType() != null && !detail.getType().isBlank()) {
                cmbInvoiceType.setValue(detail.getType());
            }
            
            // Load description
            if ("Service".equals(detail.getType())) {
                txtServiceDesc.setText(detail.getDescription());
            }

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
        return txtCustomerName != null && txtCustomerName.getText() != null ? txtCustomerName.getText().trim() : "";
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
