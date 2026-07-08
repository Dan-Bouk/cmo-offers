package com.cmo.offers.export.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.cmo.offers.dao.ClientDAO;
import com.cmo.offers.dao.OfferRefDAO;
import com.cmo.offers.entity.ClientEntity;
import com.cmo.offers.entity.OfferEntity;
import com.cmo.offers.entity.OfferRefEntity;
import com.cmo.offers.model.GIExportModel;
import com.cmo.offers.model.OfferBundle;
import com.cmo.offers.model.OfferExportModel;
import com.cmo.offers.model.ReferenceExportModel;
import com.cmo.offers.ui.service.OfferService;

import javafx.stage.Stage;

public class OfferExportService {

    private final OfferService offerService;
    private final OfferRefDAO offerRefDAO;
    private final ClientDAO clientDAO;
    private final OfferJsonService offerJsonService;
    private final OfferExcelExportService offerExcelExportService;

    public OfferExportService(
            OfferService offerService,
            OfferRefDAO offerRefDAO,
            ClientDAO clientDAO,
            OfferJsonService offerJsonService,
            OfferExcelExportService offerExcelExportService
    ) {
        this.offerService = Objects.requireNonNull(offerService, "offerService must not be null");
        this.offerRefDAO = Objects.requireNonNull(offerRefDAO, "offerRefDAO must not be null");
        this.clientDAO = Objects.requireNonNull(clientDAO, "clientDAO must not be null");
        this.offerJsonService = Objects.requireNonNull(offerJsonService, "offerJsonService must not be null");
        this.offerExcelExportService = Objects.requireNonNull(offerExcelExportService, "offerExcelExportService must not be null");
    }

    public void exportOfferToJson(int offerId, File file, Stage mainStage) throws Exception {
        OfferBundle bundle = buildOfferBundleFromSavedState(offerId);
        offerJsonService.write(file, bundle);
    }

    public void exportOfferToExcel(int offerId, File file, Stage mainStage) throws Exception {
        OfferBundle bundle = buildOfferBundleFromSavedState(offerId);
        offerExcelExportService.exportOffer(bundle, file);
    }

    private OfferBundle buildOfferBundleFromSavedState(int offerId) throws Exception {
        OfferEntity offer = offerService.getOfferById(offerId);
        if (offer == null) {
            throw new IllegalArgumentException("Offer not found: " + offerId);
        }

        ClientEntity client = clientDAO.findById(offer.getClientId()).orElse(null);

        OfferBundle bundle = new OfferBundle();
        bundle.setFormatVersion(1);
        bundle.setOffer(buildOfferDto(offer, client));

        List<ReferenceExportModel> references = new ArrayList<>();

        for (OfferRefEntity ref : offerRefDAO.findByOfferId(offerId)) {
            ReferenceExportModel referenceDto = readReferenceState(offer, ref);

            if (referenceDto == null) {
                referenceDto = buildMinimalReferenceDto(offer, client, ref);
            }

            references.add(referenceDto);
        }

        bundle.setReferences(references);
        return bundle;
    }

    private ReferenceExportModel readReferenceState(OfferEntity offer, OfferRefEntity ref) {
        try {
            File file = getReferenceStateFile(ref.getId(), offer.getId(), ref.getDoc());

            if (!file.exists()) {
                return null;
            }

            OfferBundle savedBundle = offerJsonService.read(file);

            if (savedBundle == null ||
                    savedBundle.getReferences() == null ||
                    savedBundle.getReferences().isEmpty()) {
                return null;
            }

            ReferenceExportModel dto = savedBundle.getReferences().get(0);

            if (dto.getReferenceId() == null || dto.getReferenceId().isBlank()) {
                dto.setReferenceId(ref.getDoc());
            }

            return dto;

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private ReferenceExportModel buildMinimalReferenceDto(
            OfferEntity offer,
            ClientEntity client,
            OfferRefEntity ref
    ) {
        ReferenceExportModel dto = new ReferenceExportModel();
        dto.setReferenceId(ref.getDoc());

        GIExportModel gi = new GIExportModel();
        gi.setOfferNr(offer.getOfferNr());
        gi.setOfferDate(offer.getOfferDate());
        gi.setRevision(offer.getRevision());
        gi.setRequestNr(offer.getRequestNr());
        gi.setCustomerName(client != null ? client.getName() : null);
        gi.setReferenceDoc(ref.getDoc());

        dto.setGeneralInfo(gi);

        return dto;
    }

    private OfferExportModel buildOfferDto(OfferEntity offer, ClientEntity client) {
        OfferExportModel dto = new OfferExportModel();
        dto.setOfferNr(offer.getOfferNr());
        dto.setOfferDate(offer.getOfferDate());
        dto.setRevision(offer.getRevision());
        dto.setRequest(offer.getRequestNr());
        dto.setCustomer(client != null ? client.getName() : null);
        return dto;
    }

    private File getReferenceStateFile(Integer refId, int offerId, String doc) {
        File dir = new File(System.getProperty("user.home"), ".cmooffers/reference-state");

        String safeDoc = sanitizeFileName(
                doc == null || doc.isBlank() ? "reference" : doc
        );

        if (refId != null && refId > 0) {
            return new File(dir, "ref_" + refId + "_" + safeDoc + ".json");
        }

        return new File(dir, "offer_" + offerId + "_" + safeDoc + ".json");
    }

    private String sanitizeFileName(String value) {
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}