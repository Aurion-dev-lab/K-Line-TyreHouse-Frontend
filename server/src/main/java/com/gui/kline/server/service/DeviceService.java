package com.gui.kline.server.service;

import com.gui.kline.server.dto.DeviceDTO;
import com.gui.kline.server.entity.Device;
import com.gui.kline.server.exception.ResourceNotFoundException;
import com.gui.kline.server.repository.DeviceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class for Device entity operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceService {
    
    private final DeviceRepository deviceRepository;
    private final ModelMapper modelMapper;
    
    private final UUIDGeneratorService uuidGeneratorService;
    
    /**
     * Register a new device
     */
    @Transactional
    public DeviceDTO registerDevice(DeviceDTO deviceDTO) {
        log.info("Registering new device: {}", deviceDTO.getName());
        
        // Generate unique IDs
        String id = uuidGeneratorService.generateId();
        String apiKey = uuidGeneratorService.generateApiKey();
        
        Device device = modelMapper.map(deviceDTO, Device.class);
        device.setId(id);
        device.setDeviceId(deviceDTO.getDeviceId() != null ? deviceDTO.getDeviceId() : id);
        device.setApiKey(apiKey);
        device.setActive(true);
        device.setSyncEnabled(true);
        device.setCreatedAt(LocalDateTime.now());
        device.setUpdatedAt(LocalDateTime.now());
        
        Device savedDevice = deviceRepository.save(device);
        
        DeviceDTO result = modelMapper.map(savedDevice, DeviceDTO.class);
        result.setApiKey(apiKey);
        
        log.info("Device registered successfully: {}", savedDevice.getId());
        return result;
    }
    
    /**
     * Get device by ID
     */
    public DeviceDTO getDeviceById(String deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with id: " + deviceId));
        return modelMapper.map(device, DeviceDTO.class);
    }
    
    /**
     * Get device by API key
     */
    public DeviceDTO getDeviceByApiKey(String apiKey) {
        Device device = deviceRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with API key: " + apiKey));
        return modelMapper.map(device, DeviceDTO.class);
    }
    
    /**
     * Get all devices
     */
    public List<DeviceDTO> getAllDevices() {
        return deviceRepository.findAll().stream()
                .map(device -> modelMapper.map(device, DeviceDTO.class))
                .collect(Collectors.toList());
    }
    
    /**
     * Update device
     */
    @Transactional
    public DeviceDTO updateDevice(String deviceId, DeviceDTO deviceDTO) {
        Device existingDevice = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with id: " + deviceId));
        
        modelMapper.map(deviceDTO, existingDevice);
        existingDevice.setUpdatedAt(LocalDateTime.now());
        
        Device updatedDevice = deviceRepository.save(existingDevice);
        return modelMapper.map(updatedDevice, DeviceDTO.class);
    }
    
    /**
     * Deactivate device
     */
    @Transactional
    public void deactivateDevice(String deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with id: " + deviceId));
        
        device.setActive(false);
        device.setUpdatedAt(LocalDateTime.now());
        deviceRepository.save(device);
        
        log.info("Device deactivated: {}", deviceId);
    }
    
    /**
     * Activate device
     */
    @Transactional
    public void activateDevice(String deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with id: " + deviceId));
        
        device.setActive(true);
        device.setUpdatedAt(LocalDateTime.now());
        deviceRepository.save(device);
        
        log.info("Device activated: {}", deviceId);
    }
    
    /**
     * Generate new API key for device
     */
    @Transactional
    public DeviceDTO generateApiKey(String deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with id: " + deviceId));
        
        String newApiKey = uuidGeneratorService.generateApiKey();
        device.setApiKey(newApiKey);
        device.setUpdatedAt(LocalDateTime.now());
        
        Device updatedDevice = deviceRepository.save(device);
        DeviceDTO result = modelMapper.map(updatedDevice, DeviceDTO.class);
        result.setApiKey(newApiKey);
        
        return result;
    }
    
    /**
     * Update last sync timestamp
     */
    @Transactional
    public void updateLastSync(String deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with id: " + deviceId));
        
        device.setLastSyncAt(LocalDateTime.now());
        device.setTotalSyncs(device.getTotalSyncs() + 1);
        device.setSuccessfulSyncs(device.getSuccessfulSyncs() + 1);
        device.setDailySyncCount(device.getDailySyncCount() + 1);
        device.setUpdatedAt(LocalDateTime.now());
        
        deviceRepository.save(device);
        log.info("Last sync updated for device: {}", deviceId);
    }
    
    /**
     * Update device activity
     */
    @Transactional
    public void updateActivity(String deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with id: " + deviceId));
        
        device.setLastActiveAt(LocalDateTime.now());
        device.setUpdatedAt(LocalDateTime.now());
        
        deviceRepository.save(device);
        log.info("Activity updated for device: {}", deviceId);
    }
    
    /**
     * Delete device
     */
    @Transactional
    public void deleteDevice(String deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with id: " + deviceId));
        
        deviceRepository.delete(device);
        log.info("Device deleted: {}", deviceId);
    }
    
    /**
     * Get device sync statistics
     */
    public Map<String, Object> getDeviceSyncStats(String deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with id: " + deviceId));
        
        return Map.of(
            "totalSyncs", device.getTotalSyncs(),
            "successfulSyncs", device.getSuccessfulSyncs(),
            "failedSyncs", device.getFailedSyncs(),
            "syncQuota", device.getSyncQuota(),
            "dailySyncCount", device.getDailySyncCount(),
            "lastSyncAt", device.getLastSyncAt(),
            "lastActiveAt", device.getLastActiveAt()
        );
    }
    
    /**
     * Validate device API key
     */
    public boolean validateDeviceApiKey(String apiKey) {
        return deviceRepository.existsByApiKeyAndActive(apiKey);
    }
    
    /**
     * Get device by deviceId (not entity ID)
     */
    public DeviceDTO getDeviceByDeviceId(String deviceId) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with deviceId: " + deviceId));
        return modelMapper.map(device, DeviceDTO.class);
    }
    
    /**
     * Increment failed sync count
     */
    @Transactional
    public void incrementFailedSyncs(String deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with id: " + deviceId));
        
        device.setFailedSyncs(device.getFailedSyncs() + 1);
        device.setTotalSyncs(device.getTotalSyncs() + 1);
        device.setUpdatedAt(LocalDateTime.now());
        
        deviceRepository.save(device);
    }
}