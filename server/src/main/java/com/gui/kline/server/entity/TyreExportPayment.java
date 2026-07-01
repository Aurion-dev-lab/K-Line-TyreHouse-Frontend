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
 * Payment record for tyre exports.
 */
@Entity
@Table(name = "tyre_export_payments", indexes = {
    @Index(name = "idx_tyre_export_payment_export", columnList = "tyreExportId"),
    @Index(name = "idx_tyre_export_payment_date", columnList = "paymentDate"),
    @Index(name = "idx_tyre_export_payment_device", columnList = "deviceId")
})
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TyreExportPayment extends BaseEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String tyreExportId;

    @Column(nullable = false)
    private String exportNumber;

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
    private String paidBy; // Worker ID who made the payment

    @Column
    private String paidByName;

    @Column
    private String receivedBy; // Worker ID who received the payment

    @Column
    private String receivedByName;

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

    public TyreExportPayment(String id, String tyreExportId, String exportNumber,
                           double amount, LocalDate paymentDate, String paymentMethod) {
        this.id = id;
        this.tyreExportId = tyreExportId;
        this.exportNumber = exportNumber;
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