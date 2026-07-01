package com.gui.kline.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for QuickService entity.
 */
@Data
@NoArgsConstructor
public class QuickServiceDTO {
    
    private String id;
    
    @NotBlank(message = "Service name is required")
    @Size(max = 200)
    private String service;
    
    @Size(max = 500)
    private String description;
    
    private LocalDate serviceDate;
    
    private LocalDateTime serviceTime;
    
    @Size(max = 100)
    private String customerId;
    
    @Size(max = 200)
    private String customerName;
    
    @Size(max = 50)
    private String customerPhone;
    
    @Size(max = 50)
    private String vehicleNumber;
    
    @Size(max = 100)
    private String vehicleModel;
    
    @Size(max = 100)
    private String vehicleMake;
    
    @PositiveOrZero(message = "Price must be positive or zero")
    private double price;
    
    @Size(max = 10)
    private String currency = "LKR";
    
    @Size(max = 50)
    private String paymentMethod;
    
    private boolean paid = false;
    
    @Size(max = 200)
    private String paymentReference;
    
    @Size(max = 500)
    private String notes;
    
    @Size(max = 100)
    private String assignedTo; // Worker ID
    
    @Size(max = 200)
    private String assignedToName;
    
    @Size(max = 50)
    private String status = "COMPLETED";
    
    private String syncId;
    
    private String deviceId;
    
    private LocalDateTime syncedAt;
    
    private boolean syncStatus = false;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    public QuickServiceDTO(String id, String service, double price, LocalDate serviceDate) {
        this.id = id;
        this.service = service;
        this.price = price;
        this.serviceDate = serviceDate;
        this.serviceTime = LocalDateTime.now();
        this.status = "COMPLETED";
    }
    
    public enum Status {
        SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED, PAID, UNPAID
    }
}