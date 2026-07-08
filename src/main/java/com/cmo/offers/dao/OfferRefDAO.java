package com.cmo.offers.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.cmo.offers.entity.OfferRefEntity;
import com.cmo.offers.utils.DatabaseConnection;

public class OfferRefDAO {

    private static final String BASE_SELECT =
            "SELECT id, offer_id, doc FROM offer_reference";

    private OfferRefEntity mapRow(ResultSet rs) throws SQLException {
        OfferRefEntity ref = new OfferRefEntity();
        ref.setId(rs.getInt("id"));
        ref.setOfferId(rs.getInt("offer_id"));
        ref.setDoc(rs.getString("doc"));
        return ref;
    }

    public void save(OfferRefEntity ref) throws SQLException {
        String sql = """
        		INSERT INTO offer_reference (offer_id, doc) 
        		VALUES (?, ?) 
        		RETURNING id
        		""";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, ref.getOfferId());
            ps.setString(2, ref.getDoc());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ref.setId(rs.getInt("id"));
                }
            }
        }
    }

    public Optional<OfferRefEntity> findById(int id) throws SQLException {
        String sql = BASE_SELECT + " WHERE id = ?";

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

    public List<OfferRefEntity> findByOfferId(int offerId) throws SQLException {
        String sql = BASE_SELECT + " WHERE offer_id = ? ORDER BY doc";

        List<OfferRefEntity> refs = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, offerId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    refs.add(mapRow(rs));
                }
            }
        }

        return refs;
    }

    public List<OfferRefEntity> findAll() throws SQLException {
        String sql = BASE_SELECT + " ORDER BY doc";

        List<OfferRefEntity> refs = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                refs.add(mapRow(rs));
            }
        }

        return refs;
    }

    public void update(OfferRefEntity ref) throws SQLException {
        String sql = "UPDATE offer_reference SET offer_id = ?, doc = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, ref.getOfferId());
            ps.setString(2, ref.getDoc());
            ps.setInt(3, ref.getId());

            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM offer_reference WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
    
    public List<OfferRefEntity> deleteByOfferId(int offerId) throws SQLException {

        String sql = """
            DELETE FROM offer_reference
            WHERE offer_id = ?
            RETURNING id, offer_id, doc
            """;

        List<OfferRefEntity> deleted = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, offerId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    deleted.add(mapRow(rs));
                }
            }
        }

        return deleted;
    }

    public void saveOrUpdate(OfferRefEntity ref) throws SQLException {
        if (ref.getId() <= 0) {
            save(ref);
        } else {
            update(ref);
        }
    }

    public boolean existsByOfferIdAndDoc(int offerId, String doc) throws SQLException {
        String sql = "SELECT 1 FROM offer_reference WHERE offer_id = ? AND doc = ? LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, offerId);
            ps.setString(2, doc);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}