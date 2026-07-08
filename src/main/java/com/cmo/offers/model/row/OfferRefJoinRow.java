package com.cmo.offers.model.row;

import java.time.LocalDate;

/*
* Representation of a database tree row,
* model of one row returned from the SQL JOIN
*/

public class OfferRefJoinRow {
	
	public final int offerId;
    public final String customerName;
    public final String requestNr;
    public final String offerNr;
    public final java.time.LocalDate offerDate;
    public final String revision;

    public final Integer referenceId;   // may be null if no refs
    public final String referenceDoc;  // may be null if no refs

    public OfferRefJoinRow(int offerId, String customerName, String requestNr, String offerNr,
                           java.time.LocalDate offerDate, String revision,
                           Integer referenceId, String referenceDoc) {
        this.offerId = offerId;
        this.customerName = customerName;
        this.requestNr = requestNr;
        this.offerNr = offerNr;
        this.offerDate = offerDate;
        this.revision = revision;
        this.referenceId = referenceId;
        this.referenceDoc = referenceDoc;
    }
    
    public int getOfferId() { return offerId; }

    public String getCustomerName() { return customerName; }

    public String getRequestNr() { return requestNr; }

    public String getOfferNr() { return offerNr; }

    public LocalDate getOfferDate() { return offerDate; }

    public String getRevision() { return revision; }

    public Integer getReferenceId() { return referenceId; }

    public String getReferenceDoc() { return referenceDoc; }

}

 


