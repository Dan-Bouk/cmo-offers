package com.cmo.offers.model.row;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class OtherCostsRow {

    private final BooleanProperty summary = new SimpleBooleanProperty(false);

    private final StringProperty cdc = new SimpleStringProperty("");
    private final StringProperty description = new SimpleStringProperty("");

    private final ObjectProperty<BigDecimal> quantity = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> unitCost = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> markup = new SimpleObjectProperty<>(BigDecimal.ZERO);

    private final ObjectProperty<BigDecimal> totalCost = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> price = new SimpleObjectProperty<>(BigDecimal.ZERO);

    public OtherCostsRow() {
        this(false);
    }

    public OtherCostsRow(boolean summary) {
        this.summary.set(summary);

        if (!summary) {
            bindCalculatedFields();
        }
    }

    public OtherCostsRow(String cdc, String description, BigDecimal quantity,
                         BigDecimal unitCost, BigDecimal markup) {
        this(false);
        setCdc(cdc);
        setDescription(description);
        setQuantity(quantity);
        setUnitCost(unitCost);
        setMarkup(markup);
    }

    private void bindCalculatedFields() {
        ObjectBinding<BigDecimal> totalCostBinding = Bindings.createObjectBinding(
            () -> safe(quantity.get())
                    .multiply(safe(unitCost.get()))
                    .setScale(2, RoundingMode.HALF_UP),
            quantity, unitCost
        );

        ObjectBinding<BigDecimal> priceBinding = Bindings.createObjectBinding(
            () -> safe(totalCost.get())
                    .multiply(safe(markup.get()))
                    .setScale(2, RoundingMode.HALF_UP),
            totalCost, markup
        );

        totalCost.bind(totalCostBinding);
        price.bind(priceBinding);
    }

    public boolean isMaterialAndComponentsRow() {
        return "MP".equalsIgnoreCase(getCdc());
    }

    public boolean isMachiningRow() {
        return "MACH".equalsIgnoreCase(getCdc());
    }

    public void unbindQuantity() {
        if (quantity.isBound()) {
            quantity.unbind();
        }
    }

    public void unbindCalculatedFields() {
        if (totalCost.isBound()) {
            totalCost.unbind();
        }
        if (price.isBound()) {
            price.unbind();
        }
    }

    public BooleanProperty summaryProperty() { return summary; }
    public StringProperty cdcProperty() { return cdc; }
    public StringProperty descriptionProperty() { return description; }
    public ObjectProperty<BigDecimal> quantityProperty() { return quantity; }
    public ObjectProperty<BigDecimal> unitCostProperty() { return unitCost; }
    public ObjectProperty<BigDecimal> markupProperty() { return markup; }
    public ObjectProperty<BigDecimal> totalCostProperty() { return totalCost; }
    public ObjectProperty<BigDecimal> priceProperty() { return price; }

    public boolean isSummary() { return summary.get(); }
    public String getCdc() { return cdc.get(); }
    public String getDescription() { return description.get(); }
    public BigDecimal getQuantity() { return quantity.get(); }
    public BigDecimal getUnitCost() { return unitCost.get(); }
    public BigDecimal getMarkup() { return markup.get(); }
    public BigDecimal getTotalCost() { return totalCost.get(); }
    public BigDecimal getPrice() { return price.get(); }

    public void setSummary(boolean value) {
        summary.set(value);
    }

    public void setCdc(String value) {
        cdc.set(value == null ? "" : value);
    }

    public void setDescription(String value) {
        description.set(value == null ? "" : value);
    }

    public void setQuantity(BigDecimal value) {
        if (quantity.isBound()) {
            throw new IllegalStateException("quantity is bound and cannot be set directly");
        }
        quantity.set(safe(value));
    }

    public void setUnitCost(BigDecimal value) {
        unitCost.set(safe(value));
    }

    public void setMarkup(BigDecimal value) {
        markup.set(safe(value));
    }

    public void setTotalCost(BigDecimal value) {
        if (totalCost.isBound()) {
            throw new IllegalStateException("totalCost is bound and cannot be set directly");
        }
        totalCost.set(safe(value));
    }

    public void setPrice(BigDecimal value) {
        if (price.isBound()) {
            throw new IllegalStateException("price is bound and cannot be set directly");
        }
        price.set(safe(value));
    }

    private static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}