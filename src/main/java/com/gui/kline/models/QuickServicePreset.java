package com.gui.kline.models;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

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

    public SimpleStringProperty serviceProperty() {
        return service;
    }

    public SimpleStringProperty priceLabelProperty() {
        return priceLabel;
    }

    public SimpleStringProperty statusProperty() {
        return status;
    }

    public SimpleStringProperty iconProperty() {
        return icon;
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