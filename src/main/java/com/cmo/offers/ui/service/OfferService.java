package com.cmo.offers.ui.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

import com.cmo.offers.dao.OfferDAO;
import com.cmo.offers.dao.OfferTreeDAO;
import com.cmo.offers.dao.OfferRefDAO;
import com.cmo.offers.entity.ClientEntity;
import com.cmo.offers.entity.OfferEntity;
import com.cmo.offers.entity.OfferRefEntity;
import com.cmo.offers.model.row.OfferRefJoinRow;

public class OfferService {
	
	 private final OfferDAO offerDAO;	 
	 private final OfferTreeDAO offerTreeDAO;
	 private final OfferRefDAO offerRefDAO;
	 
	 public OfferService(OfferDAO offerDAO, OfferTreeDAO offerTreeDAO, OfferRefDAO offerRefDAO) {
		 this.offerDAO = offerDAO;
	     this.offerTreeDAO = offerTreeDAO;
	     this.offerRefDAO = offerRefDAO;
	 }

	 // --------- CREATE / UPDATE ---------

	 public OfferEntity createOffer(OfferEntity offer) throws SQLException {
	     validateForSave(offer);

	     // optional: enforce unique offer number
	     if (offerDAO.findByOfferNr(offer.getOfferNr()).isPresent()) {
	         throw new IllegalArgumentException("Offer Nr already exists: " + offer.getOfferNr());
	     }

	     offerDAO.save(offer);
	     return offer;
	 }

	    public OfferEntity updateOffer(OfferEntity offer) throws SQLException {
	        if (offer.getId() == 0) {
	            throw new IllegalArgumentException("Offer id is missing (cannot update).");
	        }
	        validateForSave(offer);

	        offerDAO.update(offer);
	        return offer;
	    }
	    
	    public OfferEntity save(OfferEntity offer) throws SQLException {
	        if (offer == null) {
	            throw new IllegalArgumentException("Offer is null.");
	        }

	        if (offer.getId() == 0) {
	            return createOffer(offer);
	        } else {
	            return updateOffer(offer);
	        }
	    }

	    public void saveOrUpdate(OfferEntity offer) throws SQLException {
	        validateForSave(offer);

	        // optional uniqueness check only for new offers
	        if (offer.getId() == 0 && offerDAO.findByOfferNr(offer.getOfferNr()).isPresent()) {
	            throw new IllegalArgumentException("Offer Nr already exists: " + offer.getOfferNr());
	        }

	        offerDAO.saveOrUpdate(offer);
	    }

	    public void deleteOffer(int offerId) throws SQLException {
	        if (offerId == 0) throw new IllegalArgumentException("Offer id is missing.");
	        offerRefDAO.deleteByOfferId(offerId);
	        offerDAO.delete(offerId);
	    }

	    public OfferEntity duplicateOffer(int sourceOfferId) throws SQLException {
	        OfferEntity source = getOfferById(sourceOfferId);
	
	        OfferEntity copy = new OfferEntity();
	        copy.setOfferNr(generateDuplicateOfferNr(source.getOfferNr()));
	        copy.setOfferDate(source.getOfferDate());
	        copy.setRevision(source.getRevision());
	        copy.setClientId(source.getClientId());
	        copy.setRequestNr(source.getRequestNr());
	
	        createOffer(copy);
	
	        List<OfferRefEntity> duplicatedReferences = new ArrayList<>();
	        for (OfferRefEntity sourceRef : offerRefDAO.findByOfferId(sourceOfferId)) {
	            OfferRefEntity refCopy = new OfferRefEntity();
	            refCopy.setOfferId(copy.getId());
	            refCopy.setDoc(sourceRef.getDoc());
	            offerRefDAO.save(refCopy);
	            duplicatedReferences.add(refCopy);
	        }
	
	        return copy;
	    }

	    private String generateDuplicateOfferNr(String sourceOfferNr) throws SQLException {
	        String base = (sourceOfferNr == null || sourceOfferNr.isBlank()) ? "offer-copy" : sourceOfferNr.trim() + "-copy";
	        String candidate = base;
	        int counter = 2;
	
	        while (offerDAO.findByOfferNr(candidate).isPresent()) {
	            candidate = base + "-" + counter;
	            counter++;
	        }
	
	        return candidate;
	    }

	    // --------- LISTING FOR TABLEVIEW ---------
	    
	    public OfferEntity getOfferById(int id) throws SQLException {
	        return offerDAO.findById(id)
	                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + id));
	    }

	    public List<OfferRefJoinRow> getOfferTreeRows() throws SQLException {
	        return offerTreeDAO.findAllForTree();
	    }
	    
	    public List<OfferRefJoinRow> getOfferTreeRowsByClient(ClientEntity client) throws SQLException {
	        if (client == null || client.getId() == 0) {
	            throw new IllegalArgumentException("Client is required.");
	        }
	        return offerTreeDAO.findByClientIdForTree(client.getId());
	    }

	    // --------- VALIDATION ---------

	    private void validateForSave(OfferEntity offer) {
	        if (offer == null) throw new IllegalArgumentException("Offer is null.");

	        if (offer.getOfferNr() == null || offer.getOfferNr().isBlank())
	            throw new IllegalArgumentException("Offer Nr is required.");

	        if (offer.getOfferDate() == null)
	            throw new IllegalArgumentException("Offer Date is required.");

	        if (offer.getClientId() == 0)
	            throw new IllegalArgumentException("Customer is required.");

	        // Optional: normalize revision
	        if (offer.getRevision() != null && offer.getRevision().isBlank()) {
	            offer.setRevision(null);
	        }

	        // Offer date cannot be in future
	        if (offer.getOfferDate().isAfter(LocalDate.now())) {
	            throw new IllegalArgumentException("Offer Date cannot be in the future.");
	        }
	    }

}