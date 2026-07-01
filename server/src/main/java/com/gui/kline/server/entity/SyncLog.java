package com.gui.kline.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Sync log entity that tracks all synchronization operations between client devices and the server.
 * This is the main entity that stores all synced data and tracks sync status.
 */
@Entity
@Table(name = "sync_logs", indexes = {
    @Index(name = "idx_sync_device", columnList = "deviceId"),
    @Index(name = "idx_sync_entity", columnList = "entityType"),
    @Index(name = "idx_sync_received", columnList = "receivedAt"),
    @Index(name = "idx_sync_status", columnList = "syncStatus")
})
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SyncLog extends BaseEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String deviceId;

    @Column
    private String deviceName;

    @Column(nullable = false)
    private String entityType; // Type of entity being synced (product, invoice, service, etc.)

    @Column(nullable = false)
    private String entityId; // ID of the entity in the client database

    @Column
    private String serverEntityId; // ID of the entity in the server database (if different)

    @Column(nullable = false)
    private SyncOperation operation = SyncOperation.CREATE;

    @Column
    private Instant receivedAt;

    @Column
    private LocalDateTime processedAt;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String payload; // JSON payload of the entity data

    @Column
    private String errorMessage;

    @Column(nullable = false)
    private SyncStatus syncStatus = SyncStatus.PENDING;

    @Column
    private int retryCount = 0;

    @Column
    private String lastError;

    @Column
    private String processedBy;

    @Column
    private LocalDateTime completedAt;

    @Column
    private String syncBatchId; // Group multiple syncs together

    @Column
    private int batchSequence = 0;

    public SyncLog(String id, String deviceId, String entityType, String entityId, 
                  SyncOperation operation, String payload) {
        this.id = id;
        this.deviceId = deviceId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.operation = operation;
        this.payload = payload;
        this.receivedAt = Instant.now();
        this.syncStatus = SyncStatus.PENDING;
    }

    public enum SyncOperation {
        CREATE, UPDATE, DELETE, SYNC_REQUEST, PING
    }

    public enum SyncStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, CONFLICT, DUPLICATE, SKIPPED
    }

    public void markAsProcessing() {
        this.syncStatus = SyncStatus.PROCESSING;
        this.processedAt = LocalDateTime.now();
    }

    public void markAsCompleted() {
        this.syncStatus = SyncStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void markAsFailed(String errorMessage) {
        this.syncStatus = SyncStatus.FAILED;
        this.errorMessage = errorMessage;
        this.lastError = errorMessage;
        this.retryCount++;
    }

    public void markAsDuplicate() {
        this.syncStatus = SyncStatus.DUPLICATE;
        this.errorMessage = "Duplicate entity already exists";
    }

    public void markAsConflict() {
        this.syncStatus = SyncStatus.CONFLICT;
        this.errorMessage = "Conflict with existing data";
    }

    public boolean isSuccessful() {
        return this.syncStatus == SyncStatus.COMPLETED;
    }

    public boolean needsRetry() {
        return this.syncStatus == SyncStatus.FAILED && this.retryCount < 3;
    }

    public boolean isProcessed() {
        return this.syncStatus == SyncStatus.COMPLETED || 
               this.syncStatus == SyncStatus.FAILED ||
               this.syncStatus == SyncStatus.DUPLICATE ||
               this.syncStatus == SyncStatus.CONFLICT ||
               this.syncStatus == SyncStatus.SKIPPED;
    }
}