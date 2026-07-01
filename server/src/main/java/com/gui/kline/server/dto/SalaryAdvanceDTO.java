package com.gui.kline.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for SalaryAdvance entity.
 */
@Data
@NoArgsConstructor
public class SalaryAdvanceDTO {
    
    private String id;
    
    @NotBlank(message = "Worker ID is required")
    @Size(max = 100)
    private String workerId;
    
    @Size(max = 200)
    private String workerName;
    
    private LocalDate advanceDate;
    
    @PositiveOrZero(message = "Amount must be positive or zero")
    private double amount = 0;
    
    @Size(max = 500)
    private String reason;
    
    @Size(max = 50)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED, SETTLED, CANCELLED
    
    private LocalDate repaymentStartDate;
    
    private LocalDate repaymentEndDate;
    
    private int installmentMonths = 0;
    
    @PositiveOrZero(message = "Monthly deduction must be positive or zero")
    private double monthlyDeduction = 0;
    
    @PositiveOrZero(message = "Settled amount must be positive or zero")
    private double settledAmount = 0;
    
    @PositiveOrZero(message = "Remaining amount must be positive or zero")
    private double remainingAmount = 0;
    
    @Size(max = 10)
    private String currency = "LKR";
    
    @Size(max = 50)
    private String paymentMethod;
    
    @Size(max = 200)
    private String paymentReference;
    
    private String approvedBy;
    
    private LocalDateTime approvedAt;
    
    private String settledBy;
    
    private LocalDateTime settledAt;
    
    private String syncId;
    
    private String deviceId;
    
    private LocalDateTime syncedAt;
    
    private boolean syncStatus = false;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    // Calculated fields
    private boolean isSettled;
    
    private boolean isPartiallySettled;
    
    public SalaryAdvanceDTO(String id, String workerId, String workerName, double amount) {
        this.id = id;
        this.workerId = workerId;
        this.workerName = workerName;
        this.amount = amount;
        this.advanceDate = LocalDate.now();
        this.status = "PENDING";
        this.remainingAmount = amount;
    }
    
    public enum Status {
        PENDING, APPROVED, REJECTED, SETTLED, PARTIALLY_SETTLED, CANCELLED
    }
}