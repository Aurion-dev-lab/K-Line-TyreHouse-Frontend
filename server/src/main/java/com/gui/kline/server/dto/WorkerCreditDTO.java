package com.gui.kline.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for WorkerCredit entity.
 */
@Data
@NoArgsConstructor
public class WorkerCreditDTO {
    
    private String id;
    
    @NotBlank(message = "Worker ID is required")
    @Size(max = 100)
    private String workerId;
    
    @Size(max = 200)
    private String workerName;
    
    private LocalDate creditDate;
    
    @PositiveOrZero(message = "Amount must be positive or zero")
    private double amount = 0;
    
    @Size(max = 500)
    private String description;
    
    @Size(max = 50)
    private String type = "ADVANCE"; // ADVANCE, LOAN, BONUS, REIMBURSEMENT, OTHER
    
    @Size(max = 50)
    private String status = "ACTIVE"; // ACTIVE, SETTLED, CANCELLED
    
    @Size(max = 10)
    private String currency = "LKR";
    
    @Size(max = 50)
    private String paymentMethod;
    
    @Size(max = 200)
    private String paymentReference;
    
    @PositiveOrZero(message = "Settled amount must be positive or zero")
    private double settledAmount = 0;
    
    @PositiveOrZero(message = "Remaining amount must be positive or zero")
    private double remainingAmount = 0;
    
    private LocalDate settlementDueDate;
    
    private LocalDate settledDate;
    
    private String settledBy;
    
    private String notes;
    
    private String syncId;
    
    private String deviceId;
    
    private LocalDateTime syncedAt;
    
    private boolean syncStatus = false;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    // Calculated fields
    private boolean isSettled;
    
    private boolean isOverdue;
    
    public WorkerCreditDTO(String id, String workerId, String workerName, double amount) {
        this.id = id;
        this.workerId = workerId;
        this.workerName = workerName;
        this.amount = amount;
        this.creditDate = LocalDate.now();
        this.status = "ACTIVE";
        this.remainingAmount = amount;
    }
    
    public enum CreditType {
        ADVANCE, LOAN, BONUS, REIMBURSEMENT, SALARY_ADVANCE, OTHER
    }
    
    public enum Status {
        ACTIVE, SETTLED, PARTIALLY_SETTLED, CANCELLED, OVERDUE
    }
}