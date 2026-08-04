package com.gui.kline.models.reports;

/**
 * Model class representing customer purchase summary metrics in reports.
 */
public class CustomerSummary {
    private final String customer;
    private final int purchaseCount;
    private final double totalAmount;
    private final double totalPaid;
    private final double outstanding;

    public CustomerSummary(String customer, int purchaseCount, double totalAmount, 
                          double totalPaid, double outstanding) {
        this.customer = customer;
        this.purchaseCount = purchaseCount;
        this.totalAmount = totalAmount;
        this.totalPaid = totalPaid;
        this.outstanding = outstanding;
    }

    public String getCustomer() { return customer; }
    public int getPurchaseCount() { return purchaseCount; }
    public double getTotalAmount() { return totalAmount; }
    public double getTotalPaid() { return totalPaid; }
    public double getOutstanding() { return outstanding; }
}
