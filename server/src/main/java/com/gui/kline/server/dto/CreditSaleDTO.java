package com.gui.kline.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for CreditSale entity.
 */
@Data
@NoArgsConstructor
public class CreditSaleDTO {
    
    private String id;
    
    @NotBlank(message = "Credit sale number is required")
    @Size(max = 100)
    private String creditSaleNumber;
    
    private LocalDate saleDate;
    
    private LocalDate dueDate;
    
    @NotBlank(message = "Customer ID is required")
    @Size(max = 100)
    private String customerId;
    
    @NotBlank(message = "Customer name is required")
    @Size(max = 200)
    private String customerName;
    
    @Size(max = 50)
    private String customerPhone;
    
    @Size(max = 100)
    private String salespersonId;
    
    @Size(max = 200)
    private String salespersonName;
    
    @PositiveOrZero(message = "Subtotal must be positive or zero")
    private double subtotal = 0;
    
    @PositiveOrZero(message = "Tax rate must be positive or zero")
    private double taxRate = 0;
    
    @PositiveOrZero(message = "Tax amount must be positive or zero")
    private double taxAmount = 0;
    
    @PositiveOrZero(message = "Discount must be positive or zero")
    private double discount = 0;
    
    @PositiveOrZero(message = "Shipping must be positive or zero")
    private double shipping = 0;
    
    @PositiveOrZero(message = "Amount must be positive or zero")
    private double amount = 0; // Total amount
    
    @PositiveOrZero(message = "Paid amount must be positive or zero")
    private double paidAmount = 0;
    
    @PositiveOrZero(message = "Balance amount must be positive or zero")
    private double balanceAmount = 0;
    
    @Size(max = 10)
    private String currency = "LKR";
    
    private CreditStatus status = CreditStatus.PENDING;
    
    @Size(max = 200)
    private String paymentTerms;
    
    private LocalDate firstPaymentDate;
    
    @PositiveOrZero(message = "First payment amount must be positive or zero")
    private double firstPaymentAmount = 0;
    
    @Size(max = 500)
    private String notes;
    
    @Size(max = 1000)
    private String termsAndConditions;
    
    @Size(max = 100)
    private String approvedBy;
    
    private LocalDateTime approvedAt;
    
    private List<CreditSaleItemDTO> items = new java.util.ArrayList<>();
    
    private List<CreditSalePaymentDTO> payments = new java.util.ArrayList<>();
    
    private String syncId;
    
    private String deviceId;
    
    private LocalDateTime syncedAt;
    
    private boolean syncStatus = false;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    // Calculated fields
    private boolean overdue;
    
    private boolean fullyPaid;
    
    private boolean partiallyPaid;
    
    private Double totalProfit;
    
    private Integer daysOverdue;
    
    public CreditSaleDTO(String id, String creditSaleNumber, String customerId, String customerName) {
        this.id = id;
        this.creditSaleNumber = creditSaleNumber;
        this.customerId = customerId;
        this.customerName = customerName;
        this.saleDate = LocalDate.now();
        this.status = CreditStatus.PENDING;
    }
    
    public enum CreditStatus {
        DRAFT, PENDING, APPROVED, ACTIVE, PARTIALLY_PAID, PAID, OVERDUE, CANCELLED, RETURNED
    }
}