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
 * Invoice entity representing sales transactions.
 * Contains invoice details, customer information, and line items.
 */
@Entity
@Table(name = "invoices", indexes = {
    @Index(name = "idx_invoice_number", columnList = "invoiceNumber", unique = true),
    @Index(name = "idx_invoice_date", columnList = "invoiceDate"),
    @Index(name = "idx_invoice_customer", columnList = "customerId"),
    @Index(name = "idx_invoice_status", columnList = "status"),
    @Index(name = "idx_invoice_device", columnList = "deviceId")
})
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Invoice extends BaseEntity {

    @Id
    private String id;

    @Column(unique = true, nullable = false)
    private String invoiceNumber;

    @Column
    private LocalDate invoiceDate;

    @Column
    private LocalDate dueDate;

    @Column
    private String customerId;

    @Column
    private String customerName;

    @Column
    private String customerPhone;

    @Column
    private InvoiceType type = InvoiceType.CASH;

    @Column
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column
    private String paymentMethod;

    @Column
    private String paymentReference;

    @Column
    private double subtotal = 0;

    @Column
    private double taxRate = 0;

    @Column
    private double taxAmount = 0;

    @Column
    private double discount = 0;

    @Column
    private double discountType; // 0 = percentage, 1 = fixed

    @Column
    private double shipping = 0;

    @Column
    private double grandTotal = 0;

    @Column
    private double amountPaid = 0;

    @Column
    private double balanceDue = 0;

    @Column
    private String currency = "LKR";

    @Column
    private String notes;

    @Column
    private String termsAndConditions;

    @Column
    private String salespersonId;

    @Column
    private String salespersonName;

    @Column
    private String location;

    @Column
    private boolean isCredit = false;

    @Column
    private String creditSaleId; // Link to credit sale if applicable

    @OneToMany(mappedBy = "invoiceId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<InvoiceLineItem> lineItems = new ArrayList<>();

    @Column
    private String syncId; // For tracking sync status

    @Column
    private String deviceId; // Device that owns this invoice

    @Column
    private LocalDateTime syncedAt;

    @Column
    private boolean syncStatus = false;

    public Invoice(String id, String invoiceNumber, String customerId, String customerName) {
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

    public void addLineItem(InvoiceLineItem item) {
        if (item != null) {
            item.setInvoiceId(this.id);
            this.lineItems.add(item);
            this.recalculateTotals();
        }
    }

    public void removeLineItem(InvoiceLineItem item) {
        if (item != null && this.lineItems.remove(item)) {
            this.recalculateTotals();
        }
    }

    private void recalculateTotals() {
        this.subtotal = this.lineItems.stream()
                .mapToDouble(InvoiceLineItem::getTotal)
                .sum();
        
        this.taxAmount = this.subtotal * (this.taxRate / 100);
        this.grandTotal = this.subtotal + this.taxAmount + this.shipping - this.discount;
        this.balanceDue = this.grandTotal - this.amountPaid;
    }

    public boolean isFullyPaid() {
        return this.balanceDue <= 0;
    }

    public boolean isPartiallyPaid() {
        return this.amountPaid > 0 && this.balanceDue > 0;
    }

    public boolean isUnpaid() {
        return this.amountPaid == 0 && this.balanceDue > 0;
    }

    public double getTotalProfit() {
        return this.lineItems.stream()
                .mapToDouble(item -> item.getProfit() * item.getQuantity())
                .sum();
    }
}