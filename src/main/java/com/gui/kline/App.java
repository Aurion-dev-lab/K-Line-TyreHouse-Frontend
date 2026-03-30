package com.gui.kline;

import com.gui.kline.models.ViewModel;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        ViewModel.INSTANCE.getViewsFactory().getView("sidebar");
    }

    public static void main(String[] args) {
        launch();
    }
}