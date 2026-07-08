package com.cmo.offers.dao;

import com.cmo.offers.utils.DatabaseConnection;
import com.cmo.offers.entity.MaterialEntity;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MaterialDAO {
	
    private MaterialEntity mapRow(ResultSet rs) throws SQLException {
	        MaterialEntity m = new MaterialEntity();
	        m.setId(rs.getInt("id"));
	        m.setCode(rs.getString("code"));
	        return m;
	    }

	    public List<MaterialEntity> findAll() throws SQLException {
	        String sql = "SELECT id, code FROM material ORDER BY code DESC";

	        List<MaterialEntity> list = new ArrayList<>();

	        try (Connection conn = DatabaseConnection.getConnection();
	             PreparedStatement ps = conn.prepareStatement(sql);
	             ResultSet rs = ps.executeQuery()) {

	            while (rs.next()) {
	                list.add(mapRow(rs));
	            }
        }
        return list;
    }

    public Optional<MaterialEntity> findById(int id) throws SQLException {
        String sql = "SELECT id, code, name FROM material WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public Optional<MaterialEntity> findByCode(String code) throws SQLException {
        String sql = "SELECT id, code, name FROM material WHERE code = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, code);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public void save(MaterialEntity material) throws SQLException {
        String sql = "INSERT INTO material code VALUE ? RETURNING id";

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, material.getCode());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    material.setId(rs.getInt("id"));
                }
            }
        }
    }

    public boolean update(MaterialEntity material) throws SQLException {
        String sql = "UPDATE material SET code = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, material.getCode());
            ps.setInt(2, material.getId());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM material WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}
