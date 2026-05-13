package pack;

// --- ROUTE CLASS (Class Diagram) ---
public class Route {
    private double distanceKm;
    private int durationMinutes;
    private double trafficFactor;
    private String summary;

    public Route(double distanceKm, int durationMinutes, double trafficFactor) {
        this.distanceKm = distanceKm;
        this.durationMinutes = durationMinutes;
        this.trafficFactor = trafficFactor;
        this.summary = "";
    }

    public Route(double distanceKm, int durationMinutes) {
        this(distanceKm, durationMinutes, 1.0);
    }

    public double getDistanceKm() { return distanceKm; }
    public int getDurationMinutes() { return durationMinutes; }
    public double getTrafficFactor() { return trafficFactor; }
    public void setTrafficFactor(double tf) { this.trafficFactor = tf; }
    public String getSummary() { return summary; }
    public void setSummary(String s) { this.summary = s; }

    @Override
    public String toString() {
        return String.format("%.1f km, ~%d min (trafik: x%.1f)", distanceKm, durationMinutes, trafficFactor);
    }
}
