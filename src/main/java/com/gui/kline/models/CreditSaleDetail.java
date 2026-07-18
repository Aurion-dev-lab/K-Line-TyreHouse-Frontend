package com.gui.kline.models;

import com.gui.kline.controller.CreditSalesController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CreditSaleDetail {
    private String creditId, customer;
    private LocalDate date, dueDate;
    private final List<Part> parts = new ArrayList<>();
    private double paid = 0.0;
    private double labour = 0.0;
    private double partsCost = 0.0;
    private double discount = 0.0;

    public void addPart(Part part) { parts.add(part); }
    public void removePart(Part part) { parts.remove(part); }
    public List<Part> getParts() { return parts; }

    public double getSubtotal() { return parts.stream().mapToDouble(Part::getTotal).sum(); }
    public double getAmount() { 
        return getSubtotal() + labour + partsCost - discount; 
    }
    public double getPaid() { return paid; }
    public double getAmountDue() { return getAmount() - paid; }

    public String getCreditId() { return creditId; }
    public void setCreditId(String v) { creditId = v; }
    public String getCustomer() { return customer; }
    public void setCustomer(String v) { customer = v; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate v) { date = v; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate v) { dueDate = v; }
    public void setPaid(double v) { paid = v; }
    public double getLabour() { return labour; }
    public void setLabour(double v) { labour = v; }
    public double getPartsCost() { return partsCost; }
    public void setPartsCost(double v) { partsCost = v; }
    public double getDiscount() { return discount; }
    public void setDiscount(double v) { discount = v; }
}
