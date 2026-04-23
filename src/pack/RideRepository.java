package pack;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * RideRepository — Ride entity için CRUD.
 * Component Diagram: RideRepository → rides table.
 * State Machine Diagram: status geçişleri buradan persist edilir.
 */
public class RideRepository {

    private final Connection conn;

    public RideRepository() {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    // ── SAVE (INSERT OR REPLACE) ──────────────────────────────────────────────
    public void save(Ride ride) {
        String sql = """
            INSERT OR REPLACE INTO rides
                (id, passenger_id, driver_id, start_loc, end_loc, vehicle_type,
                 status, distance_km, fare_amount, estimated_minutes,
                 pickup_lat, pickup_lon, pickup_addr,
                 dropoff_lat, dropoff_lon, dropoff_addr,
                 scheduled_at, rating, feedback, passenger_comment, tip_amount, start_time)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,  ride.getId().toString());
            ps.setString(2,  ride.getPassenger().getId().toString());
            ps.setString(3,  ride.getDriver() != null ? ride.getDriver().getId().toString() : null);
            ps.setString(4,  ride.getStartLoc());
            ps.setString(5,  ride.getEndLoc());
            ps.setString(6,  ride.getVehicleType().name());
            ps.setString(7,  ride.getStatus().name());
            ps.setDouble(8,  ride.getDistanceKm());
            ps.setDouble(9,  ride.getFareAmount());
            ps.setInt   (10, ride.getEstimatedMinutes());

            Location pickup = ride.getPickupLocation();
            if (pickup != null) {
                ps.setDouble(11, pickup.latitude);
                ps.setDouble(12, pickup.longitude);
                ps.setString(13, pickup.address);
            } else { ps.setNull(11, Types.REAL); ps.setNull(12, Types.REAL); ps.setNull(13, Types.VARCHAR); }

            Location dropoff = ride.getDropoffLocation();
            if (dropoff != null) {
                ps.setDouble(14, dropoff.latitude);
                ps.setDouble(15, dropoff.longitude);
                ps.setString(16, dropoff.address);
            } else { ps.setNull(14, Types.REAL); ps.setNull(15, Types.REAL); ps.setNull(16, Types.VARCHAR); }

            ps.setString(17, ride.getScheduledAt() != null ? ride.getScheduledAt().toString() : null);
            ps.setInt   (18, 0);      // rating set separately
            ps.setString(19, "");
            ps.setString(20, ride.getPassengerComment());
            ps.setDouble(21, ride.getTipAmount());
            ps.setString(22, ride.getStartTime() != null ? ride.getStartTime().toString() : null);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── UPDATE STATUS (State Machine transitions) ─────────────────────────────
    public void updateStatus(String rideId, RideStatus status) {
        String sql = "UPDATE rides SET status = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, rideId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── UPDATE DRIVER ─────────────────────────────────────────────────────────
    public void assignDriver(String rideId, String driverId) {
        String sql = "UPDATE rides SET driver_id = ?, status = 'ACCEPTED' WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, driverId);
            ps.setString(2, rideId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── SAVE RATING (Use Case: "Rate Driver") ────────────────────────────────
    public void saveRating(String rideId, int stars, String feedback) {
        String sql = "UPDATE rides SET rating = ?, feedback = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt   (1, stars);
            ps.setString(2, feedback);
            ps.setString(3, rideId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── FIND BY PASSENGER (Use Case: "View Ride History") ─────────────────────
    public List<Ride> findByPassengerId(String passengerId) {
        List<Ride> list = new ArrayList<>();
        String sql = "SELECT id, passenger_id, start_loc, end_loc, status, vehicle_type, distance_km, fare_amount, estimated_minutes, tip_amount FROM rides WHERE passenger_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, passengerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRideShallow(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ── FIND BY DRIVER (Use Case: "View Earnings History") ───────────────────
    public List<Ride> findByDriverId(String driverId) {
        List<Ride> list = new ArrayList<>();
        String sql = "SELECT id, passenger_id, start_loc, end_loc, status, vehicle_type, distance_km, fare_amount, estimated_minutes, tip_amount FROM rides WHERE driver_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, driverId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRideShallow(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ── FIND ALL (Admin: system stats) ────────────────────────────────────────
    public List<Ride> findAll() {
        List<Ride> list = new ArrayList<>();
        String sql = "SELECT * FROM rides";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRideShallow(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ── STATS (Admin: View System Statistics) ────────────────────────────────
    public double getTotalRevenue() {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT SUM(fare_amount) FROM rides WHERE status='COMPLETED'")) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0.0;
    }

    public int countByStatus(RideStatus status) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM rides WHERE status=?")) {
            ps.setString(1, status.name());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    // ── SHALLOW MAPPER (no passenger/driver object needed for lists) ──────────
    private Ride mapRideShallow(ResultSet rs) throws SQLException {
        // Create a minimal ride for display purposes
        Passenger ghost = new Passenger("", "", "", "");
        try { ghost.setId(java.util.UUID.fromString(rs.getString("passenger_id"))); } catch (Exception ignored) {}
        
        Ride r = new Ride(ghost, rs.getString("start_loc"), rs.getString("end_loc"), "");
        try { r.setId(java.util.UUID.fromString(rs.getString("id"))); } catch (Exception ignored) {}

        VehicleType vt = VehicleType.ECONOMY;
        try { vt = VehicleType.valueOf(rs.getString("vehicle_type")); } catch (Exception ignored) {}

        RideStatus st = RideStatus.REQUESTED;
        try { st = RideStatus.valueOf(rs.getString("status")); } catch (Exception ignored) {}

        r.setVehicleType(vt);
        r.setStatus(st);
        r.setDistanceKm(rs.getDouble("distance_km"));
        r.setFareAmount(rs.getDouble("fare_amount"));
        r.setEstimatedMinutes(rs.getInt("estimated_minutes"));
        r.setTipAmount(rs.getDouble("tip_amount"));
        
        String stTime = rs.getString("start_time");
        if (stTime != null && !stTime.isEmpty()) {
            try { r.setStartTime(LocalDateTime.parse(stTime)); } catch (Exception ignored) {}
        }
        
        return r;
    }
}
