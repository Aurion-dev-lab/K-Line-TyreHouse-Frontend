package com.gui.kline.models;

import java.time.LocalDate;

public class WorkerAttendanceHistory {
    private final LocalDate date;
    private final String workerName;
    private final String status;

    public WorkerAttendanceHistory(LocalDate date, String workerName, String status) {
        this.date = date;
        this.workerName = workerName;
        this.status = status;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getWorkerName() {
        return workerName;
    }

    public String getStatus() {
        return status;
    }
}

