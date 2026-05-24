package com.gui.kline.controller;

import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class AnalyticsController{

    @FXML private AreaChart<String, Number> revenueTrendChart;
    @FXML private PieChart inventoryPieChart;
    @FXML private BarChart<String, Number> salesServicesChart;

    @FXML
    public void initialize() {
        setupRevenueChart();
        setupInventoryChart();
        setupSalesServicesChart();
    }

    private void setupRevenueChart() {
        XYChart.Series<String, Number> revenue = new XYChart.Series<>();
        revenue.setName("Revenue");
        revenue.getData().add(new XYChart.Data<>("Jan", 400));
        revenue.getData().add(new XYChart.Data<>("Feb", 600));
        revenue.getData().add(new XYChart.Data<>("Mar", 550));

        XYChart.Series<String, Number> profit = new XYChart.Series<>();
        profit.setName("Profit");
        profit.getData().add(new XYChart.Data<>("Jan", 100));
        profit.getData().add(new XYChart.Data<>("Feb", 250));
        profit.getData().add(new XYChart.Data<>("Mar", 200));

        revenueTrendChart.getData().addAll(revenue, profit);
    }

    private void setupInventoryChart() {
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                new PieChart.Data("Lubricants", 30),
                new PieChart.Data("Tyres", 15),
                new PieChart.Data("Spare Parts", 55)
        );
        inventoryPieChart.setData(pieChartData);
    }

    private void setupSalesServicesChart() {
        XYChart.Series<String, Number> sales = new XYChart.Series<>();
        sales.setName("Sales");
        sales.getData().add(new XYChart.Data<>("Week 1", 1200));
        sales.getData().add(new XYChart.Data<>("Week 2", 1500));

        XYChart.Series<String, Number> services = new XYChart.Series<>();
        services.setName("Services");
        services.getData().add(new XYChart.Data<>("Week 1", 800));
        services.getData().add(new XYChart.Data<>("Week 2", 1100));

        salesServicesChart.getData().addAll(sales, services);
    }
}
