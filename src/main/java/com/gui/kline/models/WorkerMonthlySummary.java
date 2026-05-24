package com.gui.kline.models;

public class WorkerMonthlySummary {
    private final String workerName;
    private final double days;
    private final double netPayable;

    public WorkerMonthlySummary(String workerName, double days, double netPayable) {
        this.workerName = workerName;
        this.days = days;
        this.netPayable = netPayable;
    }

    public String getWorkerName() {
        return workerName;
    }

    public double getDays() {
        return days;
    }

    public double getNetPayable() {
        return netPayable;
    }
}

