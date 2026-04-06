package com.gui.kline.controller;

import com.gui.kline.models.SaleRecord;
import com.gui.kline.models.ViewModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class SalesController {

    @FXML private Button btnNewSale;
    @FXML private TableView<SaleRecord> tblTransactions;
    @FXML private TableColumn<SaleRecord, String> colDate;
    @FXML private TableColumn<SaleRecord, String> colProduct;
    @FXML private TableColumn<SaleRecord, Integer> colQty;
    @FXML private TableColumn<SaleRecord, Double> colTotal;
    @FXML private TableColumn<SaleRecord, Double> colProfit;
    @FXML private TableColumn<SaleRecord, String> colRemark;

    @FXML private DatePicker dateFrom;
    @FXML private DatePicker dateTo;
    @FXML private TextField txtSearch;

    @FXML private Label lblRevenue;
    @FXML private Label lblProfit;
    @FXML private Label lblEngineOilPct;
    @FXML private Label lblTyresPct;

    @FXML private ProgressBar pbEngineOil;
    @FXML private ProgressBar pbTyres;

    private ObservableList<SaleRecord> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // 1. Setup Column Cell Factories
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colProduct.setCellValueFactory(new PropertyValueFactory<>("product"));
        colQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colProfit.setCellValueFactory(new PropertyValueFactory<>("profit"));
        colRemark.setCellValueFactory(new PropertyValueFactory<>("remark"));

        // 2. Setup Search Filtering
        FilteredList<SaleRecord> filteredData = new FilteredList<>(masterData, p -> true);
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(sale -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                return sale.getProduct().toLowerCase().contains(lowerCaseFilter) ||
                        sale.getRemark().toLowerCase().contains(lowerCaseFilter);
            });
        });

        // 3. Bind Data & Load
        tblTransactions.setItems(filteredData);
        loadSampleData();
        updateDashboard();
    }

    private void loadSampleData() {
        masterData.setAll(
                new SaleRecord("2024-05-01", "Engine Oil (5L)", 2, 12000.00, 3000.00, "Regular"),
                new SaleRecord("2024-05-02", "Michelin Tire", 4, 60000.00, 15000.00, "Bulk buy"),
                new SaleRecord("2024-05-02", "Oil Filter", 1, 1500.00, 550.00, "Direct sale"),
                new SaleRecord("2024-05-03", "Synthetic Oil", 3, 18000.00, 4500.00, "Premium"),
                new SaleRecord("2024-05-04", "Bridgestone Tire", 2, 32000.00, 8000.00, "Discounted")
        );
    }

    private void updateDashboard() {
        double totalRevenue = 0;
        double totalProfit = 0;
        int oilQty = 0;
        int tyreQty = 0;
        int totalQty = 0;

        for (SaleRecord s : masterData) {
            totalRevenue += s.getTotal();
            totalProfit += s.getProfit();
            totalQty += s.getQuantity();

            String prod = s.getProduct().toLowerCase();
            if (prod.contains("oil")) oilQty += s.getQuantity();
            if (prod.contains("tire") || prod.contains("tyre")) tyreQty += s.getQuantity();
        }

        lblRevenue.setText(String.format("Rs. %,.0f", totalRevenue));
        lblProfit.setText(String.format("Rs. %,.0f", totalProfit));

        // Progress based on share of total quantity sold
        double oilRatio = totalQty == 0 ? 0 : (double) oilQty / totalQty;
        double tyreRatio = totalQty == 0 ? 0 : (double) tyreQty / totalQty;

        pbEngineOil.setProgress(oilRatio);
        pbTyres.setProgress(tyreRatio);

        lblEngineOilPct.setText(String.format("%d%%", (int)(oilRatio * 100)));
        lblTyresPct.setText(String.format("%d%%", (int)(tyreRatio * 100)));
    }

    @FXML
    void onNewSale(ActionEvent event) {
        Stage ownerStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        ViewModel.INSTANCE.getViewsFactory().getForm("form/process-sale-dialog", ownerStage);
        updateDashboard();
    }
}