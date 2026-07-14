package com.gui.kline.models;

public class InvoiceRow {
    private final String invoiceId, date, customer, type, status;
    private final int    itemCount;
    private final double total;

    public InvoiceRow(String invoiceId, String date, String customer,
                      String type, int itemCount, double total) {
        this(invoiceId, date, customer, type, itemCount, total, "completed");
    }

    public InvoiceRow(String invoiceId, String date, String customer,
                      String type, int itemCount, double total, String status) {
        this.invoiceId = invoiceId; this.date = date;
        this.customer  = customer;  this.type = type;
        this.itemCount = itemCount; this.total = total;
        this.status = status == null || status.isBlank() ? "completed" : status;
    }

    public String getInvoiceId() { return invoiceId; }
    public String getDate()      { return date; }
    public String getCustomer()  { return customer; }
    public String getType()      { return type; }
    public int    getItemCount() { return itemCount; }
    public double getTotal()     { return total; }
    public String getStatus()    { return status; }
}
