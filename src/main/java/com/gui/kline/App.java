package com.gui.kline;

import com.gui.kline.data.DatabaseManager;
import com.gui.kline.models.ViewModel;
import com.gui.kline.utils.AlertUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {
    @Override
    public void start(Stage primaryStage) throws IOException {
        try {
            DatabaseManager.init();
        } catch (RuntimeException ex) {
            AlertUtil.showError("Database error", ex.getMessage());
            Platform.exit();
            return;
        }
        
        // Store the primary stage for proper window management
        ViewModel.INSTANCE.getViewsFactory().setPrimaryStage(primaryStage);

        // Load the main layout (this reuses the primary stage as the visible window)
        ViewModel.INSTANCE.getViewsFactory().getView("main-layout");

        // Only exit the application when the main window is closed, not when a
        // transient dialog/alert is dismissed. Without this, closing an unowned
        // alert can terminate the whole application.
        Platform.setImplicitExit(false);
        primaryStage.setOnCloseRequest(e -> Platform.exit());
    }

    public static void main(String[] args) {
        launch();
    }
}