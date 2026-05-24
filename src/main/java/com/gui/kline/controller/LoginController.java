package com.gui.kline.controller;

import com.gui.kline.models.ViewModel;
import com.gui.kline.service.AuthService;
import com.gui.kline.utils.TokenManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button        loginButton;
    @FXML private Button        closeButton;
    @FXML private Label         errorLabel;
    @FXML private CheckBox      rememberMe;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        // Clear errors automatically as soon as the user starts typing again
        usernameField.textProperty().addListener((obs, oldVal, newVal) -> errorLabel.setText(""));
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> errorLabel.setText(""));

        // Allow pressing the 'Enter' key inside the password field to submit the form
        passwordField.setOnAction(this::handleLogin);
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Username and password are required.");
            return;
        }

        // 1. Enter UI processing state (disable controls to prevent double-submissions)
        setUiLoadingState(true);
        showError("Authenticating with system server...");

        // 2. Dispatch credentials via the async service abstraction layer
        authService.login(username, password, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(String accessToken) {
                // 3. Jump safely back to the JavaFX Application Thread before switching scenes
                Platform.runLater(() -> {
                    TokenManager.setAccessToken(accessToken);
                    showError(""); // Clear message banner

                    // Route to your dashboard main view container
                    ViewModel.INSTANCE.getViewsFactory().getView("main-layout");

                    // Close down the auxiliary login authentication window
                    handleClose(event);
                });
            }

            @Override
            public void onError(String errorMessage) {
                // 4. Return to UI thread to render error message and unlock fields
                Platform.runLater(() -> {
                    setUiLoadingState(false);
                    showError(errorMessage);
                });
            }
        });
    }

    @FXML
    public void handleClose(ActionEvent event) {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    private void showError(String message) {
        errorLabel.setText(message);
    }

    private void setUiLoadingState(boolean isLoading) {
        loginButton.setDisable(isLoading);
        usernameField.setDisable(isLoading);
        passwordField.setDisable(isLoading);
        rememberMe.setDisable(isLoading);
    }
}