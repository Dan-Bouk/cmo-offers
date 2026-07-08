package com.cmo.offers.ui.controller;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.DoubleConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.cmo.offers.model.TreatmentsExportModel;
import com.cmo.offers.model.row.TreatmentsRow;
import com.cmo.offers.model.table.EditingBigDecimalCell;
import com.cmo.offers.model.table.EditingIntegerCell;
import com.cmo.offers.model.table.EditingTextCell;
import com.cmo.offers.ui.service.TreatmentsService;

public class TreatmentsTableController {

    private final TableView<TreatmentsRow> table;
    private final TreatmentsService calculationService;
    private final Supplier<YearMonth> periodSupplier;

    private final ObservableList<TreatmentsRow> data = FXCollections.observableArrayList(
            row -> new Observable[] {
                    row.faseProperty(),
                    row.treatmentNameProperty(),
                    row.typeProperty(),
                    row.intExtProperty(),
                    row.centerProperty(),
                    row.operationCostProperty(),
                    row.operationMarkupProperty(),
                    row.silverQuantityGrProperty(),
                    row.silverMarkupProperty(),
                    row.operationPriceProperty(),
                    row.silverCostProperty(),
                    row.silverPriceProperty()
            }
    );

    private final ObservableList<TreatmentsRow> tableItems = FXCollections.observableArrayList();
    private final Set<TreatmentsRow> initializedRows =
            Collections.newSetFromMap(new IdentityHashMap<>());

    private final TreatmentsRow totalRow = new TreatmentsRow(true);

    public TreatmentsTableController(TableView<TreatmentsRow> table,
                                     TreatmentsService calculationService,
                                     Supplier<YearMonth> periodSupplier) {
        this.table = Objects.requireNonNull(table, "table must not be null");
        this.calculationService = Objects.requireNonNull(calculationService, "calculationService must not be null");
        this.periodSupplier = Objects.requireNonNull(periodSupplier, "periodSupplier must not be null");

        configure();
    }

    private void configure() {
        table.setEditable(true);
        table.setSortPolicy(tv -> false);

        configureColumns();
        configureRowStyle();

        totalRow.setTreatmentName("Total");

        rebuildTableItems();
        table.setItems(tableItems);

        data.addListener((ListChangeListener<TreatmentsRow>) c -> {
            boolean structureChanged = false;

            while (c.next()) {
                if (c.wasAdded()) {
                    structureChanged = true;
                    for (TreatmentsRow row : c.getAddedSubList()) {
                        safelyRecalculateRow(row);
                    }
                }
                if (c.wasRemoved()) {
                    structureChanged = true;
                }
            }

            recalculateSummaryRow();

            if (structureChanged) {
                rebuildTableItems();
            }
        });
    }

    private void configureColumns() {
        table.getColumns().setAll(List.of(
                editableIntegerColumn("Phase", TreatmentsRow::faseProperty, 70, false),
                editableTextColumn("Treatment", TreatmentsRow::treatmentNameProperty, 220, true),
                editableTextColumn("Type", TreatmentsRow::typeProperty, 100, false),
                editableEnumColumn("Int/Ext", TreatmentsRow::intExtProperty, TreatmentsRow.IntExt.class, 80, false),
                editableTextColumn("Center", TreatmentsRow::centerProperty, 100, false),
                editableBigDecimalColumn("Oper. Cost (€/pc)", TreatmentsRow::operationCostProperty, 120, 2, false),
                editableBigDecimalColumn("% Oper Ric", TreatmentsRow::operationMarkupProperty, 100, 2, false),
                readOnlyBigDecimalColumn("Oper. Price (€)", TreatmentsRow::operationPriceProperty, 120, 2, true),
                editableBigDecimalColumn("Q.ty Ag (gr)", TreatmentsRow::silverQuantityGrProperty, 100, 3, false),
                readOnlyBigDecimalColumn("Silver Cost (€)", TreatmentsRow::silverCostProperty, 120, 2, false),
                editableBigDecimalColumn("% Silver Ric", TreatmentsRow::silverMarkupProperty, 100, 2, false),
                readOnlyBigDecimalColumn("Silver Price (€)", TreatmentsRow::silverPriceProperty, 120, 2, true)
        ));
    }

