module com.gui.kline {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;

    opens com.gui.kline to javafx.fxml;
    opens com.gui.kline.view to javafx.fxml;

    exports com.gui.kline;
    exports com.gui.kline.controller;
    exports com.gui.kline.view;


    opens com.gui.kline.controller to javafx.fxml;
}