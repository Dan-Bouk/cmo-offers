package com.cmo.offers.ui.service;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import com.cmo.offers.model.row.OtherCostsRow;
import com.cmo.offers.model.row.RawMaterialRow;

public final class OtherCostsService {

    public ObjectBinding<BigDecimal> createRawMaterialNetWeightSumBinding(
            ObservableList<RawMaterialRow> rawMaterials) {

        Objects.requireNonNull(rawMaterials, "rawMaterials must not be null");

        return Bindings.createObjectBinding(
            () -> rawMaterials.stream()
                    .filter(Objects::nonNull)
                    .filter(r -> !r.isSummary())
                    .map(RawMaterialRow::getNetWeight)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add),
            rawMaterials
        );
    }

    public void bindQuantityToRawMaterialSum(
            OtherCostsRow otherCostsRow,
            ObservableList<RawMaterialRow> rawMaterials) {

        Objects.requireNonNull(otherCostsRow, "otherCostsRow must not be null");
        Objects.requireNonNull(rawMaterials, "rawMaterials must not be null");

        otherCostsRow.unbindQuantity();
        otherCostsRow.quantityProperty().bind(
            createRawMaterialNetWeightSumBinding(rawMaterials)
        );
    }

    public void bindMaterialAndComponentsRow(
            OtherCostsRow otherCostsRow,
            ObservableValue<BigDecimal> rawMaterialCostXPieceSummary,
            ObservableValue<BigDecimal> componentsCostSummary,
            ObservableValue<BigDecimal> rawMaterialPriceSummary,
            ObservableValue<BigDecimal> componentsPriceSummary) {

        Objects.requireNonNull(otherCostsRow, "otherCostsRow must not be null");
        Objects.requireNonNull(rawMaterialCostXPieceSummary, "rawMaterialCostXPieceSummary must not be null");
        Objects.requireNonNull(componentsCostSummary, "componentsCostSummary must not be null");
        Objects.requireNonNull(rawMaterialPriceSummary, "rawMaterialPriceSummary must not be null");
        Objects.requireNonNull(componentsPriceSummary, "componentsPriceSummary must not be null");

        otherCostsRow.unbindCalculatedFields();

        otherCostsRow.totalCostProperty().bind(
            Bindings.createObjectBinding(
                () -> safe(rawMaterialCostXPieceSummary.getValue())
                        .add(safe(componentsCostSummary.getValue()))
                        .setScale(2, RoundingMode.HALF_UP),
                rawMaterialCostXPieceSummary,
                componentsCostSummary
            )
        );

        otherCostsRow.priceProperty().bind(
            Bindings.createObjectBinding(
                () -> safe(rawMaterialPriceSummary.getValue())
                        .add(safe(componentsPriceSummary.getValue()))
                        .setScale(2, RoundingMode.HALF_UP),
                rawMaterialPriceSummary,
                componentsPriceSummary
            )
        );
    }

    public void bindMachiningRowPrice(
            OtherCostsRow otherCostsRow,
            ObservableValue<BigDecimal> treatmentsOperationPriceSummary,
            ObservableValue<BigDecimal> treatmentsSilverPriceSummary,
            ObservableValue<BigDecimal> operationsMachiningSetupPriceSummary,
            ObservableValue<BigDecimal> operationsMachiningProductionPriceSummary,
            ObservableValue<BigDecimal> operationsAdditionalPriceSummary) {

        Objects.requireNonNull(otherCostsRow, "otherCostsRow must not be null");
        Objects.requireNonNull(treatmentsOperationPriceSummary, "treatmentsOperationPriceSummary must not be null");
        Objects.requireNonNull(treatmentsSilverPriceSummary, "treatmentsSilverPriceSummary must not be null");
        Objects.requireNonNull(operationsMachiningSetupPriceSummary, "operationsMachiningSetupPriceSummary must not be null");
        Objects.requireNonNull(operationsMachiningProductionPriceSummary, "operationsMachiningProductionPriceSummary must not be null");
        Objects.requireNonNull(operationsAdditionalPriceSummary, "operationsAdditionalPriceSummary must not be null");

        otherCostsRow.unbindCalculatedFields();
        otherCostsRow.setTotalCost(BigDecimal.ZERO);

        otherCostsRow.priceProperty().bind(
            Bindings.createObjectBinding(
                () -> safe(treatmentsOperationPriceSummary.getValue())
                        .add(safe(treatmentsSilverPriceSummary.getValue()))
                        .add(safe(operationsMachiningSetupPriceSummary.getValue()))
                        .add(safe(operationsMachiningProductionPriceSummary.getValue()))
                        .add(safe(operationsAdditionalPriceSummary.getValue()))
                        .setScale(2, RoundingMode.HALF_UP),
                treatmentsOperationPriceSummary,
                treatmentsSilverPriceSummary,
                operationsMachiningSetupPriceSummary,
                operationsMachiningProductionPriceSummary,
                operationsAdditionalPriceSummary
            )
        );
    }

    private static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}