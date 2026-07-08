package com.cmo.offers.ui.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.cmo.offers.model.row.GeneralInfoRow;
import com.cmo.offers.model.row.OperationsRow;

public class OperationsService {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    public static final BigDecimal DEFAULT_FIRST_ROW_SETUP_COST = new BigDecimal("1.71");
    public static final BigDecimal DEFAULT_FIRST_ROW_SETUP_PRICE = new BigDecimal("1.71");

    public OperationsRow createDefaultFirstRow(GeneralInfoRow generalInfoRow) {
        OperationsRow row = new OperationsRow();

        row.setDefaultDesignRow(true);
        row.setFase(1);
        row.setOperationName("Progettazione");
        row.setType(OperationsRow.Type.MACH);
        row.setIntExt(OperationsRow.IntExt.I);
        row.setCenter("PRG");
        row.setCost(new BigDecimal("48.0"));
        row.setSetupMinutes(new BigDecimal("30"));
        row.setProductionSeconds(new BigDecimal("0.000"));
        row.setMarkup(new BigDecimal("1.00"));

        recalculateRow(row, generalInfoRow);
        applyDefaultFirstRowOverrides(row);

        return row;
    }

    public OperationsRow createEmptyRow(int phase, GeneralInfoRow generalInfoRow) {
        OperationsRow row = new OperationsRow();

        row.setDefaultDesignRow(false);
        row.setFase(phase);
        row.setSetupMinutes(BigDecimal.ZERO);
        row.setProductionSeconds(BigDecimal.ZERO);
        row.setCost(BigDecimal.ZERO);
        row.setMarkup(BigDecimal.ZERO);

        recalculateRow(row, generalInfoRow);
        return row;
    }

    public void applyDefaultFirstRowOverrides(OperationsRow row) {
        if (row == null || row.isSummary() || !row.isDefaultDesignRow()) {
            return;
        }

        row.setSetupCost(DEFAULT_FIRST_ROW_SETUP_COST);
        row.setSetupPrice(DEFAULT_FIRST_ROW_SETUP_PRICE);
    }

    /**
     * Returns the first available phase in:
     * 10, 20, 30, 40, 60, 70, 80, ...
     * skipping 50.
     *
     * Phase 1 is reserved for the default first row.
     */
    public int nextAutoPhase(Collection<OperationsRow> rows) {
        Set<Integer> usedPhases = rows.stream()
                .filter(Objects::nonNull)
                .filter(row -> !row.isSummary())
                .map(OperationsRow::getFase)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        int phase = 10;
        while (phase == 50 || usedPhases.contains(phase)) {
            phase += 10;
            if (phase == 50) {
                phase += 10;
            }
        }

        return phase;
    }

    public void recalculateRow(OperationsRow row, GeneralInfoRow generalInfo) {
        if (row == null || row.isSummary()) {
            return;
        }

        row.setSetupCost(calculateSetupCost(row, generalInfo));
        row.setProdCost(calculateProdCost(row, generalInfo));
        row.setSetupPrice(calculateSetupPrice(row, generalInfo));
        row.setProdPrice(calculateProdPrice(row, generalInfo));

        applyDefaultFirstRowOverrides(row);
    }

    public BigDecimal calculateSetupCost(OperationsRow row, GeneralInfoRow generalInfo) {
        if (row == null || row.isSummary()) {
            return zero();
        }

        if (row.getType() != OperationsRow.Type.MACH) {
            return zero();
        }

        BigDecimal cost = nz(row.getCost());
        BigDecimal setupMinutes = nz(row.getSetupMinutes());
        BigDecimal batchQty = batchQuantity(generalInfo);

        // (€/hour * minutes / 60) / quantityPerBatch
        BigDecimal result = cost.multiply(setupMinutes)
                .divide(BigDecimal.valueOf(60), 6, ROUNDING)
                .divide(batchQty, SCALE, ROUNDING);

        if (row.isDefaultDesignRow()) {
            return DEFAULT_FIRST_ROW_SETUP_COST.setScale(SCALE, ROUNDING);
        }

        return result;
    }

    public BigDecimal calculateProdCost(OperationsRow row, GeneralInfoRow generalInfo) {
        if (row == null || row.isSummary()) {
            return zero();
        }

        BigDecimal cost = nz(row.getCost());
        BigDecimal productionSeconds = nz(row.getProductionSeconds());

        if (row.getIntExt() == null) {
            return zero();
        }

        if (row.getIntExt() == OperationsRow.IntExt.I) {
            // internal -> hourly cost converted using seconds
            return cost.multiply(productionSeconds)
                    .divide(BigDecimal.valueOf(3600), SCALE, ROUNDING);
        } else {
            // external -> direct piece cost
            return cost.setScale(SCALE, ROUNDING);
        }
    }

