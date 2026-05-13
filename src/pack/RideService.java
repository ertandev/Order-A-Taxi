package pack;

/**
 * RideService — Component Diagram: RideService
 * Activity Diagram: tam ride akışı (Request → Payment → Matching → Accept → Start → Complete)
 * State Machine: REQUESTED → ACCEPTED → IN_PROGRESS → COMPLETED / CANCELLED
 */
public class RideService {

    private final TaxiManager      manager;
    private final DriverService    driverService;
    private final RideRepository   rideRepo;
    private final PaymentRepository paymentRepo;

    public RideService(TaxiManager manager, DriverService driverService) {
        this.manager       = manager;
        this.driverService = driverService;
        this.rideRepo      = new RideRepository();
        this.paymentRepo   = new PaymentRepository();
    }

    // ── 1. RIDE TALEP ET (Activity Diagram: "Request Ride") ──────────────────
    /**
     * Sequence Diagram adımları:
     *   requestRide(passenger, pickup, dropoff, vehicleType)
     *   → getRoute → calculateFare → processPayment (mock) → createRide
     *   → findNearestDriver → notifyDriver
     */
    public Ride requestRide(Passenger passenger, Location pickup, Location dropoff,
                            VehicleType vehicleType, PaymentMethod paymentMethod) {

        // 2. Rota hesapla (MapService → OpenRouteService veya Haversine fallback)
        Route route = MapService.getRoute(pickup, dropoff);

        // 3. Dinamik fiyat hesapla (PricingService)
        double fare = PricingService.calculateFare(
                route.getDistanceKm(), vehicleType, route.getTrafficFactor());
        int eta = PricingService.estimateMinutes(
                route.getDistanceKm(), route.getTrafficFactor());

        // 4. Ride oluştur (State Machine: REQUESTED)
        String pickupAddr  = pickup  != null ? pickup.toString()  : "Unknown";
        String dropoffAddr = dropoff != null ? dropoff.toString() : "Unknown";

        Ride ride = manager.createRideRequest(passenger, pickupAddr, dropoffAddr,
                "Now", vehicleType.name());

        // Detayları set et
        ride.setPickupLocation(pickup);
        ride.setDropoffLocation(dropoff);
        ride.setDistanceKm(route.getDistanceKm());
        ride.setFareAmount(fare);
        ride.setEstimatedMinutes(eta);
        ride.setVehicleType(vehicleType);

        // 5. Ödeme oluştur (mock — Stripe Phase 3+ için)
        Payment payment = new Payment(fare, paymentMethod);
        String txnId = "TXN-" + (int)(Math.random() * 90000 + 10000);
        payment.markPaid(txnId); // mock: hemen onaylanıyor
        ride.setPayment(payment);
        paymentRepo.save(payment, ride.getId().toString());

        // 6. DB'ye kaydet
        rideRepo.save(ride);

        // Driver assignment is handled by the UI — user picks from the combo box.
        // Ride is returned in REQUESTED state; UI will set driver + call startRide().
        return ride;
    }

    // ── DRIVER ACCEPT (Driver Decision — Sequence Diagram) ─────────────────
    public void acceptRide(Ride ride, Driver driver) {
        ride.setDriver(driver);
        ride.setStatus(RideStatus.ACCEPTED);
        driverService.updateDriverStatus(driver, DriverStatus.ON_THE_WAY);
        rideRepo.updateStatus(ride.getId().toString(), RideStatus.ACCEPTED);
    }

    // ── RIDE BASLAT (State Machine: ACCEPTED -> IN_PROGRESS) ─────────────────
    /**
     * Activity Diagram: Driver "Click Start" → System "Update Ride Status (In Progress)"
     */
    public void startRide(Ride ride) {
        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.setStartTime(java.time.LocalDateTime.now());
        if (ride.getDriver() != null)
            driverService.updateDriverStatus(ride.getDriver(), DriverStatus.IN_RIDE);
        rideRepo.updateStatus(ride.getId().toString(), RideStatus.IN_PROGRESS);
        rideRepo.save(ride); // persist startTime
        System.out.println("[DB] Ride STARTED. Driver: " + 
            (ride.getDriver() != null ? ride.getDriver().getName() : "null") + 
            " | Est. time: " + ride.getEstimatedMinutes() + " min | StartTime: " + ride.getStartTime());
    }

