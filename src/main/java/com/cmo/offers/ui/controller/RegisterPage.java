package com.cmo.offers.ui.controller;

import java.sql.SQLException;

import com.cmo.offers.utils.AppContext;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class RegisterPage extends VBox {

    private final AppContext ctx;

    public RegisterPage(AppContext ctx) {
        this.ctx = ctx;
        buildUI();
    }

    private void buildUI() {

        setAlignment(Pos.CENTER);
        setPadding(new Insets(20));

        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(28));
        card.setMaxWidth(340);
        card.getStyleClass().add("login-card");

        Label titleLabel = new Label("Create Account");
        titleLabel.getStyleClass().add("login-title");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Confirm Password");

        Label message = new Label();
        message.getStyleClass().add("login-error");

        Button createButton = new Button("Create Account");
        createButton.getStyleClass().add("primary-button");
        createButton.setMaxWidth(Double.MAX_VALUE);

        Button backButton = new Button("Back to Login");
        backButton.getStyleClass().add("secondary-button");
        backButton.setMaxWidth(Double.MAX_VALUE);

        // =========================
        // CREATE ACCOUNT
        // =========================
        createButton.setOnAction(e -> {

            if (!passwordField.getText().equals(confirmField.getText())) {
                message.setText("Passwords do not match");
                return;
            }

            try {

                boolean ok = ctx.authService.register(
                        usernameField.getText(),
                        passwordField.getText()
                );

                if (ok) {
                	
                	message.getStyleClass().removeAll("login-error");
                    message.getStyleClass().add("login-success");
                    message.setText("Account created successfully! Redirecting...");

                    PauseTransition pause =
                            new PauseTransition(Duration.seconds(1.5));

                    pause.setOnFinished(event -> {
                        LoginPage loginPage = new LoginPage(ctx);

                        Scene scene = new Scene(loginPage, 1100, 700);
                        scene.getStylesheets().add(
                                getClass().getResource("/com/cmo/offers/resources/theme.css")
                                        .toExternalForm()
                        );

                        Stage stage = (Stage) getScene().getWindow();
                        stage.setScene(scene);
                    });

                    pause.play();

                } else {
                	message.getStyleClass().removeAll("login-success");
                    message.getStyleClass().add("login-error");
                    message.setText("Username already exists.");
                }

            } catch (SQLException ex) {
                ex.printStackTrace();
                message.setText("Database error");
            }
        });

        // =========================
        // BACK BUTTON
        // =========================
        backButton.setOnAction(event -> {

            LoginPage loginPage = new LoginPage(ctx);

            Scene scene = new Scene(loginPage, 1100, 700);
            scene.getStylesheets().add(
                    getClass().getResource("/com/cmo/offers/resources/theme.css")
                            .toExternalForm()
            );

            Stage stage = (Stage) getScene().getWindow();
            stage.setScene(scene);
        });

        card.getChildren().addAll(
                titleLabel,
                usernameField,
                passwordField,
                confirmField,
                createButton,
                backButton,
                message
        );

        getChildren().add(card);
    }
}