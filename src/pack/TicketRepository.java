package pack;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * TicketRepository — SupportTicket entity için CRUD.
 * Component Diagram: TicketRepository → support_tickets table.
 * Use Case: "Open Support Ticket", "Manage Support Tickets"
 */
public class TicketRepository {

    private final Connection conn;

    public TicketRepository() {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    // ── SAVE ─────────────────────────────────────────────────────────────────
    public void save(SupportTicket ticket) {
        String sql = """
            INSERT OR REPLACE INTO support_tickets
                (id, user_id, driver_id, type, description, is_resolved, created_at)
            VALUES (?,?,?,?,?,?,?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ticket.getId().toString());
            ps.setString(2, ticket.getUser().getId().toString());
            ps.setString(3, ticket.getDriver() != null ? ticket.getDriver().getId().toString() : null);
            ps.setString(4, ticket.getType().name());
            ps.setString(5, ticket.getDescription());
            ps.setInt   (6, ticket.isResolved() ? 1 : 0);
            ps.setString(7, ticket.getCreatedAt() != null ? ticket.getCreatedAt().toString() : LocalDateTime.now().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── RESOLVE ───────────────────────────────────────────────────────────────
    public void resolve(String ticketId) {
        String sql = "UPDATE support_tickets SET is_resolved = 1 WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ticketId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── FIND ALL ──────────────────────────────────────────────────────────────
    public List<SupportTicket> findAll() {
        List<SupportTicket> list = new ArrayList<>();
        String sql = "SELECT * FROM support_tickets ORDER BY created_at DESC";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapTicket(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ── FIND OPEN ─────────────────────────────────────────────────────────────
    public List<SupportTicket> findOpen() {
        List<SupportTicket> list = new ArrayList<>();
        String sql = "SELECT * FROM support_tickets WHERE is_resolved=0 ORDER BY created_at DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapTicket(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ── COUNT OPEN (Admin stats) ───────────────────────────────────────────────
    public int countOpen() {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM support_tickets WHERE is_resolved=0")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // ── MAPPER (ghost objects — full hydration in Phase 3 services) ───────────
    private SupportTicket mapTicket(ResultSet rs) throws SQLException {
        String userId   = rs.getString("user_id");
        String driverId = rs.getString("driver_id");
        String typeStr  = rs.getString("type");
        String desc     = rs.getString("description");
        boolean resolved = rs.getInt("is_resolved") == 1;

        // Ghost user/driver for display; Phase 3 AuthService will fully hydrate
        Passenger ghostUser   = new Passenger("", userId, "", "");
        Driver    ghostDriver = driverId != null
                ? new Driver("", driverId, "", "", "", "", VehicleType.ECONOMY)
                : null;

        TicketType type = TicketType.DRIVER_ISSUE;
        try { type = TicketType.valueOf(typeStr); } catch (Exception ignored) {}

        SupportTicket ticket = new SupportTicket(ghostUser, ghostDriver, type, desc);
        ticket.setResolved(resolved);
        return ticket;
    }
}
