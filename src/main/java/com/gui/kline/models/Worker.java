package com.gui.kline.models;

public class Worker {
    private final String id;
    private final String name;
    private final String phone;
    private final String role;
    private final String rate;
    private final String salaryType;

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

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
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
}
