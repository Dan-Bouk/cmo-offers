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

import java.util.Objects;
import java.util.function.Predicate;

public class EditingIntegerCell<S> extends TableCell<S, Integer> {

    private final TextField textField = new TextField();
    private final StringConverter<Integer> converter;
    private final Predicate<S> editablePredicate;
    private final Predicate<S> hideDisplayPredicate;

    public EditingIntegerCell(StringConverter<Integer> converter,
                              Predicate<S> editablePredicate,
                              Predicate<S> hideDisplayPredicate) {
        this.converter = converter;
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
            setText(getItem() == null ? "" : getItem().toString());
        }
    }

    @Override
    public void commitEdit(Integer newValue) {
        if (!isEditing() && Objects.equals(newValue, getItem())) {
            super.commitEdit(newValue);
            setGraphic(null);
            setContentDisplay(ContentDisplay.TEXT_ONLY);
            setText(newValue == null ? "" : newValue.toString());
            return;
        }

        if (!isEditing()) {
            TableView<S> table = getTableView();
            TableColumn<S, Integer> column = getTableColumn();
            int rowIndex = getIndex();

            if (table != null
                    && column != null
                    && rowIndex >= 0
                    && rowIndex < table.getItems().size()
                    && getTableRow() != null
                    && getTableRow().getItem() != null) {

                TablePosition<S, Integer> position = new TablePosition<>(table, rowIndex, column);
                TableColumn.CellEditEvent<S, Integer> event =
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
        setText(newValue == null ? "" : newValue.toString());
    }

    @Override
    protected void updateItem(Integer item, boolean empty) {
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
            setText(item == null ? "" : item.toString());
            setGraphic(null);
            setContentDisplay(ContentDisplay.TEXT_ONLY);
        }
    }
}