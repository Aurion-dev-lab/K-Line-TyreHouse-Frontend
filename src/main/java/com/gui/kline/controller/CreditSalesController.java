package com.gui.kline.controller;

import com.gui.kline.data.SyncQueueReader;
import com.gui.kline.data.SyncQueueRepository;
import com.gui.kline.data.LocalCreditSalesRepository;
import com.gui.kline.utils.JsonUtil;
import com.gui.kline.models.CreditSaleDetail;
import com.gui.kline.models.Part;
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
import java.time.format.DateTimeFormatter;
import java.util.*;

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
    @FXML private TextField         txtPartDesc;
    @FXML private TextField         txtPartQty;
    @FXML private TextField         txtPartPrice;
    @FXML private Button            btnAddPart;
    @FXML private Button            btnGenerateSale;
    @FXML private Button            btnDeselect;

    private final ObservableList<CreditSaleRow> creditSaleList =
            FXCollections.observableArrayList();
    private final SyncQueueRepository syncQueueRepository = new SyncQueueRepository();
    private final LocalCreditSalesRepository creditSalesRepository = new LocalCreditSalesRepository();

    private CreditSaleRow         selectedSale       = null;
    private CreditSaleDetail currentSaleDetail  = null;
    private boolean               isEditMode         = false;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupChoiceBoxes();
        setupEventHandlers();
        loadFromLocal();
        tblCreditSales.setItems(creditSaleList);
        disableDetailPanel();
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
                    case "PENDING" ->
                            pill.setStyle(BASE_STYLE + "-fx-background-color: #fef3c7; -fx-text-fill: #92400e;");
                    case "PARTIAL" ->
                            pill.setStyle(BASE_STYLE + "-fx-background-color: #dbeafe; -fx-text-fill: #1e40af;");
                    case "PAID" ->
                            pill.setStyle(BASE_STYLE + "-fx-background-color: #dcfce7; -fx-text-fill: #15803d;");
                    default ->
                            pill.setStyle(BASE_STYLE);
                }
                setGraphic(pill);
                setText(null);
            }
        });

        colAmount.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : "Rs. " + String.format("%,.0f", item));
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
                
                btnView.setOnAction(e -> onViewCredit(getTableView().getItems().get(getIndex())));
                btnEdit.setOnAction(e -> onEditCredit(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> onDeleteCredit(getTableView().getItems().get(getIndex())));
                
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

    private void setupChoiceBoxes() {
        cboPartCategory.setItems(FXCollections.observableArrayList(
                "Engine Parts", "Suspension", "Electrical", "Body Parts", "Accessories", "Consumables"));
        cboPartCategory.getSelectionModel().selectFirst();
    }

    private void setupEventHandlers() {
        tblCreditSales.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, nw) -> { if (nw != null) onViewCredit(nw); });
    }

    @FXML
    private void onNewCredit(ActionEvent event) {
        Stage ownerStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        ViewModel.INSTANCE.getViewsFactory().getForm("form/credit-sale-dialog", ownerStage);
    }

    @FXML
    private void onSearch(javafx.scene.input.KeyEvent event) {
        String q = txtSearch.getText().toLowerCase().trim();
        tblCreditSales.setItems(q.isEmpty()
                ? creditSaleList
                : creditSaleList.filtered(sale ->
                sale.getCustomer().toLowerCase().contains(q) ||
                        sale.getDate().toLowerCase().contains(q) ||
                        sale.getCreditId().toLowerCase().contains(q)));
    }

    private void onViewCredit(CreditSaleRow sale) {
        selectedSale = sale;
        isEditMode = false;
        enableDetailPanel();
        loadSaleDetail(sale);
    }
    
    private void onEditCredit(CreditSaleRow sale) {
        selectedSale = sale;
        isEditMode = true;
        enableDetailPanel();
        loadSaleDetail(sale);
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
    private void onAddPart(ActionEvent event) {
        if (currentSaleDetail == null) {
            showError("No active credit sale. Click '+ New Credit Sale' first.");
            return;
        }

        String category = cboPartCategory.getValue();
        String desc = txtPartDesc.getText().trim();

        if (category == null || category.isBlank()) {
            showError("Please select a part category.");
            return;
        }
        if (desc.isBlank()) {
            showError("Please enter part description.");
            return;
        }

        String qtyStr = txtPartQty.getText().trim();
        String priceStr = txtPartPrice.getText().trim();

        if (qtyStr.isBlank() || priceStr.isBlank()) {
            showError("Please enter quantity and unit price.");
            return;
        }

        try {
            int qty = Integer.parseInt(qtyStr);
            double price = Double.parseDouble(priceStr);
            if (qty <= 0 || price <= 0) {
                showError("Quantity and price must be greater than 0.");
                return;
            }

            Part part = new Part(desc, category, qty, price);
            currentSaleDetail.addPart(part);
            addPartToPanel(part);
            clearPartInputs();
            updateTotals();

        } catch (NumberFormatException e) {
            showError("Invalid quantity or price.");
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
        txtPartDesc.clear();
        txtPartQty.clear();
        txtPartPrice.clear();
        cboPartCategory.getSelectionModel().selectFirst();
    }

    @FXML
    private void onGenerateSale(ActionEvent event) {
        if (currentSaleDetail == null || currentSaleDetail.getParts().isEmpty()) {
            showError("Add at least one part before creating the sale.");
            return;
        }

        String creditId = isEditMode ? selectedSale.getCreditId() : generateCreditId();
        LocalDate dueDate = currentSaleDetail.getDueDate();

        CreditSaleRow row = new CreditSaleRow(
                creditId,
                LocalDate.now().toString(),
                currentSaleDetail.getCustomer(),
                dueDate.toString(),
                currentSaleDetail.getAmount(),
                "PENDING"
        );

        try {
            // Save to database
            creditSalesRepository.saveCreditSale(currentSaleDetail, row);
            
            if (!isEditMode) creditSaleList.add(0, row);
            enqueueCreditSale(row, currentSaleDetail);
            showSuccess("Credit sale " + (isEditMode ? "updated" : "created") + " successfully.");
            onDeselect();
        } catch (Exception ex) {
            showError("Failed to save: " + ex.getMessage());
        }
    }

    @FXML
    private void onDeselect() {
        disableDetailPanel();
        selectedSale = null;
        currentSaleDetail = null;
        tblCreditSales.getSelectionModel().clearSelection();
        tblCreditSales.setItems(creditSaleList);
        txtSearch.clear();
    }

    private void loadSaleDetail(CreditSaleRow sale) {
        currentSaleDetail = new CreditSaleDetail();
        currentSaleDetail.setCreditId(sale.getCreditId());
        currentSaleDetail.setCustomer(sale.getCustomer());
        currentSaleDetail.setDate(LocalDate.parse(sale.getDate(), DATE_FORMAT));
        currentSaleDetail.setDueDate(LocalDate.parse(sale.getDueDate(), DATE_FORMAT));

        lblCreditId.setText("#" + sale.getCreditId());
        lblCreditBadge.setText("CREDIT");
        lblCustomer.setText(currentSaleDetail.getCustomer());
        lblSaleDate.setText(sale.getDate());

        vboxParts.getChildren().clear();
        updateTotalsFromRow(sale);
        clearPartInputs();
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
        if (currentSaleDetail == null) return;
        lblSubtotal.setText("Rs. " + String.format("%,.2f", currentSaleDetail.getSubtotal()));
        lblPaid.setText("Rs. " + String.format("%,.2f", currentSaleDetail.getPaid()));
        lblAmountDue.setText("Rs. " + String.format("%,.2f", currentSaleDetail.getAmountDue()));
    }

    private void updateTotalsFromRow(CreditSaleRow row) {
        lblSubtotal.setText("Rs. " + String.format("%,.2f", row.getAmount()));
        lblPaid.setText("Rs. 0.00");
        lblAmountDue.setText("Rs. " + String.format("%,.2f", row.getAmount()));
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

    private void enqueueCreditSale(CreditSaleRow row, CreditSaleDetail detail) {
        if (detail == null) {
            return;
        }
        String[] parts = detail.getParts().stream()
                .map(part -> JsonUtil.obj(
                        JsonUtil.field("description", part.getDescription()),
                        JsonUtil.field("category", part.getCategory()),
                        JsonUtil.field("quantity", part.getQuantity()),
                        JsonUtil.field("unitPrice", part.getUnitPrice()),
                        JsonUtil.field("total", part.getTotal())
                ))
                .toArray(String[]::new);

        String payload = JsonUtil.obj(
                JsonUtil.field("creditId", row.getCreditId()),
                JsonUtil.field("date", row.getDate()),
                JsonUtil.field("customer", row.getCustomer()),
                JsonUtil.field("dueDate", row.getDueDate()),
                JsonUtil.field("amount", row.getAmount()),
                JsonUtil.field("status", row.getStatus()),
                JsonUtil.fieldRaw("parts", JsonUtil.array(parts))
        );

        syncQueueRepository.enqueue("credit_sale", payload);
    }

    public static class CreditSaleRow {
        private final String creditId, date, customer, dueDate, status;
        private final double amount;

        public CreditSaleRow(String creditId, String date, String customer,
                             String dueDate, double amount, String status) {
            this.creditId = creditId;
            this.date = date;
            this.customer = customer;
            this.dueDate = dueDate;
            this.amount = amount;
            this.status = status;
        }

        public String getCreditId() { return creditId; }
        public String getDate() { return date; }
        public String getCustomer() { return customer; }
        public String getDueDate() { return dueDate; }
        public double getAmount() { return amount; }
        public String getStatus() { return status; }
    }
}