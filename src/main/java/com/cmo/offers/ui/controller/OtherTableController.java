package com.cmo.offers.ui.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import com.cmo.offers.model.OtherExportModel;
import com.cmo.offers.model.row.OtherCostsRow;
import com.cmo.offers.model.row.RawMaterialRow;
import com.cmo.offers.model.table.EditingBigDecimalCell;
import com.cmo.offers.model.table.EditingTextCell;
import com.cmo.offers.ui.service.OtherCostsService;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.util.StringConverter;

public class OtherTableController {

    private final TableView<OtherCostsRow> table;
    private final OtherCostsService otherCostsService = new OtherCostsService();

    private final ObservableList<OtherCostsRow> data = FXCollections.observableArrayList(
            row -> new Observable[] {
                    row.quantityProperty(),
                    row.unitCostProperty(),
                    row.markupProperty(),
                    row.totalCostProperty(),
                    row.priceProperty()
            }
    );

    private final ObservableList<OtherCostsRow> tableItems = FXCollections.observableArrayList();
    private final OtherCostsRow summaryRow = new OtherCostsRow(true);

    public OtherTableController(TableView<OtherCostsRow> table) {
        this.table = Objects.requireNonNull(table, "table must not be null");
        configure();
    }

    private void configure() {
        summaryRow.setDescription("Total");

        table.setEditable(true);
        table.setSortPolicy(tv -> false);

        configureColumns();
        configureRowStyle();
        bindTotals();

        rebuildTableItems();
        table.setItems(tableItems);

        data.addListener((ListChangeListener<OtherCostsRow>) c -> rebuildTableItems());
    }

    private void configureColumns() {
        table.getColumns().setAll(List.of(
                editableTextColumn("CDC", OtherCostsRow::cdcProperty, 90, OtherCostsRow::isSummary),
                editableTextColumn("Other", OtherCostsRow::descriptionProperty, 300, row -> false),

                readOnlyBigDecimalColumn(
                        "Q.ty",
                        OtherCostsRow::quantityProperty,
                        90,
                        3,
                        row -> row.isSummary()
                                || row.isMaterialAndComponentsRow()
                                || row.isMachiningRow()
                ),

                editableBigDecimalColumn(
                        "Unit Cost (€)",
                        OtherCostsRow::unitCostProperty,
                        110,
                        2,
                        row -> row.isSummary()
                                || row.isMaterialAndComponentsRow()
                                || row.isMachiningRow()
                ),

                readOnlyBigDecimalColumn(
                        "Total cost (€)",
                        OtherCostsRow::totalCostProperty,
                        110,
                        2,
                        row -> row.isSummary()
                                || row.isMachiningRow()
                ),

                editableBigDecimalColumn(
                        "% Ric",
                        OtherCostsRow::markupProperty,
                        90,
                        2,
                        row -> row.isSummary()
                                || row.isMaterialAndComponentsRow()
                                || row.isMachiningRow()
                ),

                readOnlyBigDecimalColumn(
                        "Price (€)",
                        OtherCostsRow::priceProperty,
                        120,
                        2,
                        row -> false
                )
        ));
    }

    private void configureRowStyle() {
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(OtherCostsRow item, boolean empty) {
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
        summaryRow.totalCostProperty().bind(
                Bindings.createObjectBinding(
                        () -> data.stream()
                                .filter(Objects::nonNull)
                                .filter(r -> !r.isSummary())
                                .map(OtherCostsRow::getTotalCost)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(2, RoundingMode.HALF_UP),
                        data
                )
        );

        summaryRow.priceProperty().bind(
                Bindings.createObjectBinding(
                        () -> data.stream()
                                .filter(Objects::nonNull)
                                .filter(r -> !r.isSummary())
                                .map(OtherCostsRow::getPrice)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(2, RoundingMode.HALF_UP),
                        data
                )
        );
    }

    private void rebuildTableItems() {
        tableItems.setAll(data);
        tableItems.add(summaryRow);
    }

    private OtherCostsRow createEmptyRow() {
        OtherCostsRow row = new OtherCostsRow();
        row.setUnitCost(BigDecimal.ZERO);
        row.setMarkup(BigDecimal.ZERO);
        return row;
    }

    public void setRows(Collection<OtherCostsRow> rows) {
        data.clear();

        if (rows != null && !rows.isEmpty()) {
            rows.stream()
                    .filter(Objects::nonNull)
                    .filter(row -> !row.isSummary())
                    .forEach(data::add);
        }
    }

    public void addRow(OtherCostsRow row) {
        if (row != null && !row.isSummary()) {
            data.add(row);
        }
    }

    public void addRows(Collection<OtherCostsRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }

        rows.stream()
                .filter(Objects::nonNull)
                .filter(row -> !row.isSummary())
                .forEach(data::add);
    }

