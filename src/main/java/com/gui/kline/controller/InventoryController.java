package com.gui.kline.controller;

import com.gui.kline.controller.form.ProductFormController;
import com.gui.kline.models.Product;
import com.gui.kline.models.ViewModel;
import com.gui.kline.service.ProductService;
import javafx.application.Platform;
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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class InventoryController implements Initializable {

    @FXML private TextField txtSearch;
    @FXML private VBox      cardContainer;
    @FXML private Label     lblTotalVal;
    @FXML private Label     lblUnitsVal;
    @FXML private Label     lblLowVal;

    private static final int LOW_STOCK_THRESHOLD = 5;

    // Inject the service layer instance
    private final ProductService productService = new ProductService();
    private final ObservableList<Product> masterList   = FXCollections.observableArrayList();
    private final FilteredList<Product>   filteredList = new FilteredList<>(masterList, p -> true);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Automatically rebuild cards when filteredList changes (via search queries)
        filteredList.addListener((ListChangeListener<Product>) c -> rebuildCards());

        // Fetch real-time inventories asynchronously from our background api service
        loadInventoryData();
    }

    /**
     * Executes asynchronous sync requests via ProductService and handles threading boundaries cleanly.
     */
    private void loadInventoryData() {
        productService.getAllProducts(new ProductService.ProductCallback() {
            @Override
            public void onSuccess(ObservableList<Product> products) {
                // Return context to the JavaFX Application Thread before editing active UI layouts
                Platform.runLater(() -> {
                    masterList.setAll(products);
                    rebuildCards();
                    refreshStats();
                });
            }

            @Override
            public void onError(String errorMessage) {
                Platform.runLater(() -> {
                    System.err.println("❌ inventory fetch mismatch: " + errorMessage);
                    // Pro-tip: If you have an explicit error banner asset on your UI, update its visibility properties here
                });
            }
        });
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
                        || p.getName().toLowerCase().contains(q)
                        || p.getCategory().toLowerCase().contains(q)
        );
    }

    @FXML
    private void handleFilter() {
        // Placeholder for advanced category/stock filtering dropdown controls
    }

    public void addProduct(Product p)    { masterList.add(p); refreshStats(); }
    public void deleteProduct(Product p) { masterList.remove(p); refreshStats(); }

    public void updateProduct(Product updated) {
        masterList.replaceAll(p -> p.getId().equals(updated.getId()) ? updated : p);
        refreshStats();
    }

    private void rebuildCards() {
        cardContainer.getChildren().clear();
        for (Product p : filteredList) {
            cardContainer.getChildren().add(buildCard(p));
        }
    }

    private HBox buildCard(Product p) {
        boolean low = p.getStock() <= LOW_STOCK_THRESHOLD;

        Label icon = new Label("▣");
        icon.setStyle("-fx-font-size: 18px; -fx-text-fill: #9CA3AF;");
        VBox iconBox = new VBox(icon);
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setMinWidth(40);
        iconBox.setMaxWidth(40);
        iconBox.setStyle("-fx-background-color: #F3F4F6; -fx-background-radius: 8;");

        Label name = new Label(p.getName());
        name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #111827;");

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

        VBox info = new VBox(4, name, meta);
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
        if (low) {
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
            rightCol = new VBox(stockBox);
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
        editBtn.setOnMouseEntered(e -> editBtn.setStyle(editBtn.getStyle()
                .replace("-fx-text-fill: #6B7280;", "-fx-text-fill: #111827;")));
        editBtn.setOnMouseExited(e -> editBtn.setStyle(editBtn.getStyle()
                .replace("-fx-text-fill: #111827;", "-fx-text-fill: #6B7280;")));

        VBox actionCol = new VBox(editBtn);
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

    private void refreshStats() {
        int total = masterList.size();
        int units = masterList.stream().mapToInt(Product::getStock).sum();
        long low  = masterList.stream().filter(p -> p.getStock() <= LOW_STOCK_THRESHOLD).count();

        lblTotalVal.setText(String.valueOf(total));
        lblUnitsVal.setText(String.valueOf(units));
        lblLowVal.setText(String.valueOf(low));
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
}