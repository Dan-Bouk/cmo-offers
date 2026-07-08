package com.cmo.offers.model;

import java.util.ArrayList;
import java.util.List;

public class OfferBundle {
    private int formatVersion = 1;
    private OfferExportModel offer;
    private List<ReferenceExportModel> references = new ArrayList<>();

    public int getFormatVersion() {
        return formatVersion;
    }

    public void setFormatVersion(int formatVersion) {
        this.formatVersion = formatVersion;
    }

    public OfferExportModel getOffer() {
        return offer;
    }

    public void setOffer(OfferExportModel offer) {
        this.offer = offer;
    }

    public List<ReferenceExportModel> getReferences() {
        return references;
    }

    public void setReferences(List<ReferenceExportModel> references) {
        this.references = references;
    }
}