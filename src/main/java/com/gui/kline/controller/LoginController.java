package com.gui.kline.controller;

import com.gui.kline.models.ViewModel;
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

    @FXML
    public void initialize() {
        usernameField.textProperty().addListener((obs, o, n) -> errorLabel.setText(""));
        passwordField.textProperty().addListener((obs, o, n) -> errorLabel.setText(""));

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

        if (username.equals("admin") && password.equals("admin")) {
            showError("");
            ViewModel.INSTANCE.getViewsFactory().getView("main-layout");
            handleClose(event);
        } else {
            showError("Invalid username or password.");
        }
    }

    @FXML
    public void handleClose(ActionEvent event) {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    private void showError(String message) {
        errorLabel.setText(message);
    }
}