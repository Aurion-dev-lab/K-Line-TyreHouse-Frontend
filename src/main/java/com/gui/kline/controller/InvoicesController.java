package com.gui.kline.controller;

import com.gui.kline.models.InvoiceDetail;
import com.gui.kline.models.InvoiceRow;
import com.gui.kline.models.LineItem;
import com.gui.kline.models.ViewModel;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.util.*;

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
    @FXML private ChoiceBox<String> cboInvType;
    @FXML private ChoiceBox<String> cboInvProduct;
    @FXML private TextField         txtInvService;
    @FXML private TextField         txtInvQty;
    @FXML private TextField         txtInvAmount;
    @FXML private Button            btnAddToInvoice;
    @FXML private Button            btnGenerate;
    @FXML private Button            btnDeselect;
    private final ObservableList<InvoiceRow> invoiceList =
            FXCollections.observableArrayList();

    private InvoiceRow    selectedInvoice      = null;
    private InvoiceDetail currentInvoiceDetail = null;
    private boolean       isEditMode           = false;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupChoiceBoxes();
        setupEventHandlers();
        loadSampleData();
        tblInvoices.setItems(invoiceList);
        disableDetailPanel();
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
                pill.setStyle("-fx-padding: 2 9 2 9; -fx-background-radius: 20; -fx-font-size: 10px; -fx-font-weight: bold;");
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                pill.setText(item);
                if ("Sales".equals(item)) {
                    pill.setStyle(pill.getStyle() + "-fx-background-color: #e8f5e9; -fx-text-fill: #2e7d32;");
                } else {
                    pill.setStyle(pill.getStyle() + "-fx-background-color: #e8eaf6; -fx-text-fill: #283593;");
                }
                setGraphic(pill);
                setText(null);
            }
        });

        colTotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : "Rs. " + String.format("%,.0f", item));
            }
        });

        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("View");
            {
                btn.setStyle("-fx-background-color: transparent; -fx-border-color: rgba(0,0,0,0.12); " +
                        "-fx-border-radius: 6; -fx-font-size: 11px; -fx-text-fill: #666666; " +
                        "-fx-padding: 4 10 4 10; -fx-cursor: hand;");
                btn.setOnAction(e -> onViewInvoice(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void setupChoiceBoxes() {
        cboInvType.setItems(FXCollections.observableArrayList("Sales", "Service"));
        cboInvType.getSelectionModel().selectFirst();
        cboInvProduct.setItems(FXCollections.observableArrayList(
                "Product A", "Product B", "Product C", "Product D"));
    }

    private void setupEventHandlers() {
        tblInvoices.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, nw) -> { if (nw != null) onViewInvoice(nw); });
    }

    @FXML
    private void onNewInvoice(ActionEvent event) {
        Stage ownerStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        ViewModel.INSTANCE.getViewsFactory().getForm("form/add-invoice-dialog", ownerStage);
    }

    @FXML
    private void onSearch(javafx.scene.input.KeyEvent event) {
        String q = txtSearch.getText().toLowerCase().trim();
        tblInvoices.setItems(q.isEmpty() ? invoiceList : invoiceList.filtered(inv ->
                inv.getCustomer().toLowerCase().contains(q) ||
                        inv.getDate().toLowerCase().contains(q)     ||
                        inv.getInvoiceId().toLowerCase().contains(q)));
    }

    private void onViewInvoice(InvoiceRow invoice) {
        selectedInvoice = invoice;
        isEditMode      = true;
        enableDetailPanel();
        loadInvoiceDetail(invoice);
    }

    @FXML
    private void onInvTypeChange(ActionEvent event) {
        updateProductServiceDisplay();
    }

    private void updateProductServiceDisplay() {
        boolean isSales = "Sales".equals(cboInvType.getValue());
        cboInvProduct.setVisible(isSales);
        cboInvProduct.setManaged(isSales);
        txtInvService.setVisible(!isSales);
        txtInvService.setManaged(!isSales);
    }

    @FXML
    private void onAddToInvoice(ActionEvent event) {
        if (currentInvoiceDetail == null) {
            showError("No active invoice. Click '+ New Invoice' first.");
            return;
        }

        String type = cboInvType.getValue();
        String description;

        if ("Sales".equals(type)) {
            String product = cboInvProduct.getValue();
            if (product == null || product.isBlank()) { showError("Please select a product."); return; }
            description = product;
        } else {
            String service = txtInvService.getText().trim();
            if (service.isBlank()) { showError("Please enter a service description."); return; }
            description = service;
        }

        String qtyStr    = txtInvQty.getText().trim();
        String amountStr = txtInvAmount.getText().trim();

        if (qtyStr.isBlank() || amountStr.isBlank()) {
            showError("Please enter quantity and amount.");
            return;
        }

        try {
            int    qty    = Integer.parseInt(qtyStr);
            double amount = Double.parseDouble(amountStr);
            if (qty <= 0 || amount <= 0) { showError("Quantity and amount must be greater than 0."); return; }

            LineItem item = new LineItem(description, type, qty, amount);
            currentInvoiceDetail.addLineItem(item);
            addLineItemToPanel(item);
            clearLineItemInputs();
            updateTotals();

        } catch (NumberFormatException e) {
            showError("Invalid quantity or amount.");
        }
    }
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

    private void clearLineItemInputs() {
        txtInvService.clear();
        txtInvQty.clear();
        txtInvAmount.clear();
        cboInvProduct.getSelectionModel().clearSelection();
    }

    @FXML
    private void onGenerateInvoice(ActionEvent event) {
        if (currentInvoiceDetail == null || currentInvoiceDetail.getLineItems().isEmpty()) {
            showError("Add at least one line item before generating.");
            return;
        }
        String invoiceId = isEditMode ? selectedInvoice.getInvoiceId() : generateInvoiceId();
        String type      = currentInvoiceDetail.getLineItems().get(0).getType();

        InvoiceRow row = new InvoiceRow(invoiceId, LocalDate.now().toString(),
                "Sample Customer", type,
                currentInvoiceDetail.getLineItems().size(),
                currentInvoiceDetail.getGrandTotal());

        if (!isEditMode) invoiceList.add(0, row);
        showSuccess("Invoice " + (isEditMode ? "updated" : "created") + " successfully.");
        onDeselect(event);
    }

    @FXML
    private void onDeselect(ActionEvent event) {
        disableDetailPanel();
        selectedInvoice      = null;
        currentInvoiceDetail = null;
        tblInvoices.getSelectionModel().clearSelection();
        txtSearch.clear();
    }

    private void loadInvoiceDetail(InvoiceRow invoice) {
        currentInvoiceDetail = new InvoiceDetail();
        currentInvoiceDetail.setInvoiceId(invoice.getInvoiceId());
        currentInvoiceDetail.setCustomer("Sample Customer");
        currentInvoiceDetail.setDate(invoice.getDate());
        currentInvoiceDetail.setType(invoice.getType());

        lblInvoiceId.setText("#" + invoice.getInvoiceId());
        lblCustomer.setText(currentInvoiceDetail.getCustomer());
        lblInvoiceDate.setText(invoice.getDate());
        lblInvoiceType.setText(invoice.getType());

        vboxLineItems.getChildren().clear();
        String itemName = "Sales".equals(invoice.getType()) ? "Product A" : "Service A";
        LineItem mock = new LineItem(itemName, invoice.getType(), 2, invoice.getTotal() / 2);
        currentInvoiceDetail.addLineItem(mock);
        addLineItemToPanel(mock);

        updateTotals();
        clearLineItemInputs();
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
        clearLineItemInputs();
    }

    private void enableDetailPanel() {
        rightPanel.setDisable(false);
        rightPanel.setOpacity(1.0);
    }

    private void disableDetailPanel() {
        rightPanel.setDisable(true);
        rightPanel.setOpacity(0.45);
        clearDetailPanel();
    }

    private void updateTotals() {
        if (currentInvoiceDetail == null) return;
        lblSubtotal.setText("Rs. " + String.format("%,.2f", currentInvoiceDetail.getSubtotal()));
        lblTax.setText("Rs. "      + String.format("%,.2f", currentInvoiceDetail.getTax()));
        lblGrandTotal.setText("Rs. "+ String.format("%,.2f", currentInvoiceDetail.getGrandTotal()));
    }

    private String generateInvoiceId() { return "INV" + System.currentTimeMillis(); }

    private void loadSampleData() {
        invoiceList.addAll(
                new InvoiceRow("INV001", "2024-01-15", "John Doe",       "Sales",   3, 15000.00),
                new InvoiceRow("INV002", "2024-01-16", "Jane Smith",     "Service", 2,  8500.00),
                new InvoiceRow("INV003", "2024-01-17", "ABC Corp",       "Sales",   5, 25000.00),
                new InvoiceRow("INV004", "2024-01-18", "XYZ Services",   "Service", 1,  5000.00),
                new InvoiceRow("INV005", "2024-01-19", "Tech Solutions", "Sales",   4, 18000.00)
        );
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