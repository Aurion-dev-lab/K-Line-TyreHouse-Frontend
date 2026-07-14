package com.gui.kline.controller;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import com.gui.kline.controller.form.ProductFormController;
import com.gui.kline.data.LocalCatalogRepository;
import com.gui.kline.data.SyncQueueRepository;
import com.gui.kline.models.Product;
import com.gui.kline.models.ViewModel;
import com.gui.kline.utils.AlertUtil;
import com.gui.kline.utils.JsonUtil;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class InventoryController implements Initializable {

    @FXML private TextField txtSearch;
    @FXML private VBox      cardContainer;
    @FXML private Label     lblTotalVal;
    @FXML private Label     lblUnitsVal;
    @FXML private Label     lblLowVal;
    private static final int LOW_STOCK_THRESHOLD = 5;
    private final SyncQueueRepository syncQueueRepository = new SyncQueueRepository();
    private final LocalCatalogRepository catalogRepository = new LocalCatalogRepository();

    private final ObservableList<Product> masterList   = FXCollections.observableArrayList();
    private final FilteredList<Product>   filteredList = new FilteredList<>(masterList, p -> true);
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadFromLocal();
        filteredList.addListener((ListChangeListener<Product>) c -> rebuildCards());
        rebuildCards();
        refreshStats();
    }

    private void loadFromLocal() {
        List<Product> local = catalogRepository.loadProducts();
        masterList.setAll(local);
    }

    @FXML
    private void handleAdd(ActionEvent e) {
        Stage owner = (Stage) ((Node) e.getSource()).getScene().getWindow();
        openForm(owner, null);
    }

    @FXML
    private void handleSearch() {
        String q = txtSearch.getText().trim().toLowerCase();
        filteredList.setPredicate(p ->
                q.isEmpty()
                        || p.getCode().toLowerCase().contains(q)
                        || p.getName().toLowerCase().contains(q)
                        || p.getCategory().toLowerCase().contains(q)
        );
    }

    @FXML
    private void handleFilter() {
    }

    public void addProduct(Product p) {
        masterList.add(p);
        catalogRepository.saveProduct(p);
        enqueueProduct("create", p);
        refreshStats();
    }

    public void deleteProduct(Product p) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Product");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to delete '" + p.getName() + "'?\nThis action cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    masterList.remove(p);
                    catalogRepository.deleteProduct(p);
                    enqueueProduct("delete", p);
                    refreshStats();
                    AlertUtil.showSuccess("Success", "Product Deleted Successfully");
                } catch (Exception ex) {
                    AlertUtil.showError("Error", "Failed to delete product: " + ex.getMessage());
                }
            }
        });
    }

    public void updateProduct(Product updated) {
        masterList.replaceAll(p -> p.getId().equals(updated.getId()) ? updated : p);
        catalogRepository.saveProduct(updated);
        enqueueProduct("update", updated);
        refreshStats();
    }

    private void rebuildCards() {
        cardContainer.getChildren().clear();
        for (Product p : filteredList) {
            cardContainer.getChildren().add(buildCard(p));
        }
    }

    private HBox buildCard(Product p) {
        boolean low = p.getStock() <= p.getMinimumStockAlert();

        // Create image view or placeholder
        ImageView productImage = new ImageView();
        productImage.setFitWidth(60);
        productImage.setFitHeight(60);
        productImage.setPreserveRatio(true);
        
        String imagePath = p.getImagePath();
        if (imagePath != null && !imagePath.isEmpty()) {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                try {
                    Image image = new Image(imageFile.toURI().toString());
                    productImage.setImage(image);
                } catch (Exception ex) {
                    // Use placeholder on error
                    showPlaceholderIcon(productImage);
                }
            } else {
                showPlaceholderIcon(productImage);
            }
        } else {
            showPlaceholderIcon(productImage);
        }
        
        VBox iconBox = new VBox(productImage);
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setMinWidth(70);
        iconBox.setMaxWidth(70);
        iconBox.setStyle("-fx-background-color: #F3F4F6; -fx-background-radius: 8;");

        Label name = new Label(p.getName());
        name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #111827; -fx-cursor: hand;");
        name.setOnMouseClicked(e -> showProductDetails(p));

        Label code = new Label(p.getCode());
        code.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;");

        Label cat = new Label(p.getCategory().toUpperCase());
        cat.setStyle(
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #374151;" +
                        "-fx-background-color: #F3F4F6; -fx-background-radius: 20;" +
                        "-fx-border-color: #E5E7EB; -fx-border-width: 1; -fx-border-radius: 20;" +
                        "-fx-padding: 2 8;"
        );

        Label buy = new Label(String.format("Buy  Rs. %,.0f", p.getBuyPrice()));
        buy.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;");

        Label sell = new Label(String.format("Sell  Rs. %,.0f", p.getSellPrice()));
        sell.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280; -fx-font-weight: bold;");

        HBox meta = new HBox(8, cat, buy, sell);
        meta.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(4, name, code, meta);
        info.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label stockNum = new Label(String.valueOf(p.getStock()));
        stockNum.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; " +
                "-fx-text-fill: " + (low ? "#DC2626" : "#16A34A") + ";");

        Label stockLbl = new Label("units");
        stockLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");

        VBox stockBox = new VBox(2, stockNum, stockLbl);
        stockBox.setAlignment(Pos.CENTER_RIGHT);
        stockBox.setMinWidth(50);

        VBox rightCol;
        if (p.getStock() == 0) {
            Label outOfStockTag = new Label("Out of Stock");
            outOfStockTag.setStyle(
                    "-fx-font-size: 11px; -fx-text-fill: #DC2626;" +
                            "-fx-background-color: #FEE2E2; -fx-background-radius: 20;" +
                            "-fx-border-color: #FECACA; -fx-border-width: 1; -fx-border-radius: 20;" +
                            "-fx-padding: 2 8;"
            );
            rightCol = new VBox(4, stockBox, outOfStockTag);
            rightCol.setAlignment(Pos.CENTER_RIGHT);
        } else if (low) {
            Label lowTag = new Label("Low stock");
            lowTag.setStyle(
                    "-fx-font-size: 11px; -fx-text-fill: #D97706;" +
                            "-fx-background-color: #FEF3C7; -fx-background-radius: 20;" +
                            "-fx-border-color: #FDE68A; -fx-border-width: 1; -fx-border-radius: 20;" +
                            "-fx-padding: 2 8;"
            );
            rightCol = new VBox(4, stockBox, lowTag);
            rightCol.setAlignment(Pos.CENTER_RIGHT);
        } else {
            Label inStockTag = new Label("In Stock");
            inStockTag.setStyle(
                    "-fx-font-size: 11px; -fx-text-fill: #16A34A;" +
                            "-fx-background-color: #DCFCE7; -fx-background-radius: 20;" +
                            "-fx-border-color: #BBF7D0; -fx-border-width: 1; -fx-border-radius: 20;" +
                            "-fx-padding: 2 8;"
            );
            rightCol = new VBox(4, stockBox, inStockTag);
            rightCol.setAlignment(Pos.CENTER_RIGHT);
        }

        Button editBtn = new Button("Edit");
        editBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: #E5E7EB; -fx-border-width: 1;" +
                        "-fx-border-radius: 8; -fx-background-radius: 8;" +
                        "-fx-font-size: 12px; -fx-text-fill: #6B7280;" +
                        "-fx-padding: 5 12; -fx-cursor: hand;"
        );
        editBtn.setOnAction(e -> {
            Stage owner = (Stage) editBtn.getScene().getWindow();
            openForm(owner, p);
        });
        // Removed hover effect for clean form

        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: #FECACA; -fx-border-width: 1;" +
                        "-fx-border-radius: 8; -fx-background-radius: 8;" +
                        "-fx-font-size: 12px; -fx-text-fill: #DC2626;" +
                        "-fx-padding: 5 12; -fx-cursor: hand;"
        );
        deleteBtn.setOnAction(e -> deleteProduct(p));
        // Removed hover effect for clean form

        VBox actionCol = new VBox(8, editBtn, deleteBtn);
        actionCol.setAlignment(Pos.CENTER);
        actionCol.setMinWidth(60);

        Region spacer = new Region();
        spacer.setMinWidth(16);

        HBox card = new HBox(14, iconBox, info, rightCol, spacer, actionCol);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #E5E7EB;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 12;"
        );

        card.setOnMouseEntered(e -> card.setStyle(card.getStyle()
                .replace("-fx-border-color: #E5E7EB;", "-fx-border-color: #D1D5DB;")));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle()
                .replace("-fx-border-color: #D1D5DB;", "-fx-border-color: #E5E7EB;")));

        return card;
    }
    
    private void showPlaceholderIcon(ImageView imageView) {
        // Create a simple placeholder by setting a transparent image with background
        imageView.setStyle("-fx-background-color: #E5E7EB; -fx-background-radius: 8;");
    }

    private void refreshStats() {
        int total = masterList.size();
        int units = masterList.stream().mapToInt(Product::getStock).sum();
        long low  = masterList.stream().filter(Product::isLowStock).count();
        long outOfStock = masterList.stream().filter(p -> p.getStock() == 0).count();

        lblTotalVal.setText(String.valueOf(total));
        lblUnitsVal.setText(String.valueOf(units));
        lblLowVal.setText(String.valueOf(low + outOfStock));
    }
    
    private void showProductDetails(Product product) {
        // Create details panel
        VBox detailsPanel = new VBox(15);
        detailsPanel.setPadding(new javafx.geometry.Insets(20));
        detailsPanel.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12; -fx-border-color: #e5e7eb; -fx-border-width: 1; -fx-border-radius: 12;");
        
        // Header with close button
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(10);
        
        Label title = new Label(product.getName());
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        HBox.setHgrow(title, Priority.ALWAYS);
        
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6b7280; -fx-font-size: 16px; -fx-cursor: hand; -fx-padding: 5 10;");
        closeBtn.setOnAction(e -> {
            cardContainer.getChildren().remove(detailsPanel);
            rebuildCards();
        });
        
        header.getChildren().addAll(title, closeBtn);
        
        // Create scrollable content
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-border-color: transparent;");
        
        VBox content = new VBox(12);
        content.setPadding(new javafx.geometry.Insets(10));
        
        // Add product details in a grid
        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(20);
        detailsGrid.setVgap(12);
        
        int row = 0;
        addDetailRow(detailsGrid, row++, "Product Code:", product.getCode());
        addDetailRow(detailsGrid, row++, "Category:", product.getCategory());
        if (!product.getBrand().isEmpty()) addDetailRow(detailsGrid, row++, "Brand:", product.getBrand());
        if (!product.getDescription().isEmpty()) addDetailRow(detailsGrid, row++, "Description:", product.getDescription());
        if (!product.getVehicleType().isEmpty()) addDetailRow(detailsGrid, row++, "Vehicle Type:", product.getVehicleType());
        if (!product.getMaterial().isEmpty()) addDetailRow(detailsGrid, row++, "Material:", product.getMaterial());
        if (!product.getSupplierName().isEmpty()) addDetailRow(detailsGrid, row++, "Supplier:", product.getSupplierName());
        addDetailRow(detailsGrid, row++, "Buy Price:", String.format("Rs. %.2f", product.getBuyPrice()));
        addDetailRow(detailsGrid, row++, "Sell Price:", String.format("Rs. %.2f", product.getSellPrice()));
        addDetailRow(detailsGrid, row++, "Stock:", String.valueOf(product.getStock()));
        
        String status = product.getStock() == 0 ? "Out of Stock" : product.getStock() <= product.getMinimumStockAlert() ? "Low Stock" : "In Stock";
        String statusColor = product.getStock() == 0 ? "#dc2626" : product.getStock() <= product.getMinimumStockAlert() ? "#d97706" : "#16a34a";
        addDetailRow(detailsGrid, row++, "Status:", status, statusColor);
        
        addDetailRow(detailsGrid, row++, "Min Stock Alert:", String.valueOf(product.getMinimumStockAlert()));
        if (!product.getCreatedDate().isEmpty()) addDetailRow(detailsGrid, row++, "Created Date:", product.getCreatedDate());
        
        content.getChildren().add(detailsGrid);
        
        // Add images if available
        if (!product.getImagePaths().isEmpty()) {
            Label imagesLabel = new Label("Product Images:");
            imagesLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #374151;");
            content.getChildren().add(imagesLabel);
            
            HBox imagesBox = new HBox(10);
            imagesBox.setPadding(new javafx.geometry.Insets(5, 0, 0, 0));
            for (String imagePath : product.getImagePaths()) {
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    try {
                        Image image = new Image(imageFile.toURI().toString());
                        ImageView imageView = new ImageView(image);
                        imageView.setFitWidth(120);
                        imageView.setFitHeight(120);
                        imageView.setPreserveRatio(true);
                        imageView.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
                        imagesBox.getChildren().add(imageView);
                    } catch (Exception ex) {
                        // Skip invalid images
                    }
                }
            }
            content.getChildren().add(imagesBox);
        }
        
        scrollPane.setContent(content);
        
        detailsPanel.getChildren().addAll(header, scrollPane);
        
        // Clear cards and show details
        cardContainer.getChildren().clear();
        cardContainer.getChildren().add(detailsPanel);
    }
    
    private void addDetailRow(GridPane grid, int row, String label, String value) {
        addDetailRow(grid, row, label, value, "#374151");
    }
    
    private void addDetailRow(GridPane grid, int row, String label, String value, String valueColor) {
        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-font-weight: bold; -fx-text-fill: #6b7280; -fx-font-size: 13px;");
        GridPane.setColumnIndex(labelNode, 0);
        GridPane.setRowIndex(labelNode, row);
        
        Label valueNode = new Label(value);
        valueNode.setStyle("-fx-text-fill: " + valueColor + "; -fx-font-size: 13px;");
        GridPane.setColumnIndex(valueNode, 1);
        GridPane.setRowIndex(valueNode, row);
        
        grid.getChildren().addAll(labelNode, valueNode);
    }
    
    private javafx.scene.Node createDetailRow(String label, String value) {
        HBox row = new HBox(10);
        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-font-weight: bold; -fx-min-width: 120px;");
        Label valueNode = new Label(value);
        valueNode.setStyle("-fx-text-fill: #374151;");
        row.getChildren().addAll(labelNode, valueNode);
        return row;
    }

    private void openForm(Stage owner, Product product) {
        ProductFormController form =
                ViewModel.INSTANCE.getViewsFactory().getForm("form/product-form-dialog", owner);
        if (form == null) return;

        if (product != null) form.setProduct(product);

        form.setOnSave(saved -> {
            if (product == null) addProduct(saved);
            else updateProduct(saved);
        });
    }

    private void enqueueProduct(String operation, Product product) {
        String payload = JsonUtil.obj(
                JsonUtil.field("operation", operation),
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