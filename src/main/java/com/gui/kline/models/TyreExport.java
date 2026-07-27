package com.gui.kline.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Simplified TyreExport model class with sync support.
 */
public class TyreExport {
    private String id;
    private String exportId;
    private String serialNumber;
    private String company;
    private int tyres;
    private double custPrice;
    private double compPrice;
    private double serviceFee;
    private double subTotal;
    private double grandTotal;
    private double initialPayment;
    private double settlement;
    private String status;
    private LocalDate exportDate;
    private String remark;
    
    // Sync fields
    private boolean syncStatus = false;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getExportId() { return exportId; }
    public void setExportId(String exportId) { this.exportId = exportId; }

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

    public double getSubTotal() { return subTotal; }
    public void setSubTotal(double subTotal) { this.subTotal = subTotal; }

    public double getGrandTotal() { return grandTotal; }
    public void setGrandTotal(double grandTotal) { this.grandTotal = grandTotal; }

    public double getInitialPayment() { return initialPayment; }
    public void setInitialPayment(double initialPayment) { this.initialPayment = initialPayment; }

    public double getSettlement() { return settlement; }
    public void setSettlement(double settlement) { this.settlement = settlement; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getExportDate() { return exportDate; }
    public void setExportDate(LocalDate exportDate) { this.exportDate = exportDate; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public boolean isSyncStatus() { return syncStatus; }
    public void setSyncStatus(boolean syncStatus) { this.syncStatus = syncStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Dynamic calculations for backward compatibility / display
    public double getPaidAmount() { return settlement; }
    public double getBalanceAmount() { return Math.max(0.0, grandTotal - settlement); }
    public String getPaymentStatus() {
        double bal = getBalanceAmount();
        if (bal <= 0) return "PAID";
        if (settlement > 0) return "PARTIAL";
        return "CREDIT";
    }
}