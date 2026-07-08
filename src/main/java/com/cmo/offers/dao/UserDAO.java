package com.cmo.offers.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.cmo.offers.entity.UserEntity;
import com.cmo.offers.utils.DatabaseConnection;

public class UserDAO {
	
    private UserEntity mapRow(ResultSet rs) throws SQLException {
        UserEntity user = new UserEntity();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(rs.getString("role"));
        user.setActive(rs.getBoolean("active"));
        return user;
    }
    
    // INSERT
    public void save(UserEntity user) throws SQLException {

        String sql = """
            INSERT INTO users (username, password_hash, role, active)
            VALUES (?, ?, ?, ?)
            RETURNING id
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getRole());
            ps.setBoolean(4, user.isActive());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    user.setId(rs.getInt(1));
                }
            }
        }
    }
    
    // FIND BY ID
    public Optional<UserEntity> findById(int id) throws SQLException {

        String sql = """
                SELECT id, username, password_hash, role, active
                FROM users
                WHERE id = ?
                """;

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

    // FIND BY USERNAME
    public Optional<UserEntity> findByUsername(String username) throws SQLException {

        String sql = """
                SELECT id, username, password_hash, role, active
                FROM users
                WHERE LOWER(username) = LOWER(?)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }

        return Optional.empty();
    }

    // FIND ALL
    public List<UserEntity> findAll() throws SQLException {

        String sql = """
                SELECT id, username, password_hash, role, active
                FROM users
                ORDER BY username
                """;

        List<UserEntity> users = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                users.add(mapRow(rs));
            }
        }

        return users;
    }
    
    // UPDATE
    public void update(UserEntity user) throws SQLException {

        String sql = """
                UPDATE users
                SET username = ?, password_hash = ?, role = ?, active = ?
                WHERE id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getRole());
            ps.setBoolean(4, user.isActive());
            ps.setInt(5, user.getId());

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Updating user failed, no rows affected.");
            }
        }
    }
    
    // DELETE
    public void delete(int id) throws SQLException {

        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void saveOrUpdate(UserEntity user) throws SQLException {

        if (user.getId() == 0) {
            save(user);
        } else {
            update(user);
        }
    }
    
}
