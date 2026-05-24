package com.gui.kline.models;

import java.time.LocalDate;

public class LedgerEntry {
    private final String id;
    private final LocalDate date;
    private final String worker, type, note;
    private final double amount;

    public LedgerEntry(String id, LocalDate date, String worker, String type, String note, double amount) {
        this.id = id;
        this.date = date; this.worker = worker;
        this.type = type; this.note = note; this.amount = amount;
    }

    public String getId() { return id; }
    public LocalDate getDate()   { return date; }
    public String getWorker()    { return worker; }
    public String getType()      { return type; }
    public String getNote()      { return note; }
    public double getAmount()    { return amount; }
}
