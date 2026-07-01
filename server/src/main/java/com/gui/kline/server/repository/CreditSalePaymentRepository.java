package com.gui.kline.server.repository;

import com.gui.kline.server.entity.CreditSalePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for CreditSalePayment entity.
 */
@Repository
public interface CreditSalePaymentRepository extends JpaRepository<CreditSalePayment, String>, JpaSpecificationExecutor<CreditSalePayment> {
    
    List<CreditSalePayment> findByCreditSaleId(String creditSaleId);
    
    List<CreditSalePayment> findByPaymentDate(LocalDate paymentDate);
    
    List<CreditSalePayment> findByPaymentDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<CreditSalePayment> findByPaymentMethod(String paymentMethod);
    
    List<CreditSalePayment> findByReceivedBy(String receivedBy);
    
    List<CreditSalePayment> findByDeviceId(String deviceId);
    
    List<CreditSalePayment> findBySyncStatus(boolean syncStatus);
    
    List<CreditSalePayment> findByDeviceIdAndSyncStatus(String deviceId, boolean syncStatus);
    
    List<CreditSalePayment> findBySyncedAtAfter(LocalDateTime syncedAfter);
    
    @Query("SELECT COALESCE(SUM(csp.amount), 0) FROM CreditSalePayment csp WHERE csp.creditSaleId = :creditSaleId")
    double getTotalAmountByCreditSaleId(String creditSaleId);
    
    @Query("SELECT COUNT(csp) FROM CreditSalePayment csp WHERE csp.creditSaleId = :creditSaleId")
    long countByCreditSaleId(String creditSaleId);
    
    void deleteByCreditSaleId(String creditSaleId);
    
    long countByPaymentMethod(String paymentMethod);
}