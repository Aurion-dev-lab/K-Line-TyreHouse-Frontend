package com.gui.kline.models.dto;

public class InvoiceRow {
    private final String invoiceId, date, customer, type, status, phone, description, vehicleNumber;
    private final int    itemCount;
    private final double total;

    public InvoiceRow(String invoiceId, String date, String customer,
                      String type, int itemCount, double total) {
        this(invoiceId, date, customer, type, itemCount, total, "completed", "", "", "");
    }

    public InvoiceRow(String invoiceId, String date, String customer,
                      String type, int itemCount, double total, String status) {
        this(invoiceId, date, customer, type, itemCount, total, status, "", "", "");
    }

    public InvoiceRow(String invoiceId, String date, String customer,
                      String type, int itemCount, double total, String status, String phone) {
        this(invoiceId, date, customer, type, itemCount, total, status, phone, "", "");
    }

    public InvoiceRow(String invoiceId, String date, String customer,
                      String type, int itemCount, double total, String status, String phone, String description) {
        this(invoiceId, date, customer, type, itemCount, total, status, phone, description, "");
    }

    public InvoiceRow(String invoiceId, String date, String customer,
                      String type, int itemCount, double total, String status, String phone, String description, String vehicleNumber) {
        this.invoiceId = invoiceId; this.date = date;
        this.customer  = customer;  this.type = type;
        this.itemCount = itemCount; this.total = total;
        this.status = status == null || status.isBlank() ? "completed" : status;
        this.phone = phone != null ? phone : "";
        this.description = description != null ? description : "";
        this.vehicleNumber = vehicleNumber != null ? vehicleNumber : "";
    }

    public String getInvoiceId() { return invoiceId; }
    public String getDate()      { return date; }
    public String getCustomer()  { return customer; }
    public String getType()      { return type; }
    public int    getItemCount() { return itemCount; }
    public double getTotal()     { return total; }
    public String getStatus()    { return status; }
    public String getPhone()     { return phone; }
    public String getDescription() { return description; }
    public String getVehicleNumber() { return vehicleNumber; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InvoiceRow that = (InvoiceRow) o;
        return invoiceId != null ? invoiceId.equalsIgnoreCase(that.invoiceId) : that.invoiceId == null;
    }

    @Override
    public int hashCode() {
        return invoiceId != null ? invoiceId.toLowerCase().hashCode() : 0;
    }
}
