package com.gui.kline.server.repository;

import com.gui.kline.server.entity.SalaryAdvance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for SalaryAdvance entity.
 */
@Repository
public interface SalaryAdvanceRepository extends JpaRepository<SalaryAdvance, String>, JpaSpecificationExecutor<SalaryAdvance> {
    
    List<SalaryAdvance> findByWorkerId(String workerId);
    
    List<SalaryAdvance> findByWorkerNameContainingIgnoreCase(String workerName);
    
    List<SalaryAdvance> findByAdvanceDate(LocalDate advanceDate);
    
    List<SalaryAdvance> findByAdvanceDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<SalaryAdvance> findByStatus(SalaryAdvance.Status status);
    
    List<SalaryAdvance> findByDeviceId(String deviceId);
    
    List<SalaryAdvance> findBySyncStatus(boolean syncStatus);
    
    List<SalaryAdvance> findByDeviceIdAndSyncStatus(String deviceId, boolean syncStatus);
    
    List<SalaryAdvance> findBySyncedAtAfter(LocalDateTime syncedAfter);
    
    @Query("SELECT sa FROM SalaryAdvance sa WHERE sa.workerName LIKE %:query% OR sa.reason LIKE %:query% OR sa.paymentReference LIKE %:query%")
    List<SalaryAdvance> searchByQuery(String query);
    
    @Query("SELECT sa FROM SalaryAdvance sa WHERE sa.remainingAmount > 0 AND sa.status IN ('APPROVED', 'PARTIALLY_SETTLED')")
    List<SalaryAdvance> findSalaryAdvancesWithOutstandingBalance();
    
    @Query("SELECT sa FROM SalaryAdvance sa WHERE sa.settlementDueDate < CURRENT_DATE AND sa.remainingAmount > 0 AND sa.status != 'CANCELLED'")
    List<SalaryAdvance> findOverdueSalaryAdvances();
    
    @Query("SELECT COALESCE(SUM(sa.amount), 0) FROM SalaryAdvance sa WHERE sa.advanceDate BETWEEN :startDate AND :endDate")
    double getTotalAmountByDateRange(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT COALESCE(SUM(sa.settledAmount), 0) FROM SalaryAdvance sa WHERE sa.advanceDate BETWEEN :startDate AND :endDate")
    double getTotalSettledAmountByDateRange(LocalDate startDate, LocalDate endDate);
    
    long countByStatus(SalaryAdvance.Status status);
    
    long countByWorkerId(String workerId);
}