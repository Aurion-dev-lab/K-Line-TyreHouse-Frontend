package com.gui.kline.models;

import java.time.LocalDateTime;

public class Worker {
    private String id;
    private String name;
    private String firstName;
    private String lastName;
    private String phone;
    private String role;
    private String rate;
    private String salaryType;
    private boolean active = true;
    
    // Sync fields
    private boolean syncStatus = false;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Worker() {}

    public Worker(String id, String name, String phone, String role, String rate, String salaryType) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.role = role;
        this.rate = rate;
        this.salaryType = salaryType;
    }

    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }

    public String getRate() {
        return rate;
    }
    
    public void setRate(String rate) {
        this.rate = rate;
    }

    public String getSalaryType() {
        return salaryType;
    }
    
    public void setSalaryType(String salaryType) {
        this.salaryType = salaryType;
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    // Sync field getters and setters

    public boolean isSyncStatus() { return syncStatus; }
    public void setSyncStatus(boolean syncStatus) { this.syncStatus = syncStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
