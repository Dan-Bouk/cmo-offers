package com.cmo.offers.ui.controller;

import java.io.File;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import com.cmo.offers.dao.MarketPriceDAO;
import com.cmo.offers.dao.MaterialDAO;
import com.cmo.offers.dao.OfferDAO;
import com.cmo.offers.dao.OfferRefDAO;
import com.cmo.offers.entity.ClientEntity;
import com.cmo.offers.entity.OfferEntity;
import com.cmo.offers.entity.OfferRefEntity;
import com.cmo.offers.entity.PlantEntity;
import com.cmo.offers.export.service.OfferJsonService;
import com.cmo.offers.model.OfferBundle;
import com.cmo.offers.model.OfferExportModel;
import com.cmo.offers.model.OfferTabSheet;
import com.cmo.offers.model.ReferenceExportModel;
import com.cmo.offers.model.ExportRow;
import com.cmo.offers.model.row.ComponentsRow;
import com.cmo.offers.model.row.GeneralInfoRow;
import com.cmo.offers.model.row.RawMaterialRow;
import com.cmo.offers.ui.service.RawMaterialService;
import com.cmo.offers.ui.service.TreatmentsService;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class OfferTabController {

    private final OfferTabSheet tab;
    private final OfferEntity offer;
    private final Integer referenceId;
    private final String referenceDoc;
    private final String customerName;
    
    private final Consumer<OfferEntity> onOfferUpdated;

    @SuppressWarnings("unused")
    private final ClientEntity client;

    @SuppressWarnings("unused")
    private final PlantEntity plant;

    private final ObjectProperty<YearMonth> currentPeriod = new SimpleObjectProperty<>();
    
    @SuppressWarnings("unused")
    private final MaterialDAO materialDAO;

    @SuppressWarnings("unused")
    private final MarketPriceDAO marketPriceDAO;

    @SuppressWarnings("unused")
    private final RawMaterialService rawMaterialService;

    private final GeneralInfoRow generalInfoRow;
    private final TreatmentsService treatmentsService;

    private final GeneralInfoController generalInfoController;
    private final ToolingTableController toolingTableController;
    private final ComponentsTableController componentsTableController;
    private final RawMaterialTableController rawMaterialTableController;
    private final OperationsTableController operationsTableController;
    private final TreatmentsTableController treatmentsTableController;
    private final OtherTableController otherTableController;

    private final OfferJsonService offerJsonService = new OfferJsonService();
    private final OfferDAO offerDAO = new OfferDAO();
    private final OfferRefDAO offerRefDAO = new OfferRefDAO();

    private final Stage mainStage;

    private static final double ROW_HEIGHT = 36;
    private static final int MAX_VISIBLE_ROWS = 12;

    public OfferTabController(
            OfferTabSheet tab,
            OfferEntity offer,
            Integer referenceId,
            String referenceDoc,
            String customerName,
            ClientEntity client,
            PlantEntity plant,
            YearMonth period,
            MaterialDAO materialDAO,
            MarketPriceDAO marketPriceDAO,
            RawMaterialService rawMaterialService,
            GeneralInfoRow generalInfoRow,
            Stage mainStage,
            Consumer<OfferEntity> onOfferUpdated
    ) {
        this.tab = Objects.requireNonNull(tab, "tab must not be null");
        this.offer = Objects.requireNonNull(offer, "offer must not be null");
        this.referenceId = referenceId;
        this.referenceDoc = referenceDoc;
        this.customerName = customerName;
        this.client = client;
        this.plant = plant;
        this.currentPeriod.set(
                Objects.requireNonNull(period, "period must not be null"));        
        this.materialDAO = materialDAO;
        this.marketPriceDAO = marketPriceDAO;
        this.rawMaterialService = rawMaterialService;
        this.generalInfoRow = Objects.requireNonNull(generalInfoRow, "generalInfoRow must not be null");
        this.mainStage = Objects.requireNonNull(mainStage, "mainStage must not be null");
        this.onOfferUpdated = onOfferUpdated;

        this.treatmentsService = new TreatmentsService(marketPriceDAO);

        this.generalInfoController = new GeneralInfoController(tab.getGeneralInfoTable(), false);
        this.toolingTableController = new ToolingTableController(tab.getToolingTable());
        this.componentsTableController = new ComponentsTableController(tab.getComponentsTable());

        this.rawMaterialTableController = new RawMaterialTableController(
                tab.getRawMaterialTable(),
                client,
                plant,
                () -> currentPeriod.get(),
                rawMaterialService,
                materialDAO
        );

        this.operationsTableController = new OperationsTableController(
                tab.getOperationsTable(),
                this.generalInfoRow
        );

        this.treatmentsTableController = new TreatmentsTableController(
                tab.getTreatmentsTable(),
                treatmentsService,
                () -> currentPeriod.get()
        );

        this.otherTableController = new OtherTableController(tab.getOtherCostsTable());

        initializeTables();
        loadOfferData();
        loadPersistedReferenceStateIfPresent();
        configureGeneralInfoBindings();
        configureCrossTableBindings();
        configureButtons();
        configureTableHeights();
    }

    private void initializeTables() {
        toolingTableController.seedEmptyRows(1);
        componentsTableController.seedEmptyRows(1);
        rawMaterialTableController.seedEmptyRows(1);

        // The operations controller now owns the default first row logic.
        operationsTableController.seedEmptyRows(1);

        treatmentsTableController.seedEmptyRows(1);
        initializeOtherCostsTable();
    }

    private void loadOfferData() {
        generalInfoController.loadInitialData(
                generalInfoRow,
                offer.getOfferNr(),
                offer.getOfferDate(),
                offer.getRevision(),
                offer.getRequestNr(),
                customerName,
                referenceDoc
        );
    }

    private void configureGeneralInfoBindings() {
        GeneralInfoRow row = generalInfoController.getRow();
        if (row == null) {
            return;
        }

        row.offerDateProperty().addListener((obs, oldDate, newDate) -> {

            YearMonth pricingPeriod = newDate == null
                    ? null
                    : YearMonth.from(newDate);

            currentPeriod.set(pricingPeriod);

            rawMaterialTableController.refreshForPeriodChange();
            treatmentsTableController.refreshForPeriodChange();
        });
    }
    
    private void configureCrossTableBindings() {
        rawMaterialTableController.getData().addListener(
                (ListChangeListener<RawMaterialRow>) change -> {
                    // Reserved for future synchronization logic.
                    // Other Costs quantity/total bindings already react automatically.
                }
        );

        componentsTableController.getData().addListener(
                (ListChangeListener<ComponentsRow>) change -> {
                    // Reserved for future synchronization logic.
                    // MP row total cost should already react automatically
                    // if it is bound to the Components summary row cost property.
                }
        );
    }

    private void configureButtons() {
        configureAddButton(tab.getAddToolingRowButton(), toolingTableController::addEmptyRow);
        configureAddButton(tab.getAddComponentsRowButton(), componentsTableController::addEmptyRow);
        configureAddButton(tab.getAddRawMaterialRowButton(), rawMaterialTableController::addEmptyRow);
        configureAddButton(tab.getAddOperationsRowButton(), operationsTableController::addEmptyRow);
        configureAddButton(tab.getAddTreatmentsRowButton(), treatmentsTableController::addEmptyRow);
        configureAddButton(tab.getAddOtherCostsRowButton(), otherTableController::addEmptyRow);

        configureActionButton(tab.getExportJsonButton(), this::exportCurrentReferenceToJson);
        configureActionButton(tab.getSaveChangesButton(), this::saveToDatabase);
        configureActionButton(tab.getBackButton(), this::goBack);
        configureActionButton(tab.getCloneRevisionButton(), this::cloneRevision);
        configureActionButton(tab.getNextRevisionButton(), this::goToNextRevision);
    }

    private void configureActionButton(Button button, Runnable action) {
        button.setDefaultButton(false);
        button.setCancelButton(false);
        button.setOnAction(e -> action.run());
    }

    public void saveToDatabase() {
        try {
            saveToDatabaseSilently();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Saved");
            alert.setHeaderText(null);
            alert.setContentText(
                    "Changes saved successfully.\n\n" +
                    "Note: Offer header fields (Offer Nr, Date, Revision, Request) " +
                    "are shared across all references and have been updated everywhere."
            );
            alert.showAndWait();

        } catch (Exception ex) {
            ex.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Save error");
            alert.setHeaderText(null);
            alert.setContentText("Unable to save changes: " + ex.getMessage());
            alert.showAndWait();
        }
    }

    public void saveToDatabaseSilently() throws Exception {
        commitPendingEdits();

        GeneralInfoRow gi = generalInfoController.getRow();
        if (gi == null) {
            throw new IllegalStateException("General info row is missing.");
        }

        String offerNr = trimToNull(gi.getOfferNr());
        LocalDate offerDate = gi.getOfferDate();

        String revision = normalizeRevision(gi.getRevision());
        gi.setRevision(revision);

        String requestNr = trimToNull(gi.getRequestNr());
        String doc = trimToNull(gi.getReferenceDoc());

        if (offerDate == null) {
            throw new IllegalArgumentException("Offer date is required.");
        }

        if (doc == null) {
            throw new IllegalArgumentException("Document reference is required.");
        }

        // Persist DB-backed offer fields first
        offer.setOfferNr(offerNr);
        offer.setOfferDate(offerDate);
        offer.setRevision(revision);
        offer.setRequestNr(requestNr);

        offerDAO.saveOrUpdate(offer);

        // Persist / update offer reference row
        OfferRefEntity ref = new OfferRefEntity();
        ref.setId(referenceId != null ? referenceId : 0);
        ref.setOfferId(offer.getId());
        ref.setDoc(doc);

        offerRefDAO.saveOrUpdate(ref);

        // Persist full tab details as JSON on disk
        ReferenceExportModel referenceDto = extractReferenceDocDto();
        referenceDto.setReferenceId(doc);

        OfferBundle bundle = new OfferBundle();
        bundle.setFormatVersion(1);
        bundle.setOffer(buildOfferDtoFromGeneralInfo(gi));
        bundle.setReferences(List.of(referenceDto));

        File file = getReferenceStateFile(ref.getId(), offer.getId(), doc);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        offerJsonService.write(file, bundle);

        // Notify AFTER the offer has been fully updated
        if (onOfferUpdated != null) {
            onOfferUpdated.accept(offer);
        }
    }

    private void goBack() {
        if (mainStage.isIconified()) {
            mainStage.setIconified(false);
        }

        Stage currentStage = (Stage) tab.getScene().getWindow();
        if (currentStage != null) {
            currentStage.toBack();
        }

        Platform.runLater(() -> {
            mainStage.show();
            mainStage.toFront();
            mainStage.requestFocus();
        });
    }

    private void configureAddButton(Button button, Runnable action) {
        button.setDefaultButton(false);
        button.setCancelButton(false);
        button.setOnAction(e -> action.run());
    }

    /**
     * Loads the default rows for the Other Costs table.
     *
     * TRAS, PACK, MACH:
     * - quantity bound to sum of non-summary RawMaterialRow.netWeight
     *
     * MP (Materiali e Componenti):
     * - quantity shown empty
     * - unit cost shown empty
     * - total cost bound to:
     *   RawMaterial summary row costXPiece + Components summary row cost
     */
    private void initializeOtherCostsTable() {
        ObservableList<RawMaterialRow> rawMaterials = rawMaterialTableController.getData();

        if (rawMaterials != null) {
            otherTableController.loadDefaultRows(
                    rawMaterials,
                    rawMaterialTableController.getSummaryRow().costXPieceProperty(),
                    componentsTableController.getSummaryRow().costProperty(),
                    rawMaterialTableController.getSummaryRow().priceProperty(),
                    componentsTableController.getSummaryRow().priceProperty(),
                    treatmentsTableController.getTotalRow().operationPriceProperty(),
                    treatmentsTableController.getTotalRow().silverPriceProperty(),
                    operationsTableController.getTotalMachiningRow().setupPriceProperty(),
                    operationsTableController.getTotalMachiningRow().prodPriceProperty(),
                    operationsTableController.getTotalAdditionalRow().prodPriceProperty()
            );
        } else {
            otherTableController.seedEmptyRows(4);
        }
    }

    private void configureTableHeights() {
        fitTableToRows(tab.getGeneralInfoTable());
        fitTableToRows(tab.getToolingTable());
        fitTableToRows(tab.getComponentsTable());
        fitTableToRows(tab.getRawMaterialTable());
        fitTableToRows(tab.getOperationsTable());
        fitTableToRows(tab.getTreatmentsTable());
        fitTableToRows(tab.getOtherCostsTable());
    }

    private <T> void fitTableToRows(TableView<T> table) {
        table.setFixedCellSize(ROW_HEIGHT);

        Platform.runLater(() -> {
            Node header = table.lookup("TableHeaderRow");
            double headerHeight = header == null ? 28 : header.prefHeight(-1);

            Runnable updateHeight = () -> {
                int visibleRows = Math.min(table.getItems().size(), MAX_VISIBLE_ROWS);
                double height = visibleRows * table.getFixedCellSize() + headerHeight + 2;

                table.setPrefHeight(height);
                table.setMinHeight(height);
                table.setMaxHeight(height);
                table.requestLayout();
            };

            updateHeight.run();

            table.getItems().addListener((ListChangeListener<T>) c ->
                    Platform.runLater(updateHeight)
            );
        });
    }
    
    public int getOfferId() {
        return offer.getId();
    }

    public OtherTableController getOtherCostsTableController() {
        return otherTableController;
    }

    public RawMaterialTableController getRawMaterialTableController() {
        return rawMaterialTableController;
    }

    public ComponentsTableController getComponentsTableController() {
        return componentsTableController;
    }

    public OperationsTableController getOperationsTableController() {
        return operationsTableController;
    }

    public TreatmentsTableController getTreatmentsTableController() {
        return treatmentsTableController;
    }

    public ToolingTableController getToolingTableController() {
        return toolingTableController;
    }

    public GeneralInfoController getGeneralInfoController() {
        return generalInfoController;
    }

    // Reference details data extractor
    public ReferenceExportModel extractReferenceDocDto() {
        ReferenceExportModel dto = new ReferenceExportModel();

        GeneralInfoRow gi = generalInfoController.getRow();
        String currentDoc = gi != null ? trimToNull(gi.getReferenceDoc()) : null;

        dto.setReferenceId(currentDoc != null ? currentDoc : referenceDoc);
        dto.setGeneralInfo(generalInfoController.extractDto());

        dto.setTooling(realRowsOnly(toolingTableController.extractDtos()));
        dto.setComponents(realRowsOnly(componentsTableController.extractDtos()));
        dto.setRawMaterials(realRowsOnly(rawMaterialTableController.extractDtos()));
        dto.setOperations(realRowsOnly(operationsTableController.extractDtos()));
        dto.setTreatments(realRowsOnly(treatmentsTableController.extractDtos()));

        // Keep Other Costs as-is because this table has intentional default rows:
        // Other / TRAS / PACK / MACH / MP etc.
        dto.setOtherCosts(otherTableController.extractDtos());

        return dto;
    }

    public void loadReferenceDocDto(ReferenceExportModel dto) {
        if (dto == null) {
            return;
        }

        generalInfoController.loadDto(dto.getGeneralInfo());

        GeneralInfoRow gi = generalInfoController.getRow();
        if (gi != null && (gi.getCustomerName() == null || gi.getCustomerName().isBlank())) {
            gi.setCustomerName(customerName);
        }

        toolingTableController.loadDtos(dto.getTooling());
        componentsTableController.loadDtos(dto.getComponents());
        rawMaterialTableController.loadDtos(dto.getRawMaterials());
        operationsTableController.loadDtos(dto.getOperations());
        treatmentsTableController.loadDtos(dto.getTreatments());
        otherTableController.loadDtos(dto.getOtherCosts());
    }

    private OfferBundle buildOfferBundle() {
        OfferBundle bundle = new OfferBundle();
        bundle.setFormatVersion(1);

        GeneralInfoRow gi = generalInfoController.getRow();

        OfferExportModel offerDto = gi != null
                ? buildOfferDtoFromGeneralInfo(gi)
                : new OfferExportModel();

        if (gi == null) {
            offerDto.setOfferNr(offer.getOfferNr());
            offerDto.setRequest(offer.getRequestNr());
            offerDto.setOfferDate(offer.getOfferDate());
            offerDto.setRevision(offer.getRevision());
            offerDto.setCustomer(customerName);
        }

        bundle.setOffer(offerDto);
        bundle.setReferences(List.of(extractReferenceDocDto()));

        return bundle;
    }

    private void exportCurrentReferenceToJson() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Reference");
        chooser.getExtensionFilters().setAll(
                new FileChooser.ExtensionFilter("JSON files", "*.json")
        );

        GeneralInfoRow gi = generalInfoController.getRow();
        String currentRef = gi != null ? trimToNull(gi.getReferenceDoc()) : null;
        String currentOfferNr = gi != null ? trimToNull(gi.getOfferNr()) : null;

        String safeRef = currentRef == null ? "reference" : currentRef;
        String safeOffer = currentOfferNr == null ? "offer" : currentOfferNr;

        chooser.setInitialFileName("offer_" + safeOffer + "_" + safeRef + ".json");

        File file = chooser.showSaveDialog((Stage) tab.getScene().getWindow());
        if (file == null) {
            return;
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                commitPendingEdits();
                OfferBundle bundle = buildOfferBundle();
                offerJsonService.write(file, bundle);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Export completed");
            alert.setHeaderText(null);
            alert.setContentText("JSON exported to:\n" + file.getAbsolutePath());
            alert.showAndWait();
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Export failed");
            alert.setHeaderText(null);
            alert.setContentText(ex == null ? "Unknown error" : ex.getMessage());
            alert.showAndWait();
        });

        new Thread(task, "export-reference-json").start();
    }

    private void loadPersistedReferenceStateIfPresent() {
        try {
            String doc = referenceDoc;

            if (doc == null || doc.isBlank()) {
                GeneralInfoRow gi = generalInfoController.getRow();
                if (gi != null) {
                    doc = trimToNull(gi.getReferenceDoc());
                }
            }

            File file = getReferenceStateFile(referenceId, offer.getId(), doc);
            if (!file.exists()) {
                return;
            }

            OfferBundle bundle = offerJsonService.read(file);
            if (bundle == null || bundle.getReferences() == null || bundle.getReferences().isEmpty()) {
                return;
            }

            loadReferenceDocDto(bundle.getReferences().get(0));

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private OfferExportModel buildOfferDtoFromGeneralInfo(GeneralInfoRow gi) {
        OfferExportModel dto = new OfferExportModel();
        dto.setOfferNr(gi.getOfferNr());
        dto.setOfferDate(gi.getOfferDate());
        dto.setRevision(gi.getRevision());
        dto.setRequest(gi.getRequestNr());
        dto.setCustomer(gi.getCustomerName());
        return dto;
    }

    private File getReferenceStateFile(Integer refId, int offerId, String doc) {
        File dir = new File(System.getProperty("user.home"), ".cmooffers/reference-state");

        String safeDoc = sanitizeFileName(
                doc == null || doc.isBlank() ? "reference" : doc
        );

        if (refId != null && refId > 0) {
            return new File(dir, "ref_" + refId + "_" + safeDoc + ".json");
        }

        return new File(dir, "offer_" + offerId + "_" + safeDoc + ".json");
    }
    
    private <T extends ExportRow> List<T> realRowsOnly(List<T> rows) {
        if (rows == null) {
            return List.of();
        }

        return rows.stream()
                .filter(Objects::nonNull)
                .filter(row -> !row.isEmptyRow())
                .toList();
    }

    private String sanitizeFileName(String value) {
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void commitPendingEdits() {
        tab.getGeneralInfoTable().edit(-1, null);
        tab.getToolingTable().edit(-1, null);
        tab.getComponentsTable().edit(-1, null);
        tab.getRawMaterialTable().edit(-1, null);
        tab.getOperationsTable().edit(-1, null);
        tab.getTreatmentsTable().edit(-1, null);
        tab.getOtherCostsTable().edit(-1, null);

        if (tab.getScene() != null && tab.getScene().getRoot() != null) {
            tab.getScene().getRoot().requestFocus();
        }
    }
    
    public void applyOfferHeaderUpdate(OfferEntity updatedOffer, String updatedCustomerName) {
        if (updatedOffer == null) {
            return;
        }

        offer.setOfferNr(updatedOffer.getOfferNr());
        offer.setOfferDate(updatedOffer.getOfferDate());
        offer.setRevision(updatedOffer.getRevision());
        offer.setRequestNr(updatedOffer.getRequestNr());

        GeneralInfoRow gi = generalInfoController.getRow();
        if (gi != null) {
            gi.setOfferNr(updatedOffer.getOfferNr());
            gi.setOfferDate(updatedOffer.getOfferDate());
            gi.setRevision(updatedOffer.getRevision());
            gi.setRequestNr(updatedOffer.getRequestNr());
            if (updatedCustomerName != null && !updatedCustomerName.isBlank()) {
                gi.setCustomerName(updatedCustomerName);
            }
        }

        generalInfoController.applyOfferHeaderUpdate(updatedOffer, updatedCustomerName);
    }
    
    private void goToNextRevision() {
        commitPendingEdits();

        GeneralInfoRow gi = generalInfoController.getRow();
        if (gi == null) {
            return;
        }

        String current = trimToNull(gi.getRevision());
        gi.setRevision(nextRevisionValue(current));
    }

    private String nextRevisionValue(String revision) {
        if (revision == null || revision.isBlank()) {
            return "0";
        }

        try {
            int value = Integer.parseInt(revision.trim());
            return String.valueOf(value + 1);
        } catch (NumberFormatException ex) {
            // fallback if user typed something weird
            return "0";
        }
    }
    
    private void cloneRevision() {
        commitPendingEdits();

        GeneralInfoRow gi = generalInfoController.getRow();
        if (gi == null) {
            return;
        }

        String oldRevision = trimToNull(gi.getRevision());
        String newRevision = nextRevisionValue(oldRevision);

        gi.setRevision(newRevision);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Revision cloned");
        alert.setHeaderText(null);
        alert.setContentText(
                "Revision " + (oldRevision == null ? "0" : oldRevision) +
                " cloned to " + newRevision +
                ".\nPress Save Changes to persist it."
        );
        alert.showAndWait();
    }
    
    private String normalizeRevision(String revision) {
        if (revision == null || revision.isBlank()) {
            return "0";
        }

        try {
            int value = Integer.parseInt(revision.trim());
            return String.valueOf(Math.max(0, value));
        } catch (NumberFormatException ex) {
            return "0";
        }
    }
}