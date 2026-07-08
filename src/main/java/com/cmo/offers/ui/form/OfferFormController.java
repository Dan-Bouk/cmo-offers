package com.cmo.offers.ui.form;

import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

import com.cmo.offers.dao.ClientDAO;
import com.cmo.offers.dao.OfferRefDAO;
import com.cmo.offers.entity.ClientEntity;
import com.cmo.offers.entity.OfferEntity;
import com.cmo.offers.entity.OfferRefEntity;
import com.cmo.offers.ui.service.OfferService;

/**
 * Controller for OfferForm.
 * Responsibilities:
 *  - load clients into the ComboBox (async)
 *  - handle Save/Cancel
 *  - validate UI input
 *  - build OfferEntity and call OfferService
 */

public class OfferFormController {
	
	private final OfferForm form;
    private final OfferService offerService;
    private final ClientDAO clientDAO;
    private final OfferRefDAO offerRefDAO;

    // If editing an existing offer
    private OfferEntity editingOffer;

    public OfferFormController(
            OfferForm form,
            OfferService offerService,
            ClientDAO clientDAO,
            OfferRefDAO offerRefDAO
    ) {
        this.form = form;
        this.offerService = offerService;
        this.clientDAO = clientDAO;
        this.offerRefDAO = offerRefDAO;

        wireActions();
        loadClientsAsync();
    }

    public void setEditingOffer(OfferEntity offer) {
        this.editingOffer = offer;
        populateFormFromOffer(offer);
    }

    private void wireActions() {
        form.saveBtn.setOnAction(e -> onSave());
        form.cancelBtn.setOnAction(e -> closeWindow());
    }

    private void loadClientsAsync() {
        Task<List<ClientEntity>> task = new Task<>() {
            @Override
            protected List<ClientEntity> call() throws SQLException {
                return clientDAO.findAll();
            }
        };

        task.setOnSucceeded(e -> {
            form.clientCombo.getItems().setAll(task.getValue());

            if (editingOffer != null) {
                selectClientById(editingOffer.getClientId());
            } else if (!form.clientCombo.getItems().isEmpty()) {
                form.clientCombo.getSelectionModel().selectFirst();
            }
        });

        task.setOnFailed(e -> showError("Failed to load clients", task.getException()));

        new Thread(task, "load-clients").start();
    }

    private void onSave() {
        try {
            form.messageLabel.setText("");

            OfferEntity offer = readOfferFromForm();

            if (editingOffer != null) {
                offer.setId(editingOffer.getId());
            }

            // Save or update offer first
            offerService.saveOrUpdate(offer);

            // Save reference code if provided
            String referenceDoc = trimToNull(form.referenceDocField.getText());
            if (referenceDoc != null) {
                OfferRefEntity ref = new OfferRefEntity();
                ref.setOfferId(offer.getId());
                ref.setDoc(referenceDoc);
                offerRefDAO.save(ref);
            }

            showInfo("Offer saved.");
            closeWindow();

        } catch (Exception ex) {
            form.messageLabel.setText(ex.getMessage());
            showError("Cannot save offer", ex);
        }
    }

    private OfferEntity readOfferFromForm() {
        ClientEntity selectedClient = form.clientCombo.getValue();
        if (selectedClient == null) {
            throw new IllegalArgumentException("Customer is required.");
        }

        String offerNr = trimToNull(form.offerNrField.getText());
        if (offerNr == null) {
            throw new IllegalArgumentException("Offer. Nr. is required.");
        }

        if (form.offerDatePicker.getValue() == null) {
            throw new IllegalArgumentException("Offer Date is required.");
        }

        OfferEntity o = new OfferEntity();
        o.setClientId(selectedClient.getId());
        o.setRequestNr(trimToNull(form.requestNrField.getText()));
        o.setOfferNr(offerNr);
        o.setOfferDate(form.offerDatePicker.getValue());
        o.setRevision(trimToNull(form.revisionField.getText()));

        return o;
    }

    private void populateFormFromOffer(OfferEntity o) {
        if (o == null) return;

        form.requestNrField.setText(o.getRequestNr());
        form.offerNrField.setText(o.getOfferNr());
        form.offerDatePicker.setValue(o.getOfferDate());
        form.revisionField.setText(o.getRevision());
    }

    private void selectClientById(int clientId) {
        for (ClientEntity c : form.clientCombo.getItems()) {
            if (c.getId() == clientId) {
                form.clientCombo.getSelectionModel().select(c);
                return;
            }
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) form.getScene().getWindow();
        stage.close();
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private void showError(String title, Throwable ex) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(ex == null ? "Unknown error" : ex.getMessage());
        a.showAndWait();
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

}
