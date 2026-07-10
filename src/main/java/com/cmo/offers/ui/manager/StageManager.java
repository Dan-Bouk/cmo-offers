package com.cmo.offers.ui.manager;

import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

public class StageManager {

    private final Map<Integer, Stage> openWindows = new HashMap<>();

    public Stage getOpenWindow(Integer referenceId) {
        Stage stage = openWindows.get(referenceId);

        if (stage == null) {
            return null;
        }

        if (!stage.isShowing()) {
            openWindows.remove(referenceId);
            return null;
        }

        return stage;
    }

    public void registerWindow(Integer referenceId, Stage stage) {
        openWindows.put(referenceId, stage);
    }
    
    public void unregisterWindow(Integer referenceId) {
        openWindows.remove(referenceId);
    }

    public void focusWindow(Stage stage) {
        if (stage == null) {
            return;
        }

        if (stage.isIconified()) {
            stage.setIconified(false);
        }

        stage.show();
        stage.toFront();
        stage.requestFocus();
    }
}