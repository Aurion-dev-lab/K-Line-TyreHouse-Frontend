package com.gui.kline.models;

public class Part {
    private final String description, category;
    private final int quantity;
    private final double unitPrice;
    private final String productId;

    public Part(String description, String category, int quantity, double unitPrice) {
        this(description, category, quantity, unitPrice, null);
    }

    public Part(String description, String category, int quantity, double unitPrice, String productId) {
        this.description = description;
        this.category = category;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.productId = productId;
    }

    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public int getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }
    public String getProductId() { return productId; }
    public double getTotal() { return quantity * unitPrice; }
}
