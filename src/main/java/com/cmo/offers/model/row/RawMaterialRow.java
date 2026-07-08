package com.cmo.offers.model.row;

import java.math.BigDecimal;

import com.cmo.offers.entity.MaterialEntity;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class RawMaterialRow {
	
	public static final BigDecimal DEFAULT_SCRAP_VALUE_PERCENTAGE = new BigDecimal("0.70");

    private final BooleanProperty summary = new SimpleBooleanProperty(false);

    private final ObjectProperty<MaterialEntity> material = new SimpleObjectProperty<>();
    private final StringProperty description = new SimpleStringProperty("");

    private final ObjectProperty<BigDecimal> grossWeight = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> netWeight = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> scrapWeight = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> scrapValuePercentage =
            new SimpleObjectProperty<>(DEFAULT_SCRAP_VALUE_PERCENTAGE);

    private final ObjectProperty<BigDecimal> transformationPrice = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> costXPiece = new SimpleObjectProperty<>(BigDecimal.ZERO);

    private final ObjectProperty<BigDecimal> markup = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> lmeUsdPerTon = new SimpleObjectProperty<>();
    private final ObjectProperty<BigDecimal> fxUsdToEur = new SimpleObjectProperty<>();
    private final ObjectProperty<BigDecimal> finalEurPerKg = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> price = new SimpleObjectProperty<>(BigDecimal.ZERO);
    
    public RawMaterialRow() {
        this(false);
    }

    public RawMaterialRow(boolean summary) {
        this.summary.set(summary);
    }

    public RawMaterialRow(
    		MaterialEntity material,
            String description,
            BigDecimal grossWeight,
            BigDecimal netWeight,
            BigDecimal scrapWeight,
            BigDecimal scrapValuePercentage,
            BigDecimal transformationPrice,
            BigDecimal costXPiece,
            BigDecimal markup,
            BigDecimal lmeUsdPerTon,
            BigDecimal fxUsdToEur,
            BigDecimal finalEurPerKg,
            BigDecimal price
    ) {
    	this(false);
        setMaterial(material);
        setDescription(description);
        setGrossWeight(grossWeight);
        setNetWeight(netWeight);
        setScrapWeight(scrapWeight);
        setScrapValuePercentage(scrapValuePercentage);
        setTransformationPrice(transformationPrice);
        setCostXPiece(costXPiece);
        setMarkup(markup);
        setLmeUsdPerTon(lmeUsdPerTon);
        setFxUsdToEur(fxUsdToEur);
        setFinalEurPerKg(finalEurPerKg);
        setPrice(price);
    }
    
    public BooleanProperty summaryProperty() { return summary; }
    public StringProperty descriptionProperty() { return description; }
    public ObjectProperty<MaterialEntity> materialProperty() { return material; }
    public ObjectProperty<BigDecimal> grossWeightProperty() { return grossWeight; }
    public ObjectProperty<BigDecimal> netWeightProperty() { return netWeight; }
    public ObjectProperty<BigDecimal> scrapWeightProperty() { return scrapWeight; }
    public ObjectProperty<BigDecimal> scrapValuePercentageProperty() { return scrapValuePercentage; }
    public ObjectProperty<BigDecimal> transformationPriceProperty() { return transformationPrice; }
    public ObjectProperty<BigDecimal> costXPieceProperty() { return costXPiece; }
    public ObjectProperty<BigDecimal> markupProperty() { return markup; }
    public ObjectProperty<BigDecimal> lmeUsdPerTonProperty() { return lmeUsdPerTon; }
    public ObjectProperty<BigDecimal> fxUsdToEurProperty() { return fxUsdToEur; }
    public ObjectProperty<BigDecimal> finalEurPerKgProperty() { return finalEurPerKg; }
    public ObjectProperty<BigDecimal> priceProperty() { return price; }

    public boolean isSummary() { return summary.get(); }
    public MaterialEntity getMaterial() { return material.get(); }
    public String getDescription() { return description.get(); }
    public BigDecimal getGrossWeight() { return grossWeight.get(); }
    public BigDecimal getNetWeight() { return netWeight.get(); }
    public BigDecimal getScrapWeight() { return scrapWeight.get(); }
    public BigDecimal getScrapValuePercentage() { return scrapValuePercentage.get(); }
    public BigDecimal getTransformationPrice() { return transformationPrice.get(); }
    public BigDecimal getCostXPiece() { return costXPiece.get(); }
    public BigDecimal getMarkup() { return markup.get(); }
    public BigDecimal getLmeUsdPerTon() { return lmeUsdPerTon.get(); }
    public BigDecimal getFxUsdToEur() { return fxUsdToEur.get(); }
    public BigDecimal getFinalEurPerKg() { return finalEurPerKg.get(); }
    public BigDecimal getPrice() { return price.get(); }
    
    public void setSummary(boolean value) { summary.set(value); }
    public void setMaterial(MaterialEntity value) { material.set(value); }
    public void setDescription(String value) { description.set(value == null ? "" : value); }
    public void setGrossWeight(BigDecimal value) { grossWeight.set(defaultIfNull(value)); }
    public void setNetWeight(BigDecimal value) { netWeight.set(defaultIfNull(value)); }
    public void setScrapWeight(BigDecimal value) { scrapWeight.set(defaultIfNull(value)); }
    public void setScrapValuePercentage(BigDecimal value) { scrapValuePercentage.set(value == null ? DEFAULT_SCRAP_VALUE_PERCENTAGE : value); }
    public void setTransformationPrice(BigDecimal value) { transformationPrice.set(defaultIfNull(value)); }
    public void setCostXPiece(BigDecimal value) { costXPiece.set(defaultIfNull(value)); }     
    public void setMarkup(BigDecimal value) { markup.set(defaultIfNull(value)); }
    public void setLmeUsdPerTon(BigDecimal value) { lmeUsdPerTon.set(value); } 
    public void setFxUsdToEur(BigDecimal value) { fxUsdToEur.set(value); }    
    public void setFinalEurPerKg(BigDecimal value) { finalEurPerKg.set(defaultIfNull(value)); }
    public void setPrice(BigDecimal value) { price.set(defaultIfNull(value)); }

    private static BigDecimal defaultIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }	
}