    public void addEmptyRow() {
        table.requestFocus();

        Platform.runLater(() -> {
            OtherCostsRow newRow = createEmptyRow();
            addRow(newRow);
            focusAndEditRow(data.size() - 1);
        });
    }

    public void seedEmptyRows(int count) {
        data.clear();

        for (int i = 0; i < count; i++) {
            data.add(createEmptyRow());
        }
    }

    public void loadDefaultRows(
            ObservableList<RawMaterialRow> rawMaterials,
            ObservableValue<BigDecimal> rawMaterialCostXPieceSummary,
            ObservableValue<BigDecimal> componentsCostSummary,
            ObservableValue<BigDecimal> rawMaterialPriceSummary,
            ObservableValue<BigDecimal> componentsPriceSummary,
            ObservableValue<BigDecimal> treatmentsOperationPriceSummary,
            ObservableValue<BigDecimal> treatmentsSilverPriceSummary,
            ObservableValue<BigDecimal> operationsMachiningSetupPriceSummary,
            ObservableValue<BigDecimal> operationsMachiningProductionPriceSummary,
            ObservableValue<BigDecimal> operationsAdditionalPriceSummary) {

        data.setAll(createDefaultRows(
                rawMaterials,
                rawMaterialCostXPieceSummary,
                componentsCostSummary,
                rawMaterialPriceSummary,
                componentsPriceSummary,
                treatmentsOperationPriceSummary,
                treatmentsSilverPriceSummary,
                operationsMachiningSetupPriceSummary,
                operationsMachiningProductionPriceSummary,
                operationsAdditionalPriceSummary
        ));
    }

