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
 * DTO for Invoice entity.
 */
@Data
@NoArgsConstructor
public class InvoiceDTO {
    
    private String id;
    
    @NotBlank(message = "Invoice number is required")
    @Size(max = 100)
    private String invoiceNumber;
    
    private LocalDate invoiceDate;
    
    private LocalDate dueDate;
    
    @Size(max = 100)
    private String customerId;
    
    @Size(max = 200)
    private String customerName;
    
    @Size(max = 50)
    private String customerPhone;
    
    private InvoiceType type = InvoiceType.CASH;
    
    private InvoiceStatus status = InvoiceStatus.DRAFT;
    
    @Size(max = 50)
    private String paymentMethod;
    
    @Size(max = 200)
    private String paymentReference;
    
    @PositiveOrZero(message = "Subtotal must be positive or zero")
    private double subtotal = 0;
    
    @PositiveOrZero(message = "Tax rate must be positive or zero")
    private double taxRate = 0;
    
    @PositiveOrZero(message = "Tax amount must be positive or zero")
    private double taxAmount = 0;
    
    @PositiveOrZero(message = "Discount must be positive or zero")
    private double discount = 0;
    
    private double discountType = 0; // 0 = percentage, 1 = fixed
    
    @PositiveOrZero(message = "Shipping must be positive or zero")
    private double shipping = 0;
    
    @PositiveOrZero(message = "Grand total must be positive or zero")
    private double grandTotal = 0;
    
    @PositiveOrZero(message = "Amount paid must be positive or zero")
    private double amountPaid = 0;
    
    @PositiveOrZero(message = "Balance due must be positive or zero")
    private double balanceDue = 0;
    
    @Size(max = 10)
    private String currency = "LKR";
    
    @Size(max = 500)
    private String notes;
    
    @Size(max = 1000)
    private String termsAndConditions;
    
    @Size(max = 100)
    private String salespersonId;
    
    @Size(max = 200)
    private String salespersonName;
    
    @Size(max = 200)
    private String location;
    
    private boolean isCredit = false;
    
    @Size(max = 100)
    private String creditSaleId; // Link to credit sale if applicable
    
    private List<InvoiceLineItemDTO> lineItems = new java.util.ArrayList<>();
    
    private String syncId;
    
    private String deviceId;
    
    private LocalDateTime syncedAt;
    
    private boolean syncStatus = false;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    // Calculated fields
    private boolean fullyPaid;
    
    private boolean partiallyPaid;
    
    private boolean unpaid;
    
    private Double totalProfit;
    
    public InvoiceDTO(String id, String invoiceNumber, String customerId, String customerName) {
        this.id = id;
        this.invoiceNumber = invoiceNumber;
        this.customerId = customerId;
        this.customerName = customerName;
        this.invoiceDate = LocalDate.now();
        this.status = InvoiceType.CASH == type ? InvoiceStatus.COMPLETED : InvoiceStatus.DRAFT;
    }
    
    public enum InvoiceType {
        CASH, CREDIT, QUOTATION, RETURN, PROFORMA
    }
    
    public enum InvoiceStatus {
        DRAFT, PENDING, COMPLETED, PAID, PARTIALLY_PAID, CANCELLED, RETURNED, REFUNDED
    }
}