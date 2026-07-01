package com.gui.kline.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Invoice line item representing individual products/services on an invoice.
 */
@Entity
@Table(name = "invoice_line_items", indexes = {
    @Index(name = "idx_line_item_invoice", columnList = "invoiceId"),
    @Index(name = "idx_line_item_product", columnList = "productId")
})
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InvoiceLineItem extends BaseEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String invoiceId;

    @Column
    private int lineNumber = 1;

    @Column
    private String productId;

    @Column
    private String productCode;

    @Column(nullable = false)
    private String description;

    @Column
    private String category;

    @Column
    private ItemType itemType = ItemType.PRODUCT;

    @Column(nullable = false)
    private int quantity = 1;

    @Column(nullable = false)
    private double unitPrice = 0;

    @Column
    private double costPrice = 0;

    @Column
    private double discount = 0;

    @Column
    private double discountType = 0; // 0 = percentage, 1 = fixed

    @Column
    private double taxRate = 0;

    @Column
    private double taxAmount = 0;

    @Column(nullable = false)
    private double total = 0;

    @Column
    private String unit;

    @Column
    private String serialNumber;

    @Column
    private String batchNumber;

    @Column
    private String warranty;

    @Column
    private String notes;

    @Column
    private String syncId; // For tracking sync status

    @Column
    private String deviceId; // Device that owns this line item

    @Column
    private LocalDateTime syncedAt;

    @Column
    private boolean syncStatus = false;

    public InvoiceLineItem(String id, String invoiceId, String description, 
                         int quantity, double unitPrice, double total) {
        this.id = id;
        this.invoiceId = invoiceId;
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.total = total;
    }

    public enum ItemType {
        PRODUCT, SERVICE, DISCOUNT, TAX, SHIPPING, OTHER
    }

    public double getProfit() {
        return this.unitPrice - this.costPrice;
    }

    public double getProfitTotal() {
        return getProfit() * this.quantity;
    }

    public void recalculateTotal() {
        double baseTotal = this.unitPrice * this.quantity;
        double discountAmount = this.discountType == 0 ? 
            baseTotal * (this.discount / 100) : this.discount;
        this.total = baseTotal - discountAmount + this.taxAmount;
    }

    public boolean isDiscounted() {
        return this.discount > 0;
    }

    public boolean isTaxable() {
        return this.taxRate > 0;
    }
}