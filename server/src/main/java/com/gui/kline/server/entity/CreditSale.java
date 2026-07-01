package com.gui.kline.server.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * CreditSale entity representing sales on credit terms.
 */
@Entity
@Table(name = "credit_sales", indexes = {
    @Index(name = "idx_credit_sale_number", columnList = "creditSaleNumber", unique = true),
    @Index(name = "idx_credit_sale_date", columnList = "saleDate"),
    @Index(name = "idx_credit_sale_customer", columnList = "customerId"),
    @Index(name = "idx_credit_sale_status", columnList = "status"),
    @Index(name = "idx_credit_sale_device", columnList = "deviceId")
})
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CreditSale extends BaseEntity {

    @Id
    private String id;

    @Column(unique = true, nullable = false)
    private String creditSaleNumber;

    @Column(nullable = false)
    private LocalDate saleDate;

    @Column
    private LocalDate dueDate;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false)
    private String customerName;

    @Column
    private String customerPhone;

    @Column
    private String salespersonId;

    @Column
    private String salespersonName;

    @Column(nullable = false)
    private double subtotal = 0;

    @Column
    private double taxRate = 0;

    @Column
    private double taxAmount = 0;

    @Column
    private double discount = 0;

    @Column
    private double shipping = 0;

    @Column(nullable = false)
    private double amount = 0; // Total amount

    @Column(nullable = false)
    private double paidAmount = 0;

    @Column(nullable = false)
    private double balanceAmount = 0;

    @Column
    private String currency = "LKR";

    @Column(nullable = false)
    private CreditStatus status = CreditStatus.PENDING;

    @Column
    private String paymentTerms;

    @Column
    private LocalDate firstPaymentDate;

    @Column
    private double firstPaymentAmount = 0;

    @Column
    private String notes;

    @Column
    private String termsAndConditions;

    @Column
    private String approvedBy;

    @Column
    private LocalDateTime approvedAt;

    @OneToMany(mappedBy = "creditSaleId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<CreditSaleItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "creditSaleId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<CreditSalePayment> payments = new ArrayList<>();

    @Column
    private String syncId; // For tracking sync status

    @Column
    private String deviceId; // Device that owns this credit sale

    @Column
    private LocalDateTime syncedAt;

    @Column
    private boolean syncStatus = false;

    public CreditSale(String id, String creditSaleNumber, String customerId, String customerName) {
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

    public void addItem(CreditSaleItem item) {
        if (item != null) {
            item.setCreditSaleId(this.id);
            this.items.add(item);
            this.recalculateTotals();
        }
    }

    public void removeItem(CreditSaleItem item) {
        if (item != null && this.items.remove(item)) {
            this.recalculateTotals();
        }
    }

    public void addPayment(CreditSalePayment payment) {
        if (payment != null) {
            payment.setCreditSaleId(this.id);
            this.payments.add(payment);
            this.paidAmount += payment.getAmount();
            this.balanceAmount = this.amount - this.paidAmount;
            this.updateStatus();
        }
    }

    private void recalculateTotals() {
        this.subtotal = this.items.stream()
                .mapToDouble(CreditSaleItem::getTotal)
                .sum();
        
        this.taxAmount = this.subtotal * (this.taxRate / 100);
        this.amount = this.subtotal + this.taxAmount + this.shipping - this.discount;
        this.balanceAmount = this.amount - this.paidAmount;
    }

    private void updateStatus() {
        if (this.balanceAmount <= 0) {
            this.status = CreditStatus.PAID;
        } else if (this.paidAmount > 0) {
            this.status = CreditStatus.PARTIALLY_PAID;
        } else if (this.dueDate != null && this.dueDate.isBefore(LocalDate.now())) {
            this.status = CreditStatus.OVERDUE;
        } else {
            this.status = CreditStatus.ACTIVE;
        }
    }

    public boolean isOverdue() {
        return this.dueDate != null && this.dueDate.isBefore(LocalDate.now()) && this.balanceAmount > 0;
    }

    public boolean isFullyPaid() {
        return this.balanceAmount <= 0;
    }

    public boolean isPartiallyPaid() {
        return this.paidAmount > 0 && this.balanceAmount > 0;
    }

    public double getTotalProfit() {
        return this.items.stream()
                .mapToDouble(item -> item.getProfit() * item.getQuantity())
                .sum();
    }

    public int getDaysOverdue() {
        if (this.dueDate == null || !this.isOverdue()) return 0;
        return (int) java.time.temporal.ChronoUnit.DAYS.between(this.dueDate, LocalDate.now());
    }
}