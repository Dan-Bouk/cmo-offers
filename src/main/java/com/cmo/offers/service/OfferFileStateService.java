package com.cmo.offers.service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cmo.offers.dao.OfferRefDAO;
import com.cmo.offers.entity.OfferEntity;
import com.cmo.offers.entity.OfferRefEntity;
import com.cmo.offers.export.service.OfferJsonService;
import com.cmo.offers.model.OfferBundle;
import com.cmo.offers.model.OfferExportModel;
import com.cmo.offers.model.ReferenceExportModel;

public class OfferFileStateService {
	
    private final OfferRefDAO offerRefDAO;
    private final OfferJsonService offerJsonService;

	public OfferFileStateService(OfferRefDAO offerRefDAO, OfferJsonService offerJsonService) {
		super();
		this.offerRefDAO = offerRefDAO;
		this.offerJsonService = offerJsonService;
	}

	public void copyReferenceStateFiles(int sourceOfferId, int targetOfferId) throws Exception {
        List<OfferRefEntity> sourceRefs = offerRefDAO.findByOfferId(sourceOfferId);
        List<OfferRefEntity> targetRefs = offerRefDAO.findByOfferId(targetOfferId);

        Map<String, Integer> targetRefIdsByDoc = new HashMap<>();
        for (OfferRefEntity ref : targetRefs) {
            targetRefIdsByDoc.put(ref.getDoc(), ref.getId());
        }

        File stateDir = getReferenceStateDirectory();
        if (!stateDir.exists()) {
            return;
        }

        for (OfferRefEntity sourceRef : sourceRefs) {
            Integer targetRefId = targetRefIdsByDoc.get(sourceRef.getDoc());
            if (targetRefId == null) {
                continue;
            }

            File sourceFile = getReferenceStateFile(sourceRef.getId(), sourceOfferId, sourceRef.getDoc());
            if (!sourceFile.exists()) {
                continue;
            }

            File targetFile = getReferenceStateFile(targetRefId, targetOfferId, sourceRef.getDoc());
            File parent = targetFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
	
    public void deleteReferenceStateFiles(int offerId) throws Exception {
        for (OfferRefEntity ref : offerRefDAO.findByOfferId(offerId)) {
            File stateFile = getReferenceStateFile(ref.getId(), offerId, ref.getDoc());
            Files.deleteIfExists(stateFile.toPath());
        }
    }
    
    public void persistOfferHeaderToAllReferenceStateFiles(OfferEntity updatedOffer, String updatedCustomerName) throws Exception {
        if (updatedOffer == null) {
            return;
        }

        List<OfferRefEntity> refs = offerRefDAO.findByOfferId(updatedOffer.getId());
        for (OfferRefEntity ref : refs) {
            updateSingleReferenceStateFile(updatedOffer, ref, updatedCustomerName);
        }
    }
    
    public void updateSingleReferenceStateFile(OfferEntity updatedOffer, OfferRefEntity ref, String updatedCustomerName) throws Exception {
        if (updatedOffer == null || ref == null) {
            return;
        }

        File file = getReferenceStateFile(ref.getId(), updatedOffer.getId(), ref.getDoc());
        if (!file.exists()) {
            return;
        }

        OfferBundle bundle = offerJsonService.read(file);
        if (bundle == null) {
            return;
        }

        OfferExportModel offerDto = bundle.getOffer();
        if (offerDto == null) {
            offerDto = new OfferExportModel();
            bundle.setOffer(offerDto);
        }

        offerDto.setOfferNr(updatedOffer.getOfferNr());
        offerDto.setOfferDate(updatedOffer.getOfferDate());
        offerDto.setRevision(updatedOffer.getRevision());
        offerDto.setRequest(updatedOffer.getRequestNr());
        offerDto.setCustomer(updatedCustomerName);

        if (bundle.getReferences() != null) {
            for (ReferenceExportModel reference : bundle.getReferences()) {
                if (reference == null || reference.getGeneralInfo() == null) {
                    continue;
                }

                reference.getGeneralInfo().setOfferNr(updatedOffer.getOfferNr());
                reference.getGeneralInfo().setOfferDate(updatedOffer.getOfferDate());
                reference.getGeneralInfo().setRevision(updatedOffer.getRevision());
                reference.getGeneralInfo().setRequestNr(updatedOffer.getRequestNr());

                if (updatedCustomerName != null && !updatedCustomerName.isBlank()) {
                    reference.getGeneralInfo().setCustomerName(updatedCustomerName);
                }
            }
        }

        offerJsonService.write(file, bundle);
    }
	
	private File getReferenceStateFile(Integer refId, int offerId, String doc) {
        File dir = getReferenceStateDirectory();
        String safeDoc = sanitizeFileName(doc == null || doc.isBlank() ? "reference" : doc);

        if (refId != null && refId > 0) {
            return new File(dir, "ref_" + refId + "_" + safeDoc + ".json");
        }

        return new File(dir, "offer_" + offerId + "_" + safeDoc + ".json");
    }
	
	private File getReferenceStateDirectory() {
        return new File(System.getProperty("user.home"), ".cmooffers/reference-state");
    }
	
	private String sanitizeFileName(String value) {
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
