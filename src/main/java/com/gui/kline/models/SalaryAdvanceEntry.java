package com.gui.kline.models;

import java.time.LocalDate;

public class SalaryAdvanceEntry {
    private final String id;
    private final String worker;
    private final LocalDate date;
    private final double amount;
    private final String note;

    public SalaryAdvanceEntry(String id, String worker, LocalDate date, double amount, String note) {
        this.id = id;
        this.worker = worker;
        this.date = date;
        this.amount = amount;
        this.note = note;
    }

    public String getId() {
        return id;
    }

    public String getWorker() {
        return worker;
    }

    public LocalDate getDate() {
        return date;
    }

    public double getAmount() {
        return amount;
    }

    public String getNote() {
        return note;
    }
}

