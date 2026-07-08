package com.cmo.offers.model;

import java.math.BigDecimal;

import com.cmo.offers.utils.BaseExportRow;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class RMExportModel extends BaseExportRow{

    private String materialCode;
    private String description;

    private BigDecimal grossWeight;
    private BigDecimal netWeight;
    private BigDecimal scrapWeight;
    private BigDecimal scrapValuePercentage;

    private BigDecimal transformationPrice;
    private BigDecimal costXPiece;

    private BigDecimal markup;
    private BigDecimal lmeUsdPerTon;
    private BigDecimal fxUsdToEur;
    private BigDecimal finalEurPerKg;
    private BigDecimal price;

    public String getMaterialCode() {
        return materialCode;
    }

    public void setMaterialCode(String materialCode) {
        this.materialCode = materialCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getGrossWeight() {
        return grossWeight;
    }

    public void setGrossWeight(BigDecimal grossWeight) {
        this.grossWeight = grossWeight;
    }

    public BigDecimal getNetWeight() {
        return netWeight;
    }

    public void setNetWeight(BigDecimal netWeight) {
        this.netWeight = netWeight;
    }

    public BigDecimal getScrapWeight() {
        return scrapWeight;
    }

    public void setScrapWeight(BigDecimal scrapWeight) {
        this.scrapWeight = scrapWeight;
    }

    public BigDecimal getScrapValuePercentage() {
        return scrapValuePercentage;
    }

    public void setScrapValuePercentage(BigDecimal scrapValuePercentage) {
        this.scrapValuePercentage = scrapValuePercentage;
    }

    public BigDecimal getTransformationPrice() {
        return transformationPrice;
    }

    public void setTransformationPrice(BigDecimal transformationPrice) {
        this.transformationPrice = transformationPrice;
    }

    public BigDecimal getCostXPiece() {
        return costXPiece;
    }

    public void setCostXPiece(BigDecimal costXPiece) {
        this.costXPiece = costXPiece;
    }

    public BigDecimal getMarkup() {
        return markup;
    }

    public void setMarkup(BigDecimal markup) {
        this.markup = markup;
    }

    public BigDecimal getLmeUsdPerTon() {
        return lmeUsdPerTon;
    }

    public void setLmeUsdPerTon(BigDecimal lmeUsdPerTon) {
        this.lmeUsdPerTon = lmeUsdPerTon;
    }

    public BigDecimal getFxUsdToEur() {
        return fxUsdToEur;
    }

    public void setFxUsdToEur(BigDecimal fxUsdToEur) {
        this.fxUsdToEur = fxUsdToEur;
    }

    public BigDecimal getFinalEurPerKg() {
        return finalEurPerKg;
    }

    public void setFinalEurPerKg(BigDecimal finalEurPerKg) {
        this.finalEurPerKg = finalEurPerKg;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    @Override
    @JsonIgnore
    public boolean isEmptyRow() {
        return isBlank(materialCode)
                && isBlank(description)
                && isZero(grossWeight)
                && isZero(netWeight)
                && isZero(scrapValuePercentage)
                && isZero(transformationPrice)
                && isZero(markup);
    }
}