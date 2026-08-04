package com.gui.kline.models.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gui.kline.data.LocalCatalogRepository;
import com.gui.kline.models.Product;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LineItem {
    @JsonProperty("productId")
    @JsonAlias({"product_id"})
    private String productId;

    @JsonProperty("qty")
    @JsonAlias({"quantity"})
    private int qty;

    @JsonProperty("unitPrice")
    @JsonAlias({"unit_price"})
    private double unitPrice;

    @JsonProperty("total")
    private double total;

    public LineItem() {}

    public LineItem(String description, String type, int qty, double unitPrice) {
        this(description, type, qty, unitPrice, null);
    }

    public LineItem(String description, String type, int qty, double unitPrice, String productId) {
        if ("Service".equalsIgnoreCase(type)) {
            this.productId = description;
        } else {
            this.productId = productId;
        }
        this.qty = qty;
        this.unitPrice = unitPrice;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }

    @JsonIgnore
    public int getQuantity() { return qty; }
    public void setQuantity(int qty) { this.qty = qty; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public double getTotal() { return qty * unitPrice; }
    public void setTotal(double total) { this.total = total; }

    // Virtual getters to support UI without storing redundant fields in JSON
    @JsonIgnore
    public String getDescription() {
        if (productId == null || productId.isBlank()) {
            return "Unknown Item";
        }
        if ("Labour".equals(productId) || "Additional parts".equals(productId)) {
            return productId;
        }

        LocalCatalogRepository catalogRepo = new com.gui.kline.data.LocalCatalogRepository();
        Product p = catalogRepo.findProductById(productId);
        if (p != null) {
            String code = p.getCode();
            return (code == null || code.isBlank()) ? p.getName() : code + " - " + p.getName();
        }
        return productId;
    }

    @JsonIgnore
    public String getType() {
        if ("Labour".equals(productId) || "Additional parts".equals(productId)) {
            return "Service";
        }
        com.gui.kline.data.LocalCatalogRepository catalogRepo = new com.gui.kline.data.LocalCatalogRepository();
        Product p = catalogRepo.findProductById(productId);
        if (p != null) {
            return "Sale";
        }
        if (productId != null && !productId.isBlank()) {
            return "Service";
        }
        return "Sale";
    }
}
