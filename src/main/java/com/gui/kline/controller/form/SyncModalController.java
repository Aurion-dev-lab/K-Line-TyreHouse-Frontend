package com.gui.kline.controller.form;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gui.kline.data.SyncDataRepository;
import com.gui.kline.service.LocalRestoreService;
import com.gui.kline.utils.BackgroundTask;
import com.gui.kline.utils.SyncPreferences;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
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
import java.util.Optional;

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
    private Button btnDirectSyncRestore;
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

        lblSettingsStatus.setText("Revoking old key & generating new one...");
        lblSettingsStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #2563EB;");
        btnGenerateApiKey.setDisable(true);

        BackgroundTask.runVoid(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();

                // Step 1: Revoke old key if one exists in preferences
                String oldKey = SyncPreferences.getSyncApiKey();
                if (oldKey != null && !oldKey.isBlank()) {
                    String revokeUrl = SyncPreferences.getRevokeKeyUrl(oldKey);
                    log("Revoking existing API Key...");
                    try {
                        HttpRequest revokeRequest = HttpRequest.newBuilder()
                                .uri(URI.create(revokeUrl))
                                .DELETE()
                                .build();
                        HttpResponse<String> revokeResponse = client.send(revokeRequest, HttpResponse.BodyHandlers.ofString());
                        if (revokeResponse.statusCode() == 200) {
                            log("Old API Key revoked successfully.");
                        } else {
                            log("WARNING: Revoke returned status " + revokeResponse.statusCode() + " — proceeding to generate anyway.");
                        }
                    } catch (Exception revokeEx) {
                        log("WARNING: Could not revoke old key (" + revokeEx.getMessage() + ") — proceeding to generate anyway.");
                    }
                } else {
                    log("No existing API Key found — skipping revoke step.");
                }

                // Step 2: Generate new API key
                log("Requesting new API Key from server (" + generateKeyUrl + ")...");
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
    @FXML
    private void onDirectSyncRestoreClicked() {
        // ── Guard: require server URL ─────────────────────────────────────
        String urlInput = txtServerUrl.getText() != null ? txtServerUrl.getText().trim() : "";
        if (urlInput.isEmpty()) {
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("Missing Configuration");
            err.setHeaderText("Server Base URL is not set.");
            err.setContentText("Open Server Connection Settings, enter the Base URL and save before restoring.");
            err.showAndWait();
            return;
        }

        // ── Confirmation dialog ───────────────────────────────────────────
        Alert confirm = new Alert(Alert.AlertType.WARNING);
        confirm.setTitle("Restore from Cloud");
        confirm.setHeaderText("⚠️  This will permanently overwrite your local database!");
        confirm.setContentText(
                "All local records will be deleted and replaced with the cloud snapshot.\n" +
                "Any data that was never synced will be lost permanently.\n\n" +
                "Are you absolutely sure you want to continue?"
        );
        confirm.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            log("Restore cancelled by user.");
            return;
        }

        // ── UI lock ───────────────────────────────────────────────────────
        SyncPreferences.setSyncApiUrl(urlInput);
        String directPullUrl = SyncPreferences.getDirectPullUrl();
        String apiKey        = SyncPreferences.getSyncApiKey();

        btnDirectSyncRestore.setDisable(true);
        btnSync.setDisable(true);
        btnDirectSyncRestore.setText("Fetching Cloud Snapshot...");
        log("Starting cloud-to-local restore from " + directPullUrl + " ...");

        // ── Background worker (syncTask) ──────────────────────────────────
        BackgroundTask.runVoid(() -> {
            try {
                // Step 1: Fetch cloud snapshot
                log("Fetching cloud snapshot (GET " + directPullUrl + ")...");
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(directPullUrl))
                        .header("X-API-KEY", apiKey)
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    throw new RuntimeException("Server returned HTTP " + response.statusCode() +
                            ": " + response.body());
                }

                log("Snapshot received (" + response.body().getBytes().length + " bytes). Verifying payload...");

                // Step 2: Wipe local DB and restore from snapshot (atomic JDBC transaction)
                LocalRestoreService restoreService = new LocalRestoreService(this::log);
                restoreService.wipeAndRestore(response.body());

                // Step 3: Hook back to UI thread
                Platform.runLater(() -> {
                    btnDirectSyncRestore.setDisable(false);
                    btnDirectSyncRestore.setText("Restore from Cloud");
                    btnSync.setDisable(true);
                    lblCount.setText("0");
                    lblStatus.setText("Local database restored from cloud snapshot.");

                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("Restore Complete");
                    success.setHeaderText("Local database successfully restored.");
                    success.setContentText(
                            "All local tables have been wiped and repopulated from the cloud snapshot.\n" +
                            "Your local database is now a clean clone of the cloud."
                    );
                    success.showAndWait();
                });

            } catch (Exception ex) {
                log("ERROR during restore: " + ex.getMessage());
                ex.printStackTrace();
                Platform.runLater(() -> {
                    btnDirectSyncRestore.setDisable(false);
                    btnDirectSyncRestore.setText("Restore from Cloud");
                    btnSync.setDisable(false);

                    Alert error = new Alert(Alert.AlertType.ERROR);
                    error.setTitle("Restore Failed");
                    error.setHeaderText("Cloud restore encountered an error.");
                    error.setContentText(
                            "The restore was rolled back — your local data is intact.\n\n" +
                            "Error: " + ex.getMessage()
                    );
                    error.showAndWait();
                });
            }
        }, null);
    }
}
