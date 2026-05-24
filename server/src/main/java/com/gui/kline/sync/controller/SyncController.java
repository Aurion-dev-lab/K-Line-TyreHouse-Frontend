package com.gui.kline.sync.controller;

import com.gui.kline.sync.dto.BatchSyncRequest;
import com.gui.kline.sync.dto.BatchSyncResponse;
import com.gui.kline.sync.dto.SyncItemDto;
import com.gui.kline.sync.model.SyncedItem;
import com.gui.kline.sync.repo.SyncedItemRepository;
import com.gui.kline.sync.service.SyncPersistService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/sync")
public class SyncController {
    private final SyncedItemRepository repository;
    private final SyncPersistService persistService;

    @Value("${sync.api-key}")
    private String apiKey;

    public SyncController(SyncedItemRepository repository, SyncPersistService persistService) {
        this.repository = repository;
        this.persistService = persistService;
    }

    @PostMapping("/batch")
    public ResponseEntity<BatchSyncResponse> uploadBatch(
            @RequestBody BatchSyncRequest request,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-API-KEY", required = false) String providedKey
    ) {
        if (apiKey != null && !apiKey.isBlank() && !apiKey.equals(providedKey)) {
            return ResponseEntity.status(401).body(new BatchSyncResponse(0, 0, List.of("Unauthorized")));
        }

        if (request.getDeviceId() == null || request.getDeviceId().isBlank()) {
            return ResponseEntity.badRequest().body(new BatchSyncResponse(0, 0, List.of("Missing deviceId")));
        }

        int accepted = 0;
        int duplicate = 0;
        List<String> errors = new ArrayList<>();

        for (SyncItemDto item : request.getItems()) {
            if (item.getId() == null || item.getId().isBlank()) {
                errors.add("Missing id");
                continue;
            }
            if (item.getEntityType() == null || item.getEntityType().isBlank()) {
                errors.add("Missing entityType for id " + item.getId());
                continue;
            }
            if (repository.existsById(item.getId())) {
                duplicate++;
                continue;
            }
            String payloadJson = item.getPayload() == null ? "{}" : item.getPayload().toString();
            SyncedItem entity = new SyncedItem(item.getId(), request.getDeviceId(), item.getEntityType(), payloadJson, Instant.now());
            repository.save(entity);
            persistService.persist(item, request.getDeviceId());
            accepted++;
        }

        return ResponseEntity.ok(new BatchSyncResponse(accepted, duplicate, errors));
    }
}
