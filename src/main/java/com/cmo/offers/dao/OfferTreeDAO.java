package com.cmo.offers.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.cmo.offers.model.row.OfferRefJoinRow;
import com.cmo.offers.utils.DatabaseConnection;

public class OfferTreeDAO {
	
    private OfferRefJoinRow mapRow(ResultSet rs) throws SQLException {
        Date sqlDate = rs.getDate("offer_date");

        return new OfferRefJoinRow(
                rs.getInt("offer_id"),
                rs.getString("customer_name"),
                rs.getString("request_nr"),
                rs.getString("offer_nr"),
                sqlDate == null ? null : sqlDate.toLocalDate(),
                rs.getString("revision"),
                (Integer) rs.getObject("reference_id"),
                rs.getString("reference_doc")
        );
    }

    public List<OfferRefJoinRow> findAllForTree() throws SQLException {

        String sql = """
            SELECT
                o.id AS offer_id,
                c.name AS customer_name,
                o.request_nr,
                o.offer_nr,
                o.offer_date,
                o.revision,
                r.id AS reference_id,
                r.doc AS reference_doc
            FROM offer o
            JOIN client c
                ON c.id = o.client_id
            LEFT JOIN offer_reference r
                ON r.offer_id = o.id
            ORDER BY o.offer_date DESC, o.id DESC, r.doc
        """;

        List<OfferRefJoinRow> rows = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                rows.add(mapRow(rs));
            }
        }

        return rows;
    }

    public List<OfferRefJoinRow> findByClientIdForTree(int clientId) throws SQLException {

        String sql = """
            SELECT
                o.id AS offer_id,
                c.name AS customer_name,
                o.request_nr,
                o.offer_nr,
                o.offer_date,
                o.revision,
                r.id AS reference_id,
                r.doc AS reference_doc
            FROM offer o
            JOIN client c
                ON c.id = o.client_id
            LEFT JOIN offer_reference r
                ON r.offer_id = o.id
            WHERE o.client_id = ?
            ORDER BY o.offer_date DESC, o.id DESC, r.doc
        """;

        List<OfferRefJoinRow> rows = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, clientId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapRow(rs));
                }
            }
        }

        return rows;
    }

}
