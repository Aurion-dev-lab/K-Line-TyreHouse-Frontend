package com.gui.kline.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class WorkerAttendance {
    private String id;
    private String workerId;
    private String workerName;
    private String role;
    private String rate;
    private String salaryType;
    private LocalDate date;
    private String status;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    
    // Sync fields
    private String syncId;
    private String deviceId;
    private LocalDateTime syncedAt;
    private boolean syncStatus = false;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public WorkerAttendance() {}

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

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkerId() {
        return workerId;
    }
    
    public void setWorkerId(String workerId) { this.workerId = workerId; }

    public String getWorkerName() {
        return workerName;
    }
    
    public void setWorkerName(String workerName) { this.workerName = workerName; }

    public String getRole() {
        return role;
    }
    
    public void setRole(String role) { this.role = role; }

    public String getRate() {
        return rate;
    }
    
    public void setRate(String rate) { this.rate = rate; }

    public String getSalaryType() {
        return salaryType;
    }
    
    public void setSalaryType(String salaryType) { this.salaryType = salaryType; }

    public LocalDate getDate() {
        return date;
    }
    
    public void setDate(LocalDate date) { this.date = date; }

    public LocalDate getAttendanceDate() { return date; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.date = attendanceDate; }

    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalDateTime checkInTime) { this.checkInTime = checkInTime; }

    public LocalDateTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalDateTime checkOutTime) { this.checkOutTime = checkOutTime; }

    // Sync field getters and setters
    public String getSyncId() { return syncId; }
    public void setSyncId(String syncId) { this.syncId = syncId; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public LocalDateTime getSyncedAt() { return syncedAt; }
    public void setSyncedAt(LocalDateTime syncedAt) { this.syncedAt = syncedAt; }

    public boolean isSyncStatus() { return syncStatus; }
    public void setSyncStatus(boolean syncStatus) { this.syncStatus = syncStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

