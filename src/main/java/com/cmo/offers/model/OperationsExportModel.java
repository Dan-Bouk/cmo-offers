package com.cmo.offers.model;

import java.math.BigDecimal;

import com.cmo.offers.utils.BaseExportRow;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class OperationsExportModel extends BaseExportRow {

    private Integer fase;
    private String operationName;
    private String type;
    private String intExt;
    private String center;

    private BigDecimal cost;
    private BigDecimal setupMinutes;
    private BigDecimal productionSeconds;
    private BigDecimal markup;

    private BigDecimal setupCost;
    private BigDecimal prodCost;
    private BigDecimal setupPrice;
    private BigDecimal prodPrice;

    private boolean defaultDesignRow;

    public Integer getFase() {
        return fase;
    }

    public void setFase(Integer fase) {
        this.fase = fase;
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIntExt() {
        return intExt;
    }

    public void setIntExt(String intExt) {
        this.intExt = intExt;
    }

    public String getCenter() {
        return center;
    }

    public void setCenter(String center) {
        this.center = center;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public BigDecimal getSetupMinutes() {
        return setupMinutes;
    }

    public void setSetupMinutes(BigDecimal setupMinutes) {
        this.setupMinutes = setupMinutes;
    }

    public BigDecimal getProductionSeconds() {
        return productionSeconds;
    }

    public void setProductionSeconds(BigDecimal productionSeconds) {
        this.productionSeconds = productionSeconds;
    }

    public BigDecimal getMarkup() {
        return markup;
    }

    public void setMarkup(BigDecimal markup) {
        this.markup = markup;
    }

    public BigDecimal getSetupCost() {
        return setupCost;
    }

    public void setSetupCost(BigDecimal setupCost) {
        this.setupCost = setupCost;
    }

    public BigDecimal getProdCost() {
        return prodCost;
    }

    public void setProdCost(BigDecimal prodCost) {
        this.prodCost = prodCost;
    }

    public BigDecimal getSetupPrice() {
        return setupPrice;
    }

    public void setSetupPrice(BigDecimal setupPrice) {
        this.setupPrice = setupPrice;
    }

    public BigDecimal getProdPrice() {
        return prodPrice;
    }

    public void setProdPrice(BigDecimal prodPrice) {
        this.prodPrice = prodPrice;
    }

    public boolean isDefaultDesignRow() {
        return defaultDesignRow;
    }

    public void setDefaultDesignRow(boolean defaultDesignRow) {
        this.defaultDesignRow = defaultDesignRow;
    }
    
    @Override
    @JsonIgnore
    public boolean isEmptyRow() {

        if (isDefaultDesignRow()) {
            return false;
        }

        return isBlank(operationName)
                && isBlank(type)
                && isBlank(center)
                && isBlank(intExt)
                && isZero(cost)
                && isZero(setupMinutes)
                && isZero(productionSeconds)
                && isZero(markup);
    }
    
    protected String trim(String value) {
        return value == null ? null : value.trim();
    }
}