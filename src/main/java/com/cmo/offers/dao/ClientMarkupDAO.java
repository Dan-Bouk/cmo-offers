package com.cmo.offers.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.cmo.offers.entity.ClientEntity;
import com.cmo.offers.entity.PlantEntity;
import com.cmo.offers.entity.MaterialEntity;
import com.cmo.offers.entity.ClientMarkupEntity;
import com.cmo.offers.utils.DatabaseConnection;

public class ClientMarkupDAO {

	private ClientMarkupEntity mapRow(ResultSet rs) throws SQLException {

		ClientEntity client = new ClientEntity(rs.getInt("client_id"));
		PlantEntity plant = new PlantEntity(rs.getInt("plant_id"));
		MaterialEntity material = new MaterialEntity(rs.getInt("material_id"));

	    return new ClientMarkupEntity(
	            client,
	            plant,
	            material,
	            YearMonth.from(rs.getDate("period").toLocalDate()),
	            rs.getBigDecimal("prime"),
	            rs.getBigDecimal("financial_percent"),
	            rs.getBigDecimal("management_percent")
	    );
	}

	/*
	// INSERT NEW ROW
	public void save(ClientMarkupEntity cm) throws SQLException {

	    String sql = """
	            INSERT INTO client_markup
	                (material_id, period, financial_percent, management_percent, plant_id, client_id)
	            VALUES (?, ?, ?, ?, ?, ?)
	            """;

	    try (Connection conn = DatabaseConnection.getConnection();
	         PreparedStatement ps = conn.prepareStatement(sql)) {

	        ps.setInt(1, cm.getMaterial().getId());
	        ps.setDate(2, Date.valueOf(cm.getPeriod()));
	        ps.setBigDecimal(3, cm.getFinancialPercent());
	        ps.setBigDecimal(4, cm.getManagementPercent());
	        ps.setInt(5, cm.getPlant().getId());
	        ps.setInt(6, cm.getClient().getId());

	        ps.executeUpdate();
	    }
    } **/
	
	// FIND ALL
    public List<ClientMarkupEntity> findAll() throws SQLException {

        String sql = "SELECT * FROM client_markup";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<ClientMarkupEntity> list = new ArrayList<>();

            while (rs.next()) {
                list.add(mapRow(rs));
            }

            return list;
        }
    }
    
    // FIND BY BUSINESS KEY (NAME, PLANT, MATERIAL)
    public Optional<ClientMarkupEntity> findByCriteria(
            int clientId,
            int plantId,
            int materialId,
            YearMonth period) throws SQLException {

        String sql = """
                SELECT *
                FROM client_markup
                WHERE client_id = ?
                  AND plant_id = ?
                  AND material_id = ?
                  AND period = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, clientId);
            ps.setInt(2, plantId);
            ps.setInt(3, materialId);
            ps.setDate(4, Date.valueOf(period.atDay(1)));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }

        return Optional.empty();
    }

 /*   
    // UPDATE
    public boolean update(ClientMarkupEntity cm) throws SQLException {
        String sql = """
                UPDATE client_markup
                SET financial_percent = ?,
                    management_percent = ?
                WHERE client_id = ?
                  AND plant_id = ?
                  AND material_id = ?
                  AND period = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBigDecimal(1, cm.getFinancialPercent());
            ps.setBigDecimal(2, cm.getManagementPercent());

            ps.setInt(3, cm.getClient().getId());
            ps.setInt(4, cm.getPlant().getId());
            ps.setInt(5, cm.getMaterial().getId());
            ps.setDate(6, Date.valueOf(cm.getPeriod()));

            return ps.executeUpdate() > 0;
        }
    } **/
    

    // DELETE
    public boolean delete(int id) throws SQLException {

        String sql = "DELETE FROM client_markup WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
    
    public void saveOrUpdate(ClientMarkupEntity cm) throws SQLException {

        String sql = """
            INSERT INTO client_markup (
        		material_id, 
        		period,
        		prime,
                financial_percent, 
                management_percent, 
                plant_id, 
                client_id)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (client_id, plant_id, material_id, period)
            DO UPDATE SET
        		prime = EXCLUDED.prime,
                financial_percent = EXCLUDED.financial_percent,
                management_percent = EXCLUDED.management_percent
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, cm.getMaterial().getId());
            ps.setDate(2, Date.valueOf(cm.getPeriod().atDay(1)));
            ps.setBigDecimal(3, cm.getPrime());
            ps.setBigDecimal(4, cm.getFinancialPercent());
            ps.setBigDecimal(5, cm.getManagementPercent());  
            ps.setInt(6, cm.getPlant().getId());
            ps.setInt(7, cm.getClient().getId());

            ps.executeUpdate();
        }
    }
}
