package com.gui.kline.models;

public class LineItem {
    private final String description, type, productId;
    private final int    qty;
    private final double unitPrice;

    public LineItem(String description, String type, int qty, double unitPrice) {
        this(description, type, qty, unitPrice, null);
    }

    public LineItem(String description, String type, int qty, double unitPrice, String productId) {
        this.description = description; this.type = type;
        this.qty = qty; this.unitPrice = unitPrice;
        this.productId = productId;
    }

    public String getDescription() { return description; }
    public String getType()        { return type; }
    public int    getQty()         { return qty; }
    public double getUnitPrice()   { return unitPrice; }
    public double getTotal()       { return qty * unitPrice; }
    public String getProductId()   { return productId; }
}


