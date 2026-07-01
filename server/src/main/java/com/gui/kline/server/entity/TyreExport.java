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
 * TyreExport entity representing tyre export/import operations.
 */
@Entity
@Table(name = "tyre_exports", indexes = {
    @Index(name = "idx_tyre_export_number", columnList = "exportNumber", unique = true),
    @Index(name = "idx_tyre_export_date", columnList = "exportDate"),
    @Index(name = "idx_tyre_export_company", columnList = "company"),
    @Index(name = "idx_tyre_export_status", columnList = "status"),
    @Index(name = "idx_tyre_export_device", columnList = "deviceId")
})
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TyreExport extends BaseEntity {

    @Id
    private String id;

    @Column(unique = true, nullable = false)
    private String exportNumber;

    @Column(nullable = false)
    private LocalDate exportDate;

    @Column(nullable = false)
    private ExportOperation operation = ExportOperation.EXPORT; // EXPORT or IMPORT

    @Column(nullable = false)
    private String company;

    @Column
    private String companyContact;

    @Column
    private String companyPhone;

    @Column
    private String companyAddress;

    @Column(nullable = false)
    private int tyres = 0;

    @Column
    private String tyreSpecs; // Size, brand, model, etc.

    @Column(nullable = false)
    private double custPrice = 0; // Price charged to customer

    @Column(nullable = false)
    private double compPrice = 0; // Price paid to company (cost)

    @Column
    private double serviceFee = 0;

    @Column
    private double transportCost = 0;

    @Column
    private double otherCosts = 0;

    @Column(nullable = false)
    private double totalAmount = 0; // Total amount of the transaction

    @Column(nullable = false)
    private double paidAmount = 0; // Amount already paid

    @Column(nullable = false)
    private double balanceAmount = 0; // Amount remaining to be paid

    @Column
    private String currency = "LKR";

    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(nullable = false)
    private ExportStatus status = ExportStatus.DRAFT;

    @Column
    private String paymentMethod;

    @Column
    private String paymentReference;

    @Column
    private LocalDate paymentDueDate;

    @Column
    private String driverName;

    @Column
    private String vehicleNumber;

    @Column
    private String notes;

    @Column
    private String termsAndConditions;

    @OneToMany(mappedBy = "tyreExportId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<TyreExportPayment> payments = new ArrayList<>();

    @Column
    private String syncId; // For tracking sync status

    @Column
    private String deviceId; // Device that owns this tyre export

    @Column
    private LocalDateTime syncedAt;

    @Column
    private boolean syncStatus = false;

    public TyreExport(String id, String exportNumber, String company, 
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

    public void addPayment(TyreExportPayment payment) {
        if (payment != null) {
            payment.setTyreExportId(this.id);
            this.payments.add(payment);
            this.paidAmount += payment.getAmount();
            this.balanceAmount = this.totalAmount - this.paidAmount;
            this.updateStatus();
        }
    }

    private void updateStatus() {
        if (this.balanceAmount <= 0) {
            this.paymentStatus = PaymentStatus.PAID;
        } else if (this.paidAmount > 0) {
            this.paymentStatus = PaymentStatus.PARTIAL;
        } else if (this.paymentDueDate != null && this.paymentDueDate.isBefore(LocalDate.now())) {
            this.paymentStatus = PaymentStatus.OVERDUE;
        } else {
            this.paymentStatus = PaymentStatus.PENDING;
        }
    }

    public double getProfit() {
        return this.custPrice - this.compPrice;
    }

    public double getTotalProfit() {
        return getProfit() * this.tyres;
    }

    public double getTotalCost() {
        return this.compPrice * this.tyres + this.serviceFee + this.transportCost + this.otherCosts;
    }

    public boolean isProfitable() {
        return getProfit() >= 0;
    }

    public boolean isOverdue() {
        return this.paymentDueDate != null && this.paymentDueDate.isBefore(LocalDate.now()) && this.balanceAmount > 0;
    }

    public int getDaysOverdue() {
        if (this.paymentDueDate == null || !this.isOverdue()) return 0;
        return (int) java.time.temporal.ChronoUnit.DAYS.between(this.paymentDueDate, LocalDate.now());
    }
}