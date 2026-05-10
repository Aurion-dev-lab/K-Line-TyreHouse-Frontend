package com.gui.kline;

import com.gui.kline.controller.LoaderController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        // Load loader screen
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/gui/kline/view/loader.fxml")
        );

        Parent loaderView = loader.load();

        LoaderController ctrl = loader.getController();

        // When loading complete
        ctrl.setOnComplete(() -> {
            try {

                FXMLLoader loginLoader = new FXMLLoader(
                        getClass().getResource("/com/gui/kline/view/login.fxml")
                );

                Parent loginView = loginLoader.load();

                Platform.runLater(() -> {
                    stage.setScene(new Scene(loginView));
                    stage.setTitle("Kline — Sign In");
                    stage.centerOnScreen();
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Initial stage setup
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setResizable(false);
        stage.setScene(new Scene(loaderView, 600, 400));
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}