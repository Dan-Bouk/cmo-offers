package com.cmo.offers.ui.controller;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
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
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import com.cmo.offers.dao.MaterialDAO;
import com.cmo.offers.entity.ClientEntity;
import com.cmo.offers.entity.MaterialEntity;
import com.cmo.offers.entity.PlantEntity;
import com.cmo.offers.model.RMExportModel;
import com.cmo.offers.model.row.RawMaterialRow;
import com.cmo.offers.model.table.EditingBigDecimalCell;
import com.cmo.offers.model.table.EditingTextCell;
import com.cmo.offers.ui.service.RawMaterialService;

public class RawMaterialTableController {

    private final TableView<RawMaterialRow> table;

    private final ClientEntity client;
    private final PlantEntity plant;
    private final Supplier<YearMonth> periodSupplier;
    private final RawMaterialService rawMaterialService;
    private final MaterialDAO materialDAO;

    private final ObservableList<MaterialEntity> materials = FXCollections.observableArrayList();

    private final ObservableList<RawMaterialRow> data;
    private final ObservableList<RawMaterialRow> tableItems = FXCollections.observableArrayList();
    private final RawMaterialRow summaryRow;

    public RawMaterialTableController(
            TableView<RawMaterialRow> table,
            ClientEntity client,
            PlantEntity plant,
            Supplier<YearMonth> periodSupplier,
            RawMaterialService rawMaterialService,
            MaterialDAO materialDAO
    ) {
        this.table = Objects.requireNonNull(table, "table must not be null");
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.plant = Objects.requireNonNull(plant, "plant must not be null");
        this.periodSupplier = Objects.requireNonNull(periodSupplier, "periodSupplier must not be null");
        this.rawMaterialService = Objects.requireNonNull(rawMaterialService, "rawMaterialService must not be null");
        this.materialDAO = Objects.requireNonNull(materialDAO, "materialDAO must not be null");

        this.data = FXCollections.observableArrayList(
                r -> new Observable[] {
                        r.materialProperty(),
                        r.descriptionProperty(),
                        r.grossWeightProperty(),
                        r.netWeightProperty(),
                        r.scrapWeightProperty(),
                        r.scrapValuePercentageProperty(),
                        r.transformationPriceProperty(),
                        r.costXPieceProperty(),
                        r.markupProperty(),
                        r.lmeUsdPerTonProperty(),
                        r.fxUsdToEurProperty(),
                        r.finalEurPerKgProperty(),
                        r.priceProperty()
                }
        );

        this.summaryRow = new RawMaterialRow(true);
        this.summaryRow.setDescription("Total");

        loadMaterials();
        configure();
    }

    private void loadMaterials() {
        try {
            materials.setAll(materialDAO.findAll());
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error loading materials: " + ex.getMessage()).showAndWait();
        }
    }

    private void configure() {
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setSortPolicy(tv -> false);

        configureColumns();
        configureRowStyle();
        bindTotals();

        rebuildTableItems();
        table.setItems(tableItems);

        data.addListener((ListChangeListener<RawMaterialRow>) change -> rebuildTableItems());
    }

