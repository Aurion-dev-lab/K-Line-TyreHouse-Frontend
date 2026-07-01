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
 * Payment record for credit sales.
 */
@Entity
@Table(name = "credit_sale_payments", indexes = {
    @Index(name = "idx_credit_payment_sale", columnList = "creditSaleId"),
    @Index(name = "idx_credit_payment_date", columnList = "paymentDate"),
    @Index(name = "idx_credit_payment_device", columnList = "deviceId")
})
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CreditSalePayment extends BaseEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String creditSaleId;

    @Column(nullable = false)
    private String creditSaleNumber;

    @Column(nullable = false)
    private LocalDate paymentDate;

    @Column
    private LocalDateTime paymentTime;

    @Column(nullable = false)
    private double amount = 0;

    @Column
    private String paymentMethod;

    @Column
    private String paymentReference; // Cheque number, transaction ID, etc.

    @Column
    private String bankName;

    @Column
    private String bankBranch;

    @Column
    private String collectedBy; // Worker ID who collected payment

    @Column
    private String collectedByName;

    @Column
    private String notes;

    @Column
    private PaymentStatus status = PaymentStatus.COMPLETED;

    @Column
    private String syncId; // For tracking sync status

    @Column
    private String deviceId; // Device that owns this payment

    @Column
    private LocalDateTime syncedAt;

    @Column
    private boolean syncStatus = false;

    public CreditSalePayment(String id, String creditSaleId, String creditSaleNumber, 
                            double amount, LocalDate paymentDate, String paymentMethod) {
        this.id = id;
        this.creditSaleId = creditSaleId;
        this.creditSaleNumber = creditSaleNumber;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.paymentTime = LocalDateTime.now();
        this.paymentMethod = paymentMethod;
        this.status = PaymentStatus.COMPLETED;
    }

    public enum PaymentStatus {
        PENDING, COMPLETED, FAILED, REFUNDED, CANCELLED, BOUNCED
    }

    public enum PaymentMethod {
        CASH, CHEQUE, BANK_TRANSFER, CREDIT_CARD, DEBIT_CARD, MOBILE_PAYMENT, ONLINE_PAYMENT, OTHER
    }
}