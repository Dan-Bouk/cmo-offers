package com.cmo.offers.entity;

import java.time.LocalDate;

public class OfferEntity {
	
	private int id;                 // DB primary key
    private String offerNr;          // Offer. Nr.
    private LocalDate offerDate;     // Offer Date
    private String revision;         // Rev.
    private int clientId;            // Customer (FK -> client.id)
    private String requestNr;        // Your request

    public OfferEntity() {}

    public OfferEntity(int id) {
        this.id = id;
    }

    public OfferEntity(String offerNr, LocalDate offerDate, String revision, int clientId, String requestNr) {
        this.offerNr = offerNr;
        this.offerDate = offerDate;
        this.revision = revision;
        this.clientId = clientId;
        this.requestNr = requestNr;
    }

    public OfferEntity(int id, String offerNr, LocalDate offerDate, String revision, int clientId, String requestNr) {
        this.id = id;
        this.offerNr = offerNr;
        this.offerDate = offerDate;
        this.revision = revision;
        this.clientId = clientId;
        this.requestNr = requestNr;
    }

    /** Convenience: true when not persisted yet (matches your DAO pattern). */
    public boolean isNew() {
        return id == 0;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getOfferNr() { return offerNr; }
    public void setOfferNr(String offerNr) { this.offerNr = offerNr; }

    public LocalDate getOfferDate() { return offerDate; }
    public void setOfferDate(LocalDate offerDate) { this.offerDate = offerDate; }

    public String getRevision() { return revision; }
    public void setRevision(String revision) { this.revision = revision; }

    public int getClientId() { return clientId; }
    public void setClientId(int clientId) { this.clientId = clientId; }

    public String getRequestNr() { return requestNr; }
    public void setRequestNr(String requestNr) { this.requestNr = requestNr; }

    @Override
    public String toString() {
        // Useful when debugging/logging
        return "OfferEntity{" +
                "id=" + id +
                ", offerNr='" + offerNr + '\'' +
                ", offerDate=" + offerDate +
                ", revision='" + revision + '\'' +
                ", clientId=" + clientId +
                ", requestNr='" + requestNr + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OfferEntity)) return false;
        OfferEntity that = (OfferEntity) o;
        return id != 0 && id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
    

}

