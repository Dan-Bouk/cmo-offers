package com.cmo.offers.ui.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import com.cmo.offers.model.ComponentsExportModel;
import com.cmo.offers.model.row.ComponentsRow;
import com.cmo.offers.model.table.EditingBigDecimalCell;
import com.cmo.offers.model.table.EditingTextCell;

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

public class ComponentsTableController {

    private final TableView<ComponentsRow> table;

    private final ObservableList<ComponentsRow> data = FXCollections.observableArrayList(
            row -> new Observable[] {
                    row.quantityProperty(),
                    row.unitCostProperty(),
                    row.markupProperty(),
                    row.costProperty(),
                    row.priceProperty()
            }
    );

    private final ObservableList<ComponentsRow> tableItems = FXCollections.observableArrayList();
    private final ComponentsRow summaryRow = new ComponentsRow(true);

    public ComponentsTableController(TableView<ComponentsRow> table) {
        this.table = Objects.requireNonNull(table, "table must not be null");
        configure();
    }

    private void configure() {
        summaryRow.setComponentName("Total");

        table.setEditable(true);
        table.setSortPolicy(tv -> false);

        configureColumns();
        configureRowStyle();
        bindTotals();

        rebuildTableItems();
        table.setItems(tableItems);

        data.addListener((ListChangeListener<ComponentsRow>) change -> rebuildTableItems());
    }

    private void configureColumns() {
        table.getColumns().setAll(List.of(
                editableTextColumn("CDC", ComponentsRow::cdcProperty, 90),
                editableTextColumn("Component name", ComponentsRow::componentNameProperty, 300),
                editableBigDecimalColumn("Q.ty", ComponentsRow::quantityProperty, 80, 2, false),
                editableBigDecimalColumn("Cost (€)", ComponentsRow::unitCostProperty, 110, 2, false),
                readOnlyBigDecimalColumn("Total (€)", ComponentsRow::costProperty, 110, 2),
                editableBigDecimalColumn("% Ric", ComponentsRow::markupProperty, 90, 2, false),
                readOnlyBigDecimalColumn("Price (€)", ComponentsRow::priceProperty, 120, 2)
        ));
    }

    private void configureRowStyle() {
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(ComponentsRow item, boolean empty) {
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
                                .map(ComponentsRow::getCost)
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
                                .map(ComponentsRow::getPrice)
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

    private ComponentsRow createEmptyRow() {
        ComponentsRow row = new ComponentsRow();
        row.setQuantity(BigDecimal.ZERO);
        row.setUnitCost(BigDecimal.ZERO);
        row.setMarkup(BigDecimal.ZERO);
        return row;
    }

    public void setRows(Collection<ComponentsRow> rows) {
        data.clear();

        if (rows != null && !rows.isEmpty()) {
            rows.stream()
                    .filter(Objects::nonNull)
                    .filter(row -> !row.isSummary())
                    .forEach(data::add);
        }
    }

    public void addRow(ComponentsRow row) {
        if (row != null && !row.isSummary()) {
            data.add(row);
        }
    }

    public void addRows(Collection<ComponentsRow> rows) {
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
            ComponentsRow newRow = createEmptyRow();
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

    public ObservableList<ComponentsRow> getData() {
        return data;
    }

    public ComponentsRow getSummaryRow() {
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

    private TableColumn<ComponentsRow, String> editableTextColumn(
            String title,
            Function<ComponentsRow, StringProperty> propertyExtractor,
            double prefWidth
    ) {
        TableColumn<ComponentsRow, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cd -> propertyExtractor.apply(cd.getValue()));
        col.setCellFactory(tc -> new EditingTextCell<>(
                row -> !row.isSummary(),
                ComponentsRow::isSummary
        ));
        col.setOnEditCommit(e -> {
            ComponentsRow row = e.getRowValue();
            if (row != null && !row.isSummary()) {
                propertyExtractor.apply(row).set(e.getNewValue());
            }
        });
        col.setPrefWidth(prefWidth);
        return col;
    }

    private TableColumn<ComponentsRow, BigDecimal> editableBigDecimalColumn(
            String title,
            Function<ComponentsRow, ObjectProperty<BigDecimal>> propertyExtractor,
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

        TableColumn<ComponentsRow, BigDecimal> col = new TableColumn<>(title);
        col.setCellValueFactory(cd -> propertyExtractor.apply(cd.getValue()));
        col.setCellFactory(tc -> new EditingBigDecimalCell<>(
                converter,
                scale,
                row -> !row.isSummary(),
                row -> row.isSummary() && !showInSummary
        ));
        col.setOnEditCommit(e -> {
            ComponentsRow row = e.getRowValue();
            if (row != null && !row.isSummary()) {
                propertyExtractor.apply(row).set(e.getNewValue());
            }
        });
        col.setPrefWidth(prefWidth);
        return col;
    }

    private TableColumn<ComponentsRow, BigDecimal> readOnlyBigDecimalColumn(
            String title,
            Function<ComponentsRow, ObservableValue<BigDecimal>> propertyExtractor,
            double prefWidth,
            int scale
    ) {
        TableColumn<ComponentsRow, BigDecimal> col = new TableColumn<>(title);
        col.setCellValueFactory(cd -> propertyExtractor.apply(cd.getValue()));
        col.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);

                ComponentsRow row = getTableRow() == null ? null : getTableRow().getItem();
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
    
    public List<ComponentsExportModel> extractDtos() {
        List<ComponentsExportModel> result = new ArrayList<>();

        for (ComponentsRow row : data) {
            if (row == null || row.isSummary()) {
                continue;
            }

            ComponentsExportModel dto = new ComponentsExportModel();
            dto.setCdc(row.getCdc());
            dto.setComponentName(row.getComponentName());
            dto.setQuantity(row.getQuantity());
            dto.setUnitCost(row.getUnitCost());
            dto.setMarkup(row.getMarkup());
            dto.setCost(row.getCost());
            dto.setPrice(row.getPrice());

            result.add(dto);
        }

        return result;
    }
    
    public void loadDtos(Collection<ComponentsExportModel> dtos) {
        data.clear();

        if (dtos == null || dtos.isEmpty()) {
            return;
        }

        for (ComponentsExportModel dto : dtos) {
            if (dto == null) {
                continue;
            }

            ComponentsRow row = new ComponentsRow();
            row.setCdc(dto.getCdc());
            row.setComponentName(dto.getComponentName());
            row.setQuantity(dto.getQuantity());
            row.setUnitCost(dto.getUnitCost());
            row.setMarkup(dto.getMarkup());

            data.add(row);
        }
    }
}