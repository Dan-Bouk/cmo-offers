package com.cmo.offers.model.table;

import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.cmo.offers.dao.ClientDAO;
import com.cmo.offers.dao.ClientMarkupDAO;
import com.cmo.offers.dao.MarketPriceDAO;
import com.cmo.offers.dao.MaterialDAO;
import com.cmo.offers.dao.OfferRefDAO;
import com.cmo.offers.dao.PlantDAO;
import com.cmo.offers.entity.ClientEntity;
import com.cmo.offers.entity.OfferEntity;
import com.cmo.offers.export.service.OfferExportService;
import com.cmo.offers.load.service.OfferImportService;

import com.cmo.offers.model.row.OfferRefJoinRow;
import com.cmo.offers.model.row.OfferTreeRow;
import com.cmo.offers.service.OfferActionController;
import com.cmo.offers.service.OfferFileStateService;
import com.cmo.offers.ui.service.OfferService;
import com.cmo.offers.ui.service.RawMaterialService;
import com.cmo.offers.ui.window.ReferenceWindowManager;

public class OfferListTable extends BorderPane {

	
    private final OfferService offerService;
    private final ClientDAO clientDAO;
    @SuppressWarnings("unused")
	private final ClientMarkupDAO clientMarkupDAO;
    @SuppressWarnings("unused")
    private final MaterialDAO materialDAO;
    @SuppressWarnings("unused")
    private final MarketPriceDAO marketPriceDAO;
    @SuppressWarnings("unused")
    private final PlantDAO plantDAO;
    @SuppressWarnings("unused")
    private final RawMaterialService rawMaterialService;
    @SuppressWarnings("unused")
    private final OfferRefDAO offerRefDAO;
    private final ReferenceWindowManager windowManager;
    private final OfferFileStateService fileStateService;
    private final OfferActionController actionController;
    private final Runnable onNewOffer;

    @SuppressWarnings("unused")
    private final OfferExportService exportService;    
    @SuppressWarnings("unused")
    private final OfferImportService importService;
    
    private final ComboBox<ClientEntity> clientFilter = new ComboBox<>();
    private final Button refreshBtn = new Button("Refresh");
    private final Button newOfferBtn = new Button("New Offer");
    private final Button importBtn = new Button("Import JSON");

    private final TreeTableView<OfferTreeRow> treeTable = new TreeTableView<>();

    public OfferListTable(
            OfferService offerService,
            ClientDAO clientDAO,
            ClientMarkupDAO clientMarkupDAO,
            PlantDAO plantDAO,
            MaterialDAO materialDAO,
            MarketPriceDAO marketPriceDAO,
            RawMaterialService rawMaterialService,
            OfferRefDAO offerRefDAO,
            OfferExportService exportService,
            OfferImportService importService,
            ReferenceWindowManager windowManager,
            OfferFileStateService fileStateService,
            Runnable onNewOffer
    ) {
        this.offerService = offerService;
        this.clientDAO = clientDAO;
        this.clientMarkupDAO = clientMarkupDAO;
        this.plantDAO = plantDAO;
        this.materialDAO = materialDAO;
        this.marketPriceDAO = marketPriceDAO;
        this.rawMaterialService = rawMaterialService;
        this.offerRefDAO = offerRefDAO;
        this.exportService = exportService;
        this.importService = importService;
        this.windowManager = windowManager;
        this.fileStateService = fileStateService;
        this.onNewOffer = onNewOffer;    
        
        this.actionController = new OfferActionController(
                offerService,
                fileStateService,
                exportService,
                importService,
                windowManager,
                clientDAO,
                offerRefDAO,
                this::refresh,
                treeTable::refresh,
                this::getOwningStage
        );

        setPadding(new Insets(10));
        setTop(buildTopBar());
        setCenter(buildTreeTable());
        getStyleClass().add("content-card");

        wireActions();
        loadClientsAsync();
        loadTreeAsync(null);
    }

