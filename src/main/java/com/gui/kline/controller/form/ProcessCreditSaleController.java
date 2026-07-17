package com.gui.kline.controller.form;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.gui.kline.controller.CreditSalesController;
import com.gui.kline.data.LocalCatalogRepository;
import com.gui.kline.data.LocalCreditSalesRepository;
import com.gui.kline.data.SyncQueueRepository;
import com.gui.kline.models.CreditSaleDetail;
import com.gui.kline.models.Part;
import com.gui.kline.models.Product;
import com.gui.kline.utils.JsonUtil;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ProcessCreditSaleController {

     @FXML private Label                lblDialogTitle;
     @FXML private Label                lblCreditId;
     @FXML private ComboBox<String>    cmbCustomerName;
     @FXML private TextField           txtPhone;
     @FXML private TextField           txtVehicleNumber;
     @FXML private DatePicker          dpSaleDate;
     @FXML private DatePicker          dpDueDate;
     @FXML private ChoiceBox<String>   cboPaymentTerms;
     @FXML private ComboBox<Product>   cmbProduct;
     @FXML private TextField           txtProductSearch;
     @FXML private TextField           txtPartQty;
     @FXML private TextField           txtPartPrice;
     @FXML private Button              btnAddPart;
     @FXML private VBox                vboxPartsList;
     @FXML private Label               lblTotal;
     @FXML private Button              btnCancel;
     @FXML private Button              btnCreate;
     @FXML private TextField           txtLabour;
     @FXML private TextField           txtParts;
     @FXML private TextField           txtDiscount;

     private boolean isEditMode = false;
     private String editCreditId = null;
     private List<Part> addedParts = new ArrayList<>();
     private List<Product> availableProducts = new ArrayList<>();
     private ObservableList<Product> allProducts = FXCollections.observableArrayList();
     private FilteredList<Product> filteredProducts;
     private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
     private final SyncQueueRepository syncQueueRepository = new SyncQueueRepository();
     private final LocalCatalogRepository catalogRepository = new LocalCatalogRepository();
     private final LocalCreditSalesRepository creditSalesRepository = new LocalCreditSalesRepository();
     private static final java.util.Map<String, Integer> TERMS_DAY_MAP = new java.util.HashMap<>();
     static {
         TERMS_DAY_MAP.put("Net 15 (15 days)", 15);
         TERMS_DAY_MAP.put("Net 30 (30 days)", 30);
         TERMS_DAY_MAP.put("Net 45 (45 days)", 45);
         TERMS_DAY_MAP.put("Net 60 (60 days)", 60);
         TERMS_DAY_MAP.put("Net 90 (90 days)", 90);
         TERMS_DAY_MAP.put("COD (Cash on Delivery)", 0);
     }

     @FXML
     public void initialize() {
         lblCreditId.setText("#CS-" + String.format("%04d", (int)(Math.random() * 9000 + 1000)));

         dpSaleDate.setValue(LocalDate.now());
         dpDueDate.setValue(LocalDate.now().plusDays(30)); // 30-day default
         cmbCustomerName.setEditable(true);
         cmbCustomerName.getItems().setAll(catalogRepository.getCustomerNames());
         
         // Load products from inventory
         availableProducts = catalogRepository.loadProducts();
         allProducts.setAll(availableProducts);
         filteredProducts = new FilteredList<>(allProducts, product -> true);
         cmbProduct.setItems(filteredProducts);
         cmbProduct.setCellFactory(list -> new ListCell<>() {
             @Override
             protected void updateItem(Product item, boolean empty) {
                 super.updateItem(item, empty);
                 setText(empty || item == null ? null : formatProductLabel(item) + " (Stock: " + item.getStock() + ")");
             }
         });
         cmbProduct.setButtonCell(new ListCell<>() {
             @Override
             protected void updateItem(Product item, boolean empty) {
                 super.updateItem(item, empty);
                 setText(empty || item == null ? "Select Product" : formatProductLabel(item) + " (Stock: " + item.getStock() + ")");
             }
         });
         cmbProduct.setOnAction(e -> onProductSelected());
         
         cboPaymentTerms.setItems(FXCollections.observableArrayList(
                 "Net 15 (15 days)",
                 "Net 30 (30 days)",
                 "Net 45 (45 days)",
                 "Net 60 (60 days)",
                 "Net 90 (90 days)",
                 "COD (Cash on Delivery)"
         ));
         cboPaymentTerms.getSelectionModel().select(1); // Default Net 30
         cboPaymentTerms.setOnAction(e -> updateDueDateFromTerms());
         
         // Add listeners for cost fields
         txtPartPrice.textProperty().addListener((o, old, v) -> updateTotal());
         txtPartQty.textProperty().addListener((o, old, v) -> updateTotal());
         txtLabour.textProperty().addListener((o, old, v) -> updateTotal());
         txtParts.textProperty().addListener((o, old, v) -> updateTotal());
         txtDiscount.textProperty().addListener((o, old, v) -> updateTotal());
         
         // Add product search listener
         txtProductSearch.textProperty().addListener((o, old, v) -> filterProducts(v));
     }

     private void filterProducts(String searchText) {
         String query = searchText == null ? "" : searchText.trim().toLowerCase();
         filteredProducts.setPredicate(product -> {
             if (query.isEmpty()) {
                 return true;
             }
             return containsIgnoreCase(product.getCode(), query) ||
                    containsIgnoreCase(product.getName(), query) ||
                    containsIgnoreCase(product.getCategory(), query) ||
                    containsIgnoreCase(product.getBrand(), query);
         });
         
         if (!filteredProducts.isEmpty() && !query.isEmpty()) {
             cmbProduct.show();
         }
     }

     private boolean containsIgnoreCase(String value, String query) {
         return value != null && value.toLowerCase().contains(query);
     }

     private void updateDueDateFromTerms() {
         String selected = cboPaymentTerms.getValue();
         LocalDate saleDate = dpSaleDate.getValue();
         if (saleDate == null) saleDate = LocalDate.now();

         int days = 30;
         if (selected != null) {
             if (selected.contains("15")) days = 15;
             else if (selected.contains("30")) days = 30;
             else if (selected.contains("45")) days = 45;
             else if (selected.contains("60")) days = 60;
             else if (selected.contains("90")) days = 90;
             else if (selected.contains("COD")) days = 0;
         }

         dpDueDate.setValue(saleDate.plusDays(days));
     }

     private void onProductSelected() {
         Product selectedProduct = cmbProduct.getValue();
         if (selectedProduct == null) {
             txtPartPrice.clear();
             return;
         }
         txtPartPrice.setText(String.valueOf(selectedProduct.getSellPrice()));
         txtPartQty.setText("1");
     }

     @FXML
     private void onAddPart() {
         Product selectedProduct = cmbProduct.getValue();
         String qtyStr = txtPartQty.getText().trim();
         String priceStr = txtPartPrice.getText().trim();

         if (selectedProduct == null) {
             alert("Please select a product from inventory.");
             return;
         }
         if (qtyStr.isBlank() || priceStr.isBlank()) {
             alert("Please enter quantity and unit price.");
             return;
         }

         try {
             int qty = Integer.parseInt(qtyStr);
             double price = Double.parseDouble(priceStr);

             if (qty <= 0 || price < 0) {
                 alert("Quantity must be > 0 and price must be >= 0.");
                 return;
             }

             // Validate stock
             if (selectedProduct.getStock() < qty) {
                 alert("Insufficient stock. Available: " + selectedProduct.getStock() + 
                       ", Requested: " + qty);
                 return;
             }

             Part part = new Part(formatProductLabel(selectedProduct), selectedProduct.getCategory(), qty, price, selectedProduct.getId());
             addedParts.add(part);

             addPartToUI(part);

             // Reset fields
             cmbProduct.setValue(null);
             txtProductSearch.clear();
             txtPartQty.setText("1");
             txtPartPrice.clear();

             updateTotal();

         } catch (NumberFormatException e) {
             alert("Invalid quantity or price. Please enter numbers.");
         }
     }

     private void addPartToUI(Part part) {
         HBox row = new HBox(12);
         row.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-border-radius: 8; " +
                 "-fx-border-color: #e2e8f0; -fx-border-width: 1;");

         VBox info = new VBox(2);
         HBox.setHgrow(info, Priority.ALWAYS);

         Label name = new Label(part.getDescription());
         name.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");

         Label detail = new Label(part.getCategory() + "  ·  " + part.getQuantity() +
                 " × Rs. " + String.format("%.2f", part.getUnitPrice()));
         detail.setStyle("-fx-font-size: 10px; -fx-text-fill: #6b7280;");

         info.getChildren().addAll(name, detail);

         Label amount = new Label("Rs. " + String.format("%.0f", part.getTotal()));
         amount.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #10b981; " +
                 "-fx-min-width: 90;");

         Button del = new Button("×");
         del.setStyle("-fx-background-color: transparent; -fx-text-fill: #6b7280; " +
                 "-fx-font-size: 18px; -fx-padding: 0 4 0 4; -fx-cursor: hand;");
         del.setOnAction(e -> {
             addedParts.remove(part);
             vboxPartsList.getChildren().remove(row);
             updateTotal();
         });

         row.getChildren().addAll(info, amount, del);
         vboxPartsList.getChildren().add(row);
     }

     private void updateTotal() {
         double partsTotal = addedParts.stream().mapToDouble(Part::getTotal).sum();
         double labour = parseDouble(txtLabour.getText());
         double parts = parseDouble(txtParts.getText());
         double discount = parseDouble(txtDiscount.getText());
         
         double total = partsTotal + labour + parts - discount;
         if (total < 0) total = 0;
         
         lblTotal.setText(String.format("%.2f", total));
     }

     private double parseDouble(String value) {
         if (value == null || value.trim().isEmpty()) {
             return 0.0;
         }
         try {
             return Double.parseDouble(value.trim());
         } catch (NumberFormatException e) {
             return 0.0;
         }
     }

      /**
       * Set the dialog to edit mode, pre-populating fields from an existing credit sale.
       */
      public void setEditMode(String creditId, CreditSaleDetail detail) {
          this.isEditMode = true;
          this.editCreditId = creditId;
          
          lblDialogTitle.setText("Edit Credit Sale");
          lblCreditId.setText("#" + creditId);
          cmbCustomerName.setValue(detail.getCustomer());
          cmbCustomerName.getEditor().setText(detail.getCustomer());
          txtPhone.setText(catalogRepository.getCustomerPhone(detail.getCustomer()));
          dpSaleDate.setValue(detail.getDate());
          dpDueDate.setValue(detail.getDueDate());
          
          // Set payment terms based on due date difference
          LocalDate saleDate = detail.getDate();
          if (saleDate != null && detail.getDueDate() != null) {
              long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(saleDate, detail.getDueDate());
              for (String terms : cboPaymentTerms.getItems()) {
                  Integer termsDays = TERMS_DAY_MAP.get(terms);
                  if (termsDays != null && termsDays == (int) daysBetween) {
                      cboPaymentTerms.setValue(terms);
                      break;
                  }
              }
          }
          
          // Set additional costs
          txtLabour.setText(String.format("%.2f", detail.getLabour()));
          txtParts.setText(String.format("%.2f", detail.getPartsCost()));
          txtDiscount.setText(String.format("%.2f", detail.getDiscount()));
          
          // Load existing parts
          addedParts.clear();
          vboxPartsList.getChildren().clear();
          for (Part part : detail.getParts()) {
              Part partCopy = new Part(part.getDescription(), part.getCategory(),
                      part.getQuantity(), part.getUnitPrice(), part.getProductId());
              addedParts.add(partCopy);
              addPartToUI(partCopy);
          }
          
          // Change button text
          btnCreate.setText("Update Credit Sale");
          btnCreate.setStyle("-fx-background-color: #f59e0b; -fx-background-radius: 14; -fx-text-fill: white; "
                  + "-fx-font-weight: bold; -fx-font-size: 14px; -fx-cursor: hand;");
          
          updateTotal();
      }

      @FXML
      private void onCreate() {
          String customerName = getCustomerName();
          String phone = txtPhone.getText().trim();
          String vehicle = txtVehicleNumber.getText().trim();

          if (customerName.isBlank()) {
              alert("Customer name is required.");
              return;
          }
          if (!isEditMode && phone.isBlank()) {
              alert("Phone number is required.");
              return;
          }
          if (!isEditMode && vehicle.isBlank()) {
              alert("Vehicle number is required.");
              return;
          }

          LocalDate saleDate = dpSaleDate.getValue();
          LocalDate dueDate = dpDueDate.getValue();

          if (saleDate == null || dueDate == null) {
              alert("Please select sale and due dates.");
              return;
          }

          if (dueDate.isBefore(saleDate)) {
              alert("Due date cannot be before sale date.");
              return;
          }

          if (addedParts.isEmpty()) {
              alert("Please add at least one part to the credit sale.");
              return;
          }

          String creditId = isEditMode ? editCreditId : lblCreditId.getText().replace("#CS-", "").replace("#", "");
          double total = parseDouble(lblTotal.getText());

          double existingPaid = 0.0;
          String existingStatus = "PENDING";
          if (isEditMode) {
              // Capture existing paid amount BEFORE deleting (delete will also restore inventory)
              CreditSaleDetail existingDetail = creditSalesRepository.loadCreditSaleDetail(creditId);
              if (existingDetail != null) {
                  existingPaid = existingDetail.getPaid();
                  existingStatus = existingDetail.getPaid() >= existingDetail.getAmount() ? "PAID"
                          : (existingDetail.getPaid() > 0 ? "PARTIAL" : "PENDING");
              }
          }

          try {
              // If editing, first delete the old credit sale record (this restores inventory)
              if (isEditMode) {
                  creditSalesRepository.deleteCreditSale(creditId);
              }

              // Create CreditSaleDetail object
              CreditSaleDetail detail = new CreditSaleDetail();
              detail.setCreditId(creditId);
              detail.setCustomer(customerName);
              detail.setDate(saleDate);
              detail.setDueDate(dueDate);
              detail.setPaid(isEditMode ? existingPaid : 0);
              detail.setLabour(parseDouble(txtLabour.getText()));
              detail.setPartsCost(parseDouble(txtParts.getText()));
              detail.setDiscount(parseDouble(txtDiscount.getText()));
              for (Part part : addedParts) {
                  detail.addPart(part);
              }

              // Determine status
              String status = isEditMode ? existingStatus : "PENDING";
              if (isEditMode && existingPaid > 0) {
                  status = existingPaid >= total ? "PAID" : (existingPaid > 0 ? "PARTIAL" : "PENDING");
              }

              CreditSalesController.CreditSaleRow row = new CreditSalesController.CreditSaleRow(
                      creditId,
                      saleDate.toString(),
                      customerName,
                      dueDate.toString(),
                      total,
                      detail.getPaid(),
                      status
              );

              // Save to database (creates new row with new UUID but same credit_id)
              creditSalesRepository.saveCreditSale(detail, row);

              // Create sync payload and enqueue
              enqueueCreditSale(row, detail);

              // Save customer
              catalogRepository.saveCustomer(customerName, phone);

              showSuccess("Credit sale " + (isEditMode ? "updated" : "created") + " successfully!");
              closeDialog();

          } catch (Exception e) {
              alert("Error " + (isEditMode ? "updating" : "creating") + " credit sale: " + e.getMessage());
              e.printStackTrace();
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

      private void enqueueCreditSale(CreditSalesController.CreditSaleRow row, CreditSaleDetail detail) {
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
                  JsonUtil.field("creditId", row.getCreditId()),
                  JsonUtil.field("date", row.getDate()),
                  JsonUtil.field("customer", row.getCustomer()),
                  JsonUtil.field("dueDate", row.getDueDate()),
                  JsonUtil.field("amount", row.getAmount()),
                  JsonUtil.field("status", row.getStatus()),
                  JsonUtil.field("labour", detail.getLabour()),
                  JsonUtil.field("partsCost", detail.getPartsCost()),
                  JsonUtil.field("discount", detail.getDiscount()),
                  JsonUtil.fieldRaw("items", JsonUtil.array(parts))
          );

          syncQueueRepository.enqueue("credit_sale", payload);
      }

      @FXML
      private void onCancel() {
          closeDialog();
      }

      private void alert(String msg) {
          Alert a = new Alert(Alert.AlertType.WARNING);
          a.setTitle("Validation");
          a.setHeaderText(null);
          a.setContentText(msg);
          initOwner(a);
          a.initModality(javafx.stage.Modality.WINDOW_MODAL);
          a.showAndWait();
      }

      private void showSuccess(String msg) {
          Alert a = new Alert(Alert.AlertType.INFORMATION);
          a.setTitle("Success");
          a.setHeaderText(null);
          a.setContentText(msg);
          initOwner(a);
          a.initModality(javafx.stage.Modality.WINDOW_MODAL);
          a.showAndWait();
      }

      private void closeDialog() {
          ((Stage) btnCancel.getScene().getWindow()).close();
      }

      private void initOwner(Alert alert) {
          if (btnCancel != null && btnCancel.getScene() != null
                  && btnCancel.getScene().getWindow() != null) {
              alert.initOwner(btnCancel.getScene().getWindow());
          }
      }
}