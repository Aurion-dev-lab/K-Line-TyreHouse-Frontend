package com.gui.kline.models.ui;

import java.time.LocalDate;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ExportRecord {
    private final SimpleStringProperty exportId;
    private final SimpleStringProperty serialNumber;
    private final SimpleStringProperty company;
    private final SimpleStringProperty tyreSize;
    private final SimpleStringProperty tyreMake;
    private final SimpleIntegerProperty tyres;
    private final SimpleDoubleProperty custPrice;
    private final SimpleDoubleProperty compPrice;
    private final SimpleDoubleProperty serviceCharge;
    private final SimpleDoubleProperty subTotal;
    private final SimpleDoubleProperty grandTotal;
    private final SimpleDoubleProperty initialPayment;
    private final SimpleDoubleProperty settlement;
    private final ObjectProperty<LocalDate> date;
    private final SimpleStringProperty status;
    private final SimpleStringProperty remark;

    public ExportRecord(String company, int tyres, double custPrice,
                        double compPrice, double serviceCharge,
                        LocalDate date, String status) {
        this("", "", company, "", "", tyres, custPrice, compPrice, serviceCharge,
                custPrice * tyres, (custPrice * tyres) + serviceCharge, 0.0, 0.0, date, status, "");
    }

    public ExportRecord(String exportId, String serialNumber, String company, int tyres, double custPrice,
                        double compPrice, double serviceCharge, double subTotal, double grandTotal,
                        double initialPayment, double settlement, LocalDate date, String status, String remark) {
        this(exportId, serialNumber, company, "", "", tyres, custPrice, compPrice, serviceCharge,
                subTotal, grandTotal, initialPayment, settlement, date, status, remark);
    }

    public ExportRecord(String exportId, String serialNumber, String company, String tyreSize, String tyreMake,
                        int tyres, double custPrice, double compPrice, double serviceCharge, double subTotal,
                        double grandTotal, double initialPayment, double settlement, LocalDate date,
                        String status, String remark) {
        this.exportId       = new SimpleStringProperty(exportId == null ? "" : exportId);
        this.serialNumber   = new SimpleStringProperty(serialNumber == null ? "" : serialNumber);
        this.company        = new SimpleStringProperty(company == null ? "" : company);
        this.tyreSize       = new SimpleStringProperty(tyreSize == null ? "" : tyreSize);
        this.tyreMake       = new SimpleStringProperty(tyreMake == null ? "" : tyreMake);
        this.tyres          = new SimpleIntegerProperty(tyres);
        this.custPrice      = new SimpleDoubleProperty(custPrice);
        this.compPrice      = new SimpleDoubleProperty(compPrice);
        this.serviceCharge  = new SimpleDoubleProperty(serviceCharge);
        this.subTotal       = new SimpleDoubleProperty(subTotal);
        this.grandTotal     = new SimpleDoubleProperty(grandTotal);
        this.initialPayment = new SimpleDoubleProperty(initialPayment);
        this.settlement     = new SimpleDoubleProperty(settlement);
        this.date           = new SimpleObjectProperty<>(date);
        this.status         = new SimpleStringProperty(status);
        this.remark         = new SimpleStringProperty(remark == null ? "" : remark);
    }

    public StringProperty exportIdProperty()       { return exportId; }
    public StringProperty serialNumberProperty()   { return serialNumber; }
    public StringProperty companyProperty()       { return company; }
    public StringProperty tyreSizeProperty()      { return tyreSize; }
    public StringProperty tyreMakeProperty()      { return tyreMake; }
    public IntegerProperty tyresProperty()        { return tyres; }
    public DoubleProperty serviceChargeProperty() { return serviceCharge; }
    public DoubleProperty subTotalProperty()      { return subTotal; }
    public DoubleProperty grandTotalProperty()    { return grandTotal; }
    public DoubleProperty initialPaymentProperty(){ return initialPayment; }
    public DoubleProperty settlementProperty()    { return settlement; }
    public StringProperty statusProperty()        { return status; }
    public StringProperty remarkProperty()        { return remark; }
    public ObjectProperty<LocalDate> dateProperty(){ return date; }
    public DoubleProperty custPriceProperty()     { return custPrice; }
    public DoubleProperty compPriceProperty()     { return compPrice; }

    public String getExportId()      { return exportId.get(); }
    public String getSerialNumber()  { return serialNumber.get(); }
    public String getCompany()       { return company.get(); }
    public String getTyreSize()      { return tyreSize.get(); }
    public String getTyreMake()      { return tyreMake.get(); }
    public int    getTyres()         { return tyres.get(); }
    public double getCustPrice()     { return custPrice.get(); }
    public double getCompPrice()     { return compPrice.get(); }
    public double getServiceCharge() { return serviceCharge.get(); }
    public double getSubTotal()      { return subTotal.get(); }
    public double getGrandTotal()    { return grandTotal.get(); }
    public double getInitialPayment() { return initialPayment.get(); }
    public double getSettlement()    { return settlement.get(); }
    public LocalDate getDate()       { return date.get(); }
    public String getStatus()        { return status.get(); }
    public String getRemark()        { return remark.get(); }

    public void setExportId(String v)      { exportId.set(v == null ? "" : v); }
    public void setSerialNumber(String v)  { serialNumber.set(v == null ? "" : v); }
    public void setCompany(String v)       { company.set(v); }
    public void setTyreSize(String v)      { tyreSize.set(v == null ? "" : v); }
    public void setTyreMake(String v)      { tyreMake.set(v == null ? "" : v); }
    public void setTyres(int v)            { tyres.set(v); }
    public void setCustPrice(double v)     { custPrice.set(v); }
    public void setCompPrice(double v)     { compPrice.set(v); }
    public void setServiceCharge(double v) { serviceCharge.set(v); }
    public void setSubTotal(double v)      { subTotal.set(v); }
    public void setGrandTotal(double v)    { grandTotal.set(v); }
    public void setInitialPayment(double v){ initialPayment.set(v); }
    public void setSettlement(double v)    { settlement.set(v); }
    public void setDate(LocalDate v)       { date.set(v); }
    public void setStatus(String s)        { status.set(s); }
    public void setRemark(String v)        { remark.set(v == null ? "" : v); }

    // Dynamic Calculations / Backward Compatibility Methods
    public double getPaidAmount()          { return getSettlement(); }
    public double getBalanceAmount()       { return Math.max(0.0, getGrandTotal() - getSettlement()); }
    public String getPaymentStatus() {
        double bal = getBalanceAmount();
        if (bal <= 0.0) return "PAID";
        if (getSettlement() > 0.0) return "PARTIAL";
        return "CREDIT";
    }
    public double getTotalAmount()         { return getGrandTotal(); }

    public String getPricesDisplay() {
        return String.format("C: Rs. %,.0f|P: Rs. %,.0f", getCustPrice(), getCompPrice());
    }
    public String getProfitDisplay() {
        double profit = getCustPrice() - getCompPrice();
        return String.format("Rs. %,.0f|%s", profit, getDate().toString());
    }
    public double getNetTotal() {
        return getGrandTotal();
    }
}
