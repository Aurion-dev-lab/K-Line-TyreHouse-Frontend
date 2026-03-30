package com.gui.kline.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;

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
}
