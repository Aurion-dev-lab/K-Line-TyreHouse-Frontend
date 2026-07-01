package com.gui.kline.server.repository;

import com.gui.kline.server.entity.CreditSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for CreditSale entity.
 */
@Repository
public interface CreditSaleRepository extends JpaRepository<CreditSale, String>, JpaSpecificationExecutor<CreditSale> {
    
    Optional<CreditSale> findByCreditSaleNumber(String creditSaleNumber);
    
    List<CreditSale> findBySaleDate(LocalDate saleDate);
    
    List<CreditSale> findBySaleDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<CreditSale> findByCustomerId(String customerId);
    
    List<CreditSale> findByCustomerNameContainingIgnoreCase(String customerName);
    
    List<CreditSale> findByStatus(CreditSale.CreditStatus status);
    
    List<CreditSale> findBySalespersonId(String salespersonId);
    
    List<CreditSale> findByDueDate(LocalDate dueDate);
    
    List<CreditSale> findByDueDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<CreditSale> findByDeviceId(String deviceId);
    
    List<CreditSale> findBySyncStatus(boolean syncStatus);
    
    List<CreditSale> findByDeviceIdAndSyncStatus(String deviceId, boolean syncStatus);
    
    List<CreditSale> findBySyncedAtAfter(LocalDateTime syncedAfter);
    
    @Query("SELECT cs FROM CreditSale cs WHERE cs.creditSaleNumber LIKE %:query% OR cs.customerName LIKE %:query% OR cs.paymentReference LIKE %:query%")
    List<CreditSale> searchByQuery(String query);
    
    @Query("SELECT cs FROM CreditSale cs WHERE cs.balanceAmount > 0 AND cs.status IN ('ACTIVE', 'PARTIALLY_PAID', 'OVERDUE')")
    List<CreditSale> findCreditSalesWithOutstandingBalance();
    
    @Query("SELECT cs FROM CreditSale cs WHERE cs.dueDate < CURRENT_DATE AND cs.balanceAmount > 0 AND cs.status != 'CANCELLED'")
    List<CreditSale> findOverdueCreditSales();
    
    @Query("SELECT COALESCE(SUM(cs.amount), 0) FROM CreditSale cs WHERE cs.saleDate BETWEEN :startDate AND :endDate")
    double getTotalCreditSalesAmountByDateRange(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT COALESCE(SUM(cs.paidAmount), 0) FROM CreditSale cs WHERE cs.saleDate BETWEEN :startDate AND :endDate")
    double getTotalPaidAmountByDateRange(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT COALESCE(SUM(cs.balanceAmount), 0) FROM CreditSale cs WHERE cs.status IN ('ACTIVE', 'PARTIALLY_PAID', 'OVERDUE')")
    double getTotalOutstandingBalance();
    
    long countByStatus(CreditSale.CreditStatus status);
    
    long countByCustomerId(String customerId);
    
    boolean existsByCreditSaleNumber(String creditSaleNumber);
}