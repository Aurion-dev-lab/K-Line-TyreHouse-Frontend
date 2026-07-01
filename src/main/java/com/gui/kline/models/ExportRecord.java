package com.gui.kline.models;

import java.time.LocalDate;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

public class ExportRecord {
    private final SimpleStringProperty exportId;
    private final SimpleStringProperty company;
    private final SimpleIntegerProperty tyres;
    private final SimpleDoubleProperty custPrice;
    private final SimpleDoubleProperty  compPrice;
    private final SimpleDoubleProperty  serviceCharge;
    private final SimpleDoubleProperty  paidAmount;
    private final SimpleDoubleProperty  totalAmount;
    private final SimpleDoubleProperty  balanceAmount;
    private final SimpleStringProperty  paymentStatus;
    private final ObjectProperty<LocalDate> date;
    private final SimpleStringProperty  status;

    public ExportRecord(String company, int tyres, double custPrice,
                        double compPrice, double serviceCharge,
                        LocalDate date, String status) {
        this("", company, tyres, custPrice, compPrice, serviceCharge,
                custPrice * tyres + serviceCharge, 0.0,
                custPrice * tyres + serviceCharge, "PAID", date, status);
    }

    public ExportRecord(String exportId, String company, int tyres, double custPrice,
                        double compPrice, double serviceCharge, double totalAmount,
                        double paidAmount, double balanceAmount, String paymentStatus,
                        LocalDate date, String status) {
        this.exportId      = new SimpleStringProperty(exportId == null ? "" : exportId);
        this.company       = new SimpleStringProperty(company);
        this.tyres         = new SimpleIntegerProperty(tyres);
        this.custPrice     = new SimpleDoubleProperty(custPrice);
        this.compPrice     = new SimpleDoubleProperty(compPrice);
        this.serviceCharge = new SimpleDoubleProperty(serviceCharge);
        this.paidAmount    = new SimpleDoubleProperty(paidAmount);
        this.totalAmount   = new SimpleDoubleProperty(totalAmount);
        this.balanceAmount = new SimpleDoubleProperty(balanceAmount);
        this.paymentStatus = new SimpleStringProperty(paymentStatus == null ? "PAID" : paymentStatus);
        this.date          = new SimpleObjectProperty<>(date);
        this.status        = new SimpleStringProperty(status);
    }

    public SimpleStringProperty exportIdProperty()       { return exportId; }
    public SimpleStringProperty companyProperty()       { return company; }
    public SimpleIntegerProperty tyresProperty()        { return tyres; }
    public SimpleDoubleProperty serviceChargeProperty() { return serviceCharge; }
    public SimpleDoubleProperty paidAmountProperty()     { return paidAmount; }
    public SimpleDoubleProperty totalAmountProperty()    { return totalAmount; }
    public SimpleDoubleProperty balanceAmountProperty()  { return balanceAmount; }
    public SimpleStringProperty paymentStatusProperty()  { return paymentStatus; }
    public SimpleStringProperty statusProperty()        { return status; }

    public String getExportId()      { return exportId.get(); }
    public String  getCompany()       { return company.get(); }
    public int     getTyres()         { return tyres.get(); }
    public double  getCustPrice()     { return custPrice.get(); }
    public double  getCompPrice()     { return compPrice.get(); }
    public double  getServiceCharge() { return serviceCharge.get(); }
    public double  getPaidAmount()    { return paidAmount.get(); }
    public double  getTotalAmount()   { return totalAmount.get(); }
    public double  getBalanceAmount() { return balanceAmount.get(); }
    public String  getPaymentStatus() { return paymentStatus.get(); }
    public LocalDate getDate()        { return date.get(); }
    public String  getStatus()        { return status.get(); }
    public void    setExportId(String v) { exportId.set(v == null ? "" : v); }
    public void    setPaidAmount(double v) { paidAmount.set(v); }
    public void    setTotalAmount(double v) { totalAmount.set(v); }
    public void    setBalanceAmount(double v) { balanceAmount.set(v); }
    public void    setPaymentStatus(String v) { paymentStatus.set(v == null ? "PAID" : v); }
    public void    setStatus(String s){ status.set(s); }

    public String getPricesDisplay() {
        return String.format("C: Rs. %,.0f|P: Rs. %,.0f", getCustPrice(), getCompPrice());
    }
    public String getProfitDisplay() {
        double profit = getCustPrice() - getCompPrice();
        return String.format("Rs. %,.0f|%s", profit, getDate().toString());
    }

    public double getNetTotal() {
        return (getCustPrice() * getTyres()) + getServiceCharge();
    }
}
