package com.gui.kline.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for WorkerAttendance entity.
 */
@Data
@NoArgsConstructor
public class WorkerAttendanceDTO {
    
    private String id;
    
    @NotBlank(message = "Worker ID is required")
    @Size(max = 100)
    private String workerId;
    
    @Size(max = 200)
    private String workerName;
    
    private LocalDate attendanceDate;
    
    private LocalDateTime checkInTime;
    
    private LocalDateTime checkOutTime;
    
    @Size(max = 50)
    private String status = "PRESENT"; // PRESENT, ABSENT, LATE, HALF_DAY, LEAVE
    
    @Size(max = 50)
    private String shift = "MORNING"; // MORNING, AFTERNOON, NIGHT, FULL_DAY
    
    @Size(max = 500)
    private String notes;
    
    private double overtimeHours = 0;
    
    private double regularHours = 0;
    
    private boolean onLeave = false;
    
    @Size(max = 100)
    private String leaveType; // ANNUAL, SICK, CASUAL, MATERNITY, PATERNITY, UNPAID
    
    @Size(max = 500)
    private String leaveReason;
    
    private String approvedBy;
    
    private LocalDateTime approvedAt;
    
    private String syncId;
    
    private String deviceId;
    
    private LocalDateTime syncedAt;
    
    private boolean syncStatus = false;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    // Calculated fields
    private Double totalHours;
    
    private Boolean isLate;
    
    public WorkerAttendanceDTO(String id, String workerId, String workerName, LocalDate attendanceDate) {
        this.id = id;
        this.workerId = workerId;
        this.workerName = workerName;
        this.attendanceDate = attendanceDate;
        this.status = "PRESENT";
        this.checkInTime = LocalDateTime.now();
    }
    
    public enum Status {
        PRESENT, ABSENT, LATE, HALF_DAY, LEAVE, ON_LEAVE, BUSINESS_TRIP
    }
    
    public enum Shift {
        MORNING, AFTERNOON, NIGHT, FULL_DAY, SPLIT
    }
    
    public enum LeaveType {
        ANNUAL, SICK, CASUAL, MATERNITY, PATERNITY, UNPAID, COMPASSIONATE, STUDY, SPECIAL
    }
}