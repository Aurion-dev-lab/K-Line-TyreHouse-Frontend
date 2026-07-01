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
 * Worker/Employee entity representing staff members.
 */
@Entity
@Table(name = "workers", indexes = {
    @Index(name = "idx_worker_phone", columnList = "phone", unique = true),
    @Index(name = "idx_worker_nic", columnList = "nic", unique = true),
    @Index(name = "idx_worker_device", columnList = "deviceId")
})
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Worker extends BaseEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String firstName;

    @Column
    private String lastName;

    @Column
    private String fullName;

    @Column(unique = true)
    private String nic; // National ID Card

    @Column(unique = true)
    private String phone;

    @Column
    private String alternatePhone;

    @Column
    private String email;

    @Column
    private LocalDate dateOfBirth;

    @Column
    private String gender;

    @Column
    private String address;

    @Column
    private String city;

    @Column
    private String state;

    @Column
    private String country;

    @Column
    private String postalCode;

    @Column(nullable = false)
    private String role = "STAFF";

    @Column
    private String department;

    @Column
    private String designation;

    @Column
    private String skills;

    @Column
    private SalaryType salaryType = SalaryType.DAILY;

    @Column
    private double dailyRate = 0;

    @Column
    private double monthlySalary = 0;

    @Column
    private double hourlyRate = 0;

    @Column
    private String bankName;

    @Column
    private String bankAccountNumber;

    @Column
    private String bankBranch;

    @Column
    private String emergencyContactName;

    @Column
    private String emergencyContactPhone;

    @Column
    private String profileImageUrl;

    @Column
    private boolean active = true;

    @Column
    private LocalDate joinDate;

    @Column
    private LocalDate terminationDate;

    @Column
    private String terminationReason;

    @Column
    private String notes;

    @Column
    private String syncId; // For tracking sync status

    @Column
    private String deviceId; // Device that owns this worker

    @Column
    private LocalDateTime syncedAt;

    @Column
    private boolean syncStatus = false;

    public Worker(String id, String firstName, String lastName, String phone) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = firstName + (lastName != null ? " " + lastName : "");
        this.phone = phone;
        this.active = true;
        this.joinDate = LocalDate.now();
    }

    public enum SalaryType {
        DAILY, MONTHLY, HOURLY, COMMISSION, CONTRACT
    }

    public enum Role {
        ADMIN, MANAGER, TECHNICIAN, SALES, ACCOUNTS, CLEANER, SUPERVISOR, OWNER
    }

    public String getFullName() {
        return this.firstName + (this.lastName != null && !this.lastName.isBlank() ? " " + this.lastName : "");
    }

    public boolean isTechnician() {
        return this.role != null && 
               (this.role.equalsIgnoreCase("TECHNICIAN") || 
                this.role.equalsIgnoreCase("MECHANIC") ||
                this.role.equalsIgnoreCase("SERVICE_TECHNICIAN"));
    }

    public double calculateDailySalary() {
        switch (this.salaryType) {
            case DAILY: return this.dailyRate;
            case MONTHLY: return this.monthlySalary / 30;
            case HOURLY: return this.hourlyRate * 8; // Assuming 8 hour work day
            default: return 0;
        }
    }
}