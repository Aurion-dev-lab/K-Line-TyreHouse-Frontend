package com.gui.kline.server.repository;

import com.gui.kline.server.entity.TyreExportPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for TyreExportPayment entity.
 */
@Repository
public interface TyreExportPaymentRepository extends JpaRepository<TyreExportPayment, String>, JpaSpecificationExecutor<TyreExportPayment> {
    
    List<TyreExportPayment> findByTyreExportId(String tyreExportId);
    
    List<TyreExportPayment> findByPaymentDate(LocalDate paymentDate);
    
    List<TyreExportPayment> findByPaymentDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<TyreExportPayment> findByPaymentMethod(String paymentMethod);
    
    List<TyreExportPayment> findByReceivedBy(String receivedBy);
    
    List<TyreExportPayment> findByDeviceId(String deviceId);
    
    List<TyreExportPayment> findBySyncStatus(boolean syncStatus);
    
    List<TyreExportPayment> findByDeviceIdAndSyncStatus(String deviceId, boolean syncStatus);
    
    List<TyreExportPayment> findBySyncedAtAfter(LocalDateTime syncedAfter);
    
    @Query("SELECT COALESCE(SUM(tep.amount), 0) FROM TyreExportPayment tep WHERE tep.tyreExportId = :tyreExportId")
    double getTotalAmountByTyreExportId(String tyreExportId);
    
    @Query("SELECT COUNT(tep) FROM TyreExportPayment tep WHERE tep.tyreExportId = :tyreExportId")
    long countByTyreExportId(String tyreExportId);
    
    void deleteByTyreExportId(String tyreExportId);
    
    long countByPaymentMethod(String paymentMethod);
}