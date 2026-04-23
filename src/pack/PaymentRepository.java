package pack;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * PaymentRepository — Payment entity için CRUD.
 * Component Diagram: PaymentRepository → payments table.
 * Sequence Diagram: charge($12, cardToken) → TXN-9821 → savePayment
 */
public class PaymentRepository {

    private final Connection conn;

    public PaymentRepository() {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    // ── SAVE ─────────────────────────────────────────────────────────────────
    public void save(Payment payment, String rideId) {
        String sql = """
            INSERT OR REPLACE INTO payments
                (id, ride_id, amount, method, is_paid, transaction_id, paid_at)
            VALUES (?,?,?,?,?,?,?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, payment.getId().toString());
            ps.setString(2, rideId);
            ps.setDouble(3, payment.getAmount());
            ps.setString(4, payment.getMethod().name());
            ps.setInt   (5, payment.isPaid() ? 1 : 0);
            ps.setString(6, payment.getTransactionId());
            ps.setString(7, payment.getPaidAt() != null ? payment.getPaidAt().toString() : null);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── MARK PAID ─────────────────────────────────────────────────────────────
    public void markPaid(String paymentId, String transactionId) {
        String sql = "UPDATE payments SET is_paid=1, transaction_id=?, paid_at=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, transactionId);
            ps.setString(2, java.time.LocalDateTime.now().toString());
            ps.setString(3, paymentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── FIND BY RIDE ──────────────────────────────────────────────────────────
    public Payment findByRideId(String rideId) {
        String sql = "SELECT * FROM payments WHERE ride_id = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rideId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapPayment(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ── FIND ALL ──────────────────────────────────────────────────────────────
    public List<Payment> findAll() {
        List<Payment> list = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM payments")) {
            while (rs.next()) list.add(mapPayment(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ── TOTAL REVENUE HELPER ──────────────────────────────────────────────────
    public double getTotalRevenue() {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT SUM(amount) FROM payments WHERE is_paid=1")) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    // ── MAPPER ────────────────────────────────────────────────────────────────
    private Payment mapPayment(ResultSet rs) throws SQLException {
        PaymentMethod method = PaymentMethod.CARD;
        try { method = PaymentMethod.valueOf(rs.getString("method")); } catch (Exception ignored) {}

        Payment p = new Payment(rs.getDouble("amount"), method);
        if (rs.getInt("is_paid") == 1) {
            p.markPaid(rs.getString("transaction_id"));
        }
        return p;
    }
}
