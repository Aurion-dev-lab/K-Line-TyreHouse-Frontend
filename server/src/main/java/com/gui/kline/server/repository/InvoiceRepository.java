package com.gui.kline.server.repository;

import com.gui.kline.server.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Invoice entity.
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, String>, JpaSpecificationExecutor<Invoice> {
    
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    
    List<Invoice> findByInvoiceDate(LocalDate invoiceDate);
    
    List<Invoice> findByInvoiceDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<Invoice> findByCustomerId(String customerId);
    
    List<Invoice> findByCustomerNameContainingIgnoreCase(String customerName);
    
    List<Invoice> findByType(Invoice.InvoiceType type);
    
    List<Invoice> findByStatus(Invoice.InvoiceStatus status);
    
    List<Invoice> findByPaymentMethod(String paymentMethod);
    
    List<Invoice> findByIsCredit(boolean isCredit);
    
    List<Invoice> findByDeviceId(String deviceId);
    
    List<Invoice> findBySyncStatus(boolean syncStatus);
    
    List<Invoice> findByDeviceIdAndSyncStatus(String deviceId, boolean syncStatus);
    
    List<Invoice> findBySyncedAtAfter(LocalDateTime syncedAfter);
    
    @Query("SELECT i FROM Invoice i WHERE i.invoiceNumber LIKE %:query% OR i.customerName LIKE %:query% OR i.paymentReference LIKE %:query%")
    List<Invoice> searchByQuery(String query);
    
    @Query("SELECT i FROM Invoice i WHERE i.invoiceDate BETWEEN :startDate AND :endDate AND i.status = com.gui.kline.server.entity.Invoice.InvoiceStatus.COMPLETED")
    List<Invoice> findCompletedInvoicesByDateRange(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT i FROM Invoice i WHERE i.balanceDue > 0 AND i.status IN ('PENDING', 'PARTIALLY_PAID', 'COMPLETED')")
    List<Invoice> findInvoicesWithOutstandingBalance();
    
    @Query("SELECT i FROM Invoice i WHERE i.grandTotal >= :minAmount AND i.grandTotal <= :maxAmount")
    List<Invoice> findByAmountRange(double minAmount, double maxAmount);
    
    @Query("SELECT COALESCE(SUM(i.grandTotal), 0) FROM Invoice i WHERE i.invoiceDate BETWEEN :startDate AND :endDate")
    double getTotalSalesAmountByDateRange(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.invoiceDate BETWEEN :startDate AND :endDate")
    long countByDateRange(LocalDate startDate, LocalDate endDate);
    
    long countByStatus(Invoice.InvoiceStatus status);
    
    long countByType(Invoice.InvoiceType type);
    
    long countByCustomerId(String customerId);
    
    boolean existsByInvoiceNumber(String invoiceNumber);
}