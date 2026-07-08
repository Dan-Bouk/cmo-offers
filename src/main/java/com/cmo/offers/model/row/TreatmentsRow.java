package com.cmo.offers.model.row;

import java.math.BigDecimal;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class TreatmentsRow {

    public enum IntExt {
        I, E
    }

    private final BooleanProperty summary = new SimpleBooleanProperty(false);

    private final ObjectProperty<Integer> fase = new SimpleObjectProperty<>();
    private final StringProperty treatmentName = new SimpleStringProperty("");
    private final StringProperty type = new SimpleStringProperty("");
    private final ObjectProperty<IntExt> intExt = new SimpleObjectProperty<>();
    private final StringProperty center = new SimpleStringProperty("");

    // Input fields
    private final ObjectProperty<BigDecimal> operationCost =
            new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> operationMarkup =
            new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> silverQuantityGr =
            new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> silverMarkup =
            new SimpleObjectProperty<>(BigDecimal.ZERO);

    // Calculated/output fields
    private final ObjectProperty<BigDecimal> operationPrice =
            new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> silverCost =
            new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> silverPrice =
            new SimpleObjectProperty<>(BigDecimal.ZERO);

    public TreatmentsRow() {
        this(false);
    }

    public TreatmentsRow(boolean summary) {
        this.summary.set(summary);
    }

    public TreatmentsRow(
            Integer fase,
            String treatmentName,
            String type,
            IntExt intExt,
            String center,
            BigDecimal operationCost,
            BigDecimal operationMarkup,
            BigDecimal silverQuantityGr,
            BigDecimal silverMarkup,
            BigDecimal operationPrice,
            BigDecimal silverCost,
            BigDecimal silverPrice
    ) {
        this(false);
        setFase(fase);
        setTreatmentName(treatmentName);
        setType(type);
        setIntExt(intExt);
        setCenter(center);
        setOperationCost(operationCost);
        setOperationMarkup(operationMarkup);
        setSilverQuantityGr(silverQuantityGr);
        setSilverMarkup(silverMarkup);
        setOperationPrice(operationPrice);
        setSilverCost(silverCost);
        setSilverPrice(silverPrice);
    }

    public boolean isSummary() {
        return summary.get();
    }

    public boolean isEditable() {
        return !isSummary();
    }

    public boolean isInternal() {
        return getIntExt() == IntExt.I;
    }

    public boolean isExternal() {
        return getIntExt() == IntExt.E;
    }

    public String getSummaryLabel() {
        return isSummary() ? "Total" : "";
    }

    // Properties
    public BooleanProperty summaryProperty() { return summary; }

    public ObjectProperty<Integer> faseProperty() { return fase; }
    public StringProperty treatmentNameProperty() { return treatmentName; }
    public StringProperty typeProperty() { return type; }
    public ObjectProperty<IntExt> intExtProperty() { return intExt; }
    public StringProperty centerProperty() { return center; }

    public ObjectProperty<BigDecimal> operationCostProperty() { return operationCost; }
    public ObjectProperty<BigDecimal> operationMarkupProperty() { return operationMarkup; }
    public ObjectProperty<BigDecimal> silverQuantityGrProperty() { return silverQuantityGr; }
    public ObjectProperty<BigDecimal> silverMarkupProperty() { return silverMarkup; }

    public ObjectProperty<BigDecimal> operationPriceProperty() { return operationPrice; }
    public ObjectProperty<BigDecimal> silverCostProperty() { return silverCost; }
    public ObjectProperty<BigDecimal> silverPriceProperty() { return silverPrice; }

    // Getters
    public Integer getFase() { return fase.get(); }
    public String getTreatmentName() { return treatmentName.get(); }
    public String getType() { return type.get(); }
    public IntExt getIntExt() { return intExt.get(); }
    public String getCenter() { return center.get(); }

    public BigDecimal getOperationCost() { return operationCost.get(); }
    public BigDecimal getOperationMarkup() { return operationMarkup.get(); }
    public BigDecimal getSilverQuantityGr() { return silverQuantityGr.get(); }
    public BigDecimal getSilverMarkup() { return silverMarkup.get(); }

    public BigDecimal getOperationPrice() { return operationPrice.get(); }
    public BigDecimal getSilverCost() { return silverCost.get(); }
    public BigDecimal getSilverPrice() { return silverPrice.get(); }

    // Setters
    public void setSummary(boolean value) { summary.set(value); }

    public void setFase(Integer value) { fase.set(value); }
    public void setTreatmentName(String value) { treatmentName.set(value == null ? "" : value); }
    public void setType(String value) { type.set(value == null ? "" : value); }
    public void setIntExt(IntExt value) { intExt.set(value); }
    public void setCenter(String value) { center.set(value == null ? "" : value); }

    public void setOperationCost(BigDecimal value) { operationCost.set(defaultIfNull(value)); }
    public void setOperationMarkup(BigDecimal value) { operationMarkup.set(defaultIfNull(value)); }
    public void setSilverQuantityGr(BigDecimal value) { silverQuantityGr.set(defaultIfNull(value)); }
    public void setSilverMarkup(BigDecimal value) { silverMarkup.set(defaultIfNull(value)); }

    public void setOperationPrice(BigDecimal value) { operationPrice.set(defaultIfNull(value)); }
    public void setSilverCost(BigDecimal value) { silverCost.set(defaultIfNull(value)); }
    public void setSilverPrice(BigDecimal value) { silverPrice.set(defaultIfNull(value)); }

    private static BigDecimal defaultIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}