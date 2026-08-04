package com.gui.kline.models.reports;

import java.time.LocalDate;

/**
 * Model class representing daily sales summary metrics in reports.
 */
public class DailySummary {
    private final LocalDate date;
    private final int invoiceCount;
    private final int totalItems;
    private final double totalRevenue;

    public DailySummary(LocalDate date, int invoiceCount, int totalItems, double totalRevenue) {
        this.date = date;
        this.invoiceCount = invoiceCount;
        this.totalItems = totalItems;
        this.totalRevenue = totalRevenue;
    }

    public LocalDate getDate() { return date; }
    public int getInvoiceCount() { return invoiceCount; }
    public int getTotalItems() { return totalItems; }
    public double getTotalRevenue() { return totalRevenue; }
}
