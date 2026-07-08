package com.cmo.offers.ui.service;

import com.cmo.offers.dao.ClientMarkupDAO;
import com.cmo.offers.dao.MarketPriceDAO;
import com.cmo.offers.dao.MaterialDAO;
import com.cmo.offers.entity.ClientEntity;
import com.cmo.offers.entity.ClientMarkupEntity;
import com.cmo.offers.entity.MarketPriceEntity;
import com.cmo.offers.entity.MaterialEntity;
import com.cmo.offers.entity.PlantEntity;
import com.cmo.offers.model.row.MPRow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MPService {

    private final MaterialDAO materialDAO;
    private final MarketPriceDAO marketPriceDAO;
    private final ClientMarkupDAO clientMarkupDAO;

    public MPService(MaterialDAO materialDAO,
                          MarketPriceDAO marketPriceDAO,
                          ClientMarkupDAO clientMarkupDAO) {
        this.materialDAO = materialDAO;
        this.marketPriceDAO = marketPriceDAO;
        this.clientMarkupDAO = clientMarkupDAO;
    }
    
    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** Pure calculation (no DB). */
    public BigDecimal calculateFinalEurPerKg(MarketPriceEntity market, ClientMarkupEntity markup) {
        BigDecimal lme = safe(market.getLme());
        BigDecimal fx = safe(market.getFx());

        BigDecimal prime = markup == null
        		? BigDecimal.ZERO
        		: safe(markup.getPrime());
        
        BigDecimal financialPercent = markup == null
                ? BigDecimal.ZERO
                : safe(markup.getFinancialPercent());

        BigDecimal managementPercent = markup == null
                ? BigDecimal.ZERO
                : safe(markup.getManagementPercent());

        if (fx.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal baseUsdPerTon = lme.add(prime);

        BigDecimal finFactor = BigDecimal.ONE.add(
                financialPercent.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP)
        );

        BigDecimal mgmtFactor = BigDecimal.ONE.add(
                managementPercent.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP)
        );

        BigDecimal usdPerTon = baseUsdPerTon
                .multiply(finFactor)
                .multiply(mgmtFactor);

        BigDecimal eurPerTon = usdPerTon.divide(fx, 8, RoundingMode.HALF_UP);

        return eurPerTon.divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP);
    }

    /**
     * Load the pricing grid for UI: one row per material with market data + markup + calculated final price.
     * This does NOT store final price in DB.
     */
    public List<MPRow> loadPricingGrid(ClientEntity client, PlantEntity plant, YearMonth month)
            throws SQLException {

        List<MaterialEntity> materials = materialDAO.findAll();
        List<MPRow> rows = new ArrayList<>();

        for (MaterialEntity material : materials) {

            Optional<MarketPriceEntity> marketOpt =
                    marketPriceDAO.findByMaterialAndPeriod(material.getId(), month);

            // Reverted behavior:
            // if there is no market data, do not show the row
            if (marketOpt.isEmpty()) {
                continue;
            }

            Optional<ClientMarkupEntity> markupOpt =
                    clientMarkupDAO.findByCriteria(
                            client.getId(),
                            plant.getId(),
                            material.getId(),
                            month
                    );

            BigDecimal lme = BigDecimal.ZERO;
            BigDecimal prime = BigDecimal.ZERO;
            BigDecimal fx = BigDecimal.ZERO;
            BigDecimal fin = BigDecimal.ZERO;
            BigDecimal mgmt = BigDecimal.ZERO;
            BigDecimal finalEurKg = BigDecimal.ZERO;

            MarketPriceEntity market = marketOpt.get();

            lme = safe(market.getLme());
            fx = safe(market.getFx());

            if (markupOpt.isPresent()) {
                ClientMarkupEntity markup = markupOpt.get();

                prime = safe(markup.getPrime());
                fin = safe(markup.getFinancialPercent());
                mgmt = safe(markup.getManagementPercent());
            }

            String code = material.getCode() == null ? "" : material.getCode().trim();
            boolean isSilver =
                    "AG".equalsIgnoreCase(code) ||
                    "SILVER".equalsIgnoreCase(code);

            if (isSilver) {
                finalEurKg = safe(market.getEurPerKg());
            } else {
                ClientMarkupEntity markupForCalculation = new ClientMarkupEntity(
                        client,
                        plant,
                        material,
                        month,
                        prime,
                        fin,
                        mgmt
                );

                finalEurKg = calculateFinalEurPerKg(market, markupForCalculation);
            }

            rows.add(new MPRow(
                    material,
                    month,
                    lme,
                    prime,
                    fx,
                    fin,
                    mgmt,
                    finalEurKg
            ));
        }

        return rows;
    }
    
    public void saveOrUpdateMarketPriceFromRow(MPRow row) throws SQLException {
        MarketPriceEntity marketPrice = new MarketPriceEntity(
                row.getMaterial(),
                row.getPeriod(),
                row.getLme(),
                row.getFx(),
                row.getFinalEurPerKg()
        );

        marketPriceDAO.saveOrUpdate(marketPrice);
    }
    
    public void saveOrUpdateMarkupFromRow(
            ClientEntity client,
            PlantEntity plant,
            MPRow row) throws SQLException {

        ClientMarkupEntity markup = new ClientMarkupEntity(
                client,
                plant,
                row.getMaterial(),
                row.getPeriod(),
                row.getPrime(),
                row.getFinancialPercent(),
                row.getManagementPercent()
        );

        clientMarkupDAO.saveOrUpdate(markup);
    }
    
    public void saveOrUpdatePricingEntry(ClientEntity client,
            PlantEntity plant,
            MaterialEntity material,
            YearMonth month,
            BigDecimal lme,
            BigDecimal prime,
            BigDecimal fx,
            BigDecimal financialPercent,
            BigDecimal managementPercent) throws SQLException {
    	
    	if (client == null || client.getId() == 0) {
    		throw new IllegalArgumentException("Client is required.");
    		}
    	if (plant == null || plant.getId() == 0) {
    		throw new IllegalArgumentException("Plant is required.");
    		}
    	if (material == null || material.getId() == 0) {
    		throw new IllegalArgumentException("Material is required.");
    		}
    	if (month == null) {
    		throw new IllegalArgumentException("Month is required.");
    		}
    	    	
    	MarketPriceEntity marketPrice = new MarketPriceEntity();
    	marketPrice.setMaterial(material);
    	marketPrice.setPeriod(month);
    	marketPrice.setLme(lme == null ? BigDecimal.ZERO : lme);
    	marketPrice.setFx(fx == null ? BigDecimal.ZERO : fx);
    	marketPrice.setEurPerKg(null);
    	marketPriceDAO.saveOrUpdate(marketPrice);

    	ClientMarkupEntity markup = new ClientMarkupEntity(
    			client,
    			plant,
    			material,
    			month,
    			safe(prime),
    			safe(financialPercent),
    			safe(managementPercent));
    	
    	clientMarkupDAO.saveOrUpdate(markup);
    }
    
    public void saveOrUpdateMetalMarketPrice(MaterialEntity material,
            YearMonth month,
            BigDecimal lme,
            BigDecimal fx) throws SQLException {
    	if (material == null || material.getId() == 0) {
    		throw new IllegalArgumentException("Material is required.");
    	}
    	if (month == null) {
    		throw new IllegalArgumentException("Month is required.");
    	}

    	MarketPriceEntity mp = new MarketPriceEntity();
    	mp.setMaterial(material);
    	mp.setPeriod(month);
    	mp.setLme(safe(lme));
    	mp.setFx(safe(fx));
    	mp.setEurPerKg(null);

    	marketPriceDAO.saveOrUpdate(mp);
    }

    public void saveOrUpdateMarkup(ClientEntity client,
    		PlantEntity plant,
    		MaterialEntity material,
    		YearMonth month,
    		BigDecimal prime,
    		BigDecimal financialPercent,
    		BigDecimal managementPercent) throws SQLException {
    	if (client == null || client.getId() == 0) {
    		throw new IllegalArgumentException("Client is required.");
    	}
    	if (plant == null || plant.getId() == 0) {
    		throw new IllegalArgumentException("Plant is required.");
    	}
    	if (material == null || material.getId() == 0) {
    		throw new IllegalArgumentException("Material is required.");
    	}
    	if (month == null) {
    		throw new IllegalArgumentException("Month is required.");
    	}

    	ClientMarkupEntity markup = new ClientMarkupEntity(
    			client,
    			plant,
    			material,
    			month,
    			safe(prime),
    			safe(financialPercent),
    			safe(managementPercent)
    			);

    	clientMarkupDAO.saveOrUpdate(markup);
    }

    public void saveOrUpdateSilverBasePrice(MaterialEntity silverMaterial,
           YearMonth month,
           BigDecimal eurPerKg) throws SQLException {
    	if (silverMaterial == null || silverMaterial.getId() == 0) {
    		throw new IllegalArgumentException("Silver material is required.");
    	}
    	if (month == null) {
    		throw new IllegalArgumentException("Month is required.");
    	}

    	MarketPriceEntity mp = new MarketPriceEntity();
    	mp.setMaterial(silverMaterial);
    	mp.setPeriod(month);
    	mp.setLme(BigDecimal.ZERO);
    	mp.setFx(BigDecimal.ZERO);
    	mp.setEurPerKg(eurPerKg == null ? BigDecimal.ZERO : eurPerKg);

    	marketPriceDAO.saveOrUpdate(mp);
    }
}
