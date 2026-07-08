package com.cmo.offers.entity;

public class OfferRefEntity {


	    private int id;
	    private int offerId;
	    private String doc;

	    public OfferRefEntity() {}

	    public OfferRefEntity(int id, int offerId, String doc) {
	        this.id = id;
	        this.offerId = offerId;
	        this.doc = doc;
	    }

	    public OfferRefEntity(int offerId, String doc) {
	        this.offerId = offerId;
	        this.doc = doc;
	    }

	    public int getId() { return id; }
	    public int getOfferId() { return offerId; }
	    public String getDoc() { return doc; }
	    
	    public void setId(int id) { this.id = id; }
	    public void setOfferId(int offerId) { this.offerId = offerId; }
	    public void setDoc(String doc) { this.doc = doc; }

	    @Override
	    public String toString() { return doc; }
	
}
