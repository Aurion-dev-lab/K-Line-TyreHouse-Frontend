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
 * Individual item in a credit sale.
 */
@Entity
@Table(name = "credit_sale_items", indexes = {
    @Index(name = "idx_credit_sale_item_sale", columnList = "creditSaleId"),
    @Index(name = "idx_credit_sale_item_product", columnList = "productId")
})
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CreditSaleItem extends BaseEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String creditSaleId;

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

    @Column(nullable = false)
    private int quantity = 1;

    @Column(nullable = false)
    private double unitPrice = 0;

    @Column
    private double costPrice = 0;

    @Column
    private double discount = 0;

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
    private String warranty;

    @Column
    private String syncId; // For tracking sync status

    @Column
    private String deviceId; // Device that owns this item

    @Column
    private LocalDateTime syncedAt;

    @Column
    private boolean syncStatus = false;

    public CreditSaleItem(String id, String creditSaleId, String description, 
                         int quantity, double unitPrice, double total) {
        this.id = id;
        this.creditSaleId = creditSaleId;
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.total = total;
    }

    public double getProfit() {
        return this.unitPrice - this.costPrice;
    }

    public double getProfitTotal() {
        return getProfit() * this.quantity;
    }

    public void recalculateTotal() {
        double baseTotal = this.unitPrice * this.quantity;
        double discountAmount = this.discount > 0 ? baseTotal * (this.discount / 100) : 0;
        this.total = baseTotal - discountAmount + this.taxAmount;
    }
}