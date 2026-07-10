package com.cmo.offers.ui.window;

import java.sql.SQLException;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import com.cmo.offers.dao.ClientDAO;
import com.cmo.offers.dao.MarketPriceDAO;
import com.cmo.offers.dao.MaterialDAO;
import com.cmo.offers.dao.PlantDAO;
import com.cmo.offers.entity.ClientEntity;
import com.cmo.offers.entity.OfferEntity;
import com.cmo.offers.entity.PlantEntity;
import com.cmo.offers.model.OfferTabSheet;
import com.cmo.offers.model.row.GeneralInfoRow;
import com.cmo.offers.model.row.OfferTreeRow;
import com.cmo.offers.ui.controller.OfferTabController;
import com.cmo.offers.ui.manager.StageManager;
import com.cmo.offers.ui.service.OfferService;
import com.cmo.offers.ui.service.RawMaterialService;
import com.cmo.offers.utils.ReferenceWindowException;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ReferenceWindowManager {

    private final OfferService offerService;
    private final ClientDAO clientDAO;
    private final PlantDAO plantDAO;
    private final MaterialDAO materialDAO;
    private final MarketPriceDAO marketPriceDAO;
    private final RawMaterialService rawMaterialService;
    private final StageManager stageManager;

    private final Map<Integer, OfferTabController> openControllers = new HashMap<>();


    public ReferenceWindowManager(
            OfferService offerService,
            ClientDAO clientDAO,
            PlantDAO plantDAO,
            MaterialDAO materialDAO,
            MarketPriceDAO marketPriceDAO,
            RawMaterialService rawMaterialService,
            StageManager stageManager) {

        this.offerService = offerService;
        this.clientDAO = clientDAO;
        this.plantDAO = plantDAO;
        this.materialDAO = materialDAO;
        this.marketPriceDAO = marketPriceDAO;
        this.rawMaterialService = rawMaterialService;
        this.stageManager = stageManager;
    }


    // ==========================================================
    // Window opening
    // ==========================================================

    /**
     * Opens a reference offer window.
     *
     * The method only coordinates the workflow:
     * - validates the reference
     * - loads required data
     * - creates the UI/controller/stage
     * - registers and displays the window
     */
    public void openReference(
            OfferTreeRow selected,
            Stage ownerStage,
            Consumer<OfferEntity> onOfferUpdated) throws Exception {

        Integer referenceId = selected.getReferenceId();

        if (referenceId == null) {
            return;
        }

        Stage existingStage = stageManager.getOpenWindow(referenceId);

        if (existingStage != null) {
            stageManager.focusWindow(existingStage);
            return;
        }

        OfferEntity offer = offerService.getOfferById(selected.getOfferId());

        ClientEntity client = loadClient(offer);
        PlantEntity plant = loadPlant(client);

        YearMonth period = resolvePeriod(offer);

        GeneralInfoRow generalInfoRow = new GeneralInfoRow();

        OfferTabSheet view = createView();

        OfferTabController controller = createController(
                view,
                selected,
                offer,
                referenceId,
                client,
                plant,
                period,
                generalInfoRow,
                ownerStage,
                onOfferUpdated
        );

        Stage detailStage = createStage(selected, view);

        registerWindow(referenceId, detailStage, controller);

        detailStage.show();
        stageManager.focusWindow(detailStage);
    }


    // ==========================================================
    // Data loading
    // ==========================================================

    /**
     * Loads the client associated with an offer.
     */
    private ClientEntity loadClient(OfferEntity offer) {

        try {
            return clientDAO.findById(offer.getClientId()).orElse(null);

        } catch (SQLException e) {
            throw new ReferenceWindowException(
                    "Unable to load client for offer " + offer.getId(),
                    e
            );
        }
    }


    /**
     * Loads the first plant associated with a client.
     *
     * A client may have multiple plants, but the reference window
     * currently uses the first available one.
     */
    private PlantEntity loadPlant(ClientEntity client) {

        if (client == null) {
            return null;
        }

        try {
            List<PlantEntity> plants = plantDAO.findByClientId(client.getId());

            return plants.isEmpty()
                    ? null
                    : plants.get(0);

        } catch (SQLException e) {
            throw new ReferenceWindowException(
                    "Unable to load plant for client " + client.getId(),
                    e
            );
        }
    }


    private YearMonth resolvePeriod(OfferEntity offer) {

        return offer.getOfferDate() != null
                ? YearMonth.from(offer.getOfferDate())
                : YearMonth.now();
    }


    // ==========================================================
    // UI creation
    // ==========================================================

    /**
     * Creates the root JavaFX component.
     */
    private OfferTabSheet createView() {

        OfferTabSheet view = new OfferTabSheet();

        view.getStyleClass().add("content-card");
        view.getStyleClass().add("reference-window");

        return view;
    }


    /**
     * Creates the controller responsible for the reference window logic.
     */
    private OfferTabController createController(
            OfferTabSheet view,
            OfferTreeRow selected,
            OfferEntity offer,
            Integer referenceId,
            ClientEntity client,
            PlantEntity plant,
            YearMonth period,
            GeneralInfoRow generalInfoRow,
            Stage ownerStage,
            Consumer<OfferEntity> onOfferUpdated) {

        return new OfferTabController(
                view,
                offer,
                referenceId,
                selected.getReferenceDoc(),
                client != null
                        ? client.getName()
                        : selected.getCustomerName(),
                client,
                plant,
                period,
                materialDAO,
                marketPriceDAO,
                rawMaterialService,
                generalInfoRow,
                ownerStage,
                onOfferUpdated
        );
    }


    /**
     * Creates and configures the JavaFX window.
     */
    private Stage createStage(
            OfferTreeRow selected,
            OfferTabSheet view) {

        Stage stage = new Stage();

        stage.setTitle("SCHEDA - " + selected.getReferenceDoc());
        stage.initModality(Modality.NONE);

        Scene scene = new Scene(view, 1100, 750);

        scene.getStylesheets().add(
                getClass().getResource("/theme.css").toExternalForm()
        );

        stage.setScene(scene);

        return stage;
    }


    // ==========================================================
    // Window lifecycle management
    // ==========================================================

    /**
     * Registers a window so it can be found, focused, closed,
     * and updated while it is open.
     */
    private void registerWindow(
            Integer referenceId,
            Stage stage,
            OfferTabController controller) {

        stageManager.registerWindow(referenceId, stage);
        openControllers.put(referenceId, controller);

        stage.setOnHidden(e -> {
            stageManager.unregisterWindow(referenceId);
            openControllers.remove(referenceId);
        });
    }


    /**
     * Closes all open reference windows belonging to an offer.
     */
    public void closeOfferWindows(int offerId) {

        List<Integer> referenceIdsToClose = openControllers.entrySet()
                .stream()
                .filter(entry ->
                        entry.getValue() != null &&
                        Objects.equals(entry.getValue().getOfferId(), offerId))
                .map(Map.Entry::getKey)
                .toList();

        for (Integer referenceId : referenceIdsToClose) {

            Stage stage = stageManager.getOpenWindow(referenceId);

            if (stage != null) {
                Platform.runLater(stage::close);
            }
        }
    }


    /**
     * Propagates offer changes to all open reference windows,
     * except the one that originated the update.
     */
    public void notifyOfferUpdated(
            OfferEntity offer,
            String customerName,
            Integer excludedReferenceId) {

        if (offer == null) {
            return;
        }

        for (Map.Entry<Integer, OfferTabController> entry : openControllers.entrySet()) {

            Integer referenceId = entry.getKey();
            OfferTabController controller = entry.getValue();

            if (controller == null) {
                continue;
            }

            if (excludedReferenceId != null &&
                    excludedReferenceId.equals(referenceId)) {
                continue;
            }

            if (Objects.equals(controller.getOfferId(), offer.getId())) {
                controller.applyOfferHeaderUpdate(offer, customerName);
            }
        }
    }
}