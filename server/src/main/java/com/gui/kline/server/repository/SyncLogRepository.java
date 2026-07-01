package com.gui.kline.server.repository;

import com.gui.kline.server.entity.SyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for SyncLog entity.
 */
@Repository
public interface SyncLogRepository extends JpaRepository<SyncLog, String>, JpaSpecificationExecutor<SyncLog> {
    
    List<SyncLog> findByDeviceId(String deviceId);
    
    List<SyncLog> findByDeviceNameContainingIgnoreCase(String deviceName);
    
    List<SyncLog> findBySyncType(String syncType);
    
    List<SyncLog> findByOperation(String operation);
    
    List<SyncLog> findByStatus(String status);
    
    List<SyncLog> findByStartTimeAfter(LocalDateTime startTimeAfter);
    
    List<SyncLog> findByEndTimeAfter(LocalDateTime endTimeAfter);
    
    List<SyncLog> findByStartTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    @Query("SELECT sl FROM SyncLog sl WHERE sl.deviceName LIKE %:query% OR sl.details LIKE %:query% OR sl.errorMessage LIKE %:query%")
    List<SyncLog> searchByQuery(String query);
    
    @Query("SELECT sl FROM SyncLog sl WHERE sl.status = 'FAILED' ORDER BY sl.startTime DESC")
    List<SyncLog> findFailedSyncs();
    
    @Query("SELECT sl FROM SyncLog sl WHERE sl.deviceId = :deviceId ORDER BY sl.startTime DESC")
    List<SyncLog> findLatestSyncsByDevice(String deviceId);
    
    @Query("SELECT COALESCE(SUM(sl.itemsSynced), 0) FROM SyncLog sl WHERE sl.deviceId = :deviceId AND sl.startTime BETWEEN :startTime AND :endTime")
    long getTotalItemsSyncedByDeviceAndTimeRange(String deviceId, LocalDateTime startTime, LocalDateTime endTime);
    
    @Query("SELECT COALESCE(SUM(sl.itemsFailed), 0) FROM SyncLog sl WHERE sl.deviceId = :deviceId AND sl.startTime BETWEEN :startTime AND :endTime")
    long getTotalItemsFailedByDeviceAndTimeRange(String deviceId, LocalDateTime startTime, LocalDateTime endTime);
    
    long countByDeviceId(String deviceId);
    
    long countByStatus(String status);
    
    long countByOperation(String operation);
}