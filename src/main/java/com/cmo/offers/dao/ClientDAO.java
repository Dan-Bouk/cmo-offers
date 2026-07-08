package com.cmo.offers.dao;

import com.cmo.offers.entity.ClientEntity;
import com.cmo.offers.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClientDAO {
	
    private ClientEntity mapRow(ResultSet rs) throws SQLException {
        ClientEntity client = new ClientEntity();
        client.setId(rs.getInt("id"));
        client.setName(rs.getString("name"));
        return client;
    }
	
    // INSERT
    public void save(ClientEntity client) throws SQLException {

        String sql = """
        		INSERT INTO client (name) 
        		VALUES (?) 
        		RETURNING id
        		""";

        try (Connection conn = DatabaseConnection.getConnection();
        		PreparedStatement ps = conn.prepareStatement(sql)) {

       	    ps.setString(1, client.getName());

       	    try (ResultSet rs = ps.executeQuery()) {
       	        if (rs.next()) {
       	            client.setId(rs.getInt("id"));
       	        }
       	    }
        }
    }
    
    public Optional<ClientEntity> findById(int id) throws SQLException {

        String sql = "SELECT id, name FROM client WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }

        return Optional.empty();
    }

	
	// FIND BY NAME
    public Optional<ClientEntity> findByName(String name) throws SQLException {
        String sql = "SELECT id, name FROM client WHERE LOWER(name) = LOWER(?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                	return Optional.of(mapRow(rs));
                }
            }
        }

        return Optional.empty();
    }


	// FIND ALL
    public List<ClientEntity> findAll() throws SQLException {

        String sql = "SELECT id, name FROM client ORDER BY name";

        List<ClientEntity> clients = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
            	clients.add(mapRow(rs));
            }
        }

        return clients;
    }

    public void update(ClientEntity client) throws SQLException {

        String sql = "UPDATE client SET name = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, client.getName());
            ps.setInt(2, client.getId());

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Updating client failed, no rows affected.");
            }
        }
    }
    

    // DELETE
    public void delete(int id) throws SQLException {

        String sql = "DELETE FROM client WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
    
    public void saveOrUpdate(ClientEntity client) throws SQLException {

        if (client.getId() == 0) {
            save(client);
        } else {
            update(client);
        }
    }
}