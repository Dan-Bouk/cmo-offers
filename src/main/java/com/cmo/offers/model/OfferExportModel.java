package com.cmo.offers.model;

import java.time.LocalDate;

public class OfferExportModel {
    private String offerNr;
    private String customer;
    private String request;
    private LocalDate offerDate;
    private String revision;

    public String getOfferNr() {
        return offerNr;
    }

    public void setOfferNr(String offerNr) {
        this.offerNr = offerNr;
    }

    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public LocalDate getOfferDate() {
        return offerDate;
    }

    public void setOfferDate(LocalDate offerDate) {
        this.offerDate = offerDate;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }
}
