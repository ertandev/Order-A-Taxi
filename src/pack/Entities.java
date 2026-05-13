package pack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

// --- ENUMS ---
enum RideStatus {
    REQUESTED, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED
}

enum TicketType {
    LOST_ITEM, FARE_REVIEW, DRIVER_ISSUE, APP_ERROR, VEHICLE_PROBLEM, LATE_ARRIVAL, CLEANLINESS
}

// --- NEW ENUMS (Class Diagram) ---
enum VehicleType {
    ECONOMY, PREMIUM, XL
}

enum PaymentMethod {
    CARD, CASH, WALLET
}

enum DriverStatus {
    IDLE, ON_THE_WAY, IN_RIDE, OFFLINE
}

enum DocumentType {
    LICENSE, CRIMINAL_RECORD, VEHICLE_INSURANCE
}

// --- ABSTRACT PARENT CLASS (OOP: Inheritance & Abstraction) ---
abstract class User {
    protected UUID id = UUID.randomUUID();
    protected String name;
    protected String email;
    protected String password;
    protected String phone;

    // Add inside User class:
    public void setPassword(String newPassword) {
        this.password = newPassword;
    }

    public User(String name, String email, String password, String phone) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public boolean checkPassword(String pass) {
        return this.password.equals(pass);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    // OOP: Polymorphism (Everyone's implementation works differently)
    public abstract String getRole();
}

// --- PASSENGER CLASS ---
class Passenger extends User {
    private List<String> savedAddresses = new ArrayList<>();
    private PaymentMethod preferredPaymentMethod = PaymentMethod.CARD;

    public Passenger(String name, String email, String password, String phone) {
        super(name, email, password, phone);
    }

    public void addSavedAddress(String address) { savedAddresses.add(address); }
    public List<String> getSavedAddresses() { return savedAddresses; }
    public PaymentMethod getPreferredPaymentMethod() { return preferredPaymentMethod; }
    public void setPreferredPaymentMethod(PaymentMethod method) { this.preferredPaymentMethod = method; }

    @Override
    public String getRole() {
        return "Passenger";
    }
}

// --- DRIVER CLASS (Req 1-B & 10) ---
class Driver extends User {
    private String vehicleModel;
    private String vehicleType; // Backward compat (plate or legacy type string)
    private boolean isDocumentVerified;
    private boolean isBanned;
    private boolean isAvailable;
    private double ratingSum = 5.0;
    private int ratingCount = 1;
    private java.io.File licenseDoc;
    private java.io.File criminalDoc;
    // --- New fields (Class Diagram) ---
    private String plateNumber = "";
    private VehicleType serviceType = VehicleType.ECONOMY;
    private DriverStatus status = DriverStatus.IDLE;
    private Location currentLocation;

    // Original constructor (kept for backward compatibility)
    public Driver(String name, String email, String password, String phone, String vehicleModel, String vehicleType) {
        super(name, email, password, phone);
        this.vehicleModel = vehicleModel;
        this.vehicleType = vehicleType;
        this.isDocumentVerified = false;
        this.isBanned = false;
        this.isAvailable = true;
    }

    // New constructor with plateNumber and VehicleType (Class Diagram)
    public Driver(String name, String email, String password, String phone,
                  String vehicleModel, String plateNumber, VehicleType serviceType) {
        super(name, email, password, phone);
        this.vehicleModel = vehicleModel;
        this.plateNumber = plateNumber;
        this.vehicleType = plateNumber; // backward compat
        this.serviceType = serviceType;
        this.isDocumentVerified = false;
        this.isBanned = false;
        this.isAvailable = true;
        this.status = DriverStatus.IDLE;
    }

    // Belge setter/getter
    public void setLicenseDoc(java.io.File file) {
        this.licenseDoc = file;
    }

    public void setCriminalDoc(java.io.File file) {
        this.criminalDoc = file;
    }

    public java.io.File getLicenseDoc() {
        return licenseDoc;
    }

    public java.io.File getCriminalDoc() {
        return criminalDoc;
    }

    public boolean isBanned() {
        return isBanned;
    }

    // OOP: Encapsulation
    public void addRating(int stars) {
        this.ratingSum += stars;
        this.ratingCount++;
    }

    public double getAverageRating() {
        if (ratingCount == 0) return 5.0; // Default new drivers to 5 stars
        return (double) ratingSum / ratingCount;
    }

    public double getRatingSum() { return ratingSum; }
    public void setRatingSum(double sum) { this.ratingSum = sum; }
    public int getRatingCount() { return ratingCount; }
    public void setRatingCount(int count) { this.ratingCount = count; }

    public boolean isApproved() {
        return isDocumentVerified && !isBanned;
    }

    public void setVerified(boolean verified) {
        this.isDocumentVerified = verified;
    }

    public void setBanned(boolean banned) {
        this.isBanned = banned;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        this.isAvailable = available;
    }

