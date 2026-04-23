package pack;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * MapService — Component Diagram: MapService → GoogleMapsAPI (ücretsiz alternatif: OpenRouteService)
 * Sequence Diagram: getRoute(pickup,dropoff) → Route(9.3km, 12min) → calculateFare
 *
 * API: OpenRouteService (openrouteservice.org)
 *   - Ücretsiz: 2000 istek/gün
 *   - API key: openrouteservice.org'dan e-posta ile ücretsiz alınır
 *   - Fallback: API key yoksa Haversine (offline) hesaplama kullanılır
 */
public class MapService {

    // openrouteservice.org'dan ücretsiz alınan API key buraya yazılır
    private static String API_KEY = "YOUR_ORS_API_KEY";
    private static final String ORS_URL =
            "https://api.openrouteservice.org/v2/directions/driving-car";

    public static void setApiKey(String key) {
        API_KEY = key;
    }

    public static boolean isApiConfigured() {
        return !API_KEY.equals("YOUR_ORS_API_KEY") && !API_KEY.isBlank();
    }

    // ── ANA METOT: getRoute ───────────────────────────────────────────────────
    /**
     * Sequence Diagram: getRoute(pickup, dropoff) → Route
     * OpenRouteService çağrısı yapar; başarısız olursa Haversine fallback.
     */
    public static Route getRoute(Location from, Location to) {
        return getRoute(from, to, java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY));
    }

    public static Route getRoute(Location from, Location to, int hour) {
        if (from == null || to == null) return fallbackRoute(from, to, hour);
        if (!isApiConfigured())         return fallbackRoute(from, to, hour);

        try {
            // ORS endpoint: ?start=lon,lat&end=lon,lat
            String urlStr = ORS_URL
                    + "?api_key=" + API_KEY
                    + "&start=" + from.longitude + "," + from.latitude
                    + "&end="   + to.longitude   + "," + to.latitude;

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) return fallbackRoute(from, to, hour);

            // JSON okuma (regex ile extract — ek kütüphane gerektirmez)
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }
            String json = sb.toString();

            double distMeters = extractDouble(json, "\"distance\":", ",");
            double durSeconds = extractDouble(json, "\"duration\":", ",");

            if (distMeters <= 0) return fallbackRoute(from, to, hour);

            double distKm = distMeters / 1000.0;
            int    durationMin = (int) Math.ceil(durSeconds / 60.0);
            double trafficFactor = 1.0; // ORS zaten gerçek trafik verir

            Route route = new Route(distKm, durationMin, trafficFactor);
            route.setSummary(String.format("%.1f km via OpenRouteService", distKm));
            return route;

        } catch (Exception e) {
            System.err.println("[MapService] ORS API error: " + e.getMessage() + " — using fallback");
            return fallbackRoute(from, to, hour);
        }
    }

    // ── FALLBACK: Haversine (offline) ─────────────────────────────────────────
    /**
     * API_KEY yokken veya API başarısız olduğunda kullanılır.
     * 1.3x trafik katsayısı uygulanır (şehir içi gerçekçilik).
     */
    public static Route fallbackRoute(Location from, Location to, int hour) {
        if (from == null || to == null) return new Route(5.0, 12, 1.3);
        double distKm = from.distanceTo(to);
        // Time-of-day based traffic simulation using provided hour
        double baseTraffic = 1.1; // Baseline
        
        if (hour >= 8 && hour <= 10) baseTraffic = 1.7; // Morning rush
        else if (hour >= 17 && hour <= 19) baseTraffic = 1.9; // Evening rush
        else if (hour >= 23 || hour <= 5) baseTraffic = 0.9;  // Night (smooth)
        
        // Add a bit of location-based variation to keep it deterministic for the route
        long seed = (long)(from.latitude * 1000) ^ (long)(to.longitude * 1000);
        seed ^= hour; // Use provided hour in seed too
        java.util.Random rnd = new java.util.Random(seed);
        double variation = rnd.nextDouble() * 0.2;
        
        double trafficFactor = baseTraffic + variation;
        
        int durationMin = PricingService.estimateMinutes(distKm, trafficFactor);
        Route route = new Route(distKm, durationMin, trafficFactor);
        route.setSummary(String.format("%.1f km (time-aware estimate)", distKm));
        return route;
    }

    // ── JSON EXTRACT YARDIMCISI ───────────────────────────────────────────────
    private static double extractDouble(String json, String key, String end) {
        try {
            int idx = json.indexOf(key);
            if (idx < 0) return -1;
            int start = idx + key.length();
            int endIdx = json.indexOf(end, start);
            if (endIdx < 0) endIdx = json.indexOf("}", start);
            if (endIdx < 0) return -1;
            return Double.parseDouble(json.substring(start, endIdx).trim());
        } catch (Exception e) {
            return -1;
        }
    }
}
