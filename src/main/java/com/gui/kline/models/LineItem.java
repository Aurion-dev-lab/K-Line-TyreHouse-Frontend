package com.gui.kline.models;

import java.time.LocalDateTime;

public class LineItem {
    private String id;
    private String invoiceId;
    private String invoiceRef;
    private String description;
    private String type;
    private String productId;
    private int qty;
    private double unitPrice;
    private double total;
    
    // Sync fields
    private boolean syncStatus = false;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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
    public int getQuantity() { return qty; }
    public void setQty(int qty) { this.qty = qty; }
    
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
    
    /**
     * A line item's amount is always derived from its quantity and unit price.
     * Keeping this calculation here prevents quotation totals from becoming
     * stale (or remaining at the default Rs. 0.00) when either value changes.
     */
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

