package com.gui.kline.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Represents a client device that can sync data with the server.
 * Each device has a unique ID and can have specific permissions and quotas.
 */
@Entity
@Table(name = "devices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Device extends BaseEntity {

    @Id
    private String id;

    @Column(unique = true, nullable = false)
    private String deviceId;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String apiKey;

    @Column
    private String shopName;

    @Column
    private String location;

    @Column
    private String contactPhone;

    @Column
    private String contactEmail;

    @Column
    private String address;

    @Column
    private boolean active = true;

    @Column
    private boolean syncEnabled = true;

    @Column
    private long syncQuota = 10000; // Max sync items per day

    @Column
    private long dailySyncCount = 0;

    @Column
    private LocalDateTime lastSyncAt;

    @Column
    private LocalDateTime lastActiveAt;

    @Column
    private String ipAddress;

    @Column
    private String macAddress;

    // Sync statistics
    @Column
    private long totalSyncs = 0;

    @Column
    private long successfulSyncs = 0;

    @Column
    private long failedSyncs = 0;

    public Device(String id, String deviceId, String name, String apiKey) {
        this.id = id;
        this.deviceId = deviceId;
        this.name = name;
        this.apiKey = apiKey;
        this.active = true;
        this.syncEnabled = true;
    }
}