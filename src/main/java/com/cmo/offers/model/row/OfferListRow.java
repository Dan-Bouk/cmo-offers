package com.cmo.offers.model.row;

import java.time.LocalDate;


public class OfferListRow {
	private final int id;
	private final String customerName;
	private final String requestNr;
	private final String offerNr;
	private final LocalDate offerDate;
	private final String revision;

	public OfferListRow(int id, String customerName, String requestNr, String offerNr, LocalDate offerDate, String revision) {
		this.id = id;
	    this.customerName = customerName;
	    this.requestNr = requestNr;
	    this.offerNr = offerNr;
	    this.offerDate = offerDate;
	    this.revision = revision;
	}

	public int getId() { return id; }
	public String getCustomerName() { return customerName; }
	public String getRequestNr() { return requestNr; }
	public String getOfferNr() { return offerNr; }
	public LocalDate getOfferDate() { return offerDate; }
	public String getRevision() { return revision; }
	
}

