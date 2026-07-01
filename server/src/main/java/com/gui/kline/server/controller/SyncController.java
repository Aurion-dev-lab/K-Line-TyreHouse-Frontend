package com.gui.kline.server.controller;

import com.gui.kline.server.dto.ApiResponse;
import com.gui.kline.server.service.DeviceService;
import com.gui.kline.server.service.SyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * REST Controller for handling data synchronization between client devices and server.
 * This controller provides endpoints for uploading and downloading data.
 */
@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Sync Operations", description = "Endpoints for data synchronization between devices and server")
public class SyncController {
    
    private final SyncService syncService;
    private final DeviceService deviceService;
    
    /**
     * Upload data from a device to the server
     * This is the main endpoint that will be called when the user clicks the upload button
     */
    @Operation(summary = "Upload data from device", description = "Upload all modified data from a device to the server")
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<SyncService.SyncResponse>> uploadData(
            @Valid @RequestBody SyncService.SyncRequest syncRequest) {
        
        log.info("Received sync upload request from device: {}", syncRequest.getDeviceId());
        
        // Validate device
        if (syncRequest.getDeviceId() == null || syncRequest.getDeviceId().isBlank()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Device ID is required")
            );
        }
        
        // Check if device exists and is active
        try {
            boolean deviceValid = deviceService.validateDeviceApiKey(syncRequest.getDeviceId());
            if (!deviceValid) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiResponse.error("Invalid or inactive device")
                );
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiResponse.error("Device validation failed: " + e.getMessage())
            );
        }
        
        // Process the sync upload
        SyncService.SyncResponse syncResponse = syncService.uploadData(syncRequest);
        
        if (syncResponse.isSuccess()) {
            log.info("Sync upload completed successfully for device: {}", syncRequest.getDeviceId());
            return ResponseEntity.ok(ApiResponse.success(syncResponse, 
                String.format("Sync completed: %d/%d items uploaded successfully", 
                    syncResponse.getSuccessCount(), syncResponse.getTotalItems())));
        } else {
            log.warn("Sync upload completed with errors for device: {}", syncRequest.getDeviceId());
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(
                ApiResponse.success(syncResponse, syncResponse.getMessage())
            );
        }
    }
    
    /**
     * Download data for a device (incremental sync)
     */
    @Operation(summary = "Download incremental data", description = "Download data that has been modified since last sync")
    @GetMapping("/download/incremental")
    public ResponseEntity<ApiResponse<SyncService.SyncResponse>> downloadIncrementalData(
            @RequestParam String deviceId,
            @RequestParam(required = false) Long lastSyncTimestamp) {
        
        log.info("Received incremental sync download request from device: {}", deviceId);
        
        LocalDateTime lastSyncTime = null;
        if (lastSyncTimestamp != null) {
            lastSyncTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(lastSyncTimestamp), 
                java.time.ZoneId.systemDefault()
            );
        }
        
        SyncService.SyncResponse syncResponse = syncService.downloadData(deviceId, lastSyncTime);
        
        if (syncResponse.isSuccess()) {
            log.info("Incremental sync download completed for device: {}", deviceId);
            return ResponseEntity.ok(ApiResponse.success(syncResponse, syncResponse.getMessage()));
        } else {
            log.warn("Incremental sync download failed for device: {}", deviceId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(syncResponse.getMessage())
            );
        }
    }
    
    /**
     * Get full sync data for a device
     */
    @Operation(summary = "Download full data", description = "Download all data for a device (initial sync)")
    @GetMapping("/download/full")
    public ResponseEntity<ApiResponse<SyncService.SyncResponse>> downloadFullData(
            @RequestParam String deviceId) {
        
        log.info("Received full sync download request from device: {}", deviceId);
        
        SyncService.SyncResponse syncResponse = syncService.getFullSyncData(deviceId);
        
        if (syncResponse.isSuccess()) {
            log.info("Full sync download completed for device: {}", deviceId);
            return ResponseEntity.ok(ApiResponse.success(syncResponse, syncResponse.getMessage()));
        } else {
            log.warn("Full sync download failed for device: {}", deviceId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(syncResponse.getMessage())
            );
        }
    }
    
    /**
     * Check sync status for a device
     */
    @Operation(summary = "Check sync status", description = "Check the current sync status and statistics for a device")
    @GetMapping("/status/{deviceId}")
    public ResponseEntity<ApiResponse<Object>> checkSyncStatus(@PathVariable String deviceId) {
        
        try {
            Object stats = deviceService.getDeviceSyncStats(deviceId);
            return ResponseEntity.ok(ApiResponse.success(stats, "Sync status retrieved successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.error("Device not found or error: " + e.getMessage())
            );
        }
    }
    
    /**
     * Sync ping endpoint - simple endpoint to check if server is available
     */
    @Operation(summary = "Ping sync server", description = "Simple ping endpoint to check server availability")
    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<String>> ping() {
        return ResponseEntity.ok(ApiResponse.success("pong", "Sync server is available"));
    }
    
    /**
     * Get sync statistics for all devices (admin endpoint)
     */
    @Operation(summary = "Get sync statistics", description = "Get sync statistics for all devices")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Object>> getSyncStatistics() {
        try {
            // Implementation would return comprehensive sync statistics
            // For now, return a simple response
            return ResponseEntity.ok(ApiResponse.success(
                Map.of("message", "Sync statistics endpoint"), 
                "Sync statistics retrieved successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error("Failed to retrieve sync statistics: " + e.getMessage())
            );
        }
    }
    
    /**
     * Force sync for a specific device (admin endpoint)
     */
    @Operation(summary = "Force sync", description = "Force sync for a specific device")
    @PostMapping("/force/{deviceId}")
    public ResponseEntity<ApiResponse<String>> forceSync(@PathVariable String deviceId) {
        log.info("Forcing sync for device: {}", deviceId);
        
        try {
            // Implementation would trigger a force sync
            return ResponseEntity.ok(ApiResponse.success(
                "Force sync triggered for device: " + deviceId, 
                "Sync forced successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error("Failed to force sync: " + e.getMessage())
            );
        }
    }
}