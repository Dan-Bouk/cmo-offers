package com.cmo.offers.ui.form;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class AddReferenceForm extends VBox {

    private final TextField referenceNameField = new TextField();
    private final Button saveButton = new Button("Save");
    private final Button cancelButton = new Button("Cancel");

    public AddReferenceForm() {
        setSpacing(12);
        setPadding(new Insets(15));

        referenceNameField.setPromptText("Reference name");
        referenceNameField.setPrefWidth(300);

        HBox buttons = new HBox(10, saveButton, cancelButton);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(
                new Label("Reference doc:"),
                referenceNameField,
                buttons
        );
    }

    public TextField getReferenceNameField() {
        return referenceNameField;
    }

    public Button getSaveButton() {
        return saveButton;
    }

    public Button getCancelButton() {
        return cancelButton;
    }
}
