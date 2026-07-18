package com.gui.kline.models;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Expense {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private String id;
    private LocalDate date;
    private final SimpleStringProperty dateLabel;
    private String description;
    private final SimpleStringProperty descriptionProperty;
    private String category;
    private final SimpleStringProperty categoryProperty;
    private double amount;
    private final SimpleDoubleProperty amountProperty;
    private final SimpleStringProperty amountLabel;

    public Expense(LocalDate date, String description, String category, double amount) {
        this.date = date;
        this.dateLabel = new SimpleStringProperty(date != null ? date.format(DATE_FORMAT) : "");
        this.description = description;
        this.descriptionProperty = new SimpleStringProperty(description);
        this.category = category;
        this.categoryProperty = new SimpleStringProperty(category);
        this.amount = amount;
        this.amountProperty = new SimpleDoubleProperty(amount);
        this.amountLabel = new SimpleStringProperty("Rs. " + String.format("%.2f", amount));
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) {
        this.date = date;
        if (dateLabel != null) {
            dateLabel.set(date != null ? date.format(DATE_FORMAT) : "");
        }
    }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = description;
        if (descriptionProperty != null) {
            descriptionProperty.set(description);
        }
    }

    public String getCategory() { return category; }
    public void setCategory(String category) {
        this.category = category;
        if (categoryProperty != null) {
            categoryProperty.set(category);
        }
    }

    public double getAmount() { return amount; }
    public void setAmount(double amount) {
        this.amount = amount;
        if (amountProperty != null) {
            amountProperty.set(amount);
        }
        if (amountLabel != null) {
            amountLabel.set("Rs. " + String.format("%.2f", amount));
        }
    }

    public SimpleStringProperty dateLabelProperty() { return dateLabel; }
    public SimpleStringProperty descriptionProperty() { return descriptionProperty; }
    public SimpleStringProperty categoryProperty() { return categoryProperty; }
    public SimpleDoubleProperty amountProperty() { return amountProperty; }
    public SimpleStringProperty amountLabelProperty() { return amountLabel; }
}