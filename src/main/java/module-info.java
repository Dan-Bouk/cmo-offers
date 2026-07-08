module com.cmo.offers {

	exports com.cmo.offers;

	// --- Java ---
    requires transitive java.sql;
    requires java.logging;

    // --- JavaFX ---
    requires javafx.base;
    requires transitive javafx.graphics;
    requires transitive javafx.controls;
    requires javafx.fxml;

    // --- Libraries ---
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;

    requires org.postgresql.jdbc;
    
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;

    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;

    // --- Exports (normal access) ---
    exports com.cmo.offers.entity;
    exports com.cmo.offers.dao;
    exports com.cmo.offers.ui.service;
    exports com.cmo.offers.model;
    exports com.cmo.offers.model.row;
    exports com.cmo.offers.utils;
    exports com.cmo.offers.service;
    exports com.cmo.offers.ui.window;

    opens com.cmo.offers.entity to com.fasterxml.jackson.databind;
    
    // Needed so JavaFX can launch your Application class
    exports com.cmo.offers.ui.controller to javafx.graphics;

    // --- Reflection (FXML / JavaFX properties) ---
    opens com.cmo.offers.ui.controller to javafx.fxml;
    opens com.cmo.offers.model to javafx.base;
    opens com.cmo.offers.model.row to javafx.base;

}