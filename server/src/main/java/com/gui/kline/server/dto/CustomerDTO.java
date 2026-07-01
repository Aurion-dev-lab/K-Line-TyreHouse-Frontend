package com.gui.kline.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for Customer entity.
 */
@Data
@NoArgsConstructor
public class CustomerDTO {
    
    private String id;
    
    @NotBlank(message = "Customer name is required")
    @Size(max = 200)
    private String name;
    
    @Size(max = 200)
    private String companyName;
    
    @Size(max = 50)
    private String phone;
    
    @Size(max = 50)
    private String alternatePhone;
    
    @Size(max = 200)
    private String email;
    
    @Size(max = 500)
    private String address;
    
    @Size(max = 100)
    private String city;
    
    @Size(max = 100)
    private String state;
    
    @Size(max = 100)
    private String country;
    
    @Size(max = 20)
    private String postalCode;
    
    @Size(max = 100)
    private String taxId;
    
    private CustomerType customerType = CustomerType.RETAIL;
    
    @PositiveOrZero(message = "Credit limit must be positive or zero")
    private double creditLimit = 0;
    
    @PositiveOrZero(message = "Current credit must be positive or zero")
    private double currentCredit = 0;
    
    private boolean active = true;
    
    private LocalDate dateOfBirth;
    
    @Size(max = 500)
    private String notes;
    
    @Size(max = 100)
    private String category;
    
    @Size(max = 100)
    private String loyaltyProgramId;
    
    @PositiveOrZero(message = "Loyalty points must be positive or zero")
    private double loyaltyPoints = 0;
    
    private LocalDate memberSince;
    
    private String syncId;
    
    private String deviceId;
    
    private LocalDateTime syncedAt;
    
    private boolean syncStatus = false;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    // Calculated fields
    private Double availableCredit;
    
    private String fullAddress;
    
    public CustomerDTO(String id, String name, String phone) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.active = true;
        this.memberSince = LocalDate.now();
    }
    
    public enum CustomerType {
        RETAIL, WHOLESALE, CORPORATE, GOVERNMENT, INTERNATIONAL
    }
}