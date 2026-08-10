package com.gui.kline.models.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class QuickServicePreset {
    private final String id;
    private final SimpleStringProperty service;
    private final SimpleStringProperty priceLabel;
    private final SimpleBooleanProperty active;
    private final SimpleStringProperty status;
    private final SimpleStringProperty icon;
    private final double price;

    public QuickServicePreset(String id, String service, double price, boolean active, String icon) {
        this.id = id;
        this.service = new SimpleStringProperty(service);
        this.price = price;
        this.priceLabel = new SimpleStringProperty("Rs. " + String.format("%.0f", price));
        this.active = new SimpleBooleanProperty(active);
        this.status = new SimpleStringProperty(active ? "Active" : "Disabled");
        this.icon = new SimpleStringProperty(icon != null ? icon : "fas-bolt");
        this.active.addListener((obs, oldVal, newVal) ->
                this.status.set(newVal ? "Active" : "Disabled"));
    }

    public String getId() {
        return id;
    }

    public boolean isActive() {
        return active.get();
    }

    public StringProperty serviceProperty() {
        return service;
    }

    public StringProperty priceLabelProperty() {
        return priceLabel;
    }

    public StringProperty statusProperty() {
        return status;
    }

    public StringProperty iconProperty() {
        return icon;
    }

    public BooleanProperty activeProperty() {
        return active;
    }

    public String getService() {
        return service.get();
    }

    public double getPrice() {
        return price;
    }

    public String getIcon() {
        return icon.get();
    }
}
