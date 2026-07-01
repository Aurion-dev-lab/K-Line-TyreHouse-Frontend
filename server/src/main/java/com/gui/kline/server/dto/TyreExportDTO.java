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
 * DTO for TyreExport entity.
 */
@Data
@NoArgsConstructor
public class TyreExportDTO {
    
    private String id;
    
    @NotBlank(message = "Export number is required")
    @Size(max = 100)
    private String exportNumber;
    
    private LocalDate exportDate;
    
    private ExportOperation operation = ExportOperation.EXPORT;
    
    @NotBlank(message = "Company is required")
    @Size(max = 200)
    private String company;
    
    @Size(max = 200)
    private String companyContact;
    
    @Size(max = 50)
    private String companyPhone;
    
    @Size(max = 500)
    private String companyAddress;
    
    @PositiveOrZero(message = "Number of tyres must be positive or zero")
    private int tyres = 0;
    
    @Size(max = 500)
    private String tyreSpecs;
    
    @PositiveOrZero(message = "Customer price must be positive or zero")
    private double custPrice = 0;
    
    @PositiveOrZero(message = "Company price must be positive or zero")
    private double compPrice = 0;
    
    @PositiveOrZero(message = "Service fee must be positive or zero")
    private double serviceFee = 0;
    
    @PositiveOrZero(message = "Transport cost must be positive or zero")
    private double transportCost = 0;
    
    @PositiveOrZero(message = "Other costs must be positive or zero")
    private double otherCosts = 0;
    
    @PositiveOrZero(message = "Total amount must be positive or zero")
    private double totalAmount = 0;
    
    @PositiveOrZero(message = "Paid amount must be positive or zero")
    private double paidAmount = 0;
    
    @PositiveOrZero(message = "Balance amount must be positive or zero")
    private double balanceAmount = 0;
    
    @Size(max = 10)
    private String currency = "LKR";
    
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    
    private ExportStatus status = ExportStatus.DRAFT;
    
    @Size(max = 50)
    private String paymentMethod;
    
    @Size(max = 200)
    private String paymentReference;
    
    private LocalDate paymentDueDate;
    
    @Size(max = 200)
    private String driverName;
    
    @Size(max = 50)
    private String vehicleNumber;
    
    @Size(max = 500)
    private String notes;
    
    @Size(max = 1000)
    private String termsAndConditions;
    
    private List<TyreExportPaymentDTO> payments = new java.util.ArrayList<>();
    
    private String syncId;
    
    private String deviceId;
    
    private LocalDateTime syncedAt;
    
    private boolean syncStatus = false;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    // Calculated fields
    private Double profit;
    
    private Double totalProfit;
    
    private Double totalCost;
    
    private boolean profitable;
    
    private boolean overdue;
    
    private Integer daysOverdue;
    
    public TyreExportDTO(String id, String exportNumber, String company, 
                        ExportOperation operation, int tyres, double custPrice, double compPrice) {
        this.id = id;
        this.exportNumber = exportNumber;
        this.company = company;
        this.operation = operation;
        this.tyres = tyres;
        this.custPrice = custPrice;
        this.compPrice = compPrice;
        this.exportDate = LocalDate.now();
        this.status = ExportStatus.DRAFT;
        this.paymentStatus = PaymentStatus.PENDING;
        this.totalAmount = custPrice * tyres;
    }
    
    public enum ExportOperation {
        EXPORT, IMPORT, PURCHASE, SALE, TRANSFER
    }
    
    public enum ExportStatus {
        DRAFT, PENDING, IN_PROGRESS, COMPLETED, DELIVERED, RECEIVED, CANCELLED, RETURNED
    }
    
    public enum PaymentStatus {
        PENDING, PARTIAL, PAID, OVERDUE, CANCELLED, REFUNDED
    }
}