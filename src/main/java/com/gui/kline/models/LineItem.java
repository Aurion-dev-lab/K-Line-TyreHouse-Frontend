package com.gui.kline.models;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LineItem {
    @JsonProperty("id")
    private String id;

    @JsonProperty("invoiceId")
    @JsonAlias({"invoice_id"})
    private String invoiceId;

    @JsonProperty("invoiceRef")
    @JsonAlias({"invoice_ref"})
    private String invoiceRef;

    @JsonProperty("description")
    private String description;

    @JsonProperty("type")
    private String type;

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
    
    // Sync fields
    @JsonProperty("syncStatus")
    @JsonAlias({"sync_status"})
    private boolean syncStatus = false;

    @JsonProperty("createdAt")
    @JsonAlias({"created_at"})
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    @JsonAlias({"updated_at"})
    private LocalDateTime updatedAt;

    public LineItem() {}

    public LineItem(String description, String type, int qty, double unitPrice) {
        this(description, type, qty, unitPrice, null);
    }

    public LineItem(String description, String type, int qty, double unitPrice, String productId) {
        this.description = description; 
        this.type = type;
        this.qty = qty; 
        this.unitPrice = unitPrice;
        this.productId = productId;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public int getQty() { return qty; }

    @JsonIgnore
    public int getQuantity() { return qty; }

    public void setQty(int qty) { this.qty = qty; }

    public void setQuantity(int qty) { this.qty = qty; }
    
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
    
    public double getTotal() { return qty * unitPrice; }
    public void setTotal(double total) { this.total = total; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getInvoiceId() { return invoiceId; }
    public void setInvoiceId(String invoiceId) { this.invoiceId = invoiceId; }
    
    public String getInvoiceRef() { return invoiceRef; }
    public void setInvoiceRef(String invoiceRef) { this.invoiceRef = invoiceRef; }

    // Sync field getters and setters

    public boolean isSyncStatus() { return syncStatus; }
    public void setSyncStatus(boolean syncStatus) { this.syncStatus = syncStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

