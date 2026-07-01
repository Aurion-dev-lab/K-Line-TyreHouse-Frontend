package com.gui.kline.models;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServiceRecord {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private String id;
    private LocalDate date;
    private final SimpleStringProperty dateLabel;
    private String service;
    private final SimpleStringProperty serviceProperty;
    private String remark;
    private final SimpleStringProperty remarkProperty;
    private double price;
    private final SimpleDoubleProperty priceProperty;
    private final SimpleStringProperty priceLabel;
    
    // Customer fields for sync
    private String customerId;
    private String customerName;
    
    // Sync fields
    private String syncId;
    private String deviceId;
    private LocalDateTime syncedAt;
    private boolean syncStatus = false;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ServiceRecord(LocalDate date, String service, String remark, double price) {
        this.date = date;
        this.dateLabel = new SimpleStringProperty(date != null ? date.format(DATE_FORMAT) : "");
        this.service = service;
        this.serviceProperty = new SimpleStringProperty(service);
        this.remark = remark != null ? remark : "";
        this.remarkProperty = new SimpleStringProperty(this.remark);
        this.price = price;
        this.priceProperty = new SimpleDoubleProperty(price);
        this.priceLabel = new SimpleStringProperty("Rs. " + String.format("%.2f", price));
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public LocalDate getDate() {
        return date;
    }
    
    public void setDate(LocalDate date) {
        this.date = date;
        if (dateLabel != null) {
            dateLabel.set(date != null ? date.format(DATE_FORMAT) : "");
        }
    }

    public double getPrice() {
        return price;
    }
    
    public void setPrice(double price) {
        this.price = price;
        if (priceProperty != null) {
            priceProperty.set(price);
        }
        if (priceLabel != null) {
            priceLabel.set("Rs. " + String.format("%.2f", price));
        }
    }

    public String getService() {
        return service;
    }
    
    public void setService(String service) {
        this.service = service;
        if (serviceProperty != null) {
            serviceProperty.set(service);
        }
    }

    public String getRemark() {
        return remark;
    }
    
    public void setRemark(String remark) {
        this.remark = remark != null ? remark : "";
        if (remarkProperty != null) {
            remarkProperty.set(this.remark);
        }
    }

    // Customer-related methods for sync compatibility
    public String getServiceName() { return service; }
    public LocalDate getServiceDate() { return date; }
    public String getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }

    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public SimpleStringProperty dateLabelProperty() {
        return dateLabel;
    }

    public SimpleStringProperty serviceProperty() {
        return serviceProperty;
    }

    public SimpleStringProperty remarkProperty() {
        return remarkProperty;
    }

    public SimpleStringProperty priceLabelProperty() {
        return priceLabel;
    }

    public SimpleDoubleProperty priceProperty() {
        return priceProperty;
    }

    // Sync field getters and setters
    public String getSyncId() { return syncId; }
    public void setSyncId(String syncId) { this.syncId = syncId; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public LocalDateTime getSyncedAt() { return syncedAt; }
    public void setSyncedAt(LocalDateTime syncedAt) { this.syncedAt = syncedAt; }

    public boolean isSyncStatus() { return syncStatus; }
    public void setSyncStatus(boolean syncStatus) { this.syncStatus = syncStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

