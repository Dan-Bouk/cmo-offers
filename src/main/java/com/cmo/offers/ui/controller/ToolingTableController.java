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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import com.cmo.offers.model.ToolingExportModel;
import com.cmo.offers.model.row.ToolingRow;
import com.cmo.offers.model.table.EditingBigDecimalCell;
import com.cmo.offers.model.table.EditingTextCell;

public class ToolingTableController {

    private final TableView<ToolingRow> table;

    private final ObservableList<ToolingRow> data = FXCollections.observableArrayList(
            row -> new Observable[] {
                    row.quantityProperty(),
                    row.unitCostProperty(),
                    row.markupProperty(),
                    row.costProperty(),
                    row.priceProperty()
            }
    );

    private final ObservableList<ToolingRow> tableItems = FXCollections.observableArrayList();
    private final ToolingRow summaryRow = new ToolingRow(true);

    public ToolingTableController(TableView<ToolingRow> table) {
        this.table = Objects.requireNonNull(table, "table must not be null");
        configure();
    }

    private void configure() {
        summaryRow.setToolName("Total");

        table.setEditable(true);
        table.setSortPolicy(tv -> false);

        configureColumns();
        configureRowStyle();
        bindTotals();

        rebuildTableItems();
        table.setItems(tableItems);

        data.addListener((ListChangeListener<ToolingRow>) change -> rebuildTableItems());
    }

    private void configureColumns() {
        table.getColumns().setAll(List.of(
                editableTextColumn("CDC", ToolingRow::cdcProperty, 90),
                editableTextColumn("Tool name", ToolingRow::toolNameProperty, 300),
                editableBigDecimalColumn("Q.ty", ToolingRow::quantityProperty, 80, 2),
                editableBigDecimalColumn("Cost (€)", ToolingRow::unitCostProperty, 110, 2),
                readOnlyBigDecimalColumn("Total (€)", ToolingRow::costProperty, 110, 2),
                editableBigDecimalColumn("% Ric", ToolingRow::markupProperty, 90, 2),
                readOnlyBigDecimalColumn("Price (€)", ToolingRow::priceProperty, 120, 2)
        ));
    }

    private void configureRowStyle() {
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(ToolingRow item, boolean empty) {
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
        summaryRow.costProperty().bind(
                Bindings.createObjectBinding(
                        () -> data.stream()
                                .filter(Objects::nonNull)
                                .filter(r -> !r.isSummary())
                                .map(ToolingRow::getCost)
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
                                .map(ToolingRow::getPrice)
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

    private ToolingRow createEmptyRow() {
        ToolingRow row = new ToolingRow();
        row.setQuantity(BigDecimal.ZERO);
        row.setUnitCost(BigDecimal.ZERO);
        row.setMarkup(BigDecimal.ZERO);
        return row;
    }

    public void setRows(Collection<ToolingRow> rows) {
        data.clear();

        if (rows != null && !rows.isEmpty()) {
            rows.stream()
                    .filter(Objects::nonNull)
                    .filter(row -> !row.isSummary())
                    .forEach(data::add);
        }
    }

    public void addRow(ToolingRow row) {
        if (row != null && !row.isSummary()) {
            data.add(row);
        }
    }

    public void addRows(Collection<ToolingRow> rows) {
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
            ToolingRow newRow = createEmptyRow();
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

    public ObservableList<ToolingRow> getData() {
        return data;
    }

    public ToolingRow getSummaryRow() {
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

    private TableColumn<ToolingRow, String> editableTextColumn(
            String title,
            Function<ToolingRow, StringProperty> propertyExtractor,
            double prefWidth
    ) {
        TableColumn<ToolingRow, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cd -> propertyExtractor.apply(cd.getValue()));
        col.setCellFactory(tc -> new EditingTextCell<>(
                row -> !row.isSummary(),
                ToolingRow::isSummary
        ));
        col.setOnEditCommit(e -> {
            ToolingRow row = e.getRowValue();
            if (row != null && !row.isSummary()) {
                propertyExtractor.apply(row).set(e.getNewValue());
            }
        });
        col.setPrefWidth(prefWidth);
        return col;
    }

    private TableColumn<ToolingRow, BigDecimal> editableBigDecimalColumn(
            String title,
            Function<ToolingRow, ObjectProperty<BigDecimal>> propertyExtractor,
            double prefWidth,
            int scale
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

        TableColumn<ToolingRow, BigDecimal> col = new TableColumn<>(title);
        col.setCellValueFactory(cd -> propertyExtractor.apply(cd.getValue()));
        col.setCellFactory(tc -> new EditingBigDecimalCell<>(
                converter,
                scale,
                row -> !row.isSummary(),
                ToolingRow::isSummary
        ));
        col.setOnEditCommit(e -> {
            ToolingRow row = e.getRowValue();
            if (row != null && !row.isSummary()) {
                propertyExtractor.apply(row).set(e.getNewValue());
            }
        });
        col.setPrefWidth(prefWidth);
        return col;
    }

    private TableColumn<ToolingRow, BigDecimal> readOnlyBigDecimalColumn(
            String title,
            Function<ToolingRow, ObservableValue<BigDecimal>> propertyExtractor,
            double prefWidth,
            int scale
    ) {
        TableColumn<ToolingRow, BigDecimal> col = new TableColumn<>(title);
        col.setCellValueFactory(cd -> propertyExtractor.apply(cd.getValue()));
        col.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);

                ToolingRow row = getTableRow() == null ? null : getTableRow().getItem();
                if (empty || row == null) {
                    setText(null);
                } else {
                    setText(item == null ? "" : item.setScale(scale, RoundingMode.HALF_UP).toPlainString());
                }
            }
        });
        col.setPrefWidth(prefWidth);
        return col;
    }
    
    public List<ToolingExportModel> extractDtos() {
        List<ToolingExportModel> result = new ArrayList<>();

        for (ToolingRow row : data) {
            if (row == null || row.isSummary()) {
                continue;
            }

            ToolingExportModel dto = new ToolingExportModel();
            dto.setCdc(row.getCdc());
            dto.setToolName(row.getToolName());
            dto.setQuantity(row.getQuantity());
            dto.setUnitCost(row.getUnitCost());
            dto.setMarkup(row.getMarkup());
            dto.setCost(row.getCost());
            dto.setPrice(row.getPrice());

            result.add(dto);
        }

        return result;
    }
    
    public void loadDtos(Collection<ToolingExportModel> dtos) {
        data.clear();

        if (dtos == null || dtos.isEmpty()) {
            seedEmptyRows(1);
            return;
        }

        for (ToolingExportModel dto : dtos) {
            if (dto == null) {
                continue;
            }

            ToolingRow row = new ToolingRow();
            row.setCdc(dto.getCdc());
            row.setToolName(dto.getToolName());
            row.setQuantity(dto.getQuantity());
            row.setUnitCost(dto.getUnitCost());
            row.setMarkup(dto.getMarkup());

            data.add(row);
        }
    }
}