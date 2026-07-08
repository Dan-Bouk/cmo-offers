package com.cmo.offers.model.row;

import java.math.BigDecimal;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class OperationsRow {

    public enum Type {
        MACH, ADD
    }

    public enum IntExt {
        I, E
    }

    public enum SummaryKind {
        NONE,
        TOTAL_MACHINING,
        TOTAL_ADDITIONAL_OPERATIONS
    }

    private final BooleanProperty summary = new SimpleBooleanProperty(false);
    private final ObjectProperty<SummaryKind> summaryKind =
            new SimpleObjectProperty<>(SummaryKind.NONE);

    /**
     * Marks the special default "Progettazione" row.
     * This lets service/controller logic identify it without relying
     * on object identity.
     */
    private final BooleanProperty defaultDesignRow = new SimpleBooleanProperty(false);

    // Editable/input fields
    private final ObjectProperty<Integer> fase = new SimpleObjectProperty<>();
    private final StringProperty operationName = new SimpleStringProperty();
    private final ObjectProperty<Type> type = new SimpleObjectProperty<>();
    private final ObjectProperty<IntExt> intExt = new SimpleObjectProperty<>();
    private final StringProperty center = new SimpleStringProperty();

    /**
     * Base cost:
     * - MACH: €/hour
     * - ADD: €/piece
     */
    private final ObjectProperty<BigDecimal> cost = new SimpleObjectProperty<>();

    private final ObjectProperty<BigDecimal> setupMinutes = new SimpleObjectProperty<>();
    private final ObjectProperty<BigDecimal> productionSeconds = new SimpleObjectProperty<>();
    private final ObjectProperty<BigDecimal> markup = new SimpleObjectProperty<>();

    // Calculated/output fields
    private final ObjectProperty<BigDecimal> setupCost =
            new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> prodCost =
            new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> setupPrice =
            new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> prodPrice =
            new SimpleObjectProperty<>(BigDecimal.ZERO);

    public OperationsRow() {
        this(false, SummaryKind.NONE);
    }

    public OperationsRow(boolean summary, SummaryKind summaryKind) {
        this.summary.set(summary);
        this.summaryKind.set(summaryKind);
        this.defaultDesignRow.set(false);
    }

    public OperationsRow(Integer fase,
                         String operationName,
                         Type type,
                         IntExt intExt,
                         String center,
                         BigDecimal cost,
                         BigDecimal setupMinutes,
                         BigDecimal productionSeconds,
                         BigDecimal markup) {
        this();
        setFase(fase);
        setOperationName(operationName);
        setType(type);
        setIntExt(intExt);
        setCenter(center);
        setCost(cost);
        setSetupMinutes(setupMinutes);
        setProductionSeconds(productionSeconds);
        setMarkup(markup);
    }

    public OperationsRow(Integer fase,
                         String operationName,
                         Type type,
                         IntExt intExt,
                         String center,
                         BigDecimal cost,
                         BigDecimal setupMinutes,
                         BigDecimal productionSeconds,
                         BigDecimal markup,
                         boolean defaultDesignRow) {
        this(fase, operationName, type, intExt, center, cost, setupMinutes, productionSeconds, markup);
        setDefaultDesignRow(defaultDesignRow);
    }

    public boolean isEditable() {
        return !isSummary();
    }

    public boolean isMach() {
        return getType() == Type.MACH;
    }

    public boolean isAdd() {
        return getType() == Type.ADD;
    }

    public boolean isInternal() {
        return getIntExt() == IntExt.I;
    }

    public boolean isExternal() {
        return getIntExt() == IntExt.E;
    }

    public String getSummaryLabel() {
        if (!isSummary()) {
            return "";
        }

        return switch (getSummaryKind()) {
            case TOTAL_MACHINING -> "Total Machining";
            case TOTAL_ADDITIONAL_OPERATIONS -> "Total Additional Operations";
            default -> "";
        };
    }

    // Properties
    public BooleanProperty summaryProperty() {
        return summary;
    }

    public ObjectProperty<SummaryKind> summaryKindProperty() {
        return summaryKind;
    }

    public BooleanProperty defaultDesignRowProperty() {
        return defaultDesignRow;
    }

    public ObjectProperty<Integer> faseProperty() {
        return fase;
    }

    public StringProperty operationNameProperty() {
        return operationName;
    }

    public ObjectProperty<Type> typeProperty() {
        return type;
    }

    public ObjectProperty<IntExt> intExtProperty() {
        return intExt;
    }

    public StringProperty centerProperty() {
        return center;
    }

    public ObjectProperty<BigDecimal> costProperty() {
        return cost;
    }

    public ObjectProperty<BigDecimal> setupMinutesProperty() {
        return setupMinutes;
    }

    public ObjectProperty<BigDecimal> productionSecondsProperty() {
        return productionSeconds;
    }

    public ObjectProperty<BigDecimal> markupProperty() {
        return markup;
    }

    public ObjectProperty<BigDecimal> setupCostProperty() {
        return setupCost;
    }

    public ObjectProperty<BigDecimal> prodCostProperty() {
        return prodCost;
    }

    public ObjectProperty<BigDecimal> setupPriceProperty() {
        return setupPrice;
    }

    public ObjectProperty<BigDecimal> prodPriceProperty() {
        return prodPrice;
    }

    // Getters
    public boolean isSummary() {
        return summary.get();
    }

    public SummaryKind getSummaryKind() {
        return summaryKind.get();
    }

    public boolean isDefaultDesignRow() {
        return defaultDesignRow.get();
    }

    public Integer getFase() {
        return fase.get();
    }

    public String getOperationName() {
        return operationName.get();
    }

    public Type getType() {
        return type.get();
    }

    public IntExt getIntExt() {
        return intExt.get();
    }

    public String getCenter() {
        return center.get();
    }

    public BigDecimal getCost() {
        return cost.get();
    }

    public BigDecimal getSetupMinutes() {
        return setupMinutes.get();
    }

    public BigDecimal getProductionSeconds() {
        return productionSeconds.get();
    }

    public BigDecimal getMarkup() {
        return markup.get();
    }

    public BigDecimal getSetupCost() {
        return setupCost.get();
    }

    public BigDecimal getProdCost() {
        return prodCost.get();
    }

    public BigDecimal getSetupPrice() {
        return setupPrice.get();
    }

    public BigDecimal getProdPrice() {
        return prodPrice.get();
    }

    // Setters
    public void setSummary(boolean value) {
        summary.set(value);
    }

    public void setSummaryKind(SummaryKind value) {
        summaryKind.set(value);
    }

    public void setDefaultDesignRow(boolean value) {
        defaultDesignRow.set(value);
    }

    public void setFase(Integer value) {
        fase.set(value);
    }

    public void setOperationName(String value) {
        operationName.set(value);
    }

    public void setType(Type value) {
        type.set(value);
    }

    public void setIntExt(IntExt value) {
        intExt.set(value);
    }

    public void setCenter(String value) {
        center.set(value);
    }

    public void setCost(BigDecimal value) {
        cost.set(value);
    }

    public void setSetupMinutes(BigDecimal value) {
        setupMinutes.set(value);
    }

    public void setProductionSeconds(BigDecimal value) {
        productionSeconds.set(value);
    }

    public void setMarkup(BigDecimal value) {
        markup.set(value);
    }

    public void setSetupCost(BigDecimal value) {
        setupCost.set(value);
    }

    public void setProdCost(BigDecimal value) {
        prodCost.set(value);
    }

    public void setSetupPrice(BigDecimal value) {
        setupPrice.set(value);
    }

    public void setProdPrice(BigDecimal value) {
        prodPrice.set(value);
    }
}