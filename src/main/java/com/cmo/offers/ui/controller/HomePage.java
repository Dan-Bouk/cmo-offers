package com.cmo.offers.ui.controller;

import com.cmo.offers.entity.UserEntity;
import com.cmo.offers.model.table.MPTable;
import com.cmo.offers.model.table.OfferListTable;
import com.cmo.offers.session.UserSession;
import com.cmo.offers.ui.form.OfferForm;
import com.cmo.offers.ui.form.OfferFormController;
import com.cmo.offers.utils.AppContext;
import com.cmo.offers.utils.StyleUtils;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class HomePage extends BorderPane {
	
	private final AppContext ctx;

    private OfferListTable offerListTable;
    private MPTable pricingView;

    public HomePage(AppContext ctx) {
        this.ctx = ctx;

        setPadding(new Insets(12));
        setTop(buildHeader());
        setLeft(buildMenu());
        setCenter(buildWelcome());
    }

    private Node buildMenu() {

        Label title = new Label("Menu");
        title.getStyleClass().add("side-menu-title");

        Button offersBtn = new Button("Offers");
        offersBtn.setMaxWidth(Double.MAX_VALUE);
        offersBtn.getStyleClass().add("primary-button");

        Button newOfferBtn = new Button("New Offer");
        newOfferBtn.setMaxWidth(Double.MAX_VALUE);
        newOfferBtn.getStyleClass().add("subtle-button");

        Button pricingBtn = new Button("MP");
        pricingBtn.setMaxWidth(Double.MAX_VALUE);
        pricingBtn.getStyleClass().add("primary-button");

        offersBtn.setOnAction(e -> showOffers());
        newOfferBtn.setOnAction(e -> openNewOfferForm());
        pricingBtn.setOnAction(e -> showPricing());

        VBox menu = new VBox(10, title, offersBtn, newOfferBtn, pricingBtn);
        menu.setPadding(new Insets(10));
        menu.setAlignment(Pos.TOP_LEFT);
        menu.setPrefWidth(200);
        menu.getStyleClass().add("side-menu");

        return menu;
    }

    private Node buildWelcome() {
        Label welcome = new Label("Welcome");
        welcome.getStyleClass().add("welcome-title");

        Label hint = new Label("Use the menu on the left to open a module.");
        hint.getStyleClass().add("welcome-subtitle");

        VBox box = new VBox(8, welcome, hint);
        box.setPadding(new Insets(20));
        box.getStyleClass().add("content-card");

        return box;
    }

    private void showOffers() {
    	


        if (offerListTable == null) {

            offerListTable = new OfferListTable(
                    ctx.offerService,
                    ctx.clientDAO,
                    ctx.clientMarkupDAO,
                    ctx.plantDAO,
                    ctx.materialDAO,
                    ctx.marketPriceDAO,
                    ctx.rawMaterialService,
                    ctx.offerRefDAO,
                    ctx.windowManager,
                    ctx.fileStateService,
                    this::openNewOfferForm
            );
        }

        setCenter(offerListTable);
    }

    private void openNewOfferForm() {

        OfferForm formView = new OfferForm();

        new OfferFormController(
                formView,
                ctx.offerService,
                ctx.clientDAO,
                ctx.offerRefDAO
        );

        Stage st = new Stage();
        st.initModality(Modality.APPLICATION_MODAL);
        st.setTitle("Create Offer");

        formView.getStyleClass().add("form-window");

        Scene scene = new Scene(formView, 600, 450);
        scene.getStylesheets().add(StyleUtils.themeCss(getClass()));

        st.setScene(scene);
        st.showAndWait();

        showOffers();

        if (offerListTable != null) {
            offerListTable.refresh();
        }
    }

    private void showPricing() {

        if (pricingView == null) {
            pricingView = new MPTable(
                    ctx.clientDAO,
                    ctx.plantDAO,
                    ctx.materialDAO,
                    ctx.marketPriceDAO,
                    ctx.clientMarkupDAO
            );
            pricingView.load();
        }

        setCenter(pricingView);
    }

    private Node buildHeader() {
        ImageView logoView = new ImageView();

        try {
            Image logo = new Image(
                    getClass().getResourceAsStream(
                            "/CMO_SPA_logo_White.png"
                    )
            );
            logoView.setImage(logo);
            logoView.setPreserveRatio(true);
            logoView.setFitHeight(80);
        } catch (Exception ex) {
            System.err.println("Logo not found: " + ex.getMessage());
        }

        Label appTitle = new Label("Offers & Pricing");
        appTitle.setFont(Font.font(22));
        appTitle.getStyleClass().add("app-title");

        // Logged user
        UserEntity user = UserSession.getCurrentUser();

        String username =
                user != null ? user.getUsername() : "Unknown";

        Label userPrefix = new Label("Logged in as:");
        Label usernameLabel = new Label(username + " ▾");

        userPrefix.getStyleClass().add("user-label-prefix");
        usernameLabel.getStyleClass().add("user-label-name");

        ContextMenu userMenu = new ContextMenu();

        MenuItem changePassItem = new MenuItem("Change Password");
        MenuItem logoutItem = new MenuItem("Logout");
        
        changePassItem.getStyleClass().add("menu-item");
        logoutItem.getStyleClass().add("menu-item");

        logoutItem.setOnAction(e -> logout());

        userMenu.getItems().addAll(changePassItem, logoutItem);

        usernameLabel.setOnMouseClicked(e ->
                userMenu.show(usernameLabel, Side.BOTTOM, 0, 0)
        );

        HBox userBox = new HBox(4, userPrefix, usernameLabel);
        userBox.setAlignment(Pos.CENTER_LEFT);

        // Push user label to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(
                20,
                logoView,
                appTitle,
                spacer,
                userBox
        );

        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 10, 15, 10));
        header.getStyleClass().add("app-header");

        return header;
    }
    
    private void logout() {

        UserSession.logout();

        LoginPage loginPage = new LoginPage(ctx);

        Scene scene = new Scene(loginPage, 1100, 700);
        scene.getStylesheets().add(
                getClass().getResource("/theme.css").toExternalForm()
        );

        Stage stage = (Stage) getScene().getWindow();
        stage.setScene(scene);
    }
}