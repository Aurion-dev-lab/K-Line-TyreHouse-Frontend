package com.gui.kline.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * TyreExport model class with sync support.
 */
public class TyreExport {
    private String id;
    private String exportId;
    private String operation;
    private String serialNumber;
    private String company;
    private int tyres;
    private double custPrice;
    private double compPrice;
    private double serviceFee;
    private double paidAmount;
    private double totalAmount;
    private double balanceAmount;
    private String paymentStatus;
    private String status;
    private LocalDate exportDate;
    private String notes;
    private String createdBy;
    private String updatedBy;
    
    // Sync fields
    private String syncId;
    private String deviceId;
    private LocalDateTime syncedAt;
    private boolean syncStatus = false;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getExportId() { return exportId; }
    public void setExportId(String exportId) { this.exportId = exportId; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public int getTyres() { return tyres; }
    public void setTyres(int tyres) { this.tyres = tyres; }

    public double getCustPrice() { return custPrice; }
    public void setCustPrice(double custPrice) { this.custPrice = custPrice; }

    public double getCompPrice() { return compPrice; }
    public void setCompPrice(double compPrice) { this.compPrice = compPrice; }

    public double getServiceFee() { return serviceFee; }
    public void setServiceFee(double serviceFee) { this.serviceFee = serviceFee; }

    public double getPaidAmount() { return paidAmount; }
    public void setPaidAmount(double paidAmount) { this.paidAmount = paidAmount; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public double getBalanceAmount() { return balanceAmount; }
    public void setBalanceAmount(double balanceAmount) { this.balanceAmount = balanceAmount; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getExportDate() { return exportDate; }
    public void setExportDate(LocalDate exportDate) { this.exportDate = exportDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

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