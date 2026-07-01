package com.gui.kline.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for Worker entity.
 */
@Data
@NoArgsConstructor
public class WorkerDTO {
    
    private String id;
    
    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;
    
    @Size(max = 100)
    private String lastName;
    
    @Size(max = 200)
    private String fullName;
    
    @Size(max = 50)
    private String nic; // National ID Card
    
    @Size(max = 50)
    private String phone;
    
    @Size(max = 50)
    private String alternatePhone;
    
    @Size(max = 200)
    private String email;
    
    private LocalDate dateOfBirth;
    
    @Size(max = 20)
    private String gender;
    
    @Size(max = 500)
    private String address;
    
    @Size(max = 100)
    private String city;
    
    @Size(max = 100)
    private String state;
    
    @Size(max = 100)
    private String country;
    
    @Size(max = 20)
    private String postalCode;
    
    @NotBlank(message = "Role is required")
    @Size(max = 50)
    private String role = "STAFF";
    
    @Size(max = 100)
    private String department;
    
    @Size(max = 100)
    private String designation;
    
    @Size(max = 500)
    private String skills;
    
    private SalaryType salaryType = SalaryType.DAILY;
    
    @PositiveOrZero(message = "Daily rate must be positive or zero")
    private double dailyRate = 0;
    
    @PositiveOrZero(message = "Monthly salary must be positive or zero")
    private double monthlySalary = 0;
    
    @PositiveOrZero(message = "Hourly rate must be positive or zero")
    private double hourlyRate = 0;
    
    @Size(max = 100)
    private String bankName;
    
    @Size(max = 50)
    private String bankAccountNumber;
    
    @Size(max = 100)
    private String bankBranch;
    
    @Size(max = 200)
    private String emergencyContactName;
    
    @Size(max = 50)
    private String emergencyContactPhone;
    
    @Size(max = 500)
    private String profileImageUrl;
    
    private boolean active = true;
    
    private LocalDate joinDate;
    
    private LocalDate terminationDate;
    
    @Size(max = 500)
    private String terminationReason;
    
    @Size(max = 500)
    private String notes;
    
    private String syncId;
    
    private String deviceId;
    
    private LocalDateTime syncedAt;
    
    private boolean syncStatus = false;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    // Calculated fields
    private boolean technician;
    
    private Double dailySalary;
    
    public WorkerDTO(String id, String firstName, String lastName, String phone) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = firstName + (lastName != null ? " " + lastName : "");
        this.phone = phone;
        this.active = true;
        this.joinDate = LocalDate.now();
    }
    
    public enum SalaryType {
        DAILY, MONTHLY, HOURLY, COMMISSION, CONTRACT
    }
    
    public enum Role {
        ADMIN, MANAGER, TECHNICIAN, SALES, ACCOUNTS, CLEANER, SUPERVISOR, OWNER
    }
}