    private Node buildTopBar() {
        clientFilter.setPromptText("All clients");
        clientFilter.setPrefWidth(220);

        refreshBtn.getStyleClass().add("subtle-button");
        newOfferBtn.getStyleClass().add("primary-button");
        importBtn.getStyleClass().add("subtle-button");

        HBox bar = new HBox(
                10,
                new Label("Client:"),
                clientFilter,
                refreshBtn,
                newOfferBtn,
                importBtn
        );
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 0, 10, 0));
        bar.getStyleClass().add("module-toolbar");

        return bar;
    }

    private Node buildTreeTable() {
        treeTable.setShowRoot(false);
        treeTable.setEditable(true);
        treeTable.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        StringConverter<LocalDate> localDateConverter = new StringConverter<>() {
            @Override
            public String toString(LocalDate value) {
                return value == null ? "" : value.toString();
            }

            @Override
            public LocalDate fromString(String text) {
                if (text == null || text.isBlank()) {
                    return null;
                }
                return LocalDate.parse(text.trim());
            }
        };

        TreeTableColumn<OfferTreeRow, String> offerNrCol = new TreeTableColumn<>("Offer Nr.");
        offerNrCol.setCellValueFactory(param -> param.getValue().getValue().offerNrProperty());
        offerNrCol.setCellFactory(col -> new TextFieldTreeTableCell<>(new DefaultStringConverter()) {
            @Override
            public void startEdit() {
                OfferTreeRow row = getRowValue(getTreeTableView(), getIndex());
                if (row == null || row.isReferenceRow()) {
                    return;
                }
                super.startEdit();
            }
        });
        offerNrCol.setOnEditCommit(event -> {
            OfferTreeRow row = event.getRowValue() == null ? null : event.getRowValue().getValue();
            if (row != null && !row.isReferenceRow()) {
                row.setOfferNr(event.getNewValue());
            }
        });

        TreeTableColumn<OfferTreeRow, String> customerCol = new TreeTableColumn<>("Customer");
        customerCol.setCellValueFactory(param -> param.getValue().getValue().customerNameProperty());
        customerCol.setCellFactory(col -> new TextFieldTreeTableCell<>(new DefaultStringConverter()) {
            @Override
            public void startEdit() {
                OfferTreeRow row = getRowValue(getTreeTableView(), getIndex());
                if (row == null || row.isReferenceRow()) {
                    return;
                }
                super.startEdit();
            }
        });
        customerCol.setOnEditCommit(event -> {
            OfferTreeRow row = event.getRowValue() == null ? null : event.getRowValue().getValue();
            if (row != null && !row.isReferenceRow()) {
                row.setCustomerName(event.getNewValue());
            }
        });

        TreeTableColumn<OfferTreeRow, String> requestCol = new TreeTableColumn<>("Your request");
        requestCol.setCellValueFactory(param -> param.getValue().getValue().requestNrProperty());
        requestCol.setCellFactory(col -> new TextFieldTreeTableCell<>(new DefaultStringConverter()) {
            @Override
            public void startEdit() {
                OfferTreeRow row = getRowValue(getTreeTableView(), getIndex());
                if (row == null || row.isReferenceRow()) {
                    return;
                }
                super.startEdit();
            }
        });
        requestCol.setOnEditCommit(event -> {
            OfferTreeRow row = event.getRowValue() == null ? null : event.getRowValue().getValue();
            if (row != null && !row.isReferenceRow()) {
                row.setRequestNr(event.getNewValue());
            }
        });

        TreeTableColumn<OfferTreeRow, LocalDate> dateCol = new TreeTableColumn<>("Offer Date");
        dateCol.setCellValueFactory(param -> param.getValue().getValue().offerDateProperty());
        dateCol.setCellFactory(col -> new TextFieldTreeTableCell<>(localDateConverter) {
            @Override
            public void startEdit() {
                OfferTreeRow row = getRowValue(getTreeTableView(), getIndex());
                if (row == null || row.isReferenceRow()) {
                    return;
                }
                super.startEdit();
            }
        });
        dateCol.setOnEditCommit(event -> {
            OfferTreeRow row = event.getRowValue() == null ? null : event.getRowValue().getValue();
            if (row != null && !row.isReferenceRow()) {
                row.setOfferDate(event.getNewValue());
            }
        });

        TreeTableColumn<OfferTreeRow, String> revCol = new TreeTableColumn<>("Rev.");
        revCol.setCellValueFactory(param -> param.getValue().getValue().revisionProperty());
        revCol.setCellFactory(col -> new TextFieldTreeTableCell<>(new DefaultStringConverter()) {
            @Override
            public void startEdit() {
                OfferTreeRow row = getRowValue(getTreeTableView(), getIndex());
                if (row == null || row.isReferenceRow()) {
                    return;
                }
                super.startEdit();
            }
        });
        revCol.setOnEditCommit(event -> {
            OfferTreeRow row = event.getRowValue() == null ? null : event.getRowValue().getValue();
            if (row != null && !row.isReferenceRow()) {
                row.setRevision(event.getNewValue());
            }
        });

        TreeTableColumn<OfferTreeRow, String> refDocCol = new TreeTableColumn<>("Reference Doc");
        refDocCol.setCellValueFactory(param -> param.getValue().getValue().referenceDocProperty());

        refDocCol.setCellFactory(col -> new TreeTableCell<>() {
            private final Label linkLabel = new Label();
            private final ListChangeListener<Integer> selectionListener = change -> updateLabelAppearance();

            {
                linkLabel.getStyleClass().add("reference-link");
                linkLabel.setStyle("-fx-cursor: hand;");

                linkLabel.setOnMouseClicked(e -> {
                    OfferTreeRow row = getCurrentRow();
                    if (row != null && row.isReferenceRow()) {
                    	try {
                    	    windowManager.openReference(row, getOwningStage(), updatedOffer -> syncAfterReferenceUpdate(
                    	            updatedOffer,
                    	            row.getReferenceId()));
                    	}
                    	catch (Exception ex) {
                    	    showError("Failed to open reference details", ex);
                    	    ex.printStackTrace();
                    	}
                    }
                });

                treeTableViewProperty().addListener((obs, oldTable, newTable) -> {
                    if (oldTable != null) {
                        oldTable.getSelectionModel().getSelectedIndices().removeListener(selectionListener);
                    }
                    if (newTable != null) {
                        newTable.getSelectionModel().getSelectedIndices().addListener(selectionListener);
                    }
                });
            }

            private OfferTreeRow getCurrentRow() {
                return getRowValue(getTreeTableView(), getIndex());
            }

            private void updateLabelAppearance() {
                TreeTableView<OfferTreeRow> table = getTreeTableView();
                boolean rowSelected =
                        table != null
                        && getIndex() >= 0
                        && table.getSelectionModel().isSelected(getIndex());

                linkLabel.getStyleClass().removeAll("reference-link", "reference-link-selected");
                linkLabel.getStyleClass().add(rowSelected ? "reference-link-selected" : "reference-link");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                OfferTreeRow row = getCurrentRow();
                if (row != null && row.isReferenceRow()) {
                    linkLabel.setText(item);
                    updateLabelAppearance();
                    setText(null);
                    setGraphic(linkLabel);
                } else {
                    setGraphic(null);
                    setText(item);
                }
            }
        });

        TreeTableColumn<OfferTreeRow, Void> actionCol = new TreeTableColumn<>("Actions");
        actionCol.setPrefWidth(220);
        actionCol.setCellFactory(col -> new TreeTableCell<>() {
            private final Button addReferenceBtn = createPrimaryActionButton("Add reference");
            private final MenuItem saveChangesItem = new MenuItem("Save Changes");
            private final MenuItem duplicateItem = new MenuItem("Duplicate");
            private final MenuItem exportJsonItem = new MenuItem("Export JSON");
            private final MenuItem exportExcelItem = new MenuItem("Export Excel");
            private final MenuItem deleteItem = new MenuItem("Delete");
            private final MenuButton moreBtn = new MenuButton(
                    "More",
                    null,
                    saveChangesItem,
                    new SeparatorMenuItem(),
                    duplicateItem,
                    new SeparatorMenuItem(),
                    exportJsonItem,
                    exportExcelItem,
                    new SeparatorMenuItem(),
                    deleteItem
            );
            private final HBox actionsBox = new HBox(8, addReferenceBtn, moreBtn);

            {
                actionsBox.setAlignment(Pos.CENTER_LEFT);
                moreBtn.getStyleClass().add("subtle-button");

                addReferenceBtn.setOnAction(e -> {
                    OfferTreeRow row = getCurrentRow();
                    if (row != null && !row.isReferenceRow()) {
                        openAddReferenceWindow(row);
                    }
                });

                saveChangesItem.setOnAction(e -> {
                    OfferTreeRow row = getCurrentRow();
                    if (row != null && !row.isReferenceRow()) {
                    	actionController.saveOfferRowChanges(row);                    
                    }
                });

                duplicateItem.setOnAction(e -> {
                    OfferTreeRow row = getCurrentRow();
                    if (row != null && !row.isReferenceRow()) {
                    	actionController.duplicateOffer(row);                    
                    }
                });

                exportJsonItem.setOnAction(e -> {
                    OfferTreeRow row = getCurrentRow();
                    if (row != null && !row.isReferenceRow()) {
                    	actionController.exportOfferRowToJson(row);
                    }
                });

                exportExcelItem.setOnAction(e -> {
                    OfferTreeRow row = getCurrentRow();
                    if (row != null && !row.isReferenceRow()) {
                    	actionController.exportOfferRowToExcel(row);
                    }
                });

                deleteItem.setOnAction(e -> {
                    OfferTreeRow row = getCurrentRow();
                    if (row != null && !row.isReferenceRow()) {
                    	actionController.deleteOffer(row);
                    }
                });
            }

            private OfferTreeRow getCurrentRow() {
                return getRowValue(getTreeTableView(), getIndex());
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                    return;
                }

                OfferTreeRow row = getCurrentRow();
                if (row == null || row.isReferenceRow()) {
                    setGraphic(null);
                } else {
                    setGraphic(actionsBox);
                }
            }
        });

        treeTable.getColumns().setAll(List.of(
                offerNrCol,
                customerCol,
                requestCol,
                dateCol,
                revCol,
                refDocCol,
                actionCol
        ));

        return treeTable;
    }

    private OfferTreeRow getRowValue(TreeTableView<OfferTreeRow> table, int index) {
        if (table == null || index < 0 || index >= table.getExpandedItemCount()) {
            return null;
        }

        TreeItem<OfferTreeRow> item = table.getTreeItem(index);
        return item == null ? null : item.getValue();
    }

    private Button createPrimaryActionButton(String text) {
        Button button = new Button(text);
        button.setMinWidth(Button.USE_PREF_SIZE);
        button.getStyleClass().add("primary-button");
        HBox.setHgrow(button, Priority.NEVER);
        return button;
    }

    private void wireActions() {
        refreshBtn.setOnAction(e -> refresh());

        clientFilter.valueProperty().addListener((obs, oldValue, newValue) ->
                loadTreeAsync(newValue)
        );

        newOfferBtn.setOnAction(e -> {
            if (onNewOffer != null) {
                onNewOffer.run();
            }
        });
        
        importBtn.setOnAction(e -> actionController.importOfferJson());
    }

    public void refresh() {
        loadTreeAsync(clientFilter.getValue());
    }

    private void loadClientsAsync() {
        Task<List<ClientEntity>> task = new Task<>() {
            @Override
            protected List<ClientEntity> call() throws Exception {
                return clientDAO.findAll();
            }
        };

        task.setOnSucceeded(e -> {
            clientFilter.getItems().setAll(task.getValue());
            clientFilter.getSelectionModel().clearSelection();
        });

        task.setOnFailed(e -> showError("Failed to load clients", task.getException()));
        new Thread(task, "load-clients").start();
    }

    private void loadTreeAsync(ClientEntity client) {
        Task<List<OfferRefJoinRow>> task = new Task<>() {
            @Override
            protected List<OfferRefJoinRow> call() throws Exception {
                if (client == null) {
                    return offerService.getOfferTreeRows();
                } else {
                    return offerService.getOfferTreeRowsByClient(client);
                }
            }
        };

        task.setOnSucceeded(e -> {
            TreeItem<OfferTreeRow> root = buildTreeRoot(task.getValue());
            treeTable.setRoot(root);
        });

        task.setOnFailed(e -> showError("Failed to load offers", task.getException()));
        new Thread(task, "load-offer-tree").start();
    }

    private TreeItem<OfferTreeRow> buildTreeRoot(List<OfferRefJoinRow> rows) {
        TreeItem<OfferTreeRow> root =
                new TreeItem<>(OfferTreeRow.offerRow(0, "", "", "", null, ""));

        Map<Integer, TreeItem<OfferTreeRow>> offerItems = new LinkedHashMap<>();

        for (OfferRefJoinRow row : rows) {
            TreeItem<OfferTreeRow> offerItem = offerItems.get(row.getOfferId());

            if (offerItem == null) {
                OfferTreeRow offerRow = OfferTreeRow.offerRow(
                        row.getOfferId(),
                        row.getCustomerName(),
                        row.getRequestNr(),
                        row.getOfferNr(),
                        row.getOfferDate(),
                        row.getRevision()
                );

                offerItem = new TreeItem<>(offerRow);
                offerItem.setExpanded(true);

                offerItems.put(row.getOfferId(), offerItem);
                root.getChildren().add(offerItem);
            }

            if (row.getReferenceId() != null && row.getReferenceDoc() != null) {
                OfferTreeRow refRow = OfferTreeRow.referenceRow(
                        row.getOfferId(),
                        row.getReferenceId(),
                        row.getReferenceDoc()
                );

                offerItem.getChildren().add(new TreeItem<>(refRow));
            }
        }

        return root;
    }

    private void openAddReferenceWindow(OfferTreeRow offerRow) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Reference");
        dialog.setHeaderText("Add reference to offer " + offerRow.getOfferNr());
        dialog.setContentText("Reference name:");

        DialogPane pane = dialog.getDialogPane();
        if (pane.lookupButton(ButtonType.OK) instanceof Button okBtn) {
            okBtn.getStyleClass().add("primary-button");
        }
        if (pane.lookupButton(ButtonType.CANCEL) instanceof Button cancelBtn) {
            cancelBtn.getStyleClass().add("subtle-button");
        }

        dialog.showAndWait().ifPresent(referenceName -> {
            String trimmed = referenceName == null ? "" : referenceName.trim();

            if (trimmed.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Validation");
                alert.setHeaderText("Validation");
                alert.setContentText("Reference name is required.");
                alert.showAndWait();
                return;
            }

            actionController.saveReference(
                    offerRow.getOfferId(),
                    trimmed
            );        });
    }    
        
    private Stage getOwningStage() {
        Scene scene = getScene();
        if (scene == null) {
            return null;
        }
        Window window = scene.getWindow();
        return window instanceof Stage ? (Stage) window : null;
    }
    

    
    // Synchronize the application after a reference updated an offer
    private void syncAfterReferenceUpdate(
            OfferEntity updatedOffer,
            Integer sourceReferenceId) {

        if (updatedOffer == null) {
            return;
        }

        refresh();

        String updatedCustomerName = null;

        try {
            updatedCustomerName = clientDAO.findById(updatedOffer.getClientId())
                    .map(ClientEntity::getName)
                    .orElse(null);
        } catch (Exception ex) {
            showError("Failed to reload customer", ex);
            return;
        }

        try {
            fileStateService.persistOfferHeaderToAllReferenceStateFiles(
                    updatedOffer,
                    updatedCustomerName);
        } catch (Exception ex) {
            showError("Failed to update saved reference files", ex);
        }

        windowManager.notifyOfferUpdated(
                updatedOffer,
                updatedCustomerName,
                sourceReferenceId
        );
    }    
    
    private void showError(String title, Throwable ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(
                ex == null || ex.getMessage() == null
                        ? "Unknown error"
                        : ex.getMessage()
        );
        alert.showAndWait();
    }
}