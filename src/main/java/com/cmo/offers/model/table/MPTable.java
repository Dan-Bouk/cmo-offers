package com.cmo.offers.model.table;

import com.cmo.offers.dao.*;
import com.cmo.offers.entity.*;
import com.cmo.offers.model.row.MPRow;
import com.cmo.offers.ui.service.MPService;
import com.cmo.offers.utils.StyleUtils;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.Node;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.math.RoundingMode;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.Optional;

import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;

public class MPTable extends BorderPane {

    // DAOs + Service
    private final ClientDAO clientDAO;
    private final PlantDAO plantDAO;
    private final MaterialDAO materialDAO;
    @SuppressWarnings("unused")
    private final MarketPriceDAO marketPriceDAO;
    @SuppressWarnings("unused")
    private final ClientMarkupDAO clientMarkupDAO;

    private final MPService mPService;

    // UI
    private final ComboBox<ClientEntity> clientCombo = new ComboBox<>();
    private final ComboBox<PlantEntity> plantCombo = new ComboBox<>();
    private final ComboBox<Month> monthCombo = new ComboBox<>();
    private final ComboBox<Integer> yearCombo = new ComboBox<>();
    private final Button addBtn = new Button("Add");
    
    private enum MpEntryType {
        METAL_MARKET_DATA("Cu/Al market data"),
        CLIENT_PLANT_MARKUP("Client/Plant markup"),
        SILVER_BASE_PRICE("Silver base price");

        private final String label;

