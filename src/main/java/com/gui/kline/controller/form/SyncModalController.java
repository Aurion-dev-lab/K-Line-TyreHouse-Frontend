package com.gui.kline.controller.form;

import com.gui.kline.data.SyncDataRepository;
import com.gui.kline.utils.BackgroundTask;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SyncModalController {

    @FXML private Label lblCount;
    @FXML private Label lblStatus;
    @FXML private Button btnSync;
    @FXML private TextArea txtLog;

    private SyncDataRepository syncRepository;
    private int totalPending;
    private int pendingRows;
    private int pendingDeletions;

    @FXML
    public void initialize() {
        syncRepository = new SyncDataRepository();
        txtLog.appendText("Checking for pending synchronizations...\n");
        loadSyncData();
    }

    private void loadSyncData() {
        BackgroundTask.runVoid(() -> {
            pendingRows = syncRepository.getTotalUnsyncedCount();
            pendingDeletions = syncRepository.getPendingDeletions().size();
            totalPending = pendingRows + pendingDeletions;

            Platform.runLater(() -> {
                lblCount.setText(String.valueOf(totalPending));
                if (totalPending == 0) {
                    lblStatus.setText("Local database is fully synchronized.");
                    btnSync.setDisable(true);
                    log("All data is up to date.");
                } else {
                    lblStatus.setText("Found " + pendingRows + " updates and " + pendingDeletions + " deletions pending.");
                    btnSync.setDisable(false);
                    log("Ready to synchronize " + totalPending + " total records.");
                }
            });
        }, null);
    }

    private void log(String message) {
        Platform.runLater(() -> {
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            txtLog.appendText("[" + time + "] " + message + "\n");
        });
    }

    @FXML
    private void onSyncClicked() {
        btnSync.setDisable(true);
        log("Compiling sync payload...");
        
        String syncInitiationTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String jsonPayload = syncRepository.getSyncPayloadAsJson();
        
        log("Connecting to server (http://localhost:8080/api/sync)...");

        BackgroundTask.runVoid(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/sync"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .build();

                log("Uploading " + jsonPayload.getBytes().length + " bytes of data...");
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    log("Server accepted payload (Status 200).");
                    log("Cleaning up local tombstones and marking records as synced...");
                    syncRepository.markAsSynced(syncInitiationTimestamp);
                    log("Sync successfully completed!");
                    
                    Platform.runLater(() -> {
                        lblCount.setText("0");
                        lblStatus.setText("Local database is fully synchronized.");
                    });
                } else {
                    log("ERROR: Server responded with status " + response.statusCode());
                    log("Response body: " + response.body());
                    Platform.runLater(() -> btnSync.setDisable(false));
                }
            } catch (Exception e) {
                log("ERROR: Failed to connect to server.");
                log("Exception details: " + e.getMessage());
                Platform.runLater(() -> btnSync.setDisable(false));
                e.printStackTrace();
            }
        }, null);
    }
}
