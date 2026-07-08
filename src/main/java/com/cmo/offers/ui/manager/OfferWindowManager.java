package com.cmo.offers.ui.manager;

import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

public class OfferWindowManager {

    private final Map<Integer, Stage> openWindowsByReferenceId = new HashMap<>();

    public boolean isOpen(Integer referenceId) {
        Stage stage = openWindowsByReferenceId.get(referenceId);
        return stage != null && stage.isShowing();
    }

    public Stage getOpenWindow(Integer referenceId) {
        Stage stage = openWindowsByReferenceId.get(referenceId);
        if (stage != null && stage.isShowing()) {
            return stage;
        }
        openWindowsByReferenceId.remove(referenceId);
        return null;
    }

    public void register(Integer referenceId, Stage stage) {
        openWindowsByReferenceId.put(referenceId, stage);

        stage.setOnHidden(e -> openWindowsByReferenceId.remove(referenceId));
    }

    public void focusWindow(Integer referenceId) {
        Stage stage = getOpenWindow(referenceId);
        if (stage != null) {
            stage.show();
            stage.toFront();
            stage.requestFocus();
        }
    }
}
