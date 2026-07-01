package com.gui.kline.server.repository;

import com.gui.kline.server.entity.TyreExport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for TyreExport entity.
 */
@Repository
public interface TyreExportRepository extends JpaRepository<TyreExport, String>, JpaSpecificationExecutor<TyreExport> {
    
    Optional<TyreExport> findByExportNumber(String exportNumber);
    
    List<TyreExport> findByExportDate(LocalDate exportDate);
    
    List<TyreExport> findByExportDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<TyreExport> findByCompanyContainingIgnoreCase(String company);
    
    List<TyreExport> findByOperation(TyreExport.ExportOperation operation);
    
    List<TyreExport> findByStatus(TyreExport.ExportStatus status);
    
    List<TyreExport> findByPaymentStatus(TyreExport.PaymentStatus paymentStatus);
    
    List<TyreExport> findByPaymentMethod(String paymentMethod);
    
    List<TyreExport> findByDeviceId(String deviceId);
    
    List<TyreExport> findBySyncStatus(boolean syncStatus);
    
    List<TyreExport> findByDeviceIdAndSyncStatus(String deviceId, boolean syncStatus);
    
    List<TyreExport> findBySyncedAtAfter(LocalDateTime syncedAfter);
    
    @Query("SELECT te FROM TyreExport te WHERE te.exportNumber LIKE %:query% OR te.company LIKE %:query% OR te.paymentReference LIKE %:query%")
    List<TyreExport> searchByQuery(String query);
    
    @Query("SELECT te FROM TyreExport te WHERE te.balanceAmount > 0 AND te.paymentStatus IN ('PENDING', 'PARTIAL')")
    List<TyreExport> findTyreExportsWithOutstandingBalance();
    
    @Query("SELECT te FROM TyreExport te WHERE te.paymentDueDate < CURRENT_DATE AND te.balanceAmount > 0 AND te.paymentStatus != 'CANCELLED'")
    List<TyreExport> findOverdueTyreExports();
    
    @Query("SELECT COALESCE(SUM(te.totalAmount), 0) FROM TyreExport te WHERE te.exportDate BETWEEN :startDate AND :endDate")
    double getTotalAmountByDateRange(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT COALESCE(SUM(te.compPrice * te.tyres), 0) FROM TyreExport te WHERE te.exportDate BETWEEN :startDate AND :endDate")
    double getTotalCostByDateRange(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT COALESCE(SUM(te.totalAmount - te.compPrice * te.tyres), 0) FROM TyreExport te WHERE te.exportDate BETWEEN :startDate AND :endDate")
    double getTotalProfitByDateRange(LocalDate startDate, LocalDate endDate);
    
    long countByStatus(TyreExport.ExportStatus status);
    
    long countByOperation(TyreExport.ExportOperation operation);
    
    long countByPaymentStatus(TyreExport.PaymentStatus paymentStatus);
    
    boolean existsByExportNumber(String exportNumber);
}