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
 * Customer entity representing clients of the business.
 * Contains contact information, credit limits, and sync metadata.
 */
@Entity
@Table(name = "customers", indexes = {
    @Index(name = "idx_customer_phone", columnList = "phone"),
    @Index(name = "idx_customer_name", columnList = "name"),
    @Index(name = "idx_customer_device", columnList = "deviceId")
})
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Customer extends BaseEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column
    private String companyName;

    @Column(unique = true)
    private String phone;

    @Column
    private String alternatePhone;

    @Column
    private String email;

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

    @Column
    private String taxId; // Tax identification number

    @Column
    private CustomerType customerType = CustomerType.RETAIL;

    @Column
    private double creditLimit = 0;

    @Column
    private double currentCredit = 0;

    @Column
    private boolean active = true;

    @Column
    private LocalDate dateOfBirth;

    @Column
    private String notes;

    @Column
    private String category; // Customer category (VIP, Regular, etc.)

    @Column
    private String loyaltyProgramId;

    @Column
    private double loyaltyPoints = 0;

    @Column
    private LocalDate memberSince;

    @Column
    private String syncId; // For tracking sync status

    @Column
    private String deviceId; // Device that owns this customer

    @Column
    private LocalDateTime syncedAt;

    @Column
    private boolean syncStatus = false;

    public Customer(String id, String name, String phone) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.active = true;
        this.memberSince = LocalDate.now();
    }

    public enum CustomerType {
        RETAIL, WHOLESALE, CORPORATE, GOVERNMENT, INTERNATIONAL
    }

    public double getAvailableCredit() {
        return creditLimit - currentCredit;
    }

    public boolean canExtendCredit(double amount) {
        return getAvailableCredit() >= amount;
    }

    public String getFullAddress() {
        StringBuilder addressBuilder = new StringBuilder();
        if (address != null && !address.isBlank()) addressBuilder.append(address).append(", ");
        if (city != null && !city.isBlank()) addressBuilder.append(city).append(", ");
        if (state != null && !state.isBlank()) addressBuilder.append(state).append(", ");
        if (country != null && !country.isBlank()) addressBuilder.append(country);
        if (postalCode != null && !postalCode.isBlank()) {
            if (addressBuilder.length() > 0) addressBuilder.append(" - ");
            addressBuilder.append(postalCode);
        }
        return addressBuilder.toString();
    }
}