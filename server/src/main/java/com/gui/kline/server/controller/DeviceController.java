package com.gui.kline.server.controller;

import com.gui.kline.server.dto.DeviceDTO;
import com.gui.kline.server.dto.ApiResponse;
import com.gui.kline.server.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing devices that can sync data with the server.
 */
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@Tag(name = "Device Management", description = "Operations for managing client devices")
public class DeviceController {

    private final DeviceService deviceService;

    @Operation(summary = "Register a new device", description = "Register a new client device with the server")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<DeviceDTO>> registerDevice(
            @Valid @RequestBody DeviceDTO deviceDTO) {
        DeviceDTO registeredDevice = deviceService.registerDevice(deviceDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(registeredDevice, "Device registered successfully"));
    }

    @Operation(summary = "Get device by ID", description = "Retrieve device details by device ID")
    @GetMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<DeviceDTO>> getDeviceById(@PathVariable String deviceId) {
        DeviceDTO device = deviceService.getDeviceById(deviceId);
        return ResponseEntity.ok(ApiResponse.success(device));
    }

    @Operation(summary = "Get device by API key", description = "Retrieve device details by API key")
    @GetMapping("/by-api-key/{apiKey}")
    public ResponseEntity<ApiResponse<DeviceDTO>> getDeviceByApiKey(@PathVariable String apiKey) {
        DeviceDTO device = deviceService.getDeviceByApiKey(apiKey);
        return ResponseEntity.ok(ApiResponse.success(device));
    }

    @Operation(summary = "Get all devices", description = "Retrieve all registered devices")
    @GetMapping("/")
    public ResponseEntity<ApiResponse<List<DeviceDTO>>> getAllDevices() {
        List<DeviceDTO> devices = deviceService.getAllDevices();
        return ResponseEntity.ok(ApiResponse.success(devices));
    }

    @Operation(summary = "Update device", description = "Update device information")
    @PutMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<DeviceDTO>> updateDevice(
            @PathVariable String deviceId, 
            @Valid @RequestBody DeviceDTO deviceDTO) {
        DeviceDTO updatedDevice = deviceService.updateDevice(deviceId, deviceDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedDevice, "Device updated successfully"));
    }

    @Operation(summary = "Deactivate device", description = "Deactivate a device to prevent further syncs")
    @PutMapping("/{deviceId}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateDevice(@PathVariable String deviceId) {
        deviceService.deactivateDevice(deviceId);
        return ResponseEntity.ok(ApiResponse.success(null, "Device deactivated successfully"));
    }

    @Operation(summary = "Activate device", description = "Reactivate a previously deactivated device")
    @PutMapping("/{deviceId}/activate")
    public ResponseEntity<ApiResponse<Void>> activateDevice(@PathVariable String deviceId) {
        deviceService.activateDevice(deviceId);
        return ResponseEntity.ok(ApiResponse.success(null, "Device activated successfully"));
    }

    @Operation(summary = "Generate new API key", description = "Generate a new API key for a device")
    @PutMapping("/{deviceId}/generate-api-key")
    public ResponseEntity<ApiResponse<DeviceDTO>> generateApiKey(@PathVariable String deviceId) {
        DeviceDTO device = deviceService.generateApiKey(deviceId);
        return ResponseEntity.ok(ApiResponse.success(device, "New API key generated successfully"));
    }

    @Operation(summary = "Update device last sync", description = "Update the last sync timestamp for a device")
    @PutMapping("/{deviceId}/update-last-sync")
    public ResponseEntity<ApiResponse<Void>> updateLastSync(@PathVariable String deviceId) {
        deviceService.updateLastSync(deviceId);
        return ResponseEntity.ok(ApiResponse.success(null, "Last sync updated successfully"));
    }

    @Operation(summary = "Update device activity", description = "Update the last activity timestamp for a device")
    @PutMapping("/{deviceId}/update-activity")
    public ResponseEntity<ApiResponse<Void>> updateActivity(@PathVariable String deviceId) {
        deviceService.updateActivity(deviceId);
        return ResponseEntity.ok(ApiResponse.success(null, "Activity updated successfully"));
    }

    @Operation(summary = "Delete device", description = "Permanently delete a device from the system")
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<Void>> deleteDevice(@PathVariable String deviceId) {
        deviceService.deleteDevice(deviceId);
        return ResponseEntity.ok(ApiResponse.success(null, "Device deleted successfully"));
    }

    @Operation(summary = "Get device sync statistics", description = "Get sync statistics for a specific device")
    @GetMapping("/{deviceId}/stats")
    public ResponseEntity<ApiResponse<Object>> getDeviceSyncStats(@PathVariable String deviceId) {
        Object stats = deviceService.getDeviceSyncStats(deviceId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}