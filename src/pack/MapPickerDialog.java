package pack;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.viewer.*;
import org.jxmapviewer.painter.CompoundPainter;
import org.jxmapviewer.painter.Painter;

import org.jxmapviewer.input.PanMouseInputListener;
import javax.swing.event.MouseInputListener;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.*;

/**
 * MapPickerDialog — Google Maps benzeri konum seçici.
 * - OpenStreetMap tile'ları (ücretsiz, API key gerekmez)
 * - IP geolocation ile başlangıç konumu
 * - 1. tıklama: Yeşil marker = Pickup
 * - 2. tıklama: Kırmızı marker = Destination
 * - Nominatim ile adres alma (reverse geocode)
 */
public class MapPickerDialog extends JDialog {

    // Results
    private GeoPosition pickupPos    = null;
    private GeoPosition dropoffPos   = null;
    private String      pickupAddr   = null;
    private String      dropoffAddr  = null;
    private boolean     confirmed    = false;

    // UI
    private JXMapViewer mapViewer;
    private JLabel      lblStatus;
    private JLabel      lblPickup;
    private JLabel      lblDropoff;
    private JButton     btnConfirm;
    private JButton     btnReset;

    // Colors
    private static final Color CLR_BG      = new Color(18, 18, 18);
    private static final Color CLR_CARD    = new Color(28, 28, 30);
    private static final Color CLR_YELLOW  = new Color(255, 209, 0);
    private static final Color CLR_BLACK   = new Color(18, 18, 18);
    private static final Color CLR_TEXT    = new Color(255, 255, 255);
    private static final Color CLR_GRAY    = new Color(152, 152, 157);
    private static final Color CLR_GREEN   = new Color(52, 199, 89);
    private static final Color CLR_RED     = new Color(255, 69, 58);

    // Mode: 0 = waiting pickup, 1 = waiting dropoff, 2 = done
    private int mode = 0;

    // Waypoints
    private final Set<Waypoint> waypoints = new HashSet<>();

    public MapPickerDialog(Frame parent) {
        super(parent, "Pick Locations on Map", true);
        setSize(820, 680);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        getContentPane().setBackground(CLR_BG);

        buildUI();
        initMap();
        autoDetectLocation();
    }

