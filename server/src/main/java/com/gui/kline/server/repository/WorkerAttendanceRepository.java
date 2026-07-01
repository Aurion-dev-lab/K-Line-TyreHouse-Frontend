package com.gui.kline.server.repository;

import com.gui.kline.server.entity.WorkerAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for WorkerAttendance entity.
 */
@Repository
public interface WorkerAttendanceRepository extends JpaRepository<WorkerAttendance, String>, JpaSpecificationExecutor<WorkerAttendance> {
    
    List<WorkerAttendance> findByWorkerId(String workerId);
    
    List<WorkerAttendance> findByWorkerNameContainingIgnoreCase(String workerName);
    
    List<WorkerAttendance> findByAttendanceDate(LocalDate attendanceDate);
    
    List<WorkerAttendance> findByAttendanceDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<WorkerAttendance> findByStatus(WorkerAttendance.Status status);
    
    List<WorkerAttendance> findByShift(WorkerAttendance.Shift shift);
    
    List<WorkerAttendance> findByCheckInTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    List<WorkerAttendance> findByOnLeave(boolean onLeave);
    
    List<WorkerAttendance> findByLeaveType(WorkerAttendance.LeaveType leaveType);
    
    List<WorkerAttendance> findByDeviceId(String deviceId);
    
    List<WorkerAttendance> findBySyncStatus(boolean syncStatus);
    
    List<WorkerAttendance> findByDeviceIdAndSyncStatus(String deviceId, boolean syncStatus);
    
    List<WorkerAttendance> findBySyncedAtAfter(LocalDateTime syncedAfter);
    
    @Query("SELECT wa FROM WorkerAttendance wa WHERE wa.workerName LIKE %:query% OR wa.status LIKE %:query% OR wa.leaveReason LIKE %:query%")
    List<WorkerAttendance> searchByQuery(String query);
    
    @Query("SELECT wa FROM WorkerAttendance wa WHERE wa.workerId = :workerId AND wa.attendanceDate BETWEEN :startDate AND :endDate")
    List<WorkerAttendance> findByWorkerIdAndDateRange(String workerId, LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT COUNT(wa) FROM WorkerAttendance wa WHERE wa.workerId = :workerId AND wa.attendanceDate BETWEEN :startDate AND :endDate AND wa.status = 'PRESENT'")
    long countPresentDaysByWorkerAndDateRange(String workerId, LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT COUNT(wa) FROM WorkerAttendance wa WHERE wa.attendanceDate BETWEEN :startDate AND :endDate AND wa.status = 'PRESENT'")
    long countTotalPresentDaysByDateRange(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT COUNT(wa) FROM WorkerAttendance wa WHERE wa.attendanceDate = :attendanceDate AND wa.status = 'PRESENT'")
    long countPresentToday(LocalDate attendanceDate);
    
    long countByAttendanceDate(LocalDate attendanceDate);
    
    long countByWorkerIdAndStatus(String workerId, WorkerAttendance.Status status);
}