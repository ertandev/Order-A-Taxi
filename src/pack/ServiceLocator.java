package pack;

/**
 * ServiceLocator — Tüm servislerin tek noktadan erişildiği singleton.
 * TaxiFinalApp, manager yerine buradan servislere erişir.
 *
 * Kullanım:
 *   ServiceLocator sl = ServiceLocator.getInstance();
 *   sl.getRideService().requestRide(...)
 *   sl.getAuthService().sendOTP(email)
 */
public class ServiceLocator {

    private static ServiceLocator instance;

    private final TaxiManager   manager;
    private final AuthService   authService;
    private final DriverService driverService;
    private final RideService   rideService;
    private final AdminService  adminService;

    private ServiceLocator() {
        this.manager       = new TaxiManager();
        this.driverService = new DriverService(manager);
        this.rideService   = new RideService(manager, driverService);
        this.authService   = new AuthService(manager);
        this.adminService  = new AdminService(manager);
    }

    public static synchronized ServiceLocator getInstance() {
        if (instance == null) instance = new ServiceLocator();
        return instance;
    }

    public TaxiManager   getManager()       { return manager; }
    public AuthService   getAuthService()   { return authService; }
    public DriverService getDriverService() { return driverService; }
    public RideService   getRideService()   { return rideService; }
    public AdminService  getAdminService()  { return adminService; }
}
