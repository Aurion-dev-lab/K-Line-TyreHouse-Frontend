package com.gui.kline.models;

import java.time.LocalDate;

public class WorkerAttendance {
    private final String workerId;
    private final String workerName;
    private final String role;
    private final String rate;
    private final String salaryType;
    private final LocalDate date;
    private final String status;

    public WorkerAttendance(String workerId, String workerName, String role, String rate,
                            String salaryType, LocalDate date, String status) {
        this.workerId = workerId;
        this.workerName = workerName;
        this.role = role;
        this.rate = rate;
        this.salaryType = salaryType;
        this.date = date;
        this.status = status;
    }

    public String getWorkerId() {
        return workerId;
    }

    public String getWorkerName() {
        return workerName;
    }

    public String getRole() {
        return role;
    }

    public String getRate() {
        return rate;
    }

    public String getSalaryType() {
        return salaryType;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getStatus() {
        return status;
    }
}

