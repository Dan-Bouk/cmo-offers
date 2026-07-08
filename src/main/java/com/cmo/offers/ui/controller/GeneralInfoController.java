package com.cmo.offers.ui.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

import com.cmo.offers.model.GIExportModel;
import com.cmo.offers.model.row.GeneralInfoRow;
import com.cmo.offers.model.table.EditingIntegerCell;
import com.cmo.offers.model.table.EditingTextCell;
import com.cmo.offers.entity.OfferEntity;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.StringConverter;
import javafx.util.converter.LocalDateStringConverter;

public class GeneralInfoController {

    private final TableView<GeneralInfoRow> table;
    private final boolean inheritedOfferFieldsEditable;

    public GeneralInfoController(TableView<GeneralInfoRow> table) {
        this(table, true);
    }

    public GeneralInfoController(TableView<GeneralInfoRow> table, boolean inheritedOfferFieldsEditable) {
        this.table = table;
        this.inheritedOfferFieldsEditable = inheritedOfferFieldsEditable;
        configure();
    }

    private void configure() {
        table.setEditable(true);

        table.getColumns().setAll(List.of(
                textColumn("Offer. Nr.", GeneralInfoRow::offerNrProperty, inheritedOfferFieldsEditable),
                dateColumn("Offer Date", GeneralInfoRow::offerDateProperty, inheritedOfferFieldsEditable),
                textColumn("Revision", GeneralInfoRow::revisionProperty, inheritedOfferFieldsEditable),
                textColumn("Customer", GeneralInfoRow::customerNameProperty, true),
                textColumn("Your request", GeneralInfoRow::requestNrProperty, inheritedOfferFieldsEditable),
                textColumn("Document reference", GeneralInfoRow::referenceDocProperty, true),
                textColumn("Description", GeneralInfoRow::descriptionProperty, true),
                textColumn("Drawing", GeneralInfoRow::drawingProperty, true),
                textColumn("Drawing Rev.", GeneralInfoRow::drawingRevProperty, true),
                integerColumn("Quantity / Year", GeneralInfoRow::quantityPerYearProperty, true),
                integerColumn("Pr. Batch", GeneralInfoRow::quantityPerBatchProperty, true),
                dateColumn("First Delivery Date", GeneralInfoRow::firstDeliveryDateProperty, true),
                dateColumn("Last Delivery Date", GeneralInfoRow::lastDeliveryDateProperty, true),
                integerColumn("Delivery Batch", GeneralInfoRow::deliveryBatchProperty, true)
        ));
    }

    public void loadInitialData(
            GeneralInfoRow row,
            String offerNr,
            LocalDate offerDate,
            String revision,
            String requestNr,
            String customerName,
            String referenceDoc
    ) {
        row.setCustomerName(customerName != null ? customerName : "");
        row.setRequestNr(requestNr);
        row.setOfferNr(offerNr);
        row.setOfferDate(offerDate);
        row.setRevision(revision);
        row.setReferenceDoc(referenceDoc);
        row.setDescription("");
        row.setDrawing("");
        row.setDrawingRev("");
        row.setQuantityPerYear(null);
        row.setQuantityPerBatch(null);
        row.setFirstDeliveryDate(null);
        row.setLastDeliveryDate(null);
        row.setDeliveryBatch(null);

        table.setItems(FXCollections.observableArrayList(row));
    }

    private static TableColumn<GeneralInfoRow, String> textColumn(
            String title,
            Function<GeneralInfoRow, StringProperty> propertyExtractor,
            boolean editable
    ) {
        TableColumn<GeneralInfoRow, String> col = new TableColumn<>(title);
        col.setEditable(editable);
        col.setCellValueFactory(cd -> propertyExtractor.apply(cd.getValue()));

        if (editable) {
            col.setCellFactory(tc -> new EditingTextCell<>(row -> true, row -> false));
            col.setOnEditCommit(e -> {
                GeneralInfoRow row = e.getRowValue();
                if (row != null) {
                    propertyExtractor.apply(row).set(e.getNewValue());
                }
            });
        }

        return col;
    }

    private static TableColumn<GeneralInfoRow, LocalDate> dateColumn(
            String title,
            Function<GeneralInfoRow, ObjectProperty<LocalDate>> propertyExtractor,
            boolean editable
    ) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        StringConverter<LocalDate> converter = new LocalDateStringConverter(formatter, formatter);

        TableColumn<GeneralInfoRow, LocalDate> col = new TableColumn<>(title);
        col.setEditable(editable);
        col.setCellValueFactory(cd -> propertyExtractor.apply(cd.getValue()));

