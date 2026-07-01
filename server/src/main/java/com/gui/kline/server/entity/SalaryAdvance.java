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
 * Salary advance given to workers.
 */
@Entity
@Table(name = "salary_advances", indexes = {
    @Index(name = "idx_salary_advance_worker", columnList = "workerId"),
    @Index(name = "idx_salary_advance_date", columnList = "advanceDate"),
    @Index(name = "idx_salary_advance_status", columnList = "status"),
    @Index(name = "idx_salary_advance_device", columnList = "deviceId")
})
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SalaryAdvance extends BaseEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String workerId;

    @Column
    private String workerName;

    @Column(nullable = false)
    private LocalDate advanceDate;

    @Column
    private LocalDateTime advanceTime;

    @Column(nullable = false)
    private double amount = 0;

    @Column
    private String currency = "LKR";

    @Column
    private String paymentMethod;

    @Column
    private String paymentReference; // Cheque number, transaction ID, etc.

    @Column
    private String purpose;

    @Column
    private String notes;

    @Column(nullable = false)
    private AdvanceStatus status = AdvanceStatus.PENDING;

    @Column
    private String approvedBy;

    @Column
    private LocalDateTime approvedAt;

    @Column
    private String settledBy;

    @Column
    private LocalDateTime settledAt;

    @Column
    private String settlementMethod;

    @Column
    private String settlementReference;

    @Column
    private double settlementAmount = 0;

    @Column
    private double remainingAmount = 0;

    @Column
    private LocalDate repaymentDate;

    @Column
    private String syncId; // For tracking sync status

    @Column
    private String deviceId; // Device that owns this advance

    @Column
    private LocalDateTime syncedAt;

    @Column
    private boolean syncStatus = false;

    public SalaryAdvance(String id, String workerId, String workerName, 
                       double amount, LocalDate advanceDate, String purpose) {
        this.id = id;
        this.workerId = workerId;
        this.workerName = workerName;
        this.amount = amount;
        this.advanceDate = advanceDate;
        this.advanceTime = LocalDateTime.now();
        this.purpose = purpose;
        this.status = AdvanceStatus.PENDING;
        this.remainingAmount = amount;
    }

    public enum AdvanceStatus {
        PENDING, APPROVED, REJECTED, PAID, PARTIALLY_SETTLED, FULLY_SETTLED, CANCELLED
    }

    public void setSettledAmount(double amount) {
        this.settlementAmount = amount;
        this.remainingAmount = this.amount - this.settlementAmount;
        if (this.remainingAmount <= 0) {
            this.status = AdvanceStatus.FULLY_SETTLED;
        } else if (this.settlementAmount > 0) {
            this.status = AdvanceStatus.PARTIALLY_SETTLED;
        }
    }

    public boolean isFullySettled() {
        return this.remainingAmount <= 0;
    }

    public boolean isPartiallySettled() {
        return this.settlementAmount > 0 && this.remainingAmount > 0;
    }

    public boolean isNotSettled() {
        return this.settlementAmount == 0 && this.remainingAmount > 0;
    }

    public int getDaysSinceAdvance() {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(this.advanceDate, LocalDate.now());
    }

    public boolean isOverdue() {
        return this.repaymentDate != null && this.repaymentDate.isBefore(LocalDate.now()) && !isFullySettled();
    }

    public int getDaysOverdue() {
        if (this.repaymentDate == null || !this.isOverdue()) return 0;
        return (int) java.time.temporal.ChronoUnit.DAYS.between(this.repaymentDate, LocalDate.now());
    }
}