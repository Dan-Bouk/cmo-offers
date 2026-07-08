package com.cmo.offers.model.row;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ToolingRow {
	
    private final BooleanProperty summary = new SimpleBooleanProperty(false);

	private final StringProperty cdc = new SimpleStringProperty();
    private final StringProperty toolName = new SimpleStringProperty();
    private final ObjectProperty<BigDecimal> quantity = new SimpleObjectProperty<>();
    private final ObjectProperty<BigDecimal> unitCost = new SimpleObjectProperty<>();
    private final ObjectProperty<BigDecimal> markup = new SimpleObjectProperty<>();
    
    // Calculated values
    private final ObjectProperty<BigDecimal> cost = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> price = new SimpleObjectProperty<>(BigDecimal.ZERO);
    
    public ToolingRow() {
        this(false);
    }

    public ToolingRow(boolean summary) {
        this.summary.set(summary);
        if (!summary) {
            bindCalculatedFields();
        }
    }

    public ToolingRow(String cdc, String toolName, BigDecimal quantity,
                      BigDecimal unitCost, BigDecimal markup) {
        this();
        setCdc(cdc);
        setToolName(toolName);
        setQuantity(quantity);
        setUnitCost(unitCost);
        setMarkup(markup);
    }

    private void bindCalculatedFields() {
        ObjectBinding<BigDecimal> costBinding = Bindings.createObjectBinding(
            () -> {
                BigDecimal q = getQuantity() == null
                        ? BigDecimal.ZERO
                        : getQuantity();
                BigDecimal uc = getUnitCost() == null
                        ? BigDecimal.ZERO
                        : getUnitCost();

                return uc.multiply(q).setScale(2, RoundingMode.HALF_UP);
            },
            quantity, unitCost
        );
        
        cost.bind(costBinding);

        ObjectBinding<BigDecimal> priceBinding = Bindings.createObjectBinding(
            () -> {
                BigDecimal c = getCost() == null ? BigDecimal.ZERO : getCost();
                BigDecimal m = getMarkup() == null ? BigDecimal.ZERO : getMarkup();

                return c.multiply(m).setScale(2, RoundingMode.HALF_UP);
            },
            cost, markup
        );

        price.bind(priceBinding);
    }
    
    public BooleanProperty summaryProperty() { return summary; }
    public StringProperty cdcProperty() { return cdc; }
    public StringProperty toolNameProperty() { return toolName; }
    public ObjectProperty<BigDecimal> quantityProperty() { return quantity; }
    public ObjectProperty<BigDecimal> unitCostProperty() { return unitCost; }
    public ObjectProperty<BigDecimal> markupProperty() { return markup; }
    public ObjectProperty<BigDecimal> costProperty() { return cost; }
    public ObjectProperty<BigDecimal> priceProperty() { return price; }

    public boolean isSummary() { return summary.get(); }
    public String getCdc() { return cdc.get(); }
    public String getToolName() { return toolName.get(); }
    public BigDecimal getQuantity() { return quantity.get(); }
    public BigDecimal getUnitCost() { return unitCost.get(); }
    public BigDecimal getMarkup() { return markup.get(); }
    public BigDecimal getCost() { return cost.get(); }
    public BigDecimal getPrice() { return price.get(); }
    
    public void setSummary(boolean value) { summary.set(value); }
    public void setCdc(String value) { cdc.set(value); }
    public void setToolName(String value) { toolName.set(value); }
    public void setQuantity(BigDecimal value) { quantity.set(value); }
    public void setUnitCost(BigDecimal value) { unitCost.set(value); }
    public void setMarkup(BigDecimal value) { markup.set(value); }
    public void setCost(BigDecimal value) { cost.set(value); }
    public void setPrice(BigDecimal value) { price.set(value); }

}
