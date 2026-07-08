package com.cmo.offers.load.service;

import java.io.File;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.cmo.offers.dao.ClientDAO;
import com.cmo.offers.dao.MarketPriceDAO;
import com.cmo.offers.dao.MaterialDAO;
import com.cmo.offers.dao.OfferRefDAO;
import com.cmo.offers.dao.PlantDAO;
import com.cmo.offers.entity.ClientEntity;
import com.cmo.offers.entity.OfferEntity;
import com.cmo.offers.entity.OfferRefEntity;
import com.cmo.offers.entity.PlantEntity;
import com.cmo.offers.export.service.OfferJsonService;
import com.cmo.offers.model.GIExportModel;
import com.cmo.offers.model.OfferBundle;
import com.cmo.offers.model.OfferExportModel;
import com.cmo.offers.model.OfferTabSheet;
import com.cmo.offers.model.ReferenceExportModel;
import com.cmo.offers.model.row.GeneralInfoRow;
import com.cmo.offers.ui.controller.OfferTabController;
import com.cmo.offers.ui.service.OfferService;
import com.cmo.offers.ui.service.RawMaterialService;

import javafx.stage.Stage;

public class OfferImportService {

    private final OfferJsonService offerJsonService;
    private final OfferService offerService;
    private final OfferRefDAO offerRefDAO;
    private final ClientDAO clientDAO;
    private final PlantDAO plantDAO;
    private final MaterialDAO materialDAO;
    private final MarketPriceDAO marketPriceDAO;
    private final RawMaterialService rawMaterialService;

    public OfferImportService(
            OfferJsonService offerJsonService,
            OfferService offerService,
            OfferRefDAO offerRefDAO,
            ClientDAO clientDAO,
            PlantDAO plantDAO,
            MaterialDAO materialDAO,
            MarketPriceDAO marketPriceDAO,
            RawMaterialService rawMaterialService
    ) {
        this.offerJsonService = Objects.requireNonNull(offerJsonService, "offerJsonService must not be null");
        this.offerService = Objects.requireNonNull(offerService, "offerService must not be null");
        this.offerRefDAO = Objects.requireNonNull(offerRefDAO, "offerRefDAO must not be null");
        this.clientDAO = Objects.requireNonNull(clientDAO, "clientDAO must not be null");
        this.plantDAO = Objects.requireNonNull(plantDAO, "plantDAO must not be null");
        this.materialDAO = Objects.requireNonNull(materialDAO, "materialDAO must not be null");
        this.marketPriceDAO = Objects.requireNonNull(marketPriceDAO, "marketPriceDAO must not be null");
        this.rawMaterialService = Objects.requireNonNull(rawMaterialService, "rawMaterialService must not be null");
    }

    public OfferEntity importOffer(File file, Stage mainStage) throws Exception {
        OfferBundle bundle = offerJsonService.read(file);
        validateBundle(bundle);

        OfferEntity offer = createOfferEntity(bundle.getOffer());

        List<ReferenceExportModel> references = bundle.getReferences();
        if (references != null) {
            for (ReferenceExportModel referenceDto : references) {
                importReference(offer, referenceDto, mainStage);
            }
        }

        return offer;
    }

    private void validateBundle(OfferBundle bundle) {
        if (bundle == null) {
            throw new IllegalArgumentException("Imported file is empty or invalid.");
        }

        if (bundle.getOffer() == null) {
            throw new IllegalArgumentException("Imported file does not contain offer data.");
        }
    }

    private OfferEntity createOfferEntity(OfferExportModel dto) throws Exception {
        if (dto == null) {
            throw new IllegalArgumentException("Offer data is missing.");
        }

        OfferEntity offer = new OfferEntity();
        offer.setOfferNr(dto.getOfferNr());
        offer.setRequestNr(readRequest(dto));
        offer.setOfferDate(dto.getOfferDate());
        offer.setRevision(dto.getRevision());

        Integer clientId = resolveClientId(dto);
        if (clientId == null) {
            throw new IllegalArgumentException(
                    "Could not resolve client from imported offer: " + readCustomer(dto)
            );
        }

        offer.setClientId(clientId);

        OfferEntity savedOffer = offerService.save(offer);
        if (savedOffer == null) {
            throw new IllegalStateException("OfferService returned null while saving offer.");
        }

        return savedOffer;
    }

    private void importReference(OfferEntity offer, ReferenceExportModel dto, Stage mainStage) throws Exception {
        if (dto == null) {
            return;
        }

        OfferRefEntity ref = new OfferRefEntity();
        ref.setOfferId(offer.getId());
        ref.setDoc(resolveReferenceDoc(dto));

        offerRefDAO.save(ref);

        ClientEntity client = clientDAO.findById(offer.getClientId()).orElse(null);
        PlantEntity plant = resolvePlant(client);

        YearMonth period = YearMonth.from(offer.getOfferDate()).minusMonths(1);

        GeneralInfoRow generalInfoRow = new GeneralInfoRow();
        OfferTabSheet view = new OfferTabSheet();

        OfferTabController controller = new OfferTabController(
                view,
                offer,
                ref.getId(),
                ref.getDoc(),
                client != null ? client.getName() : readCustomerFromReference(dto),
                client,
                plant,
                period,
                materialDAO,
                marketPriceDAO,
                rawMaterialService,
                generalInfoRow,
                mainStage,
                updatedOffer -> {
                    // no-op during import
                }
        );

        controller.loadReferenceDocDto(dto);

        // If your method now returns boolean, just ignore the return value.
        controller.saveToDatabaseSilently();
    }

    private String resolveReferenceDoc(ReferenceExportModel dto) {
        if (dto.getReferenceId() != null && !dto.getReferenceId().isBlank()) {
            return dto.getReferenceId();
        }

        GIExportModel gi = dto.getGeneralInfo();
        if (gi != null && gi.getReferenceDoc() != null && !gi.getReferenceDoc().isBlank()) {
            return gi.getReferenceDoc();
        }

        throw new IllegalArgumentException("Reference doc is missing in imported reference.");
    }

    private Integer resolveClientId(OfferExportModel dto) throws SQLException {
        String importedCustomer = readCustomer(dto);
        if (importedCustomer == null || importedCustomer.isBlank()) {
            return null;
        }

        List<ClientEntity> clients = clientDAO.findAll();

        Optional<ClientEntity> exact = clients.stream()
                .filter(Objects::nonNull)
                .filter(c -> c.getName() != null)
                .filter(c -> c.getName().trim().equalsIgnoreCase(importedCustomer.trim()))
                .findFirst();

        return exact.map(ClientEntity::getId).orElse(null);
    }

    private PlantEntity resolvePlant(ClientEntity client) throws SQLException {
        if (client == null) {
            return null;
        }

        List<PlantEntity> plants = plantDAO.findByClientId(client.getId());
        return plants.isEmpty() ? null : plants.get(0);
    }

    private String readCustomer(OfferExportModel dto) {
        try {
            return dto.getCustomer();
        } catch (Exception ex) {
            return null;
        }
    }

    private String readRequest(OfferExportModel dto) {
        try {
            return dto.getRequest();
        } catch (Exception ex) {
            return null;
        }
    }

    private String readCustomerFromReference(ReferenceExportModel dto) {
        if (dto == null || dto.getGeneralInfo() == null) {
            return null;
        }
        return dto.getGeneralInfo().getCustomerName();
    }
}