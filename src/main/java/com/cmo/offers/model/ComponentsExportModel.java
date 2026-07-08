package com.cmo.offers.model;

import java.math.BigDecimal;

import com.cmo.offers.utils.BaseExportRow;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class ComponentsExportModel extends BaseExportRow{

    private String cdc;
    private String componentName;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private BigDecimal markup;
    private BigDecimal cost;
    private BigDecimal price;

    public String getCdc() {
        return cdc;
    }

    public void setCdc(String cdc) {
        this.cdc = cdc;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public BigDecimal getMarkup() {
        return markup;
    }

    public void setMarkup(BigDecimal markup) {
        this.markup = markup;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
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
        return isBlank(cdc)
                && isBlank(componentName)
                && isZero(quantity)
                && isZero(unitCost)
                && isZero(markup);
    }
}