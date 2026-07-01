package com.gui.kline.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Product entity.
 */
@Data
@NoArgsConstructor
public class ProductDTO {
    
    private String id;
    
    @NotBlank(message = "Product code is required")
    @Size(max = 100)
    private String productCode;
    
    @NotBlank(message = "Product name is required")
    @Size(max = 200)
    private String name;
    
    @Size(max = 500)
    private String description;
    
    @Size(max = 100)
    private String category;
    
    @Size(max = 100)
    private String brand;
    
    @Size(max = 100)
    private String model;
    
    @Size(max = 50)
    private String size;
    
    @PositiveOrZero(message = "Buy price must be positive or zero")
    private double buyPrice;
    
    @PositiveOrZero(message = "Sell price must be positive or zero")
    private double sellPrice;
    
    @PositiveOrZero(message = "Stock must be positive or zero")
    private int stock;
    
    @PositiveOrZero(message = "Minimum stock level must be positive or zero")
    private int minStockLevel = 0;
    
    @Size(max = 20)
    private String unit;
    
    @Size(max = 100)
    private String barcode;
    
    @Size(max = 200)
    private String supplier;
    
    @Size(max = 50)
    private String supplierContact;
    
    private boolean active = true;
    
    private boolean featured = false;
    
    @PositiveOrZero(message = "Weight must be positive or zero")
    private double weight;
    
    @Size(max = 100)
    private String dimensions;
    
    @Size(max = 50)
    private String color;
    
    @Size(max = 200)
    private String warranty;
    
    @Size(max = 200)
    private String tags;
    
    @Size(max = 500)
    private String imageUrl;
    
    private String syncId;
    
    private String deviceId;
    
    private LocalDateTime syncedAt;
    
    private boolean syncStatus = false;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    // Calculated fields
    private Double profitMargin;
    
    private Double profit;
    
    private boolean lowStock;
    
    public ProductDTO(String id, String productCode, String name, String category, 
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
}