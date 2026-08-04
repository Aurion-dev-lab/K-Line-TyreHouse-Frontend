package com.gui.kline.models.reports;

import java.time.LocalDateTime;

/**
 * Model class to represent individual salary payments.
 */
public class SalaryPayment {
    private final String id;
    private final String worker;
    private final double amount;
    private final LocalDateTime paidAt;

    public SalaryPayment(String id, String worker, double amount, LocalDateTime paidAt) {
        this.id = id;
        this.worker = worker;
        this.amount = amount;
        this.paidAt = paidAt;
    }

    public String getId() { return id; }
    public String getWorker() { return worker; }
    public double getAmount() { return amount; }
    public LocalDateTime getPaidAt() { return paidAt; }
}
