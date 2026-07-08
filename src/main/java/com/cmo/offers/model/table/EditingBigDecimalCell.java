package com.cmo.offers.model.table;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.function.Predicate;

public class EditingBigDecimalCell<S> extends TableCell<S, BigDecimal> {

    private final TextField textField = new TextField();
    private final StringConverter<BigDecimal> converter;
    private final int scale;
    private final Predicate<S> editablePredicate;
    private final Predicate<S> hideDisplayPredicate;

    public EditingBigDecimalCell(StringConverter<BigDecimal> converter,
                                 int scale,
                                 Predicate<S> editablePredicate,
                                 Predicate<S> hideDisplayPredicate) {
        this.converter = converter;
        this.scale = scale;
        this.editablePredicate = editablePredicate == null ? row -> true : editablePredicate;
        this.hideDisplayPredicate = hideDisplayPredicate == null ? row -> false : hideDisplayPredicate;

        textField.setOnAction(e -> commitFromText());
        textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                commitFromText();
            }
        });
    }

    private void commitFromText() {
        try {
            commitEdit(converter.fromString(textField.getText()));
        } catch (Exception ex) {
            cancelEdit();
        }
    }

    @Override
    public void startEdit() {
        S row = getTableRow() == null ? null : getTableRow().getItem();
        if (row == null || !editablePredicate.test(row)) {
            return;
        }

        super.startEdit();
        textField.setText(converter.toString(getItem()));
        setText(null);
        setGraphic(textField);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        Platform.runLater(() -> {
            textField.requestFocus();
            textField.selectAll();
        });
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setGraphic(null);
        setContentDisplay(ContentDisplay.TEXT_ONLY);

        S row = getTableRow() == null ? null : getTableRow().getItem();
        if (row == null) {
            setText(null);
        } else if (hideDisplayPredicate.test(row)) {
            setText("");
        } else {
            setText(format(getItem()));
        }
    }

    @Override
    public void commitEdit(BigDecimal newValue) {
        if (!isEditing() && Objects.equals(newValue, getItem())) {
            super.commitEdit(newValue);
            setGraphic(null);
            setContentDisplay(ContentDisplay.TEXT_ONLY);
            setText(format(newValue));
            return;
        }

        if (!isEditing()) {
            TableView<S> table = getTableView();
            TableColumn<S, BigDecimal> column = getTableColumn();
            int rowIndex = getIndex();

            if (table != null
                    && column != null
                    && rowIndex >= 0
                    && rowIndex < table.getItems().size()
                    && getTableRow() != null
                    && getTableRow().getItem() != null) {

                TablePosition<S, BigDecimal> position = new TablePosition<>(table, rowIndex, column);
                TableColumn.CellEditEvent<S, BigDecimal> event =
                        new TableColumn.CellEditEvent<>(
                                table,
                                position,
                                TableColumn.editCommitEvent(),
                                newValue
                        );

                Event.fireEvent(column, event);
            }
        }

        super.commitEdit(newValue);
        setGraphic(null);
        setContentDisplay(ContentDisplay.TEXT_ONLY);
        setText(format(newValue));
    }

    @Override
    protected void updateItem(BigDecimal item, boolean empty) {
        super.updateItem(item, empty);

        S row = getTableRow() == null ? null : getTableRow().getItem();

        if (empty || row == null) {
            setText(null);
            setGraphic(null);
        } else if (isEditing()) {
            textField.setText(converter.toString(item));
            setText(null);
            setGraphic(textField);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        } else if (hideDisplayPredicate.test(row)) {
            setText("");
            setGraphic(null);
            setContentDisplay(ContentDisplay.TEXT_ONLY);
        } else {
            setText(format(item));
            setGraphic(null);
            setContentDisplay(ContentDisplay.TEXT_ONLY);
        }
    }

    private String format(BigDecimal value) {
        return value == null ? "" : value.setScale(scale, RoundingMode.HALF_UP).toPlainString();
    }
}