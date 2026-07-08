package com.cmo.offers.model;

import com.cmo.offers.model.row.ComponentsRow;
import com.cmo.offers.model.row.GeneralInfoRow;
import com.cmo.offers.model.row.OperationsRow;
import com.cmo.offers.model.row.OtherCostsRow;
import com.cmo.offers.model.row.RawMaterialRow;
import com.cmo.offers.model.row.ToolingRow;
import com.cmo.offers.model.row.TreatmentsRow;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class OfferTabSheet extends BorderPane {

    private static final String SECTION_TITLE_STYLE =
            "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #12384d;";

    private final TableView<GeneralInfoRow> generalInfoTable = new TableView<>();
    private final TableView<ToolingRow> toolingTable = new TableView<>();
    private final TableView<ComponentsRow> componentsTable = new TableView<>();
    private final TableView<RawMaterialRow> rawMaterialTable = new TableView<>();
    private final TableView<OperationsRow> operationsTable = new TableView<>();
    private final TableView<TreatmentsRow> treatmentsTable = new TableView<>();
    private final TableView<OtherCostsRow> otherCostsTable = new TableView<>();

    private final Button addToolingRowButton = new Button("Add row");
    private final Button addComponentsRowButton = new Button("Add row");
    private final Button addRawMaterialRowButton = new Button("Add row");
    private final Button addOperationsRowButton = new Button("Add row");
    private final Button addTreatmentsRowButton = new Button("Add row");
    private final Button addOtherCostsRowButton = new Button("Add row");

    private final Button saveChangesButton = new Button("Save Changes");
    private final Button backButton = new Button("Back");
    private final Button exportJsonButton = new Button("Export to File");
    private final Button cloneRevisionButton = new Button("Clone Revision");
    private final Button nextRevisionButton = new Button("Next Revision");

    public OfferTabSheet() {
        initializeRoot();
        configureButtons();
        configureTables();

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        HBox topBar = new HBox(
                10,
                backButton,
                cloneRevisionButton,
                nextRevisionButton,
                topSpacer,
                exportJsonButton,
                saveChangesButton
        );
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 0, 12, 0));
        topBar.getStyleClass().add("module-toolbar");

        hideRevisionButtons();
        hideExportButton();

        VBox content = new VBox(
                14,
                section("General Info", generalInfoTable),
                section("Tooling", toolingTable, addToolingRowButton),
                section("Components", componentsTable, addComponentsRowButton),
                section("Raw Material", rawMaterialTable, addRawMaterialRowButton),
                section("Machining and Additional Operations", operationsTable, addOperationsRowButton),
                section("Treatments", treatmentsTable, addTreatmentsRowButton),
                section("Other", otherCostsTable, addOtherCostsRowButton)
        );
        content.setPadding(new Insets(4, 2, 4, 2));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        setTop(topBar);
        setCenter(scrollPane);
    }

    private void initializeRoot() {
        setPadding(new Insets(16));
        setPrefSize(1100, 750);
        getStyleClass().addAll("content-card", "offer-tab-sheet");
    }

    private void configureButtons() {
        styleSubtleButton(backButton);
        styleSubtleButton(exportJsonButton);
        stylePrimaryButton(saveChangesButton);

        styleSubtleButton(addToolingRowButton);
        styleSubtleButton(addComponentsRowButton);
        styleSubtleButton(addRawMaterialRowButton);
        styleSubtleButton(addOperationsRowButton);
        styleSubtleButton(addTreatmentsRowButton);
        styleSubtleButton(addOtherCostsRowButton);

        styleSubtleButton(cloneRevisionButton);
        styleSubtleButton(nextRevisionButton);
    }

    private void configureTables() {
        configureTable(generalInfoTable);
        configureTable(toolingTable);
        configureTable(componentsTable);
        configureTable(rawMaterialTable);
        configureTable(operationsTable);
        configureTable(treatmentsTable);
        configureTable(otherCostsTable);
    }

    private void configureTable(TableView<?> table) {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setEditable(true);
        table.setMinHeight(160);
    }

    private void stylePrimaryButton(Button button) {
        button.getStyleClass().add("primary-button");
        button.setFocusTraversable(false);
    }

    private void styleSubtleButton(Button button) {
        button.getStyleClass().add("subtle-button");
        button.setFocusTraversable(false);
    }

    private void hideRevisionButtons() {
        cloneRevisionButton.setManaged(false);
        cloneRevisionButton.setVisible(false);

        nextRevisionButton.setManaged(false);
        nextRevisionButton.setVisible(false);
    }

    private void hideExportButton() {
        exportJsonButton.setManaged(false);
        exportJsonButton.setVisible(false);
    }

    private VBox section(String title, TableView<?> table) {
        Label label = new Label(title);
        label.setStyle(SECTION_TITLE_STYLE);

        VBox box = new VBox(8, label, table);
        box.setPadding(new Insets(10));
        box.getStyleClass().add("content-card");

        return box;
    }

    private VBox section(String title, TableView<?> table, Button addRowButton) {
        Label label = new Label(title);
        label.setStyle(SECTION_TITLE_STYLE);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(8, label, spacer, addRowButton);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(8, header, table);
        box.setPadding(new Insets(10));
        box.getStyleClass().add("content-card");

        return box;
    }

    public TableView<GeneralInfoRow> getGeneralInfoTable() {
        return generalInfoTable;
    }

    public TableView<ToolingRow> getToolingTable() {
        return toolingTable;
    }

    public TableView<ComponentsRow> getComponentsTable() {
        return componentsTable;
    }

    public TableView<RawMaterialRow> getRawMaterialTable() {
        return rawMaterialTable;
    }

    public TableView<OperationsRow> getOperationsTable() {
        return operationsTable;
    }

    public TableView<TreatmentsRow> getTreatmentsTable() {
        return treatmentsTable;
    }

    public TableView<OtherCostsRow> getOtherCostsTable() {
        return otherCostsTable;
    }

    public Button getAddToolingRowButton() {
        return addToolingRowButton;
    }

    public Button getAddComponentsRowButton() {
        return addComponentsRowButton;
    }

    public Button getAddRawMaterialRowButton() {
        return addRawMaterialRowButton;
    }

    public Button getAddOperationsRowButton() {
        return addOperationsRowButton;
    }

    public Button getAddTreatmentsRowButton() {
        return addTreatmentsRowButton;
    }

    public Button getAddOtherCostsRowButton() {
        return addOtherCostsRowButton;
    }

    public Button getSaveChangesButton() {
        return saveChangesButton;
    }

    public Button getExportJsonButton() {
        return exportJsonButton;
    }

    public Button getBackButton() {
        return backButton;
    }

    public Button getCloneRevisionButton() {
        return cloneRevisionButton;
    }

    public Button getNextRevisionButton() {
        return nextRevisionButton;
    }
}
