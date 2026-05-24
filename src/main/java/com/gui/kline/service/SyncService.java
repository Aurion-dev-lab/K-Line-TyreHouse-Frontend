package com.gui.kline.service;

import com.gui.kline.data.DatabaseManager;
import com.gui.kline.data.SyncQueueItem;
import com.gui.kline.data.SyncQueueRepository;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

public class SyncService {
    private final SyncQueueRepository repository = new SyncQueueRepository();
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public SyncResult syncPending() {
        List<SyncQueueItem> items = repository.findPending(250);
        if (items.isEmpty()) {
            return new SyncResult(0, 0, "No pending records");
        }

        String deviceId = DatabaseManager.getDeviceId();
        String payload = buildPayload(items, deviceId);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(SyncConfig.getSyncUrl()))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("X-API-KEY", SyncConfig.getApiKey());
        HttpRequest request = builder.POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                repository.markSynced(items);
                return new SyncResult(items.size(), 0, "Uploaded " + items.size() + " records");
            }
            repository.markFailed(items, response.body());
            return new SyncResult(0, items.size(), "Upload failed: " + response.statusCode());
        } catch (Exception ex) {
            repository.markFailed(items, ex.getMessage());
            return new SyncResult(0, items.size(), "Upload failed: " + ex.getMessage());
        }
    }

    private String buildPayload(List<SyncQueueItem> items, String deviceId) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"deviceId\":\"").append(escapeJson(deviceId)).append("\",");
        sb.append("\"items\":[");
        for (int i = 0; i < items.size(); i++) {
            SyncQueueItem item = items.get(i);
            sb.append("{");
            sb.append("\"id\":\"").append(escapeJson(item.getId())).append("\",");
            sb.append("\"entityType\":\"").append(escapeJson(item.getEntityType())).append("\",");
            sb.append("\"payload\":").append(item.getPayload() == null ? "null" : item.getPayload());
            sb.append("}");
            if (i < items.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]}");
        return sb.toString();
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static class SyncResult {
        private final int synced;
        private final int failed;
        private final String message;

        public SyncResult(int synced, int failed, String message) {
            this.synced = synced;
            this.failed = failed;
            this.message = message;
        }

        public int getSynced() {
            return synced;
        }

        public int getFailed() {
            return failed;
        }

        public String getMessage() {
            return message;
        }
    }
}
