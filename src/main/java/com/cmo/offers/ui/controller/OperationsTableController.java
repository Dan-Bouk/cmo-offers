package com.cmo.offers.ui.controller;

import javafx.application.Platform;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.DoubleConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.cmo.offers.model.OperationsExportModel;
import com.cmo.offers.model.row.GeneralInfoRow;
import com.cmo.offers.model.row.OperationsRow;
import com.cmo.offers.model.table.EditingBigDecimalCell;
import com.cmo.offers.model.table.EditingIntegerCell;
import com.cmo.offers.model.table.EditingTextCell;
import com.cmo.offers.ui.service.OperationsService;

public class OperationsTableController {

    private final TableView<OperationsRow> table;
    private final GeneralInfoRow generalInfoRow;
    private final OperationsService calculationService = new OperationsService();

    private final ObservableList<OperationsRow> data = FXCollections.observableArrayList();

    /**
     * Visible rows in the table:
     * [data rows..., totalMachiningRow, totalAdditionalRow]
     */
    private final ObservableList<OperationsRow> tableItems = FXCollections.observableArrayList();

    private final Set<OperationsRow> initializedRows =
            Collections.newSetFromMap(new IdentityHashMap<>());

    private final OperationsRow totalMachiningRow =
            new OperationsRow(true, OperationsRow.SummaryKind.TOTAL_MACHINING);

    private final OperationsRow totalAdditionalRow =
            new OperationsRow(true, OperationsRow.SummaryKind.TOTAL_ADDITIONAL_OPERATIONS);

    private boolean syncScheduled = false;
    private boolean ensuringDefaultRow = false;

    public OperationsTableController(TableView<OperationsRow> table, GeneralInfoRow generalInfoRow) {
        this.table = Objects.requireNonNull(table, "table must not be null");
        this.generalInfoRow = Objects.requireNonNull(generalInfoRow, "generalInfoRow must not be null");
        configure();
    }

    private void configure() {
        totalMachiningRow.setOperationName("Total Machining");
        totalAdditionalRow.setOperationName("Total Additional Operations");

        table.setEditable(true);
        table.setSortPolicy(tv -> false);

        configureColumns();
        configureRowStyle();

        tableItems.setAll(totalMachiningRow, totalAdditionalRow);
        table.setItems(tableItems);

        data.addListener((ListChangeListener<OperationsRow>) change -> {
            boolean structureChanged = false;

            while (change.next()) {
                if (change.wasAdded()) {
                    structureChanged = true;
                    for (OperationsRow row : change.getAddedSubList()) {
                        calculationService.recalculateRow(row, generalInfoRow);
                    }
                }

                if (change.wasRemoved()) {
                    structureChanged = true;
                }
            }

            applyDefaultRowOverrides();
            calculationService.recalculateSummaryRows(data, totalMachiningRow, totalAdditionalRow);

            if (structureChanged) {
                ensureDefaultFirstRowIfEmpty();
                scheduleTableItemsSync();
            }
        });

        generalInfoRow.quantityPerBatchProperty().addListener((obs, oldVal, newVal) -> recalculateAll());
    }

    private void configureColumns() {
        table.getColumns().setAll(List.of(
                editableIntegerColumn("Phase", OperationsRow::faseProperty, 70, row -> false),
                editableTextColumn("Operation", OperationsRow::operationNameProperty, 220, row -> true),
                editableEnumColumn("Type", OperationsRow::typeProperty, OperationsRow.Type.class, 80, row -> false),
                editableEnumColumn("Int/Ext", OperationsRow::intExtProperty, OperationsRow.IntExt.class, 80, row -> false),
                editableTextColumn("Center", OperationsRow::centerProperty, 100, row -> false),
                editableBigDecimalColumn("Cost", OperationsRow::costProperty, 100, 2, row -> false),
                editableBigDecimalColumn("Setup min", OperationsRow::setupMinutesProperty, 100, 3, row -> false),
                editableBigDecimalColumn("Prod sec", OperationsRow::productionSecondsProperty, 100, 3, row -> false),
                readOnlyBigDecimalColumn("Setup Cost", OperationsRow::setupCostProperty, 110, 2, row -> false),
                readOnlyBigDecimalColumn("Prod Cost", OperationsRow::prodCostProperty, 110, 2, row -> false),
                editableBigDecimalColumn("% Ric", OperationsRow::markupProperty, 90, 2, row -> false),
                readOnlyBigDecimalColumn(
                        "Setup Price",
                        OperationsRow::setupPriceProperty,
                        110,
                        2,
                        row -> row.getSummaryKind() == OperationsRow.SummaryKind.TOTAL_MACHINING
                ),
                readOnlyBigDecimalColumn("Prod Price", OperationsRow::prodPriceProperty, 110, 2, row -> true)
        ));
    }

