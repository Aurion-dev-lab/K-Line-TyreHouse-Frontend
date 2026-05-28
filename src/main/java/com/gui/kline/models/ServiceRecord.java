package com.gui.kline.models;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ServiceRecord {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final LocalDate date;
    private final SimpleStringProperty dateLabel;
    private final SimpleStringProperty service;
    private final SimpleStringProperty remark;
    private final SimpleDoubleProperty price;
    private final SimpleStringProperty priceLabel;

    public ServiceRecord(LocalDate date, String service, String remark, double price) {
        this.date = date;
        this.dateLabel = new SimpleStringProperty(date != null ? date.format(DATE_FORMAT) : "");
        this.service = new SimpleStringProperty(service);
        this.remark = new SimpleStringProperty(remark == null ? "" : remark);
        this.price = new SimpleDoubleProperty(price);
        this.priceLabel = new SimpleStringProperty("Rs. " + String.format("%.2f", price));
    }

    public LocalDate getDate() {
        return date;
    }

    public double getPrice() {
        return price.get();
    }

    public String getService() {
        return service.get();
    }

    public String getRemark() {
        return remark.get();
    }

    public SimpleStringProperty dateLabelProperty() {
        return dateLabel;
    }

    public SimpleStringProperty serviceProperty() {
        return service;
    }

    public SimpleStringProperty remarkProperty() {
        return remark;
    }

    public SimpleStringProperty priceLabelProperty() {
        return priceLabel;
    }
}

