package pack;

import java.util.List;

/**
 * DriverService — Component Diagram: DriverService
 * Activity Diagram: "Search Nearest Available Driver" → "Assign Driver to Ride"
 * State Machine: IDLE → ON_THE_WAY → IN_RIDE → IDLE
 */
public class DriverService {

    private final TaxiManager      manager;
    private final DriverRepository driverRepo;

    public DriverService(TaxiManager manager) {
        this.manager    = manager;
        this.driverRepo = new DriverRepository();
    }

    // ── EN YAKIN SÜRÜCÜYÜ BUL (Activity Diagram: "Search Nearest Available Driver") ──
    /**
     * Communication Diagram: findNearestAvailable(pickup, accept) → driver
     * Uygun sürücüleri alır, pickup'a en yakın olanı döndürür.
     */
    public Driver findNearestDriver(Location pickup, VehicleType requiredType) {
        List<Driver> available = manager.getAvailableDrivers();
        Driver nearest  = null;
        double minDist  = Double.MAX_VALUE;

        for (Driver d : available) {
            // VehicleType filtresi
            if (requiredType != null && d.getServiceType() != requiredType) continue;

            Location driverLoc = d.getCurrentLocation();
            if (driverLoc == null) {
                // Konum bilinmiyorsa random İstanbul konumu ata (demo)
                driverLoc = new Location(
                    41.0370 + (Math.random() * 0.1 - 0.05),
                    28.9850 + (Math.random() * 0.1 - 0.05)
                );
                d.setCurrentLocation(driverLoc);
            }

            double dist = pickup.distanceTo(driverLoc);
            if (dist < minDist) {
                minDist = dist;
                nearest = d;
            }
        }
        return nearest; // null → no driver available (State Machine: CANCELLED)
    }

    // ── Herhangi VehicleType ile bul (fallback) ───────────────────────────────
    public Driver findNearestDriver(Location pickup) {
        return findNearestDriver(pickup, null);
    }

    // ── SÜRÜCÜ DURUMU GÜNCELLE (State Machine geçişleri) ─────────────────────
    /**
     * State Machine:
     *   IDLE       → driver accepts → ON_THE_WAY
     *   ON_THE_WAY → driver picks up → IN_RIDE
     *   IN_RIDE    → driver completes → IDLE
     */
    public void updateDriverStatus(Driver driver, DriverStatus newStatus) {
        driver.setStatus(newStatus);

        // Availability güncelle
        boolean available = (newStatus == DriverStatus.IDLE);
        driver.setAvailable(available);

        // DB'ye yaz
        driverRepo.updateStatus(driver.getId().toString(), newStatus);
        driverRepo.save(driver);
    }

    // ── SÜRÜCÜ KONUMU GÜNCELLE (WebSocketService simülasyonu) ─────────────────
    /**
     * Sequence Diagram: broadcastLocation(live, driver) → subscribeToDriver(passenger, city)
     * Desktop uygulamada Timer ile çağrılır (gerçek WebSocket yok).
     */
    public void updateDriverLocation(Driver driver, Location newLocation) {
        driver.setCurrentLocation(newLocation);
        // Gerçek uygulamada WebSocket broadcast yapılır
        // Burada in-memory güncelleme yeterli
    }

    // ── ONAY / BAN ────────────────────────────────────────────────────────────
    public void approveDriver(Driver driver) {
        manager.approveDriver(driver);
    }

    public void banDriver(Driver driver) {
        updateDriverStatus(driver, DriverStatus.OFFLINE);
        manager.banDriver(driver);
    }

    public void unbanDriver(Driver driver) {
        manager.unbanDriver(driver);
        updateDriverStatus(driver, DriverStatus.IDLE);
    }

    // ── ETA HESAPLA (Use Case: "Calculate ETA") ───────────────────────────────
    /**
     * State Machine ACCEPTED: "Live location tracking active"
     * Sürücünün pickup'a kalan süresi (dakika).
     */
    public int calculateETA(Driver driver, Location pickup) {
        Location driverLoc = driver.getCurrentLocation();
        if (driverLoc == null) return 5; // default 5 dakika
        Route route = MapService.getRoute(driverLoc, pickup);
        return route.getDurationMinutes();
    }
}
