package com.gui.kline.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Worker attendance record for tracking employee presence, late arrivals, etc.
 */
@Entity
@Table(name = "worker_attendance", indexes = {
    @Index(name = "idx_attendance_worker", columnList = "workerId"),
    @Index(name = "idx_attendance_date", columnList = "attendanceDate"),
    @Index(name = "idx_attendance_status", columnList = "status"),
    @Index(name = "idx_attendance_device", columnList = "deviceId")
})
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WorkerAttendance extends BaseEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String workerId;

    @Column
    private String workerName;

    @Column(nullable = false)
    private LocalDate attendanceDate;

    @Column(nullable = false)
    private AttendanceStatus status = AttendanceStatus.PRESENT;

    @Column
    private LocalTime checkInTime;

    @Column
    private LocalTime checkOutTime;

    @Column
    private String lateReason;

    @Column
    private String earlyLeaveReason;

    @Column
    private boolean isLate = false;

    @Column
    private boolean isEarlyLeave = false;

    @Column
    private boolean isHalfDay = false;

    @Column
    private String notes;

    @Column
    private String approvedBy;

    @Column
    private LocalDateTime approvedAt;

    @Column
    private boolean isApproved = false;

    @Column
    private String shift;

    @Column
    private String location;

    @Column
    private String deviceUsed; // Device used for check-in

    @Column
    private String syncId; // For tracking sync status

    @Column
    private String deviceId; // Device that owns this attendance record

    @Column
    private LocalDateTime syncedAt;

    @Column
    private boolean syncStatus = false;

    public WorkerAttendance(String id, String workerId, String workerName, 
                           LocalDate attendanceDate, AttendanceStatus status) {
        this.id = id;
        this.workerId = workerId;
        this.workerName = workerName;
        this.attendanceDate = attendanceDate;
        this.status = status;
        this.checkInTime = LocalTime.now();
    }

    public enum AttendanceStatus {
        PRESENT, ABSENT, LATE, HALF_DAY, LEAVE, HOLIDAY, BUSINESS_TRIP, TRAINING, SICK_LEAVE, MATERNITY_LEAVE, PATERNITY_LEAVE
    }

    public enum LeaveType {
        ANNUAL, SICK, CASUAL, MATERNITY, PATERNITY, COMPASSIONATE, UNPAID, SPECIAL
    }

    public double getWorkingHours() {
        if (checkInTime == null || checkOutTime == null) return 0;
        
        long minutes = java.time.Duration.between(checkInTime, checkOutTime).toMinutes();
        // Subtract lunch break (30 minutes) if worked more than 4 hours
        if (minutes > 240) {
            minutes -= 30;
        }
        
        return minutes / 60.0;
    }

    public boolean isLate() {
        return this.isLate || (this.checkInTime != null && 
               this.checkInTime.isAfter(LocalTime.of(9, 0))); // Default late after 9 AM
    }

    public boolean isEarlyLeave() {
        return this.isEarlyLeave || (this.checkOutTime != null && 
               this.checkOutTime.isBefore(LocalTime.of(17, 0))); // Default early before 5 PM
    }

    public boolean isFullDay() {
        return this.status == AttendanceStatus.PRESENT && 
               !isLate() && !isEarlyLeave() && !isHalfDay;
    }
}