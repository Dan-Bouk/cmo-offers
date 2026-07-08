package com.cmo.offers.ui.window;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.cmo.offers.ui.service.OfferService;
import com.cmo.offers.ui.service.RawMaterialService;

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

    private final Map<Integer, Stage> openStages = new HashMap<>();
    private final Map<Integer, OfferTabController> openControllers = new HashMap<>();
    
    public ReferenceWindowManager(OfferService offerService, ClientDAO clientDAO, PlantDAO plantDAO,
			MaterialDAO materialDAO, MarketPriceDAO marketPriceDAO, RawMaterialService rawMaterialService) {
		super();
		this.offerService = offerService;
		this.clientDAO = clientDAO;
		this.plantDAO = plantDAO;
		this.materialDAO = materialDAO;
		this.marketPriceDAO = marketPriceDAO;
		this.rawMaterialService = rawMaterialService;
	}

	public void openReference(OfferTreeRow selected, 
			Stage ownerStage, 
			Consumer<OfferEntity> callback) throws Exception {
		
		Integer referenceId = selected.getReferenceId();
		
        if (referenceId == null) {
        	return;
        }

        Stage existingStage = getExistingReferenceStage(referenceId);
        if (existingStage != null) {
            focusStage(existingStage);
            return;
        }

        OfferEntity offer = offerService.getOfferById(referenceId);

        ClientEntity client = clientDAO.findById(offer.getClientId()).orElse(null);
        PlantEntity plant = null;
        if (client != null) {
            List<PlantEntity> plants = plantDAO.findByClientId(client.getId());
            if (!plants.isEmpty()) {
            	plant = plants.get(0);
            }
        }

        YearMonth period = offer.getOfferDate() != null
        		? YearMonth.from(offer.getOfferDate())
                : YearMonth.now();

        GeneralInfoRow generalInfoRow = new GeneralInfoRow();
        OfferTabSheet view = new OfferTabSheet();
        view.getStyleClass().add("content-card");
        view.getStyleClass().add("reference-window");

        OfferTabController controller = new OfferTabController(
        		view,
                offer,
                referenceId,
                selected.getReferenceDoc(),
                client != null ? client.getName() : selected.getCustomerName(),
                client,
                plant,
                period,
                materialDAO,
                marketPriceDAO,
                rawMaterialService,
                generalInfoRow,
                ownerStage,
                callback
        );

        Stage detailStage = new Stage();
        detailStage.setTitle("SCHEDA - " + selected.getReferenceDoc());
        detailStage.initModality(Modality.NONE);

        Scene detailScene = new Scene(view, 1100, 750);
        detailScene.getStylesheets().add(
        		getClass().getResource("/theme.css").toExternalForm()
        );
        detailStage.setScene(detailScene);

        registerReferenceWindow(referenceId, detailStage, controller);

        detailStage.show();
        focusStage(detailStage);
    }
	
    private Stage getExistingReferenceStage(Integer referenceId) {
        Stage stage = openStages.get(referenceId);

        if (stage == null) {
            return null;
        }

        if (!stage.isShowing()) {
            openStages.remove(referenceId);
            openControllers.remove(referenceId);
            return null;
        }

        return stage;
    }
    
    private void focusStage(Stage stage) {
        if (stage == null) {
            return;
        }

        if (stage.isIconified()) {
            stage.setIconified(false);
        }

        stage.show();
        stage.toFront();
        stage.requestFocus();
    }
    
    private void registerReferenceWindow(Integer referenceId, Stage stage, OfferTabController controller) {
        openStages.put(referenceId, stage);
        openControllers.put(referenceId, controller);

        stage.setOnHidden(e -> {
            openStages.remove(referenceId);
            openControllers.remove(referenceId);
        });
    }   
    
    public void closeOfferWindows(int offerId) {
        List<Integer> referenceIdsToClose = openControllers.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().getOfferId() == offerId)
                .map(Map.Entry::getKey)
                .toList();

        for (Integer referenceId : referenceIdsToClose) {
            Stage stage = openStages.get(referenceId);
            if (stage != null) {
                javafx.application.Platform.runLater(stage::close);
            }
        }
    }
    
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

            if (controller.getOfferId() == offer.getId()) {
                controller.applyOfferHeaderUpdate(offer, customerName);
            }
        }
    }
}
