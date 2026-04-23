package pack;

import java.util.List;

/**
 * AdminService — Component Diagram: AdminService
 * Use Case: "View System Statistics", "Approve Driver Documents", "Ban/Unban Driver"
 */
public class AdminService {

    private final TaxiManager      manager;
    private final DriverRepository driverRepo;
    private final RideRepository   rideRepo;
    private final TicketRepository ticketRepo;

    public AdminService(TaxiManager manager) {
        this.manager    = manager;
        this.driverRepo = new DriverRepository();
        this.rideRepo   = new RideRepository();
        this.ticketRepo = new TicketRepository();
    }

    // ── SİSTEM İSTATİSTİKLERİ (Use Case: "View System Statistics") ───────────
    /**
     * Class Diagram: AdminService.getSystemStats(): SystemStats
     * Returns a formatted summary string for the Admin panel.
     */
    public String getSystemStats() {
        int totalRides     = rideRepo.countByStatus(RideStatus.COMPLETED)
                           + rideRepo.countByStatus(RideStatus.CANCELLED)
                           + rideRepo.countByStatus(RideStatus.IN_PROGRESS);
        int completedRides = rideRepo.countByStatus(RideStatus.COMPLETED);
        int cancelledRides = rideRepo.countByStatus(RideStatus.CANCELLED);
        int activeRides    = rideRepo.countByStatus(RideStatus.IN_PROGRESS);
        double revenue     = rideRepo.getTotalRevenue();
        int openTickets    = ticketRepo.countOpen();
        int pendingDrivers = manager.getPendingDrivers().size();
        int activeDrivers  = manager.getAvailableDrivers().size();

        return String.format(
            "System Statistics\n\n" +
            "  Total Rides:      %d\n" +
            "  Completed:        %d\n" +
            "  Cancelled:        %d\n" +
            "  Active Now:       %d\n" +
            "  Total Revenue:    %.1f TL\n" +
            "  Active Drivers:   %d\n" +
            "  Pending Approval: %d\n" +
            "  Open Tickets:     %d",
            totalRides, completedRides, cancelledRides, activeRides,
            revenue, activeDrivers, pendingDrivers, openTickets
        );
    }

    // ── SÜRÜCÜ ONAYLA (Use Case: "Approve Driver Documents") ─────────────────
    public void approveDriver(Driver driver) {
        driver.setVerified(true);
        driverRepo.save(driver);
    }

    // ── SÜRÜCÜ YASAKLA / YASAK KALDIR ────────────────────────────────────────
    public void banDriver(Driver driver) {
        manager.banDriver(driver);
    }

    public void unbanDriver(Driver driver) {
        manager.unbanDriver(driver);
    }

    // ── BEKLEYEN SÜRÜCÜLER ───────────────────────────────────────────────────
    public List<Driver> getPendingDrivers() {
        return manager.getPendingDrivers();
    }

    // ── TÜM SÜRÜCÜLER ────────────────────────────────────────────────────────
    public List<Driver> getAllDrivers() {
        return manager.getAllDrivers();
    }

    // ── TÜM BİLETLER ─────────────────────────────────────────────────────────
    public List<SupportTicket> getAllTickets() {
        return manager.getTickets();
    }

    // ── BİLET ÇÖZÜMLE ────────────────────────────────────────────────────────
    public void resolveTicket(SupportTicket ticket) {
        ticket.setResolved(true);
        ticketRepo.resolve(ticket.getId().toString());
    }
}
