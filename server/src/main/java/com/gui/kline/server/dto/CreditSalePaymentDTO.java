package com.gui.kline.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for CreditSalePayment entity.
 */
@Data
@NoArgsConstructor
public class CreditSalePaymentDTO {
    
    private String id;
    
    @NotBlank(message = "Credit sale ID is required")
    @Size(max = 100)
    private String creditSaleId;
    
    @Size(max = 100)
    private String creditSaleNumber;
    
    private LocalDate paymentDate;
    
    @PositiveOrZero(message = "Amount must be positive or zero")
    private double amount = 0;
    
    @Size(max = 50)
    private String paymentMethod;
    
    @Size(max = 200)
    private String paymentReference;
    
    @Size(max = 500)
    private String notes;
    
    @Size(max = 10)
    private String currency = "LKR";
    
    private String receivedBy; // User who recorded the payment
    
    private LocalDateTime receivedAt;
    
    private String verifiedBy;
    
    private LocalDateTime verifiedAt;
    
    private String syncId;
    
    private String deviceId;
    
    private LocalDateTime syncedAt;
    
    private boolean syncStatus = false;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    public CreditSalePaymentDTO(String id, String creditSaleId, double amount, LocalDate paymentDate) {
        this.id = id;
        this.creditSaleId = creditSaleId;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.receivedAt = LocalDateTime.now();
    }
    
    public enum PaymentMethod {
        CASH, CHEQUE, BANK_TRANSFER, CARD, MOBILE, ONLINE, OTHER
    }
}