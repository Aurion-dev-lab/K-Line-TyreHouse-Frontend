package com.gui.kline.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for SyncLog entity.
 */
@Data
@NoArgsConstructor
public class SyncLogDTO {
    
    private String id;
    
    @NotBlank(message = "Device ID is required")
    @Size(max = 100)
    private String deviceId;
    
    @Size(max = 200)
    private String deviceName;
    
    @Size(max = 50)
    private String syncType = "FULL"; // FULL, PARTIAL, INCREMENTAL, MANUAL
    
    @Size(max = 50)
    private String operation = "UPLOAD"; // UPLOAD, DOWNLOAD, BIDIRECTIONAL
    
    @Size(max = 50)
    private String status = "STARTED"; // STARTED, IN_PROGRESS, COMPLETED, FAILED, PARTIAL
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;
    
    private long itemsToSync = 0;
    
    private long itemsSynced = 0;
    
    private long itemsFailed = 0;
    
    @Size(max = 1000)
    private String errorMessage;
    
    @Size(max = 2000)
    private String details;
    
    @Size(max = 500)
    private String serverResponse;
    
    private String requestPayload;
    
    private String responsePayload;
    
    private String syncId;
    
    private LocalDateTime syncedAt;
    
    private boolean syncStatus = false;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    // Calculated fields
    private Long durationSeconds;
    
    private Double successRate;
    
    public SyncLogDTO(String id, String deviceId, String operation, String syncType) {
        this.id = id;
        this.deviceId = deviceId;
        this.operation = operation;
        this.syncType = syncType;
        this.status = "STARTED";
        this.startTime = LocalDateTime.now();
    }
    
    public enum SyncType {
        FULL, PARTIAL, INCREMENTAL, MANUAL, SCHEDULED, ON_DEMAND
    }
    
    public enum Operation {
        UPLOAD, DOWNLOAD, BIDIRECTIONAL, SYNC_STATUS, PING
    }
    
    public enum Status {
        STARTED, IN_PROGRESS, COMPLETED, FAILED, PARTIAL, CANCELLED, TIMEOUT
    }
}