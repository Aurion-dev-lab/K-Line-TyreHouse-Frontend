package com.gui.kline.server.repository;

import com.gui.kline.server.entity.ServiceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for ServiceRecord entity.
 */
@Repository
public interface ServiceRecordRepository extends JpaRepository<ServiceRecord, String>, JpaSpecificationExecutor<ServiceRecord> {
    
    List<ServiceRecord> findByServiceNameContainingIgnoreCase(String serviceName);
    
    List<ServiceRecord> findByServiceDate(LocalDate serviceDate);
    
    List<ServiceRecord> findByServiceDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<ServiceRecord> findByAssignedTo(String assignedTo);
    
    List<ServiceRecord> findByAssignedToNameContainingIgnoreCase(String assignedToName);
    
    List<ServiceRecord> findByCustomerId(String customerId);
    
    List<ServiceRecord> findByCustomerNameContainingIgnoreCase(String customerName);
    
    List<ServiceRecord> findByVehicleNumber(String vehicleNumber);
    
    List<ServiceRecord> findByStatus(String status);
    
    List<ServiceRecord> findByPaid(boolean paid);
    
    List<ServiceRecord> findByDeviceId(String deviceId);
    
    List<ServiceRecord> findBySyncStatus(boolean syncStatus);
    
    List<ServiceRecord> findByDeviceIdAndSyncStatus(String deviceId, boolean syncStatus);
    
    List<ServiceRecord> findBySyncedAtAfter(LocalDateTime syncedAfter);
    
    @Query("SELECT s FROM ServiceRecord s WHERE s.serviceName LIKE %:query% OR s.customerName LIKE %:query% OR s.vehicleNumber LIKE %:query% OR s.paymentReference LIKE %:query%")
    List<ServiceRecord> searchByQuery(String query);
    
    @Query("SELECT s FROM ServiceRecord s WHERE s.serviceDate BETWEEN :startDate AND :endDate AND s.paid = true")
    List<ServiceRecord> findPaidServicesByDateRange(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM ServiceRecord s WHERE s.serviceDate BETWEEN :startDate AND :endDate")
    double getTotalRevenueByDateRange(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM ServiceRecord s WHERE s.assignedTo = :workerId AND s.serviceDate BETWEEN :startDate AND :endDate")
    double getTotalRevenueByWorkerAndDateRange(String workerId, LocalDate startDate, LocalDate endDate);
    
    long countByServiceDateBetween(LocalDate startDate, LocalDate endDate);
    
    long countByAssignedTo(String assignedTo);
    
    long countByStatus(String status);
}