    private void configureColumns() {
        TableColumn<RawMaterialRow, MaterialEntity> colMaterial =
                editableMaterialColumn("CDC", 130, false);

        TableColumn<RawMaterialRow, String> colDescr =
                editableTextColumn("Description", RawMaterialRow::descriptionProperty, 170, true);

        TableColumn<RawMaterialRow, BigDecimal> colGross =
                editableBigDecimalColumn("Gross Weight (kg/pc)", RawMaterialRow::grossWeightProperty, 140, 3, true, true);

        TableColumn<RawMaterialRow, BigDecimal> colNet =
                editableBigDecimalColumn("Net Weight (kg/pc)", RawMaterialRow::netWeightProperty, 140, 3, true, true);

        TableColumn<RawMaterialRow, BigDecimal> colScrapW =
                readOnlyBigDecimalColumn("Scrap Weight (kg/pc)", RawMaterialRow::scrapWeightProperty, 150, 4, false);

        TableColumn<RawMaterialRow, BigDecimal> colScrapV =
                editableBigDecimalColumn("Scrap Value %", RawMaterialRow::scrapValuePercentageProperty, 120, 2, true, false);

        TableColumn<RawMaterialRow, BigDecimal> colTrans =
                editableBigDecimalColumn("Transformation Price", RawMaterialRow::transformationPriceProperty, 150, 2, true, false);

        TableColumn<RawMaterialRow, BigDecimal> colCost =
                readOnlyBigDecimalColumn("Material Cost (EUR/pc)", RawMaterialRow::costXPieceProperty, 150, 2, true);

        TableColumn<RawMaterialRow, BigDecimal> colMarkup =
                editableBigDecimalColumn("Markup %", RawMaterialRow::markupProperty, 110, 2, true, false);

        TableColumn<RawMaterialRow, BigDecimal> colLme =
                readOnlyBigDecimalColumn("LME (USD/ton)", RawMaterialRow::lmeUsdPerTonProperty, 140, 2, false);

        TableColumn<RawMaterialRow, BigDecimal> colFx =
                readOnlyBigDecimalColumn("Exchange (USD→EUR)", RawMaterialRow::fxUsdToEurProperty, 150, 4, false);

        TableColumn<RawMaterialRow, BigDecimal> colMatCost =
                readOnlyBigDecimalColumn("Material Cost (EUR/kg)", RawMaterialRow::finalEurPerKgProperty, 150, 2, false);

        TableColumn<RawMaterialRow, BigDecimal> colPrice =
                readOnlyBigDecimalColumn("Price (EUR)", RawMaterialRow::priceProperty, 140, 2, true);

        table.getColumns().setAll(List.of(
                colMaterial, colDescr, colGross, colNet,
                colScrapW, colScrapV, colTrans, colCost,
                colMarkup, colLme, colFx, colMatCost, colPrice
        ));
    }

