package com.gui.kline.utils;

import javafx.scene.control.Alert;
import javafx.stage.Modality;
import javafx.stage.Window;

public class AlertUtil {
    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setStyle(
            "-fx-border-color: #16A34A;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 8;"
        );
        alert.showAndWait();
    }

    /**
     * Owner-aware variants. When an owner window is provided the alert is
     * parented to it (WINDOW_MODAL) so it appears centered on top of the
     * calling dialog instead of spawning a detached top-level window. This is
     * important when the application is in full-screen mode, where an unowned
     * alert can open in a separate window and leave the modal form stuck.
     */
    public static void showInfo(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        initOwner(alert, owner);
        alert.showAndWait();
    }

    public static void showError(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        initOwner(alert, owner);
        alert.showAndWait();
    }

    public static void showWarning(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        initOwner(alert, owner);
        alert.showAndWait();
    }

    public static void showSuccess(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setStyle(
            "-fx-border-color: #16A34A;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 8;"
        );
        initOwner(alert, owner);
        alert.showAndWait();
    }

    private static void initOwner(Alert alert, Window owner) {
        if (owner != null) {
            alert.initOwner(owner);
            alert.initModality(Modality.WINDOW_MODAL);
        }
    }
}