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
 * Service record representing services performed.
 */
@Entity
@Table(name = "services", indexes = {
    @Index(name = "idx_service_date", columnList = "serviceDate"),
    @Index(name = "idx_service_device", columnList = "deviceId")
})
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ServiceRecord extends BaseEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String serviceName;

    @Column
    private String description;

    @Column
    private String category;

    @Column
    private LocalDate serviceDate;

    @Column
    private LocalDateTime startTime;

    @Column
    private LocalDateTime endTime;

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
    private String vehicleMake;

    @Column
    private double price;

    @Column
    private double cost = 0;

    @Column
    private double discount = 0;

    @Column
    private String discountType = "PERCENTAGE";

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
    private String partsUsed; // JSON array of part IDs

    @Column
    private String beforePhotos; // JSON array of photo URLs

    @Column
    private String afterPhotos; // JSON array of photo URLs

    @Column
    private String warrantyPeriod;

    @Column
    private LocalDate warrantyExpires;

    @Column
    private String syncId; // For tracking sync status

    @Column
    private String deviceId; // Device that owns this service

    @Column
    private LocalDateTime syncedAt;

    @Column
    private boolean syncStatus = false;

    public ServiceRecord(String id, String serviceName, double price, LocalDate serviceDate) {
        this.id = id;
        this.serviceName = serviceName;
        this.price = price;
        this.totalAmount = price;
        this.serviceDate = serviceDate;
        this.startTime = LocalDateTime.now();
        this.status = "COMPLETED";
    }

    public enum Status {
        SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED, PAID, UNPAID
    }

    public double getProfit() {
        return this.totalAmount - this.cost;
    }

    public double getDurationMinutes() {
        if (startTime == null || endTime == null) return 0;
        return java.time.Duration.between(startTime, endTime).toMinutes();
    }
}