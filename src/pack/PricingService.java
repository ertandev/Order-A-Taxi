package pack;

// --- PRICING SERVICE (Class Diagram + Sequence Diagram) ---
// Sequence: calculateFare(9.3) -> $12 shown to passenger
// Formula:  BASE + (km * RATE_PER_KM) * vehicleMultiplier * trafficFactor
public class PricingService {

    private static final double OPENING_FEE     = 65.40;  // TL (Real 2026 Istanbul)
    private static final double RATE_PER_KM     = 28.0;   // TL per km (Estimate)
    private static final double MINIMUM_FARE    = 210.0;  // TL (İndi-Bindi)
    private static final double ECONOMY_MULT    = 1.0;
    private static final double PREMIUM_MULT    = 2.0;
    private static final double XL_MULT         = 1.6;
    private static final int    AVG_CITY_SPEED  = 25;     // km/h (Istanbul traffic)

    /**
     * Main fare calculation (Activity Diagram: "Calculate Expected Fare").
     */
    public static double calculateFare(double distanceKm, VehicleType vehicleType, double trafficFactor) {
        double mult;
        switch (vehicleType) {
            case PREMIUM: mult = PREMIUM_MULT; break;
            case XL:      mult = XL_MULT;      break;
            default:      mult = ECONOMY_MULT; break;
        }
        
        double fare = (OPENING_FEE + distanceKm * RATE_PER_KM) * mult * trafficFactor;
        
        // Apply Minimum Fare (İndi-Bindi) logic
        if (fare < MINIMUM_FARE && vehicleType == VehicleType.ECONOMY) {
            fare = MINIMUM_FARE;
        }
        
        return Math.round(fare * 10.0) / 10.0;
    }

    public static double calculateFare(double distanceKm, VehicleType vehicleType) {
        return calculateFare(distanceKm, vehicleType, 1.0);
    }

    /**
     * Estimates ETA in minutes (State Machine: "Calculate ETA").
     */
    public static int estimateMinutes(double distanceKm, double trafficFactor) {
        double effectiveSpeed = AVG_CITY_SPEED / trafficFactor;
        return (int) Math.ceil((distanceKm / effectiveSpeed) * 60);
    }

    public static String formatFare(double fare) {
        return String.format("%.1f TL", fare);
    }

    public static String formatMinutes(int minutes) {
        return minutes + " min";
    }
}