    private void configureRowStyle() {
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(TreatmentsRow item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setStyle("");
                } else if (item.isSummary()) {
                    setStyle("-fx-font-weight: bold;");
                } else {
                    setStyle("");
                }
            }
        });
    }

    private void attachRowListeners(TreatmentsRow row) {
        if (row == null || row.isSummary() || initializedRows.contains(row)) {
            return;
        }

        initializedRows.add(row);

        row.faseProperty().addListener((obs, oldVal, newVal) -> recalculateRowAndSummary(row));
        row.treatmentNameProperty().addListener((obs, oldVal, newVal) -> recalculateRowAndSummary(row));
        row.typeProperty().addListener((obs, oldVal, newVal) -> recalculateRowAndSummary(row));
        row.intExtProperty().addListener((obs, oldVal, newVal) -> recalculateRowAndSummary(row));
        row.centerProperty().addListener((obs, oldVal, newVal) -> recalculateRowAndSummary(row));
        row.operationCostProperty().addListener((obs, oldVal, newVal) -> recalculateRowAndSummary(row));
        row.operationMarkupProperty().addListener((obs, oldVal, newVal) -> recalculateRowAndSummary(row));
        row.silverQuantityGrProperty().addListener((obs, oldVal, newVal) -> recalculateRowAndSummary(row));
        row.silverMarkupProperty().addListener((obs, oldVal, newVal) -> recalculateRowAndSummary(row));
    }

    private void recalculateRowAndSummary(TreatmentsRow row) {
        if (row == null || row.isSummary()) {
            return;
        }

        safelyRecalculateRow(row);
        recalculateSummaryRow();
    }

    private void recalculateAll() {
        for (TreatmentsRow row : data) {
            safelyRecalculateRow(row);
        }
        recalculateSummaryRow();
    }

    private void safelyRecalculateRow(TreatmentsRow row) {
        try {
            YearMonth period = currentPeriod();
            if (period != null) {
                calculationService.recalculateRow(row, period);
            } else {
                row.setOperationPrice(
                        nvl(row.getOperationCost())
                                .multiply(nvl(row.getOperationMarkup()))
                                .setScale(4, RoundingMode.HALF_UP)
                );
                row.setSilverCost(BigDecimal.ZERO);
                row.setSilverPrice(BigDecimal.ZERO);
            }
        } catch (SQLException | IllegalStateException ex) {
            row.setSilverCost(BigDecimal.ZERO);
            row.setSilverPrice(BigDecimal.ZERO);
        }
    }

    private void recalculateSummaryRow() {
        calculationService.recalculateTotalRow(totalRow, data);
    }

    private void rebuildTableItems() {
        tableItems.setAll(data);
        tableItems.add(totalRow);
    }

    private YearMonth currentPeriod() {
    	YearMonth value = periodSupplier.get();
        return value.minusMonths(1) == null ? null : value.minusMonths(1);
    }

    public void refreshForPeriodChange() {
        recalculateAll();
    }

    public void setRows(Collection<TreatmentsRow> rows) {
        data.clear();
        initializedRows.clear();

        if (rows != null) {
            for (TreatmentsRow row : rows) {
                if (row != null && !row.isSummary()) {
                    data.add(prepareRow(row));
                }
            }
        }

        recalculateSummaryRow();
        rebuildTableItems();
        table.refresh();
    }

    public void addRow(TreatmentsRow row) {
        if (row != null && !row.isSummary()) {
            data.add(prepareRow(row));
            recalculateSummaryRow();
            rebuildTableItems();
            table.refresh();
        }
    }

    public void addRows(Collection<TreatmentsRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }

        for (TreatmentsRow row : rows) {
            if (row != null && !row.isSummary()) {
                data.add(prepareRow(row));
            }
        }

        recalculateSummaryRow();
        rebuildTableItems();
        table.refresh();
    }

    public void addEmptyRow() {
        table.requestFocus();

        Platform.runLater(() -> {
            addRow(new TreatmentsRow());

            int rowIndex = data.size() - 1;
            table.requestFocus();
            table.scrollTo(rowIndex);
            table.getSelectionModel().clearAndSelect(rowIndex);

            if (table.getColumns().size() > 1) {
                table.edit(rowIndex, table.getColumns().get(1));
            } else if (!table.getColumns().isEmpty()) {
                table.edit(rowIndex, table.getColumns().get(0));
            }
        });
    }

    public void seedEmptyRows(int count) {
        data.clear();
        initializedRows.clear();

        for (int i = 0; i < count; i++) {
            data.add(prepareRow(new TreatmentsRow()));
        }

        recalculateSummaryRow();
        rebuildTableItems();
        table.refresh();
    }

    public ObservableList<TreatmentsRow> getData() {
        return data;
    }

    public TreatmentsRow getTotalRow() {
        return totalRow;
    }

    private TreatmentsRow prepareRow(TreatmentsRow row) {
        attachRowListeners(row);
        safelyRecalculateRow(row);
        return row;
    }

    private TableColumn<TreatmentsRow, String> editableTextColumn(
            String title,
            Function<TreatmentsRow, StringProperty> propertyExtractor,
            double prefWidth,
            boolean showInSummary
    ) {
        TableColumn<TreatmentsRow, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cd -> propertyExtractor.apply(cd.getValue()));
        col.setCellFactory(tc -> new EditingTextCell<>(
                row -> !row.isSummary(),
                row -> row.isSummary() && !showInSummary
        ));
        col.setOnEditCommit(e -> {
            TreatmentsRow row = e.getRowValue();
            if (row != null && !row.isSummary()) {
                propertyExtractor.apply(row).set(e.getNewValue());
            }
        });
        col.setPrefWidth(prefWidth);
        return col;
    }

    private TableColumn<TreatmentsRow, Integer> editableIntegerColumn(
            String title,
            Function<TreatmentsRow, ObjectProperty<Integer>> propertyExtractor,
            double prefWidth,
            boolean showInSummary
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

        TableColumn<TreatmentsRow, Integer> col = new TableColumn<>(title);
        col.setCellValueFactory(cd -> propertyExtractor.apply(cd.getValue()));
        col.setCellFactory(tc -> new EditingIntegerCell<>(
                converter,
                row -> !row.isSummary(),
                row -> row.isSummary() && !showInSummary
        ));
        col.setOnEditCommit(e -> {
            TreatmentsRow row = e.getRowValue();
            if (row != null && !row.isSummary()) {
                propertyExtractor.apply(row).set(e.getNewValue());
            }
        });
        col.setPrefWidth(prefWidth);
        return col;
    }

    private TableColumn<TreatmentsRow, BigDecimal> editableBigDecimalColumn(
            String title,
            Function<TreatmentsRow, ObjectProperty<BigDecimal>> propertyExtractor,
            double prefWidth,
            int scale,
            boolean showInSummary
    ) {
        StringConverter<BigDecimal> converter = new StringConverter<>() {
            @Override
            public String toString(BigDecimal value) {
                return value == null ? "" : value.setScale(scale, RoundingMode.HALF_UP).toPlainString();
            }

            @Override
            public BigDecimal fromString(String text) {
                if (text == null || text.trim().isEmpty()) {
                    return null;
                }
                return new BigDecimal(text.trim());
            }
        };

        TableColumn<TreatmentsRow, BigDecimal> col = new TableColumn<>(title);
        col.setCellValueFactory(cd -> propertyExtractor.apply(cd.getValue()));
        col.setCellFactory(tc -> new EditingBigDecimalCell<>(
                converter,
                scale,
                row -> !row.isSummary(),
                row -> row.isSummary() && !showInSummary
        ));
        col.setOnEditCommit(e -> {
            TreatmentsRow row = e.getRowValue();
            if (row != null && !row.isSummary()) {
                propertyExtractor.apply(row).set(e.getNewValue());
            }
        });
        col.setPrefWidth(prefWidth);
        return col;
    }

    private <E extends Enum<E>> TableColumn<TreatmentsRow, E> editableEnumColumn(
            String title,
            Function<TreatmentsRow, ObjectProperty<E>> propertyExtractor,
            Class<E> enumType,
            double prefWidth,
            boolean showInSummary
    ) {
        TableColumn<TreatmentsRow, E> col = new TableColumn<>(title);
        col.setCellValueFactory(cd -> propertyExtractor.apply(cd.getValue()));

        StringConverter<E> converter = enumConverter();
        ObservableList<E> enumValues = FXCollections.observableArrayList(enumType.getEnumConstants());

        col.setCellFactory(tc -> new TableCell<>() {

            private final ComboBox<E> comboBox = new ComboBox<>(enumValues);
            private Double savedScrollVvalue;
            private Double savedScrollHvalue;

            {
                comboBox.setConverter(converter);
                comboBox.setMaxWidth(Double.MAX_VALUE);

                comboBox.setOnAction(evt -> {
                    if (isEditing() && !Objects.equals(comboBox.getValue(), getItem())) {
                        commitEdit(comboBox.getValue());
                    }
                    evt.consume();
                });

                comboBox.focusedProperty().addListener((obs, oldVal, newVal) -> {
                    if (!newVal && isEditing() && !Objects.equals(comboBox.getValue(), getItem())) {
                        commitEdit(comboBox.getValue());
                    }
                });

                comboBox.showingProperty().addListener((obs, wasShowing, isShowing) -> {
                    if (isShowing) {
                        saveEnclosingScrollPosition();
                    } else {
                        restoreEnclosingScrollPosition();
                    }
                });

                comboBox.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        cancelEdit();
                        event.consume();
                    }
                });
            }

            private void saveEnclosingScrollPosition() {
                saveScrollPosition(this,
                        v -> savedScrollVvalue = v,
                        h -> savedScrollHvalue = h);
            }

            private void restoreEnclosingScrollPosition() {
                restoreScrollPosition(this, savedScrollVvalue, savedScrollHvalue);
            }

            @Override
            public void startEdit() {
                TreatmentsRow row = getTableRow() == null ? null : getTableRow().getItem();
                if (row == null || row.isSummary()) {
                    return;
                }

                saveEnclosingScrollPosition();
                super.startEdit();

                comboBox.setValue(getItem());
                setText(null);
                setGraphic(comboBox);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

                Platform.runLater(() -> {
                    saveEnclosingScrollPosition();
                    setGraphic(comboBox);
                    comboBox.show();
                });
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                showEnumAsText(this, getItem(), showInSummary, converter);
                restoreEnclosingScrollPosition();
            }

            @Override
            public void commitEdit(E newValue) {
                if (!isEditing() && Objects.equals(newValue, getItem())) {
                    super.commitEdit(newValue);
                    showEnumAsText(this, newValue, showInSummary, converter);
                    restoreEnclosingScrollPosition();
                    return;
                }

                if (!isEditing()) {
                    TableView<TreatmentsRow> table = getTableView();
                    TableColumn<TreatmentsRow, E> column = getTableColumn();
                    int rowIndex = getIndex();

                    if (table != null
                            && column != null
                            && rowIndex >= 0
                            && rowIndex < table.getItems().size()
                            && getTableRow() != null
                            && getTableRow().getItem() != null) {

                        TablePosition<TreatmentsRow, E> position =
                                new TablePosition<>(table, rowIndex, column);

                        TableColumn.CellEditEvent<TreatmentsRow, E> event =
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
                showEnumAsText(this, newValue, showInSummary, converter);
                restoreEnclosingScrollPosition();
            }

            @Override
            protected void updateItem(E item, boolean empty) {
                super.updateItem(item, empty);

                TreatmentsRow row = getTableRow() == null ? null : getTableRow().getItem();

                if (empty || row == null) {
                    setText(null);
                    setGraphic(null);
                } else if (isEditing()) {
                    comboBox.setValue(item);
                    setText(null);
                    setGraphic(comboBox);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                } else {
                    showEnumAsText(this, item, showInSummary, converter);
                }
            }
        });

        col.setOnEditCommit(e -> {
            TreatmentsRow row = e.getRowValue();
            if (row != null && !row.isSummary()) {
                propertyExtractor.apply(row).set(e.getNewValue());
            }
        });

        col.setPrefWidth(prefWidth);
        return col;
    }

    private TableColumn<TreatmentsRow, BigDecimal> readOnlyBigDecimalColumn(
            String title,
            Function<TreatmentsRow, ObservableValue<BigDecimal>> propertyExtractor,
            double prefWidth,
            int scale,
            boolean showInSummary
    ) {
        TableColumn<TreatmentsRow, BigDecimal> col = new TableColumn<>(title);
        col.setCellValueFactory(cd -> propertyExtractor.apply(cd.getValue()));
        col.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);

                TreatmentsRow row = getTableRow() == null ? null : getTableRow().getItem();

                if (empty || row == null) {
                    setText(null);
                } else if (row.isSummary() && !showInSummary) {
                    setText("");
                } else {
                    setText(item == null ? "" : item.setScale(scale, RoundingMode.HALF_UP).toPlainString());
                }
            }
        });
        col.setPrefWidth(prefWidth);
        return col;
    }

    private <E extends Enum<E>> void showEnumAsText(
            TableCell<TreatmentsRow, E> cell,
            E item,
            boolean showInSummary,
            StringConverter<E> converter
    ) {
        TreatmentsRow row = cell.getTableRow() == null ? null : cell.getTableRow().getItem();

        cell.setGraphic(null);
        cell.setContentDisplay(ContentDisplay.TEXT_ONLY);

        if (row == null) {
            cell.setText(null);
        } else if (row.isSummary() && !showInSummary) {
            cell.setText("");
        } else {
            cell.setText(converter.toString(item));
        }
    }

    private <E extends Enum<E>> StringConverter<E> enumConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(E value) {
                return value == null ? "" : value.toString();
            }

            @Override
            public E fromString(String string) {
                return null;
            }
        };
    }
    
    private void saveScrollPosition(Node node, DoubleConsumer saveV, DoubleConsumer saveH) {
        ScrollPane scrollPane = findEnclosingScrollPane(node);
        if (scrollPane != null) {
            saveV.accept(scrollPane.getVvalue());
            saveH.accept(scrollPane.getHvalue());
        }
    }
    
    private void restoreScrollPosition(Node node, Double savedV, Double savedH) {
        if (savedV == null || savedH == null) {
            return;
        }

        ScrollPane scrollPane = findEnclosingScrollPane(node);
        if (scrollPane != null) {
            Platform.runLater(() -> {
                scrollPane.setVvalue(savedV);
                scrollPane.setHvalue(savedH);

                Platform.runLater(() -> {
                    scrollPane.setVvalue(savedV);
                    scrollPane.setHvalue(savedH);
                });
            });
        }
    }
    
    
    private ScrollPane findEnclosingScrollPane(Node node) {
        Parent current = node == null ? null : node.getParent();

        while (current != null) {
            if (current instanceof ScrollPane scrollPane) {
                return scrollPane;
            }
            current = current.getParent();
        }

        return null;
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
    
    public List<TreatmentsExportModel> extractDtos() {
        List<TreatmentsExportModel> result = new ArrayList<>();

        for (TreatmentsRow row : data) {
            if (row == null || row.isSummary()) {
                continue;
            }

            TreatmentsExportModel dto = new TreatmentsExportModel();
            dto.setFase(row.getFase());
            dto.setTreatmentName(row.getTreatmentName());
            dto.setType(row.getType());
            dto.setIntExt(row.getIntExt() == null ? null : row.getIntExt().name());
            dto.setCenter(row.getCenter());
            dto.setOperationCost(row.getOperationCost());
            dto.setOperationMarkup(row.getOperationMarkup());
            dto.setSilverQuantityGr(row.getSilverQuantityGr());
            dto.setSilverMarkup(row.getSilverMarkup());
            dto.setOperationPrice(row.getOperationPrice());
            dto.setSilverCost(row.getSilverCost());
            dto.setSilverPrice(row.getSilverPrice());

            result.add(dto);
        }

        return result;
    }
    
    public void loadDtos(Collection<TreatmentsExportModel> dtos) {
        data.clear();
        initializedRows.clear();

        if (dtos != null) {
            for (TreatmentsExportModel dto : dtos) {
                if (dto == null) {
                    continue;
                }

                TreatmentsRow row = new TreatmentsRow();
                row.setFase(dto.getFase());
                row.setTreatmentName(dto.getTreatmentName());
                row.setType(dto.getType());
                row.setIntExt(parseIntExt(dto.getIntExt()));
                row.setCenter(dto.getCenter());
                row.setOperationCost(dto.getOperationCost());
                row.setOperationMarkup(dto.getOperationMarkup());
                row.setSilverQuantityGr(dto.getSilverQuantityGr());
                row.setSilverMarkup(dto.getSilverMarkup());

                data.add(prepareRow(row));
            }
        }

        recalculateSummaryRow();
        rebuildTableItems();
        table.refresh();
    }
    
    private TreatmentsRow.IntExt parseIntExt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return TreatmentsRow.IntExt.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}