    // ── UI Builder ────────────────────────────────────────────────────────────
    private void buildUI() {
        // ── Top instruction bar ──
        JPanel topBar = new JPanel(new BorderLayout(10, 0));
        topBar.setBackground(CLR_CARD);
        topBar.setBorder(new EmptyBorder(12, 16, 12, 16));

        lblStatus = new JLabel("Click on the map to set your PICKUP location");
        lblStatus.setFont(new Font("Segoe UI Symbol", Font.BOLD, 14));
        lblStatus.setForeground(CLR_YELLOW);
        topBar.add(lblStatus, BorderLayout.CENTER);

        btnReset = new JButton("Reset \u21BB");
        styleBtn(btnReset, CLR_CARD, CLR_YELLOW);
        btnReset.setFont(new Font("Segoe UI Symbol", Font.BOLD, 12));
        btnReset.addActionListener(e -> resetMarkers());
        topBar.add(btnReset, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // ── Map (center) ──
        mapViewer = new JXMapViewer();
        add(mapViewer, BorderLayout.CENTER);

        // ── Bottom info bar ──
        JPanel bottomBar = new JPanel(new BorderLayout(10, 0));
        bottomBar.setBackground(CLR_CARD);
        bottomBar.setBorder(new EmptyBorder(10, 16, 10, 16));

        JPanel coordPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        coordPanel.setBackground(CLR_CARD);

        lblPickup = new JLabel("[P] Pickup:  —");
        lblPickup.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblPickup.setForeground(CLR_GREEN);

        lblDropoff = new JLabel("[D] Destination:  —");
        lblDropoff.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblDropoff.setForeground(CLR_RED);

        coordPanel.add(lblPickup);
        coordPanel.add(lblDropoff);
        bottomBar.add(coordPanel, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setBackground(CLR_CARD);

        JButton btnCancel = new JButton("Cancel");
        styleBtn(btnCancel, CLR_CARD, CLR_GRAY);
        btnCancel.addActionListener(e -> dispose());

        btnConfirm = new JButton("Confirm Locations \u2713");
        btnConfirm.setFont(new Font("Segoe UI Symbol", Font.BOLD, 14));
        styleBtn(btnConfirm, CLR_YELLOW, CLR_BLACK);
        btnConfirm.setEnabled(false);
        btnConfirm.addActionListener(e -> {
            confirmed = true;
            dispose();
        });

        btnRow.add(btnCancel);
        btnRow.add(btnConfirm);
        bottomBar.add(btnRow, BorderLayout.EAST);
        add(bottomBar, BorderLayout.SOUTH);
    }

    // ── Map Init ──────────────────────────────────────────────────────────────
    private void initMap() {
        // OpenStreetMap kuralları: Bot olmadığımızı belli et ve HTTPS kullan
        System.setProperty("http.agent", "OrderAtaxiApp/1.0");
        TileFactoryInfo info = new OSMTileFactoryInfo("OpenStreetMap", "https://tile.openstreetmap.org");
        DefaultTileFactory tileFactory = new DefaultTileFactory(info);
        tileFactory.setThreadPoolSize(4);
        mapViewer.setTileFactory(tileFactory);

        // Custom Painter to mask areas outside Turkey
        Painter<JXMapViewer> turkeyMask = (g, map, width, height) -> {
            Graphics2D g2 = (Graphics2D) g.create();
            Rectangle rect = map.getViewportBounds();
            g2.translate(-rect.x, -rect.y);

            // Turkey boundaries in pixels
            Point2D sw = map.getTileFactory().geoToPixel(new GeoPosition(35.5, 25.5), map.getZoom());
            Point2D ne = map.getTileFactory().geoToPixel(new GeoPosition(42.5, 45.0), map.getZoom());

            int x = (int) sw.getX();
            int y = (int) ne.getY();
            int w = (int) (ne.getX() - sw.getX());
            int h = (int) (sw.getY() - ne.getY());

            if (w <= 0 || h <= 0) {
                g2.dispose();
                return;
            }

            // Darken everything outside
            Area outside = new Area(new Rectangle2D.Double(rect.x, rect.y, rect.width, rect.height));
            Area inside = new Area(new Rectangle2D.Double(x, y, w, h));
            outside.subtract(inside);

            g2.setColor(new Color(0, 0, 0, 180)); // 70% dark mask
            g2.fill(outside);
            
            // Draw border
            g2.setColor(CLR_YELLOW);
            g2.setStroke(new BasicStroke(3));
            g2.drawRect(x, y, w, h);
            
            g2.dispose();
        };

        mapViewer.setOverlayPainter(turkeyMask);
        
        // Default center: Istanbul
        GeoPosition istanbul = new GeoPosition(41.0082, 28.9784);
        mapViewer.setZoom(5);
        mapViewer.setAddressLocation(istanbul);



        // Click listener
        mapViewer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (mode == 2) return; // done

                // JXMapViewer returns (lon, lat) as (x, y) — need to convert properly
                GeoPosition clicked = mapViewer.convertPointToGeoPosition(e.getPoint());

                if (!isInsideTurkey(clicked)) {
                    lblStatus.setText("\u26A0 Please select a location WITHIN Turkey!");
                    lblStatus.setForeground(CLR_RED);
                    return;
                }

                if (mode == 0) {
                    // Set pickup
                    pickupPos = clicked;
                    addMarker(clicked, true);
                    lblPickup.setText("[P] Pickup:  Loading address...");
                    lblStatus.setText("Now click to set DESTINATION");
                    lblStatus.setForeground(CLR_RED);
                    mode = 1;

                    // Reverse geocode in background
                    new Thread(() -> {
                        String addr = reverseGeocode(clicked.getLatitude(), clicked.getLongitude());
                        pickupAddr = addr;
                        SwingUtilities.invokeLater(() -> {
                            lblPickup.setText("[P] Pickup:  " + addr);
                            checkConfirmEnable();
                        });
                    }).start();

                } else if (mode == 1) {
                    // Set destination
                    dropoffPos = clicked;
                    addMarker(clicked, false);
                    lblDropoff.setText("[D] Destination:  Loading address...");
                    lblStatus.setText("Loading addresses, please wait...");
                    lblStatus.setForeground(CLR_YELLOW);
                    mode = 2;
                    // Disable confirm until addresses are loaded
                    btnConfirm.setEnabled(false);

                    // Reverse geocode in background
                    new Thread(() -> {
                        String addr = reverseGeocode(clicked.getLatitude(), clicked.getLongitude());
                        dropoffAddr = addr;
                        SwingUtilities.invokeLater(() -> {
                            lblDropoff.setText("[D] Destination:  " + addr);
                            checkConfirmEnable();
                        });
                    }).start();
                }
            }
        });

