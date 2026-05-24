package com.gui.kline.sync.dto;

import com.fasterxml.jackson.databind.JsonNode;

public class SyncItemDto {
    private String id;
    private String entityType;
    private JsonNode payload;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public void setPayload(JsonNode payload) {
        this.payload = payload;
    }
}

