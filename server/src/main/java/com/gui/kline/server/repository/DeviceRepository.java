package com.gui.kline.server.repository;

import com.gui.kline.server.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Device entity.
 */
@Repository
public interface DeviceRepository extends JpaRepository<Device, String>, JpaSpecificationExecutor<Device> {
    
    Optional<Device> findByDeviceId(String deviceId);
    
    Optional<Device> findByApiKey(String apiKey);
    
    List<Device> findByActive(boolean active);
    
    List<Device> findBySyncEnabled(boolean syncEnabled);
    
    List<Device> findByDeviceIdContainingIgnoreCase(String deviceId);
    
    List<Device> findByNameContainingIgnoreCase(String name);
    
    List<Device> findByShopNameContainingIgnoreCase(String shopName);
    
    List<Device> findByLastActiveAtAfter(LocalDateTime lastActiveAfter);
    
    List<Device> findByLastSyncAtAfter(LocalDateTime lastSyncAfter);
    
    @Query("SELECT d FROM Device d WHERE d.dailySyncCount < d.syncQuota ORDER BY d.dailySyncCount ASC")
    List<Device> findDevicesWithAvailableSyncQuota();
    
    @Query("SELECT COUNT(d) > 0 FROM Device d WHERE d.apiKey = :apiKey AND d.active = true")
    boolean existsByApiKeyAndActive(String apiKey);
    
    long countByActiveAndSyncEnabled(boolean active, boolean syncEnabled);
}