    // ── RIDE TAMAMLA (State Machine: IN_PROGRESS → COMPLETED) ───────────────
    /**
     * Activity Diagram: Driver "Click Complete" → "Update Ride Status (Completed)"
     *   → "Generate Receipt" → "Rate Driver & Leave Comment"
     */
    public void completeRide(Ride ride) {
        manager.completeRide(ride); // COMPLETED + DB update
        if (ride.getDriver() != null) {
            driverService.updateDriverStatus(ride.getDriver(), DriverStatus.IDLE);
            System.out.println("[DB] Ride COMPLETED. Driver '" + ride.getDriver().getName() + "' -> IDLE (available)");
        }
    }

    /**
     * Arka plan Timer tarafından çağrılır.
     * 1) Süresi dolan yolculukları otomatik bitirir.
     * 2) ON_THE_WAY'de takılı kalan sürücüleri temizler.
     */
    public void processAutoCompletions() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        
        // 1. Auto-complete rides whose estimated time has passed
        for (Ride ride : manager.getRides()) {
            if (ride.getStatus() == RideStatus.IN_PROGRESS && ride.getStartTime() != null) {
                long secondsPassed = java.time.Duration.between(ride.getStartTime(), now).toSeconds();
                long estimatedSeconds = ride.getEstimatedMinutes() * 60L;
                if (secondsPassed >= estimatedSeconds) {
                    System.out.println("[RideService] Auto-completing ride: " + ride.getId() + 
                        " (" + secondsPassed + "s passed, est: " + estimatedSeconds + "s)");
                    completeRide(ride);
                }
            }
        }
        
        // 2. Safety net: Reset drivers stuck in ON_THE_WAY or IN_RIDE (no active ride referencing them)
        for (Driver d : manager.getAllDrivers()) {
            if (d.getStatus() == DriverStatus.ON_THE_WAY || d.getStatus() == DriverStatus.IN_RIDE) {
                boolean hasActiveRide = manager.getRides().stream()
                    .anyMatch(r -> r.getDriver() != null 
                        && r.getDriver().getId().equals(d.getId())
                        && (r.getStatus() == RideStatus.ACCEPTED || r.getStatus() == RideStatus.IN_PROGRESS));
                if (!hasActiveRide) {
                    System.out.println("[RideService] Resetting stuck " + d.getStatus() + " driver: " + d.getName());
                    driverService.updateDriverStatus(d, DriverStatus.IDLE);
                }
            }
        }
    }

    // ── RIDE İPTAL ET (State Machine: * → CANCELLED) ─────────────────────────
    /**
     * CancellationPolicy uygular:
     *   REQUESTED → 0% ceza
     *   ACCEPTED  → 25% ceza (para iadesi %75)
     *   IN_PROGRESS → 50% ceza
     * Returns refund amount.
     */
    public double cancelRide(Ride ride) {
        double refund = CancellationPolicy.calculateRefund(ride);
        double penalty = CancellationPolicy.calculatePenalty(ride);

        ride.setStatus(RideStatus.CANCELLED);
        rideRepo.updateStatus(ride.getId().toString(), RideStatus.CANCELLED);

        // Sürücü varsa IDLE'a döndür
        if (ride.getDriver() != null) {
            driverService.updateDriverStatus(ride.getDriver(), DriverStatus.IDLE);
        }

        System.out.printf("[RideService] Ride cancelled. Penalty: %.1f TL | Refund: %.1f TL%n",
                penalty, refund);
        return refund;
    }

    // ── PUANLAMA (Use Case: "Rate Driver") ───────────────────────────────────
    public void rateDriver(Ride ride, int stars, String comment) {
        manager.rateDriver(ride, stars, comment); // in-memory + DB
        if (ride.getDriver() != null) {
            System.out.println("[DB] Rating saved: " + ride.getDriver().getName() + 
                " -> " + stars + "* (avg: " + String.format("%.1f", ride.getDriver().getAverageRating()) + 
                ", count: " + ride.getDriver().getRatingCount() + ")");
        }
    }
}