        // Tutup kaydırma (Drag to Pan) desteği
        MouseInputListener mia = new PanMouseInputListener(mapViewer);
        mapViewer.addMouseListener(mia);
        mapViewer.addMouseMotionListener(mia);

        // Tekerlek ile yakınlaştırma (Zoom) desteği
        mapViewer.addMouseWheelListener(e -> {
            int zoom = mapViewer.getZoom();
            if (e.getWheelRotation() > 0) {
                // Limit zoom out to 9 (Turkey scale)
                mapViewer.setZoom(Math.min(zoom + 1, 9));
            } else {
                mapViewer.setZoom(Math.max(zoom - 1, 1));
            }
        });
    }

    private void checkConfirmEnable() {
        if (pickupAddr != null && dropoffAddr != null) {
            btnConfirm.setEnabled(true);
            lblStatus.setText("Both locations set — click Confirm!");
            lblStatus.setForeground(CLR_GREEN);
        }
    }

    // ── Marker Drawing ────────────────────────────────────────────────────────
    private void addMarker(GeoPosition pos, boolean isPickup) {
        final Color markerColor = isPickup ? CLR_GREEN : CLR_RED;
        final String label = isPickup ? "P" : "D";

        Waypoint wp = new DefaultWaypoint(pos);
        waypoints.add(wp);

        WaypointPainter<Waypoint> painter = new WaypointPainter<Waypoint>() {
            @Override
            protected void doPaint(Graphics2D g, JXMapViewer map,
                                   int width, int height) {
                for (Waypoint w : getWaypoints()) {
                    Point2D pt = map.getTileFactory().geoToPixel(
                            w.getPosition(), map.getZoom());
                    Rectangle bounds = map.getViewportBounds();
                    int x = (int)(pt.getX() - bounds.getX());
                    int y = (int)(pt.getY() - bounds.getY());

                    // Determine color from label stored in GeoPosition comparison
                    Color c = w.getPosition().equals(pickupPos) ? CLR_GREEN : CLR_RED;
                    String lbl = w.getPosition().equals(pickupPos) ? "P" : "D";

                    // Shadow
                    g.setColor(new Color(0, 0, 0, 80));
                    g.fillOval(x - 9, y - 9 + 22, 18, 6);

                    // Pin body
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(c);
                    g.fillOval(x - 12, y - 32, 24, 24);

                    // Pin tail
                    int[] xp = {x - 5, x + 5, x};
                    int[] yp = {y - 10, y - 10, y + 2};
                    g.fillPolygon(xp, yp, 3);

                    // Inner circle
                    g.setColor(Color.WHITE);
                    g.fillOval(x - 6, y - 26, 12, 12);

                    // Label
                    g.setColor(c);
                    g.setFont(new Font("Segoe UI", Font.BOLD, 9));
                    g.drawString(lbl, x - 3, y - 17);
                }
            }
        };
        painter.setWaypoints(new HashSet<>(waypoints));
        mapViewer.setOverlayPainter(painter);
        mapViewer.repaint();
    }

    private void resetMarkers() {
        waypoints.clear();
        pickupPos = null; dropoffPos = null;
        pickupAddr = null; dropoffAddr = null;
        mode = 0;
        mapViewer.setOverlayPainter(null);
        mapViewer.repaint();
        lblPickup.setText("[P] Pickup:  —");
        lblDropoff.setText("[D] Destination:  —");
        lblStatus.setText("Click on the map to set your PICKUP location");
        lblStatus.setForeground(CLR_YELLOW);
        btnConfirm.setEnabled(false);
    }

    private boolean isInsideTurkey(GeoPosition p) {
        double lat = p.getLatitude();
        double lon = p.getLongitude();
        // Approximate Turkey bounding box (36-42N, 26-45E)
        return (lat >= 35.8 && lat <= 42.1) && (lon >= 25.6 && lon <= 44.8);
    }

    // ── IP Auto-Location ──────────────────────────────────────────────────────
    private void autoDetectLocation() {
        new Thread(() -> {
            try {
                URL url = new URL("http://ip-api.com/json");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                String json = sb.toString();
                double lat = parseDouble(json, "lat");
                double lon = parseDouble(json, "lon");

                if (lat != 0 && lon != 0) {
                    GeoPosition pos = new GeoPosition(lat, lon);
                    SwingUtilities.invokeLater(() -> {
                        mapViewer.setAddressLocation(pos);
                        mapViewer.setZoom(4);
                    });
                }
            } catch (Exception ignored) {
                // Fallback to Istanbul — already set
            }
        }).start();
    }

    // ── Nominatim Reverse Geocode ─────────────────────────────────────────────
    private String reverseGeocode(double lat, double lon) {
        try {
            String urlStr = String.format(
                    "https://nominatim.openstreetmap.org/reverse?lat=%.6f&lon=%.6f&format=json&accept-language=tr",
                    lat, lon);
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "OrderAtaxiApp/1.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();

            String json = sb.toString();
            // Try to get "display_name" or a shorter version
            String displayName = parseString(json, "display_name");
            if (displayName.length() > 60)
                displayName = displayName.substring(0, 57) + "...";
            return displayName;

        } catch (Exception e) {
            return String.format("%.4f, %.4f", lat, lon);
        }
    }

    // ── JSON Helpers (no external lib) ───────────────────────────────────────
    private double parseDouble(String json, String key) {
        try {
            int idx = json.indexOf("\"" + key + "\"");
            if (idx < 0) return 0;
            int colon = json.indexOf(":", idx);
            int end = json.indexOf(",", colon);
            if (end < 0) end = json.indexOf("}", colon);
            return Double.parseDouble(json.substring(colon + 1, end).trim());
        } catch (Exception e) { return 0; }
    }

    private String parseString(String json, String key) {
        try {
            int idx = json.indexOf("\"" + key + "\"");
            if (idx < 0) return "";
            int colon = json.indexOf(":", idx);
            int start = json.indexOf("\"", colon + 1);
            int end   = json.indexOf("\"", start + 1);
            return json.substring(start + 1, end);
        } catch (Exception e) { return ""; }
    }

    // ── Style Helper ─────────────────────────────────────────────────────────
    private void styleBtn(JButton btn, Color bg, Color fg) {
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(new Font("Segoe UI Symbol", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 20, 8, 20));
    }

    // ── Result Getters ────────────────────────────────────────────────────────
    public boolean isConfirmed()     { return confirmed; }
    public String  getPickupAddress()  { return pickupAddr  != null ? pickupAddr  : ""; }
    public String  getDropoffAddress() { return dropoffAddr != null ? dropoffAddr : ""; }
    public GeoPosition getPickupPos()  { return pickupPos; }
    public GeoPosition getDropoffPos() { return dropoffPos; }
}
