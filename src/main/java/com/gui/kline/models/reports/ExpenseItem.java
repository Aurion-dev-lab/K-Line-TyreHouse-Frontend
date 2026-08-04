package com.gui.kline.models.reports;

import java.time.LocalDate;

/**
 * Model class representing an individual expense item in reports.
 */
public class ExpenseItem {
    private final LocalDate date;
    private final String description;
    private final double amount;
    private final String category;

    public ExpenseItem(LocalDate date, String description, double amount, String category) {
        this.date = date;
        this.description = description;
        this.amount = amount;
        this.category = category;
    }

    public LocalDate getDate() { return date; }
    public String getDescription() { return description; }
    public double getAmount() { return amount; }
    public String getCategory() { return category; }
}
