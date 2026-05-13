package pack;

import java.sql.*;

/**
 * DatabaseManager — Singleton SQLite bağlantı yöneticisi.
 * Component Diagram: PostgreSQL → SQLite (embedded, ücretsiz).
 * Uygulama başlarken initialize() çağrılır, tüm tablolar oluşturulur.
 */
public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:orderataxi.db";
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            connection.createStatement().execute("PRAGMA foreign_keys = ON");
            initialize();
        } catch (Exception e) {
            throw new RuntimeException("SQLite connection failed: " + e.getMessage(), e);
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    /** Creates all tables if they don't exist (Component Diagram schema). */
    private void initialize() throws SQLException {
        try (Statement st = connection.createStatement()) {

            // --- USERS (passengers + admins base) ---
            st.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id        TEXT PRIMARY KEY,
                    name      TEXT NOT NULL,
                    email     TEXT UNIQUE NOT NULL,
                    password  TEXT NOT NULL,
                    phone     TEXT,
                    role      TEXT NOT NULL   -- PASSENGER | ADMIN
                )
            """);

            // --- DRIVERS (separate table, joined via id) ---
            st.execute("""
                CREATE TABLE IF NOT EXISTS drivers (
                    id               TEXT PRIMARY KEY,
                    name             TEXT NOT NULL,
                    email            TEXT UNIQUE NOT NULL,
                    password         TEXT NOT NULL,
                    phone            TEXT,
                    vehicle_model    TEXT,
                    plate_number     TEXT,
                    service_type     TEXT DEFAULT 'ECONOMY',
                    is_verified      INTEGER DEFAULT 0,
                    is_banned        INTEGER DEFAULT 0,
                    is_available     INTEGER DEFAULT 1,
                    rating_sum       REAL    DEFAULT 5.0,
                    rating_count     INTEGER DEFAULT 1,
                    avg_rating       REAL    DEFAULT 5.0,
                    status           TEXT    DEFAULT 'IDLE'
                )
            """);

            // --- RIDES ---
            st.execute("""
                CREATE TABLE IF NOT EXISTS rides (
                    id                 TEXT PRIMARY KEY,
                    passenger_id       TEXT NOT NULL,
                    driver_id          TEXT,
                    start_loc          TEXT,
                    end_loc            TEXT,
                    vehicle_type       TEXT DEFAULT 'ECONOMY',
                    status             TEXT DEFAULT 'REQUESTED',
                    distance_km        REAL DEFAULT 0,
                    fare_amount        REAL DEFAULT 0,
                    estimated_minutes  INTEGER DEFAULT 0,
                    pickup_lat         REAL,
                    pickup_lon         REAL,
                    pickup_addr        TEXT,
                    dropoff_lat        REAL,
                    dropoff_lon        REAL,
                    dropoff_addr       TEXT,
                    scheduled_at       TEXT,
                    rating             INTEGER DEFAULT 0,
                    feedback           TEXT    DEFAULT '',
                    passenger_comment  TEXT    DEFAULT '',
                    tip_amount         REAL    DEFAULT 0
                )
            """);

            // --- PAYMENTS ---
            st.execute("""
                CREATE TABLE IF NOT EXISTS payments (
                    id             TEXT PRIMARY KEY,
                    ride_id        TEXT NOT NULL,
                    amount         REAL NOT NULL,
                    method         TEXT DEFAULT 'CARD',
                    is_paid        INTEGER DEFAULT 0,
                    transaction_id TEXT DEFAULT '',
                    paid_at        TEXT
                )
            """);

            // --- SUPPORT TICKETS ---
            st.execute("""
                CREATE TABLE IF NOT EXISTS support_tickets (
                    id          TEXT PRIMARY KEY,
                    user_id     TEXT NOT NULL,
                    driver_id   TEXT,
                    type        TEXT NOT NULL,
                    description TEXT,
                    is_resolved INTEGER DEFAULT 0,
                    created_at  TEXT
                )
            """);

            // --- DOCUMENTS ---
            st.execute("""
                CREATE TABLE IF NOT EXISTS documents (
                    id          TEXT PRIMARY KEY,
                    owner_id    TEXT NOT NULL,
                    type        TEXT NOT NULL,
                    file_path   TEXT,
                    uploaded_at TEXT,
                    is_approved INTEGER DEFAULT 0
                )
            """);

            // Migration: Add tip_amount if not exists
            try { st.execute("ALTER TABLE rides ADD COLUMN tip_amount REAL DEFAULT 0"); } catch (SQLException ignored) {}
            // Migration: Add avg_rating if not exists
            try { st.execute("ALTER TABLE drivers ADD COLUMN avg_rating REAL DEFAULT 5.0"); } catch (SQLException ignored) {}
            // Migration: Add start_time if not exists
            try { st.execute("ALTER TABLE rides ADD COLUMN start_time TEXT"); } catch (SQLException ignored) {}
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed())
                connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
