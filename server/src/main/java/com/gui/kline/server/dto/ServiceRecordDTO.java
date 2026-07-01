package com.gui.kline.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for ServiceRecord entity.
 */
@Data
@NoArgsConstructor
public class ServiceRecordDTO {
    
    private String id;
    
    @NotBlank(message = "Service name is required")
    @Size(max = 200)
    private String serviceName;
    
    @Size(max = 500)
    private String description;
    
    @Size(max = 100)
    private String category;
    
    private LocalDate serviceDate;
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;
    
    @Size(max = 100)
    private String assignedTo; // Worker ID
    
    @Size(max = 200)
    private String assignedToName;
    
    @Size(max = 100)
    private String customerId;
    
    @Size(max = 200)
    private String customerName;
    
    @Size(max = 50)
    private String vehicleNumber;
    
    @Size(max = 100)
    private String vehicleModel;
    
    @Size(max = 100)
    private String vehicleMake;
    
    @PositiveOrZero(message = "Price must be positive or zero")
    private double price;
    
    @PositiveOrZero(message = "Cost must be positive or zero")
    private double cost = 0;
    
    @PositiveOrZero(message = "Discount must be positive or zero")
    private double discount = 0;
    
    @Size(max = 20)
    private String discountType = "PERCENTAGE";
    
    @PositiveOrZero(message = "Tax rate must be positive or zero")
    private double taxRate = 0;
    
    @PositiveOrZero(message = "Tax amount must be positive or zero")
    private double taxAmount = 0;
    
    @PositiveOrZero(message = "Total amount must be positive or zero")
    private double totalAmount = 0;
    
    @Size(max = 10)
    private String currency = "LKR";
    
    @Size(max = 50)
    private String paymentMethod;
    
    private boolean paid = false;
    
    @Size(max = 200)
    private String paymentReference;
    
    @Size(max = 50)
    private String status = "COMPLETED";
    
    @Size(max = 500)
    private String notes;
    
    @Size(max = 1000)
    private String partsUsed; // JSON array of part IDs
    
    @Size(max = 1000)
    private String beforePhotos; // JSON array of photo URLs
    
    @Size(max = 1000)
    private String afterPhotos; // JSON array of photo URLs
    
    @Size(max = 100)
    private String warrantyPeriod;
    
    private LocalDate warrantyExpires;
    
    private String syncId;
    
    private String deviceId;
    
    private LocalDateTime syncedAt;
    
    private boolean syncStatus = false;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    // Calculated fields
    private Double profit;
    
    private Long durationMinutes;
    
    public ServiceRecordDTO(String id, String serviceName, double price, LocalDate serviceDate) {
        this.id = id;
        this.serviceName = serviceName;
        this.price = price;
        this.totalAmount = price;
        this.serviceDate = serviceDate;
        this.startTime = LocalDateTime.now();
        this.status = "COMPLETED";
    }
    
    public enum Status {
        SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED, PAID, UNPAID
    }
}