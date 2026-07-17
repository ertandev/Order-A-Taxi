package pack;

import java.sql.*;

/**
 * DatabaseManager — Singleton MySQL bağlantı yöneticisi.
 */
public class DatabaseManager {

    // MySQL Bağlantı Bilgileri
    private static final String DB_URL = "jdbc:mysql://localhost:3306/";
    private static final String DB_USER = "root"; // Genelde root'tur
    private static final String DB_PASS = "YOUR_DATABASE_PASSWORD"; 
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        try {
            // MySQL Sürücüsünü yüklüyoruz
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            
            // Veritabanı yoksa oluştur ve seç
            try (Statement st = connection.createStatement()) {
                st.execute("CREATE DATABASE IF NOT EXISTS orderataxi");
                st.execute("USE orderataxi");
            }
            
            initialize();
        } catch (Exception e) {
            throw new RuntimeException("MySQL connection failed: " + e.getMessage(), e);
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    /** Tabloları oluşturur (MySQL uyumlu) */
    private void initialize() throws SQLException {
        try (Statement st = connection.createStatement()) {

            // MySQL'de TEXT PRIMARY KEY olamaz, bu yüzden VARCHAR(255) yaptık.
            st.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id        VARCHAR(255) PRIMARY KEY,
                    name      VARCHAR(255) NOT NULL,
                    email     VARCHAR(255) UNIQUE NOT NULL,
                    password  VARCHAR(255) NOT NULL,
                    phone     VARCHAR(255),
                    role      VARCHAR(255) NOT NULL
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS drivers (
                    id               VARCHAR(255) PRIMARY KEY,
                    name             VARCHAR(255) NOT NULL,
                    email            VARCHAR(255) UNIQUE NOT NULL,
                    password         VARCHAR(255) NOT NULL,
                    phone            VARCHAR(255),
                    vehicle_model    VARCHAR(255),
                    plate_number     VARCHAR(255),
                    service_type     VARCHAR(255) DEFAULT 'ECONOMY',
                    is_verified      INTEGER DEFAULT 0,
                    is_banned        INTEGER DEFAULT 0,
                    is_available     INTEGER DEFAULT 1,
                    rating_sum       REAL    DEFAULT 5.0,
                    rating_count     INTEGER DEFAULT 1,
                    avg_rating       REAL    DEFAULT 5.0,
                    status           VARCHAR(255) DEFAULT 'IDLE'
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS rides (
                    id                 VARCHAR(255) PRIMARY KEY,
                    passenger_id       VARCHAR(255) NOT NULL,
                    driver_id          VARCHAR(255),
                    start_loc          TEXT,
                    end_loc            TEXT,
                    vehicle_type       VARCHAR(255) DEFAULT 'ECONOMY',
                    status             VARCHAR(255) DEFAULT 'REQUESTED',
                    distance_km        REAL DEFAULT 0,
                    fare_amount        REAL DEFAULT 0,
                    estimated_minutes  INTEGER DEFAULT 0,
                    pickup_lat         REAL,
                    pickup_lon         REAL,
                    pickup_addr        TEXT,
                    dropoff_lat        REAL,
                    dropoff_lon        REAL,
                    dropoff_addr       TEXT,
                    scheduled_at       VARCHAR(255),
                    rating             INTEGER DEFAULT 0,
                    feedback           TEXT,
                    passenger_comment  TEXT,
                    tip_amount         REAL    DEFAULT 0,
                    start_time         VARCHAR(255)
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS payments (
                    id             VARCHAR(255) PRIMARY KEY,
                    ride_id        VARCHAR(255) NOT NULL,
                    amount         REAL NOT NULL,
                    method         VARCHAR(255) DEFAULT 'CARD',
                    is_paid        INTEGER DEFAULT 0,
                    transaction_id VARCHAR(255) DEFAULT '',
                    paid_at        VARCHAR(255)
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS support_tickets (
                    id          VARCHAR(255) PRIMARY KEY,
                    user_id     VARCHAR(255) NOT NULL,
                    driver_id   VARCHAR(255),
                    type        VARCHAR(255) NOT NULL,
                    description TEXT,
                    is_resolved INTEGER DEFAULT 0,
                    created_at  VARCHAR(255)
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS documents (
                    id          VARCHAR(255) PRIMARY KEY,
                    owner_id    VARCHAR(255) NOT NULL,
                    type        VARCHAR(255) NOT NULL,
                    file_path   TEXT,
                    uploaded_at VARCHAR(255),
                    is_approved INTEGER DEFAULT 0
                )
            """);
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
