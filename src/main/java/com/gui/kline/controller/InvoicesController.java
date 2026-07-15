package com.gui.kline.controller;

import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import com.gui.kline.data.LocalCatalogRepository;
import com.gui.kline.data.LocalInvoiceRepository;
import com.gui.kline.data.SyncQueueReader;
import com.gui.kline.data.SyncQueueRepository;
import com.gui.kline.models.InvoiceDetail;
import com.gui.kline.models.InvoiceRow;
import com.gui.kline.models.LineItem;
import com.gui.kline.models.Product;
import com.gui.kline.models.ViewModel;
import com.gui.kline.utils.JsonUtil;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class InvoicesController implements Initializable {

    @FXML private Button btnNewInvoice;
    @FXML private TableView<InvoiceRow>            tblInvoices;
    @FXML private TableColumn<InvoiceRow, String>  colDate;
    @FXML private TableColumn<InvoiceRow, String>  colCustomer;
    @FXML private TableColumn<InvoiceRow, String>  colType;
    @FXML private TableColumn<InvoiceRow, Integer> colItems;
    @FXML private TableColumn<InvoiceRow, Double>  colTotal;
    @FXML private TableColumn<InvoiceRow, Void>    colAction;
    @FXML private TextField txtSearch;
    @FXML private VBox  rightPanel;
    @FXML private Label lblInvoiceId;
    @FXML private Label lblCustomer;
    @FXML private Label lblInvoiceDate;
    @FXML private Label lblInvoiceType;
    @FXML private VBox  vboxLineItems;
    @FXML private Label lblSubtotal;
    @FXML private Label lblTax;
    @FXML private Label lblGrandTotal;
    // Removed: cboInvType, cboInvProduct, txtInvService, txtInvQty, txtInvAmount, btnAddToInvoice
    // (These FXML elements were removed from the UI)
    // ...existing code...
    private final ObservableList<InvoiceRow> invoiceList =
            FXCollections.observableArrayList();
    private final SyncQueueRepository syncQueueRepository = new SyncQueueRepository();
    private final LocalCatalogRepository catalogRepository = new LocalCatalogRepository();
    private final LocalInvoiceRepository invoiceRepository = new LocalInvoiceRepository();
    private Map<String, Product> productMap = new HashMap<>();

    private InvoiceRow    selectedInvoice      = null;
    private InvoiceDetail currentInvoiceDetail = null;
    private boolean       isEditMode           = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupEventHandlers();
        loadProductMap();
        loadFromLocal();
        tblInvoices.setItems(invoiceList);
        hideDetailPanel(); // hidden by default, not dimmed
    }

    private void loadProductMap() {
        productMap.clear();
        for (Product p : catalogRepository.loadProducts()) {
            productMap.put(p.getId(), p);
        }
    }

    private void setupTableColumns() {
        colDate.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getDate()));
        colCustomer.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getCustomer()));
        colType.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getType()));
        colItems.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleIntegerProperty(cd.getValue().getItemCount()).asObject());
        colTotal.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleDoubleProperty(cd.getValue().getTotal()).asObject());

        colType.setCellFactory(col -> new TableCell<>() {
            private final Label pill = new Label();
            {
                pill.setStyle("-fx-padding: 2 9 2 9; -fx-background-radius: 20; " +
                        "-fx-font-size: 10px; -fx-font-weight: bold;");
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                pill.setText(item);
                if ("Sales".equals(item)) {
                    pill.setStyle(pill.getStyle() + "-fx-background-color: #dcfce7; -fx-text-fill: #166534;");
                } else {
                    pill.setStyle(pill.getStyle() + "-fx-background-color: #dbeafe; -fx-text-fill: #1e40af;");
                }
                setGraphic(pill);
                setText(null);
            }
        });

        colTotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null :
                        "Rs. " + String.format("%,.0f", item));
            }
        });

        colAction.setCellFactory(param -> new TableCell<>() {
            private final HBox box = new HBox(6);
            private final Button btnView = new Button("View");
            private final Button btnEdit = new Button("Edit");
            private final Button btnDelete = new Button("✕");
            
            {
                btnView.setStyle("-fx-background-color: #3b82f6; -fx-border-color: #1e3a8a; " +
                        "-fx-border-radius: 6; -fx-font-size: 11px; -fx-text-fill: #ffffff; " +
                        "-fx-padding: 4 10 4 10; -fx-cursor: hand; -fx-font-weight: bold;");
                btnEdit.setStyle("-fx-background-color: #f59e0b; -fx-border-color: #b45309; " +
                        "-fx-border-radius: 6; -fx-font-size: 11px; -fx-text-fill: #ffffff; " +
                        "-fx-padding: 4 10 4 10; -fx-cursor: hand; -fx-font-weight: bold;");
                btnDelete.setStyle("-fx-background-color: #ef4444; -fx-border-color: #991b1b; " +
                        "-fx-border-radius: 6; -fx-font-size: 11px; -fx-text-fill: #ffffff; " +
                        "-fx-padding: 4 8 4 8; -fx-cursor: hand; -fx-font-weight: bold;");
                
                btnView.setOnAction(e -> {
                    InvoiceRow row = getTableView().getItems().get(getIndex());
                    onViewInvoice(row);
                });
                btnEdit.setOnAction(e -> {
                    InvoiceRow row = getTableView().getItems().get(getIndex());
                    onEditInvoice(row);
                });
                btnDelete.setOnAction(e -> {
                    InvoiceRow row = getTableView().getItems().get(getIndex());
                    onDeleteInvoice(row);
                });
                
                box.setStyle("-fx-spacing: 6;");
                box.getChildren().addAll(btnView, btnEdit, btnDelete);
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }


    private void setupEventHandlers() {
        // Removed auto-open on selection since we now have View button
    }

    @FXML
    private void onNewInvoice(ActionEvent event) {
        Stage ownerStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        ViewModel.INSTANCE.getViewsFactory().getForm("form/add-invoice-dialog", ownerStage);
    }

    @FXML
    private void onSearch(javafx.scene.input.KeyEvent event) {
        String q = txtSearch.getText().toLowerCase().trim();
        tblInvoices.setItems(q.isEmpty() ? invoiceList :
                invoiceList.filtered(inv ->
                        inv.getCustomer().toLowerCase().contains(q) ||
                                inv.getDate().toLowerCase().contains(q)     ||
                                inv.getInvoiceId().toLowerCase().contains(q)));
    }

    private void onViewInvoice(InvoiceRow invoice) {
        selectedInvoice = invoice;
        isEditMode = false;  // Read-only mode
        showDetailPanel();
        loadInvoiceDetail(invoice);
    }
    
    /**
     * Open edit dialog for invoice
     */
    private void onEditInvoice(InvoiceRow invoice) {
        try {
            selectedInvoice = invoice;
            isEditMode = true;
            
            // Load invoice detail
            InvoiceDetail detail = invoiceRepository.loadInvoiceDetail(invoice.getInvoiceId());
            if (detail == null) {
                showError("Could not load invoice details");
                return;
            }
            
            // Open edit dialog
            Stage ownerStage = (Stage) tblInvoices.getScene().getWindow();
            ViewModel.INSTANCE.getViewsFactory().getForm("form/add-invoice-dialog", ownerStage);
            
            // Note: The dialog will need to support edit mode via initialization
            showSuccess("Edit dialog opened");
        } catch (Exception ex) {
            showError("Error opening edit dialog: " + ex.getMessage());
        }
    }

    // Removed: onInvTypeChange, updateProductServiceDisplay, onAddToInvoice
    // (These methods referenced FXML elements that were removed)
    private void addLineItemToPanel(LineItem item) {
        HBox row = new HBox(10);
        row.getStyleClass().add("line-item-row");

        VBox textBlock = new VBox(2);
        HBox.setHgrow(textBlock, Priority.ALWAYS);

        Label name = new Label(item.getDescription());
        name.getStyleClass().add("li-name");

        Label detail = new Label(
                item.getType() + "  ·  Qty " + item.getQty()
                        + " × Rs. " + String.format("%,.2f", item.getUnitPrice()));
        detail.getStyleClass().add("li-detail");

        textBlock.getChildren().addAll(name, detail);

        Label amount = new Label("Rs. " + String.format("%,.0f", item.getTotal()));
        amount.getStyleClass().add("li-amount");

        Button del = new Button("×");
        del.getStyleClass().add("li-delete");
        del.setOnAction(e -> {
            currentInvoiceDetail.removeLineItem(item);
            vboxLineItems.getChildren().remove(row);
            updateTotals();
        });

        row.getChildren().addAll(textBlock, amount, del);
        vboxLineItems.getChildren().add(row);
    }

    // Removed: clearLineItemInputs() - referenced removed FXML elements

    @FXML
    private void onGenerateInvoice(ActionEvent event) {
        if (currentInvoiceDetail == null ||
                currentInvoiceDetail.getLineItems().isEmpty()) {
            showError("Add at least one line item before generating.");
            return;
        }
        String type      = currentInvoiceDetail.getLineItems().get(0).getType();

        InvoiceRow row = new InvoiceRow(
                currentInvoiceDetail.getInvoiceId(), 
                LocalDate.now().toString(),
                currentInvoiceDetail.getCustomer(), 
                type,
                currentInvoiceDetail.getLineItems().size(),
                currentInvoiceDetail.getGrandTotal()
        );

        if (!isEditMode) {
            invoiceList.add(0, row);
        } else {
            // Update existing row
            int idx = invoiceList.indexOf(selectedInvoice);
            if (idx >= 0) invoiceList.set(idx, row);
        }
        
        // Save invoice
        invoiceRepository.saveInvoice(currentInvoiceDetail, row);
        
        // Only deduct inventory on first creation, not on edit
        if (!isEditMode) {
            deductInventory(currentInvoiceDetail);
        }
        
        enqueueInvoice(row, currentInvoiceDetail);
        showSuccess("Invoice " + (isEditMode ? "updated" : "created") + " successfully.");
        onDeselect(event);
    }

    private void deductInventory(InvoiceDetail detail) {
        for (LineItem item : detail.getLineItems()) {
            if ("Sales".equals(item.getType()) && item.getProductId() != null) {
                Product product = catalogRepository.findProductById(item.getProductId());
                if (product != null) {
                    int newStock = product.getStock() - item.getQty();
                    if (newStock < 0) {
                        showError("Insufficient stock for " + product.getName());
                        return;
                    }
                    product.setStock(newStock);
                    catalogRepository.saveProduct(product);
                    
                    // Enqueue inventory update
                    String payload = JsonUtil.obj(
                            JsonUtil.field("operation", "update"),
                            JsonUtil.field("productId", product.getId()),
                            JsonUtil.field("productCode", product.getCode()),
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

    @FXML
    private void onDeselect(ActionEvent event) {
        hideDetailPanel();
        selectedInvoice      = null;
        currentInvoiceDetail = null;
        tblInvoices.getSelectionModel().clearSelection();
        txtSearch.clear();
        loadProductMap();
        isEditMode           = false;
    }

    /**
     * Delete the currently selected invoice with confirmation
     */
    private void onDeleteInvoice(InvoiceRow invoice) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Delete Invoice");
        confirmDialog.setHeaderText("Are you sure?");
        confirmDialog.setContentText("This will permanently delete invoice #" + invoice.getInvoiceId() + 
                "\n\nInventory will be restored for any products sold.");
        
        if (confirmDialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                // Restore inventory before deletion
                InvoiceDetail detail = invoiceRepository.loadInvoiceDetail(invoice.getInvoiceId());
                if (detail != null) {
                    restoreInventoryFromInvoice(detail);
                }
                
                // Delete invoice
                invoiceRepository.deleteInvoice(invoice.getInvoiceId());
                invoiceList.remove(invoice);
                showSuccess("Invoice deleted and inventory restored successfully");
                onDeselect(null);
            } catch (Exception ex) {
                showError("Failed to delete invoice: " + ex.getMessage());
            }
        }
    }
    
    /**
     * Restore inventory from deleted invoice
     */
    private void restoreInventoryFromInvoice(InvoiceDetail detail) {
        for (LineItem item : detail.getLineItems()) {
            if ("Sale".equals(item.getType()) && item.getProductId() != null) {
                Product product = catalogRepository.findProductById(item.getProductId());
                if (product != null) {
                    int restoredStock = product.getStock() + item.getQty();
                    product.setStock(restoredStock);
                    catalogRepository.saveProduct(product);
                    
                    // Enqueue inventory update
                    String payload = JsonUtil.obj(
                            JsonUtil.field("operation", "update"),
                            JsonUtil.field("productId", product.getId()),
                            JsonUtil.field("productCode", product.getCode()),
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
        loadProductMap();  // Reload product map
    }

    private void loadInvoiceDetail(InvoiceRow invoice) {
        currentInvoiceDetail = new InvoiceDetail();
        currentInvoiceDetail.setInvoiceId(invoice.getInvoiceId());
        currentInvoiceDetail.setCustomer(invoice.getCustomer());
        currentInvoiceDetail.setDate(invoice.getDate());
        currentInvoiceDetail.setType(invoice.getType());

        lblInvoiceId.setText("#" + invoice.getInvoiceId());
        lblCustomer.setText(currentInvoiceDetail.getCustomer());
        lblInvoiceDate.setText(invoice.getDate());
        lblInvoiceType.setText(invoice.getType());

        vboxLineItems.getChildren().clear();
        updateTotalsFromRow(invoice);
    }

    private void clearDetailPanel() {
        lblInvoiceId.setText("#—");
        lblCustomer.setText("—");
        lblInvoiceDate.setText("—");
        lblInvoiceType.setText("—");
        vboxLineItems.getChildren().clear();
        lblSubtotal.setText("Rs. 0.00");
        lblTax.setText("Rs. 0.00");
        lblGrandTotal.setText("Rs. 0.00");
    }

    // ── Show/hide (no dim/glass effect) ──────────────────────────────────────

    private void showDetailPanel() {
        rightPanel.setVisible(true);
        rightPanel.setManaged(true);
        rightPanel.setDisable(false);
        rightPanel.setOpacity(1.0);
    }

    private void hideDetailPanel() {
        rightPanel.setVisible(false);
        rightPanel.setManaged(false);
        clearDetailPanel();
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void updateTotals() {
        if (currentInvoiceDetail == null) return;
        lblSubtotal.setText("Rs. "  + String.format("%,.2f", currentInvoiceDetail.getSubtotal()));
        lblTax.setText("Rs. "       + String.format("%,.2f", currentInvoiceDetail.getTax()));
        lblGrandTotal.setText("Rs. "+ String.format("%,.2f", currentInvoiceDetail.getGrandTotal()));
    }

    private void updateTotalsFromRow(InvoiceRow row) {
        lblSubtotal.setText("Rs. " + String.format("%,.2f", row.getTotal()));
        lblTax.setText("Rs. 0.00");
        lblGrandTotal.setText("Rs. " + String.format("%,.2f", row.getTotal()));
    }

    private String generateInvoiceId() { return "INV" + System.currentTimeMillis(); }

    private void enqueueInvoice(InvoiceRow row, InvoiceDetail detail) {
        if (detail == null) {
            return;
        }
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
                JsonUtil.field("type", row.getType()),
                JsonUtil.field("itemCount", detail.getLineItems().size()),
                JsonUtil.field("subtotal", detail.getSubtotal()),
                JsonUtil.field("tax", detail.getTax()),
                JsonUtil.field("grandTotal", detail.getGrandTotal()),
                JsonUtil.fieldRaw("items", JsonUtil.array(items))
        );

        syncQueueRepository.enqueue("invoice", payload);
    }

    private void loadFromLocal() {
        SyncQueueReader reader = new SyncQueueReader();
        List<InvoiceRow> local = reader.loadInvoices();
        invoiceList.setAll(local);
    }


    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void showSuccess(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Success"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    public static class HistoryRow {
        private final String date, invoiceNo, type, item, status;
        private final int    qty;
        private final double amount;

        public HistoryRow(String date, String invoiceNo, String type,
                          String item, int qty, double amount, String status) {
            this.date = date; this.invoiceNo = invoiceNo; this.type = type;
            this.item = item; this.qty = qty; this.amount = amount; this.status = status;
        }

        public String getDate()      { return date; }
        public String getInvoiceNo() { return invoiceNo; }
        public String getType()      { return type; }
        public String getItem()      { return item; }
        public int    getQty()       { return qty; }
        public double getAmount()    { return amount; }
        public String getStatus()    { return status; }
    }
}