package com.gui.kline.server.repository;

import com.gui.kline.server.entity.CreditSaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for CreditSaleItem entity.
 */
@Repository
public interface CreditSaleItemRepository extends JpaRepository<CreditSaleItem, String>, JpaSpecificationExecutor<CreditSaleItem> {
    
    List<CreditSaleItem> findByCreditSaleId(String creditSaleId);
    
    List<CreditSaleItem> findByProductId(String productId);
    
    List<CreditSaleItem> findByProductCode(String productCode);
    
    List<CreditSaleItem> findByDescriptionContainingIgnoreCase(String description);
    
    List<CreditSaleItem> findByDeviceId(String deviceId);
    
    List<CreditSaleItem> findBySyncStatus(boolean syncStatus);
    
    List<CreditSaleItem> findByDeviceIdAndSyncStatus(String deviceId, boolean syncStatus);
    
    List<CreditSaleItem> findBySyncedAtAfter(LocalDateTime syncedAfter);
    
    @Query("SELECT COALESCE(SUM(csi.total), 0) FROM CreditSaleItem csi WHERE csi.creditSaleId = :creditSaleId")
    double getTotalByCreditSaleId(String creditSaleId);
    
    @Query("SELECT COUNT(csi) FROM CreditSaleItem csi WHERE csi.creditSaleId = :creditSaleId")
    long countByCreditSaleId(String creditSaleId);
    
    void deleteByCreditSaleId(String creditSaleId);
    
    long countByProductId(String productId);
}