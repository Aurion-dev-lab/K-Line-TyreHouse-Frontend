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
 * Product entity representing items in the inventory.
 * Contains product details, pricing, stock information, and sync metadata.
 */
@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_product_code", columnList = "productCode", unique = true),
    @Index(name = "idx_product_category", columnList = "category"),
    @Index(name = "idx_product_name", columnList = "name")
})
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Product extends BaseEntity {

    @Id
    private String id;

    @Column(unique = true, nullable = false)
    private String productCode;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column
    private String category;

    @Column
    private String brand;

    @Column
    private String model;

    @Column
    private String size;

    @Column(nullable = false)
    private double buyPrice;

    @Column(nullable = false)
    private double sellPrice;

    @Column(nullable = false)
    private int stock;

    @Column
    private int minStockLevel = 0;

    @Column
    private String unit;

    @Column
    private String barcode;

    @Column
    private String supplier;

    @Column
    private String supplierContact;

    @Column
    private boolean active = true;

    @Column
    private boolean featured = false;

    @Column
    private double weight; // in kg

    @Column
    private String dimensions; // LxWxH

    @Column
    private String color;

    @Column
    private String warranty;

    @Column
    private String tags;

    @Column
    private String imageUrl;

    @Column
    private String syncId; // For tracking sync status

    @Column
    private String deviceId; // Device that owns this product

    @Column
    private LocalDateTime syncedAt;

    @Column
    private boolean syncStatus = false;

    public Product(String id, String productCode, String name, String category, 
                   double buyPrice, double sellPrice, int stock) {
        this.id = id;
        this.productCode = productCode;
        this.name = name;
        this.category = category;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.stock = stock;
        this.active = true;
    }

    public double getProfitMargin() {
        if (buyPrice <= 0) return 0;
        return ((sellPrice - buyPrice) / buyPrice) * 100;
    }

    public double getProfit() {
        return sellPrice - buyPrice;
    }

    public boolean isLowStock() {
        return stock <= minStockLevel;
    }
}