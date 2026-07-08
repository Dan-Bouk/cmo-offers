package com.cmo.offers.model.row;

import java.time.LocalDate;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/*
 * Represents one node in the TreeTableView.
 * For UI purposes only.
 */
public class OfferTreeRow {

    private final boolean referenceRow;
    private final int offerId;

    private final StringProperty customerName;
    private final StringProperty requestNr;
    private final StringProperty offerNr;
    private final ObjectProperty<LocalDate> offerDate;
    private final StringProperty revision;

    private final Integer referenceId;
    private final StringProperty referenceDoc;

    public static OfferTreeRow offerRow(int offerId, String customerName, String requestNr,
                                        String offerNr, LocalDate offerDate, String revision) {
        return new OfferTreeRow(false, offerId, customerName, requestNr, offerNr, offerDate, revision, null, null);
    }

    public static OfferTreeRow referenceRow(int offerId, Integer referenceId, String referenceDoc) {
        return new OfferTreeRow(true, offerId, null, null, null, null, null, referenceId, referenceDoc);
    }

    private OfferTreeRow(boolean referenceRow, int offerId, String customerName, String requestNr,
                         String offerNr, LocalDate offerDate, String revision,
                         Integer referenceId, String referenceDoc) {
        this.referenceRow = referenceRow;
        this.offerId = offerId;
        this.customerName = new SimpleStringProperty(this, "customerName", customerName);
        this.requestNr = new SimpleStringProperty(this, "requestNr", requestNr);
        this.offerNr = new SimpleStringProperty(this, "offerNr", offerNr);
        this.offerDate = new SimpleObjectProperty<>(this, "offerDate", offerDate);
        this.revision = new SimpleStringProperty(this, "revision", revision);
        this.referenceId = referenceId;
        this.referenceDoc = new SimpleStringProperty(this, "referenceDoc", referenceDoc);
    }

    public boolean isReferenceRow() { return referenceRow; }
    public int getOfferId() { return offerId; }

    public String getCustomerName() { return customerName.get(); }
    public void setCustomerName(String customerName) { this.customerName.set(customerName); }
    public StringProperty customerNameProperty() { return customerName; }

    public String getRequestNr() { return requestNr.get(); }
    public void setRequestNr(String requestNr) { this.requestNr.set(requestNr); }
    public StringProperty requestNrProperty() { return requestNr; }

    public String getOfferNr() { return offerNr.get(); }
    public void setOfferNr(String offerNr) { this.offerNr.set(offerNr); }
    public StringProperty offerNrProperty() { return offerNr; }

    public LocalDate getOfferDate() { return offerDate.get(); }
    public void setOfferDate(LocalDate offerDate) { this.offerDate.set(offerDate); }
    public ObjectProperty<LocalDate> offerDateProperty() { return offerDate; }

    public String getRevision() { return revision.get(); }
    public void setRevision(String revision) { this.revision.set(revision); }
    public StringProperty revisionProperty() { return revision; }

    public Integer getReferenceId() { return referenceId; }

    public String getReferenceDoc() { return referenceDoc.get(); }
    public void setReferenceDoc(String referenceDoc) { this.referenceDoc.set(referenceDoc); }
    public StringProperty referenceDocProperty() { return referenceDoc; }
}
