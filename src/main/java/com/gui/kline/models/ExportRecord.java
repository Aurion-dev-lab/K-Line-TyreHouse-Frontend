package com.gui.kline.models;

import javafx.beans.property.*;

import java.time.LocalDate;

public class ExportRecord {
    private final SimpleStringProperty company;
    private final SimpleIntegerProperty tyres;
    private final SimpleDoubleProperty custPrice;
    private final SimpleDoubleProperty  compPrice;
    private final SimpleDoubleProperty  serviceCharge;
    private final ObjectProperty<LocalDate> date;
    private final SimpleStringProperty  status;

    public ExportRecord(String company, int tyres, double custPrice,
                        double compPrice, double serviceCharge,
                        LocalDate date, String status) {
        this.company       = new SimpleStringProperty(company);
        this.tyres         = new SimpleIntegerProperty(tyres);
        this.custPrice     = new SimpleDoubleProperty(custPrice);
        this.compPrice     = new SimpleDoubleProperty(compPrice);
        this.serviceCharge = new SimpleDoubleProperty(serviceCharge);
        this.date          = new SimpleObjectProperty<>(date);
        this.status        = new SimpleStringProperty(status);
    }

    public SimpleStringProperty companyProperty()       { return company; }
    public SimpleIntegerProperty tyresProperty()        { return tyres; }
    public SimpleDoubleProperty serviceChargeProperty() { return serviceCharge; }
    public SimpleStringProperty statusProperty()        { return status; }

    public String  getCompany()       { return company.get(); }
    public int     getTyres()         { return tyres.get(); }
    public double  getCustPrice()     { return custPrice.get(); }
    public double  getCompPrice()     { return compPrice.get(); }
    public double  getServiceCharge() { return serviceCharge.get(); }
    public LocalDate getDate()        { return date.get(); }
    public String  getStatus()        { return status.get(); }
    public void    setStatus(String s){ status.set(s); }

    public String getPricesDisplay() {
        return String.format("C: Rs. %,.0f|P: Rs. %,.0f", getCustPrice(), getCompPrice());
    }
    public String getProfitDisplay() {
        double profit = getCustPrice() - getCompPrice();
        return String.format("Rs. %,.0f|%s", profit, getDate().toString());
    }
}
