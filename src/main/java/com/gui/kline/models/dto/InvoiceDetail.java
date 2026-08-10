package com.gui.kline.models.dto;

import java.util.ArrayList;
import java.util.List;

public class InvoiceDetail {
    private String invoiceId, customer, date, dueDate, type, phone, description, vehicleNumber;
    private String serialNumber, tyreSize, tyreMake, remark;
    private double initialPayment = 0.0;
    private final List<LineItem> lineItems = new ArrayList<>();
    private final List<PaymentRecord> paymentHistory = new ArrayList<>();
    private double taxRate = 0.0;
    private double discountAmount = 0.0;
    private String status = "quotation";

    private String paymentTerms;
    private String termsAndConditions;

    public String getPaymentTerms() { return paymentTerms != null ? paymentTerms : ""; }
    public void setPaymentTerms(String v) { paymentTerms = v; }

    public String getTermsAndConditions() {
        if (termsAndConditions != null && !termsAndConditions.isBlank()) {
            return termsAndConditions;
        }
        return "1. All goods sold are inspected & delivered in good condition. Returns accepted within 7 days with original invoice.\n" +
               "2. Warranty applies to manufacturing defects subject to manufacturer inspection.\n" +
               "3. Cheques payable to 'K-Line Tyre House'. Overdue payments subject to credit terms.";
    }
    public void setTermsAndConditions(String v) { termsAndConditions = v; }

    public String getVehicleNumber() { return vehicleNumber != null ? vehicleNumber : ""; }
    public void setVehicleNumber(String v) { vehicleNumber = v; }

    public String getDescription() { return description != null ? description : ""; }
    public void setDescription(String v) { description = v; }

    public String getDueDate() { return dueDate != null ? dueDate : ""; }
    public void setDueDate(String v) { dueDate = v; }

    public String getSerialNumber() { return serialNumber != null ? serialNumber : ""; }
    public void setSerialNumber(String v) { serialNumber = v; }

    public String getTyreSize() { return tyreSize != null ? tyreSize : ""; }
    public void setTyreSize(String v) { tyreSize = v; }

    public String getTyreMake() { return tyreMake != null ? tyreMake : ""; }
    public void setTyreMake(String v) { tyreMake = v; }

    public String getRemark() { return remark != null ? remark : ""; }
    public void setRemark(String v) { remark = v; }

    public double getInitialPayment() { return initialPayment; }
    public void setInitialPayment(double v) { initialPayment = Math.max(0, v); }

    public void addLineItem(LineItem item)    { lineItems.add(item); }
    public void removeLineItem(LineItem item) { lineItems.remove(item); }
    public List<LineItem> getLineItems()      { return lineItems; }

    public void addPaymentRecord(PaymentRecord record) { if (record != null) paymentHistory.add(record); }
    public void setPaymentHistory(List<PaymentRecord> records) {
        paymentHistory.clear();
        if (records != null) paymentHistory.addAll(records);
    }
    public List<PaymentRecord> getPaymentHistory() { return paymentHistory; }

    public double getSubtotal()  { return lineItems.stream().mapToDouble(LineItem::getTotal).sum(); }
    public double getTax()       { return getSubtotal() * taxRate; }
    public double getGrandTotal(){ return Math.max(0, getSubtotal() + getTax() - discountAmount); }

    public double getTotalSettlements() {
        return paymentHistory.stream().mapToDouble(PaymentRecord::getAmount).sum();
    }

    public boolean isFullyPaid() {
        if ("quotation".equalsIgnoreCase(status)) {
            return false;
        }
        if ("completed".equalsIgnoreCase(status) || "paid".equalsIgnoreCase(status)) {
            return true;
        }
        return getBalanceDue() <= 0.001;
    }

    public double getTotalPaid() {
        if ("quotation".equalsIgnoreCase(status)) {
            return 0.0;
        }
        if (!paymentHistory.isEmpty()) {
            return getTotalSettlements();
        }
        if ("completed".equalsIgnoreCase(status) || "paid".equalsIgnoreCase(status)) {
            return getGrandTotal();
        }
        return initialPayment;
    }

    public double getBalanceDue() {
        if ("quotation".equalsIgnoreCase(status)) {
            return getGrandTotal();
        }
        if ("completed".equalsIgnoreCase(status) || "paid".equalsIgnoreCase(status)) {
            return 0.0;
        }
        if (!paymentHistory.isEmpty()) {
            return Math.max(0.0, getGrandTotal() - getTotalSettlements());
        }
        return Math.max(0.0, getGrandTotal() - initialPayment);
    }

    public String getInvoiceId()         { return invoiceId; }
    public void   setInvoiceId(String v) { invoiceId = v; }
    public String getCustomer()          { return customer; }
    public void   setCustomer(String v)  { customer = v; }
    public String getDate()              { return date; }
    public void   setDate(String v)      { date = v; }
    public String getType()              { return type; }
    public void   setType(String v)      { type = v; }
    public String getPhone()             { return phone != null ? phone : ""; }
    public void   setPhone(String v)     { phone = v; }
    public double getTaxRate()           { return taxRate; }
    public void   setTaxRate(double v)   { taxRate = v; }
    public double getDiscountAmount()    { return discountAmount; }
    public void   setDiscountAmount(double v) { discountAmount = Math.max(0, v); }
    public String getStatus()            { return status; }
    public void   setStatus(String v)    { status = v == null || v.isBlank() ? "quotation" : v; }
}

