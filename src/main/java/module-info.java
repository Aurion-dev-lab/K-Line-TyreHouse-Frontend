module com.gui.kline {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    uses org.kordamp.ikonli.IkonProvider;
    uses org.kordamp.ikonli.IkonHandler;
    requires org.kordamp.bootstrapfx.core;
    requires java.desktop;
    requires java.sql;
    requires java.net.http;
    requires mysql.connector.j;
    requires com.google.gson;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires okhttp3;
    requires com.github.librepdf.openpdf;

    opens com.gui.kline to javafx.fxml;
    opens com.gui.kline.view to javafx.fxml;

    exports com.gui.kline;
    exports com.gui.kline.controller;
    exports com.gui.kline.view;


    opens com.gui.kline.controller to javafx.fxml;
    exports com.gui.kline.controller.form;
    opens com.gui.kline.controller.form to javafx.fxml;
    opens com.gui.kline.models to javafx.base;
}