    private void configureRowStyle() {
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(RawMaterialRow item, boolean empty) {
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

    private void bindTotals() {
        summaryRow.grossWeightProperty().bind(
                Bindings.createObjectBinding(
                        () -> data.stream()
                                .filter(Objects::nonNull)
                                .filter(r -> !r.isSummary())
                                .map(RawMaterialRow::getGrossWeight)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        data
                )
        );

        summaryRow.netWeightProperty().bind(
                Bindings.createObjectBinding(
                        () -> data.stream()
                                .filter(Objects::nonNull)
                                .filter(r -> !r.isSummary())
                                .map(RawMaterialRow::getNetWeight)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        data
                )
        );

        summaryRow.costXPieceProperty().bind(
                Bindings.createObjectBinding(
                        () -> data.stream()
                                .filter(Objects::nonNull)
                                .filter(r -> !r.isSummary())
                                .map(RawMaterialRow::getCostXPiece)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        data
                )
        );

        summaryRow.priceProperty().bind(
                Bindings.createObjectBinding(
                        () -> data.stream()
                                .filter(Objects::nonNull)
                                .filter(r -> !r.isSummary())
                                .map(RawMaterialRow::getPrice)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        data
                )
        );
    }

    private void rebuildTableItems() {
        tableItems.setAll(data);
        tableItems.add(summaryRow);
    }

    private RawMaterialRow createEmptyRow() {
        RawMaterialRow row = new RawMaterialRow();
        row.setDescription("");
        row.setGrossWeight(null);
        row.setNetWeight(null);
        row.setScrapWeight(null);
        row.setScrapValuePercentage(RawMaterialRow.DEFAULT_SCRAP_VALUE_PERCENTAGE);
        row.setTransformationPrice(null);
        row.setCostXPiece(null);
        row.setMarkup(null);
        row.setLmeUsdPerTon(null);
        row.setFxUsdToEur(null);
        row.setFinalEurPerKg(null);
        row.setPrice(null);
        return row;
    }

    public void setRows(Collection<RawMaterialRow> rows) {
        data.clear();

        if (rows != null && !rows.isEmpty()) {
            rows.stream()
                    .filter(Objects::nonNull)
                    .filter(row -> !row.isSummary())
                    .forEach(data::add);
        }
    }

    public void addRow(RawMaterialRow row) {
        if (row != null && !row.isSummary()) {
            data.add(row);
        }
    }

    public void addRows(Collection<RawMaterialRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }

        rows.stream()
                .filter(Objects::nonNull)
                .filter(row -> !row.isSummary())
                .forEach(data::add);
    }

    public void removeRow(RawMaterialRow row) {
        if (row != null && !row.isSummary()) {
            data.remove(row);
        }
    }

    public void addEmptyRow() {
        table.requestFocus();

        Platform.runLater(() -> {
            RawMaterialRow newRow = createEmptyRow();
            addRow(newRow);
            focusAndEditRow(data.size() - 1);
        });
    }

    public void seedEmptyRows(int count) {
        data.clear();

        for (int i = 0; i < count; i++) {
            addRow(createEmptyRow());
        }
    }

    public ObservableList<RawMaterialRow> getData() {
        return data;
    }

    public RawMaterialRow getSummaryRow() {
        return summaryRow;
    }

    private void focusAndEditRow(int rowIndex) {
        Platform.runLater(() -> {
            if (rowIndex < 0 || rowIndex >= data.size()) {
                return;
            }

            table.requestFocus();
            table.scrollTo(rowIndex);
            table.getSelectionModel().clearAndSelect(rowIndex);

            if (!table.getColumns().isEmpty()) {
                table.edit(rowIndex, table.getColumns().get(0));
            }
        });
    }

    private void recalculateRowSafely(RawMaterialRow row) {
        if (row == null || row.isSummary()) {
            return;
        }

        try {
        	YearMonth period = currentPeriod();

        	rawMaterialService.recalculateRow(row, client, plant, period);
        	if (period == null) {
        	    return;
        	}
        	rawMaterialService.recalculateRow(row, client, plant, period);
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(
                    Alert.AlertType.ERROR,
                    "Error recalculating raw material row: " + ex.getMessage()
            ).showAndWait();
        }
    }

    private TableColumn<RawMaterialRow, MaterialEntity> editableMaterialColumn(
            String title,
            double prefWidth,
            boolean showInSummary
    ) {
        TableColumn<RawMaterialRow, MaterialEntity> col = new TableColumn<>(title);

        col.setCellValueFactory(cd -> cd.getValue().materialProperty());

        StringConverter<MaterialEntity> converter = materialConverter();

        col.setCellFactory(tc -> new TableCell<>() {

            private final ComboBox<MaterialEntity> comboBox = new ComboBox<>(materials);

            {
                comboBox.setConverter(converter);
                comboBox.setMaxWidth(Double.MAX_VALUE);

                comboBox.setOnAction(evt -> {
                    if (isEditing() && !Objects.equals(comboBox.getValue(), getItem())) {
                        commitEdit(comboBox.getValue());
                    }
                });

                comboBox.focusedProperty().addListener((obs, oldVal, newVal) -> {
                    if (!newVal && isEditing() && !Objects.equals(comboBox.getValue(), getItem())) {
                        commitEdit(comboBox.getValue());
                    }
                });

                comboBox.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        cancelEdit();
                        event.consume();
                    }
                });
            }

            @Override
            public void startEdit() {
                RawMaterialRow row = getTableRow() == null ? null : getTableRow().getItem();
                if (row == null || row.isSummary()) {
                    return;
                }

                super.startEdit();

                comboBox.setValue(getItem());
                setText(null);
                setGraphic(comboBox);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

                Platform.runLater(() -> {
                    comboBox.requestFocus();
                    comboBox.show();
                });
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                showMaterialAsText(this, getItem(), showInSummary, converter);
            }

            @Override
            public void commitEdit(MaterialEntity newValue) {
                if (!isEditing() && Objects.equals(newValue, getItem())) {
                    super.commitEdit(newValue);

                    TableView<RawMaterialRow> table = getTableView();
                    if (table != null) {
                        table.edit(-1, null);
                    }

                    showMaterialAsText(this, newValue, showInSummary, converter);
                    return;
                }

                if (!isEditing()) {
                    TableView<RawMaterialRow> table = getTableView();
                    TableColumn<RawMaterialRow, MaterialEntity> column = getTableColumn();
                    int rowIndex = getIndex();

                    if (table != null
                            && column != null
                            && rowIndex >= 0
                            && rowIndex < table.getItems().size()
                            && getTableRow() != null
                            && getTableRow().getItem() != null) {

                        TablePosition<RawMaterialRow, MaterialEntity> position =
                                new TablePosition<>(table, rowIndex, column);

                        TableColumn.CellEditEvent<RawMaterialRow, MaterialEntity> event =
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

                TableView<RawMaterialRow> table = getTableView();
                if (table != null) {
                    table.edit(-1, null);
                }

                showMaterialAsText(this, newValue, showInSummary, converter);
            }

            @Override
            protected void updateItem(MaterialEntity item, boolean empty) {
                super.updateItem(item, empty);

                RawMaterialRow row = getTableRow() == null ? null : getTableRow().getItem();

                if (empty || row == null) {
                    setText(null);
                    setGraphic(null);
                } else if (isEditing()) {
                    comboBox.setValue(item);
                    setText(null);
                    setGraphic(comboBox);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                } else {
                    showMaterialAsText(this, item, showInSummary, converter);
                }
            }
        });

        col.setOnEditCommit(e -> {
            RawMaterialRow row = e.getRowValue();
            if (row != null && !row.isSummary()) {
                row.setMaterial(e.getNewValue());
                recalculateRowSafely(row);
            }
        });

        col.setPrefWidth(prefWidth);
        return col;
    }
    private TableColumn<RawMaterialRow, String> editableTextColumn(
            String title,
            Function<RawMaterialRow, StringProperty> propertyExtractor,
            double prefWidth,
            boolean showInSummary
    ) {
        TableColumn<RawMaterialRow, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cd -> propertyExtractor.apply(cd.getValue()));

