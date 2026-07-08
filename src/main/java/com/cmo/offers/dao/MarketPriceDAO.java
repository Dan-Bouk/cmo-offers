package com.cmo.offers.dao;

import com.cmo.offers.utils.DatabaseConnection;
import com.cmo.offers.entity.MaterialEntity;
import com.cmo.offers.entity.MarketPriceEntity;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MarketPriceDAO {

    private MarketPriceEntity mapRow(ResultSet rs) throws SQLException {
        MarketPriceEntity mp = new MarketPriceEntity();

        MaterialEntity m = new MaterialEntity();
        m.setId(rs.getInt("material_id"));
        mp.setMaterial(m);

        mp.setPeriod(YearMonth.from(rs.getDate("period").toLocalDate()));
        mp.setLme(rs.getBigDecimal("lme"));
        mp.setFx(rs.getBigDecimal("fx"));
        mp.setEurPerKg(rs.getBigDecimal("eur_per_kg"));

        return mp;
    }

    public Optional<MarketPriceEntity> findByMaterialAndPeriod(int materialId, YearMonth period) throws SQLException {
    	
    	LocalDate dbDate = period.atDay(1);
    	
        String sql = """
                SELECT material_id, period, lme, fx, eur_per_kg
                FROM market_price
                WHERE material_id = ? AND period = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, materialId);
            ps.setDate(2, Date.valueOf(dbDate));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }
    
    public Optional<MarketPriceEntity> findSilverPriceByPeriod(YearMonth period) throws SQLException {
    	
    	LocalDate dbDate = period.atDay(1);
    	
        String sql = """
                SELECT mp.material_id, mp.period, mp.lme, mp.fx, mp.eur_per_kg
                FROM market_price mp
                JOIN material m ON m.id = mp.material_id
                WHERE m.code = ? AND mp.period = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "AG");
            ps.setDate(2, Date.valueOf(dbDate));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }

        return Optional.empty();
    }

    public List<MarketPriceEntity> findByPeriod(YearMonth period) throws SQLException {
    	
    	LocalDate dbDate = period.atDay(1);
    	
        String sql = """
                SELECT material_id, period, lme, fx, eur_per_kg
                FROM market_price
                WHERE period = ?
                ORDER BY material_id
                """;

        List<MarketPriceEntity> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(dbDate));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public void save(MarketPriceEntity mp) throws SQLException {
        String sql = """
                INSERT INTO market_price (material_id, period, lme, fx, eur_per_kg)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, mp.getMaterial().getId());
            ps.setDate(2, Date.valueOf(mp.getPeriod().atDay(1)));
            ps.setBigDecimal(3, mp.getLme());
            ps.setBigDecimal(4, mp.getFx());
            ps.setBigDecimal(5, mp.getEurPerKg());

            ps.executeUpdate();
        }
    }

    public void saveOrUpdate(MarketPriceEntity mp) throws SQLException {
        String sql = """
            INSERT INTO market_price
                (material_id, period, lme, fx, eur_per_kg)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (material_id, period)
            DO UPDATE SET
                lme = EXCLUDED.lme,
                fx = EXCLUDED.fx,
                eur_per_kg = EXCLUDED.eur_per_kg
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            bindMarketPrice(ps, mp);
            ps.executeUpdate();
        }
    }

    public boolean delete(int materialId, YearMonth period) throws SQLException {
    	
    	LocalDate dbDate = period.atDay(1);
    	
        String sql = "DELETE FROM market_price WHERE material_id = ? AND period = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, materialId);
            ps.setDate(2, Date.valueOf(dbDate));
            return ps.executeUpdate() > 0;
        }
    }
    
    private void bindMarketPrice(PreparedStatement ps, MarketPriceEntity mp) throws SQLException {
        ps.setInt(1, mp.getMaterial().getId());
        ps.setDate(2, Date.valueOf(mp.getPeriod().atDay(1)));
        ps.setBigDecimal(3, mp.getLme());
        ps.setBigDecimal(4, mp.getFx());
        ps.setBigDecimal(5, mp.getEurPerKg());
    }
}
