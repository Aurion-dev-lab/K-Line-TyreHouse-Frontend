package com.gui.kline.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for CreditSaleItem entity.
 */
@Data
@NoArgsConstructor
public class CreditSaleItemDTO {
    
    private String id;
    
    @NotBlank(message = "Credit sale ID is required")
    @Size(max = 100)
    private String creditSaleId;
    
    private int lineNumber = 1;
    
    @Size(max = 100)
    private String productId;
    
    @Size(max = 100)
    private String productCode;
    
    @NotBlank(message = "Description is required")
    @Size(max = 500)
    private String description;
    
    @Size(max = 100)
    private String category;
    
    private ItemType itemType = ItemType.PRODUCT;
    
    @PositiveOrZero(message = "Quantity must be positive or zero")
    private int quantity = 1;
    
    @PositiveOrZero(message = "Unit price must be positive or zero")
    private double unitPrice = 0;
    
    @PositiveOrZero(message = "Cost price must be positive or zero")
    private double costPrice = 0;
    
    @PositiveOrZero(message = "Discount must be positive or zero")
    private double discount = 0;
    
    private double discountType = 0; // 0 = percentage, 1 = fixed
    
    @PositiveOrZero(message = "Tax rate must be positive or zero")
    private double taxRate = 0;
    
    @PositiveOrZero(message = "Tax amount must be positive or zero")
    private double taxAmount = 0;
    
    @PositiveOrZero(message = "Total must be positive or zero")
    private double total = 0;
    
    @Size(max = 20)
    private String unit;
    
    @Size(max = 100)
    private String serialNumber;
    
    @Size(max = 100)
    private String batchNumber;
    
    @Size(max = 200)
    private String warranty;
    
    @Size(max = 500)
    private String notes;
    
    private String syncId;
    
    private String deviceId;
    
    private LocalDateTime syncedAt;
    
    private boolean syncStatus = false;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    // Calculated fields
    private Double profit;
    
    private Double profitTotal;
    
    public CreditSaleItemDTO(String id, String creditSaleId, String description, 
                            int quantity, double unitPrice, double total) {
        this.id = id;
        this.creditSaleId = creditSaleId;
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.total = total;
    }
    
    public enum ItemType {
        PRODUCT, SERVICE, DISCOUNT, TAX, SHIPPING, OTHER
    }
}