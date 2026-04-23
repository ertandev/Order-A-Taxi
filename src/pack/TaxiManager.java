package pack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TaxiManager {
    // In-memory lists (UI katmanı bunları kullanmaya devam eder)
    private List<Passenger> passengers = new ArrayList<>();
    private List<Driver>    drivers    = new ArrayList<>();
    private List<Admin>     admins     = new ArrayList<>();
    private List<Ride>      rides      = new ArrayList<>();
    private List<SupportTicket> tickets = new ArrayList<>();

    // Repository layer (Phase 2 — SQLite persistence)
    private final UserRepository    userRepo;
    private final DriverRepository  driverRepo;
    private final RideRepository    rideRepo;
    private final PaymentRepository paymentRepo;
    private final TicketRepository  ticketRepo;

    public TaxiManager() {
        // Initialize SQLite database + repositories
        DatabaseManager.getInstance();
        userRepo   = new UserRepository();
        driverRepo = new DriverRepository();
        rideRepo   = new RideRepository();
        paymentRepo = new PaymentRepository();
        ticketRepo  = new TicketRepository();

        // --- LOAD DATA FROM DB ---
        this.drivers = driverRepo.findAll();
        this.passengers = userRepo.findAll().stream()
                .filter(u -> u instanceof Passenger)
                .map(u -> (Passenger)u)
                .collect(Collectors.toList());
        this.rides = rideRepo.findAll();
        this.tickets = ticketRepo.findAll();

        // If DB is empty, generate initial data
        if (this.drivers.isEmpty()) {
            generateDummyDrivers();
            // Save dummy drivers to DB so they persist
            for (Driver d : drivers) {
                driverRepo.save(d);
            }
        }
        
        if (this.passengers.isEmpty()) {
            Passenger p = new Passenger("Test Passenger", "test", "123", "555-0000");
            passengers.add(p);
            userRepo.save(p);
        }

        if (admins.isEmpty()) {
            admins.add(new Admin("System Admin", "admin", "123"));
        }

        // Add a specific driver for the user (driver/123)
        boolean hasDriver = drivers.stream().anyMatch(d -> d.getEmail().equals("driver"));
        if (!hasDriver) {
            Driver d = new Driver("Kral Sürücü", "driver", "123", "555-9999", "Mercedes S-Class", "34 KRAL 01", VehicleType.PREMIUM);
            d.setVerified(true);
            drivers.add(d);
            driverRepo.save(d);
        }
    }

    // --- METHOD TO ADD 10 READY DRIVERS ---
    private void generateDummyDrivers() {
        // { name, email, vehicleModel, plateNumber, VehicleType, ratingBase }
        Object[][] dummyData = {
                { "Ahmet Yılmaz",  "ahmet.yilmaz@gmail.com",   "Fiat Egea",           "34 TKS 01", VehicleType.ECONOMY, 4 },
                { "Mehmet Demir",  "mehmet.demir@outlook.com",  "Renault Megane",      "34 TKS 02", VehicleType.ECONOMY, 3 },
                { "Ayşe Çelik",   "ayse.celik@gmail.com",      "Toyota Corolla",      "34 TKS 03", VehicleType.ECONOMY, 4 },
                { "Fatma Kaya",   "fatma.kaya@outlook.com",    "VW Passat",           "34 VIP 01", VehicleType.PREMIUM, 5 },
                { "Mustafa Koç",  "mustafa.koc@gmail.com",     "Ford Focus",          "34 TKS 05", VehicleType.ECONOMY, 2 },
                { "Zeynep Işık",  "zeynep.isik@outlook.com",   "Mercedes Vito",       "34 XL 88",  VehicleType.XL,      4 },
                { "Can Yıldız",   "can.yildiz@gmail.com",      "Hyundai i20",         "34 TKS 07", VehicleType.ECONOMY, 3 },
                { "Elif Polat",   "elif.polat@outlook.com",    "Honda Civic",         "34 TKS 08", VehicleType.ECONOMY, 2 },
                { "Burak Sahin",  "burak.sahin@gmail.com",     "BMW 320i",            "34 VIP 99", VehicleType.PREMIUM, 5 },
                { "Cemre Aydın",  "cemre.aydin@outlook.com",   "Skoda Octavia",       "34 TKS 10", VehicleType.ECONOMY, 3 }
        };

        for (Object[] data : dummyData) {
            String name        = (String) data[0];
            String email       = (String) data[1];
            String car         = (String) data[2];
            String plate       = (String) data[3];
            VehicleType vtype  = (VehicleType) data[4];

            Driver d = new Driver(name, email, "123", "555-100" + (int)(Math.random()*100), car, plate, vtype);
            d.setVerified(true);
            
            // Randomize ratings (between 3.5 and 5.0 typically)
            int randomVotes = 5 + (int)(Math.random() * 15); // 5 to 20 votes
            double randomAvg = 3.5 + (Math.random() * 1.5); // Average between 3.5 and 5.0
            
            d.setRatingSum(randomAvg * randomVotes);
            d.setRatingCount(randomVotes);
            
            drivers.add(d);
        }
    }

    // --- AUTHENTICATION ---
    public User login(String email, String password) {
        for (Admin a : admins)
            if (a.getEmail().equals(email) && a.checkPassword(password)) return a;
        for (Passenger p : passengers)
            if (p.getEmail().equals(email) && p.checkPassword(password)) return p;
        for (Driver d : drivers)
            if (d.getEmail().equals(email) && d.checkPassword(password)) return d;
        return null;
    }

    public void registerPassenger(Passenger p) {
        passengers.add(p);
        userRepo.save(p); // persist to DB
    }

    public void registerDriver(Driver d) {
        drivers.add(d);
        driverRepo.save(d); // persist to DB
    }

    // --- DRIVER METHODS ---
    public List<Driver> getAvailableDrivers() {
        return drivers.stream()
                .filter(d -> d.isApproved() && d.isAvailable())
                .collect(Collectors.toList());
    }

    // --- RIDE METHODS ---
    public Ride createRideRequest(Passenger p, String start, String end, String time, String vehicleTypeName) {
        Ride ride = new Ride(p, start, end, time);

        // Map legacy string to VehicleType enum
        VehicleType vtype = VehicleType.ECONOMY;
        if (vehicleTypeName != null) {
            String v = vehicleTypeName.toUpperCase();
            if (v.contains("PREMIUM")) vtype = VehicleType.PREMIUM;
            else if (v.contains("XL"))  vtype = VehicleType.XL;
        }
        ride.setVehicleType(vtype);

        // Calculate distance using Haversine (fallback — Phase 3 will use OpenRouteService)
        Location from = getCoordinatesForManager(start);
        Location to   = getCoordinatesForManager(end);
        double distKm = (from != null && to != null) ? from.distanceTo(to) : 5.0;
        double traffic = 1.0 + (Math.random() * 0.4); // 1.0 – 1.4x simulated traffic

        double fare = PricingService.calculateFare(distKm, vtype, traffic);
        int    eta  = PricingService.estimateMinutes(distKm, traffic);

        ride.setPickupLocation(from);
        ride.setDropoffLocation(to);
        ride.setDistanceKm(distKm);
        ride.setFareAmount(fare);
        ride.setEstimatedMinutes(eta);

        rides.add(ride);
        return ride;
    }

    // Coordinate helper (mirrors TaxiFinalApp.getCoordinates — keep in sync)
    private Location getCoordinatesForManager(String placeName) {
        if (placeName == null) return new Location(41.0370, 28.9850);
        String t = placeName.toLowerCase().trim();
        if (t.contains("taksim"))   return new Location(41.0370, 28.9850, "Taksim");
        if (t.contains("besiktas")) return new Location(41.0422, 29.0060, "Beşiktaş");
        if (t.contains("kadikoy"))  return new Location(40.9910, 29.0254, "Kadıköy");
        if (t.contains("sabiha"))   return new Location(40.8986, 29.3092, "Sabiha Gökçen");
        if (t.contains("ist") || t.contains("istanbul airport"))
                                    return new Location(41.2811, 28.7519, "İstanbul Havalimanı");
        if (t.contains("galata"))   return new Location(41.0256, 28.9741, "Galata");
        if (t.contains("maslak"))   return new Location(41.1122, 29.0201, "Maslak");
        if (t.contains("levent"))   return new Location(41.0760, 29.0130, "Levent");
        return new Location(41.0370 + Math.random() * 0.05, 28.9850 + Math.random() * 0.05);
    }

    public void completeRide(Ride ride) {
        ride.setStatus(RideStatus.COMPLETED);
        rideRepo.updateStatus(ride.getId().toString(), RideStatus.COMPLETED); // persist
    }

    // --- RATING ---
    public void rateDriver(Ride ride, int stars, String comment) {
        ride.setRating(stars, comment);
        if (ride.getDriver() != null) {
            ride.getDriver().addRating(stars);
            driverRepo.save(ride.getDriver()); // Persist updated ratingSum and ratingCount
        }
        rideRepo.saveRating(ride.getId().toString(), stars, comment); // persist
    }

    // --- SUPPORT ---
    public void createTicket(User u, Driver driver, TicketType type, String desc) {
        SupportTicket t = new SupportTicket(u, driver, type, desc);
        tickets.add(t);
        ticketRepo.save(t); // persist
    }

    // --- ADMIN METHODS ---
    public List<Driver> getPendingDrivers() {
        return drivers.stream()
                .filter(d -> !d.isApproved() && !d.isBanned())
                .collect(Collectors.toList());
    }

    public List<Driver> getAllDrivers() {
        return drivers;
    }

    public void approveDriver(Driver d) {
        d.setVerified(true);
        driverRepo.save(d); // persist
    }

    public void banDriver(Driver d) {
        d.setBanned(true);
        driverRepo.save(d); // persist
    }

    public void unbanDriver(Driver d) {
        d.setBanned(false);
        driverRepo.save(d); // persist
    }

    public List<SupportTicket> getTickets() { return tickets; }
    public List<Ride>          getRides()   { return rides;   }
    public List<Passenger>     getPassengers() { return passengers; }

    // --- PASSWORD RESET ---
    public boolean resetPassword(String email, String newPass) {
        for (Passenger p : passengers)
            if (p.getEmail().equals(email)) {
                p.setPassword(newPass);
                return true;
            }
        for (Driver d : drivers)
            if (d.getEmail().equals(email)) {
                d.setPassword(newPass);
                return true;
            }
        for (Admin a : admins)
            if (a.getEmail().equals(email)) {
                a.setPassword(newPass);
                return true;
            }
        return false;
    }
}