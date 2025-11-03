module com.proautokimium.alternarpowerbi {
	requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.swing;
    requires javafx.graphics;


    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires de.jensd.fx.glyphs.fontawesome;
    
    requires java.desktop;
    requires java.logging;
    requires java.net.http;
    requires jdk.crypto.ec;
    requires jdk.crypto.mscapi;
    requires jdk.crypto.cryptoki;

    opens com.proautokimium.alternarpowerbi to javafx.fxml, javafx.graphics;
    opens com.proautokimium.alternarpowerbi.infrastructure.services to javafx.fxml, javafx.graphics;

    exports com.proautokimium.alternarpowerbi;
    exports com.proautokimium.alternarpowerbi.infrastructure.services;
}