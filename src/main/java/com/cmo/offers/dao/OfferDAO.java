package com.cmo.offers.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.cmo.offers.entity.ClientEntity;
import com.cmo.offers.entity.OfferEntity;
import com.cmo.offers.model.row.OfferListRow;
import com.cmo.offers.utils.DatabaseConnection;

public class OfferDAO {

    private OfferEntity mapRow(ResultSet rs) throws SQLException {
        OfferEntity o = new OfferEntity();
        o.setId(rs.getInt("id"));
        o.setOfferNr(rs.getString("offer_nr"));

        Date d = rs.getDate("offer_date");
        o.setOfferDate(d == null ? null : d.toLocalDate());

        o.setRevision(rs.getString("revision"));
        o.setClientId(rs.getInt("client_id"));
        o.setRequestNr(rs.getString("request_nr"));
        return o;
    }

    // INSERT
    public void save(OfferEntity offer) throws SQLException {
        String sql = """
            INSERT INTO offer (offer_nr, offer_date, revision, client_id, request_nr)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, offer.getOfferNr());
            ps.setDate(2, Date.valueOf(offer.getOfferDate()));
            ps.setString(3, offer.getRevision());
            ps.setInt(4, offer.getClientId());
            ps.setString(5, offer.getRequestNr());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) offer.setId(rs.getInt("id"));
            }
        }
    }

    // FIND BY ID
    public Optional<OfferEntity> findById(int id) throws SQLException {
        String sql = """
            SELECT id, offer_nr, offer_date, revision, client_id, request_nr
            FROM offer
            WHERE id = ?
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    // FIND BY OFFER NR (useful if offer_nr must be unique)
    public Optional<OfferEntity> findByOfferNr(String offerNr) throws SQLException {
        String sql = """
            SELECT id, offer_nr, offer_date, revision, client_id, request_nr
            FROM offer
            WHERE offer_nr = ?
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, offerNr);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }
    
    // FIND BY CLIENT ID (db logic, uses ClientEntity)
    public List<OfferEntity> findByClientId(int clientId) throws SQLException {

    	 String sql = """
    		        SELECT id, offer_nr, offer_date, revision, client_id, request_nr
    		        FROM offer
    		        WHERE client_id = ?
    		        ORDER BY offer_date DESC
    		    """;

    		    List<OfferEntity> offers = new ArrayList<>();

    		    try (Connection conn = DatabaseConnection.getConnection();
    		         PreparedStatement ps = conn.prepareStatement(sql)) {

    		        ps.setInt(1, clientId);

    		        try (ResultSet rs = ps.executeQuery()) {
    		            while (rs.next()) {
    		                offers.add(mapRow(rs));
    		            }
    		        }
    		    }

    		    return offers;
    }
    
    // FIND BY CLIENT NAME (db logic, uses ClientEntity)
    public List<OfferEntity> findByClient(ClientEntity client) throws SQLException {
        return findByClientId(client.getId());
    }
        
    // This returns "rows" ready for TableView: customer name + request + offerNr + date + rev.
    public List<OfferListRow> findAllForList() throws SQLException {

        String sql = """
            SELECT
              o.id,
              c.name AS customer_name,
              o.request_nr,
              o.offer_nr,
              o.offer_date,
              o.revision
            FROM offer o
            JOIN client c ON c.id = o.client_id
            ORDER BY o.offer_date DESC, o.id DESC
        """;

        List<OfferListRow> rows = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                rows.add(new OfferListRow(
                        rs.getInt("id"),
                        rs.getString("customer_name"),
                        rs.getString("request_nr"),
                        rs.getString("offer_nr"),
                        rs.getDate("offer_date").toLocalDate(),
                        rs.getString("revision")
                ));
            }
        }

        return rows;
    }
    
    // Search ui table by client id, returns row with client name
    public List<OfferListRow> findForListByClientId(int clientId) throws SQLException {

        String sql = """
            SELECT
              o.id,
              c.name AS customer_name,
              o.request_nr,
              o.offer_nr,
              o.offer_date,
              o.revision
            FROM offer o
            JOIN client c ON c.id = o.client_id
            WHERE o.client_id = ?
            ORDER BY o.offer_date DESC, o.id DESC
        """;

        List<OfferListRow> rows = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, clientId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new OfferListRow(
                            rs.getInt("id"),
                            rs.getString("customer_name"),
                            rs.getString("request_nr"),
                            rs.getString("offer_nr"),
                            rs.getDate("offer_date").toLocalDate(),
                            rs.getString("revision")
                    ));
                }
            }
        }
        return rows;
    }

    // FIND ALL
    public List<OfferEntity> findAll() throws SQLException {
        String sql = """
            SELECT id, offer_nr, offer_date, revision, client_id, request_nr
            FROM offer
            ORDER BY offer_date DESC, id DESC
        """;

        List<OfferEntity> offers = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) offers.add(mapRow(rs));
        }

        return offers;
    }

    // UPDATE
    public void update(OfferEntity offer) throws SQLException {
        String sql = """
            UPDATE offer
            SET offer_nr = ?, offer_date = ?, revision = ?, client_id = ?, request_nr = ?
            WHERE id = ?
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, offer.getOfferNr());
            ps.setDate(2, Date.valueOf(offer.getOfferDate()));
            ps.setString(3, offer.getRevision());
            ps.setInt(4, offer.getClientId());
            ps.setString(5, offer.getRequestNr());
            ps.setInt(6, offer.getId());

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Updating offer failed, no rows affected.");
            }
        }
    }

    // DELETE
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM offer WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void saveOrUpdate(OfferEntity offer) throws SQLException {
        if (offer.getId() == 0) save(offer);
        else update(offer);
    }

}
