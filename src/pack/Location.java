package pack;

public class Location {
    public double latitude;
    public double longitude;
    public String address; // Object Diagram: "Taksim Square, Istanbul"

    public Location(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = "";
    }

    public Location(double latitude, double longitude, String address) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
    }

    /**
     * Haversine formula — gerçek dünya km cinsinden mesafe.
     * (OpenRouteService API yokken offline fallback olarak kullanılır)
     */
    public double distanceTo(Location other) {
        final double R = 6371.0; // Earth radius km
        double lat1 = Math.toRadians(this.latitude);
        double lat2 = Math.toRadians(other.latitude);
        double dLat = Math.toRadians(other.latitude - this.latitude);
        double dLon = Math.toRadians(other.longitude - this.longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(lat1) * Math.cos(lat2)
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    @Override
    public String toString() {
        return address.isEmpty()
                ? String.format("%.4f, %.4f", latitude, longitude)
                : address;
    }
}