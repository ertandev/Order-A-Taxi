package pack;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DriverRepository — Driver entity için CRUD.
 * Component Diagram: DriverRepository → drivers table.
 */
public class DriverRepository {

    private final Connection conn;

    public DriverRepository() {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    // ── SAVE (INSERT OR REPLACE) ──────────────────────────────────────────────
    public void save(Driver d) {
        String sql = """
            INSERT OR REPLACE INTO drivers
                (id, name, email, password, phone, vehicle_model, plate_number,
                 service_type, is_verified, is_banned, is_available,
                 rating_sum, rating_count, avg_rating, status)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,  d.getId().toString());
            ps.setString(2,  d.getName());
            ps.setString(3,  d.getEmail());
            ps.setString(4,  d.password);
            ps.setString(5,  d.phone);
            ps.setString(6,  d.getVehicleInfo().split("\\(")[0].trim());
            ps.setString(7,  d.getPlateNumber());
            ps.setString(8,  d.getServiceType().name());
            ps.setInt   (9,  d.isApproved() ? 1 : 0);
            ps.setInt   (10, d.isBanned()   ? 1 : 0);
            ps.setInt   (11, d.isAvailable()? 1 : 0);
            ps.setDouble(12, d.getRatingSum());
            ps.setInt   (13, d.getRatingCount());
            ps.setDouble(14, d.getAverageRating());
            ps.setString(15, d.getStatus().name());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── FIND BY EMAIL ─────────────────────────────────────────────────────────
    public Driver findByEmail(String email) {
        String sql = "SELECT * FROM drivers WHERE email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapDriver(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ── FIND BY ID ────────────────────────────────────────────────────────────
    public Driver findById(String id) {
        String sql = "SELECT * FROM drivers WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapDriver(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ── FIND ALL ──────────────────────────────────────────────────────────────
    public List<Driver> findAll() {
        List<Driver> list = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM drivers")) {
            while (rs.next()) list.add(mapDriver(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ── FIND AVAILABLE (Activity Diagram: "Search Nearest Available Driver") ──
    public List<Driver> findAvailable() {
        List<Driver> list = new ArrayList<>();
        String sql = "SELECT * FROM drivers WHERE is_available=1 AND is_verified=1 AND is_banned=0";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapDriver(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ── FIND PENDING (Admin: approve docs) ───────────────────────────────────
    public List<Driver> findPending() {
        List<Driver> list = new ArrayList<>();
        String sql = "SELECT * FROM drivers WHERE is_verified=0 AND is_banned=0";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapDriver(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ── UPDATE STATUS ─────────────────────────────────────────────────────────
    public void updateStatus(String driverId, DriverStatus status) {
        String sql = "UPDATE drivers SET status = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, driverId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── MAPPER ────────────────────────────────────────────────────────────────
    private Driver mapDriver(ResultSet rs) throws SQLException {
        String name    = rs.getString("name");
        String email   = rs.getString("email");
        String pass    = rs.getString("password");
        String phone   = rs.getString("phone");
        String vModel  = rs.getString("vehicle_model");
        String plate   = rs.getString("plate_number");
        String sType   = rs.getString("service_type");

        VehicleType vt = VehicleType.ECONOMY;
        try { vt = VehicleType.valueOf(sType); } catch (Exception ignored) {}

        Driver d = new Driver(name, email, pass, phone, vModel, plate, vt);
        try { d.setId(java.util.UUID.fromString(rs.getString("id"))); } catch (Exception ignored) {}
        d.setVerified(rs.getInt("is_verified") == 1);
        d.setBanned  (rs.getInt("is_banned")   == 1);
        d.setAvailable(rs.getInt("is_available") == 1);

        DriverStatus ds = DriverStatus.IDLE;
        try { ds = DriverStatus.valueOf(rs.getString("status")); } catch (Exception ignored) {}
        d.setStatus(ds);
        
        d.setRatingSum(rs.getDouble("rating_sum"));
        d.setRatingCount(rs.getInt("rating_count"));

        return d;
    }
}
