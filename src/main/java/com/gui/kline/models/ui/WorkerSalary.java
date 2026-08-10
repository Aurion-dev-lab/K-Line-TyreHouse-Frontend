package com.gui.kline.models.ui;

public class WorkerSalary {
    private final String workerId, name, role, avatarColor, status;
    private final int present, late, absent;
    private final double grossSalary, advances, creditBalance, payrollSettledCredit, paidAmount;

    public WorkerSalary(String workerId, String name, String role, String avatarColor,
                        int present, int late, int absent,
                        double grossSalary, double advances, double creditBalance, double paidAmount, String status) {
        this(workerId, name, role, avatarColor, present, late, absent, grossSalary, advances, creditBalance, 0.0, paidAmount, status);
    }

    public WorkerSalary(String workerId, String name, String role, String avatarColor,
                        int present, int late, int absent,
                        double grossSalary, double advances, double creditBalance, double payrollSettledCredit,
                        double paidAmount, String status) {
        this.workerId = workerId; this.name = name; this.role = role; this.avatarColor = avatarColor;
        this.present = present; this.late = late; this.absent = absent;
        this.grossSalary = grossSalary; this.advances = advances;
        this.creditBalance = creditBalance; this.payrollSettledCredit = payrollSettledCredit;
        this.paidAmount = paidAmount; this.status = status;
    }

    public String getName()                  { return name; }
    public String getWorkerId()              { return workerId; }
    public String getRole()                  { return role; }
    public String getAvatarColor()           { return avatarColor; }
    public String getStatus()                { return status; }
    public int    getPresent()               { return present; }
    public int    getLate()                  { return late; }
    public int    getAbsent()                { return absent; }
    public double getGrossSalary()           { return grossSalary; }
    public double getAdvances()              { return advances; }
    public double getCreditBalance()         { return creditBalance; }
    public double getPayrollSettledCredit()  { return payrollSettledCredit; }

    /** Cash salary due after salary advances and optionally the worker's outstanding credit are deducted. */
    public double getNetPayable()            { return getNetPayable(true); }
    public double getNetPayable(boolean includeCreditDeduction) {
        double creditDeduction = (includeCreditDeduction ? creditBalance : 0) + payrollSettledCredit;
        return Math.max(0, grossSalary - advances - creditDeduction);
    }

    public double getPaidAmount()            { return paidAmount; }
    public double getRemainingPayable()      { return getRemainingPayable(true); }
    public double getRemainingPayable(boolean includeCreditDeduction) {
        return Math.max(0, getNetPayable(includeCreditDeduction) - paidAmount);
    }

    public String getStatus(boolean includeCreditDeduction) {
        double net = getNetPayable(includeCreditDeduction);
        if (net <= 0) return "NO PAYABLE";
        if (paidAmount >= net - 0.0001) return "PAID";
        if (paidAmount > 0) return "PARTIALLY PAID";
        return "READY";
    }
}
