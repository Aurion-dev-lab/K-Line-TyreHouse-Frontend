package com.gui.kline.data;

public class SyncQueueItem {
    private final String id;
    private final String entityType;
    private final String payload;
    private final String status;

    public SyncQueueItem(String id, String entityType, String payload, String status) {
        this.id = id;
        this.entityType = entityType;
        this.payload = payload;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getPayload() {
        return payload;
    }

    public String getStatus() {
        return status;
    }
}

