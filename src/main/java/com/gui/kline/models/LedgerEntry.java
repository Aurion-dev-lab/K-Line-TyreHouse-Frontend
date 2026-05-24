package com.gui.kline.models;

import java.time.LocalDate;

public class LedgerEntry {
    private final LocalDate date;
    private final String worker, type, note;
    private final double amount;

    public LedgerEntry(LocalDate date, String worker, String type, String note, double amount) {
        this.date = date; this.worker = worker;
        this.type = type; this.note = note; this.amount = amount;
    }

    public LocalDate getDate()   { return date; }
    public String getWorker()    { return worker; }
    public String getType()      { return type; }
    public String getNote()      { return note; }
    public double getAmount()    { return amount; }
}
