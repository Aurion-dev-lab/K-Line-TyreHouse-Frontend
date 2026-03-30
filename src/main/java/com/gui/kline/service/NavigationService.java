package com.gui.kline.service;

import com.gui.kline.models.ViewModel;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;

public class NavigationService {
    public static void navigate(Pane container, String page) {
        Node view = ViewModel.INSTANCE
                .getViewsFactory()
                .getPage(page);

        if (view == null) {
            System.err.println("View is null, skipping navigation to: " + page);
            return;
        }

        AnchorPane.setTopAnchor(view, 0.0);
        AnchorPane.setLeftAnchor(view, 0.0);
        AnchorPane.setRightAnchor(view, 0.0);
        AnchorPane.setBottomAnchor(view, 0.0);

        container.getChildren().setAll(view);
    }
}
