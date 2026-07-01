package com.gui.kline.server.repository;

import com.gui.kline.server.entity.InvoiceLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for InvoiceLineItem entity.
 */
@Repository
public interface InvoiceLineItemRepository extends JpaRepository<InvoiceLineItem, String>, JpaSpecificationExecutor<InvoiceLineItem> {
    
    List<InvoiceLineItem> findByInvoiceId(String invoiceId);
    
    List<InvoiceLineItem> findByProductId(String productId);
    
    List<InvoiceLineItem> findByProductCode(String productCode);
    
    List<InvoiceLineItem> findByDescriptionContainingIgnoreCase(String description);
    
    List<InvoiceLineItem> findByCategory(String category);
    
    List<InvoiceLineItem> findByItemType(InvoiceLineItem.ItemType itemType);
    
    List<InvoiceLineItem> findByDeviceId(String deviceId);
    
    List<InvoiceLineItem> findBySyncStatus(boolean syncStatus);
    
    List<InvoiceLineItem> findByDeviceIdAndSyncStatus(String deviceId, boolean syncStatus);
    
    List<InvoiceLineItem> findBySyncedAtAfter(LocalDateTime syncedAfter);
    
    @Query("SELECT ili FROM InvoiceLineItem ili WHERE ili.invoiceId IN (SELECT i.id FROM Invoice i WHERE i.invoiceDate BETWEEN :startDate AND :endDate)")
    List<InvoiceLineItem> findByInvoiceDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate);
    
    @Query("SELECT COALESCE(SUM(ili.total), 0) FROM InvoiceLineItem ili WHERE ili.invoiceId = :invoiceId")
    double getTotalByInvoiceId(String invoiceId);
    
    @Query("SELECT COUNT(ili) FROM InvoiceLineItem ili WHERE ili.invoiceId = :invoiceId")
    long countByInvoiceId(String invoiceId);
    
    @Query("SELECT ili FROM InvoiceLineItem ili WHERE ili.productId = :productId AND ili.invoiceId IN (SELECT i.id FROM Invoice i WHERE i.invoiceDate BETWEEN :startDate AND :endDate)")
    List<InvoiceLineItem> findByProductIdAndDateRange(String productId, java.time.LocalDate startDate, java.time.LocalDate endDate);
    
    void deleteByInvoiceId(String invoiceId);
    
    long countByProductId(String productId);
    
    long countByCategory(String category);
}