    public String getPlateNumber() { return plateNumber; }
    public void setPlateNumber(String plate) { this.plateNumber = plate; }
    public VehicleType getServiceType() { return serviceType; }
    public void setServiceType(VehicleType type) { this.serviceType = type; }
    public DriverStatus getStatus() { return status; }
    public void setStatus(DriverStatus s) { this.status = s; }
    public Location getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(Location loc) { this.currentLocation = loc; }

    public String getVehicleInfo() {
        String plate = plateNumber.isEmpty() ? vehicleType : plateNumber;
        return vehicleModel + " (" + plate + ")";
    }

    @Override
    public String getRole() {
        return "Driver";
    }

    @Override
    public String toString() {
        return name + " | " + getVehicleInfo() + " | Rate: " + String.format("%.1f", getAverageRating());
    }
}

// --- ADMIN CLASS (Req 10) ---
class Admin extends User {
    public Admin(String name, String email, String password) {
        super(name, email, password, "000");
    }

    @Override
    public String getRole() {
        return "Admin";
    }
}

// --- RIDE CLASS (Req 3, 4, 5, 6, 7, 8) ---
class Ride {
    private UUID id = UUID.randomUUID();
    private Passenger passenger;
    private Driver driver;
    private String startLoc;
    private String endLoc;
    private double price;
    private RideStatus status;
    private String scheduledTime;
    private int rating = 0;
    private String feedback = "";
    // --- New fields (Class Diagram + Object Diagram) ---
    private VehicleType vehicleType = VehicleType.ECONOMY;
    private Location pickupLocation;
    private Location dropoffLocation;
    private double distanceKm = 0.0;
    private double fareAmount = 0.0;
    private int estimatedMinutes = 0;
    private LocalDateTime scheduledAt;
    private String passengerComment = "";
    private Payment payment;
    private double tipAmount = 0.0;
    private LocalDateTime startTime;

    public Ride(Passenger p, String start, String end, String time) {
        this.passenger = p;
        this.startLoc = start;
        this.endLoc = end;
        this.scheduledTime = time;
        this.status = RideStatus.REQUESTED;
    }

    public void setDriver(Driver d) {
        this.driver = d;
    }

    public void setPrice(double p) {
        this.price = p;
    }

    public void setStatus(RideStatus s) {
        this.status = s;
    }

    public void setRating(int r, String f) {
        this.rating = r;
        this.feedback = f;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Driver getDriver() { return driver; }
    public Passenger getPassenger() { return passenger; }
    public double getPrice() { return price; }
    public RideStatus getStatus() { return status; }
    public String getStartLoc() { return startLoc; }
    public String getEndLoc() { return endLoc; }
    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType vt) { this.vehicleType = vt; }
    public Location getPickupLocation() { return pickupLocation; }
    public void setPickupLocation(Location l) { this.pickupLocation = l; }
    public Location getDropoffLocation() { return dropoffLocation; }
    public void setDropoffLocation(Location l) { this.dropoffLocation = l; }
    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double d) { this.distanceKm = d; }
    public double getFareAmount() { return fareAmount > 0 ? fareAmount : price; }
    public void setFareAmount(double f) { this.fareAmount = f; this.price = f; }
    public int getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(int m) { this.estimatedMinutes = m; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime dt) { this.scheduledAt = dt; }
    public String getPassengerComment() { return passengerComment; }
    public void setPassengerComment(String c) { this.passengerComment = c; }
    public Payment getPayment() { return payment; }
    public void setPayment(Payment p) { this.payment = p; }
    public int getRating() { return rating; }
    public String getFeedback() { return feedback; }
    public double getTipAmount() { return tipAmount; }
    public void setTipAmount(double t) { this.tipAmount = t; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime st) { this.startTime = st; }

    @Override
    public String toString() {
        return "Ride: " + startLoc + " -> " + endLoc + " (" + status + ") - " + getFareAmount() + " TL";
    }
}

// --- SUPPORT TICKET CLASS (Req 9) ---
class SupportTicket {
    private UUID id = UUID.randomUUID();
    private LocalDateTime createdAt = LocalDateTime.now();
    private User user;
    private Driver driver;
    private TicketType type;
    private String description;
    private boolean isResolved;

    public SupportTicket(User user, Driver driver, TicketType type, String desc) {
        this.user = user;
        this.driver = driver;
        this.type = type;
        this.description = desc;
        this.isResolved = false;
    }

    public UUID getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public User getUser() {
        return user;
    }

    public Driver getDriver() {
        return driver;
    }

    public TicketType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public boolean isResolved() {
        return isResolved;
    }

    public void setResolved(boolean resolved) {
        this.isResolved = resolved;
    }

    @Override
    public String toString() {
        String driverName = driver != null ? driver.getName() : "N/A";
        return "[" + type + "] " + user.getName() + " → " + driverName + ": " + description
                + (isResolved ? " (SOLVED)" : " (OPEN)");
    }
}