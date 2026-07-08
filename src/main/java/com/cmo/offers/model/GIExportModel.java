package com.cmo.offers.model;

import java.time.LocalDate;

public class GIExportModel {

    private String customerName;
    private String requestNr;
    private String offerNr;
    private LocalDate offerDate;
    private String revision;
    private String referenceDoc;

    private String description;
    private String drawing;
    private String drawingRev;
    private Integer quantityPerYear;
    private Integer quantityPerBatch;
    private LocalDate firstDeliveryDate;
    private LocalDate lastDeliveryDate;
    private Integer deliveryBatch;

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getRequestNr() {
        return requestNr;
    }

    public void setRequestNr(String requestNr) {
        this.requestNr = requestNr;
    }

    public String getOfferNr() {
        return offerNr;
    }

    public void setOfferNr(String offerNr) {
        this.offerNr = offerNr;
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

    public String getReferenceDoc() {
        return referenceDoc;
    }

    public void setReferenceDoc(String referenceDoc) {
        this.referenceDoc = referenceDoc;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDrawing() {
        return drawing;
    }

    public void setDrawing(String drawing) {
        this.drawing = drawing;
    }

    public String getDrawingRev() {
        return drawingRev;
    }

    public void setDrawingRev(String drawingRev) {
        this.drawingRev = drawingRev;
    }

    public Integer getQuantityPerYear() {
        return quantityPerYear;
    }

    public void setQuantityPerYear(Integer quantityPerYear) {
        this.quantityPerYear = quantityPerYear;
    }

    public Integer getQuantityPerBatch() {
        return quantityPerBatch;
    }

    public void setQuantityPerBatch(Integer quantityPerBatch) {
        this.quantityPerBatch = quantityPerBatch;
    }

    public LocalDate getFirstDeliveryDate() {
        return firstDeliveryDate;
    }

    public void setFirstDeliveryDate(LocalDate firstDeliveryDate) {
        this.firstDeliveryDate = firstDeliveryDate;
    }

    public LocalDate getLastDeliveryDate() {
        return lastDeliveryDate;
    }

    public void setLastDeliveryDate(LocalDate lastDeliveryDate) {
        this.lastDeliveryDate = lastDeliveryDate;
    }

    public Integer getDeliveryBatch() {
        return deliveryBatch;
    }

    public void setDeliveryBatch(Integer deliveryBatch) {
        this.deliveryBatch = deliveryBatch;
    }
}