        if (editable) {
            col.setCellFactory(TextFieldTableCell.forTableColumn(converter));
            col.setOnEditCommit(e -> {
                GeneralInfoRow row = e.getRowValue();
                if (row != null) {
                    propertyExtractor.apply(row).set(e.getNewValue());
                }
            });
        } else {
            col.setCellFactory(tc -> new TableCell<>() {
                @Override
                protected void updateItem(LocalDate item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : formatter.format(item));
                }
            });
        }

        return col;
    }

    private static TableColumn<GeneralInfoRow, Integer> integerColumn(
            String title,
            Function<GeneralInfoRow, ObjectProperty<Integer>> propertyExtractor,
            boolean editable
    ) {
        StringConverter<Integer> converter = new StringConverter<>() {
            @Override
            public String toString(Integer value) {
                return value == null ? "" : value.toString();
            }

            @Override
            public Integer fromString(String text) {
                if (text == null || text.trim().isEmpty()) {
                    return null;
                }
                return Integer.valueOf(text.trim());
            }
        };

        TableColumn<GeneralInfoRow, Integer> col = new TableColumn<>(title);
        col.setEditable(editable);
        col.setCellValueFactory(cd -> propertyExtractor.apply(cd.getValue()));

        if (editable) {
            col.setCellFactory(tc -> new EditingIntegerCell<>(converter, row -> true, row -> false));
            col.setOnEditCommit(e -> {
                GeneralInfoRow row = e.getRowValue();
                if (row != null) {
                    propertyExtractor.apply(row).set(e.getNewValue());
                }
            });
        }

        return col;
    }

    public GeneralInfoRow getRow() {
        return table.getItems().isEmpty() ? null : table.getItems().get(0);
    }

    public GIExportModel extractDto() {
        GeneralInfoRow row = getRow();
        if (row == null) {
            return null;
        }

        GIExportModel dto = new GIExportModel();
        dto.setCustomerName(row.getCustomerName());
        dto.setRequestNr(row.getRequestNr());
        dto.setOfferNr(row.getOfferNr());
        dto.setOfferDate(row.getOfferDate());
        dto.setRevision(row.getRevision());
        dto.setReferenceDoc(row.getReferenceDoc());
        dto.setDescription(row.getDescription());
        dto.setDrawing(row.getDrawing());
        dto.setDrawingRev(row.getDrawingRev());
        dto.setQuantityPerYear(row.getQuantityPerYear());
        dto.setQuantityPerBatch(row.getQuantityPerBatch());
        dto.setFirstDeliveryDate(row.getFirstDeliveryDate());
        dto.setLastDeliveryDate(row.getLastDeliveryDate());
        dto.setDeliveryBatch(row.getDeliveryBatch());
        return dto;
    }

    public void loadDto(GIExportModel dto) {
        GeneralInfoRow row = getRow();

        if (dto == null) {
            if (row != null) {
                row.setCustomerName("");
                row.setRequestNr("");
                row.setOfferNr("");
                row.setOfferDate(null);
                row.setRevision("");
                row.setReferenceDoc("");
                row.setDescription("");
                row.setDrawing("");
                row.setDrawingRev("");
                row.setQuantityPerYear(null);
                row.setQuantityPerBatch(null);
                row.setFirstDeliveryDate(null);
                row.setLastDeliveryDate(null);
                row.setDeliveryBatch(null);
            }
            return;
        }

        if (row == null) {
            row = new GeneralInfoRow();
            table.setItems(FXCollections.observableArrayList(row));
        }

        if (dto.getCustomerName() != null && !dto.getCustomerName().isBlank()) {
            row.setCustomerName(dto.getCustomerName());
        }
        row.setRequestNr(dto.getRequestNr());
        row.setOfferNr(dto.getOfferNr());
        row.setOfferDate(dto.getOfferDate());
        row.setRevision(dto.getRevision());
        row.setReferenceDoc(dto.getReferenceDoc());
        row.setDescription(dto.getDescription());
        row.setDrawing(dto.getDrawing());
        row.setDrawingRev(dto.getDrawingRev());
        row.setQuantityPerYear(dto.getQuantityPerYear());
        row.setQuantityPerBatch(dto.getQuantityPerBatch());
        row.setFirstDeliveryDate(dto.getFirstDeliveryDate());
        row.setLastDeliveryDate(dto.getLastDeliveryDate());
        row.setDeliveryBatch(dto.getDeliveryBatch());
    }

    public void applyOfferHeaderUpdate(OfferEntity updatedOffer, String updatedCustomerName) {
        if (updatedOffer == null) {
            return;
        }

        GeneralInfoRow row = getRow();
        if (row == null) {
            return;
        }

        row.setOfferNr(updatedOffer.getOfferNr());
        row.setOfferDate(updatedOffer.getOfferDate());
        row.setRevision(updatedOffer.getRevision());
        row.setRequestNr(updatedOffer.getRequestNr());

        if (updatedCustomerName != null && !updatedCustomerName.isBlank()) {
            row.setCustomerName(updatedCustomerName);
        }

        table.refresh();
    }
}