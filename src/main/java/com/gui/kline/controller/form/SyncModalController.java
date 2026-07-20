package com.gui.kline.controller.form;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gui.kline.data.SyncDataRepository;
import com.gui.kline.utils.BackgroundTask;
import com.gui.kline.utils.SyncPreferences;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SyncModalController {

    @FXML
    private VBox vboxSettingsContent;
    @FXML
    private FontIcon iconSettingsChevron;

    @FXML
    private TextField txtServerUrl;
    @FXML
    private PasswordField pwdServerUrl;
    @FXML
    private FontIcon iconEyeUrl;

    @FXML
    private TextField txtApiKey;
    @FXML
    private PasswordField pwdApiKey;
    @FXML
    private FontIcon iconEyeKey;
    @FXML
    private Button btnGenerateApiKey;

    @FXML
    private Label lblSettingsStatus;
    @FXML
    private Button btnSaveSettings;

    @FXML
    private Label lblCount;
    @FXML
    private Label lblStatus;
    @FXML
    private Button btnSync;
    @FXML
    private TextArea txtLog;

    private SyncDataRepository syncRepository;
    private int totalPending;
    private int pendingRows;
    private int pendingDeletions;

    @FXML
    public void initialize() {
        syncRepository = new SyncDataRepository();

        // Bind text fields and password fields bidirectionally
        txtServerUrl.textProperty().bindBidirectional(pwdServerUrl.textProperty());
        txtApiKey.textProperty().bindBidirectional(pwdApiKey.textProperty());

        loadSettings();
        txtLog.appendText("Checking for pending synchronizations...\n");
        loadSyncData();
    }

    private void loadSettings() {
        String savedUrl = SyncPreferences.getBaseUrl();
        String savedKey = SyncPreferences.getSyncApiKey();
        txtServerUrl.setText(savedUrl);
        txtApiKey.setText(savedKey);
    }

    @FXML
    private void onToggleSettingsVisibility() {
        boolean show = !vboxSettingsContent.isVisible();
        vboxSettingsContent.setVisible(show);
        vboxSettingsContent.setManaged(show);
        iconSettingsChevron.setIconLiteral(show ? "fas-chevron-down" : "fas-chevron-right");
    }

    @FXML
    private void onToggleMaskUrl() {
        boolean isTextVisible = txtServerUrl.isVisible();
        if (isTextVisible) {
            txtServerUrl.setVisible(false);
            txtServerUrl.setManaged(false);
            pwdServerUrl.setVisible(true);
            pwdServerUrl.setManaged(true);
            iconEyeUrl.setIconLiteral("fas-eye-slash");
        } else {
            pwdServerUrl.setVisible(false);
            pwdServerUrl.setManaged(false);
            txtServerUrl.setVisible(true);
            txtServerUrl.setManaged(true);
            iconEyeUrl.setIconLiteral("fas-eye");
        }
    }

    @FXML
    private void onToggleMaskKey() {
        boolean isTextVisible = txtApiKey.isVisible();
        if (isTextVisible) {
            txtApiKey.setVisible(false);
            txtApiKey.setManaged(false);
            pwdApiKey.setVisible(true);
            pwdApiKey.setManaged(true);
            iconEyeKey.setIconLiteral("fas-eye-slash");
        } else {
            pwdApiKey.setVisible(false);
            pwdApiKey.setManaged(false);
            txtApiKey.setVisible(true);
            txtApiKey.setManaged(true);
            iconEyeKey.setIconLiteral("fas-eye");
        }
    }

    @FXML
    private void onSaveSettingsClicked() {
        String url = txtServerUrl.getText() != null ? txtServerUrl.getText().trim() : "";
        String key = txtApiKey.getText() != null ? txtApiKey.getText().trim() : "";

        if (url.isEmpty()) {
            lblSettingsStatus.setText("Server URL cannot be empty");
            lblSettingsStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #DC2626;");
            return;
        }

        SyncPreferences.saveSyncSettings(url, key);
        lblSettingsStatus.setText("Settings saved successfully!");
        lblSettingsStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #16A34A;");
        log("Server connection settings updated and saved to preferences.");
    }

    @FXML
    private void onGenerateApiKeyClicked() {
        String urlInput = txtServerUrl.getText() != null ? txtServerUrl.getText().trim() : "";
        if (urlInput.isEmpty()) {
            lblSettingsStatus.setText("Enter Server Base URL first");
            lblSettingsStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #DC2626;");
            return;
        }

        // Save current base URL input first
        SyncPreferences.setSyncApiUrl(urlInput);
        String generateKeyUrl = SyncPreferences.getGenerateKeyUrl();

        lblSettingsStatus.setText("Generating API Key...");
        lblSettingsStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #2563EB;");
        btnGenerateApiKey.setDisable(true);

        log("Requesting new API Key from server (" + generateKeyUrl + ")...");

        BackgroundTask.runVoid(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(generateKeyUrl))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200 || response.statusCode() == 201) {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(response.body());
                    String newKey = root.path("data").path("api_key").asText();

                    if (newKey != null && !newKey.isBlank()) {
                        SyncPreferences.setSyncApiKey(newKey);
                        Platform.runLater(() -> {
                            txtApiKey.setText(newKey);
                            lblSettingsStatus.setText("New API Key generated & saved!");
                            lblSettingsStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #16A34A;");
                            btnGenerateApiKey.setDisable(false);
                        });
                        log("Successfully generated and saved new API Key.");
                    } else {
                        Platform.runLater(() -> {
                            lblSettingsStatus.setText("API Key missing in server response");
                            lblSettingsStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #DC2626;");
                            btnGenerateApiKey.setDisable(false);
                        });
                        log("ERROR: API key field missing in response payload.");
                    }
                } else {
                    Platform.runLater(() -> {
                        lblSettingsStatus.setText("Failed: HTTP Status " + response.statusCode());
                        lblSettingsStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #DC2626;");
                        btnGenerateApiKey.setDisable(false);
                    });
                    log("ERROR: Key generation server returned status " + response.statusCode());
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblSettingsStatus.setText("Connection failed for key generation");
                    lblSettingsStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #DC2626;");
                    btnGenerateApiKey.setDisable(false);
                });
                log("ERROR: Failed to connect to server for key generation.");
                log("Exception details: " + e.getMessage());
                e.printStackTrace();
            }
        }, null);
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
                    lblStatus.setText(
                            "Found " + pendingRows + " updates and " + pendingDeletions + " deletions pending.");
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

        String apiUrl = SyncPreferences.getSyncApiUrl();
        String apiKey = SyncPreferences.getSyncApiKey();

        log("Connecting to server (" + apiUrl + ")...");

        BackgroundTask.runVoid(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .header("Content-Type", "application/json")
                        .header("X-API-KEY", apiKey)
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