    private void configureRowStyle() {
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(OperationsRow item, boolean empty) {
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

    private void scheduleTableItemsSync() {
        if (syncScheduled) {
            return;
        }

        syncScheduled = true;

        Platform.runLater(() -> {
            syncScheduled = false;
            syncTableItemsFromData();
        });
    }

    private void syncTableItemsFromData() {
        ObservableList<OperationsRow> fresh = FXCollections.observableArrayList();
        fresh.addAll(data);
        fresh.add(totalMachiningRow);
        fresh.add(totalAdditionalRow);
        table.setItems(fresh);
    }

    private OperationsRow createDefaultFirstRow() {
        return calculationService.createDefaultFirstRow(generalInfoRow);
    }

    private OperationsRow createEmptyRow() {
        int phase = calculationService.nextAutoPhase(data);
        return calculationService.createEmptyRow(phase, generalInfoRow);
    }

    private OperationsRow createEmptyRow(int phase) {
        return calculationService.createEmptyRow(phase, generalInfoRow);
    }

    private Optional<OperationsRow> findDefaultDesignRow() {
        return data.stream()
                .filter(Objects::nonNull)
                .filter(row -> !row.isSummary())
                .filter(OperationsRow::isDefaultDesignRow)
                .findFirst();
    }

    private void applyDefaultRowOverrides() {
        findDefaultDesignRow().ifPresent(calculationService::applyDefaultFirstRowOverrides);
    }

    private void ensureDefaultFirstRowIfEmpty() {
        if (ensuringDefaultRow || !data.isEmpty()) {
            return;
        }

        ensuringDefaultRow = true;
        try {
            data.add(prepareRow(createDefaultFirstRow()));
        } finally {
            ensuringDefaultRow = false;
        }
    }

    private void attachRowListeners(OperationsRow row) {
        if (row == null || row.isSummary() || initializedRows.contains(row)) {
            return;
        }

        initializedRows.add(row);

        row.faseProperty().addListener((obs, oldVal, newVal) -> recalculateRowAndSummaries(row));
        row.operationNameProperty().addListener((obs, oldVal, newVal) -> recalculateRowAndSummaries(row));
        row.typeProperty().addListener((obs, oldVal, newVal) -> recalculateRowAndSummaries(row));
        row.intExtProperty().addListener((obs, oldVal, newVal) -> recalculateRowAndSummaries(row));
        row.centerProperty().addListener((obs, oldVal, newVal) -> recalculateRowAndSummaries(row));
        row.costProperty().addListener((obs, oldVal, newVal) -> recalculateRowAndSummaries(row));
        row.setupMinutesProperty().addListener((obs, oldVal, newVal) -> recalculateRowAndSummaries(row));
        row.productionSecondsProperty().addListener((obs, oldVal, newVal) -> recalculateRowAndSummaries(row));
        row.markupProperty().addListener((obs, oldVal, newVal) -> recalculateRowAndSummaries(row));
    }

    private void recalculateRowAndSummaries(OperationsRow row) {
        if (row == null || row.isSummary()) {
            return;
        }

        calculationService.recalculateRow(row, generalInfoRow);
        applyDefaultRowOverrides();
        calculationService.recalculateSummaryRows(data, totalMachiningRow, totalAdditionalRow);
    }

    private void recalculateAll() {
        calculationService.recalculateAll(data, generalInfoRow, totalMachiningRow, totalAdditionalRow);
        applyDefaultRowOverrides();
        calculationService.recalculateSummaryRows(data, totalMachiningRow, totalAdditionalRow);
    }

    public void setRows(Collection<OperationsRow> rows) {
        initializedRows.clear();

        List<OperationsRow> preparedRows = rows == null
                ? List.of()
                : rows.stream()
                        .filter(Objects::nonNull)
                        .filter(row -> !row.isSummary())
                        .map(this::prepareRow)
                        .collect(Collectors.toList());

        if (preparedRows.isEmpty()) {
            preparedRows = List.of(prepareRow(createDefaultFirstRow()));
        }

        data.setAll(preparedRows);
        applyDefaultRowOverrides();
        calculationService.recalculateSummaryRows(data, totalMachiningRow, totalAdditionalRow);
        syncTableItemsFromData();
    }

    public void addRow(OperationsRow row) {
        if (row != null && !row.isSummary()) {
            data.add(prepareRow(row));
        }
    }

    public void addRows(Collection<OperationsRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }

        List<OperationsRow> preparedRows = rows.stream()
                .filter(Objects::nonNull)
                .filter(row -> !row.isSummary())
                .map(this::prepareRow)
                .collect(Collectors.toList());

        if (!preparedRows.isEmpty()) {
            data.addAll(preparedRows);
        }
    }

    public void addEmptyRow() {
        addRow(createEmptyRow());
        Platform.runLater(() -> focusAndEditRow(data.size() - 1));
    }

    public void seedEmptyRows(int count) {
        initializedRows.clear();

        List<OperationsRow> rows = new ArrayList<>();

        if (count > 0) {
            rows.add(prepareRow(createDefaultFirstRow()));
        }

        for (int i = 1; i < count; i++) {
            int phase = calculationService.nextAutoPhase(rows);
            rows.add(prepareRow(createEmptyRow(phase)));
        }

        if (rows.isEmpty()) {
            rows.add(prepareRow(createDefaultFirstRow()));
        }

        data.setAll(rows);
        applyDefaultRowOverrides();
        calculationService.recalculateSummaryRows(data, totalMachiningRow, totalAdditionalRow);
        syncTableItemsFromData();
    }

    public ObservableList<OperationsRow> getData() {
        return data;
    }

    public OperationsRow getTotalMachiningRow() {
        return totalMachiningRow;
    }

    public OperationsRow getTotalAdditionalRow() {
        return totalAdditionalRow;
    }

    private OperationsRow prepareRow(OperationsRow row) {
        attachRowListeners(row);
        calculationService.recalculateRow(row, generalInfoRow);
        applyDefaultRowOverrides();
        return row;
    }

    private void focusAndEditRow(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= data.size()) {
            return;
        }

        table.requestFocus();
        table.scrollTo(rowIndex);
        table.getSelectionModel().clearAndSelect(rowIndex);

        if (table.getColumns().size() > 1) {
            table.edit(rowIndex, table.getColumns().get(1));
        } else if (!table.getColumns().isEmpty()) {
            table.edit(rowIndex, table.getColumns().get(0));
        }
    }