        MpEntryType(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final TableView<MPRow> table = new TableView<>();
    private final ObservableList<MPRow> tableData = FXCollections.observableArrayList();

    private final Label statusLabel = new Label("");
    private final ProgressIndicator progress = new ProgressIndicator();

    public MPTable(
            ClientDAO clientDAO,
            PlantDAO plantDAO,
            MaterialDAO materialDAO,
            MarketPriceDAO marketPriceDAO,
            ClientMarkupDAO clientMarkupDAO
    ) {
        this.clientDAO = clientDAO;
        this.plantDAO = plantDAO;
        this.materialDAO = materialDAO;
        this.marketPriceDAO = marketPriceDAO;
        this.clientMarkupDAO = clientMarkupDAO;

        this.mPService = new MPService(materialDAO, marketPriceDAO, clientMarkupDAO);

        getStyleClass().add("content-card");
        buildUi();
        wireEvents();
    }

    private void buildUi() {
        setPadding(new Insets(10));

        progress.setVisible(false);
        progress.setPrefSize(22, 22);

        statusLabel.getStyleClass().add("status-label");

        initMonthYearControls();
        configureTable();

        Node topBar = buildTopBar();
        setTop(topBar);

        BorderPane.setMargin(table, new Insets(0));
        setCenter(table);

        HBox bottom = new HBox(10, progress, statusLabel);
        bottom.setPadding(new Insets(8, 0, 0, 0));
        bottom.setAlignment(Pos.CENTER_LEFT);
        setBottom(bottom);
    }

    private void initMonthYearControls() {

        monthCombo.setItems(FXCollections.observableArrayList(Month.values()));
        monthCombo.setValue(YearMonth.now().getMonth());

        ObservableList<Integer> years = FXCollections.observableArrayList();
        int currentYear = YearMonth.now().getYear();
        for (int y = currentYear - 5; y <= currentYear + 5; y++) {
            years.add(y);
        }
        yearCombo.setItems(years);
        yearCombo.setValue(currentYear);

        monthCombo.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Month item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null
                        ? ""
                        : item.getDisplayName(TextStyle.FULL, Locale.getDefault()));
            }
        });

        monthCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Month item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null
                        ? ""
                        : item.getDisplayName(TextStyle.FULL, Locale.getDefault()));
            }
        });

        double height = 26;
        clientCombo.setPrefHeight(height);
        plantCombo.setPrefHeight(height);
        monthCombo.setPrefHeight(height);
        yearCombo.setPrefHeight(height);
    }

    private void wireEvents() {
        clientCombo.setPromptText("Client");
        plantCombo.setPromptText("Plant");

        monthCombo.valueProperty().addListener((obs, oldV, newV) -> reloadGridAsync());
        yearCombo.valueProperty().addListener((obs, oldV, newV) -> reloadGridAsync());

        clientCombo.valueProperty().addListener((obs, oldV, newV) -> {
            plantCombo.getItems().clear();
            tableData.clear();
            if (newV != null) {
                loadPlantsAsync(newV);
            }
        });

        plantCombo.valueProperty().addListener((obs, oldV, newV) -> reloadGridAsync());
    }

    private HBox buildTopBar() {
        Button reloadBtn = new Button("Reload");
        reloadBtn.getStyleClass().add("subtle-button");
        reloadBtn.setOnAction(e -> reloadGridAsync());

        addBtn.getStyleClass().add("primary-button");
        addBtn.setOnAction(e -> openAddDialog());

        HBox filters = new HBox(
                12,
                field("Client:", clientCombo),
                field("Plant:", plantCombo),
                field("Month:", monthCombo),
                field("Year:", yearCombo)
        );
        filters.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        HBox actions = new HBox(10, reloadBtn, addBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        HBox bar = new HBox(12, filters, spacer, actions);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 0, 10, 0));
        bar.getStyleClass().add("module-toolbar");

        return bar;
    }

    private HBox field(String labelText, Node input) {
        Label label = new Label(labelText);
        label.setMinHeight(26);
        label.setAlignment(Pos.CENTER_LEFT);

        if (input instanceof ComboBox<?> combo) {
            combo.setPrefWidth(150);
            combo.setMinWidth(120);
            combo.setMaxWidth(180);
        }

        HBox box = new HBox(6, label, input);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    public void load() {
        loadClientsAsync();
    }

    public void setClient(ClientEntity client) {
        clientCombo.setValue(client);
    }

    public void setPlant(PlantEntity plant) {
        plantCombo.setValue(plant);
    }

    public void setMonth(YearMonth month) {
        if (month == null) {
            return;
        }

        int year = month.getYear();
        if (!yearCombo.getItems().contains(year)) {
            yearCombo.getItems().add(year);
            FXCollections.sort(yearCombo.getItems());
        }

        monthCombo.setValue(month.getMonth());
        yearCombo.setValue(year);
    }

    private YearMonth getSelectedMonth() {
        Month month = monthCombo.getValue();
        Integer year = yearCombo.getValue();

        if (month == null || year == null) {
            return null;
        }

        return YearMonth.of(year, month);
    }

    private void configureTable() {
        table.setEditable(true);
        table.setItems(tableData);

        // Make the table fill all available width
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setMaxWidth(Double.MAX_VALUE);
        table.setPrefWidth(Region.USE_COMPUTED_SIZE);
        table.setMinHeight(300);

        TableColumn<MPRow, MaterialEntity> colMaterial = new TableColumn<>("Material");
        colMaterial.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().getMaterial()));
        colMaterial.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(MaterialEntity item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getCode());
            }
        });

        TableColumn<MPRow, BigDecimal> colLme =
                formattedBigDecCol("LME (USD/t)", MPRow::lmeProperty, 2);

        TableColumn<MPRow, BigDecimal> colFx =
                formattedBigDecCol("FX (USD→EUR)", MPRow::fxProperty, null);
        
        TableColumn<MPRow, BigDecimal> colPrime =
                editableBigDecCol(
                        "Prime (USD/t)",
                        MPRow::primeProperty,
                        2,
                        row -> !"AG".equalsIgnoreCase(row.getMaterial().getCode()),
                        (row, value) -> {
                            row.setPrime(safeBd(value));
                            persistAndRecalcRowAsync(row);
                        }
                );

        TableColumn<MPRow, BigDecimal> colFin =
                editableBigDecCol(
                        "Financial %",
                        MPRow::financialPercentProperty,
                        2,
                        row -> !"AG".equalsIgnoreCase(row.getMaterial().getCode()),
                        (row, value) -> {
                            row.setFinancialPercent(safeBd(value));
                            persistAndRecalcRowAsync(row);
                        }
                );

        TableColumn<MPRow, BigDecimal> colMgmt =
                editableBigDecCol(
                        "Management %",
                        MPRow::managementPercentProperty,
                        2,
                        row -> !"AG".equalsIgnoreCase(row.getMaterial().getCode()),
                        (row, value) -> {
                            row.setManagementPercent(safeBd(value));
                            persistAndRecalcRowAsync(row);
                        }
                );

        TableColumn<MPRow, BigDecimal> colFinal =
                formattedBigDecCol("Final (EUR/kg)", MPRow::finalEurPerKgProperty, 2);

        // Optional relative widths
        colMaterial.setMaxWidth(1f * Integer.MAX_VALUE * 12);
        colLme.setMaxWidth(1f * Integer.MAX_VALUE * 16);
        colFx.setMaxWidth(1f * Integer.MAX_VALUE * 14);
        colPrime.setMaxWidth(1f * Integer.MAX_VALUE * 16);
        colFin.setMaxWidth(1f * Integer.MAX_VALUE * 14);
        colMgmt.setMaxWidth(1f * Integer.MAX_VALUE * 14);
        colFinal.setMaxWidth(1f * Integer.MAX_VALUE * 14);

        table.getColumns().setAll(List.of(
                colMaterial,
                colLme,
                colFx,
                colPrime,
                colFin,
                colMgmt,
                colFinal
        ));
    }

    private TableColumn<MPRow, BigDecimal> formattedBigDecCol(
            String title,
            Function<MPRow, ObjectProperty<BigDecimal>> propFn,
            Integer scale
    ) {
        TableColumn<MPRow, BigDecimal> col = new TableColumn<>(title);
        col.setCellValueFactory(c -> propFn.apply(c.getValue()));

        col.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : formatBigDecimal(item, scale));
            }
        });

        return col;
    }

    private TableColumn<MPRow, BigDecimal> editableBigDecCol(
            String title,
            Function<MPRow, ObjectProperty<BigDecimal>> propFn,
            Integer scale,
            Predicate<MPRow> editablePredicate,
            BiConsumer<MPRow, BigDecimal> onCommit
    ) {
        TableColumn<MPRow, BigDecimal> col = new TableColumn<>(title);
        col.setCellValueFactory(c -> propFn.apply(c.getValue()));

        col.setCellFactory(tc -> new TableCell<>() {

            private final TextField textField = new TextField();
            private boolean committing = false;

            {
                textField.setOnAction(e -> commitFromTextField());

                textField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                    if (!isFocused && isEditing()) {
                        commitFromTextField();
                    }
                });

                textField.setOnKeyPressed(e -> {
                    switch (e.getCode()) {
                        case ESCAPE -> cancelEdit();
                        default -> {
                        }
                    }
                });
            }

            @Override
            public void startEdit() {
                if (getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    return;
                }

                MPRow row = getTableView().getItems().get(getIndex());

                if (row == null || !editablePredicate.test(row)) {
                    return;
                }

                super.startEdit();

                BigDecimal value = getItem();
                textField.setText(value == null ? "" : formatBigDecimal(value, scale));
                setText(null);
                setGraphic(textField);

                Platform.runLater(() -> {
                    textField.requestFocus();
                    textField.selectAll();
                });
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setGraphic(null);
                setText(getItem() == null ? "" : formatBigDecimal(getItem(), scale));
            }

            @Override
            public void commitEdit(BigDecimal newValue) {
                if (!isEditing()) {
                    return;
                }

                super.commitEdit(newValue);

                MPRow row = getTableView().getItems().get(getIndex());
                onCommit.accept(row, safeBd(newValue));

                setGraphic(null);
                setText(formatBigDecimal(safeBd(newValue), scale));
            }

            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                if (isEditing()) {
                    textField.setText(item == null ? "" : formatBigDecimal(item, scale));
                    setText(null);
                    setGraphic(textField);
                } else {
                    setGraphic(null);
                    setText(item == null ? "" : formatBigDecimal(item, scale));
                }
            }

            private void commitFromTextField() {
                if (committing) {
                    return;
                }

                committing = true;

                try {
                    BigDecimal value = parseDecimal(textField.getText(), title);
                    commitEdit(value);
                } catch (Exception ex) {
                    cancelEdit();
                    showError("Invalid value", ex);
                } finally {
                    committing = false;
                }
            }
        });

        return col;
    }

    private String formatBigDecimal(BigDecimal value, Integer scale) {
        if (value == null) {
            return "";
        }
        if (scale == null) {
            return value.stripTrailingZeros().toPlainString();
        }
        return value.setScale(scale, RoundingMode.HALF_UP).toPlainString();
    }

    private BigDecimal safeBd(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private void setBusy(boolean busy, String message) {
        progress.setVisible(busy);
        statusLabel.setText(message == null ? "" : message);

        clientCombo.setDisable(busy);
        plantCombo.setDisable(busy);
        monthCombo.setDisable(busy);
        yearCombo.setDisable(busy);
        table.setDisable(busy);
        addBtn.setDisable(busy);
    }

    private void loadClientsAsync() {
        Task<List<ClientEntity>> task = new Task<>() {
            @Override
            protected List<ClientEntity> call() throws Exception {
                return clientDAO.findAll();
            }
        };

        task.setOnRunning(e -> setBusy(true, "Loading clients..."));
        task.setOnSucceeded(e -> {
            setBusy(false, "");
            clientCombo.setItems(FXCollections.observableArrayList(task.getValue()));
        });
        task.setOnFailed(e -> {
            setBusy(false, "");
            showError("Failed to load clients", task.getException());
        });

        new Thread(task, "load-clients").start();
    }

    private void loadPlantsAsync(ClientEntity client) {
        Task<List<PlantEntity>> task = new Task<>() {
            @Override
            protected List<PlantEntity> call() throws Exception {
                return plantDAO.findByClientId(client.getId());
            }
        };

        task.setOnRunning(e -> setBusy(true, "Loading plants..."));
        task.setOnSucceeded(e -> {
            setBusy(false, "");
            plantCombo.setItems(FXCollections.observableArrayList(task.getValue()));
            if (!plantCombo.getItems().isEmpty()) {
                plantCombo.getSelectionModel().selectFirst();
            }
        });
        task.setOnFailed(e -> {
            setBusy(false, "");
            showError("Failed to load plants", task.getException());
        });

        new Thread(task, "load-plants").start();
    }
    
    private void openAddDialog() {
        try {
            List<MaterialEntity> allMaterials = materialDAO.findAll();

            List<MaterialEntity> cuAlMaterials = allMaterials.stream()
                    .filter(m -> {
                        String name = m.getCode() == null ? "" : m.getCode().trim().toLowerCase();
                        return name.equals("cu") || name.equals("copper")
                                || name.equals("al") || name.equals("aluminium") || name.equals("aluminum");
                    })
                    .toList();

            MaterialEntity silverMaterial = allMaterials.stream()
                    .filter(m -> {
                        String name = m.getCode() == null ? "" : m.getCode().trim().toLowerCase();
                        return name.equals("ag") || name.equals("silver");
                    })
                    .findFirst()
                    .orElse(null);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Add MP Entry");
            dialog.setHeaderText("Create a new MP entry");
            dialog.getDialogPane().getStylesheets().add(StyleUtils.themeCss(getClass()));
            dialog.getDialogPane().getStyleClass().add("form-window");
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
            okButton.getStyleClass().add("primary-button");

            Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
            cancelButton.getStyleClass().add("subtle-button");

            ComboBox<MpEntryType> entryTypeCombo = new ComboBox<>();
            entryTypeCombo.getItems().setAll(MpEntryType.values());
            entryTypeCombo.setValue(MpEntryType.METAL_MARKET_DATA);

            ComboBox<ClientEntity> dialogClient = new ComboBox<>();
            dialogClient.setEditable(true);
            dialogClient.setItems(FXCollections.observableArrayList(clientCombo.getItems()));
            dialogClient.setValue(clientCombo.getValue());

            ComboBox<PlantEntity> dialogPlant = new ComboBox<>();
            dialogPlant.setEditable(true);

            dialogClient.setConverter(new javafx.util.StringConverter<>() {
                @Override
                public String toString(ClientEntity client) {
                    return client == null ? "" : client.getName();
                }

                @Override
                public ClientEntity fromString(String string) {
                    return null;
                }
            });

            dialogPlant.setConverter(new javafx.util.StringConverter<>() {
                @Override
                public String toString(PlantEntity plant) {
                    return plant == null ? "" : plant.getName();
                }

                @Override
                public PlantEntity fromString(String string) {
                    return null;
                }
            });

            if (dialogClient.getValue() != null) {
                dialogPlant.setItems(FXCollections.observableArrayList(
                        plantDAO.findByClientId(dialogClient.getValue().getId())
                ));
                if (plantCombo.getValue() != null) {
                    dialogPlant.setValue(plantCombo.getValue());
                } else if (!dialogPlant.getItems().isEmpty()) {
                    dialogPlant.getSelectionModel().selectFirst();
                }
            }

            dialogClient.valueProperty().addListener((obs, oldV, newV) -> {
                dialogPlant.getItems().clear();
                dialogPlant.setValue(null);

                if (newV != null) {
                    try {
                        dialogPlant.setItems(FXCollections.observableArrayList(
                                plantDAO.findByClientId(newV.getId())
                        ));
                        if (!dialogPlant.getItems().isEmpty()) {
                            dialogPlant.getSelectionModel().selectFirst();
                        }
                    } catch (Exception ex) {
                        showError("Failed to load plants", ex);
                    }
                }
            });

            DatePicker periodPicker = new DatePicker(
            	    getSelectedMonth() == null ? null : getSelectedMonth().atDay(1)
            	);

            ComboBox<MaterialEntity> materialCombo = new ComboBox<>();
            materialCombo.setConverter(new javafx.util.StringConverter<>() {
                @Override
                public String toString(MaterialEntity material) {
                    return material == null ? "" : material.getCode();
                }

                @Override
                public MaterialEntity fromString(String string) {
                    return null;
                }
            });

            TextField lmeField = new TextField();
            TextField fxField = new TextField();
            TextField primeField = new TextField();
            TextField financialField = new TextField();
            TextField managementField = new TextField();
            TextField silverBaseField = new TextField();

            Label clientLabel = new Label("Client:");
            Label plantLabel = new Label("Plant:");
            Label materialLabel = new Label("Material:");
            Label lmeLabel = new Label("LME:");
            Label fxLabel = new Label("FX:");
            Label primeLabel = new Label("Prime:");
            Label financialLabel = new Label("Financial %:");
            Label managementLabel = new Label("Management %:");
            Label silverBaseLabel = new Label("Base price €/kg:");

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(10));

            int r = 0;
            grid.add(new Label("Entry type:"), 0, r);
            grid.add(entryTypeCombo, 1, r++);

            grid.add(new Label("Month:"), 0, r);
            grid.add(periodPicker, 1, r++);

            grid.add(clientLabel, 0, r);
            grid.add(dialogClient, 1, r++);

            grid.add(plantLabel, 0, r);
            grid.add(dialogPlant, 1, r++);

            grid.add(materialLabel, 0, r);
            grid.add(materialCombo, 1, r++);

            grid.add(lmeLabel, 0, r);
            grid.add(lmeField, 1, r++);

            grid.add(fxLabel, 0, r);
            grid.add(fxField, 1, r++);            

            grid.add(primeLabel, 0, r);
            grid.add(primeField, 1, r++);

            grid.add(financialLabel, 0, r);
            grid.add(financialField, 1, r++);

            grid.add(managementLabel, 0, r);
            grid.add(managementField, 1, r++);

            grid.add(silverBaseLabel, 0, r);
            grid.add(silverBaseField, 1, r++);

            Runnable refreshForm = () -> {
                MpEntryType type = entryTypeCombo.getValue();

                boolean isMarket = type == MpEntryType.METAL_MARKET_DATA;
                boolean isMarkup = type == MpEntryType.CLIENT_PLANT_MARKUP;
                boolean isSilver = type == MpEntryType.SILVER_BASE_PRICE;

                setRowVisible(grid, clientLabel, dialogClient, isMarkup);
                setRowVisible(grid, plantLabel, dialogPlant, isMarkup);

                setRowVisible(grid, materialLabel, materialCombo, isMarket || isMarkup);
                setRowVisible(grid, lmeLabel, lmeField, isMarket);
                setRowVisible(grid, fxLabel, fxField, isMarket);

                setRowVisible(grid, primeLabel, primeField, isMarkup);
                setRowVisible(grid, financialLabel, financialField, isMarkup);
                setRowVisible(grid, managementLabel, managementField, isMarkup);

                setRowVisible(grid, silverBaseLabel, silverBaseField, isSilver);

                if (isMarket || isMarkup) {
                    materialCombo.setItems(FXCollections.observableArrayList(cuAlMaterials));
                    if (materialCombo.getValue() == null && !materialCombo.getItems().isEmpty()) {
                        materialCombo.getSelectionModel().selectFirst();
                    }
                } else if (isSilver) {
                    materialCombo.getItems().clear();
                    materialCombo.setValue(null);
                }

                if (isSilver && silverMaterial == null) {
                    showError("Missing silver material", new IllegalArgumentException("No material named Ag/Silver found."));
                }
            };

            entryTypeCombo.valueProperty().addListener((obs, oldV, newV) -> refreshForm.run());
            refreshForm.run();

            dialog.getDialogPane().setContent(grid);
            dialog.getDialogPane().setPrefSize(450, 530);
            dialog.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            dialog.setResizable(true);

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return;
            }

            MpEntryType type = entryTypeCombo.getValue();
            LocalDate selectedDate = periodPicker.getValue();

            if (selectedDate == null) {
                throw new IllegalArgumentException("Month is required.");
            }

            YearMonth period = YearMonth.from(selectedDate);

            if (type == MpEntryType.METAL_MARKET_DATA) {
                MaterialEntity material = materialCombo.getValue();
                if (material == null) {
                    throw new IllegalArgumentException("Material is required.");
                }

                BigDecimal lme = parseDecimal(lmeField.getText(), "LME");
                BigDecimal fx = parseDecimal(fxField.getText(), "FX");

                saveMetalMarketDataAsync(material, period, lme, fx);

            } else if (type == MpEntryType.CLIENT_PLANT_MARKUP) {
                ClientEntity client = resolveOrCreateClient(dialogClient);
                PlantEntity plant = resolveOrCreatePlant(dialogPlant, client);
                MaterialEntity material = materialCombo.getValue();

                if (material == null) {
                    throw new IllegalArgumentException("Material is required.");
                }

                BigDecimal prime = parseDecimal(primeField.getText(), "Prime");
                BigDecimal financial = parseDecimal(financialField.getText(), "Financial %");
                BigDecimal management = parseDecimal(managementField.getText(), "Management %");

                saveMarkupAsync(client, plant, material, period, prime, financial, management);

            } else if (type == MpEntryType.SILVER_BASE_PRICE) {
                if (silverMaterial == null) {
                    throw new IllegalArgumentException("No Ag/Silver material found in database.");
                }

                BigDecimal silverBase = parseDecimal(silverBaseField.getText(), "Base price €/kg");
                saveSilverBasePriceAsync(silverMaterial, period, silverBase);
            }

        } catch (Exception ex) {
            showError("Failed to add MP entry", ex);
        }
    }
    
    private void setRowVisible(GridPane grid, Label label, Node field, boolean visible) {
        label.setVisible(visible);
        label.setManaged(visible);
        field.setVisible(visible);
        field.setManaged(visible);
    }

    private BigDecimal parseDecimal(String text, String fieldName) {
        String value = text == null ? "" : text.trim();

        if (value.isEmpty()) {
            return BigDecimal.ZERO;
        }

        try {
            return new BigDecimal(value.replace(",", "."));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " must be a valid number.");
        }
    }
       
    private void saveMetalMarketDataAsync(MaterialEntity material,
            YearMonth month,
            BigDecimal lme,
            BigDecimal fx) {
    	Task<Void> task = new Task<>() {
    		@Override
    		protected Void call() throws Exception {
    			mPService.saveOrUpdateMetalMarketPrice(material, month, lme, fx);
    			return null;
    		}
    	};

    	task.setOnRunning(e -> setBusy(true, "Saving market data..."));
    	task.setOnSucceeded(e -> {
    		setBusy(false, "");
    		setMonth(month);
    		reloadGridAsync();
    	});
    	task.setOnFailed(e -> {
    		setBusy(false, "");
    		showError("Failed to save market data", task.getException());
    	});

    	new Thread(task, "save-market-data").start();
    }

    private void saveMarkupAsync(ClientEntity client,
            PlantEntity plant,
            MaterialEntity material,
            YearMonth month,
            BigDecimal prime,
            BigDecimal financialPercent,
            BigDecimal managementPercent) {
    	
    	Task<Void> task = new Task<>() {
    		@Override
    		protected Void call() throws Exception {
    			mPService.saveOrUpdateMarkup(
    					client,
    					plant,
    					material,
    					month,
    					prime,
    					financialPercent,
    					managementPercent
    					);
    			return null;
    			}
    		};
    		
    		task.setOnRunning(e -> setBusy(true, "Saving markup..."));
    		
    		task.setOnSucceeded(e -> {
    			setBusy(false, "");
        		clientCombo.setValue(client);
        		plantCombo.setValue(plant);
        		setMonth(month);
        		reloadGridAsync();    		
        	});

    		task.setOnFailed(e -> {
    			setBusy(false, "");
    			showError("Failed to save markup", task.getException());
    		});

    		new Thread(task, "save-markup").start();
    }

    private void saveSilverBasePriceAsync(MaterialEntity silverMaterial,
    		YearMonth month,
            BigDecimal eurPerKg) {

    	Task<Void> task = new Task<>() {
    		@Override
    		protected Void call() throws Exception {
    			mPService.saveOrUpdateSilverBasePrice(silverMaterial, month, eurPerKg);
    			return null;
    		}
    	};

    	task.setOnRunning(e -> setBusy(true, "Saving silver base price..."));
    	task.setOnSucceeded(e -> {
    		setBusy(false, "");
    		setMonth(month);
    		reloadGridAsync();
    	});
    	task.setOnFailed(e -> {
    		setBusy(false, "");
    		showError("Failed to save silver base price", task.getException());
    	});

    	new Thread(task, "save-silver-base").start();
    }

    private void reloadGridAsync() {
        ClientEntity client = clientCombo.getValue();
        PlantEntity plant = plantCombo.getValue();
        YearMonth month = getSelectedMonth();

        if (client == null || plant == null || month == null) {
            return;
        }

        Task<List<MPRow>> task = new Task<>() {
            @Override
            protected List<MPRow> call() throws Exception {
                return mPService.loadPricingGrid(client, plant, month);
            }
        };

        task.setOnRunning(e -> setBusy(true, "Loading pricing grid..."));

        task.setOnSucceeded(e -> {
            setBusy(false, "");
            tableData.setAll(task.getValue().stream().map(this::toModel).toList());
        });

        task.setOnFailed(e -> {
            setBusy(false, "");
            showError("Failed to load pricing grid", task.getException());
        });

        new Thread(task, "load-grid").start();
    }

    private MPRow toModel(MPRow r) {
        return new MPRow(
                r.getMaterial(),
                r.getPeriod(),
                r.getLme(),
                r.getPrime(),
                r.getFx(),
                r.getFinancialPercent(),
                r.getManagementPercent(),
                r.getFinalEurPerKg()
        );
    }

    private void persistAndRecalcRowAsync(MPRow row) {
        ClientEntity client = clientCombo.getValue();
        PlantEntity plant = plantCombo.getValue();
        YearMonth month = getSelectedMonth();

        if (client == null || plant == null || month == null) {
            return;
        }

        Task<BigDecimal> task = new Task<>() {
            @Override
            protected BigDecimal call() throws Exception {
                mPService.saveOrUpdateMarkup(
                        client,
                        plant,
                        row.getMaterial(),
                        month,
                        row.getPrime(),
                        row.getFinancialPercent(),
                        row.getManagementPercent());

                MarketPriceEntity market = new MarketPriceEntity();
                market.setMaterial(row.getMaterial());
                market.setPeriod(month);
                market.setLme(row.getLme());
                market.setFx(row.getFx());
                
                ClientMarkupEntity markup = new ClientMarkupEntity(
                        client,
                        plant,
                        row.getMaterial(),
                        month,
                        row.getPrime(),
                        row.getFinancialPercent(),
                        row.getManagementPercent()
                );

                return mPService.calculateFinalEurPerKg(market, markup);
                            
            }
        };

        task.setOnRunning(e -> setBusy(true, "Saving markup..."));

        task.setOnSucceeded(e -> {
            setBusy(false, "");
            row.setFinalEurPerKg(task.getValue());
            reloadGridAsync();
        });

        task.setOnFailed(e -> {
            setBusy(false, "");
            showError("Failed to save markup", task.getException());
            reloadGridAsync();
        });

        new Thread(task, "save-markup").start();
    }

    private void showError(String title, Throwable ex) {
        ex.printStackTrace();
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(title);
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        });
    }
    
    // Helpers
    private ClientEntity resolveOrCreateClient(ComboBox<ClientEntity> combo) throws Exception {
        ClientEntity selected = combo.getValue();

        // If user selected an existing client
        if (selected != null && selected.getId() != 0) {
            return selected;
        }

        String typed = combo.getEditor().getText();
        if (typed == null || typed.isBlank()) {
            throw new IllegalArgumentException("Client is required.");
        }

        String name = typed.trim();

        // Check if already exists
        Optional<ClientEntity> existing = clientDAO.findByName(name);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Create new client
        ClientEntity newClient = new ClientEntity();
        newClient.setName(name);

        clientDAO.save(newClient);

        return newClient;
    }
    
    private PlantEntity resolveOrCreatePlant(ComboBox<PlantEntity> combo, ClientEntity client) throws Exception {
        PlantEntity selected = combo.getValue();

        if (selected != null && selected.getId() != 0) {
            return selected;
        }

        String typed = combo.getEditor().getText();
        if (typed == null || typed.isBlank()) {
            throw new IllegalArgumentException("Plant is required.");
        }

        String name = typed.trim();

        Optional<PlantEntity> existing = plantDAO.findByClientIdAndName(client.getId(), name);
        if (existing.isPresent()) {
            return existing.get();
        }

        PlantEntity newPlant = new PlantEntity();
        newPlant.setClient(client);   // ← THIS replaces setClientId
        newPlant.setName(name);

        plantDAO.save(newPlant);

        return newPlant;
    }
}