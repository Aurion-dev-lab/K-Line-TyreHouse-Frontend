package com.gui.kline.models;

import javafx.beans.property.*;

public class SaleRecord {
    private final StringProperty product;
    private final IntegerProperty quantity;
    private final DoubleProperty total;
    private final DoubleProperty profit;
    private final StringProperty date;
    private final StringProperty remark;

    public SaleRecord(String date, String product, int quantity, double total, double profit, String remark) {
        this.date = new SimpleStringProperty(date);
        this.product = new SimpleStringProperty(product);
        this.quantity = new SimpleIntegerProperty(quantity);
        this.total = new SimpleDoubleProperty(total);
        this.profit = new SimpleDoubleProperty(profit);
        this.remark = new SimpleStringProperty(remark);
    }

    // Getters for TableView to use
    public String getProduct() { return product.get(); }
    public int getQuantity() { return quantity.get(); }
    public double getTotal() { return total.get(); }
    public double getProfit() { return profit.get(); }
    public String getDate() { return date.get(); }
    public String getRemark() { return remark.get(); }
}
