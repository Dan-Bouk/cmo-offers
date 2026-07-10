package com.cmo.offers.service;

import java.io.File;
import java.sql.SQLException;
import java.util.function.Supplier;

import com.cmo.offers.dao.ClientDAO;
import com.cmo.offers.dao.OfferRefDAO;
import com.cmo.offers.entity.ClientEntity;
import com.cmo.offers.entity.OfferEntity;
import com.cmo.offers.entity.OfferRefEntity;
import com.cmo.offers.export.service.OfferExportService;
import com.cmo.offers.load.service.OfferImportService;
import com.cmo.offers.model.row.OfferTreeRow;
import com.cmo.offers.ui.window.ReferenceWindowManager;
import com.cmo.offers.ui.service.OfferService;

import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class OfferActionController {
	
	private final OfferService offerService;
	private final OfferFileStateService fileStateService;
	private final OfferExportService exportService;
	private final OfferImportService importService;
	private final ReferenceWindowManager windowManager;
	private final ClientDAO clientDAO;
	private final OfferRefDAO offerRefDAO;
	private final Runnable refreshAction;
	private final Runnable refreshTableAction;
	private final Supplier<Stage> ownerStageSupplier;
	
	public OfferActionController(
	        OfferService offerService,
	        OfferFileStateService fileStateService,
	        OfferExportService exportService,
	        OfferImportService importService,
	        ReferenceWindowManager windowManager,
	        ClientDAO clientDAO,
	        OfferRefDAO offerRefDAO,
	        Runnable refreshAction,
	        Runnable refreshTableAction,
	        Supplier<Stage> ownerStageSupplier
	) {
	    this.offerService = offerService;
	    this.fileStateService = fileStateService;
	    this.exportService = exportService;
	    this.importService = importService;
	    this.windowManager = windowManager;
	    this.clientDAO = clientDAO;
	    this.offerRefDAO = offerRefDAO;
	    this.refreshAction = refreshAction;
	    this.refreshTableAction = refreshTableAction;
	    this.ownerStageSupplier = ownerStageSupplier;
	}
	
	public void duplicateOffer(OfferTreeRow row) {
	    if (!confirmDuplicate(row)) {
	        return;
	    }

	    runDuplicateTask(row);
	}
	
	private boolean confirmDuplicate(OfferTreeRow row) {
	    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
	    confirm.setTitle("Duplicate offer");
	    confirm.setHeaderText("Duplicate offer " + row.getOfferNr() + "?");
	    confirm.setContentText("A new offer will be created with the same header and references.");

	    styleAlertButtons(confirm);

	    return confirm.showAndWait()
	            .filter(result -> result == ButtonType.OK)
	            .isPresent();
	}
	
	private void runDuplicateTask(OfferTreeRow row) {
	    Task<OfferEntity> task = new Task<>() {
	        @Override
	        protected OfferEntity call() throws Exception {
	            OfferEntity duplicatedOffer = offerService.duplicateOffer(row.getOfferId());
	            fileStateService.copyReferenceStateFiles(row.getOfferId(), duplicatedOffer.getId());
	            return duplicatedOffer;
	        }
	    };

	    task.setOnSucceeded(e -> {
	        OfferEntity duplicatedOffer = task.getValue();
	        refreshAction.run();
	        showOfferDuplicated(duplicatedOffer);
	    });

	    task.setOnFailed(e -> showError("Failed to duplicate offer", task.getException()));

	    new Thread(task, "duplicate-offer-" + row.getOfferId()).start();
	}
	
	private void showOfferDuplicated(OfferEntity duplicatedOffer) {
	    Alert alert = new Alert(Alert.AlertType.INFORMATION);
	    alert.setTitle("Offer duplicated");
	    alert.setHeaderText("Offer duplicated successfully");
	    alert.setContentText("Created offer: " + duplicatedOffer.getOfferNr());
	    alert.showAndWait();
	}
	
//	public void duplicateOffer(OfferTreeRow row) {
//        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
//        confirm.setTitle("Duplicate offer");
//        confirm.setHeaderText("Duplicate offer " + row.getOfferNr() + "?");
//        confirm.setContentText("A new offer will be created with the same header and references.");
//
//        styleAlertButtons(confirm);
//
//        confirm.showAndWait().ifPresent(result -> {
//            if (result != ButtonType.OK) {
//                return;
//            }
//
//            Task<OfferEntity> task = new Task<>() {
//                @Override
//                protected OfferEntity call() throws Exception {
//                    OfferEntity duplicatedOffer = offerService.duplicateOffer(row.getOfferId());
//                    fileStateService.copyReferenceStateFiles(row.getOfferId(), duplicatedOffer.getId());
//                    return duplicatedOffer;
//                }
//            };
//
//            task.setOnSucceeded(e -> {
//                OfferEntity duplicatedOffer = task.getValue();
//                refreshAction.run();
//
//                Alert alert = new Alert(Alert.AlertType.INFORMATION);
//                alert.setTitle("Offer duplicated");
//                alert.setHeaderText("Offer duplicated successfully");
//                alert.setContentText("Created offer: " + duplicatedOffer.getOfferNr());
//                alert.showAndWait();
//            });
//
//            task.setOnFailed(e -> showError("Failed to duplicate offer", task.getException()));
//            new Thread(task, "duplicate-offer-" + row.getOfferId()).start();
//        });
//    }
	
	public void deleteOffer(OfferTreeRow row) {
	    if (!confirmDeleted(row)) {
	        return;
	    }

	    runDeleteTask(row);
	}
	
	private boolean confirmDeleted(OfferTreeRow row) {
		Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete offer");
        confirm.setHeaderText("Delete offer " + row.getOfferNr() + "?");
        confirm.setContentText("This will remove the offer, all its references, and the saved reference files.");

        styleAlertButtons(confirm);

	    return confirm.showAndWait()
	            .filter(result -> result == ButtonType.OK)
	            .isPresent();
	}
	
	private void runDeleteTask(OfferTreeRow row) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                windowManager.closeOfferWindows(row.getOfferId());
                fileStateService.deleteReferenceStateFiles(row.getOfferId());
                offerService.deleteOffer(row.getOfferId());
                return null;
            }
        };

        task.setOnSucceeded(e -> refreshAction.run());
        task.setOnFailed(e -> showError("Failed to delete offer", task.getException()));
        
        new Thread(task, "delete-offer-" + row.getOfferId()).start();
	}
	
    public void styleAlertButtons(Alert alert) {
        DialogPane pane = alert.getDialogPane();
        if (pane.lookupButton(ButtonType.OK) instanceof Button okBtn) {
            okBtn.getStyleClass().add("primary-button");
        }
        if (pane.lookupButton(ButtonType.CANCEL) instanceof Button cancelBtn) {
            cancelBtn.getStyleClass().add("subtle-button");
        }
    } 
    
    public void exportOfferRowToJson(OfferTreeRow row) {
        File selectedFile = chooseExportFile(
                "Export Offer to JSON",
                "JSON files",
                "*.json",
                "offer_" + row.getOfferNr() + ".json"
        );

        if (selectedFile == null) {
            return;
        }

        runJsonExport(row, selectedFile);
    }
	
    public void exportOfferRowToExcel(OfferTreeRow row) {
    	File selectedFile = chooseExportFile(
                "Export Offer to Excel",
                "Excel files",
                "*.xlsx",
                "offer_" + row.getOfferNr() + ".xlsx"
        );
    	
    	if (selectedFile == null) {
            return;
        }

        runExcelExport(row, selectedFile);
    }
    
    private File chooseExportFile(
            String title,
            String description,
            String extension,
            String initialFileName
    ) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().setAll(
                new FileChooser.ExtensionFilter(description, extension)
        );
        chooser.setInitialFileName(initialFileName);

        return chooser.showSaveDialog(ownerStageSupplier.get());
    }
    
    private void runJsonExport(OfferTreeRow row, File selectedFile) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                exportService.exportOfferToJson(
                        row.getOfferId(),
                        selectedFile,
                        ownerStageSupplier.get()
                );
                return null;
            }
        };

        task.setOnSucceeded(e ->
                showExportCompleted(selectedFile)
        );

        task.setOnFailed(e ->
                showError(
                        "Failed to export offer to JSON",
                        task.getException()
                )
        );

        new Thread(
                task,
                "export-offer-json-" + row.getOfferId()
        ).start();
    }
    
    private void runExcelExport(OfferTreeRow row, File selectedFile) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                exportService.exportOfferToExcel(
                        row.getOfferId(),
                        selectedFile,
                        ownerStageSupplier.get()
                );
                return null;
            }
        };

        task.setOnSucceeded(e ->
                showExportCompleted(selectedFile)
        );

        task.setOnFailed(e ->
                showError(
                        "Failed to export offer to Excel",
                        task.getException()
                )
        );

        new Thread(
                task,
                "export-offer-excel-" + row.getOfferId()
        ).start();
    }

    
    private void showExportCompleted(File selectedFile) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export completed");
        alert.setHeaderText("Offer exported successfully");
        alert.setContentText(
                "Saved to:\n" + selectedFile.getAbsolutePath()
        );
        alert.showAndWait();
    }
    
    public void importOfferJson() {
        File selectedFile = chooseImportFile();

        if (selectedFile == null) {
            return;
        }

        runJsonImport(selectedFile);
    }
    
    private File chooseImportFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import Offer");
        chooser.getExtensionFilters().setAll(
                new FileChooser.ExtensionFilter(
                        "JSON files",
                        "*.json"
                )
        );

        return chooser.showOpenDialog(ownerStageSupplier.get());
    }
    
    private void runJsonImport(File selectedFile) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                importService.importOffer(
                        selectedFile,
                        ownerStageSupplier.get()
                );
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            showImportCompleted(selectedFile);
            refreshAction.run();
        });

        task.setOnFailed(e ->
                showError(
                        "Failed to import offer",
                        task.getException()
                )
        );

        new Thread(task, "import-offer-json").start();
    }
    
    private void showImportCompleted(File selectedFile) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Import completed");
        alert.setHeaderText("Offer imported successfully");
        alert.setContentText(
                "Imported from:\n" + selectedFile.getAbsolutePath()
        );
        alert.showAndWait();
    }
    
    public void saveReference(int offerId, String referenceName) {
        String normalizedName = trimToNull(referenceName);

        if (offerId <= 0) {
            showError(
                    "Failed to add reference",
                    new IllegalArgumentException("Offer id is missing.")
            );
            return;
        }

        if (normalizedName == null) {
            showError(
                    "Failed to add reference",
                    new IllegalArgumentException("Reference name is required.")
            );
            return;
        }

        runSaveReferenceTask(offerId, normalizedName);
    }
    
    private void runSaveReferenceTask(int offerId, String referenceName) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                saveReferenceInternal(offerId, referenceName);
                return null;
            }
        };

        task.setOnSucceeded(event -> refreshAction.run());

        task.setOnFailed(event ->
                showError(
                        "Failed to add reference",
                        task.getException()
                )
        );

        Thread thread = new Thread(
                task,
                "save-reference-" + offerId
        );
        thread.setDaemon(true);
        thread.start();
    }
    
    private void saveReferenceInternal(
            int offerId,
            String referenceName
    ) throws SQLException {

        OfferRefEntity reference = new OfferRefEntity();
        reference.setOfferId(offerId);
        reference.setDoc(referenceName);

        offerRefDAO.save(reference);
    }
    
    public void saveOfferRowChanges(OfferTreeRow row) {
        if (row == null || row.isReferenceRow()) {
            return;
        }

        runSaveOfferTask(row);
    }
    
    private void runSaveOfferTask(OfferTreeRow row) {
        Task<OfferEntity> task = new Task<>() {
            @Override
            protected OfferEntity call() throws Exception {
                return saveOfferFromRow(row);
            }
        };

        task.setOnSucceeded(event ->
                handleSaveSuccess(task.getValue(), row)
        );

        task.setOnFailed(event ->
                showError(
                        "Failed to save offer changes",
                        task.getException()
                )
        );

        Thread thread = new Thread(
                task,
                "save-offer-row-" + row.getOfferId()
        );
        thread.setDaemon(true);
        thread.start();
    }
    
    private OfferEntity saveOfferFromRow(OfferTreeRow row) throws SQLException {
        OfferEntity offer = updateOfferFromRow(row);
        ClientEntity client = resolveClient(row);

        offer.setClientId(client.getId());

        offerService.saveOrUpdate(offer);

        return offerService.getOfferById(offer.getId());
    }
    
    private void handleSaveSuccess(
            OfferEntity updatedOffer,
            OfferTreeRow row
    ) {
        String customerName =
                loadCustomerName(updatedOffer, row);

        updateRow(
                row,
                updatedOffer,
                customerName
        );

        persistReferenceState(
                updatedOffer,
                customerName
        );

        refreshTableAction.run();

        windowManager.notifyOfferUpdated(
                updatedOffer,
                customerName,
                null
        );

        showSaveCompleted();
    }
        
    private String loadCustomerName(OfferEntity updatedOffer, OfferTreeRow row) {
    	
    	try {
            return clientDAO.findById(updatedOffer.getClientId())
            		.map(ClientEntity::getName)
            		.orElse(row.getCustomerName());
        } catch (Exception ex) {
            showError("Failed to reload customer", ex);
            return row.getCustomerName();
        }
    }
    
    private void persistReferenceState(OfferEntity updatedOffer, String updatedCustomerName) {    	
        try {
            fileStateService.persistOfferHeaderToAllReferenceStateFiles(
            		updatedOffer, 
            		updatedCustomerName
            );
        } catch (Exception ex) {
            showError("Failed to update saved reference files", ex);
        }
    }
    
    private void showSaveCompleted() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Save completed");
        alert.setHeaderText("Offer updated successfully");
        alert.setContentText("The offer row and all references were updated.");
        alert.showAndWait();
    }
    
    private OfferEntity updateOfferFromRow(OfferTreeRow row) throws SQLException {    	
    	OfferEntity offer = offerService.getOfferById(row.getOfferId());

        offer.setOfferNr(trimToNull(row.getOfferNr()));
        offer.setRevision(trimToNull(row.getRevision()));
        offer.setOfferDate(row.getOfferDate());
        offer.setRequestNr(trimToNull(row.getRequestNr()));
        
        return offer;
    }
    
    private ClientEntity resolveClient(OfferTreeRow row) throws SQLException{
    	
    	String customerName = trimToNull(row.getCustomerName());
    	
        if (customerName == null) {
            throw new IllegalArgumentException("Customer is required.");
        }
        
        return clientDAO.findByName(customerName)
                .orElseThrow(() -> 
                		new IllegalArgumentException(
                				"Customer not found: " + customerName));
    }    
    
    private void updateRow(OfferTreeRow row, OfferEntity offer, String customerName) {
        row.setOfferNr(offer.getOfferNr());
        row.setRevision(offer.getRevision());
        row.setOfferDate(offer.getOfferDate());
        row.setRequestNr(offer.getRequestNr());
        row.setCustomerName(customerName);
    }
    
    // Helpers
    
    private void showError(String title, Throwable ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(ex == null ? "Unknown error" : ex.getMessage());
        alert.showAndWait();
    }
    
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
