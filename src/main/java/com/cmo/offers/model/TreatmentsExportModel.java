package com.cmo.offers.model;

import java.math.BigDecimal;

import com.cmo.offers.utils.BaseExportRow;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class TreatmentsExportModel extends BaseExportRow{

    private Integer fase;
    private String treatmentName;
    private String type;
    private String intExt;
    private String center;

    private BigDecimal operationCost;
    private BigDecimal operationMarkup;
    private BigDecimal silverQuantityGr;
    private BigDecimal silverMarkup;

    private BigDecimal operationPrice;
    private BigDecimal silverCost;
    private BigDecimal silverPrice;

    public Integer getFase() {
        return fase;
    }

    public void setFase(Integer fase) {
        this.fase = fase;
    }

    public String getTreatmentName() {
        return treatmentName;
    }

    public void setTreatmentName(String treatmentName) {
        this.treatmentName = treatmentName;
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

    public BigDecimal getOperationCost() {
        return operationCost;
    }

    public void setOperationCost(BigDecimal operationCost) {
        this.operationCost = operationCost;
    }

    public BigDecimal getOperationMarkup() {
        return operationMarkup;
    }

    public void setOperationMarkup(BigDecimal operationMarkup) {
        this.operationMarkup = operationMarkup;
    }

    public BigDecimal getSilverQuantityGr() {
        return silverQuantityGr;
    }

    public void setSilverQuantityGr(BigDecimal silverQuantityGr) {
        this.silverQuantityGr = silverQuantityGr;
    }

    public BigDecimal getSilverMarkup() {
        return silverMarkup;
    }

    public void setSilverMarkup(BigDecimal silverMarkup) {
        this.silverMarkup = silverMarkup;
    }

    public BigDecimal getOperationPrice() {
        return operationPrice;
    }

    public void setOperationPrice(BigDecimal operationPrice) {
        this.operationPrice = operationPrice;
    }

    public BigDecimal getSilverCost() {
        return silverCost;
    }

    public void setSilverCost(BigDecimal silverCost) {
        this.silverCost = silverCost;
    }

    public BigDecimal getSilverPrice() {
        return silverPrice;
    }

    public void setSilverPrice(BigDecimal silverPrice) {
        this.silverPrice = silverPrice;
    }
    
    @Override
    @JsonIgnore
    public boolean isEmptyRow() {
        return isZero(fase)
                && isBlank(treatmentName)
                && isBlank(type)
                && isBlank(intExt)
                && isBlank(center)
                && isZero(operationCost)
                && isZero(operationMarkup)
                && isZero(silverQuantityGr)
                && isZero(silverMarkup);
    }
}