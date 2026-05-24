module com.gui.kline {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires java.desktop;
    requires okhttp3;
    requires com.google.gson;

    opens com.gui.kline to javafx.fxml;
    opens com.gui.kline.view to javafx.fxml;
    exports com.gui.kline;
    exports com.gui.kline.controller;
    exports com.gui.kline.view;


    opens com.gui.kline.controller to javafx.fxml;
    exports com.gui.kline.controller.form;
    opens com.gui.kline.controller.form to javafx.fxml;
    opens com.gui.kline.models to javafx.base, com.google.gson;

}