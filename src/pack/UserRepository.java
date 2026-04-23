package pack;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * UserRepository — Passenger ve Admin için CRUD.
 * Component Diagram: UserRepository → users table.
 */
public class UserRepository {

    private final Connection conn;

    public UserRepository() {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    // ── SAVE (INSERT OR REPLACE) ──────────────────────────────────────────────
    public void save(User user) {
        String role = user instanceof Admin ? "ADMIN" : "PASSENGER";
        String sql  = """
            INSERT OR REPLACE INTO users (id, name, email, password, phone, role)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getId().toString());
            ps.setString(2, user.getName());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.password);       // package-level access
            ps.setString(5, user.phone);
            ps.setString(6, role);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── FIND BY EMAIL ─────────────────────────────────────────────────────────
    public User findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapUser(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ── FIND ALL ──────────────────────────────────────────────────────────────
    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM users")) {
            while (rs.next()) list.add(mapUser(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ── UPDATE PASSWORD ───────────────────────────────────────────────────────
    public boolean updatePassword(String email, String newPass) {
        String sql = "UPDATE users SET password = ? WHERE email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPass);
            ps.setString(2, email);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ── MAPPER ────────────────────────────────────────────────────────────────
    private User mapUser(ResultSet rs) throws SQLException {
        String role  = rs.getString("role");
        String name  = rs.getString("name");
        String email = rs.getString("email");
        String pass  = rs.getString("password");
        String phone = rs.getString("phone");
        User u;
        if ("ADMIN".equals(role)) {
            u = new Admin(name, email, pass);
        } else {
            u = new Passenger(name, email, pass, phone);
        }
        try { u.setId(java.util.UUID.fromString(rs.getString("id"))); } catch (Exception ignored) {}
        return u;
    }
}
