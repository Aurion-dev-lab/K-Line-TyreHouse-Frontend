package com.gui.kline.models;

public class Part {
    private final String description, category;
    private final int quantity;
    private final double unitPrice;

    public Part(String description, String category, int quantity, double unitPrice) {
        this.description = description;
        this.category = category;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public int getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }
    public double getTotal() { return quantity * unitPrice; }
}
