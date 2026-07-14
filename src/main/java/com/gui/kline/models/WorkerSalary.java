package com.gui.kline.models;

public class WorkerSalary {
    private final String workerId, name, role, avatarColor, status;
    private final int present, late, absent;
    private final double grossSalary, advances, creditBalance, paidAmount;

    public WorkerSalary(String workerId, String name, String role, String avatarColor,
                        int present, int late, int absent,
                        double grossSalary, double advances, double creditBalance, double paidAmount, String status) {
        this.workerId = workerId; this.name = name; this.role = role; this.avatarColor = avatarColor;
        this.present = present; this.late = late; this.absent = absent;
        this.grossSalary = grossSalary; this.advances = advances;
        this.creditBalance = creditBalance; this.paidAmount = paidAmount; this.status = status;
    }

    public String getName()          { return name; }
    public String getWorkerId()      { return workerId; }
    public String getRole()          { return role; }
    public String getAvatarColor()   { return avatarColor; }
    public String getStatus()        { return status; }
    public int    getPresent()       { return present; }
    public int    getLate()          { return late; }
    public int    getAbsent()        { return absent; }
    public double getGrossSalary()   { return grossSalary; }
    public double getAdvances()      { return advances; }
    public double getCreditBalance() { return creditBalance; }
    public double getNetPayable()    { return grossSalary - advances; }
    public double getPaidAmount()    { return paidAmount; }
    public double getRemainingPayable() { return Math.max(0, getNetPayable() - paidAmount); }

}