        col.setCellFactory(tc -> new EditingTextCell<>(
                row -> !row.isSummary(),
                row -> row.isSummary() && !showInSummary
        ));

        col.setOnEditCommit(e -> {
            RawMaterialRow row = e.getRowValue();
            if (row != null && !row.isSummary()) {
                propertyExtractor.apply(row).set(e.getNewValue());
            }
        });

        col.setPrefWidth(prefWidth);
        return col;
    }

    private TableColumn<RawMaterialRow, BigDecimal> editableBigDecimalColumn(
            String title,
            Function<RawMaterialRow, ObjectProperty<BigDecimal>> propertyExtractor,
            double prefWidth,
            int scale,
            boolean triggerRecalculation,
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

        TableColumn<RawMaterialRow, BigDecimal> col = new TableColumn<>(title);
        col.setCellValueFactory(cd -> propertyExtractor.apply(cd.getValue()));

        col.setCellFactory(tc -> new EditingBigDecimalCell<>(
                converter,
                scale,
                row -> !row.isSummary(),
                row -> row.isSummary() && !showInSummary
        ));

        col.setOnEditCommit(e -> {
            RawMaterialRow row = e.getRowValue();
            if (row != null && !row.isSummary()) {
                propertyExtractor.apply(row).set(e.getNewValue());
                if (triggerRecalculation) {
                    recalculateRowSafely(row);
                }
            }
        });

        col.setPrefWidth(prefWidth);
        return col;
    }

    private TableColumn<RawMaterialRow, BigDecimal> readOnlyBigDecimalColumn(
            String title,
            Function<RawMaterialRow, ObservableValue<BigDecimal>> propertyExtractor,
            double prefWidth,
            int scale,
            boolean showInSummary
    ) {
        TableColumn<RawMaterialRow, BigDecimal> col = new TableColumn<>(title);
        col.setCellValueFactory(cd -> propertyExtractor.apply(cd.getValue()));

        col.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);

                RawMaterialRow row = getTableRow() == null ? null : getTableRow().getItem();

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
    
    // Helpers 
    private void showMaterialAsText(
            TableCell<RawMaterialRow, MaterialEntity> cell,
            MaterialEntity item,
            boolean showInSummary,
            StringConverter<MaterialEntity> converter
    ) {
        RawMaterialRow row = cell.getTableRow() == null ? null : cell.getTableRow().getItem();

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
    
    private StringConverter<MaterialEntity> materialConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(MaterialEntity material) {
                return material == null ? "" : material.getCode();
            }

            @Override
            public MaterialEntity fromString(String string) {
                return materials.stream()
                        .filter(m -> Objects.equals(m.getCode(), string))
                        .findFirst()
                        .orElse(null);
            }
        };
    }
    
    public List<RMExportModel> extractDtos() {
        List<RMExportModel> result = new ArrayList<>();

        for (RawMaterialRow row : data) {
            if (row == null || row.isSummary()) {
                continue;
            }

            RMExportModel dto = new RMExportModel();
            dto.setMaterialCode(row.getMaterial() == null ? null : row.getMaterial().getCode());
            dto.setDescription(row.getDescription());
            dto.setGrossWeight(row.getGrossWeight());
            dto.setNetWeight(row.getNetWeight());
            dto.setScrapWeight(row.getScrapWeight());
            dto.setScrapValuePercentage(row.getScrapValuePercentage());
            dto.setTransformationPrice(row.getTransformationPrice());
            dto.setCostXPiece(row.getCostXPiece());
            dto.setMarkup(row.getMarkup());
            dto.setLmeUsdPerTon(row.getLmeUsdPerTon());
            dto.setFxUsdToEur(row.getFxUsdToEur());
            dto.setFinalEurPerKg(row.getFinalEurPerKg());
            dto.setPrice(row.getPrice());

            result.add(dto);
        }

        return result;
    }
    
    public void loadDtos(Collection<RMExportModel> dtos) {
        data.clear();

        if (dtos == null || dtos.isEmpty()) {
            return;
        }

        for (RMExportModel dto : dtos) {
            if (dto == null) {
                continue;
            }

            RawMaterialRow row = new RawMaterialRow();
            row.setMaterial(findMaterialByCode(dto.getMaterialCode()));
            row.setDescription(dto.getDescription());
            row.setGrossWeight(dto.getGrossWeight());
            row.setNetWeight(dto.getNetWeight());
            row.setScrapWeight(dto.getScrapWeight());
            row.setScrapValuePercentage(dto.getScrapValuePercentage());
            row.setTransformationPrice(dto.getTransformationPrice());
            row.setCostXPiece(dto.getCostXPiece());
            row.setMarkup(dto.getMarkup());
            row.setLmeUsdPerTon(dto.getLmeUsdPerTon());
            row.setFxUsdToEur(dto.getFxUsdToEur());
            row.setFinalEurPerKg(dto.getFinalEurPerKg());
            row.setPrice(dto.getPrice());

            data.add(row);
        }
    }
    
    private MaterialEntity findMaterialByCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }

        return materials.stream()
                .filter(Objects::nonNull)
                .filter(m -> Objects.equals(m.getCode(), code))
                .findFirst()
                .orElse(null);
    }
    
    // Helpers    
    private YearMonth currentPeriod() {
        return periodSupplier.get();
    }
    
    public void refreshForPeriodChange() {
        for (RawMaterialRow row : data) {
            recalculateRowSafely(row);
        }
    }
}