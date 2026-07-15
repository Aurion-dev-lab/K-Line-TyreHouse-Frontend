package com.gui.kline.controller;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import com.gui.kline.data.LocalCatalogRepository;
import com.gui.kline.data.LocalCreditSalesRepository;
import com.gui.kline.data.SyncQueueRepository;
import com.gui.kline.models.CreditSaleDetail;
import com.gui.kline.models.Part;
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
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class CreditSalesController implements Initializable {

    @FXML private Button btnNewCredit;
    @FXML private TableView<CreditSaleRow> tblCreditSales;
    @FXML private TableColumn<CreditSaleRow, String>  colDate;
    @FXML private TableColumn<CreditSaleRow, String>  colCustomer;
    @FXML private TableColumn<CreditSaleRow, String>  colDueDate;
    @FXML private TableColumn<CreditSaleRow, Double>  colAmount;
    @FXML private TableColumn<CreditSaleRow, String>  colStatus;
    @FXML private TableColumn<CreditSaleRow, Void>    colAction;
    @FXML private TextField txtSearch;
    @FXML private VBox  rightPanel;
    @FXML private Label lblCreditId;
    @FXML private Label lblCreditBadge;
    @FXML private Label lblCustomer;
    @FXML private Label lblSaleDate;
    @FXML private VBox  vboxParts;
    @FXML private Label lblSubtotal;
    @FXML private Label lblPaid;
    @FXML private Label lblAmountDue;
    @FXML private ChoiceBox<String> cboPartCategory;
    @FXML private ComboBox<Product> cboProduct;
    @FXML private TextField         txtPartQty;
    @FXML private Button            btnAddPart;
    @FXML private Button            btnSettleCredit;
    @FXML private Button            btnGenerateSale;
    @FXML private Button            btnDeselect;

    private final ObservableList<CreditSaleRow> creditSaleList =
            FXCollections.observableArrayList();
    private final SyncQueueRepository syncQueueRepository = new SyncQueueRepository();
    private final LocalCreditSalesRepository creditSalesRepository = new LocalCreditSalesRepository();
    private final LocalCatalogRepository catalogRepository = new LocalCatalogRepository();

    private CreditSaleRow    selectedSale      = null;
    private CreditSaleDetail currentSaleDetail = null;
    private boolean          isEditMode        = false;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupChoiceBoxes();
        setupEventHandlers();
        loadFromLocal();
        tblCreditSales.setItems(creditSaleList);
        hideDetailPanel(); // hidden by default, not dimmed
    }

    private void setupTableColumns() {
        colDate.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getDate()));
        colCustomer.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getCustomer()));
        colDueDate.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getDueDate()));
        colAmount.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleDoubleProperty(cd.getValue().getAmount()).asObject());
        colStatus.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getStatus()));

        colStatus.setCellFactory(col -> new TableCell<>() {
            private final Label pill = new Label();
            private static final String BASE_STYLE =
                    "-fx-padding: 4 9 4 9; -fx-background-radius: 6; " +
                            "-fx-font-size: 10px; -fx-font-weight: bold;";

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                pill.setText(item);
                switch (item) {
                    case "PENDING" -> pill.setStyle(BASE_STYLE +
                            "-fx-background-color: #fef3c7; -fx-text-fill: #92400e;");
                    case "PARTIAL" -> pill.setStyle(BASE_STYLE +
                            "-fx-background-color: #dbeafe; -fx-text-fill: #1e40af;");
                    case "PAID"    -> pill.setStyle(BASE_STYLE +
                            "-fx-background-color: #dcfce7; -fx-text-fill: #15803d;");
                    default        -> pill.setStyle(BASE_STYLE);
                }
                setGraphic(pill);
                setText(null);
            }
        });

        colAmount.setCellFactory(col -> new TableCell<>() {
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
            private final Button btnSettle = new Button("Settle");
            private final Button btnDelete = new Button("✕");
            
            {
                btnView.setStyle("-fx-background-color: #3b82f6; -fx-border-color: #1e3a8a; " +
                        "-fx-border-radius: 6; -fx-font-size: 11px; -fx-text-fill: #ffffff; " +
                        "-fx-padding: 4 10 4 10; -fx-cursor: hand; -fx-font-weight: bold;");
                btnEdit.setStyle("-fx-background-color: #f59e0b; -fx-border-color: #b45309; " +
                        "-fx-border-radius: 6; -fx-font-size: 11px; -fx-text-fill: #ffffff; " +
                        "-fx-padding: 4 10 4 10; -fx-cursor: hand; -fx-font-weight: bold;");
                btnSettle.setStyle("-fx-background-color: #10b981; -fx-border-color: #047857; " +
                    "-fx-border-radius: 6; -fx-font-size: 11px; -fx-text-fill: #ffffff; " +
                    "-fx-padding: 4 10 4 10; -fx-cursor: hand; -fx-font-weight: bold;");
                btnDelete.setStyle("-fx-background-color: #ef4444; -fx-border-color: #991b1b; " +
                        "-fx-border-radius: 6; -fx-font-size: 11px; -fx-text-fill: #ffffff; " +
                        "-fx-padding: 4 8 4 8; -fx-cursor: hand; -fx-font-weight: bold;");
                
                btnView.setOnAction(e -> onViewCredit(getTableView().getItems().get(getIndex())));
                btnEdit.setOnAction(e -> onEditCredit(getTableView().getItems().get(getIndex())));
                btnSettle.setOnAction(e -> onSettleCredit(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> onDeleteCredit(getTableView().getItems().get(getIndex())));
                
                box.setStyle("-fx-spacing: 6;");
                box.getChildren().addAll(btnView, btnEdit, btnSettle, btnDelete);
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void setupChoiceBoxes() {
        cboPartCategory.setItems(FXCollections.observableArrayList(
                "Engine Parts", "Suspension", "Electrical",
                "Body Parts", "Accessories", "Consumables"));
        cboPartCategory.getSelectionModel().selectFirst();
        
        // Load products for the selected category
        loadProductsForCategory(cboPartCategory.getValue());
        
        cboPartCategory.getSelectionModel().selectedItemProperty().addListener((obs, old, nw) -> {
            if (nw != null) {
                loadProductsForCategory(nw);
            }
        });
    }

    private void loadProductsForCategory(String category) {
        try {
            List<Product> products = catalogRepository.getProductsByCategory(category);
            ObservableList<Product> productList = FXCollections.observableArrayList(products);
            cboProduct.setItems(productList);
            
            // Custom cell factory to display product name and stock
            cboProduct.setCellFactory(param -> new ListCell<>() {
                @Override
                protected void updateItem(Product item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(formatProductLabel(item) + " (Stock: " + item.getStock() + ")");
                    }
                }
            });
            
            cboProduct.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(Product item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText("Select Product");
                    } else {
                        setText(formatProductLabel(item) + " (Stock: " + item.getStock() + ")");
                    }
                }
            });
            
            if (!productList.isEmpty()) {
                cboProduct.getSelectionModel().selectFirst();
            }
        } catch (Exception ex) {
            showError("Failed to load products: " + ex.getMessage());
        }
    }

    private void setupEventHandlers() {
        tblCreditSales.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, nw) -> { if (nw != null) onViewCredit(nw); });
    }

     @FXML
     private void onNewCredit(ActionEvent event) {
         Stage ownerStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
         ViewModel.INSTANCE.getViewsFactory().getForm("form/credit-sale-dialog", ownerStage);
         
         // Add a listener to refresh table when dialog closes
         Stage dialogStage = ViewModel.INSTANCE.getViewsFactory().getLastDialogStage();
         if (dialogStage != null) {
             dialogStage.setOnHidden(e -> {
                 // Reload credit sales from database
                 loadFromLocal();
             });
         }
     }

    @FXML
    private void onSearch(javafx.scene.input.KeyEvent event) {
        String q = txtSearch.getText().toLowerCase().trim();
        tblCreditSales.setItems(q.isEmpty() ? creditSaleList :
                creditSaleList.filtered(sale ->
                        sale.getCustomer().toLowerCase().contains(q) ||
                                sale.getDate().toLowerCase().contains(q)     ||
                                sale.getCreditId().toLowerCase().contains(q)));
    }

    private void onViewCredit(CreditSaleRow sale) {
        selectedSale = sale;
        isEditMode = false;
        showDetailPanel();
        loadSaleDetail(sale);
    }
    
    private void onEditCredit(CreditSaleRow sale) {
        selectedSale = sale;
        isEditMode = true;
        showDetailPanel();
        loadSaleDetail(sale);
    }

    private void onSettleCredit(CreditSaleRow sale) {
        selectedSale = sale;
        showDetailPanel();
        loadSaleDetail(sale);
        settleSelectedCredit();
    }
    
    private void onDeleteCredit(CreditSaleRow sale) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Credit Sale?");
        confirm.setContentText("Are you sure you want to delete credit sale #" + sale.getCreditId() + "?");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                creditSalesRepository.deleteCreditSale(sale.getCreditId());
                creditSaleList.remove(sale);
                showSuccess("Credit sale deleted successfully");
                onDeselect();
            } catch (Exception ex) {
                showError("Failed to delete: " + ex.getMessage());
            }
        }
    }

    @FXML
    private void onSettleCredit(ActionEvent event) {
        settleSelectedCredit();
    }

    private void settleSelectedCredit() {
        if (selectedSale == null || currentSaleDetail == null) {
            showError("Select a credit sale first.");
            return;
        }

        double amountDue = currentSaleDetail.getAmountDue();
        if (amountDue <= 0.0) {
            showSuccess("This credit sale is already settled.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(String.format("%.2f", amountDue));
        dialog.setTitle("Settle Credit");
        dialog.setHeaderText("Record payment for " + selectedSale.getCustomer());
        dialog.setContentText("Payment amount (balance Rs. " + String.format("%,.2f", amountDue) + "):");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        double paymentAmount;
        try {
            paymentAmount = Double.parseDouble(result.get().replace(",", "").trim());
        } catch (NumberFormatException ex) {
            showError("Invalid payment amount.");
            return;
        }

        if (paymentAmount <= 0.0) {
            showError("Payment amount must be greater than zero.");
            return;
        }
        if (paymentAmount > amountDue) {
            showError("Payment cannot exceed the outstanding balance.");
            return;
        }

        double newPaidAmount = currentSaleDetail.getPaid() + paymentAmount;
        String paymentStatus = paymentStatusFor(newPaidAmount, currentSaleDetail.getAmount());

        try {
            creditSalesRepository.updatePayment(selectedSale.getCreditId(), newPaidAmount);
            currentSaleDetail.setPaid(newPaidAmount);
            enqueueCreditSale(selectedSale.getCreditId(), currentSaleDetail, paymentStatus, "payment");
            loadFromLocal();
            refreshSelection(selectedSale.getCreditId());
            showSuccess("Payment recorded successfully.");
        } catch (Exception ex) {
            showError("Failed to record payment: " + ex.getMessage());
        }
    }

    @FXML
    private void onAddPart(ActionEvent event) {
        if (currentSaleDetail == null) {
            showError("No active credit sale. Click '+ New Credit Sale' first.");
            return;
        }

        Product selectedProduct = cboProduct.getValue();
        
        if (selectedProduct == null) {
            showError("Please select a product.");
            return;
        }

        String qtyStr = txtPartQty.getText().trim();

        if (qtyStr.isBlank()) {
            showError("Please enter quantity.");
            return;
        }

        try {
            int qty = Integer.parseInt(qtyStr);
            if (qty <= 0) {
                showError("Quantity must be greater than 0.");
                return;
            }
            
            if (qty > selectedProduct.getStock()) {
                showError("Insufficient stock. Available: " + selectedProduct.getStock());
                return;
            }

            Part part = new Part(
                    selectedProduct.getName(),
                    selectedProduct.getCategory(),
                    qty,
                    selectedProduct.getSellPrice(),
                    selectedProduct.getId()
            );
            currentSaleDetail.addPart(part);
            addPartToPanel(part);
            clearPartInputs();
            updateTotals();

        } catch (NumberFormatException e) {
            showError("Invalid quantity.");
        }
    }

    private void addPartToPanel(Part part) {
        HBox row = new HBox(10);
        row.getStyleClass().add("line-item-row");

        VBox textBlock = new VBox(2);
        HBox.setHgrow(textBlock, Priority.ALWAYS);

        Label name = new Label(part.getDescription());
        name.getStyleClass().add("li-name");

        Label detail = new Label(
                part.getCategory() + "  ·  Qty " + part.getQuantity()
                        + " × Rs. " + String.format("%,.2f", part.getUnitPrice()));
        detail.getStyleClass().add("li-detail");

        textBlock.getChildren().addAll(name, detail);

        Label amount = new Label("Rs. " + String.format("%,.0f", part.getTotal()));
        amount.getStyleClass().add("li-amount");

        Button del = new Button("×");
        del.getStyleClass().add("li-delete");
        del.setOnAction(e -> {
            currentSaleDetail.removePart(part);
            vboxParts.getChildren().remove(row);
            updateTotals();
        });

        row.getChildren().addAll(textBlock, amount, del);
        vboxParts.getChildren().add(row);
    }

    private void clearPartInputs() {
        txtPartQty.clear();
        cboPartCategory.getSelectionModel().selectFirst();
        loadProductsForCategory(cboPartCategory.getValue());
    }

    private String formatProductLabel(Product product) {
        String code = product.getCode();
        if (code == null || code.isBlank()) {
            return product.getName();
        }
        return code + " - " + product.getName();
    }

    @FXML
    private void onGenerateSale(ActionEvent event) {
        if (currentSaleDetail == null || currentSaleDetail.getParts().isEmpty()) {
            showError("Add at least one part before creating the sale.");
            return;
        }

        String    creditId = isEditMode ? selectedSale.getCreditId() : generateCreditId();
        LocalDate dueDate  = currentSaleDetail.getDueDate();

        CreditSaleRow row = new CreditSaleRow(
                creditId,
                LocalDate.now().toString(),
                currentSaleDetail.getCustomer(),
                dueDate.toString(),
                currentSaleDetail.getAmount(),
                currentSaleDetail.getPaid(),
            paymentStatusFor(currentSaleDetail.getPaid(), currentSaleDetail.getAmount())
        );

        try {
            // Save to database
            creditSalesRepository.saveCreditSale(currentSaleDetail, row);
            
            if (!isEditMode) creditSaleList.add(0, row);
            enqueueCreditSale(row.getCreditId(), currentSaleDetail, row.getStatus(), isEditMode ? "update" : "create");
            showSuccess("Credit sale " + (isEditMode ? "updated" : "created") + " successfully.");
            onDeselect();
        } catch (Exception ex) {
            showError("Failed to save: " + ex.getMessage());
        }
    }

    @FXML
    private void onDeselect() {
        hideDetailPanel();
        selectedSale      = null;
        currentSaleDetail = null;
        tblCreditSales.getSelectionModel().clearSelection();
        tblCreditSales.setItems(creditSaleList);
        txtSearch.clear();
    }

    private void loadSaleDetail(CreditSaleRow sale) {
        CreditSaleDetail detail = creditSalesRepository.loadCreditSaleDetail(sale.getCreditId());
        if (detail == null) {
            detail = new CreditSaleDetail();
            detail.setCreditId(sale.getCreditId());
            detail.setCustomer(sale.getCustomer());
            detail.setDate(LocalDate.parse(sale.getDate(), DATE_FORMAT));
            detail.setDueDate(LocalDate.parse(sale.getDueDate(), DATE_FORMAT));
            detail.setPaid(sale.getPaidAmount());
        }
        currentSaleDetail = detail;

        lblCreditId.setText("#" + sale.getCreditId());
        lblCreditBadge.setText("CREDIT");
        lblCustomer.setText(currentSaleDetail.getCustomer());
        lblSaleDate.setText(sale.getDate());

        vboxParts.getChildren().clear();
        for (Part part : currentSaleDetail.getParts()) {
            addPartToPanel(part);
        }
        updateTotals();
        clearPartInputs();
        updateActionState();
    }

    private void clearDetailPanel() {
        lblCreditId.setText("#—");
        lblCreditBadge.setText("CREDIT");
        lblCustomer.setText("—");
        lblSaleDate.setText("—");
        vboxParts.getChildren().clear();
        lblSubtotal.setText("Rs. 0.00");
        lblPaid.setText("Rs. 0.00");
        lblAmountDue.setText("Rs. 0.00");
        clearPartInputs();
        updateActionState();
    }

    // ── Show / hide (no dim / glass effect) ──────────────────────────────────

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
        if (currentSaleDetail == null) return;
        lblSubtotal.setText("Rs. " + String.format("%,.2f", currentSaleDetail.getSubtotal()));
        lblPaid.setText("Rs. " + String.format("%,.2f", currentSaleDetail.getPaid()));
        lblAmountDue.setText("Rs. " + String.format("%,.2f", currentSaleDetail.getAmountDue()));
        updateActionState();
    }

    private void updateTotalsFromRow(CreditSaleRow row) {
        lblSubtotal.setText("Rs. " + String.format("%,.2f", row.getAmount()));
        lblPaid.setText("Rs. " + String.format("%,.2f", row.getPaidAmount()));
        lblAmountDue.setText("Rs. " + String.format("%,.2f", row.getBalanceAmount()));
        updateActionState();
    }

    private void updateActionState() {
        boolean hasSelection = currentSaleDetail != null;
        boolean hasBalance = hasSelection && currentSaleDetail.getAmountDue() > 0.0;
        if (btnSettleCredit != null) {
            btnSettleCredit.setDisable(!hasSelection || !hasBalance);
        }
    }

    private void refreshSelection(String creditId) {
        CreditSaleRow refreshed = creditSaleList.stream()
                .filter(row -> row.getCreditId().equals(creditId))
                .findFirst()
                .orElse(null);
        if (refreshed != null) {
            onViewCredit(refreshed);
        } else {
            onDeselect();
        }
    }

    private String paymentStatusFor(double paidAmount, double totalAmount) {
        if (paidAmount <= 0.0) {
            return "PENDING";
        }
        if (paidAmount >= totalAmount) {
            return "PAID";
        }
        return "PARTIAL";
    }

    private String generateCreditId() { return "CS" + System.currentTimeMillis(); }

    private void loadFromLocal() {
        try {
            List<CreditSaleRow> local = creditSalesRepository.loadAllCreditSales();
            creditSaleList.setAll(local);
        } catch (Exception ex) {
            showError("Failed to load credit sales: " + ex.getMessage());
        }
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void showSuccess(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Success"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void enqueueCreditSale(String creditId, CreditSaleDetail detail, String status, String operation) {
        if (detail == null) {
            return;
        }
        String[] parts = detail.getParts().stream()
                .map(part -> JsonUtil.obj(
                        JsonUtil.field("description", part.getDescription()),
                        JsonUtil.field("category", part.getCategory()),
                        JsonUtil.field("quantity", part.getQuantity()),
                        JsonUtil.field("unitPrice", part.getUnitPrice()),
                        JsonUtil.field("total", part.getTotal()),
                        JsonUtil.field("productId", part.getProductId())
                ))
                .toArray(String[]::new);

        String payload = JsonUtil.obj(
        JsonUtil.field("operation", operation),
        JsonUtil.field("creditId", creditId),
        JsonUtil.field("date", detail.getDate() == null ? LocalDate.now().toString() : detail.getDate().toString()),
        JsonUtil.field("customer", detail.getCustomer()),
        JsonUtil.field("dueDate", detail.getDueDate() == null ? LocalDate.now().toString() : detail.getDueDate().toString()),
        JsonUtil.field("amount", detail.getAmount()),
        JsonUtil.field("paidAmount", detail.getPaid()),
        JsonUtil.field("balanceAmount", detail.getAmountDue()),
        JsonUtil.field("status", status),
                JsonUtil.fieldRaw("parts", JsonUtil.array(parts))
        );

        syncQueueRepository.enqueue("credit_sale", payload);
    }

    public static class CreditSaleRow {
        private final String creditId, date, customer, dueDate, status;
        private final double amount;
        private final double paidAmount;

        public CreditSaleRow(String creditId, String date, String customer,
                             String dueDate, double amount, String status) {
            this(creditId, date, customer, dueDate, amount, 0.0, status);
        }

        public CreditSaleRow(String creditId, String date, String customer,
                             String dueDate, double amount, double paidAmount, String status) {
            this.creditId = creditId;
            this.date = date;
            this.customer = customer;
            this.dueDate = dueDate;
            this.amount = amount;
            this.paidAmount = paidAmount;
            this.status = status;
        }

        public String getCreditId() { return creditId; }
        public String getDate()     { return date;     }
        public String getCustomer() { return customer; }
        public String getDueDate() { return dueDate; }
        public double getAmount() { return amount; }
        public double getPaidAmount() { return paidAmount; }
        public double getBalanceAmount() { return Math.max(0.0, amount - paidAmount); }
        public String getStatus() { return status; }
    }
}