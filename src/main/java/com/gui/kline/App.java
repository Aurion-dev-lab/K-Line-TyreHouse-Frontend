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
    public void start(Stage stage) throws IOException {
        try {
            DatabaseManager.init();
        } catch (RuntimeException ex) {
            AlertUtil.showError("Database error", ex.getMessage());
            Platform.exit();
            return;
        }
        ViewModel.INSTANCE.getViewsFactory().getView("main-layout");
    }

    public static void main(String[] args) {
        launch();
    }
}