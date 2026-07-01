package com.gui.kline.server.repository;

import com.gui.kline.server.entity.QuickService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for QuickService entity.
 */
@Repository
public interface QuickServiceRepository extends JpaRepository<QuickService, String>, JpaSpecificationExecutor<QuickService> {
    
    List<QuickService> findByServiceContainingIgnoreCase(String service);
    
    List<QuickService> findByDescriptionContainingIgnoreCase(String description);
    
    List<QuickService> findByServiceDate(LocalDate serviceDate);
    
    List<QuickService> findByServiceDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<QuickService> findByCustomerId(String customerId);
    
    List<QuickService> findByCustomerNameContainingIgnoreCase(String customerName);
    
    List<QuickService> findByVehicleNumber(String vehicleNumber);
    
    List<QuickService> findByPaid(boolean paid);
    
    List<QuickService> findByStatus(QuickService.Status status);
    
    List<QuickService> findByAssignedTo(String assignedTo);
    
    List<QuickService> findByDeviceId(String deviceId);
    
    List<QuickService> findBySyncStatus(boolean syncStatus);
    
    List<QuickService> findByDeviceIdAndSyncStatus(String deviceId, boolean syncStatus);
    
    List<QuickService> findBySyncedAtAfter(LocalDateTime syncedAfter);
    
    @Query("SELECT qs FROM QuickService qs WHERE qs.service LIKE %:query% OR qs.customerName LIKE %:query% OR qs.vehicleNumber LIKE %:query% OR qs.paymentReference LIKE %:query%")
    List<QuickService> searchByQuery(String query);
    
    @Query("SELECT COALESCE(SUM(qs.price), 0) FROM QuickService qs WHERE qs.serviceDate BETWEEN :startDate AND :endDate")
    double getTotalRevenueByDateRange(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT COALESCE(SUM(qs.price), 0) FROM QuickService qs WHERE qs.serviceDate BETWEEN :startDate AND :endDate AND qs.paid = true")
    double getTotalPaidRevenueByDateRange(LocalDate startDate, LocalDate endDate);
    
    long countByServiceDateBetween(LocalDate startDate, LocalDate endDate);
    
    long countByStatus(QuickService.Status status);
    
    long countByPaid(boolean paid);
}