    private TableColumn<OperationsRow, String> editableTextColumn(
            String title,
            Function<OperationsRow, StringProperty> propertyExtractor,
            double prefWidth,
            Predicate<OperationsRow> showInSummary
    ) {
        TableColumn<OperationsRow, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cd -> propertyExtractor.apply(cd.getValue()));
        col.setCellFactory(tc -> new EditingTextCell<>(
                row -> !row.isSummary(),
                row -> row.isSummary() && !showInSummary.test(row)
        ));
        col.setOnEditCommit(e -> {
            OperationsRow row = e.getRowValue();
            if (row != null && !row.isSummary()) {
                propertyExtractor.apply(row).set(e.getNewValue());
            }
        });
        col.setPrefWidth(prefWidth);
        return col;
    }

    private TableColumn<OperationsRow, Integer> editableIntegerColumn(
            String title,
            Function<OperationsRow, ObjectProperty<Integer>> propertyExtractor,
            double prefWidth,
            Predicate<OperationsRow> showInSummary
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

        TableColumn<OperationsRow, Integer> col = new TableColumn<>(title);
        col.setCellValueFactory(cd -> propertyExtractor.apply(cd.getValue()));
        col.setCellFactory(tc -> new EditingIntegerCell<>(
                converter,
                row -> !row.isSummary(),
                row -> row.isSummary() && !showInSummary.test(row)
        ));
        col.setOnEditCommit(e -> {
            OperationsRow row = e.getRowValue();
            if (row != null && !row.isSummary()) {
                propertyExtractor.apply(row).set(e.getNewValue());
            }
        });
        col.setPrefWidth(prefWidth);
        return col;
    }

    private TableColumn<OperationsRow, BigDecimal> editableBigDecimalColumn(
            String title,
            Function<OperationsRow, ObjectProperty<BigDecimal>> propertyExtractor,
            double prefWidth,
            int scale,
            Predicate<OperationsRow> showInSummary
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

        TableColumn<OperationsRow, BigDecimal> col = new TableColumn<>(title);
        col.setCellValueFactory(cd -> propertyExtractor.apply(cd.getValue()));
        col.setCellFactory(tc -> new EditingBigDecimalCell<>(
                converter,
                scale,
                row -> !row.isSummary(),
                row -> row.isSummary() && !showInSummary.test(row)
        ));
        col.setOnEditCommit(e -> {
            OperationsRow row = e.getRowValue();
            if (row != null && !row.isSummary()) {
                propertyExtractor.apply(row).set(e.getNewValue());
            }
        });
        col.setPrefWidth(prefWidth);
        return col;
    }

    private <E extends Enum<E>> TableColumn<OperationsRow, E> editableEnumColumn(
            String title,
            Function<OperationsRow, ObjectProperty<E>> propertyExtractor,
            Class<E> enumType,
            double prefWidth,
            Predicate<OperationsRow> showInSummary
    ) {
        TableColumn<OperationsRow, E> col = new TableColumn<>(title);
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
                OperationsRow row = getTableRow() == null ? null : getTableRow().getItem();
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
                    TableView<OperationsRow> table = getTableView();
                    TableColumn<OperationsRow, E> column = getTableColumn();
                    int rowIndex = getIndex();

                    if (table != null
                            && column != null
                            && rowIndex >= 0
                            && rowIndex < table.getItems().size()
                            && getTableRow() != null
                            && getTableRow().getItem() != null) {

                        TablePosition<OperationsRow, E> position =
                                new TablePosition<>(table, rowIndex, column);

                        TableColumn.CellEditEvent<OperationsRow, E> event =
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

                OperationsRow row = getTableRow() == null ? null : getTableRow().getItem();

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
            OperationsRow row = e.getRowValue();
            if (row != null && !row.isSummary()) {
                propertyExtractor.apply(row).set(e.getNewValue());
            }
        });

        col.setPrefWidth(prefWidth);
        return col;
    }

    private TableColumn<OperationsRow, BigDecimal> readOnlyBigDecimalColumn(
            String title,
            Function<OperationsRow, ObservableValue<BigDecimal>> propertyExtractor,
            double prefWidth,
            int scale,
            Predicate<OperationsRow> showInSummary
    ) {
        TableColumn<OperationsRow, BigDecimal> col = new TableColumn<>(title);
        col.setCellValueFactory(cd -> propertyExtractor.apply(cd.getValue()));
        col.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);

                OperationsRow row = getTableRow() == null ? null : getTableRow().getItem();

                if (empty || row == null) {
                    setText(null);
                } else if (row.isSummary() && !showInSummary.test(row)) {
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
            TableCell<OperationsRow, E> cell,
            E item,
            Predicate<OperationsRow> showInSummary,
            StringConverter<E> converter
    ) {
        OperationsRow row = cell.getTableRow() == null ? null : cell.getTableRow().getItem();

        cell.setGraphic(null);
        cell.setContentDisplay(ContentDisplay.TEXT_ONLY);

        if (row == null) {
            cell.setText(null);
        } else if (row.isSummary() && !showInSummary.test(row)) {
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
    
    public List<OperationsExportModel> extractDtos() {
        List<OperationsExportModel> result = new ArrayList<>();

        for (OperationsRow row : data) {
            if (row == null || row.isSummary()) {
                continue;
            }

            OperationsExportModel dto = new OperationsExportModel();
            dto.setFase(row.getFase());
            dto.setOperationName(row.getOperationName());
            dto.setType(row.getType() == null ? null : row.getType().name());
            dto.setIntExt(row.getIntExt() == null ? null : row.getIntExt().name());
            dto.setCenter(row.getCenter());
            dto.setCost(row.getCost());
            dto.setSetupMinutes(row.getSetupMinutes());
            dto.setProductionSeconds(row.getProductionSeconds());
            dto.setMarkup(row.getMarkup());
            dto.setSetupCost(row.getSetupCost());
            dto.setProdCost(row.getProdCost());
            dto.setSetupPrice(row.getSetupPrice());
            dto.setProdPrice(row.getProdPrice());
            dto.setDefaultDesignRow(row.isDefaultDesignRow());

            result.add(dto);
        }

        return result;
    }
    
    public void loadDtos(Collection<OperationsExportModel> dtos) {
        initializedRows.clear();

        List<OperationsRow> rows = new ArrayList<>();

        if (dtos != null) {
            for (OperationsExportModel dto : dtos) {
                if (dto == null) {
                    continue;
                }

                OperationsRow row = new OperationsRow();
                row.setFase(dto.getFase());
                row.setOperationName(dto.getOperationName());
                row.setType(parseType(dto.getType()));
                row.setIntExt(parseIntExt(dto.getIntExt()));
                row.setCenter(dto.getCenter());
                row.setCost(dto.getCost());
                row.setSetupMinutes(dto.getSetupMinutes());
                row.setProductionSeconds(dto.getProductionSeconds());
                row.setMarkup(dto.getMarkup());
                row.setDefaultDesignRow(dto.isDefaultDesignRow());

                rows.add(prepareRow(row));
            }
        }

        if (rows.isEmpty()) {
            rows.add(prepareRow(createDefaultFirstRow()));
        }

        data.setAll(rows);
        applyDefaultRowOverrides();
        calculationService.recalculateSummaryRows(data, totalMachiningRow, totalAdditionalRow);
        syncTableItemsFromData();
    }
    
    private OperationsRow.Type parseType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return OperationsRow.Type.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
    
    private OperationsRow.IntExt parseIntExt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return OperationsRow.IntExt.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}