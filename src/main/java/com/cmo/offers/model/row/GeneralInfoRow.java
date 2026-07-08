package com.cmo.offers.model.row;

import javafx.beans.property.*;
import java.time.LocalDate;

public class GeneralInfoRow {

    private final StringProperty customerName = new SimpleStringProperty();
    private final StringProperty requestNr = new SimpleStringProperty();
    private final StringProperty offerNr = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> offerDate = new SimpleObjectProperty<>();
    private final StringProperty revision = new SimpleStringProperty();
    private final StringProperty referenceDoc = new SimpleStringProperty();
    
    private final StringProperty description = new SimpleStringProperty("");
    private final StringProperty drawing = new SimpleStringProperty("");
    private final StringProperty drawingRev = new SimpleStringProperty("");
    private final ObjectProperty<Integer> quantityPerYear = new SimpleObjectProperty<>();
    private final ObjectProperty<Integer> quantityPerBatch = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDate> firstDeliveryDate = new SimpleObjectProperty<>(null);
    private final ObjectProperty<LocalDate> lastDeliveryDate = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Integer> deliveryBatch = new SimpleObjectProperty<>();

    public StringProperty customerNameProperty() { return customerName; }
    public StringProperty requestNrProperty() { return requestNr; }
    public StringProperty offerNrProperty() { return offerNr; }
    public ObjectProperty<LocalDate> offerDateProperty() { return offerDate; }
    public StringProperty revisionProperty() { return revision; }
    public StringProperty referenceDocProperty() { return referenceDoc; }
    
    public StringProperty descriptionProperty() { return description; }
    public StringProperty drawingProperty() { return drawing; }
    public StringProperty drawingRevProperty() { return drawingRev; }
    public ObjectProperty<Integer> quantityPerYearProperty() { return quantityPerYear; }
    public ObjectProperty<Integer> quantityPerBatchProperty() { return quantityPerBatch; }
    public ObjectProperty<LocalDate> firstDeliveryDateProperty() { return firstDeliveryDate; }
    public ObjectProperty<LocalDate> lastDeliveryDateProperty() { return lastDeliveryDate; }
    public ObjectProperty<Integer> deliveryBatchProperty() { return deliveryBatch; }


    public String getCustomerName() { return customerName.get(); }
    public String getRequestNr() { return requestNr.get(); }
    public String getOfferNr() { return offerNr.get(); }
    public LocalDate getOfferDate() { return offerDate.get(); }
    public String getRevision() { return revision.get(); }
    public String getReferenceDoc() { return referenceDoc.get(); }
    public String getDescription() { return description.get(); }
    public String getDrawing() { return drawing.get(); }
    public String getDrawingRev() { return drawingRev.get(); }
    public Integer getQuantityPerYear() { return quantityPerYear.get(); }
    public Integer getQuantityPerBatch() { return quantityPerBatch.get(); }
    public LocalDate getFirstDeliveryDate() { return firstDeliveryDate.get(); }
    public LocalDate getLastDeliveryDate() { return lastDeliveryDate.get(); }
    public Integer getDeliveryBatch() { return deliveryBatch.get(); }
    
    public void setCustomerName(String value) { customerName.set(value); }
    public void setRequestNr(String value) { requestNr.set(value); }
    public void setOfferNr(String value) { offerNr.set(value); }
    public void setOfferDate(LocalDate value) { offerDate.set(value); }
    public void setRevision(String value) { revision.set(value); }
    public void setReferenceDoc(String value) { referenceDoc.set(value); }
    public void setDescription(String value) { description.set(value); }
    public void setDrawing(String value) { drawing.set(value); }
    public void setDrawingRev(String value) { drawingRev.set(value); }
    public void setQuantityPerYear(Integer value) { quantityPerYear.set(value); }
    public void setQuantityPerBatch(Integer value) { quantityPerBatch.set(value); }
    public void setFirstDeliveryDate(LocalDate value) { firstDeliveryDate.set(value); }
    public void setLastDeliveryDate(LocalDate value) { lastDeliveryDate.set(value); }
    public void setDeliveryBatch(Integer value) { deliveryBatch.set(value); }
		
}
