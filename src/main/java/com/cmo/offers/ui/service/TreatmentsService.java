package com.cmo.offers.ui.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.Objects;

import com.cmo.offers.dao.MarketPriceDAO;
import com.cmo.offers.entity.MarketPriceEntity;
import com.cmo.offers.model.row.TreatmentsRow;

public class TreatmentsService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE_THOUSAND = BigDecimal.valueOf(1000);
    private static final int SCALE_RESULT = 4;
    private static final int SCALE_DIVISION = 8;

    private final MarketPriceDAO marketPriceDAO;

    public TreatmentsService(MarketPriceDAO marketPriceDAO) {
        this.marketPriceDAO = Objects.requireNonNull(marketPriceDAO, "marketPriceDAO must not be null");
    }

    public TreatmentsRow createRow(YearMonth period,
                                   Integer fase,
                                   String treatmentName,
                                   String type,
                                   TreatmentsRow.IntExt intExt,
                                   String center,
                                   BigDecimal operationCost,
                                   BigDecimal operationMarkup,
                                   BigDecimal silverQuantityGr,
                                   BigDecimal silverMarkup) throws SQLException {

        require(period, "Period is null.");

        YearMonth previous = period.minusMonths(1);

        BigDecimal silverBasePriceEurPerKg = loadSilverBasePrice(previous);

        BigDecimal operCost = nvl(operationCost);
        BigDecimal operMarkupValue = nvl(operationMarkup);
        BigDecimal qtySilverGr = nvl(silverQuantityGr);
        BigDecimal silverMarkupValue = nvl(silverMarkup);

        BigDecimal operationPrice = calculateOperationPrice(operCost, operMarkupValue);
        BigDecimal silverCost = calculateSilverCost(qtySilverGr, silverBasePriceEurPerKg);
        BigDecimal silverPrice = calculateSilverPrice(silverCost, silverMarkupValue);

        return new TreatmentsRow(
                fase,
                treatmentName,
                type,
                intExt,
                center,
                operCost,
                operMarkupValue,
                qtySilverGr,
                silverMarkupValue,
                operationPrice,
                silverCost,
                silverPrice
        );
    }

    public void recalculateRow(TreatmentsRow row,
                               YearMonth period) throws SQLException {

        if (row == null || row.isSummary()) {
            return;
        }

        require(period, "Period is null.");

        YearMonth previous = period.minusMonths(1);

        BigDecimal silverBasePriceEurPerKg = loadSilverBasePrice(previous);

        BigDecimal operationPrice = calculateOperationPrice(
                nvl(row.getOperationCost()),
                nvl(row.getOperationMarkup())
        );

        BigDecimal silverCost = calculateSilverCost(
                nvl(row.getSilverQuantityGr()),
                silverBasePriceEurPerKg
        );

        BigDecimal silverPrice = calculateSilverPrice(
                silverCost,
                nvl(row.getSilverMarkup())
        );

        row.setOperationPrice(operationPrice);
        row.setSilverCost(silverCost);
        row.setSilverPrice(silverPrice);
    }

    public void recalculateTotalRow(TreatmentsRow totalRow,
                                    Iterable<TreatmentsRow> rows) {

        if (totalRow == null) {
            return;
        }

        BigDecimal totalOperationPrice = ZERO;
        BigDecimal totalSilverCost = ZERO;
        BigDecimal totalSilverPrice = ZERO;

        for (TreatmentsRow row : rows) {
            if (row == null || row.isSummary()) {
                continue;
            }

            totalOperationPrice = totalOperationPrice.add(nvl(row.getOperationPrice()));
            totalSilverCost = totalSilverCost.add(nvl(row.getSilverCost()));
            totalSilverPrice = totalSilverPrice.add(nvl(row.getSilverPrice()));
        }

        totalRow.setOperationPrice(totalOperationPrice.setScale(SCALE_RESULT, RoundingMode.HALF_UP));
        totalRow.setSilverCost(totalSilverCost.setScale(SCALE_RESULT, RoundingMode.HALF_UP));
        totalRow.setSilverPrice(totalSilverPrice.setScale(SCALE_RESULT, RoundingMode.HALF_UP));
    }

    private BigDecimal loadSilverBasePrice(YearMonth previous) throws SQLException {

        MarketPriceEntity market = marketPriceDAO
                .findSilverPriceByPeriod(previous)
                .orElseThrow(() -> new IllegalStateException(
                        "Missing market_price for AG @ " + previous
                ));

        if (market.getEurPerKg() == null) {
            throw new IllegalStateException(
                    "Missing eur_per_kg for material AG @ " + previous
            );
        }

        return market.getEurPerKg();
    }
    private BigDecimal calculateOperationPrice(BigDecimal operationCost,
                                               BigDecimal operationMarkup) {
        return operationCost
                .multiply(operationMarkup)
                .setScale(SCALE_RESULT, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateSilverCost(BigDecimal silverQuantityGr,
                                           BigDecimal silverBasePriceEurPerKg) {

        return silverQuantityGr
                .multiply(silverBasePriceEurPerKg)
                .divide(ONE_THOUSAND, SCALE_DIVISION, RoundingMode.HALF_UP)
                .setScale(SCALE_RESULT, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateSilverPrice(BigDecimal silverCost,
                                            BigDecimal silverMarkup) {
        return silverCost
                .multiply(silverMarkup)
                .setScale(SCALE_RESULT, RoundingMode.HALF_UP);
    }

    private static void require(Object value, String message) {
        if (value == null) {
            throw new IllegalStateException(message);
        }
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? ZERO : value;
    }
}