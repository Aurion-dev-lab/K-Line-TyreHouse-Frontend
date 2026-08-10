package com.gui.kline.models.reports;

import java.time.LocalDate;

public class WorkerAttendanceHistory {
    private final LocalDate date;
    private final String workerId;
    private final String workerName;
    private final String status;

    public WorkerAttendanceHistory(LocalDate date, String workerId, String workerName, String status) {
        this.date = date;
        this.workerId = workerId;
        this.workerName = workerName;
        this.status = status;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getWorkerId() {
        return workerId;
    }

    public String getWorkerName() {
        return workerName;
    }

    public String getStatus() {
        return status;
    }
}

