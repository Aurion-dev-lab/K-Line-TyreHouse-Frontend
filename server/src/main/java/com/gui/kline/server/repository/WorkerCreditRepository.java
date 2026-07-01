package com.gui.kline.server.repository;

import com.gui.kline.server.entity.WorkerCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for WorkerCredit entity.
 */
@Repository
public interface WorkerCreditRepository extends JpaRepository<WorkerCredit, String>, JpaSpecificationExecutor<WorkerCredit> {
    
    List<WorkerCredit> findByWorkerId(String workerId);
    
    List<WorkerCredit> findByWorkerNameContainingIgnoreCase(String workerName);
    
    List<WorkerCredit> findByCreditDate(LocalDate creditDate);
    
    List<WorkerCredit> findByCreditDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<WorkerCredit> findByType(WorkerCredit.CreditType type);
    
    List<WorkerCredit> findByStatus(WorkerCredit.Status status);
    
    List<WorkerCredit> findByDeviceId(String deviceId);
    
    List<WorkerCredit> findBySyncStatus(boolean syncStatus);
    
    List<WorkerCredit> findByDeviceIdAndSyncStatus(String deviceId, boolean syncStatus);
    
    List<WorkerCredit> findBySyncedAtAfter(LocalDateTime syncedAfter);
    
    @Query("SELECT wc FROM WorkerCredit wc WHERE wc.workerName LIKE %:query% OR wc.description LIKE %:query% OR wc.paymentReference LIKE %:query%")
    List<WorkerCredit> searchByQuery(String query);
    
    @Query("SELECT wc FROM WorkerCredit wc WHERE wc.remainingAmount > 0 AND wc.status IN ('ACTIVE', 'PARTIALLY_SETTLED')")
    List<WorkerCredit> findWorkerCreditsWithOutstandingBalance();
    
    @Query("SELECT wc FROM WorkerCredit wc WHERE wc.settlementDueDate < CURRENT_DATE AND wc.remainingAmount > 0 AND wc.status != 'CANCELLED'")
    List<WorkerCredit> findOverdueWorkerCredits();
    
    @Query("SELECT COALESCE(SUM(wc.amount), 0) FROM WorkerCredit wc WHERE wc.creditDate BETWEEN :startDate AND :endDate")
    double getTotalAmountByDateRange(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT COALESCE(SUM(wc.settledAmount), 0) FROM WorkerCredit wc WHERE wc.creditDate BETWEEN :startDate AND :endDate")
    double getTotalSettledAmountByDateRange(LocalDate startDate, LocalDate endDate);
    
    long countByStatus(WorkerCredit.Status status);
    
    long countByWorkerId(String workerId);
    
    long countByType(WorkerCredit.CreditType type);
}