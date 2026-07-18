package com.gui.kline.models;

import java.util.ArrayList;
import java.util.List;

public class InvoiceDetail {
    private String invoiceId, customer, date, type;
    private final List<LineItem> lineItems = new ArrayList<>();
    private double taxRate = 0.0;
    private double discountAmount = 0.0;
    private String status = "quotation";

    public void addLineItem(LineItem item)    { lineItems.add(item); }
    public void removeLineItem(LineItem item) { lineItems.remove(item); }
    public List<LineItem> getLineItems()      { return lineItems; }

    public double getSubtotal()  { return lineItems.stream().mapToDouble(LineItem::getTotal).sum(); }
    public double getTax()       { return getSubtotal() * taxRate; }
    public double getGrandTotal(){ return Math.max(0, getSubtotal() + getTax() - discountAmount); }

    public String getInvoiceId()         { return invoiceId; }
    public void   setInvoiceId(String v) { invoiceId = v; }
    public String getCustomer()          { return customer; }
    public void   setCustomer(String v)  { customer = v; }
    public String getDate()              { return date; }
    public void   setDate(String v)      { date = v; }
    public String getType()              { return type; }
    public void   setType(String v)      { type = v; }
    public double getTaxRate()           { return taxRate; }
    public void   setTaxRate(double v)   { taxRate = v; }
    public double getDiscountAmount()    { return discountAmount; }
    public void   setDiscountAmount(double v) { discountAmount = Math.max(0, v); }
    public String getStatus()            { return status; }
    public void   setStatus(String v)    { status = v == null || v.isBlank() ? "quotation" : v; }
}