    public List<OtherCostsRow> createDefaultRows(
            ObservableList<RawMaterialRow> rawMaterials,
            ObservableValue<BigDecimal> rawMaterialCostXPieceSummary,
            ObservableValue<BigDecimal> componentsCostSummary,
            ObservableValue<BigDecimal> rawMaterialPriceSummary,
            ObservableValue<BigDecimal> componentsPriceSummary,
            ObservableValue<BigDecimal> treatmentsOperationPriceSummary,
            ObservableValue<BigDecimal> treatmentsSilverPriceSummary,
            ObservableValue<BigDecimal> operationsMachiningSetupPriceSummary,
            ObservableValue<BigDecimal> operationsMachiningProductionPriceSummary,
            ObservableValue<BigDecimal> operationsAdditionalPriceSummary) {

        Objects.requireNonNull(rawMaterials, "rawMaterials must not be null");

        OtherCostsRow tras = new OtherCostsRow("TRAS", "Transport costs", BigDecimal.ZERO, null, BigDecimal.ONE);
        OtherCostsRow pack = new OtherCostsRow("PACK", "Packaging", BigDecimal.ZERO, null, BigDecimal.ONE);
        OtherCostsRow mp   = new OtherCostsRow("MP", "Materiali e Componenti", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE);
        OtherCostsRow mach = new OtherCostsRow("MACH", "Lavorazioni", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        otherCostsService.bindQuantityToRawMaterialSum(tras, rawMaterials);
        otherCostsService.bindQuantityToRawMaterialSum(pack, rawMaterials);

        otherCostsService.bindMaterialAndComponentsRow(
                mp,
                rawMaterialCostXPieceSummary,
                componentsCostSummary,
                rawMaterialPriceSummary,
                componentsPriceSummary
        );

        otherCostsService.bindMachiningRowPrice(
                mach,
                treatmentsOperationPriceSummary,
                treatmentsSilverPriceSummary,
                operationsMachiningSetupPriceSummary,
                operationsMachiningProductionPriceSummary,
                operationsAdditionalPriceSummary
        );

        return List.of(tras, pack, mp, mach);
    }

    public ObservableList<OtherCostsRow> getData() {
        return data;
    }

    public OtherCostsRow getSummaryRow() {
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

            if (table.getColumns().size() > 1) {
                table.edit(rowIndex, table.getColumns().get(1));
            } else if (!table.getColumns().isEmpty()) {
                table.edit(rowIndex, table.getColumns().get(0));
            }
        });
    }

    private TableColumn<OtherCostsRow, String> editableTextColumn(
            String title,
            Function<OtherCostsRow, StringProperty> propertyExtractor,
            double prefWidth,
            Predicate<OtherCostsRow> hideCellPredicate
    ) {
        TableColumn<OtherCostsRow, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cd -> propertyExtractor.apply(cd.getValue()));
        col.setCellFactory(tc -> new EditingTextCell<>(
                row -> !row.isSummary() && !hideCellPredicate.test(row),
                hideCellPredicate
        ));
        col.setOnEditCommit(e -> {
            OtherCostsRow row = e.getRowValue();
            if (row != null && !row.isSummary() && !hideCellPredicate.test(row)) {
                propertyExtractor.apply(row).set(e.getNewValue());
            }
        });
        col.setPrefWidth(prefWidth);
        return col;
    }

    private TableColumn<OtherCostsRow, BigDecimal> editableBigDecimalColumn(
            String title,
            Function<OtherCostsRow, ObjectProperty<BigDecimal>> propertyExtractor,
            double prefWidth,
            int scale,
            Predicate<OtherCostsRow> hideCellPredicate
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

        TableColumn<OtherCostsRow, BigDecimal> col = new TableColumn<>(title);
        col.setCellValueFactory(cd -> propertyExtractor.apply(cd.getValue()));
        col.setCellFactory(tc -> new EditingBigDecimalCell<>(
                converter,
                scale,
                row -> !row.isSummary() && !hideCellPredicate.test(row),
                hideCellPredicate
        ));
        col.setOnEditCommit(e -> {
            OtherCostsRow row = e.getRowValue();
            if (row != null && !row.isSummary() && !hideCellPredicate.test(row)) {
                propertyExtractor.apply(row).set(e.getNewValue());
            }
        });
        col.setPrefWidth(prefWidth);
        return col;
    }

    private TableColumn<OtherCostsRow, BigDecimal> readOnlyBigDecimalColumn(
            String title,
            Function<OtherCostsRow, ObservableValue<BigDecimal>> propertyExtractor,
            double prefWidth,
            int scale,
            Predicate<OtherCostsRow> hideCellPredicate
    ) {
        TableColumn<OtherCostsRow, BigDecimal> col = new TableColumn<>(title);
        col.setCellValueFactory(cd -> propertyExtractor.apply(cd.getValue()));
        col.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);

                OtherCostsRow row = getTableRow() == null ? null : getTableRow().getItem();

                if (empty || row == null) {
                    setText(null);
                } else if (hideCellPredicate.test(row)) {
                    setText("");
                } else {
                    setText(item == null ? "" : item.setScale(scale, RoundingMode.HALF_UP).toPlainString());
                }
            }
        });
        col.setPrefWidth(prefWidth);
        return col;
    }
    
    public List<OtherExportModel> extractDtos() {
        List<OtherExportModel> result = new ArrayList<>();

        for (OtherCostsRow row : data) {
            if (row == null || row.isSummary()) {
                continue;
            }

            OtherExportModel dto = new OtherExportModel();
            dto.setCdc(row.getCdc());
            dto.setDescription(row.getDescription());
            dto.setQuantity(row.getQuantity());
            dto.setUnitCost(row.getUnitCost());
            dto.setMarkup(row.getMarkup());
            dto.setTotalCost(row.getTotalCost());
            dto.setPrice(row.getPrice());

            result.add(dto);
        }

        return result;
    }
    
    public void loadDtos(Collection<OtherExportModel> dtos) {
        data.clear();

        if (dtos == null || dtos.isEmpty()) {
            return;
        }

        for (OtherExportModel dto : dtos) {
            if (dto == null) {
                continue;
            }

            OtherCostsRow row = new OtherCostsRow();
            row.setCdc(dto.getCdc());
            row.setDescription(dto.getDescription());

            // Avoid bound-property issues on import:
            row.unbindQuantity();
            row.unbindCalculatedFields();

            row.setQuantity(dto.getQuantity());
            row.setUnitCost(dto.getUnitCost());
            row.setMarkup(dto.getMarkup());
            row.setTotalCost(dto.getTotalCost());
            row.setPrice(dto.getPrice());

            data.add(row);
        }
    }
}