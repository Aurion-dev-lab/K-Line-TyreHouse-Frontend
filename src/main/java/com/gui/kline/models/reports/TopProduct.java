package com.gui.kline.models.reports;

/**
 * Model class representing top product metrics in reports.
 */
public class TopProduct {
    private final String productName;
    private final int quantity;
    private final double revenue;

    public TopProduct(String productName, int quantity, double revenue) {
        this.productName = productName;
        this.quantity = quantity;
        this.revenue = revenue;
    }

    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public double getRevenue() { return revenue; }
}
