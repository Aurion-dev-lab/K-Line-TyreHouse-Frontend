package com.gui.kline.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Device entity.
 */
@Data
@NoArgsConstructor
public class DeviceDTO {
    
    private String id;
    
    @NotBlank(message = "Device ID is required")
    @Size(max = 100)
    private String deviceId;
    
    @NotBlank(message = "Device name is required")
    @Size(max = 200)
    private String name;
    
    @Size(max = 255)
    private String apiKey;
    
    @Size(max = 200)
    private String shopName;
    
    @Size(max = 200)
    private String location;
    
    @Size(max = 50)
    private String contactPhone;
    
    @Size(max = 200)
    private String contactEmail;
    
    @Size(max = 500)
    private String address;
    
    private boolean active = true;
    
    private boolean syncEnabled = true;
    
    private long syncQuota = 10000;
    
    private long dailySyncCount = 0;
    
    private LocalDateTime lastSyncAt;
    
    private LocalDateTime lastActiveAt;
    
    @Size(max = 50)
    private String ipAddress;
    
    @Size(max = 50)
    private String macAddress;
    
    private long totalSyncs = 0;
    
    private long successfulSyncs = 0;
    
    private long failedSyncs = 0;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    public DeviceDTO(String id, String deviceId, String name, String apiKey) {
        this.id = id;
        this.deviceId = deviceId;
        this.name = name;
        this.apiKey = apiKey;
        this.active = true;
        this.syncEnabled = true;
    }
}