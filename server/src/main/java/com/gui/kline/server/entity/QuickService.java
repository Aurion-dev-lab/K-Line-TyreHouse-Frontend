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
 * Quick service representing small, fast services performed.
 */
@Entity
@Table(name = "quick_services", indexes = {
    @Index(name = "idx_quick_service_date", columnList = "serviceDate"),
    @Index(name = "idx_quick_service_type", columnList = "serviceType"),
    @Index(name = "idx_quick_service_device", columnList = "deviceId")
})
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QuickService extends BaseEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String serviceName;

    @Column
    private String description;

    @Column
    private String serviceType;

    @Column
    private LocalDate serviceDate;

    @Column
    private LocalDateTime serviceTime;

    @Column
    private String assignedTo; // Worker ID

    @Column
    private String assignedToName;

    @Column
    private String customerId;

    @Column
    private String customerName;

    @Column
    private String vehicleNumber;

    @Column
    private String vehicleModel;

    @Column
    private double price;

    @Column
    private double cost = 0;

    @Column
    private double discount = 0;

    @Column
    private double taxRate = 0;

    @Column
    private double taxAmount = 0;

    @Column
    private double totalAmount = 0;

    @Column
    private String currency = "LKR";

    @Column
    private String paymentMethod;

    @Column
    private boolean paid = false;

    @Column
    private String paymentReference;

    @Column
    private String status = "COMPLETED";

    @Column
    private String notes;

    @Column
    private String presetId; // Reference to quick service preset

    @Column
    private boolean fromPreset = false;

    @Column
    private String syncId; // For tracking sync status

    @Column
    private String deviceId; // Device that owns this service

    @Column
    private LocalDateTime syncedAt;

    @Column
    private boolean syncStatus = false;

    public QuickService(String id, String serviceName, double price, LocalDate serviceDate) {
        this.id = id;
        this.serviceName = serviceName;
        this.price = price;
        this.totalAmount = price;
        this.serviceDate = serviceDate;
        this.serviceTime = LocalDateTime.now();
        this.status = "COMPLETED";
    }

    public enum Status {
        SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED, PAID, UNPAID
    }

    public enum PaymentMethod {
        CASH, CHEQUE, BANK_TRANSFER, CREDIT_CARD, DEBIT_CARD, MOBILE_PAYMENT, ONLINE_PAYMENT, OTHER
    }

    public double getProfit() {
        return this.totalAmount - this.cost;
    }

    public void markAsPaid() {
        this.paid = true;
        this.status = "PAID";
    }

    public void markAsUnpaid() {
        this.paid = false;
        this.status = "UNPAID";
    }
}