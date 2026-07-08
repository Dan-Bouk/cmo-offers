package com.cmo.offers.utils;

import java.util.function.Supplier;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;

public final class OfferTabUtils {

    public static <R> void seedEmptyRows(
            TableView<R> table,
            int count,
            Supplier<R> supplier
    ) {
        ObservableList<R> rows = FXCollections.observableArrayList();
        for (int i = 0; i < count; i++) {
            rows.add(supplier.get());
        }
        table.setItems(rows);
    }

}
