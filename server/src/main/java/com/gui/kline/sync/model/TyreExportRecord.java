package com.gui.kline.sync.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "tyre_exports")
public class TyreExportRecord {
    @Id
    private String syncId;
    private String deviceId;
    private String company;
    private int tyres;
    private double custPrice;
    private double compPrice;
    private double serviceFee;
    private LocalDate date;
    private String status;

    public String getSyncId() {
        return syncId;
    }

    public void setSyncId(String syncId) {
        this.syncId = syncId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public int getTyres() {
        return tyres;
    }

    public void setTyres(int tyres) {
        this.tyres = tyres;
    }

    public double getCustPrice() {
        return custPrice;
    }

    public void setCustPrice(double custPrice) {
        this.custPrice = custPrice;
    }

    public double getCompPrice() {
        return compPrice;
    }

    public void setCompPrice(double compPrice) {
        this.compPrice = compPrice;
    }

    public double getServiceFee() {
        return serviceFee;
    }

    public void setServiceFee(double serviceFee) {
        this.serviceFee = serviceFee;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