    public BigDecimal calculateSetupPrice(OperationsRow row, GeneralInfoRow generalInfo) {
        if (row == null || row.isSummary()) {
            return zero();
        }

        if (row.isDefaultDesignRow()) {
            return DEFAULT_FIRST_ROW_SETUP_PRICE.setScale(SCALE, ROUNDING);
        }

        BigDecimal setupCost = calculateSetupCost(row, generalInfo);
        BigDecimal markup = nz(row.getMarkup());

        return setupCost.multiply(markup).setScale(SCALE, ROUNDING);
    }

    public BigDecimal calculateProdPrice(OperationsRow row, GeneralInfoRow generalInfo) {
        if (row == null || row.isSummary()) {
            return zero();
        }

        BigDecimal prodCost = calculateProdCost(row, generalInfo);
        BigDecimal setupCost = calculateSetupCost(row, generalInfo);
        BigDecimal markup = nz(row.getMarkup());

        if (row.getType() == null) {
            return zero();
        }

        if (row.getType() == OperationsRow.Type.ADD) {
            return (prodCost.add(setupCost)).multiply(markup).setScale(SCALE, ROUNDING);
        } else {
            return prodCost.multiply(markup).setScale(SCALE, ROUNDING);
        }
    }

    public void recalculateAllRows(List<OperationsRow> rows, GeneralInfoRow generalInfo) {
        if (rows == null) {
            return;
        }

        for (OperationsRow row : rows) {
            if (row != null && !row.isSummary()) {
                recalculateRow(row, generalInfo);
            }
        }
    }

    public void recalculateSummaryRows(
            List<OperationsRow> sourceRows,
            OperationsRow machiningSummary,
            OperationsRow additionalSummary
    ) {
        if (machiningSummary != null) {
            machiningSummary.setSetupCost(sumSetupCostByType(sourceRows, OperationsRow.Type.MACH));
            machiningSummary.setProdCost(sumProdCostByType(sourceRows, OperationsRow.Type.MACH));
            machiningSummary.setSetupPrice(sumSetupPriceByType(sourceRows, OperationsRow.Type.MACH));
            machiningSummary.setProdPrice(sumProdPriceByType(sourceRows, OperationsRow.Type.MACH));
        }

        if (additionalSummary != null) {
            additionalSummary.setSetupCost(sumSetupCostByType(sourceRows, OperationsRow.Type.ADD));
            additionalSummary.setProdCost(sumProdCostByType(sourceRows, OperationsRow.Type.ADD));
            additionalSummary.setSetupPrice(sumSetupPriceByType(sourceRows, OperationsRow.Type.ADD));
            additionalSummary.setProdPrice(sumProdPriceByType(sourceRows, OperationsRow.Type.ADD));
        }
    }

    public void recalculateAll(
            List<OperationsRow> rows,
            GeneralInfoRow generalInfo,
            OperationsRow machiningSummary,
            OperationsRow additionalSummary
    ) {
        recalculateAllRows(rows, generalInfo);
        recalculateSummaryRows(rows, machiningSummary, additionalSummary);
    }

    public BigDecimal sumSetupPriceByType(List<OperationsRow> rows, OperationsRow.Type type) {
        return rows.stream()
                .filter(Objects::nonNull)
                .filter(r -> !r.isSummary())
                .filter(r -> r.getType() == type)
                .map(r -> nz(r.getSetupPrice()))
                .reduce(zero(), BigDecimal::add)
                .setScale(SCALE, ROUNDING);
    }

    public BigDecimal sumProdPriceByType(List<OperationsRow> rows, OperationsRow.Type type) {
        return rows.stream()
                .filter(Objects::nonNull)
                .filter(r -> !r.isSummary())
                .filter(r -> r.getType() == type)
                .map(r -> nz(r.getProdPrice()))
                .reduce(zero(), BigDecimal::add)
                .setScale(SCALE, ROUNDING);
    }

    public BigDecimal sumSetupCostByType(List<OperationsRow> rows, OperationsRow.Type type) {
        return rows.stream()
                .filter(Objects::nonNull)
                .filter(r -> !r.isSummary())
                .filter(r -> r.getType() == type)
                .map(r -> nz(r.getSetupCost()))
                .reduce(zero(), BigDecimal::add)
                .setScale(SCALE, ROUNDING);
    }

    public BigDecimal sumProdCostByType(List<OperationsRow> rows, OperationsRow.Type type) {
        return rows.stream()
                .filter(Objects::nonNull)
                .filter(r -> !r.isSummary())
                .filter(r -> r.getType() == type)
                .map(r -> nz(r.getProdCost()))
                .reduce(zero(), BigDecimal::add)
                .setScale(SCALE, ROUNDING);
    }

    private BigDecimal batchQuantity(GeneralInfoRow generalInfo) {
        if (generalInfo == null
                || generalInfo.getQuantityPerBatch() == null
                || generalInfo.getQuantityPerBatch() <= 0) {
            return BigDecimal.ONE;
        }
        return BigDecimal.valueOf(generalInfo.getQuantityPerBatch());
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
    }
}