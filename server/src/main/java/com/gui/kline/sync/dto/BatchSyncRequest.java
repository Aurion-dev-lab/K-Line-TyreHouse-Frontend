package com.gui.kline.sync.dto;

import java.util.ArrayList;
import java.util.List;

public class BatchSyncRequest {
    private String deviceId;
    private List<SyncItemDto> items = new ArrayList<>();

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public List<SyncItemDto> getItems() {
        return items;
    }

    public void setItems(List<SyncItemDto> items) {
        this.items = items;
    }
}
