package com.cmo.offers.ui.form;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;

import com.cmo.offers.entity.ClientEntity;

public class OfferForm extends BorderPane {

	/**
	 * Offer form UI (View only).
	 * - Customer is a ComboBox<ClientEntity> (loads from DB in the controller)
	 * - Fields match your TableView columns: Customer, Your request, Offer Nr, Offer Date, Rev.
	 *
	 * Controller responsibilities:
	 *  - populate clientCombo using ClientDAO.findAll()
	 *  - on Save: build OfferEntity + call OfferService.saveOrUpdate(...)
	 */

	    // --- Controls are public/final so controller can access easily ---
	    public final ComboBox<ClientEntity> clientCombo = new ComboBox<>();

	    public final TextField requestNrField = new TextField();   // "Your request"
	    public final TextField offerNrField = new TextField();     // "Offer. Nr."
	    public final DatePicker offerDatePicker = new DatePicker(LocalDate.now()); // "Offer Date"
	    public final TextField revisionField = new TextField();    // "Rev."
	    public final TextField referenceDocField = new TextField();   // "Document reference"

	    public final Button saveBtn = new Button("Save");
	    public final Button cancelBtn = new Button("Cancel");

	    // Optional: message label for inline validation feedback
	    public final Label messageLabel = new Label();

	    public OfferForm() {
	        setPadding(new Insets(16));
	        setCenter(buildForm());
	        setBottom(buildButtons());
	    }

	    private Pane buildForm() {
	        GridPane g = new GridPane();
	        g.setHgap(10);
	        g.setVgap(10);

	        ColumnConstraints c0 = new ColumnConstraints();
	        c0.setPrefWidth(130);
	        ColumnConstraints c1 = new ColumnConstraints();
	        c1.setHgrow(Priority.ALWAYS);
	        g.getColumnConstraints().addAll(c0, c1);

	        // Configure controls
	        clientCombo.setPromptText("Select customer");
	        clientCombo.setMaxWidth(Double.MAX_VALUE);

	        requestNrField.setPromptText("");
	        offerNrField.setPromptText("");
	        revisionField.setPromptText("");

	        offerDatePicker.setMaxWidth(Double.MAX_VALUE);

	        messageLabel.setStyle("-fx-text-fill: #b00020;"); // simple red text
	        messageLabel.setWrapText(true);

	        int r = 0;

	        g.add(new Label("Customer:"), 0, r);
	        g.add(clientCombo, 1, r++);

	        g.add(new Label("Your request:"), 0, r);
	        g.add(requestNrField, 1, r++);

	        g.add(new Label("Offer. Nr.:"), 0, r);
	        g.add(offerNrField, 1, r++);

	        g.add(new Label("Offer Date:"), 0, r);
	        g.add(offerDatePicker, 1, r++);

	        g.add(new Label("Rev.:"), 0, r);
	        g.add(revisionField, 1, r++);
	        
	        g.add(new Label("Document reference:"), 0, r);
	        g.add(referenceDocField, 1, r++);

	        // Optional inline message row
	        g.add(messageLabel, 0, r, 2, 1);

	        // Nice spacing around the grid
	        VBox wrapper = new VBox(10, g);
	        wrapper.setPadding(new Insets(8, 8, 0, 8));
	        return wrapper;
	    }

	    private Pane buildButtons() {
	        // Make Save the default button (Enter)
	        saveBtn.setDefaultButton(true);
	        // Make Cancel respond to Escape
	        cancelBtn.setCancelButton(true);

	        HBox box = new HBox(10, saveBtn, cancelBtn);
	        box.setAlignment(Pos.CENTER_RIGHT);
	        box.setPadding(new Insets(16, 8, 8, 8));
	        return box;
	    }
	

}
