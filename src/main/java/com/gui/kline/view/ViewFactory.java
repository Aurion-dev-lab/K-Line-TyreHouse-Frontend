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

public class ViewFactory {
    public void getView(String view){
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/gui/kline/view/" + view + ".fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e){
            System.out.println(e.getMessage());
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
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(ownerStage);

            Scene scene = new Scene(root, ownerStage.getWidth(), ownerStage.getHeight());
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);

            stage.setX(ownerStage.getX());
            stage.setY(ownerStage.getY());

            ownerStage.xProperty().addListener((obs, oldVal, newVal) -> stage.setX(newVal.doubleValue()));
            ownerStage.yProperty().addListener((obs, oldVal, newVal) -> stage.setY(newVal.doubleValue()));
            ownerStage.widthProperty().addListener((obs, oldVal, newVal) -> stage.setWidth(newVal.doubleValue()));
            ownerStage.heightProperty().addListener((obs, oldVal, newVal) -> stage.setHeight(newVal.doubleValue()));

            stage.show();

            return controller;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
