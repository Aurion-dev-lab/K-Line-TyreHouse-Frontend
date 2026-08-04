package com.gui.kline.models.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Part {
    @JsonProperty("description")
    private String description;

    @JsonProperty("category")
    private String category;

    @JsonProperty("quantity")
    @JsonAlias({"qty"})
    private int quantity;

    @JsonProperty("unitPrice")
    @JsonAlias({"unit_price"})
    private double unitPrice;

    @JsonProperty("productId")
    @JsonAlias({"product_id"})
    private String productId;

    public Part() {}

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
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public double getTotal() { return quantity * unitPrice; }
}
