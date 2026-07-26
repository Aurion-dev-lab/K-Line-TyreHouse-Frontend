package com.gui.kline.models;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CreditSaleDetail {
    private String creditId;
    private String customerId;
    private String customerName;
    private LocalDate date;
    private LocalDate dueDate;
    private final List<Part> parts = new ArrayList<>();
    private String phone;
    private double settlement = 0.0;

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public void addPart(Part part) { parts.add(part); }
    public void removePart(Part part) { parts.remove(part); }
    public List<Part> getParts() { return parts; }

    public double getSubtotal() { 
        return parts.stream().mapToDouble(Part::getTotal).sum(); 
    }
    
    public double getGrandTotal() { 
        return getSubtotal(); 
    }
    
    public double getSettlement() { 
        return settlement; 
    }
    
    public void setSettlement(double v) { 
        this.settlement = v; 
    }

    public double getAmountDue() { 
        return getGrandTotal() - settlement; 
    }

    public String getCreditId() { return creditId; }
    public void setCreditId(String v) { creditId = v; }
    
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String v) { customerId = v; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String v) { customerName = v; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate v) { date = v; }
    
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate v) { dueDate = v; }

    // Backward compatibility wrapper for old controllers
    @Deprecated
    public String getCustomer() { return customerName; }
    @Deprecated
    public void setCustomer(String v) { this.customerName = v; }
    @Deprecated
    public double getPaid() { return getSettlement(); }
    @Deprecated
    public void setPaid(double v) { setSettlement(v); }
    @Deprecated
    public double getAmount() { return getGrandTotal(); }
    @Deprecated
    public double getLabour() { return 0.0; }
    @Deprecated
    public double getPartsCost() { return 0.0; }
    @Deprecated
    public double getDiscount() { return 0.0; }
}
