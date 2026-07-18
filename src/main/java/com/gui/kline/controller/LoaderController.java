package com.gui.kline.controller;

import javafx.animation.KeyFrame;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.ResourceBundle;

public class LoaderController implements Initializable {

    @FXML private AnchorPane rootPane;
    @FXML private Label lblStatus;
    @FXML private Label lblPercent;

    private static final String[] STATUS_MESSAGES = {
            "Initialising system…",
            "Loading inventory data…",
            "Connecting to services…",
            "Preparing dashboard…",
            "Almost ready…"
    };

    private int msgIndex = 0;
    private double progress = 0.0;
    private Timeline timeline;
    private RotateTransition wrenchRotation;

    private Runnable onComplete;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lblStatus.setText(STATUS_MESSAGES[0]);
        lblPercent.setText("0%");

        javafx.application.Platform.runLater(this::animateWrench);

        timeline = new Timeline(new KeyFrame(Duration.millis(60), e -> tick()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void animateWrench() {
        try {
            animateIconsInNode(rootPane);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void animateIconsInNode(javafx.scene.Node node) {
        if (node instanceof FontIcon icon) {
            try {
                if (icon.getIconLiteral() != null && icon.getIconLiteral().contains("wrench")) {
                    wrenchRotation = new RotateTransition(Duration.millis(3000), icon);
                    wrenchRotation.setByAngle(360);
                    wrenchRotation.setCycleCount(RotateTransition.INDEFINITE);
                    wrenchRotation.play();
                }
            } catch (Exception e) {
            }
        }

        if (node instanceof javafx.scene.Parent parent) {
            parent.getChildrenUnmodifiable().forEach(this::animateIconsInNode);
        }
    }

    private void tick() {
        progress = Math.min(progress + 0.008, 1.0);
        lblPercent.setText((int)(progress * 100) + "%");

        int idx = (int)(progress / 0.2);
        if (idx < STATUS_MESSAGES.length) {
            lblStatus.setText(STATUS_MESSAGES[idx]);
        }

        if (progress >= 1.0) {
            timeline.stop();
            if (wrenchRotation != null) {
                wrenchRotation.stop();
            }
            lblStatus.setText("Ready!");
            if (onComplete != null) {
                onComplete.run();
            }
        }
    }

    public void setOnComplete(Runnable onComplete) {
        this.onComplete = onComplete;
    }

}