package com.gui.kline.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.Map;

public class ViewFactory {
    // Storage for primary stage and last opened dialog stage
    private Stage primaryStage;
    private Stage lastDialogStage;
    
    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }
    
    public Stage getPrimaryStage() {
        return primaryStage;
    }
    
    public void getView(String view) {
        getView(view, primaryStage);
    }
    
    public void getView(String view, Stage ownerStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/gui/kline/view/" + view + ".fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("K-Line - " + view);
            
            // Use provided owner or fall back to primary stage
            Stage actualOwner = ownerStage != null ? ownerStage : primaryStage;
            
            // If owner stage is available, make this window a child of it
            if (actualOwner != null) {
                stage.initOwner(actualOwner);
                // Don't make main layout modal - only dialogs should be modal
                if (!view.equals("main-layout")) {
                    stage.initModality(Modality.WINDOW_MODAL);
                }
            }
            
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
            
            // Store reference
            lastDialogStage = stage;
        } catch (Exception e) {
            System.out.println("Error loading view: " + view + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Node getPage(String page){
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/gui/kline/view/" + page + ".fxml"));
            return loader.load();
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public <T> T getForm(String view, Stage ownerStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/gui/kline/view/" + view + ".fxml"));
            Parent root = loader.load();
            T controller = loader.getController();

            Stage stage = new Stage();
            stage.initStyle(StageStyle.TRANSPARENT);
            
            // Use provided owner or fall back to primary stage
            Stage actualOwner = ownerStage != null ? ownerStage : primaryStage;
            
            // Only set modality and owner if owner stage is available
            if (actualOwner != null) {
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.initOwner(actualOwner);

                Scene scene = new Scene(root, actualOwner.getWidth(), actualOwner.getHeight());
                scene.setFill(Color.TRANSPARENT);
                stage.setScene(scene);

                stage.setX(actualOwner.getX());
                stage.setY(actualOwner.getY());

                actualOwner.xProperty().addListener((obs, oldVal, newVal) -> stage.setX(newVal.doubleValue()));
                actualOwner.yProperty().addListener((obs, oldVal, newVal) -> stage.setY(newVal.doubleValue()));
                actualOwner.widthProperty().addListener((obs, oldVal, newVal) -> stage.setWidth(newVal.doubleValue()));
                actualOwner.heightProperty().addListener((obs, oldVal, newVal) -> stage.setHeight(newVal.doubleValue()));
            } else {
                // If no owner, create a regular scene
                Scene scene = new Scene(root);
                scene.setFill(Color.TRANSPARENT);
                stage.setScene(scene);
            }

            stage.show();
            
            // Store reference so callers can access it
            lastDialogStage = stage;

            return controller;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // New method to get the last opened dialog stage
    public Stage getLastDialogStage() {
        return lastDialogStage;
    }
}
