package com.cmo.offers.ui.service; // adjust

import com.cmo.offers.dao.ClientMarkupDAO;
import com.cmo.offers.dao.MarketPriceDAO;
import com.cmo.offers.entity.ClientEntity;
import com.cmo.offers.entity.ClientMarkupEntity;
import com.cmo.offers.entity.MarketPriceEntity;
import com.cmo.offers.entity.MaterialEntity;
import com.cmo.offers.entity.PlantEntity;
import com.cmo.offers.model.row.RawMaterialRow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.Objects;
import java.util.Optional;

public class RawMaterialService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE_THOUSAND = BigDecimal.valueOf(1000);
    private static final int SCALE_RESULT = 4;
    private static final int SCALE_DIVISION = 8;

    private final MarketPriceDAO marketPriceDAO;
    private final ClientMarkupDAO clientMarkupDAO;
    private final MPService mpService;

    public RawMaterialService(MarketPriceDAO marketPriceDAO,
                              ClientMarkupDAO clientMarkupDAO,
                              MPService mpService) {
        this.marketPriceDAO = Objects.requireNonNull(marketPriceDAO, "marketPriceDAO must not be null");
        this.clientMarkupDAO = Objects.requireNonNull(clientMarkupDAO, "clientMarkupDAO must not be null");
        this.mpService = Objects.requireNonNull(mpService, "mpService must not be null");
    }

    public RawMaterialRow createRow(ClientEntity client,
                                    PlantEntity plant,
                                    YearMonth period,
                                    MaterialEntity material,
                                    String description,
                                    BigDecimal grossWeight,
                                    BigDecimal netWeight,
                                    BigDecimal scrapValuePercentage,
                                    BigDecimal transformationPrice,
                                    BigDecimal markup) throws SQLException {

        require(client, "Client is null.");
        require(plant, "Plant is null.");
        require(period, "Period is null.");
        require(material, "Material is null.");

        YearMonth previous = period.minusMonths(1);

        PricingData pricing = loadPricingData(client, plant, previous, material);

        BigDecimal scrapWeight = calculateScrapWeight(grossWeight, netWeight);
        BigDecimal scrapRecovery = calculateScrapRecovery(
                pricing.lmeUsdPerTon(),
                pricing.fxUsdToEur(),
                nvl(scrapWeight),
                nvl(scrapValuePercentage)
        );

        BigDecimal costXPiece = calculateCostPerPiece(
                nvl(transformationPrice),
                pricing.finalEurPerKg(),
                nvl(grossWeight),
                scrapRecovery
        );

        BigDecimal price = calculatePrice(costXPiece, nvl(markup));

        return new RawMaterialRow(
                material,
                description,
                grossWeight,
                netWeight,
                scrapWeight,
                scrapValuePercentage,
                transformationPrice,
                costXPiece,
                markup,
                pricing.lmeUsdPerTon(),
                pricing.fxUsdToEur(),
                pricing.finalEurPerKg(),
                price
        );
    }

    public void recalculateRow(RawMaterialRow row,
                               ClientEntity client,
                               PlantEntity plant,
                               YearMonth period) throws SQLException {

    	if (row == null) {
            return;
        }

        require(client, "Client is null.");
        require(plant, "Plant is null.");
        require(period, "Period is null.");

        BigDecimal gross = row.getGrossWeight();
        BigDecimal net = row.getNetWeight();
        BigDecimal scrapPct = row.getScrapValuePercentage();
        BigDecimal trans = row.getTransformationPrice();
        BigDecimal rowMarkup = row.getMarkup();

        // Always computable, even without material
        BigDecimal scrapWeight = calculateScrapWeight(gross, net);
        row.setScrapWeight(scrapWeight);

        // Material-dependent pricing part
        if (row.getMaterial() == null) {
            row.setCostXPiece(null);
            row.setLmeUsdPerTon(null);
            row.setFxUsdToEur(null);
            row.setFinalEurPerKg(null);
            row.setPrice(null);
            return;
        }

        YearMonth previous = period.minusMonths(1);
        
        PricingData pricing = loadPricingData(client, plant, previous, row.getMaterial());

        BigDecimal scrapRecovery = calculateScrapRecovery(
                pricing.lmeUsdPerTon(),
                pricing.fxUsdToEur(),
                nvl(scrapWeight),
                nvl(scrapPct)
        );

        BigDecimal costXPiece = calculateCostPerPiece(
                nvl(trans),
                pricing.finalEurPerKg(),
                nvl(gross),
                scrapRecovery
        );

        BigDecimal price = calculatePrice(costXPiece, nvl(rowMarkup));

        row.setCostXPiece(costXPiece);
        row.setLmeUsdPerTon(pricing.lmeUsdPerTon());
        row.setFxUsdToEur(pricing.fxUsdToEur());
        row.setFinalEurPerKg(pricing.finalEurPerKg());
        row.setPrice(price);
    }

    private PricingData loadPricingData(ClientEntity client,
                                        PlantEntity plant,
                                        YearMonth previous,
                                        MaterialEntity material) throws SQLException {
    	
        MarketPriceEntity market = marketPriceDAO
                .findByMaterialAndPeriod(material.getId(), previous)
                .orElseThrow(() -> new IllegalStateException(
                        "Missing market_price for " + material.getCode() + " @ " + previous
                ));

        Optional<ClientMarkupEntity> markupOpt = clientMarkupDAO.findByCriteria(
                client.getId(),
                plant.getId(),
                material.getId(),
                previous
        );

        BigDecimal prime = markupOpt
                .map(ClientMarkupEntity::getPrime )
                .map(RawMaterialService::nvl)
                .orElse(ZERO);
        
        BigDecimal financialPercent = markupOpt
                .map(ClientMarkupEntity::getFinancialPercent)
                .map(RawMaterialService::nvl)
                .orElse(ZERO);

        BigDecimal managementPercent = markupOpt
                .map(ClientMarkupEntity::getManagementPercent)
                .map(RawMaterialService::nvl)
                .orElse(ZERO);

        boolean hasDirectEurPerKg = market.getEurPerKg() != null;

        BigDecimal finalEurPerKg = hasDirectEurPerKg
                ? market.getEurPerKg()
                : mpService.calculateFinalEurPerKg(
                        market,
                        new ClientMarkupEntity(
                                client,
                                plant,
                                material,
                                previous,
                                prime,
                                financialPercent,
                                managementPercent
                        )
                );

        BigDecimal lmeUsdPerTon = hasDirectEurPerKg ? null : market.getLme();
        BigDecimal fxUsdToEur = hasDirectEurPerKg ? null : market.getFx();

        return new PricingData(lmeUsdPerTon, fxUsdToEur, finalEurPerKg);
    }

    private BigDecimal calculateScrapWeight(BigDecimal grossWeight, BigDecimal netWeight) {
        if (grossWeight == null || netWeight == null) {
            return null;
        }

        BigDecimal scrap = grossWeight.subtract(netWeight);

        if (scrap.compareTo(ZERO) < 0) {
            return ZERO.setScale(SCALE_RESULT, RoundingMode.HALF_UP);
        }

        return scrap.setScale(SCALE_RESULT, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateScrapRecovery(BigDecimal lmeUsdPerTon,
                                              BigDecimal fxUsdToEur,
                                              BigDecimal scrapWeight,
                                              BigDecimal scrapValuePercentage) {

        if (lmeUsdPerTon == null || fxUsdToEur == null || fxUsdToEur.compareTo(ZERO) == 0) {
            return ZERO;
        }

        BigDecimal eurPerKgScrap = lmeUsdPerTon
                .divide(ONE_THOUSAND, SCALE_DIVISION, RoundingMode.HALF_UP)
                .divide(fxUsdToEur, SCALE_DIVISION, RoundingMode.HALF_UP);

        return eurPerKgScrap
                .multiply(scrapWeight)
                .multiply(scrapValuePercentage);
    }

    private BigDecimal calculateCostPerPiece(BigDecimal transformationPrice,
                                             BigDecimal finalEurPerKg,
                                             BigDecimal grossWeight,
                                             BigDecimal scrapRecovery) {
        return transformationPrice
                .add(finalEurPerKg)
                .multiply(grossWeight)
                .subtract(scrapRecovery)
                .setScale(SCALE_RESULT, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePrice(BigDecimal costXPiece, BigDecimal markup) {
        return costXPiece
                .multiply(markup)
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

    private record PricingData(
            BigDecimal lmeUsdPerTon,
            BigDecimal fxUsdToEur,
            BigDecimal finalEurPerKg
    ) {}
}