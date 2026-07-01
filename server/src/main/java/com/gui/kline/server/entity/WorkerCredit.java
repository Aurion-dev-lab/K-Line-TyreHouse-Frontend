package com.gui.kline.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Worker credit (given to workers for purchases, etc.)
 */
@Entity
@Table(name = "worker_credits", indexes = {
    @Index(name = "idx_worker_credit_worker", columnList = "workerId"),
    @Index(name = "idx_worker_credit_date", columnList = "creditDate"),
    @Index(name = "idx_worker_credit_type", columnList = "creditType"),
    @Index(name = "idx_worker_credit_device", columnList = "deviceId")
})
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WorkerCredit extends BaseEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String workerId;

    @Column
    private String workerName;

    @Column(nullable = false)
    private LocalDate creditDate;

    @Column
    private LocalDateTime creditTime;

    @Column(nullable = false)
    private double amount = 0;

    @Column
    private String currency = "LKR";

    @Column(nullable = false)
    private CreditType creditType = CreditType.GIVEN;

    @Column
    private String purpose;

    @Column
    private String notes;

    @Column
    private String referenceNumber;

    @Column(nullable = false)
    private CreditStatus status = CreditStatus.ACTIVE;

    @Column
    private String settledBy;

    @Column
    private LocalDateTime settledAt;

    @Column
    private String settlementMethod;

    @Column
    private String settlementReference;

    @Column
    private double settledAmount = 0;

    @Column
    private double remainingAmount = 0;

    @Column
    private String syncId; // For tracking sync status

    @Column
    private String deviceId; // Device that owns this credit

    @Column
    private LocalDateTime syncedAt;

    @Column
    private boolean syncStatus = false;

    public WorkerCredit(String id, String workerId, String workerName, 
                       double amount, LocalDate creditDate, CreditType creditType) {
        this.id = id;
        this.workerId = workerId;
        this.workerName = workerName;
        this.amount = amount;
        this.creditDate = creditDate;
        this.creditTime = LocalDateTime.now();
        this.creditType = creditType;
        this.status = CreditStatus.ACTIVE;
        this.remainingAmount = amount;
    }

    public enum CreditType {
        GIVEN, SETTLED, ADVANCE, LOAN, REIMBURSEMENT, BONUS, COMMISSION
    }

    public enum CreditStatus {
        ACTIVE, PARTIALLY_SETTLED, FULLY_SETTLED, CANCELLED, EXPIRED
    }

    public void setSettledAmount(double amount) {
        this.settledAmount = amount;
        this.remainingAmount = this.amount - this.settledAmount;
        if (this.remainingAmount <= 0) {
            this.status = CreditStatus.FULLY_SETTLED;
        } else if (this.settledAmount > 0) {
            this.status = CreditStatus.PARTIALLY_SETTLED;
        }
    }

    public boolean isFullySettled() {
        return this.remainingAmount <= 0;
    }

    public boolean isPartiallySettled() {
        return this.settledAmount > 0 && this.remainingAmount > 0;
    }

    public boolean isActive() {
        return this.status == CreditStatus.ACTIVE || this.status == CreditStatus.PARTIALLY_SETTLED;
    }

    public int getDaysSinceCredit() {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(this.creditDate, LocalDate.now());
    }

    public boolean isOverdue() {
        // Worker credits are typically settled within a month
        return this.creditDate != null && 
               this.creditDate.plusDays(30).isBefore(LocalDate.now()) && 
               !isFullySettled();
    }

    public int getDaysOverdue() {
        if (!this.isOverdue()) return 0;
        return (int) java.time.temporal.ChronoUnit.DAYS.between(
            this.creditDate.plusDays(30), LocalDate.now());
    }

    public double getNetAmount() {
        return this.amount - this.settledAmount;
    }
}