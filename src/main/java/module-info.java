module com.example.gridsmart {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires java.sql;

    exports com.example.gridsmart.ui;
    opens com.example.gridsmart.ui to javafx.fxml;
    exports com.example.gridsmart.model;
    opens com.example.gridsmart.model to javafx.fxml;
    exports com.example.gridsmart.graph;
    opens com.example.gridsmart.graph to javafx.fxml;
    exports com.example.gridsmart.util;
    opens com.example.gridsmart.util to javafx.fxml;
    exports com.example.gridsmart.tests;
    opens com.example.gridsmart.tests to javafx.fxml;
    exports com.example.gridsmart.offline;
    opens com.example.gridsmart.offline to javafx.fxml;
    exports com.example.gridsmart.MasterController;
    opens com.example.gridsmart.MasterController to javafx.fxml;
}