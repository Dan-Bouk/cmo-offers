package com.cmo.offers.dao;

import com.cmo.offers.utils.DatabaseConnection;
import com.cmo.offers.entity.PlantEntity;
import com.cmo.offers.entity.ClientEntity;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlantDAO {
	
    private PlantEntity mapRow(ResultSet rs) throws SQLException {
    	PlantEntity p = new PlantEntity();
        p.setId(rs.getInt("id"));
        p.setName(rs.getString("name"));

        ClientEntity c = new ClientEntity();
        c.setId(rs.getInt("client_id"));
        p.setClient(c);

        return p;
    }
    
    public void save(PlantEntity plant) throws SQLException {
        String sql = """
        		INSERT INTO plant (client_id, name)
        		VALUES (?, ?)
        		RETURNING id
        		""";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, plant.getClient().getId());
            ps.setString(2, plant.getName());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    plant.setId(rs.getInt("id"));
                }
            }
        }
    }

	    public List<PlantEntity> findAll() throws SQLException {
	        String sql = """
	        		SELECT id, 
	        		client_id, 
	        		name FROM plant 
	        		ORDER BY name
	        		""";

	        List<PlantEntity> list = new ArrayList<>();

	        try (Connection conn = DatabaseConnection.getConnection();
	             PreparedStatement ps = conn.prepareStatement(sql);
	             ResultSet rs = ps.executeQuery()) {

	            while (rs.next()) {
	                list.add(mapRow(rs));
	            }
	        }
	        return list;
	    }

	    public List<PlantEntity> findByClientId(int clientId) throws SQLException {
	        String sql = """
	        		SELECT id, 
	        		client_id, 
	        		name FROM plant 
	        		WHERE client_id = ? 
	        		ORDER BY name""";

	        List<PlantEntity> list = new ArrayList<>();

	        try (Connection conn = DatabaseConnection.getConnection();
	             PreparedStatement ps = conn.prepareStatement(sql)) {

	            ps.setInt(1, clientId);

	            try (ResultSet rs = ps.executeQuery()) {
	                while (rs.next()) {
	                    list.add(mapRow(rs));
	                }
	            }
	        }
	        return list;
	    }

	    public Optional<PlantEntity> findById(int id) throws SQLException {
	        String sql = "SELECT id, client_id, name FROM plant WHERE id = ?";

	        try (Connection conn = DatabaseConnection.getConnection();
	             PreparedStatement ps = conn.prepareStatement(sql)) {

	            ps.setInt(1, id);

	            try (ResultSet rs = ps.executeQuery()) {
	                if (rs.next()) return Optional.of(mapRow(rs));
	            }
	        }
	        return Optional.empty();
	    }

	    public Optional<PlantEntity> findByClientIdAndName(int clientId, String name) throws SQLException {
	        String sql = "SELECT id, client_id, name FROM plant WHERE client_id = ? AND name = ?";

	        try (Connection conn = DatabaseConnection.getConnection();
	             PreparedStatement ps = conn.prepareStatement(sql)) {

	            ps.setInt(1, clientId);
	            ps.setString(2, name);

	            try (ResultSet rs = ps.executeQuery()) {
	                if (rs.next()) return Optional.of(mapRow(rs));
	            }
	        }
	        return Optional.empty();
	    }

	    

	    public boolean update(PlantEntity plant) throws SQLException {
	        // Usually you update only name; moving a plant to another client is a separate operation.
	        String sql = "UPDATE plant SET name = ? WHERE id = ?";

	        try (Connection conn = DatabaseConnection.getConnection();
	             PreparedStatement ps = conn.prepareStatement(sql)) {

	            ps.setString(1, plant.getName());
	            ps.setInt(2, plant.getId());

	            return ps.executeUpdate() > 0;
	        }
	    }

	    public boolean delete(int id) throws SQLException {
	        String sql = "DELETE FROM plant WHERE id = ?";

	        try (Connection conn = DatabaseConnection.getConnection();
	             PreparedStatement ps = conn.prepareStatement(sql)) {

	            ps.setInt(1, id);
	            return ps.executeUpdate() > 0;
        }
    }
}
