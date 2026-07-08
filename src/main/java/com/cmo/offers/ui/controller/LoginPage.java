package com.cmo.offers.ui.controller;

import java.sql.SQLException;
import java.util.Optional;

import com.cmo.offers.entity.UserEntity;
import com.cmo.offers.session.UserSession;
import com.cmo.offers.utils.AppContext;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginPage extends VBox {

    private final AppContext ctx;

    public LoginPage(AppContext ctx) {
        this.ctx = ctx;
        buildUI();
    }

    private void buildUI() {

        setAlignment(Pos.CENTER);
        setPadding(new Insets(20));

        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(25));
        card.setMaxWidth(320);
        card.getStyleClass().add("login-card");

        Label titleLabel = new Label("Welcome Back");
        titleLabel.getStyleClass().add("login-title");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setMaxWidth(Double.MAX_VALUE);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setMaxWidth(Double.MAX_VALUE);

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("login-error");

        Button loginButton = new Button("Login");
        loginButton.getStyleClass().add("primary-button");
        loginButton.setMaxWidth(Double.MAX_VALUE);

        Button registerButton = new Button("Register");
        registerButton.getStyleClass().add("secondary-button");
        registerButton.setMaxWidth(Double.MAX_VALUE);

        // =========================
        // LOGIN ACTION
        // =========================
        loginButton.setOnAction(event -> {
            try {

                Optional<UserEntity> userOpt =
                        ctx.authService.login(
                                usernameField.getText(),
                                passwordField.getText()
                        );

                if (userOpt.isPresent()) {

                    UserSession.setCurrentUser(userOpt.get());

                    HomePage homePage = new HomePage(ctx);

                    Scene scene = new Scene(homePage, 1100, 700);
                    scene.getStylesheets().add(
                            getClass().getResource("/theme.css").toExternalForm()
                    );

                    Stage stage = (Stage) getScene().getWindow();
                    stage.setScene(scene);

                } else {
                    errorLabel.setText("Invalid username or password");
                }

            } catch (SQLException e) {
                errorLabel.setText("Database error");
                e.printStackTrace();
            }
        });

        // =========================
        // REGISTER ACTION
        // =========================
        registerButton.setOnAction(event -> {

            RegisterPage registerPage = new RegisterPage(ctx);

            Scene scene = new Scene(registerPage, 1100, 700);
            scene.getStylesheets().add(
                    getClass().getResource("/theme.css").toExternalForm()
            );

            Stage stage = (Stage) getScene().getWindow();
            stage.setScene(scene);
        });

        card.getChildren().addAll(
                titleLabel,
                usernameField,
                passwordField,
                loginButton,
                registerButton,
                errorLabel
        );

        getChildren().add(card);
    }
}