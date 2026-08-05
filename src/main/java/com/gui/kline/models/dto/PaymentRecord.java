package com.gui.kline.models.dto;

import java.time.LocalDate;

/**
 * Represents a single payment record from either credit_payments or tyre_export_payments.
 */
public class PaymentRecord {
    private final String id;
    private final String referenceId;
    private final LocalDate date;
    private final double amount;
    private final String method;
    private final String notes;

    public PaymentRecord(String id, String referenceId, LocalDate date, double amount, String method, String notes) {
        this.id = id;
        this.referenceId = referenceId;
        this.date = date;
        this.amount = amount;
        this.method = method != null ? method : "Cash";
        this.notes = notes != null ? notes : "";
    }

    public String getId() { return id; }
    public String getReferenceId() { return referenceId; }
    public LocalDate getDate() { return date; }
    public double getAmount() { return amount; }
    public String getMethod() { return method; }
    public String getNotes() { return notes; }
}