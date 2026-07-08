package com.cmo.offers.model.table;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.Objects;
import java.util.function.Predicate;

public class EditingTextCell<S> extends TableCell<S, String> {

    private final TextField textField = new TextField();
    private final Predicate<S> editablePredicate;
    private final Predicate<S> hideDisplayPredicate;

    public EditingTextCell(Predicate<S> editablePredicate,
                           Predicate<S> hideDisplayPredicate) {
        this.editablePredicate = editablePredicate == null ? row -> true : editablePredicate;
        this.hideDisplayPredicate = hideDisplayPredicate == null ? row -> false : hideDisplayPredicate;

        textField.setOnAction(e -> commitEdit(textField.getText()));
        textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                commitEdit(textField.getText());
            }
        });
    }

    @Override
    public void startEdit() {
        S row = getTableRow() == null ? null : getTableRow().getItem();
        if (row == null || !editablePredicate.test(row)) {
            return;
        }

        super.startEdit();
        textField.setText(getItem() == null ? "" : getItem());
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
            setText(getItem() == null ? "" : getItem());
        }
    }

    @Override
    public void commitEdit(String newValue) {
        if (!isEditing() && Objects.equals(newValue, getItem())) {
            super.commitEdit(newValue);
            setGraphic(null);
            setContentDisplay(ContentDisplay.TEXT_ONLY);
            setText(newValue == null ? "" : newValue);
            return;
        }

        if (!isEditing()) {
            TableView<S> table = getTableView();
            TableColumn<S, String> column = getTableColumn();
            int rowIndex = getIndex();

            if (table != null
                    && column != null
                    && rowIndex >= 0
                    && rowIndex < table.getItems().size()
                    && getTableRow() != null
                    && getTableRow().getItem() != null) {

                TablePosition<S, String> position = new TablePosition<>(table, rowIndex, column);
                TableColumn.CellEditEvent<S, String> event =
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
        setText(newValue == null ? "" : newValue);
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);

        S row = getTableRow() == null ? null : getTableRow().getItem();

        if (empty || row == null) {
            setText(null);
            setGraphic(null);
        } else if (isEditing()) {
            textField.setText(item == null ? "" : item);
            setText(null);
            setGraphic(textField);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        } else if (hideDisplayPredicate.test(row)) {
            setText("");
            setGraphic(null);
            setContentDisplay(ContentDisplay.TEXT_ONLY);
        } else {
            setText(item == null ? "" : item);
            setGraphic(null);
            setContentDisplay(ContentDisplay.TEXT_ONLY);
        }
    }
}