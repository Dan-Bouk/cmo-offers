package com.cmo.offers;

import com.cmo.offers.ui.controller.LoginPage;
import com.cmo.offers.utils.AppContext;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {

        // =========================
        // APP CONTEXT (all wiring inside)
        // =========================
        AppContext ctx = new AppContext();

        // =========================
        // ROOT VIEW
        // =========================
        LoginPage root = new LoginPage(ctx);

        Scene scene = new Scene(root, 1100, 700);
        scene.getStylesheets().add(
                getClass().getResource("/theme.css")
                        .toExternalForm()
        );

        stage.setTitle("ARIS Offers");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
