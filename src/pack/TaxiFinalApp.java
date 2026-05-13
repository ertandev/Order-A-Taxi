package pack;

import java.util.Calendar;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.Date;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Taskbar;

// --- LIBRARY IMPORT ---
import com.toedter.calendar.JDateChooser;

public class TaxiFinalApp extends JFrame {

    // Backend â€” Phase 3 Service Layer
    private final ServiceLocator sl = ServiceLocator.getInstance();
    private final TaxiManager manager = sl.getManager();
    private final AuthService authService = sl.getAuthService();
    private final RideService rideService = sl.getRideService();
    private final DriverService driverService = sl.getDriverService();
    private final AdminService adminService = sl.getAdminService();
    private User currentUser;
    private Ride currentRide;
    private JLabel lblPassengerWelcome;

    // UI Navigation
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);
    private CardLayout wizardLayout = new CardLayout();
    private JPanel wizardPanel = new JPanel(wizardLayout);

    // TAXI DARK THEME COLORS
    private final Color CLR_TAXI_YELLOW = new Color(255, 209, 0); // Vibrant taxi yellow #FFD100
    private final Color CLR_TAXI_GOLD = new Color(255, 193, 7); // Gold accent #FFC107
    private final Color CLR_TAXI_BLACK = new Color(18, 18, 18); // Pure dark #121212
    private final Color CLR_DARK_BG = new Color(12, 12, 12); // Deep background #0C0C0C
    private final Color CLR_CARD_BG = new Color(28, 28, 30); // Card surface #1C1C1E
    private final Color CLR_BG = new Color(18, 18, 18); // Main dark background
    private final Color CLR_STRAW = new Color(255, 209, 0); // Compatibility
    private final Color CLR_BLACK = new Color(12, 12, 12); // Compatibility
    private final Color CLR_DARK_GRAY = new Color(72, 72, 74); // Subtle gray #48484A
    private final Color CLR_BORDER = new Color(56, 56, 58); // Border color #38383A
    private final Color CLR_TEXT_PRIMARY = new Color(255, 255, 255); // White text
    private final Color CLR_TEXT_SECONDARY = new Color(152, 152, 157); // Gray text #98989D
    private final Color CLR_SUCCESS = new Color(52, 199, 89); // iOS green #34C759
    private final Color CLR_ERROR = new Color(255, 69, 58); // iOS red #FF453A

    // UNIFIED FONT SYSTEM (Inter/Segoe UI - San Francisco-like)
    private static final String FONT_FAMILY = "Segoe UI";
    private final Font FONT_TITLE = new Font(FONT_FAMILY, Font.BOLD, 28);
    private final Font FONT_SUBTITLE = new Font(FONT_FAMILY, Font.BOLD, 20);
    private final Font FONT_NORMAL = new Font(FONT_FAMILY, Font.BOLD, 14);
    private final Font FONT_BUTTON = new Font(FONT_FAMILY, Font.BOLD, 15);
    private final Font FONT_SMALL = new Font(FONT_FAMILY, Font.BOLD, 12);
    private final Font FONT_EMOJI = new Font("Segoe UI Emoji", Font.PLAIN, 14);

    // CLASS LEVEL VARIABLES
    private File selectedDriverDoc = null;
    private File selectedCriminalDoc = null;

    // Password reset code
    private String generatedResetCode = "";

    public TaxiFinalApp() {
        setTitle("Order A taxi");
        setSize(450, 876);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // App Icon & macOS Dock Icon
        try {
            File logoFile = new File("resources/logo.png");
            if (!logoFile.exists()) logoFile = new File("logo.png");
            if (logoFile.exists()) {
                BufferedImage bimg = ImageIO.read(logoFile);
                setIconImage(bimg);
                
                // Set Dock icon for macOS
                if (Taskbar.isTaskbarSupported()) {
                    Taskbar taskbar = Taskbar.getTaskbar();
                    if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                        taskbar.setIconImage(bimg);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Could not load app icon: " + e.getMessage());
        }

        // OpenRouteService API â€” gerÃ§ek yol mesafesi + ETA
        MapService.setApiKey(
                "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6ImM2ZmZlZTUwNzk5MzQ0OTdhZWU4NDA2ZWJhMGU1MzdiIiwiaCI6Im11cm11cjY0In0=");

        // Add Panels
        mainPanel.add(createLoginPanel(), "LOGIN");
        mainPanel.add(createRegisterPanel(), "REGISTER");
        mainPanel.add(createForgotPasswordPanel(), "FORGOT_PASS");
        mainPanel.add(createPassengerHome(), "PASSENGER_HOME");
        mainPanel.add(createDriverHome(), "DRIVER_HOME");
        mainPanel.add(createAdminPanel(), "ADMIN_PANEL");
        mainPanel.add(createRidePanel(), "RIDE_PANEL");
        mainPanel.add(createPaymentPanel(), "PAYMENT_PANEL");
        mainPanel.add(createRatingPanel(), "RATING_PANEL");
        mainPanel.add(createSupportPanel(), "SUPPORT_PANEL");
        mainPanel.add(createRideHistoryPanel(), "RIDE_HISTORY");
        mainPanel.add(createEarningsHistoryPanel(), "EARNINGS_HISTORY");

        add(mainPanel);
        cardLayout.show(mainPanel, "LOGIN");

        // --- BACKGROUND AUTOMATION ---
        // Every 5 seconds, check for rides that should be auto-completed
        new javax.swing.Timer(5000, e -> {
            rideService.processAutoCompletions();
            // If current view is passenger home or admin panel, they might need refresh
            // But usually the comboDrivers check price button handles it.
        }).start();

        // Startup Cleanup: Finish rides that should have finished while app was closed
        SwingUtilities.invokeLater(() -> rideService.processAutoCompletions());
    }

    private String[] generateTimeSlots(boolean isToday) {
        Vector<String> slots = new Vector<>();
        Calendar now = Calendar.getInstance();

        if (isToday) {
            // Round to next 15 minute slot from current time
            int currentMinute = now.get(Calendar.MINUTE);
            int currentHour = now.get(Calendar.HOUR_OF_DAY);

            int nextSlotMinute = ((currentMinute / 15) + 1) * 15;
            if (nextSlotMinute >= 60) {
                nextSlotMinute = 0;
                currentHour++;
            }

            if (currentHour >= 24) {
                return new String[0];
            }

            for (int h = currentHour; h < 24; h++) {
                int startMinute = (h == currentHour) ? nextSlotMinute : 0;
                for (int m = startMinute; m < 60; m += 15) {
                    slots.add(String.format("%02d:%02d", h, m));
                }
            }
        } else {
            // Full day slots for tomorrow
            for (int h = 0; h < 24; h++) {
                for (int m = 0; m < 60; m += 15) {
                    slots.add(String.format("%02d:%02d", h, m));
                }
            }
        }
        return slots.toArray(new String[0]);
    }

    // --- HELPER: COORDINATE FINDER ---
    private Location getCoordinates(String placeName) {
        String text = placeName.toLowerCase().trim();
        if (text.matches(".*[0-9]+.*,.*[0-9]+.*")) {
            try {
                String rawNums = text.replaceAll("[^0-9.,-]", "");
                String[] parts = rawNums.split(",");
                if (parts.length >= 2)
                    return new Location(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
            } catch (Exception e) {
            }
        }
        if (text.contains("taksim"))
            return new Location(41.0370, 28.9850);
        if (text.contains("besiktas"))
            return new Location(41.0422, 29.0060);
        if (text.contains("kadikoy"))
            return new Location(40.9910, 29.0254);
        if (text.contains("sabiha") || text.contains("saw"))
            return new Location(40.8986, 29.3092);
        if (text.contains("istanbul airport") || text.contains("ist"))
            return new Location(41.2811, 28.7519);
        if (text.contains("galata"))
            return new Location(41.0256, 28.9741);
        if (text.contains("maslak"))
            return new Location(41.1122, 29.0201);
        if (text.contains("levent"))
            return new Location(41.0760, 29.0130);
        if (text.contains("zorlu"))
            return new Location(41.0660, 29.0170);
        return new Location(41.0370 + (Math.random() * 0.05), 28.9850 + (Math.random() * 0.05));
    }

    // --- UI HELPER METHODS ---
    private JButton createModernButton(String text, Color bg, Color textColor) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        btn.setFont(FONT_BUTTON);
        btn.setBackground(bg);
        btn.setForeground(textColor);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setBorder(new EmptyBorder(16, 32, 16, 32));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(350, 54));

        // Smooth hover animation
        final Color originalBg = bg;
        final Color hoverBg = bg.equals(CLR_TAXI_YELLOW) ? CLR_TAXI_GOLD : bg.brighter();

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                animateColor(btn, originalBg, hoverBg, 150);
            }

            public void mouseExited(java.awt.event.MouseEvent e) {
                animateColor(btn, hoverBg, originalBg, 150);
            }

            public void mousePressed(java.awt.event.MouseEvent e) {
                btn.setBackground(originalBg.darker());
            }

            public void mouseReleased(java.awt.event.MouseEvent e) {
                btn.setBackground(hoverBg);
            }
        });
        return btn;
    }

    // Smooth color transition animation
    private void animateColor(JButton btn, Color from, Color to, int duration) {
        final int steps = 10;
        final int delay = duration / steps;
        Timer timer = new Timer(delay, null);
        final int[] step = { 0 };
        timer.addActionListener(e -> {
            step[0]++;
            float ratio = (float) step[0] / steps;
            int r = (int) (from.getRed() + (to.getRed() - from.getRed()) * ratio);
            int g = (int) (from.getGreen() + (to.getGreen() - from.getGreen()) * ratio);
            int b = (int) (from.getBlue() + (to.getBlue() - from.getBlue()) * ratio);
            btn.setBackground(new Color(r, g, b));
            if (step[0] >= steps) {
                timer.stop();
                btn.setBackground(to);
            }
        });
        timer.start();
    }

    private JTextField createStyledField() {
        JTextField tf = new JTextField();
        tf.setFont(FONT_NORMAL);
        tf.setBackground(CLR_CARD_BG);
        tf.setForeground(CLR_TEXT_PRIMARY);
        tf.setCaretColor(CLR_TAXI_YELLOW);
        tf.setBorder(new CompoundBorder(
                new LineBorder(CLR_BORDER, 1, true),
                new EmptyBorder(14, 16, 14, 16)));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

        // Focus animation
        tf.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                tf.setBorder(new CompoundBorder(
                        new LineBorder(CLR_TAXI_YELLOW, 2, true),
                        new EmptyBorder(13, 15, 13, 15)));
            }

            public void focusLost(java.awt.event.FocusEvent e) {
                tf.setBorder(new CompoundBorder(
                        new LineBorder(CLR_BORDER, 1, true),
                        new EmptyBorder(14, 16, 14, 16)));
            }
        });
        return tf;
    }

    private void addPlaceholder(JTextField field, String placeholder) {
        field.setText(placeholder);
        field.setForeground(CLR_TEXT_SECONDARY);
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(CLR_TEXT_PRIMARY);
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(CLR_TEXT_SECONDARY);
                }
            }
        });
    }

    private JTextField createTitledField(String title, String placeholder, boolean numbersOnly, int maxLength) {
        JTextField tf = new JTextField(placeholder);
        tf.setFont(FONT_NORMAL);
        tf.setBackground(CLR_CARD_BG);
        tf.setForeground(CLR_TEXT_SECONDARY);
        tf.setCaretColor(CLR_TAXI_YELLOW);

        javax.swing.border.Border defaultBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(CLR_BORDER), title,
                0, 0, FONT_SMALL, CLR_TEXT_SECONDARY);
        javax.swing.border.Border focusedBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(CLR_TAXI_YELLOW, 2), title,
                0, 0, FONT_SMALL, CLR_TAXI_YELLOW);

        tf.setBorder(BorderFactory.createCompoundBorder(defaultBorder, new EmptyBorder(2, 8, 2, 8)));

        tf.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                tf.setBorder(BorderFactory.createCompoundBorder(focusedBorder, new EmptyBorder(2, 8, 2, 8)));
                if (tf.getText().equals(placeholder)) {
                    tf.setText("");
                    tf.setForeground(CLR_TEXT_PRIMARY);
                }
            }

            public void focusLost(java.awt.event.FocusEvent e) {
                tf.setBorder(BorderFactory.createCompoundBorder(defaultBorder, new EmptyBorder(2, 8, 2, 8)));
                if (tf.getText().trim().isEmpty()) {
                    tf.setText(placeholder);
                    tf.setForeground(CLR_TEXT_SECONDARY);
                }
            }
        });

        if (maxLength > 0) {
            ((javax.swing.text.AbstractDocument) tf.getDocument())
                    .setDocumentFilter(new javax.swing.text.DocumentFilter() {
                        public void insertString(FilterBypass fb, int offset, String string,
                                javax.swing.text.AttributeSet attr) throws javax.swing.text.BadLocationException {
                            boolean match = numbersOnly ? string.matches("[0-9/ +]*")
                                    : string.matches(
                                            "[a-zA-Z\u011F\u00FC\u015F\u0131\u00F6\u00E7\u015E\u00DC\u011E\u0130\u00D6\u00C7 ]*");
                            // Special case for email or mixed fields: if maxLength is high and numbersOnly
                            // is false, allow more chars
                            if (title.toLowerCase().contains("email"))
                                match = true;

                            if (match && (fb.getDocument().getLength() + string.length() <= maxLength)) {
                                super.insertString(fb, offset, string, attr);
                            }
                        }

                        public void replace(FilterBypass fb, int offset, int length, String text,
                                javax.swing.text.AttributeSet attrs) throws javax.swing.text.BadLocationException {
                            boolean match = numbersOnly ? text.matches("[0-9/ +]*")
                                    : text.matches(
                                            "[a-zA-Z\u011F\u00FC\u015F\u0131\u00F6\u00E7\u015E\u00DC\u011E\u0130\u00D6\u00C7 ]*");
                            if (title.toLowerCase().contains("email"))
                                match = true;

                            if (match && (fb.getDocument().getLength() - length + text.length() <= maxLength)) {
                                super.replace(fb, offset, length, text, attrs);
                            }
                        }
                    });
        }

        return tf;
    }

    private JPasswordField createTitledPassField(String title) {
        JPasswordField pf = new JPasswordField();
        pf.setBackground(CLR_CARD_BG);
        pf.setForeground(CLR_TEXT_PRIMARY);
        pf.setCaretColor(CLR_TAXI_YELLOW);
        pf.setFont(FONT_NORMAL);

        javax.swing.border.Border defaultBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(CLR_BORDER), title,
                0, 0, FONT_SMALL, CLR_TEXT_SECONDARY);
        javax.swing.border.Border focusedBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(CLR_TAXI_YELLOW, 2), title,
                0, 0, FONT_SMALL, CLR_TAXI_YELLOW);

        pf.setBorder(BorderFactory.createCompoundBorder(defaultBorder, new EmptyBorder(2, 8, 2, 8)));

        pf.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                pf.setBorder(BorderFactory.createCompoundBorder(focusedBorder, new EmptyBorder(2, 8, 2, 8)));
            }

            public void focusLost(java.awt.event.FocusEvent e) {
                pf.setBorder(BorderFactory.createCompoundBorder(defaultBorder, new EmptyBorder(2, 8, 2, 8)));
            }
        });
        return pf;
    }

    private JPasswordField createStyledPassField() {
        JPasswordField pf = new JPasswordField();
        pf.setFont(FONT_NORMAL);
        pf.setBackground(CLR_CARD_BG);
        pf.setForeground(CLR_TEXT_PRIMARY);
        pf.setCaretColor(CLR_TAXI_YELLOW);
        pf.setBorder(new CompoundBorder(
                new LineBorder(CLR_BORDER, 1, true),
                new EmptyBorder(14, 16, 14, 16)));
        pf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

        // Focus animation
        pf.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                pf.setBorder(new CompoundBorder(
                        new LineBorder(CLR_TAXI_YELLOW, 2, true),
                        new EmptyBorder(13, 15, 13, 15)));
            }

            public void focusLost(java.awt.event.FocusEvent e) {
                pf.setBorder(new CompoundBorder(
                        new LineBorder(CLR_BORDER, 1, true),
                        new EmptyBorder(14, 16, 14, 16)));
            }
        });
        return pf;
    }

    private JLabel createLabel(String text, boolean isTitle) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(isTitle ? FONT_TITLE : FONT_NORMAL);
        lbl.setForeground(isTitle ? CLR_TEXT_PRIMARY : CLR_TEXT_SECONDARY);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        return lbl;
    }

    private JPanel createWrapper() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(CLR_BG);
        p.setBorder(new EmptyBorder(25, 35, 35, 35));
        return p;
    }

    private void setupAutoComplete(JTextField textField, List<String> items) {
        final JPopupMenu popup = new JPopupMenu();
        popup.setLayout(new BorderLayout());
        popup.setBackground(CLR_CARD_BG);
        popup.setBorder(BorderFactory.createLineBorder(CLR_TAXI_YELLOW, 1));

        textField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) {
                String text = textField.getText().trim().toLowerCase();
                popup.setVisible(false);
                popup.removeAll();
                if (text.isEmpty())
                    return;

                // Collect matching suggestions
                java.util.Vector<String> matches = new java.util.Vector<>();
                for (String item : items) {
                    if (item.toLowerCase().contains(text)) {
                        matches.add(item);
                    }
                }

                if (matches.isEmpty())
                    return;

                // Create JList with dark theme
                JList<String> list = new JList<>(matches);
                list.setFont(FONT_NORMAL);
                list.setBackground(CLR_CARD_BG);
                list.setForeground(CLR_TAXI_YELLOW);
                list.setSelectionBackground(CLR_TAXI_YELLOW);
                list.setSelectionForeground(CLR_TAXI_BLACK);
                list.addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        String selected = list.getSelectedValue();
                        if (selected != null) {
                            textField.setText(selected);
                            popup.setVisible(false);
                        }
                    }
                });

                // Scrollable panel - max 6 visible
                JScrollPane scrollPane = new JScrollPane(list);
                scrollPane.getViewport().setBackground(CLR_CARD_BG);
                int visibleRows = Math.min(6, matches.size());
                scrollPane.setPreferredSize(new Dimension(textField.getWidth(), visibleRows * 28));
                scrollPane.setBorder(null);

                popup.add(scrollPane, BorderLayout.CENTER);
                popup.setPopupSize(textField.getWidth(), visibleRows * 28 + 5);
                popup.show(textField, 0, textField.getHeight());
                textField.requestFocus();
            }
        });
    }

    // --- 1. LOGIN SCREEN ---
    private JPanel createLoginPanel() {
        JPanel p = createWrapper();
        // Branded Logo
        try {
            File logoFile = new File("resources/logo.png");
            if (!logoFile.exists()) logoFile = new File("logo.png");
            
            if (logoFile.exists()) {
                BufferedImage bimg = ImageIO.read(logoFile);
                if (bimg != null) {
                    Image scaled = bimg.getScaledInstance(120, 120, Image.SCALE_SMOOTH);
                    ImageIcon logoIcon = new ImageIcon(scaled);
                    JLabel logoLabel = new JLabel(logoIcon, SwingConstants.CENTER);
                    logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                    
                    // Explicitly set sizes to prevent BoxLayout from collapsing it
                    logoLabel.setPreferredSize(new Dimension(120, 120));
                    logoLabel.setMinimumSize(new Dimension(120, 120));
                    logoLabel.setMaximumSize(new Dimension(120, 120));
                    logoLabel.setToolTipText("Order A Taxi Logo");
                    
                    p.add(logoLabel);
                } else {
                    throw new Exception("ImageIO.read returned null");
                }
            } else {
                throw new Exception("Logo file not found at " + logoFile.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("Logo loading failed: " + e.getMessage());
            JLabel icon = new JLabel("\uD83D\uDE96", SwingConstants.CENTER);
            icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 70));
            icon.setForeground(CLR_TAXI_YELLOW);
            icon.setAlignmentX(Component.CENTER_ALIGNMENT);
            p.add(icon);
        }

        JTextField mail = createStyledField();
        JPasswordField pass = createStyledPassField();

        // Remember Me checkbox
        JCheckBox rememberMe = new JCheckBox("Remember Me");
        rememberMe.setFont(FONT_SMALL);
        rememberMe.setBackground(CLR_BG);
        rememberMe.setForeground(CLR_TEXT_SECONDARY);
        rememberMe.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Load saved credentials
        File rememberFile = new File("remember.dat");
        if (rememberFile.exists()) {
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(rememberFile))) {
                String savedMail = reader.readLine();
                String savedPass = reader.readLine();
                if (savedMail != null && savedPass != null) {
                    mail.setText(savedMail);
                    pass.setText(savedPass);
                    rememberMe.setSelected(true);
                }
            } catch (Exception ex) {
            }
        }

        JButton btnLogin = createModernButton("SIGN IN", CLR_TAXI_YELLOW, CLR_TAXI_BLACK);

        JPanel linkPanel = new JPanel(new FlowLayout());
        linkPanel.setBackground(CLR_BG);
        linkPanel.setMaximumSize(new Dimension(350, 40));

        JButton btnReg = new JButton("Create Account");
        btnReg.setFont(FONT_SMALL);
        btnReg.setBorderPainted(false);
        btnReg.setContentAreaFilled(false);
        btnReg.setForeground(CLR_TAXI_YELLOW);
        btnReg.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnReg.addActionListener(e -> cardLayout.show(mainPanel, "REGISTER"));

        JButton btnForgot = new JButton("Forgot Password?");
        btnForgot.setFont(FONT_SMALL);
        btnForgot.setBorderPainted(false);
        btnForgot.setContentAreaFilled(false);
        btnForgot.setForeground(CLR_ERROR);
        btnForgot.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnForgot.addActionListener(e -> cardLayout.show(mainPanel, "FORGOT_PASS"));

        JLabel separator = new JLabel("|");
        separator.setForeground(CLR_DARK_GRAY);
        linkPanel.add(btnReg);
        linkPanel.add(separator);
        linkPanel.add(btnForgot);

        btnLogin.addActionListener(e -> {
            currentUser = manager.login(mail.getText(), new String(pass.getPassword()));
            if (currentUser == null) {
                JOptionPane.showMessageDialog(this, "Login Failed! Incorrect email or password.");
            } else {
                // Remember Me operation
                if (rememberMe.isSelected()) {
                    try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter("remember.dat"))) {
                        writer.println(mail.getText());
                        writer.println(new String(pass.getPassword()));
                    } catch (Exception ex) {
                    }
                } else {
                    // Delete file if checkbox is unchecked
                    new File("remember.dat").delete();
                }

                if (currentUser instanceof Admin) {
                    cardLayout.show(mainPanel, "ADMIN_PANEL");
                    refreshAdminTables();
                } else if (currentUser instanceof Driver) {
                    if (((Driver) currentUser).isApproved()) {
                        cardLayout.show(mainPanel, "DRIVER_HOME");
                    } else {
                        JOptionPane.showMessageDialog(this, "Your account has not been approved by Admin yet.");
                    }
                } else {
                    if (lblPassengerWelcome != null) {
                        lblPassengerWelcome.setText("Welcome " + currentUser.getName() + "!");
                    }
                    cardLayout.show(mainPanel, "PASSENGER_HOME");
                }
            }
        });

        p.add(Box.createVerticalStrut(20));
        JLabel title = createLabel("ORDER a Taxi", true);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(title);
        p.add(Box.createVerticalStrut(30));
        p.add(createLabel("Email", false));
        p.add(mail);
        p.add(Box.createVerticalStrut(15));
        p.add(createLabel("Password", false));
        p.add(pass);
        p.add(Box.createVerticalStrut(10));
        p.add(rememberMe);
        p.add(Box.createVerticalStrut(30));
        p.add(btnLogin);
        p.add(Box.createVerticalStrut(10));
        p.add(linkPanel);
        p.add(Box.createVerticalGlue());
        return p;
    }

    // --- 2. REGISTER SCREEN ---
    private JPanel createRegisterPanel() {
        JPanel p = createWrapper();

        // Title
        JLabel titleLbl = new JLabel("Create Account", SwingConstants.CENTER);
        titleLbl.setFont(FONT_TITLE);
        titleLbl.setForeground(CLR_TAXI_YELLOW);
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(titleLbl);
        p.add(Box.createVerticalStrut(20));

        JTextField name = createTitledField("Full Name", "John Doe", false, 40);
        name.setMaximumSize(new Dimension(300, 55));
        JTextField email = createTitledField("Email", "example@mail.com", false, 50);
        email.setMaximumSize(new Dimension(300, 55));

        // Email format check domains list
        String[] validDomains = { "gmail.com", "outlook.com", "hotmail.com", "yahoo.com", "icloud.com", "yandex.com",
                "protonmail.com", "live.com", "msn.com", "edu.tr", "gov.tr" };

        JPasswordField pass = createTitledPassField("Create Password");
        pass.setMaximumSize(new Dimension(300, 55));
        JPasswordField passConfirm = createTitledPassField("Confirm Password");
        passConfirm.setMaximumSize(new Dimension(300, 55));

        // Phone number - +90 prefix and auto format
        JTextField phone = createTitledField("Phone (+90 XXX XXX XX XX)", "", true, 0);
        phone.setText("+90 ");
        phone.setMaximumSize(new Dimension(300, 55));

        // Only digits and max 10 digits
        phone.addKeyListener(new java.awt.event.KeyAdapter() {
            private String getDigitsAfterPrefix(String text) {
                // Get digits after +90
                if (text.startsWith("+90 ")) {
                    return text.substring(4).replaceAll("[^0-9]", "");
                }
                return text.replaceAll("[^0-9]", "");
            }

            private String formatPhone(String digits) {
                StringBuilder sb = new StringBuilder("+90 ");
                for (int i = 0; i < digits.length() && i < 10; i++) {
                    if (i == 3 || i == 6 || i == 8)
                        sb.append(" ");
                    sb.append(digits.charAt(i));
                }
                return sb.toString();
            }

            public void keyTyped(java.awt.event.KeyEvent e) {
                char c = e.getKeyChar();
                String digits = getDigitsAfterPrefix(phone.getText());

                // Allow Backspace and Delete
                if (c == '\b' || c == 127)
                    return;

                // Only digits and max 10 digits
                if (!Character.isDigit(c) || digits.length() >= 10) {
                    e.consume();
                }
            }

            public void keyReleased(java.awt.event.KeyEvent e) {
                String text = phone.getText();

                // +90 protection
                if (!text.startsWith("+90 ")) {
                    phone.setText("+90 ");
                    phone.setCaretPosition(4);
                    return;
                }

                String digits = getDigitsAfterPrefix(text);
                String formatted = formatPhone(digits);

                if (!text.equals(formatted)) {
                    phone.setText(formatted);
                    if (formatted.length() >= 4) {
                        phone.setCaretPosition(formatted.length());
                    } else {
                        phone.setCaretPosition(formatted.length());
                    }
                }
            }
        });

        JCheckBox isDriver = new JCheckBox("Register as Driver");
        isDriver.setAlignmentX(Component.CENTER_ALIGNMENT);
        isDriver.setBackground(CLR_BG);
        isDriver.setForeground(CLR_TAXI_YELLOW);
        isDriver.setFont(FONT_NORMAL);

        JPanel driverPanel = new JPanel();
        driverPanel.setLayout(new BoxLayout(driverPanel, BoxLayout.Y_AXIS));
        driverPanel.setBackground(CLR_BG);
        driverPanel.setVisible(false);

        // Car brand dropdown - wrap in panel for dark background
        JPanel brandPanel = new JPanel(new BorderLayout());
        brandPanel.setBackground(CLR_CARD_BG);
        brandPanel.setMaximumSize(new Dimension(280, 55));
        brandPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(CLR_TAXI_YELLOW), "Car Brand",
                javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.TOP,
                FONT_SMALL, CLR_TAXI_YELLOW));
        String[] carBrands = { "Toyota", "Honda", "Volkswagen", "Ford", "BMW", "Mercedes", "Audi", "Hyundai", "Kia",
                "Renault", "Fiat", "Peugeot", "Opel", "Nissan", "Mazda" };
        JComboBox<String> brandCombo = new JComboBox<>(carBrands);
        brandCombo.setBackground(CLR_CARD_BG);
        brandCombo.setForeground(CLR_TAXI_YELLOW);
        brandCombo.setFont(FONT_NORMAL);
        brandCombo.setBorder(null);
        brandCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                boolean highlighted = isSelected && index >= 0;
                JPanel panel = new JPanel(new BorderLayout());
                panel.setBackground(highlighted ? CLR_TAXI_YELLOW : CLR_CARD_BG);
                panel.setBorder(new EmptyBorder(5, 8, 5, 8));
                if (value != null) {
                    JLabel label = new JLabel(value.toString());
                    label.setFont(FONT_NORMAL);
                    label.setForeground(highlighted ? CLR_TAXI_BLACK : CLR_TAXI_YELLOW);
                    panel.add(label, BorderLayout.WEST);
                }
                return panel;
            }
        });
        brandPanel.add(brandCombo, BorderLayout.CENTER);

        // Car model dropdown - wrap in panel for dark background
        JPanel modelPanel = new JPanel(new BorderLayout());
        modelPanel.setBackground(CLR_CARD_BG);
        modelPanel.setMaximumSize(new Dimension(280, 55));
        modelPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(CLR_TAXI_YELLOW), "Car Model",
                javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.TOP,
                FONT_SMALL, CLR_TAXI_YELLOW));
        JComboBox<String> modelCombo = new JComboBox<>();
        modelCombo.setBackground(CLR_CARD_BG);
        modelCombo.setForeground(CLR_TAXI_YELLOW);
        modelCombo.setFont(FONT_NORMAL);
        modelCombo.setBorder(null);
        modelCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                boolean highlighted = isSelected && index >= 0;
                JPanel panel = new JPanel(new BorderLayout());
                panel.setBackground(highlighted ? CLR_TAXI_YELLOW : CLR_CARD_BG);
                panel.setBorder(new EmptyBorder(5, 8, 5, 8));
                if (value != null) {
                    JLabel label = new JLabel(value.toString());
                    label.setFont(FONT_NORMAL);
                    label.setForeground(highlighted ? CLR_TAXI_BLACK : CLR_TAXI_YELLOW);
                    panel.add(label, BorderLayout.WEST);
                }
                return panel;
            }
        });
        modelPanel.add(modelCombo, BorderLayout.CENTER);

        // Brand-Model mapping
        java.util.Map<String, String[]> brandModels = new java.util.HashMap<>();
        brandModels.put("Toyota", new String[] { "Corolla", "Camry", "Yaris", "C-HR", "RAV4", "Land Cruiser" });
        brandModels.put("Honda", new String[] { "Civic", "Accord", "CR-V", "HR-V", "Jazz", "City" });
        brandModels.put("Volkswagen", new String[] { "Golf", "Passat", "Polo", "Tiguan", "Jetta", "Arteon" });
        brandModels.put("Ford", new String[] { "Focus", "Fiesta", "Mondeo", "Kuga", "Puma", "Mustang" });
        brandModels.put("BMW", new String[] { "3 Series", "5 Series", "X1", "X3", "X5", "1 Series" });
        brandModels.put("Mercedes", new String[] { "C-Class", "E-Class", "A-Class", "GLA", "GLC", "S-Class" });
        brandModels.put("Audi", new String[] { "A3", "A4", "A6", "Q3", "Q5", "Q7" });
        brandModels.put("Hyundai", new String[] { "i20", "i30", "Tucson", "Kona", "Elantra", "Santa Fe" });
        brandModels.put("Kia", new String[] { "Ceed", "Sportage", "Seltos", "Stonic", "Rio", "Sorento" });
        brandModels.put("Renault", new String[] { "Clio", "Megane", "Captur", "Kadjar", "Taliant", "Koleos" });
        brandModels.put("Fiat", new String[] { "Egea", "500", "Panda", "Tipo", "Doblo", "500X" });
        brandModels.put("Peugeot", new String[] { "208", "308", "3008", "2008", "508", "5008" });
        brandModels.put("Opel", new String[] { "Corsa", "Astra", "Insignia", "Mokka", "Crossland", "Grandland" });
        brandModels.put("Nissan", new String[] { "Qashqai", "Juke", "X-Trail", "Micra", "Leaf", "Navara" });
        brandModels.put("Mazda", new String[] { "3", "6", "CX-3", "CX-5", "CX-30", "MX-5" });

        // Load models for first brand
        modelCombo.setModel(new DefaultComboBoxModel<>(brandModels.get("Toyota")));

        // Update models when brand changes
        brandCombo.addActionListener(e -> {
            String selectedBrand = (String) brandCombo.getSelectedItem();
            String[] models = brandModels.get(selectedBrand);
            modelCombo.setModel(new DefaultComboBoxModel<>(models));
        });

        // Vehicle type dropdown - wrap in panel for dark background
        JPanel vehicleTypePanel = new JPanel(new BorderLayout());
        vehicleTypePanel.setBackground(CLR_CARD_BG);
        vehicleTypePanel.setMaximumSize(new Dimension(280, 55));
        vehicleTypePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(CLR_TAXI_YELLOW), "Vehicle Type",
                javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.TOP,
                FONT_SMALL, CLR_TAXI_YELLOW));
        String[] vehicleTypes = { "Sedan", "SUV", "Hatchback", "Minivan", "Station Wagon", "Pickup" };
        JComboBox<String> vehicleTypeCombo = new JComboBox<>(vehicleTypes);
        vehicleTypeCombo.setBackground(CLR_CARD_BG);
        vehicleTypeCombo.setForeground(CLR_TAXI_YELLOW);
        vehicleTypeCombo.setFont(FONT_NORMAL);
        vehicleTypeCombo.setBorder(null);
        vehicleTypeCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                boolean highlighted = isSelected && index >= 0;
                JPanel panel = new JPanel(new BorderLayout());
                panel.setBackground(highlighted ? CLR_TAXI_YELLOW : CLR_CARD_BG);
                panel.setBorder(new EmptyBorder(5, 8, 5, 8));
                if (value != null) {
                    JLabel label = new JLabel(value.toString());
                    label.setFont(FONT_NORMAL);
                    label.setForeground(highlighted ? CLR_TAXI_BLACK : CLR_TAXI_YELLOW);
                    panel.add(label, BorderLayout.WEST);
                }
                return panel;
            }
        });
        vehicleTypePanel.add(vehicleTypeCombo, BorderLayout.CENTER);

        JButton btnUploadLicense = new JButton("Upload License");
        btnUploadLicense.setFont(FONT_SMALL);
        btnUploadLicense.setBackground(CLR_CARD_BG);
        btnUploadLicense.setForeground(CLR_TAXI_YELLOW);
        btnUploadLicense.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(CLR_TAXI_YELLOW, 1, true),
                new EmptyBorder(8, 12, 8, 12)));
        btnUploadLicense.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnUploadLicense.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                selectedDriverDoc = fileChooser.getSelectedFile();
                btnUploadLicense.setText(selectedDriverDoc.getName());
            }
        });

        JButton btnUploadCriminal = new JButton("Upload Record");
        btnUploadCriminal.setFont(FONT_SMALL);
        btnUploadCriminal.setBackground(CLR_CARD_BG);
        btnUploadCriminal.setForeground(CLR_TAXI_YELLOW);
        btnUploadCriminal.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(CLR_TAXI_YELLOW, 1, true),
                new EmptyBorder(8, 12, 8, 12)));
        btnUploadCriminal.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnUploadCriminal.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                selectedCriminalDoc = fileChooser.getSelectedFile();
                btnUploadCriminal.setText(selectedCriminalDoc.getName());
            }
        });

        // Upload buttons side by side
        JPanel uploadPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        uploadPanel.setBackground(CLR_BG);
        uploadPanel.add(btnUploadLicense);
        uploadPanel.add(btnUploadCriminal);

        driverPanel.add(Box.createVerticalStrut(10));
        driverPanel.add(brandPanel);
        driverPanel.add(Box.createVerticalStrut(8));
        driverPanel.add(modelPanel);
        driverPanel.add(Box.createVerticalStrut(8));
        driverPanel.add(vehicleTypePanel);
        driverPanel.add(Box.createVerticalStrut(10));
        driverPanel.add(uploadPanel);
        driverPanel.add(Box.createVerticalStrut(10));

        isDriver.addActionListener(e -> {
            driverPanel.setVisible(isDriver.isSelected());
            p.revalidate();
            p.repaint();
        });

        JButton btnSave = createModernButton("SIGN UP", CLR_TAXI_YELLOW, CLR_TAXI_BLACK);
        btnSave.addActionListener(e -> {
            String tName = name.getText().trim();
            String tEmail = email.getText().trim();
            String tPass = new String(pass.getPassword()).trim();
            String tPassConf = new String(passConfirm.getPassword()).trim();
            String tPhone = phone.getText().trim();
            String tBrand = (String) brandCombo.getSelectedItem();
            String tModel = (String) modelCombo.getSelectedItem();
            String tVehicleType = (String) vehicleTypeCombo.getSelectedItem();
            String tCarFull = tBrand + " " + tModel; // Ã¶rn: "Toyota Corolla"

            if (tName.isEmpty() || tEmail.isEmpty() || tPass.isEmpty() || tPassConf.isEmpty() || tPhone.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all fields!", "Missing Information",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!tPass.equals(tPassConf)) {
                JOptionPane.showMessageDialog(this, "Passwords do not match!", "Password Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Email domain check
            boolean validEmail = false;
            for (String domain : validDomains) {
                if (tEmail.toLowerCase().endsWith("@" + domain)) {
                    validEmail = true;
                    break;
                }
            }
            if (!validEmail) {
                JOptionPane.showMessageDialog(this,
                        "Invalid email address!",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Phone format check (must be 10 digits - excluding +90)
            String phoneDigits = tPhone.startsWith("+90 ") ? tPhone.substring(4).replaceAll("[^0-9]", "")
                    : tPhone.replaceAll("[^0-9]", "");
            if (phoneDigits.length() != 10) {
                JOptionPane.showMessageDialog(this, "Phone number must be 10 digits!", "Phone Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (isDriver.isSelected()) {
                if (selectedDriverDoc == null || selectedCriminalDoc == null) {
                    JOptionPane.showMessageDialog(this, "Driver license AND criminal record are required!",
                            "Missing Documents",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                Driver d = new Driver(tName, tEmail, tPass, tPhone, tCarFull, tVehicleType);
                d.setLicenseDoc(selectedDriverDoc);
                d.setCriminalDoc(selectedCriminalDoc);
                manager.registerDriver(d);
                JOptionPane.showMessageDialog(this, "Application received! Waiting for admin approval.");
                selectedDriverDoc = null;
                selectedCriminalDoc = null;
                selectedCriminalDoc = null;
                btnUploadLicense.setText("Upload Driver License");
                btnUploadCriminal.setText("Upload Criminal Record");
            } else {
                Passenger pa = new Passenger(tName, tEmail, tPass, tPhone);
                manager.registerPassenger(pa);
                JOptionPane.showMessageDialog(this, "Account created successfully!");
            }
            cardLayout.show(mainPanel, "LOGIN");
        });

        JButton btnBack = createModernButton("Back to Login", CLR_TAXI_YELLOW,
                CLR_TAXI_BLACK);
        btnBack.addActionListener(e -> cardLayout.show(mainPanel, "LOGIN"));

        p.add(createLabel("New Account", true));
        p.add(Box.createVerticalStrut(10));
        p.add(name);
        p.add(Box.createVerticalStrut(5));
        p.add(email);
        p.add(Box.createVerticalStrut(5));
        p.add(pass);
        p.add(Box.createVerticalStrut(5));
        p.add(passConfirm);
        p.add(Box.createVerticalStrut(5));
        p.add(phone);
        p.add(Box.createVerticalStrut(10));
        p.add(isDriver);
        p.add(driverPanel);
        p.add(Box.createVerticalStrut(20));
        p.add(btnSave);
        p.add(Box.createVerticalStrut(10));
        p.add(btnBack);
        return p;
    }

    // --- FORGOT PASSWORD PANEL ---
    private JPanel createForgotPasswordPanel() {
        JPanel p = createWrapper();
        JLabel title = createLabel("Reset Password", true);
        title.setForeground(CLR_ERROR);
        JTextField emailField = createStyledField();
        JTextField codeField = createStyledField();
        codeField.setToolTipText("Verification Code");
        codeField.setEnabled(false);
        JPasswordField newPassField = createStyledPassField();
        newPassField.setEnabled(false);

        JButton btnSendCode = createModernButton("SEND CODE", CLR_TAXI_YELLOW, CLR_TAXI_BLACK);
        JButton btnReset = createModernButton("RESET PASSWORD", CLR_TAXI_YELLOW, CLR_TAXI_BLACK);
        btnReset.setEnabled(false);

        btnSendCode.addActionListener(e -> {
            String mail = emailField.getText().trim();
            if (mail.isEmpty() || !mail.contains("@")) {
                JOptionPane.showMessageDialog(this, "Please enter a valid email!");
                return;
            }
            // AuthService â€” Mock OTP (JOptionPane simÃ¼lasyonu)
            authService.sendOTP(mail);
            generatedResetCode = ""; // artÄ±k AuthService'te saklanÄ±yor
            codeField.setEnabled(true);
            newPassField.setEnabled(true);
            btnReset.setEnabled(true);
            btnReset.setBackground(CLR_STRAW);
            btnReset.setForeground(Color.BLACK);
            btnSendCode.setEnabled(false);
        });

        btnReset.addActionListener(e -> {
            String mail = emailField.getText().trim();
            String entered = codeField.getText().trim();
            if (!authService.verifyOTP(mail, entered)) {
                JOptionPane.showMessageDialog(this, "Invalid Code!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (new String(newPassField.getPassword()).isEmpty()) {
                JOptionPane.showMessageDialog(this, "Password cannot be empty!");
                return;
            }
            boolean success = authService.resetPassword(mail, new String(newPassField.getPassword()));
            if (success) {
                JOptionPane.showMessageDialog(this, "Password Updated! Please Login.");
                emailField.setText("");
                codeField.setText("");
                newPassField.setText("");
                codeField.setEnabled(false);
                newPassField.setEnabled(false);
                btnSendCode.setEnabled(true);
                cardLayout.show(mainPanel, "LOGIN");
            } else {
                JOptionPane.showMessageDialog(this, "Email not found!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnBack = createModernButton("Back to Login", CLR_DARK_GRAY, CLR_STRAW);
        btnBack.addActionListener(e -> cardLayout.show(mainPanel, "LOGIN"));

        p.add(title);
        p.add(Box.createVerticalStrut(20));
        p.add(createLabel("Registered Email:", false));
        p.add(emailField);
        p.add(Box.createVerticalStrut(10));
        p.add(btnSendCode);
        p.add(Box.createVerticalStrut(20));
        p.add(createLabel("Enter Code:", false));
        p.add(codeField);
        p.add(Box.createVerticalStrut(10));
        p.add(createLabel("New Password:", false));
        p.add(newPassField);
        p.add(Box.createVerticalStrut(20));
        p.add(btnReset);
        p.add(Box.createVerticalStrut(10));
        p.add(btnBack);
        return p;
    }

    // --- 3. PASSENGER HOME WIZARD ---
    private JComboBox<String> comboDrivers;

    private JPanel createPassengerHome() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(CLR_BG);
        wrapper.setBorder(new EmptyBorder(5, 25, 20, 10));

        // HEADER logic moved to STEP1 only

        // Use instance variables for wizard
        wizardPanel.removeAll();
        wizardPanel.setBackground(CLR_BG);

        // SHARED WIZARD FIELDS
        JTextField from = createStyledField();
        JTextField to = createStyledField();

        List<String> places = List.of(
                "Istanbul Airport", "Sabiha Gokcen Airport", "Taksim", "Beyoglu", "Galata", "Karakoy", "Eminonu",
                "Besiktas", "Nisantasi", "Sisli", "Mecidiyekoy", "Levent", "Maslak", "Kadikoy", "Moda",
                "Fenerbahce", "Caddebostan", "Bostanci", "Maltepe", "Zorlu Center", "Istinye Park");
        setupAutoComplete(from, places);
        setupAutoComplete(to, places);

        JDateChooser dateChooser = new JDateChooser();
        dateChooser.setLocale(Locale.ENGLISH);
        dateChooser.setDateFormatString("dd MMMM yyyy, EEEE");
        dateChooser.setFont(FONT_NORMAL);
        dateChooser.setBackground(CLR_CARD_BG);
        Calendar calDate = Calendar.getInstance();
        Date today = calDate.getTime();
        dateChooser.setMinSelectableDate(today);
        calDate.add(Calendar.DAY_OF_YEAR, 1);
        Date tomorrow = calDate.getTime();
        dateChooser.setMaxSelectableDate(tomorrow);
        dateChooser.setDate(today);

        final String[] selectedTime = { "" };
        JComboBox<String> vehicleType = new JComboBox<>(new String[] { "Economy", "Premium", "XL" });
        vehicleType.setBackground(CLR_TAXI_YELLOW);
        vehicleType.setForeground(Color.BLACK);
        vehicleType.setFont(FONT_NORMAL);

        comboDrivers = new JComboBox<>();
        comboDrivers.setBackground(CLR_TAXI_YELLOW);
        comboDrivers.setForeground(Color.BLACK);
        comboDrivers.setFont(FONT_NORMAL);

        JPanel pricePanel = new JPanel();
        pricePanel.setLayout(new BoxLayout(pricePanel, BoxLayout.Y_AXIS));
        pricePanel.setBackground(CLR_BG);
        pricePanel.setVisible(false);

        JLabel lblPriceAmount = createLabel("Est. Price: - TL", false);
        lblPriceAmount.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblPriceAmount.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblPriceAmount.setForeground(CLR_TAXI_YELLOW);

        JLabel lblPriceFee = createLabel("", false);
        lblPriceFee.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblPriceFee.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblPriceFee.setForeground(new Color(152, 152, 157));

        JLabel lblPriceDetails = createLabel("", false);
        lblPriceDetails.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblPriceDetails.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 15));
        lblPriceDetails.setForeground(Color.WHITE);

        pricePanel.add(lblPriceAmount);
        pricePanel.add(lblPriceFee);
        pricePanel.add(Box.createVerticalStrut(5));
        pricePanel.add(lblPriceDetails);

        // ==========================================
        // STEP 1: WELCOME
        // ==========================================
        JPanel step1 = new JPanel(new BorderLayout());
        step1.setBackground(CLR_BG);

        // Header for Step 1
        JPanel header = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        header.setBackground(CLR_BG);
        JButton btnHeaderLog = new JButton("Logout \u23FB");
        btnHeaderLog.setFont(new Font("Segoe UI Symbol", Font.BOLD, 14));
        btnHeaderLog.setForeground(Color.WHITE);
        btnHeaderLog.setBackground(CLR_BG);
        btnHeaderLog.setBorderPainted(false);
        btnHeaderLog.setFocusPainted(false);
        btnHeaderLog.setContentAreaFilled(false);
        btnHeaderLog.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnHeaderLog.addActionListener(e -> cardLayout.show(mainPanel, "LOGIN"));
        header.add(btnHeaderLog);
        step1.add(header, BorderLayout.NORTH);

        JPanel welcomeContent = new JPanel(new GridBagLayout());
        welcomeContent.setBackground(CLR_BG);
        GridBagConstraints g1 = new GridBagConstraints();
        g1.gridx = 0;
        g1.gridy = 0;
        g1.insets = new Insets(0, 0, 10, 0);

        JLabel lblWelcome = new JLabel("Welcome " + (currentUser != null ? currentUser.getName() : "Passenger") + "!");
        lblPassengerWelcome = lblWelcome;
        lblWelcome.setFont(new Font("Segoe UI", Font.BOLD, 32));
        lblWelcome.setForeground(CLR_TAXI_YELLOW);
        welcomeContent.add(lblWelcome, g1);

        g1.gridy = 1;
        g1.insets = new Insets(0, 0, 40, 0);
        JLabel lblSub = new JLabel("Where to next?");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        lblSub.setForeground(CLR_TEXT_SECONDARY);
        welcomeContent.add(lblSub, g1);

        g1.gridy = 2;
        g1.insets = new Insets(0, 0, 0, 0);
        JButton btnStartWizard = createModernButton("START A RIDE", CLR_TAXI_YELLOW, CLR_TAXI_BLACK);
        btnStartWizard.setPreferredSize(new Dimension(280, 60));
        btnStartWizard.setFont(new Font("Segoe UI", Font.BOLD, 20));
        btnStartWizard.addActionListener(e -> wizardLayout.show(wizardPanel, "STEP2"));
        welcomeContent.add(btnStartWizard, g1);

        step1.add(welcomeContent, BorderLayout.CENTER);

        // FOOTER (Only on Welcome Page)
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        footer.setBackground(CLR_BG);
        JButton btnSup = createModernButton("Support", CLR_TAXI_BLACK, CLR_TAXI_YELLOW);
        btnSup.addActionListener(e -> cardLayout.show(mainPanel, "SUPPORT_PANEL"));
        JButton btnHistory = createModernButton("My Rides", CLR_TAXI_BLACK, CLR_TAXI_YELLOW);
        btnHistory.addActionListener(e -> {
            cardLayout.show(mainPanel, "RIDE_HISTORY");
        });
        footer.add(btnHistory);
        footer.add(btnSup);
        step1.add(footer, BorderLayout.SOUTH);

        wizardPanel.add(step1, "STEP1");

        // ==========================================
        // STEP 2: LOCATION SELECTION
        // ==========================================
        JPanel step2 = new JPanel(new BorderLayout());
        step2.setBackground(CLR_BG);

        JPanel locForm = new JPanel(new GridBagLayout());
        locForm.setBackground(CLR_CARD_BG);
        locForm.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(CLR_TAXI_YELLOW, 2, true),
                new EmptyBorder(20, 20, 20, 20)));

        GridBagConstraints g2 = new GridBagConstraints();
        g2.insets = new Insets(10, 10, 10, 10);
        g2.anchor = GridBagConstraints.WEST;

        g2.gridx = 0;
        g2.gridy = 0;
        g2.weightx = 0;
        locForm.add(createLabel("From:", false), g2);

        JPanel fromContainer = new JPanel(new BorderLayout());
        fromContainer.setBackground(CLR_CARD_BG);
        fromContainer.add(from, BorderLayout.CENTER);
        g2.gridx = 1;
        g2.weightx = 1.0;
        g2.fill = GridBagConstraints.HORIZONTAL;
        locForm.add(fromContainer, g2);

        g2.gridx = 0;
        g2.gridy = 1;
        g2.weightx = 0;
        g2.fill = GridBagConstraints.NONE;
        locForm.add(createLabel("To:", false), g2);
        g2.gridx = 1;
        g2.weightx = 1.0;
        g2.fill = GridBagConstraints.HORIZONTAL;
        locForm.add(to, g2);

        JButton btnMap = createModernButton("Pick on Map", CLR_CARD_BG, CLR_TAXI_YELLOW);
        btnMap.setBorder(BorderFactory.createCompoundBorder(new LineBorder(CLR_TAXI_YELLOW, 1, true),
                new EmptyBorder(10, 24, 10, 24)));
        btnMap.addActionListener(e -> {
            MapPickerDialog dlg = new MapPickerDialog(TaxiFinalApp.this);
            dlg.setVisible(true);
            if (dlg.isConfirmed()) {
                if (!dlg.getPickupAddress().isEmpty())
                    from.setText(dlg.getPickupAddress());
                if (!dlg.getDropoffAddress().isEmpty())
                    to.setText(dlg.getDropoffAddress());
                if (dlg.getPickupPos() != null && dlg.getDropoffPos() != null) {
                    from.putClientProperty("lat", dlg.getPickupPos().getLatitude());
                    from.putClientProperty("lon", dlg.getPickupPos().getLongitude());
                    to.putClientProperty("lat", dlg.getDropoffPos().getLatitude());
                    to.putClientProperty("lon", dlg.getDropoffPos().getLongitude());
                }
            }
        });

        JPanel s2Top = new JPanel();
        s2Top.setLayout(new BoxLayout(s2Top, BoxLayout.Y_AXIS));
        s2Top.setBackground(CLR_BG);
        s2Top.add(createLabel("Step 1: Choose Locations", true));
        s2Top.add(Box.createVerticalStrut(10));
        s2Top.add(btnMap);
        s2Top.add(Box.createVerticalStrut(15));
        s2Top.add(locForm);

        JButton btnNext2 = createModernButton("NEXT STEP", CLR_TAXI_YELLOW, CLR_TAXI_BLACK);
        btnNext2.addActionListener(e -> {
            if (from.getText().isEmpty() || to.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter pickup and dropoff locations!");
                return;
            }
            wizardLayout.show(wizardPanel, "STEP3");
        });
        JPanel s2Bot = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        s2Bot.setBackground(CLR_BG);
        JButton btnBack1 = createModernButton("Back", CLR_CARD_BG, CLR_TAXI_YELLOW);
        btnBack1.addActionListener(e -> wizardLayout.show(wizardPanel, "STEP1"));
        s2Bot.add(btnBack1);
        s2Bot.add(btnNext2);

        step2.add(s2Top, BorderLayout.NORTH);
        step2.add(s2Bot, BorderLayout.SOUTH);
        wizardPanel.add(step2, "STEP2");

        // ==========================================
        // STEP 3: SCHEDULE / NOW
        // ==========================================
        JPanel step3 = new JPanel(new BorderLayout());
        step3.setBackground(CLR_BG);

        JPanel schedForm = new JPanel(new GridBagLayout());
        schedForm.setBackground(CLR_CARD_BG);
        schedForm.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(CLR_TAXI_YELLOW, 2, true),
                new EmptyBorder(20, 20, 20, 20)));

        GridBagConstraints g3 = new GridBagConstraints();
        g3.insets = new Insets(10, 10, 10, 10);
        g3.anchor = GridBagConstraints.WEST;

        g3.gridx = 0;
        g3.gridy = 0;
        schedForm.add(createLabel("Timing:", false), g3);

        String[] timingOpts = { "Ride Now", "Schedule Later" };
        JComboBox<String> timingCombo = new JComboBox<>(timingOpts);
        timingCombo.setBackground(CLR_TAXI_YELLOW);
        timingCombo.setForeground(Color.BLACK);
        g3.gridx = 1;
        g3.fill = GridBagConstraints.HORIZONTAL;
        g3.weightx = 1.0;
        schedForm.add(timingCombo, g3);

        g3.gridx = 0;
        g3.gridy = 1;
        g3.weightx = 0;
        JLabel lblDate = createLabel("Date:", false);
        schedForm.add(lblDate, g3);
        g3.gridx = 1;
        g3.weightx = 1.0;
        schedForm.add(dateChooser, g3);

        g3.gridx = 0;
        g3.gridy = 2;
        g3.weightx = 0;
        JLabel lblTime = createLabel("Time (HH:MM):", false);
        schedForm.add(lblTime, g3);

        JTextField timeField = createStyledField();
        timeField.setText("12:00");
        timeField.setEditable(false);
        timeField.setCursor(new Cursor(Cursor.HAND_CURSOR));
        timeField.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int minH = -1;
                int minM = -1;
                Calendar now = Calendar.getInstance();
                Calendar chosen = Calendar.getInstance();
                if (dateChooser.getDate() != null) {
                    chosen.setTime(dateChooser.getDate());
                    if (now.get(Calendar.YEAR) == chosen.get(Calendar.YEAR) &&
                            now.get(Calendar.DAY_OF_YEAR) == chosen.get(Calendar.DAY_OF_YEAR)) {
                        minH = now.get(Calendar.HOUR_OF_DAY);
                        minM = now.get(Calendar.MINUTE);
                    }
                }

                TimePickerDialog tpd = new TimePickerDialog(TaxiFinalApp.this, timeField.getText(), minH, minM);
                tpd.setVisible(true);
                if (tpd.isConfirmed()) {
                    timeField.setText(tpd.getSelectedTime());
                    selectedTime[0] = tpd.getSelectedTime();
                }
            }
        });
        g3.gridx = 1;
        g3.weightx = 1.0;
        schedForm.add(timeField, g3);

        // Hide date/time if 'Ride Now' is selected
        lblDate.setVisible(false);
        dateChooser.setVisible(false);
        lblTime.setVisible(false);
        timeField.setVisible(false);

        timingCombo.addActionListener(e -> {
            boolean isSched = timingCombo.getSelectedIndex() == 1;
            lblDate.setVisible(isSched);
            dateChooser.setVisible(isSched);
            lblTime.setVisible(isSched);
            timeField.setVisible(isSched);
            if (!isSched) {
                selectedTime[0] = "NOW";
            } else {
                selectedTime[0] = timeField.getText();
            }
        });
        selectedTime[0] = "NOW"; // Default

        timeField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                selectedTime[0] = timeField.getText();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                selectedTime[0] = timeField.getText();
            }

            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                selectedTime[0] = timeField.getText();
            }
        });

        g3.gridx = 0;
        g3.gridy = 3;
        g3.weightx = 0;
        schedForm.add(createLabel("Vehicle Type:", false), g3);
        g3.gridx = 1;
        g3.weightx = 1.0;
        schedForm.add(vehicleType, g3);

        JPanel s3Top = new JPanel();
        s3Top.setLayout(new BoxLayout(s3Top, BoxLayout.Y_AXIS));
        s3Top.setBackground(CLR_BG);
        s3Top.add(createLabel("Step 2: Ride Options", true));
        s3Top.add(Box.createVerticalStrut(15));
        s3Top.add(schedForm);

        JPanel actionPanel = new JPanel();
        actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.Y_AXIS));
        actionPanel.setBackground(CLR_BG);

        JButton btnFind = createModernButton("Check Price & Drivers", CLR_TAXI_YELLOW, CLR_TAXI_BLACK);
        btnFind.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnFind.addActionListener(e -> {
            Location loc1;
            Location loc2;
            if (from.getClientProperty("lat") != null && from.getClientProperty("lon") != null) {
                loc1 = new Location((Double) from.getClientProperty("lat"), (Double) from.getClientProperty("lon"));
            } else {
                loc1 = getCoordinates(from.getText());
            }
            if (to.getClientProperty("lat") != null && to.getClientProperty("lon") != null) {
                loc2 = new Location((Double) to.getClientProperty("lat"), (Double) to.getClientProperty("lon"));
            } else {
                loc2 = getCoordinates(to.getText());
            }

            int targetHour;
            if (timingCombo.getSelectedIndex() == 0) { // Ride Now
                targetHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            } else { // Schedule Later
                String timeStr = timeField.getText();
                targetHour = Integer.parseInt(timeStr.split(":")[0]);
            }
            Route route = MapService.getRoute(loc1, loc2, targetHour);
            VehicleType vt = vehicleType.getSelectedItem().equals("Premium") ? VehicleType.PREMIUM
                    : vehicleType.getSelectedItem().equals("XL") ? VehicleType.XL : VehicleType.ECONOMY;
            double price = PricingService.calculateFare(route.getDistanceKm(), vt, route.getTrafficFactor());
            String feeNote = "";
            if (timingCombo.getSelectedIndex() == 1) {
                price *= 1.20; // 20% Uber Reserve premium
                feeNote = " (inc. fee)";
            }
            int eta = route.getDurationMinutes();

            List<Driver> allDrivers = manager.getAllDrivers().stream().filter(d -> d.isApproved() && !d.isBanned())
                    .collect(java.util.stream.Collectors.toList());
            comboDrivers.removeAllItems();
            for (Driver d : allDrivers) {
                comboDrivers.addItem(d.getName() + " (" + String.format("%.1f", d.getAverageRating()) + "*)"
                        + (d.isAvailable() ? "" : " [BUSY]"));
            }
            // Native Swing vertical layout (No HTML wrapping issues)
            lblPriceAmount.setText(String.format("Est. %.0f TL", price));
            if (feeNote.isEmpty()) {
                lblPriceFee.setVisible(false);
            } else {
                lblPriceFee.setVisible(true);
                lblPriceFee.setText(feeNote.trim());
            }
            lblPriceDetails.setText(String.format("\u23F2 ~%dmin    \uD83D\uDE95 %.1fkm", eta, route.getDistanceKm()));
            pricePanel.setVisible(true);
        });

        comboDrivers.setMaximumSize(new Dimension(300, 40));
        pricePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        comboDrivers.setAlignmentX(Component.CENTER_ALIGNMENT);

        actionPanel.add(Box.createVerticalStrut(15));
        actionPanel.add(btnFind);
        actionPanel.add(Box.createVerticalStrut(10));
        actionPanel.add(pricePanel);
        actionPanel.add(Box.createVerticalStrut(5));
        actionPanel.add(comboDrivers);

        JButton btnRequest = createModernButton("PROCEED TO PAYMENT", CLR_TAXI_YELLOW, CLR_TAXI_BLACK);
        btnRequest.setAlignmentX(Component.CENTER_ALIGNMENT);

        // --- STEP 4: PAYMENT (NEW) ---
        JPanel step4 = new JPanel(new BorderLayout());
        step4.setBackground(CLR_BG);

        JPanel s4Top = createWrapper();
        s4Top.add(createLabel("Step 3: Payment Method", true));
        s4Top.add(Box.createVerticalStrut(20));

        JComboBox<String> payMethodCombo = new JComboBox<>(
                new String[] { "\uD83D\uDCB3 Credit Card", "\uD83D\uDCB5 Cash" });
        payMethodCombo.setBackground(CLR_TAXI_YELLOW);
        payMethodCombo.setForeground(Color.BLACK);
        payMethodCombo.setFont(new Font("Segoe UI Symbol", Font.BOLD, 15));
        payMethodCombo.setMaximumSize(new Dimension(300, 45));
        payMethodCombo.setAlignmentX(Component.CENTER_ALIGNMENT);

        // --- Tipping Section ---
        JPanel tipPanel = new JPanel();
        tipPanel.setLayout(new BoxLayout(tipPanel, BoxLayout.Y_AXIS));
        tipPanel.setBackground(CLR_BG);
        tipPanel.add(Box.createVerticalStrut(15));

        JTextField tipField = createStyledField();
        tipField.setMaximumSize(new Dimension(300, 45));
        addPlaceholder(tipField, "Tip if you want (TL)");
        tipPanel.add(tipField);

        // --- Card Info Form (Hidden by default) ---
        JPanel cardInfoForm = new JPanel();
        cardInfoForm.setLayout(new BoxLayout(cardInfoForm, BoxLayout.Y_AXIS));
        cardInfoForm.setBackground(CLR_BG);
        cardInfoForm.setVisible(true); // Default selection is Card

        JTextField cardNo = createTitledField("Card Number", "1234 5678 9012 3456", true, 19);
        cardNo.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                format();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
            }

            private void format() {
                SwingUtilities.invokeLater(() -> {
                    String text = cardNo.getText().replace(" ", "");
                    StringBuilder formatted = new StringBuilder();
                    for (int i = 0; i < text.length(); i++) {
                        if (i > 0 && i % 4 == 0) {
                            formatted.append(" ");
                        }
                        formatted.append(text.charAt(i));
                    }
                    if (!cardNo.getText().equals(formatted.toString())) {
                        cardNo.setText(formatted.toString());
                    }
                });
            }
        });

        JTextField cardName = createTitledField("Cardholder Name", "John Doe", false, 40);
        JTextField cardExp = createTitledField("Expiry (MM/YY)", "MM/YY", true, 5);
        cardExp.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                format();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
            }

            private void format() {
                SwingUtilities.invokeLater(() -> {
                    String text = cardExp.getText();
                    if (text.length() == 2 && !text.contains("/")) {
                        cardExp.setText(text + "/");
                    }
                });
            }
        });

        JTextField cardCvv = createTitledField("CVV", "123", true, 3);

        cardNo.setMaximumSize(new Dimension(300, 45));
        cardName.setMaximumSize(new Dimension(300, 45));

        JPanel smallRow = new JPanel(new GridLayout(1, 2, 10, 0));
        smallRow.setBackground(CLR_BG);
        smallRow.setMaximumSize(new Dimension(300, 45));
        smallRow.add(cardExp);
        smallRow.add(cardCvv);

        cardInfoForm.add(Box.createVerticalStrut(10));
        cardInfoForm.add(cardNo);
        cardInfoForm.add(Box.createVerticalStrut(8));
        cardInfoForm.add(cardName);
        cardInfoForm.add(Box.createVerticalStrut(8));
        cardInfoForm.add(smallRow);

        payMethodCombo.addActionListener(e -> {
            cardInfoForm.setVisible(payMethodCombo.getSelectedIndex() == 0);
            cardInfoForm.revalidate();
            cardInfoForm.repaint();
        });

        s4Top.add(payMethodCombo);
        s4Top.add(cardInfoForm);
        s4Top.add(tipPanel);
        s4Top.add(Box.createVerticalStrut(30));

        JButton btnFinalConfirm = createModernButton("CONFIRM & REQUEST RIDE", CLR_TAXI_YELLOW, CLR_TAXI_BLACK);
        btnFinalConfirm.setAlignmentX(Component.CENTER_ALIGNMENT);

        s4Top.add(btnFinalConfirm);
        step4.add(s4Top, BorderLayout.NORTH);

        JPanel s4Bot = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        s4Bot.setBackground(CLR_BG);
        JButton btnBack3 = createModernButton("Back", CLR_CARD_BG, CLR_TAXI_YELLOW);
        btnBack3.addActionListener(e -> wizardLayout.show(wizardPanel, "STEP3"));
        s4Bot.add(btnBack3);
        step4.add(s4Bot, BorderLayout.SOUTH);

        wizardPanel.add(step4, "STEP4");

        btnRequest.addActionListener(e -> {
            if (comboDrivers.getItemCount() == 0) {
                JOptionPane.showMessageDialog(this, "Click 'Check Price' first!");
                return;
            }
            wizardLayout.show(wizardPanel, "STEP4");
        });

        btnFinalConfirm.addActionListener(e -> {
            List<Driver> allDrivers = manager.getAllDrivers().stream().filter(d -> d.isApproved() && !d.isBanned())
                    .collect(java.util.stream.Collectors.toList());
            int sIdx = comboDrivers.getSelectedIndex();
            if (sIdx < 0)
                return;
            Driver selectedDriver = allDrivers.get(sIdx);
            if (!selectedDriver.isAvailable()) {
                JOptionPane.showMessageDialog(this, "Driver is busy!");
                return;
            }

            if (timingCombo.getSelectedIndex() == 1) { // Schedule Later
                Date chosenDate = dateChooser.getDate();
                String timeStr = timeField.getText();
                String[] parts = timeStr.split(":");
                int hour = Integer.parseInt(parts[0]);
                int minute = Integer.parseInt(parts[1]);

                Calendar chosenCal = Calendar.getInstance();
                chosenCal.setTime(chosenDate);
                chosenCal.set(Calendar.HOUR_OF_DAY, hour);
                chosenCal.set(Calendar.MINUTE, minute);
                chosenCal.set(Calendar.SECOND, 0);

                if (chosenCal.getTime().before(new Date())) {
                    JOptionPane.showMessageDialog(this, "You cannot schedule a ride for a past time!");
                    return;
                }
                selectedTime[0] = timeStr;
            }

            if (selectedTime[0].isEmpty()
                    || (!selectedTime[0].equals("NOW") && !selectedTime[0].matches("\\d{1,2}:\\d{2}"))) {
                JOptionPane.showMessageDialog(this, "Please enter a valid time (HH:MM)!");
                return;
            }

            VehicleType vt = vehicleType.getSelectedItem().equals("Premium") ? VehicleType.PREMIUM
                    : vehicleType.getSelectedItem().equals("XL") ? VehicleType.XL : VehicleType.ECONOMY;
            Location loc1;
            Location loc2;
            if (from.getClientProperty("lat") != null && from.getClientProperty("lon") != null) {
                loc1 = new Location((Double) from.getClientProperty("lat"), (Double) from.getClientProperty("lon"));
            } else {
                loc1 = getCoordinates(from.getText());
            }
            if (to.getClientProperty("lat") != null && to.getClientProperty("lon") != null) {
                loc2 = new Location((Double) to.getClientProperty("lat"), (Double) to.getClientProperty("lon"));
            } else {
                loc2 = getCoordinates(to.getText());
            }

            if (payMethodCombo.getSelectedIndex() == 0) {
                if (cardNo.getText().trim().isEmpty() || cardNo.getText().equals("1234 5678 9012 3456") ||
                        cardName.getText().trim().isEmpty() || cardName.getText().equals("John Doe") ||
                        cardExp.getText().trim().isEmpty() || cardExp.getText().equals("MM/YY") ||
                        cardCvv.getText().trim().isEmpty() || cardCvv.getText().equals("123")) {
                    JOptionPane.showMessageDialog(this, "Please enter valid credit card details!");
                    return;
                }
            }

            double tipVal = 0;
            String tipText = tipField.getText();
            if (!tipText.equals("Tip if you want (TL)") && !tipText.trim().isEmpty()) {
                try {
                    tipVal = Double.parseDouble(tipText);
                } catch (Exception ex) {
                }
            }

            PaymentMethod pm = PaymentMethod.CARD;
            if (payMethodCombo.getSelectedIndex() == 1)
                pm = PaymentMethod.CASH;

            currentRide = rideService.requestRide((Passenger) currentUser, loc1, loc2, vt, pm);
            currentRide.setTipAmount(tipVal);
            double total = currentRide.getFareAmount() + tipVal;

            if (pm == PaymentMethod.CARD) {
                JOptionPane.showMessageDialog(this,
                        String.format(
                                "Payment Processed!\nFare: %.1f TL\nTip: %.1f TL\nTotal: %.1f TL charged from card.",
                                currentRide.getFareAmount(), tipVal, total));
            }

            // Always use the driver selected by the user
            currentRide.setDriver(selectedDriver);
            currentRide.setStatus(RideStatus.ACCEPTED);
            rideService.startRide(currentRide);
            cardLayout.show(mainPanel, "RIDE_PANEL");
            updateRidePanel();
        });

        actionPanel.add(Box.createVerticalStrut(20));
        actionPanel.add(btnRequest);

        s3Top.add(actionPanel);

        JPanel s3Bot = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        s3Bot.setBackground(CLR_BG);
        JButton btnBack2 = createModernButton("Back", CLR_CARD_BG, CLR_TAXI_YELLOW);
        btnBack2.addActionListener(e -> wizardLayout.show(wizardPanel, "STEP2"));
        s3Bot.add(btnBack2);

        step3.add(s3Top, BorderLayout.NORTH);
        step3.add(s3Bot, BorderLayout.SOUTH);
        wizardPanel.add(step3, "STEP3");

        wrapper.add(wizardPanel, BorderLayout.CENTER);

        return wrapper;
    }
    // --- END PASSENGER HOME WIZARD ---

    // --- 4. RIDE PANEL ---
    private JLabel lblRideStatus, lblDriverInfo;

    private JPanel createRidePanel() {
        JPanel p = createWrapper();
        p.setBackground(CLR_BG);

        // Title
        JLabel titleLabel = new JLabel("Your Ride is On The Way!", SwingConstants.CENTER);
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(CLR_TAXI_YELLOW);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Animated road panel - fake map view
        JPanel roadPanel = new JPanel() {
            private int roadOffset = 0;
            private int taxiX = 50;
            private Timer animTimer;
            private int[] buildingX = { 30, 120, 210, 300, 390 };
            private int[] buildingH = { 60, 45, 70, 50, 65 };

            {
                setPreferredSize(new Dimension(350, 180));
                setMaximumSize(new Dimension(350, 180));
                setBackground(CLR_CARD_BG);
                setBorder(BorderFactory.createLineBorder(CLR_TAXI_YELLOW, 2));

                // Animation timer
                animTimer = new Timer(50, e -> {
                    roadOffset = (roadOffset + 8) % 40;
                    // Move buildings to simulate movement
                    for (int i = 0; i < buildingX.length; i++) {
                        buildingX[i] -= 3;
                        if (buildingX[i] < -50) {
                            buildingX[i] = 380;
                            buildingH[i] = 40 + (int) (Math.random() * 40);
                        }
                    }
                    repaint();
                });
                animTimer.start();
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Sky gradient
                GradientPaint sky = new GradientPaint(0, 0, new Color(25, 25, 35), 0, h / 2, new Color(45, 45, 55));
                g2.setPaint(sky);
                g2.fillRect(0, 0, w, h / 2 + 20);

                // Buildings silhouette (background)
                g2.setColor(new Color(35, 35, 45));
                for (int i = 0; i < buildingX.length; i++) {
                    int bx = buildingX[i];
                    int bh = buildingH[i];
                    g2.fillRect(bx, h / 2 - bh + 10, 45, bh);
                    // Windows
                    g2.setColor(new Color(255, 220, 100, 80));
                    for (int wy = h / 2 - bh + 18; wy < h / 2; wy += 15) {
                        for (int wx = bx + 8; wx < bx + 40; wx += 15) {
                            g2.fillRect(wx, wy, 6, 8);
                        }
                    }
                    g2.setColor(new Color(35, 35, 45));
                }

                // Road
                g2.setColor(new Color(50, 50, 55));
                g2.fillRect(0, h / 2 + 10, w, h / 2 - 10);

                // Road lines (animated)
                g2.setColor(CLR_TAXI_YELLOW);
                g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0,
                        new float[] { 20, 15 }, roadOffset));
                g2.drawLine(0, h / 2 + 45, w, h / 2 + 45);

                // Side road lines
                g2.setColor(new Color(200, 200, 200));
                g2.setStroke(new BasicStroke(2));
                g2.drawLine(0, h / 2 + 15, w, h / 2 + 15);
                g2.drawLine(0, h - 5, w, h - 5);

                // Taxi car (simple representation)
                int carY = h / 2 + 50;
                // Car body
                g2.setColor(CLR_TAXI_YELLOW);
                g2.fillRoundRect(taxiX, carY, 60, 25, 8, 8);
                // Car top
                g2.fillRoundRect(taxiX + 12, carY - 12, 35, 15, 6, 6);
                // Windows
                g2.setColor(new Color(150, 200, 255));
                g2.fillRect(taxiX + 15, carY - 9, 12, 10);
                g2.fillRect(taxiX + 32, carY - 9, 12, 10);
                // Wheels
                g2.setColor(new Color(30, 30, 30));
                g2.fillOval(taxiX + 8, carY + 18, 14, 14);
                g2.fillOval(taxiX + 40, carY + 18, 14, 14);
                // Taxi sign
                g2.setColor(CLR_TAXI_BLACK);
                g2.setFont(new Font(FONT_FAMILY, Font.BOLD, 8));
                g2.drawString("TAXI", taxiX + 18, carY + 14);
            }
        };
        roadPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Status label with yellow color
        lblRideStatus = new JLabel("Status: -", SwingConstants.CENTER);
        lblRideStatus.setFont(FONT_SUBTITLE);
        lblRideStatus.setForeground(CLR_TAXI_YELLOW);
        lblRideStatus.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Driver info
        lblDriverInfo = new JLabel("Driver Info", SwingConstants.CENTER);
        lblDriverInfo.setFont(FONT_NORMAL);
        lblDriverInfo.setForeground(CLR_TEXT_SECONDARY);
        lblDriverInfo.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Animated progress bar
        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);
        progress.setMaximumSize(new Dimension(300, 8));
        progress.setPreferredSize(new Dimension(300, 8));
        progress.setForeground(CLR_TAXI_YELLOW);
        progress.setBackground(CLR_CARD_BG);
        progress.setBorderPainted(false);
        progress.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Modern finish button
        JButton btnFinish = createModernButton("Complete Ride", CLR_TAXI_YELLOW, CLR_TAXI_BLACK);
        btnFinish.addActionListener(e -> {
            // Navigation only — actual completion is handled by the background auto-timer
            // Driver will go IDLE automatically when est. time passes
            cardLayout.show(mainPanel, "RATING_PANEL");
        });

        p.add(Box.createVerticalStrut(20));
        p.add(titleLabel);
        p.add(Box.createVerticalStrut(20));
        p.add(roadPanel);
        p.add(Box.createVerticalStrut(25));
        p.add(lblRideStatus);
        p.add(Box.createVerticalStrut(8));
        p.add(lblDriverInfo);
        p.add(Box.createVerticalStrut(25));
        p.add(progress);
        p.add(Box.createVerticalStrut(30));
        p.add(btnFinish);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private void updateRidePanel() {
        if (currentRide == null)
            return;
        lblRideStatus.setText("Status: " + currentRide.getStatus());
        String driverName = currentRide.getDriver() != null ? currentRide.getDriver().getName() : "Searching...";
        String carInfo = currentRide.getDriver() != null ? currentRide.getDriver().getVehicleInfo() : "";
        String plate = currentRide.getDriver() != null ? currentRide.getDriver().getPlateNumber() : "";
        lblDriverInfo.setText(String.format(
                "<html><center>Driver: <b>%s</b><br>Car: %s &nbsp;|&nbsp; Plate: %s<br>" +
                        "Fare: <b>%.0f TL</b> &nbsp;|&nbsp; Distance: %.1f km &nbsp;|&nbsp; ETA: ~%d min</center></html>",
                driverName, carInfo, plate,
                currentRide.getFareAmount(), currentRide.getDistanceKm(), currentRide.getEstimatedMinutes()));
    }

    // --- 5. PAYMENT PANEL ---
    private JLabel lblFinalAmount;

    private JPanel createPaymentPanel() {
        JPanel p = createWrapper();
        JLabel icon = new JLabel("CARD", SwingConstants.CENTER);
        icon.setFont(new Font(FONT_FAMILY, Font.BOLD, 40));
        icon.setForeground(CLR_TAXI_YELLOW);
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblFinalAmount = createLabel("- TL", true);
        lblFinalAmount.setFont(new Font(FONT_FAMILY, Font.BOLD, 40));
        lblFinalAmount.setForeground(CLR_TAXI_YELLOW);
        ActionListener payAction = e -> {
            JOptionPane.showMessageDialog(TaxiFinalApp.this, "Payment Successful! Receipt Sent.");
            // Navigation only — actual completion is handled by the background auto-timer
            cardLayout.show(mainPanel, "RATING_PANEL");
        };
        JButton btnCard = createModernButton("Pay with Card", CLR_TAXI_YELLOW, CLR_TAXI_BLACK);
        btnCard.addActionListener(e -> {
            // Stylish card info dialog - DARK THEME
            JPanel cardPanel = new JPanel();
            cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
            cardPanel.setBackground(CLR_CARD_BG);
            cardPanel.setBorder(new EmptyBorder(25, 30, 25, 30));

            // Header with card logo
            JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
            headerPanel.setBackground(CLR_CARD_BG);
            headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Visual credit card icon
            JPanel cardIcon = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    // Card body
                    g2.setColor(new Color(45, 45, 50));
                    g2.fillRoundRect(0, 0, 60, 38, 6, 6);

                    // Chip
                    g2.setColor(new Color(218, 165, 32));
                    g2.fillRoundRect(8, 12, 15, 12, 3, 3);
                    g2.setColor(new Color(180, 140, 25));
                    g2.drawLine(12, 14, 12, 22);
                    g2.drawLine(16, 14, 16, 22);
                    g2.drawLine(20, 14, 20, 22);

                    // Contactless symbol
                    g2.setColor(CLR_TAXI_YELLOW);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawArc(35, 8, 10, 10, 45, 180);
                    g2.drawArc(38, 11, 10, 10, 45, 180);
                    g2.drawArc(41, 14, 10, 10, 45, 180);

                    // Card number hint
                    g2.setFont(new Font("Consolas", Font.BOLD, 6));
                    g2.setColor(new Color(120, 120, 120));
                    g2.drawString("**** **** ****", 8, 32);
                }
            };
            cardIcon.setPreferredSize(new Dimension(60, 38));
            cardIcon.setBackground(CLR_CARD_BG);

            // Title
            JLabel titleLabel = new JLabel("Secure Payment");
            titleLabel.setFont(FONT_SUBTITLE);
            titleLabel.setForeground(CLR_TAXI_YELLOW);

            headerPanel.add(cardIcon);
            headerPanel.add(titleLabel);

            // Accepted cards text
            JLabel acceptedLabel = new JLabel("  Visa â€¢ Mastercard â€¢ AMEX");
            acceptedLabel.setFont(FONT_SMALL);
            acceptedLabel.setForeground(CLR_TEXT_SECONDARY);
            acceptedLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Card Number (digits only)
            JLabel lblCardNo = new JLabel("Card Number");
            lblCardNo.setFont(FONT_SMALL);
            lblCardNo.setForeground(CLR_TEXT_SECONDARY);
            JTextField cardNumber = new JTextField(19);
            cardNumber.setFont(new Font("Consolas", Font.PLAIN, 16));
            cardNumber.setBackground(CLR_BG);
            cardNumber.setForeground(CLR_TEXT_PRIMARY);
            cardNumber.setCaretColor(CLR_TAXI_YELLOW);
            cardNumber.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(CLR_BORDER, 1, true),
                    new EmptyBorder(10, 12, 10, 12)));
            cardNumber.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
            // Digits only filter
            ((javax.swing.text.AbstractDocument) cardNumber.getDocument())
                    .setDocumentFilter(new javax.swing.text.DocumentFilter() {
                        public void insertString(FilterBypass fb, int offset, String string,
                                javax.swing.text.AttributeSet attr) throws javax.swing.text.BadLocationException {
                            if (string.matches("[0-9 ]*") && fb.getDocument().getLength() + string.length() <= 19)
                                super.insertString(fb, offset, string, attr);
                        }

                        public void replace(FilterBypass fb, int offset, int length, String text,
                                javax.swing.text.AttributeSet attrs) throws javax.swing.text.BadLocationException {
                            if (text.matches("[0-9 ]*") && fb.getDocument().getLength() - length + text.length() <= 19)
                                super.replace(fb, offset, length, text, attrs);
                        }
                    });

            // Cardholder (letters only)
            JLabel lblHolder = new JLabel("Cardholder Name");
            lblHolder.setFont(FONT_SMALL);
            lblHolder.setForeground(CLR_TEXT_SECONDARY);
            JTextField cardHolder = new JTextField();
            cardHolder.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            cardHolder.setBackground(CLR_BG);
            cardHolder.setForeground(CLR_TEXT_PRIMARY);
            cardHolder.setCaretColor(CLR_TAXI_YELLOW);
            cardHolder.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(CLR_BORDER, 1, true),
                    new EmptyBorder(10, 12, 10, 12)));
            cardHolder.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
            // Letters only filter
            ((javax.swing.text.AbstractDocument) cardHolder.getDocument())
                    .setDocumentFilter(new javax.swing.text.DocumentFilter() {
                        public void insertString(FilterBypass fb, int offset, String string,
                                javax.swing.text.AttributeSet attr) throws javax.swing.text.BadLocationException {
                            if (string.matches(
                                    "[a-zA-Z\u011F\u00FC\u015F\u0131\u00F6\u00E7\u015E\u00DC\u011E\u0130\u00D6\u00C7 ]*"))
                                super.insertString(fb, offset, string, attr);
                        }

                        public void replace(FilterBypass fb, int offset, int length, String text,
                                javax.swing.text.AttributeSet attrs) throws javax.swing.text.BadLocationException {
                            if (text.matches(
                                    "[a-zA-Z\u011F\u00FC\u015F\u0131\u00F6\u00E7\u015E\u00DC\u011E\u0130\u00D6\u00C7 ]*"))
                                super.replace(fb, offset, length, text, attrs);
                        }
                    });

            // Expiry and CVV side by side - FlowLayout
            JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 30, 0));
            rowPanel.setBackground(CLR_CARD_BG);
            rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
            rowPanel.setPreferredSize(new Dimension(350, 90));
            rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Expiry Date
            JPanel expiryContainer = new JPanel(new BorderLayout(0, 5));
            expiryContainer.setBackground(CLR_CARD_BG);
            JLabel lblExpiry = new JLabel("Expiry Date");
            lblExpiry.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblExpiry.setForeground(CLR_TEXT_SECONDARY);

            JPanel expiryFields = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            expiryFields.setBackground(CLR_CARD_BG);
            String[] months = { "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12" };
            String[] years = { "2024", "2025", "2026", "2027", "2028", "2029", "2030" };
            JComboBox<String> expiryMonth = new JComboBox<>(months);
            expiryMonth.setBackground(CLR_CARD_BG);
            expiryMonth.setForeground(CLR_TAXI_YELLOW);
            expiryMonth.setFont(FONT_SMALL);
            expiryMonth.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                        boolean isSelected, boolean cellHasFocus) {
                    boolean highlighted = isSelected && index >= 0;
                    JPanel panel = new JPanel(new BorderLayout());
                    panel.setBackground(highlighted ? CLR_TAXI_YELLOW : CLR_CARD_BG);
                    panel.setBorder(new EmptyBorder(4, 6, 4, 6));
                    if (value != null) {
                        JLabel label = new JLabel(value.toString());
                        label.setFont(FONT_SMALL);
                        label.setForeground(highlighted ? CLR_TAXI_BLACK : CLR_TAXI_YELLOW);
                        panel.add(label, BorderLayout.WEST);
                    }
                    return panel;
                }
            });
            JComboBox<String> expiryYear = new JComboBox<>(years);
            expiryYear.setBackground(CLR_CARD_BG);
            expiryYear.setForeground(CLR_TAXI_YELLOW);
            expiryYear.setFont(FONT_SMALL);
            expiryYear.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                        boolean isSelected, boolean cellHasFocus) {
                    boolean highlighted = isSelected && index >= 0;
                    JPanel panel = new JPanel(new BorderLayout());
                    panel.setBackground(highlighted ? CLR_TAXI_YELLOW : CLR_CARD_BG);
                    panel.setBorder(new EmptyBorder(4, 6, 4, 6));
                    if (value != null) {
                        JLabel label = new JLabel(value.toString());
                        label.setFont(FONT_SMALL);
                        label.setForeground(highlighted ? CLR_TAXI_BLACK : CLR_TAXI_YELLOW);
                        panel.add(label, BorderLayout.WEST);
                    }
                    return panel;
                }
            });
            expiryMonth.setPreferredSize(new Dimension(60, 30));
            expiryYear.setPreferredSize(new Dimension(75, 30));
            expiryFields.add(expiryMonth);
            JLabel slashLabel = new JLabel("/");
            slashLabel.setForeground(CLR_TEXT_SECONDARY);
            expiryFields.add(slashLabel);
            expiryFields.add(expiryYear);
            expiryContainer.add(lblExpiry, BorderLayout.NORTH);
            expiryContainer.add(expiryFields, BorderLayout.CENTER);

            // CVV
            JPanel cvvContainer = new JPanel();
            cvvContainer.setLayout(new BoxLayout(cvvContainer, BoxLayout.Y_AXIS));
            cvvContainer.setBackground(CLR_CARD_BG);
            JLabel lblCvv = new JLabel("CVV");
            lblCvv.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblCvv.setForeground(CLR_TEXT_SECONDARY);
            JPasswordField cvv = new JPasswordField(4);
            cvv.setFont(new Font("Consolas", Font.PLAIN, 14));
            cvv.setBackground(CLR_BG);
            cvv.setForeground(CLR_TEXT_PRIMARY);
            cvv.setCaretColor(CLR_TAXI_YELLOW);
            cvv.setMaximumSize(new Dimension(70, 32));
            cvv.setPreferredSize(new Dimension(70, 32));
            cvv.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(CLR_BORDER, 1, true),
                    new EmptyBorder(6, 8, 6, 8)));
            // CVV digits only
            ((javax.swing.text.AbstractDocument) cvv.getDocument())
                    .setDocumentFilter(new javax.swing.text.DocumentFilter() {
                        public void insertString(FilterBypass fb, int offset, String string,
                                javax.swing.text.AttributeSet attr) throws javax.swing.text.BadLocationException {
                            if (string.matches("[0-9]*") && fb.getDocument().getLength() + string.length() <= 4)
                                super.insertString(fb, offset, string, attr);
                        }

                        public void replace(FilterBypass fb, int offset, int length, String text,
                                javax.swing.text.AttributeSet attrs) throws javax.swing.text.BadLocationException {
                            if (text.matches("[0-9]*") && fb.getDocument().getLength() - length + text.length() <= 4)
                                super.replace(fb, offset, length, text, attrs);
                        }
                    });
            cvvContainer.add(lblCvv);
            cvvContainer.add(Box.createVerticalStrut(5));
            cvvContainer.add(cvv);

            rowPanel.add(expiryContainer);
            rowPanel.add(cvvContainer);

            // Checkbox
            JCheckBox saveCard = new JCheckBox("Save card for future payments");
            saveCard.setFont(FONT_SMALL);
            saveCard.setBackground(CLR_CARD_BG);
            saveCard.setForeground(CLR_TEXT_SECONDARY);
            saveCard.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Add to panel
            cardPanel.add(headerPanel);
            cardPanel.add(Box.createVerticalStrut(5));
            cardPanel.add(acceptedLabel);
            cardPanel.add(Box.createVerticalStrut(20));
            cardPanel.add(lblCardNo);
            cardPanel.add(Box.createVerticalStrut(5));
            cardPanel.add(cardNumber);
            cardPanel.add(Box.createVerticalStrut(15));
            cardPanel.add(lblHolder);
            cardPanel.add(Box.createVerticalStrut(5));
            cardPanel.add(cardHolder);
            cardPanel.add(Box.createVerticalStrut(20));
            cardPanel.add(rowPanel);
            cardPanel.add(Box.createVerticalStrut(35)); // More space for expiry dropdown
            cardPanel.add(saveCard);
            cardPanel.add(Box.createVerticalStrut(30)); // Bottom space - for dropdown

            cardPanel.setPreferredSize(new Dimension(400, 450));

            int result = JOptionPane.showConfirmDialog(TaxiFinalApp.this, cardPanel,
                    "Secure Payment", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                String cardNoText = cardNumber.getText().replaceAll(" ", "");
                if (cardNoText.length() < 16 || cardHolder.getText().isEmpty() || cvv.getPassword().length < 3) {
                    JOptionPane.showMessageDialog(TaxiFinalApp.this, "Please fill all fields correctly!", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String savedMsg = saveCard.isSelected() ? "\nCard saved for future use." : "";
                JOptionPane.showMessageDialog(TaxiFinalApp.this,
                        "âœ“ Payment Successful!\nReceipt sent to your email." + savedMsg,
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                if (currentRide != null)
                    manager.completeRide(currentRide);
                cardLayout.show(mainPanel, "RATING_PANEL");
            }
        });
        JButton btnCash = createModernButton("Pay with Cash", CLR_BLACK, CLR_STRAW);
        btnCash.addActionListener(payAction);
        JButton btnTip = createModernButton("Add Tip (+20 TL)", CLR_DARK_GRAY, CLR_STRAW);
        btnTip.addActionListener(e -> {
            if (currentRide != null) {
                currentRide.setPrice(currentRide.getPrice() + 20);
                updatePaymentPanel();
            }
        });
        p.add(Box.createVerticalStrut(30));
        p.add(icon);
        p.add(createLabel("Ride Completed", true));
        p.add(Box.createVerticalStrut(20));
        p.add(lblFinalAmount);
        p.add(Box.createVerticalStrut(30));
        p.add(btnTip);
        p.add(Box.createVerticalStrut(15));
        p.add(btnCard);
        p.add(Box.createVerticalStrut(15));
        p.add(btnCash);
        return p;
    }

    private void updatePaymentPanel() {
        if (currentRide == null)
            return;
        double fare = currentRide.getFareAmount() > 0 ? currentRide.getFareAmount() : currentRide.getPrice();
        lblFinalAmount.setText(String.format("%.0f TL", fare));
    }

    // --- 6. RATING PANEL ---
    private JPanel createRatingPanel() {
        JPanel p = createWrapper();
        p.setBackground(CLR_BG);

        // Title
        JLabel titleLabel = new JLabel("Rate Your Driver", SwingConstants.CENTER);
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(CLR_TAXI_YELLOW);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        String[] stars = { "***** (5)", "**** (4)", "*** (3)", "** (2)", "* (1)" };
        JComboBox<String> starBox = new JComboBox<>(stars);
        starBox.setMaximumSize(new Dimension(200, 35));
        starBox.setBackground(CLR_CARD_BG);
        starBox.setForeground(CLR_TAXI_YELLOW);
        starBox.setFont(FONT_NORMAL);
        starBox.setBorder(BorderFactory.createLineBorder(CLR_TAXI_YELLOW, 1));
        starBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                boolean highlighted = isSelected && index >= 0;
                JPanel panel = new JPanel(new BorderLayout());
                panel.setBackground(highlighted ? CLR_TAXI_YELLOW : CLR_CARD_BG);
                panel.setBorder(new EmptyBorder(5, 10, 5, 10));
                if (value != null) {
                    JLabel label = new JLabel(value.toString());
                    label.setFont(FONT_NORMAL);
                    label.setForeground(highlighted ? CLR_TAXI_BLACK : CLR_TAXI_YELLOW);
                    panel.add(label, BorderLayout.WEST);
                }
                return panel;
            }
        });

        JTextField comment = createStyledField();
        comment.setText("Write a comment...");
        comment.setForeground(CLR_TEXT_SECONDARY);
        comment.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                if (comment.getText().equals("Write a comment...")) {
                    comment.setText("");
                    comment.setForeground(CLR_TAXI_YELLOW);
                }
            }

            public void focusLost(java.awt.event.FocusEvent e) {
                if (comment.getText().isEmpty()) {
                    comment.setText("Write a comment...");
                    comment.setForeground(CLR_TEXT_SECONDARY);
                }
            }
        });

        JButton btnSubmit = createModernButton("Submit Rating", CLR_TAXI_YELLOW, CLR_TAXI_BLACK);
        btnSubmit.addActionListener(e -> {
            int s = 5 - starBox.getSelectedIndex();
            String commentText = comment.getText().equals("Write a comment...") ? "" : comment.getText();
            if (currentRide != null)
                rideService.rateDriver(currentRide, s, commentText);
            JOptionPane.showMessageDialog(this, "Thank you! * " + s + "/5");
            wizardLayout.show(wizardPanel, "STEP1");
            cardLayout.show(mainPanel, "PASSENGER_HOME");
        });

        p.add(Box.createVerticalStrut(30));
        p.add(titleLabel);
        p.add(Box.createVerticalStrut(30));
        p.add(starBox);
        p.add(Box.createVerticalStrut(15));
        p.add(comment);
        p.add(Box.createVerticalStrut(20));
        p.add(btnSubmit);
        p.add(Box.createVerticalStrut(30));

        // Customer Support button
        JButton btnSupport = createModernButton("Customer Support", CLR_TAXI_YELLOW, CLR_TAXI_BLACK);
        btnSupport.setFont(FONT_BUTTON);
        btnSupport.addActionListener(e -> cardLayout.show(mainPanel, "SUPPORT_PANEL"));
        p.add(btnSupport);

        return p;
    }

    // --- 7. ADMIN PANEL ---
    private DefaultTableModel driverTableModel;
    private DefaultTableModel ticketTableModel;
    private JTable driverTable;
    private JTable ticketTable;

    private JPanel createAdminPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(CLR_BG);

        // Title
        JLabel titleLabel = new JLabel("Admin Dashboard", SwingConstants.CENTER);
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(CLR_TAXI_YELLOW);
        titleLabel.setBorder(new EmptyBorder(20, 0, 15, 0));
        titleLabel.setOpaque(true);
        titleLabel.setBackground(CLR_BG);
        p.add(titleLabel, BorderLayout.NORTH);

        // Tab panel - Drivers and Tickets tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(CLR_BG);
        tabbedPane.setForeground(CLR_TAXI_YELLOW);
        tabbedPane.setFont(FONT_BUTTON);
        tabbedPane.setOpaque(true);
        tabbedPane.setBorder(null);
        // Override Windows L&F TabbedPane UI to remove white content border
        tabbedPane.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                contentBorderInsets = new Insets(0, 0, 0, 0);
                tabAreaInsets = new Insets(0, 0, 0, 0);
            }

            @Override
            protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
                // Don't paint content border - removes white line
            }

            @Override
            protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h,
                    boolean isSelected) {
                g.setColor(isSelected ? CLR_CARD_BG : CLR_BG);
                g.fillRect(x, y, w, h);
            }

            @Override
            protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h,
                    boolean isSelected) {
                if (isSelected) {
                    g.setColor(CLR_TAXI_YELLOW);
                    g.drawLine(x, y + h - 2, x + w, y + h - 2);
                }
            }

            @Override
            protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex,
                    Rectangle iconRect, Rectangle textRect, boolean isSelected) {
                // Don't paint focus indicator
            }
        });

        // --- Drivers Tab ---
        JPanel driversPanel = new JPanel(new BorderLayout());
        driversPanel.setBackground(CLR_CARD_BG);
        driverTableModel = new DefaultTableModel(
                new String[] { "Name", "Email", "Verified", "Banned", "Avg. Rating", "Votes" }, 0);
        driverTable = new JTable(driverTableModel);
        driverTable.setRowHeight(30);
        driverTable.setBackground(CLR_CARD_BG);
        driverTable.setForeground(Color.WHITE);
        driverTable.setGridColor(CLR_BORDER);
        driverTable.setSelectionBackground(CLR_TAXI_YELLOW);
        driverTable.setSelectionForeground(CLR_TAXI_BLACK);
        driverTable.setFont(FONT_NORMAL);
        driverTable.getTableHeader().setBackground(CLR_TAXI_YELLOW);
        driverTable.getTableHeader().setForeground(CLR_TAXI_BLACK);
        driverTable.getTableHeader().setFont(FONT_BUTTON);
        driverTable.getTableHeader().setOpaque(true);
        // Custom header renderer to override Windows L&F
        driverTable.getTableHeader().setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                JLabel label = new JLabel(value != null ? value.toString() : "");
                label.setFont(FONT_BUTTON);
                label.setBackground(CLR_TAXI_YELLOW);
                label.setForeground(CLR_TAXI_BLACK);
                label.setOpaque(true);
                label.setBorder(new EmptyBorder(8, 10, 8, 10));
                label.setHorizontalAlignment(SwingConstants.LEFT);
                return label;
            }
        });
        JScrollPane driverScrollPane = new JScrollPane(driverTable);
        driverScrollPane.getViewport().setBackground(CLR_CARD_BG);
        driverScrollPane.setBackground(CLR_CARD_BG);
        driverScrollPane.getVerticalScrollBar().setBackground(CLR_CARD_BG);
        driverScrollPane.getHorizontalScrollBar().setBackground(CLR_CARD_BG);
        driverScrollPane.setBorder(BorderFactory.createLineBorder(CLR_BORDER));
        // Fix corner background
        driverScrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, new JPanel() {
            {
                setBackground(CLR_CARD_BG);
            }
        });
        driversPanel.add(driverScrollPane, BorderLayout.CENTER);

        JPanel driverBtnPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        driverBtnPanel.setBackground(CLR_CARD_BG);
        driverBtnPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JButton btnApprove = new JButton("Approve");
        btnApprove.setBackground(CLR_TAXI_YELLOW);
        btnApprove.setForeground(CLR_TAXI_BLACK);
        btnApprove.setFont(FONT_BUTTON);
        btnApprove.setFocusPainted(false);
        btnApprove.setOpaque(true);
        btnApprove.setContentAreaFilled(true);
        btnApprove.setBorderPainted(false);
        btnApprove.setBorder(new EmptyBorder(10, 15, 10, 15));

        JButton btnBan = new JButton("Ban");
        btnBan.setBackground(CLR_TAXI_YELLOW);
        btnBan.setForeground(CLR_ERROR); // Red text for Ban
        btnBan.setFont(FONT_BUTTON);
        btnBan.setFocusPainted(false);
        btnBan.setOpaque(true);
        btnBan.setContentAreaFilled(true);
        btnBan.setBorderPainted(false);
        btnBan.setBorder(new EmptyBorder(10, 15, 10, 15));

        JButton btnViewLicense = new JButton("View License");
        btnViewLicense.setBackground(CLR_TAXI_YELLOW);
        btnViewLicense.setForeground(CLR_TAXI_BLACK);
        btnViewLicense.setFont(FONT_BUTTON);
        btnViewLicense.setFocusPainted(false);
        btnViewLicense.setOpaque(true);
        btnViewLicense.setContentAreaFilled(true);
        btnViewLicense.setBorderPainted(false);
        btnViewLicense.setBorder(new EmptyBorder(10, 15, 10, 15));

        JButton btnViewCriminal = new JButton("Criminal Record");
        btnViewCriminal.setBackground(CLR_TAXI_YELLOW);
        btnViewCriminal.setForeground(CLR_TAXI_BLACK);
        btnViewCriminal.setFont(FONT_BUTTON);
        btnViewCriminal.setFocusPainted(false);
        btnViewCriminal.setOpaque(true);
        btnViewCriminal.setContentAreaFilled(true);
        btnViewCriminal.setBorderPainted(false);
        btnViewCriminal.setBorder(new EmptyBorder(10, 15, 10, 15));

        btnApprove.addActionListener(e -> {
            int row = driverTable.getSelectedRow();
            if (row != -1) {
                String email = (String) driverTableModel.getValueAt(row, 1);
                Driver d = (Driver) manager.getAllDrivers().stream().filter(x -> x.getEmail().equals(email)).findFirst()
                        .orElse(null);
                if (d != null) {
                    manager.approveDriver(d);
                    refreshAdminTables();
                }
            }
        });
        btnBan.addActionListener(e -> {
            int row = driverTable.getSelectedRow();
            if (row != -1) {
                String email = (String) driverTableModel.getValueAt(row, 1);
                Driver d = (Driver) manager.getAllDrivers().stream().filter(x -> x.getEmail().equals(email)).findFirst()
                        .orElse(null);
                if (d != null) {
                    d.setBanned(!d.isBanned());
                    // Update button text and color based on new state
                    if (d.isBanned()) {
                        btnBan.setText("Unban");
                        btnBan.setForeground(CLR_SUCCESS); // Green for Unban
                    } else {
                        btnBan.setText("Ban");
                        btnBan.setForeground(CLR_ERROR); // Red for Ban
                    }
                    refreshAdminTables();
                }
            }
        });

        // Update button based on selected row
        driverTable.getSelectionModel().addListSelectionListener(e -> {
            int row = driverTable.getSelectedRow();
            if (row != -1) {
                String banned = (String) driverTableModel.getValueAt(row, 3);
                if ("YES".equals(banned)) {
                    btnBan.setText("Unban");
                    btnBan.setForeground(CLR_SUCCESS); // Green for Unban
                } else {
                    btnBan.setText("Ban");
                    btnBan.setForeground(CLR_ERROR); // Red for Ban
                }
            }
        });

        // Document viewing
        btnViewLicense.addActionListener(e -> {
            int row = driverTable.getSelectedRow();
            if (row != -1) {
                String email = (String) driverTableModel.getValueAt(row, 1);
                Driver d = manager.getAllDrivers().stream().filter(x -> x.getEmail().equals(email)).findFirst()
                        .orElse(null);
                if (d != null && d.getLicenseDoc() != null) {
                    try {
                        java.awt.Desktop.getDesktop().open(d.getLicenseDoc());
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, "Cannot open file: " + ex.getMessage(), "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "No license document found!", "Info",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a driver first!", "Info",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });

        btnViewCriminal.addActionListener(e -> {
            int row = driverTable.getSelectedRow();
            if (row != -1) {
                String email = (String) driverTableModel.getValueAt(row, 1);
                Driver d = manager.getAllDrivers().stream().filter(x -> x.getEmail().equals(email)).findFirst()
                        .orElse(null);
                if (d != null && d.getCriminalDoc() != null) {
                    try {
                        java.awt.Desktop.getDesktop().open(d.getCriminalDoc());
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, "Cannot open file: " + ex.getMessage(), "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "No criminal record document found!", "Info",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a driver first!", "Info",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });

        driverBtnPanel.add(btnApprove);
        driverBtnPanel.add(btnBan);
        driverBtnPanel.add(btnViewLicense);
        driverBtnPanel.add(btnViewCriminal);
        driversPanel.add(driverBtnPanel, BorderLayout.SOUTH);
        tabbedPane.addTab("Drivers", driversPanel);

        // --- Tickets Tab ---
        JPanel ticketsPanel = new JPanel(new BorderLayout());
        ticketsPanel.setBackground(CLR_CARD_BG);
        ticketTableModel = new DefaultTableModel(new String[] { "User", "Driver", "Type", "Description", "Status" },
                0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Table not editable
            }
        };
        ticketTable = new JTable(ticketTableModel);
        ticketTable.setRowHeight(30);
        ticketTable.setBackground(CLR_CARD_BG);
        ticketTable.setForeground(Color.WHITE);
        ticketTable.setGridColor(CLR_BORDER);
        ticketTable.setSelectionBackground(CLR_TAXI_YELLOW);
        ticketTable.setSelectionForeground(CLR_TAXI_BLACK);
        ticketTable.setFont(FONT_NORMAL);
        ticketTable.getTableHeader().setBackground(CLR_TAXI_YELLOW);
        ticketTable.getTableHeader().setForeground(CLR_TAXI_BLACK);
        ticketTable.getTableHeader().setFont(FONT_BUTTON);
        ticketTable.getTableHeader().setOpaque(true);
        // Custom header renderer for tickets table
        ticketTable.getTableHeader().setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                JLabel label = new JLabel(value != null ? value.toString() : "");
                label.setFont(FONT_BUTTON);
                label.setBackground(CLR_TAXI_YELLOW);
                label.setForeground(CLR_TAXI_BLACK);
                label.setOpaque(true);
                label.setBorder(new EmptyBorder(8, 10, 8, 10));
                label.setHorizontalAlignment(SwingConstants.LEFT);
                return label;
            }
        });
        JScrollPane ticketScrollPane = new JScrollPane(ticketTable);
        ticketScrollPane.getViewport().setBackground(CLR_CARD_BG);
        ticketScrollPane.setBackground(CLR_CARD_BG);
        ticketScrollPane.getVerticalScrollBar().setBackground(CLR_CARD_BG);
        ticketScrollPane.getHorizontalScrollBar().setBackground(CLR_CARD_BG);
        ticketScrollPane.setBorder(BorderFactory.createLineBorder(CLR_BORDER));
        ticketScrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, new JPanel() {
            {
                setBackground(CLR_CARD_BG);
            }
        });
        ticketsPanel.add(ticketScrollPane, BorderLayout.CENTER);

        JPanel ticketBtnPanel = new JPanel();
        ticketBtnPanel.setBackground(CLR_CARD_BG);
        ticketBtnPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

        JButton btnResolve = new JButton("Mark as Resolved");
        btnResolve.setBackground(CLR_TAXI_YELLOW);
        btnResolve.setForeground(CLR_TAXI_BLACK);
        btnResolve.setFont(FONT_BUTTON);
        btnResolve.setFocusPainted(false);
        btnResolve.setOpaque(true);
        btnResolve.setContentAreaFilled(true);
        btnResolve.setBorderPainted(false);
        btnResolve.setBorder(new EmptyBorder(10, 20, 10, 20));
        btnResolve.addActionListener(e -> {
            int row = ticketTable.getSelectedRow();
            if (row != -1) {
                List<SupportTicket> tickets = manager.getTickets();
                if (row < tickets.size()) {
                    tickets.get(row).setResolved(true);
                    refreshAdminTables();
                }
            }
        });
        ticketBtnPanel.add(btnResolve);
        ticketsPanel.add(ticketBtnPanel, BorderLayout.SOUTH);
        tabbedPane.addTab("Tickets", ticketsPanel);

        p.add(tabbedPane, BorderLayout.CENTER);

        // Logout button
        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(CLR_BG);
        btnPanel.setBorder(new EmptyBorder(15, 0, 15, 0));

        JButton btnBack = new JButton("Logout");
        btnBack.setBackground(CLR_TAXI_YELLOW);
        btnBack.setForeground(CLR_TAXI_BLACK);
        btnBack.setFont(FONT_BUTTON);
        btnBack.setFocusPainted(false);
        btnBack.setOpaque(true);
        btnBack.setContentAreaFilled(true);
        btnBack.setBorderPainted(false);
        btnBack.setBorder(new EmptyBorder(12, 35, 12, 35));
        btnBack.addActionListener(e -> cardLayout.show(mainPanel, "LOGIN"));
        btnPanel.add(btnBack);
        p.add(btnPanel, BorderLayout.SOUTH);

        // Auto-refresh when admin panel is shown
        p.addHierarchyListener(e -> {
            if (p.isShowing())
                refreshAdminTables();
        });

        return p;
    }

    private void refreshAdminTables() {
        // Update drivers table
        driverTableModel.setRowCount(0);
        for (Driver d : manager.getAllDrivers()) {
            driverTableModel.addRow(new Object[] {
                    d.getName(),
                    d.getEmail(),
                    d.isApproved() ? "YES" : "NO",
                    d.isBanned() ? "YES" : "NO",
                    String.format("%.1f / 5.0", d.getAverageRating()),
                    d.getRatingCount()
            });
        }

        // Update tickets table
        ticketTableModel.setRowCount(0);
        for (SupportTicket t : manager.getTickets()) {
            ticketTableModel.addRow(new Object[] {
                    t.getUser().getName(),
                    t.getDriver() != null ? t.getDriver().getName() : "N/A",
                    t.getType().toString(),
                    t.getDescription(),
                    t.isResolved() ? "RESOLVED" : "OPEN"
            });
        }
    }

    // --- SUPPORT & DRIVER ---
    private JPanel createSupportPanel() {
        JPanel p = createWrapper();
        p.add(createLabel("Customer Support", true));
        p.add(Box.createVerticalStrut(20));

        // Ticket type selection
        String[] ticketTypes = { "Select Issue Type...", "Driver Issue", "Fare/Pricing Problem", "Lost Item",
                "App Error", "Vehicle Problem", "Late Arrival", "Cleanliness Issue" };
        JComboBox<String> typeCombo = new JComboBox<>(ticketTypes);
        typeCombo.setMaximumSize(new Dimension(280, 40));
        typeCombo.setBackground(CLR_CARD_BG);
        typeCombo.setForeground(CLR_TAXI_YELLOW);
        typeCombo.setFont(FONT_NORMAL);
        typeCombo.setBorder(BorderFactory.createLineBorder(CLR_TAXI_YELLOW));
        typeCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                boolean highlighted = isSelected && index >= 0;
                JPanel panel = new JPanel(new BorderLayout());
                panel.setBackground(highlighted ? CLR_TAXI_YELLOW : CLR_CARD_BG);
                panel.setBorder(new EmptyBorder(6, 10, 6, 10));
                if (value != null) {
                    JLabel label = new JLabel(value.toString());
                    label.setFont(FONT_NORMAL);
                    label.setForeground(highlighted ? CLR_TAXI_BLACK : CLR_TAXI_YELLOW);
                    panel.add(label, BorderLayout.WEST);
                }
                return panel;
            }
        });
        p.add(typeCombo);
        p.add(Box.createVerticalStrut(15));

        // Description field
        JTextField desc = createStyledField();
        desc.setText("Describe issue...");
        desc.setForeground(CLR_TEXT_SECONDARY);
        desc.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                if (desc.getText().equals("Describe issue...")) {
                    desc.setText("");
                    desc.setForeground(CLR_TAXI_YELLOW);
                }
            }

            public void focusLost(java.awt.event.FocusEvent e) {
                if (desc.getText().isEmpty()) {
                    desc.setText("Describe issue...");
                    desc.setForeground(CLR_TEXT_SECONDARY);
                }
            }
        });
        p.add(desc);
        p.add(Box.createVerticalStrut(15));

        JButton btnSend = createModernButton("Send Ticket", CLR_TAXI_YELLOW, CLR_TAXI_BLACK);
        btnSend.addActionListener(e -> {
            // Validation
            if (typeCombo.getSelectedIndex() == 0) {
                JOptionPane.showMessageDialog(this, "Please select an issue type!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String descText = desc.getText().trim();
            if (descText.isEmpty() || descText.equals("Describe issue...")) {
                JOptionPane.showMessageDialog(this, "Please describe your issue!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Determine ticket type
            TicketType ticketType;
            switch (typeCombo.getSelectedIndex()) {
                case 1:
                    ticketType = TicketType.DRIVER_ISSUE;
                    break;
                case 2:
                    ticketType = TicketType.FARE_REVIEW;
                    break;
                case 3:
                    ticketType = TicketType.LOST_ITEM;
                    break;
                case 4:
                    ticketType = TicketType.APP_ERROR;
                    break;
                case 5:
                    ticketType = TicketType.VEHICLE_PROBLEM;
                    break;
                case 6:
                    ticketType = TicketType.LATE_ARRIVAL;
                    break;
                case 7:
                    ticketType = TicketType.CLEANLINESS;
                    break;
                default:
                    ticketType = TicketType.APP_ERROR;
            }

            // Get driver from last ride (if exists)
            Driver ticketDriver = (currentRide != null) ? currentRide.getDriver() : null;

            manager.createTicket(currentUser, ticketDriver, ticketType, descText);
            JOptionPane.showMessageDialog(this, "Ticket Sent! We'll review your issue soon.");
            cardLayout.show(mainPanel, "PASSENGER_HOME");
        });

        JButton btnBack = createModernButton("Back", CLR_TAXI_YELLOW, CLR_TAXI_BLACK);
        btnBack.addActionListener(e -> cardLayout.show(mainPanel, "PASSENGER_HOME"));

        p.add(btnSend);
        p.add(Box.createVerticalStrut(10));
        p.add(btnBack);
        return p;
    }

    private JPanel createDriverHome() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(CLR_BG);

        // Title & Back Button
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(CLR_BG);
        header.setBorder(new EmptyBorder(10, 20, 0, 20));

        JButton btnHeaderBack = new JButton("\u2190 Logout");
        btnHeaderBack.setFont(FONT_SMALL);
        btnHeaderBack.setForeground(CLR_TAXI_YELLOW);
        btnHeaderBack.setBackground(CLR_BG);
        btnHeaderBack.setBorderPainted(false);
        btnHeaderBack.setFocusPainted(false);
        btnHeaderBack.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnHeaderBack.addActionListener(e -> cardLayout.show(mainPanel, "LOGIN"));
        header.add(btnHeaderBack, BorderLayout.WEST);

        JLabel title = new JLabel("Driver Dashboard", SwingConstants.CENTER);
        title.setFont(FONT_TITLE);
        title.setForeground(CLR_TAXI_YELLOW);
        header.add(title, BorderLayout.CENTER);

        p.add(header, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(CLR_BG);

        String[] columns = { "Passenger", "Route", "Distance", "Fare", "Tip" };
        DefaultTableModel requestModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Real Data from Manager
        List<Ride> activeRides = manager.getRides().stream()
                .filter(r -> r.getStatus() == RideStatus.REQUESTED)
                .collect(java.util.stream.Collectors.toList());

        for (Ride r : activeRides) {
            requestModel.addRow(new Object[] {
                    r.getPassenger().getName(),
                    r.getStartLoc() + " -> " + r.getEndLoc(),
                    String.format("%.1f km", r.getDistanceKm()),
                    String.format("%.0f TL", r.getFareAmount()),
                    String.format("%.0f TL", r.getTipAmount())
            });
        }

        // Add some realistic fallback data if no real rides
        if (activeRides.isEmpty()) {
            requestModel.addRow(new Object[] { "Ahmet Y.", "Taksim -> Besiktas", "4.2 km", "60 TL", "10 TL" });
            requestModel.addRow(new Object[] { "Selma K.", "Kadikoy -> Moda", "2.1 km", "45 TL", "5 TL" });
            requestModel.addRow(new Object[] { "Burak T.", "Sisli -> Levent", "6.5 km", "85 TL", "15 TL" });
        }

        JTable requestTable = new JTable(requestModel);
        requestTable.setRowHeight(40);
        requestTable.setFont(FONT_NORMAL);
        requestTable.setBackground(CLR_CARD_BG);
        requestTable.setForeground(Color.WHITE);
        requestTable.setGridColor(CLR_BORDER);
        requestTable.setSelectionBackground(CLR_TAXI_YELLOW);
        requestTable.setSelectionForeground(CLR_TAXI_BLACK);
        requestTable.getTableHeader().setFont(FONT_BUTTON);
        requestTable.getTableHeader().setBackground(CLR_TAXI_YELLOW);
        requestTable.getTableHeader().setForeground(CLR_TAXI_BLACK);
        requestTable.getTableHeader().setOpaque(true);
        // Custom header renderer to override Windows L&F
        requestTable.getTableHeader().setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                JLabel label = new JLabel(value != null ? value.toString() : "");
                label.setFont(FONT_BUTTON);
                label.setBackground(CLR_TAXI_YELLOW);
                label.setForeground(CLR_TAXI_BLACK);
                label.setOpaque(true);
                label.setBorder(new EmptyBorder(8, 10, 8, 10));
                label.setHorizontalAlignment(SwingConstants.LEFT);
                return label;
            }
        });
        requestTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(requestTable);
        scrollPane.getViewport().setBackground(CLR_CARD_BG);
        scrollPane.setBackground(CLR_CARD_BG);
        scrollPane.getVerticalScrollBar().setBackground(CLR_CARD_BG);
        scrollPane.getHorizontalScrollBar().setBackground(CLR_CARD_BG);
        scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, new JPanel() {
            {
                setBackground(CLR_CARD_BG);
            }
        });
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(CLR_TAXI_YELLOW), "Pending Ride Requests",
                javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.TOP,
                FONT_SMALL, CLR_TAXI_YELLOW));
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        p.add(centerPanel, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        btnPanel.setBackground(CLR_BG);

        JButton btnAccept = createModernButton("Accept Ride", CLR_TAXI_YELLOW, CLR_TAXI_BLACK);
        JButton btnDecline = createModernButton("Decline", CLR_CARD_BG, CLR_TAXI_YELLOW);
        JButton btnEarnings = createModernButton("My Earnings", CLR_CARD_BG, CLR_TAXI_YELLOW);
        JButton btnLogout = createModernButton("Logout", CLR_TAXI_YELLOW, CLR_TAXI_BLACK);
        btnDecline.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(CLR_TAXI_YELLOW, 1, true),
                new EmptyBorder(12, 28, 12, 28)));
        btnEarnings.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(CLR_TAXI_YELLOW, 1, true),
                new EmptyBorder(12, 28, 12, 28)));
        btnEarnings.addActionListener(e -> cardLayout.show(mainPanel, "EARNINGS_HISTORY"));

        btnAccept.addActionListener(e -> {
            int row = requestTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select a ride request first!", "Info",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String passenger = (String) requestModel.getValueAt(row, 0);
            String route = (String) requestModel.getValueAt(row, 1);
            String distance = (String) requestModel.getValueAt(row, 2);
            String fare = (String) requestModel.getValueAt(row, 3);
            String tip = (String) requestModel.getValueAt(row, 4);

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Accept this ride?\n\nPassenger: " + passenger + "\nRoute: " + route +
                            "\nDistance: " + distance + "\nFare: " + fare + "\nTip: " + tip,
                    "Confirm Ride", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                Ride selectedRide;
                if (row < activeRides.size()) {
                    selectedRide = activeRides.get(row);
                } else {
                    // Create a mock ride for fallback data
                    String[] pts = route.split(" -> ");
                    selectedRide = new Ride(new Passenger(passenger, "mock@mail.com", "123", "555"),
                            pts.length > 0 ? pts[0] : "Start",
                            pts.length > 1 ? pts[1] : "End", "Now");
                    try {
                        selectedRide.setFareAmount(Double.parseDouble(fare.replaceAll("[^0-9.]", "")));
                        selectedRide.setTipAmount(Double.parseDouble(tip.replaceAll("[^0-9.]", "")));
                        selectedRide.setDistanceKm(Double.parseDouble(distance.replaceAll("[^0-9.]", "")));
                    } catch (Exception ex) {
                    }
                }
                // Set the current logged-in driver on the ride
                if (currentUser instanceof Driver) {
                    selectedRide.setDriver((Driver) currentUser);
                    driverService.updateDriverStatus((Driver) currentUser, DriverStatus.IN_RIDE);
                }
                showRideInProgress(selectedRide);
                // Remove request after ride ends
                requestModel.removeRow(row);
            }
        });

        btnDecline.addActionListener(e -> {
            int row = requestTable.getSelectedRow();
            if (row != -1) {
                requestModel.removeRow(row);
                JOptionPane.showMessageDialog(this, "Ride declined.", "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        btnLogout.addActionListener(e -> cardLayout.show(mainPanel, "LOGIN"));

        btnPanel.add(btnAccept);
        btnPanel.add(btnDecline);
        btnPanel.add(btnEarnings);
        btnPanel.add(btnLogout);
        p.add(btnPanel, BorderLayout.SOUTH);

        return p;
    }

    private void showRideInProgress(Ride ride) {
        String passenger = ride.getPassenger().getName();
        String route = ride.getStartLoc() + " -> " + ride.getEndLoc();
        String fare = String.format("%.0f TL", ride.getFareAmount() + ride.getTipAmount());
        // Ride simulation dialog
        JDialog rideDialog = new JDialog(this, "Ride In Progress", true);
        rideDialog.setSize(450, 400);
        rideDialog.setLocationRelativeTo(this);
        rideDialog.setLayout(new BorderLayout());
        rideDialog.getContentPane().setBackground(CLR_BG);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(new EmptyBorder(25, 25, 25, 25));
        infoPanel.setBackground(CLR_BG);

        JLabel lblTitle = new JLabel("Ride in Progress");
        lblTitle.setFont(FONT_TITLE);
        lblTitle.setForeground(CLR_TAXI_YELLOW);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblPassenger = new JLabel("Passenger: " + passenger);
        lblPassenger.setFont(FONT_NORMAL);
        lblPassenger.setForeground(CLR_TEXT_PRIMARY);
        lblPassenger.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblRoute = new JLabel("Route: " + route);
        lblRoute.setFont(FONT_NORMAL);
        lblRoute.setForeground(CLR_TEXT_SECONDARY);
        lblRoute.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblFare = new JLabel("Fare: " + fare);
        lblFare.setFont(FONT_SUBTITLE);
        lblFare.setForeground(CLR_TAXI_YELLOW);
        lblFare.setAlignmentX(Component.CENTER_ALIGNMENT);

        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Driving to destination...");
        progressBar.setForeground(CLR_TAXI_YELLOW);
        progressBar.setBackground(CLR_CARD_BG);
        progressBar.setFont(FONT_SMALL);
        progressBar.setBorderPainted(false);
        progressBar.setMaximumSize(new Dimension(320, 28));

        infoPanel.add(lblTitle);
        infoPanel.add(Box.createVerticalStrut(25));
        infoPanel.add(lblPassenger);
        infoPanel.add(Box.createVerticalStrut(10));
        infoPanel.add(lblRoute);
        infoPanel.add(Box.createVerticalStrut(15));
        infoPanel.add(lblFare);
        infoPanel.add(Box.createVerticalStrut(30));
        infoPanel.add(progressBar);

        JButton btnEndRide = new JButton("End Ride & Collect Payment") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        btnEndRide.setBackground(CLR_TAXI_YELLOW);
        btnEndRide.setForeground(CLR_TAXI_BLACK);
        btnEndRide.setFont(FONT_BUTTON);
        btnEndRide.setFocusPainted(false);
        btnEndRide.setBorderPainted(false);
        btnEndRide.setContentAreaFilled(false);
        btnEndRide.setOpaque(false);
        btnEndRide.setBorder(new EmptyBorder(12, 25, 12, 25));
        btnEndRide.setEnabled(false);

        JButton btnCancelRide = createModernButton("Cancel / Back", CLR_CARD_BG, CLR_TAXI_YELLOW);
        btnCancelRide.setBorder(new CompoundBorder(new LineBorder(CLR_TAXI_YELLOW), new EmptyBorder(10, 20, 10, 20)));
        btnCancelRide.addActionListener(e -> {
            rideDialog.dispose();
        });

        JPanel btnPanelDialog = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        btnPanelDialog.setBackground(CLR_BG);
        btnPanelDialog.setBorder(new EmptyBorder(15, 0, 20, 0));
        btnPanelDialog.add(btnCancelRide);
        btnPanelDialog.add(btnEndRide);

        rideDialog.add(infoPanel, BorderLayout.CENTER);
        rideDialog.add(btnPanelDialog, BorderLayout.SOUTH);

        // Simulation timer
        Timer timer = new Timer(50, null);
        final int[] progress = { 0 };
        timer.addActionListener(evt -> {
            progress[0] += 5;
            progressBar.setValue(progress[0]);

            if (progress[0] < 30) {
                progressBar.setString("Picking up passenger...");
            } else if (progress[0] < 70) {
                progressBar.setString("Driving to destination...");
            } else if (progress[0] < 100) {
                progressBar.setString("Almost there...");
            } else {
                progressBar.setString("Arrived at destination!");
                btnEndRide.setEnabled(true);
                timer.stop();
            }
        });
        timer.start();

        btnEndRide.addActionListener(evt -> {
            timer.stop();
            rideDialog.dispose();

            // Navigation only — actual completion is handled by the background auto-timer
            double total = ride.getFareAmount() + ride.getTipAmount();
            JOptionPane.showMessageDialog(this,
                    String.format(
                            "Ride in progress!\nTotal Fare: %.1f TL\n(Fare: %.1f + Tip: %.1f)\n\nDriver will be available after est. time.",
                            total, ride.getFareAmount(), ride.getTipAmount()),
                    "Ride Info", JOptionPane.INFORMATION_MESSAGE);
        });

        rideDialog.setVisible(true);
    }

    private void showPaymentCollection(String passenger, String fare) {
        String[] paymentOptions = { "Cash", "Card (Already Charged)", "In-App Wallet" };

        JPanel paymentPanel = new JPanel();
        paymentPanel.setLayout(new BoxLayout(paymentPanel, BoxLayout.Y_AXIS));
        paymentPanel.setBorder(new EmptyBorder(20, 35, 20, 35));
        paymentPanel.setBackground(CLR_CARD_BG);

        JLabel lblSuccess = new JLabel("Ride completed successfully!");
        lblSuccess.setFont(FONT_SUBTITLE);
        lblSuccess.setForeground(CLR_TAXI_YELLOW);
        lblSuccess.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblPassenger = new JLabel("Passenger: " + passenger);
        lblPassenger.setFont(FONT_NORMAL);
        lblPassenger.setForeground(CLR_TEXT_PRIMARY);
        lblPassenger.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblFare = new JLabel("Total Fare: " + fare);
        lblFare.setFont(FONT_TITLE);
        lblFare.setForeground(CLR_TAXI_YELLOW);
        lblFare.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblMethod = new JLabel("Payment Method:");
        lblMethod.setFont(FONT_NORMAL);
        lblMethod.setForeground(CLR_TEXT_SECONDARY);
        lblMethod.setAlignmentX(Component.CENTER_ALIGNMENT);

        JComboBox<String> paymentCombo = new JComboBox<>(paymentOptions);
        paymentCombo.setAlignmentX(Component.CENTER_ALIGNMENT);
        paymentCombo.setMaximumSize(new Dimension(220, 35));
        paymentCombo.setBackground(CLR_CARD_BG);
        paymentCombo.setForeground(CLR_TAXI_YELLOW);
        paymentCombo.setFont(FONT_NORMAL);
        paymentCombo.setBorder(BorderFactory.createLineBorder(CLR_TAXI_YELLOW, 1));
        paymentCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                boolean highlighted = isSelected && index >= 0;
                JPanel panel = new JPanel(new BorderLayout());
                panel.setBackground(highlighted ? CLR_TAXI_YELLOW : CLR_CARD_BG);
                panel.setBorder(new EmptyBorder(6, 10, 6, 10));
                if (value != null) {
                    JLabel label = new JLabel(value.toString());
                    label.setFont(FONT_NORMAL);
                    label.setForeground(highlighted ? CLR_TAXI_BLACK : CLR_TAXI_YELLOW);
                    panel.add(label, BorderLayout.WEST);
                }
                return panel;
            }
        });

        paymentPanel.add(lblSuccess);
        paymentPanel.add(Box.createVerticalStrut(15));
        paymentPanel.add(lblPassenger);
        paymentPanel.add(Box.createVerticalStrut(8));
        paymentPanel.add(lblFare);
        paymentPanel.add(Box.createVerticalStrut(25));
        paymentPanel.add(lblMethod);
        paymentPanel.add(Box.createVerticalStrut(8));
        paymentPanel.add(paymentCombo);

        int result = JOptionPane.showConfirmDialog(this, paymentPanel,
                "Collect Payment", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String method = (String) paymentCombo.getSelectedItem();

            // Tip option
            int tipChoice = JOptionPane.showConfirmDialog(this,
                    "Did the passenger leave a tip?",
                    "Tip", JOptionPane.YES_NO_OPTION);

            String tipMsg = "";
            if (tipChoice == JOptionPane.YES_OPTION) {
                String tipAmount = JOptionPane.showInputDialog(this, "Enter tip amount (TL):", "20");
                if (tipAmount != null && !tipAmount.isEmpty()) {
                    tipMsg = "\nTip received: " + tipAmount + " TL";
                }
            }

            JOptionPane.showMessageDialog(this,
                    "Payment collected!\n\nFare: " + fare + tipMsg + "\nPayment Method: " + method +
                            "\n\nThank you for driving with OrderATaxi!",
                    "Payment Successful", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // â”€â”€ RIDE HISTORY (Passenger â€” Use Case: "View Ride History")
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private JPanel createRideHistoryPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(CLR_BG);

        JLabel title = new JLabel("My Rides", SwingConstants.CENTER);
        title.setFont(FONT_TITLE);
        title.setForeground(CLR_TAXI_YELLOW);
        title.setBorder(new EmptyBorder(20, 0, 15, 0));
        title.setOpaque(true);
        title.setBackground(CLR_BG);
        p.add(title, BorderLayout.NORTH);

        String[] cols = { "#", "From", "To", "Type", "Fare (TL)", "Status" };
        DefaultTableModel histModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable histTable = new JTable(histModel);
        histTable.setRowHeight(32);
        histTable.setBackground(CLR_CARD_BG);
        histTable.setForeground(Color.WHITE);
        histTable.setGridColor(CLR_BORDER);
        histTable.setSelectionBackground(CLR_TAXI_YELLOW);
        histTable.setSelectionForeground(CLR_TAXI_BLACK);
        histTable.setFont(FONT_NORMAL);
        histTable.getTableHeader().setFont(FONT_BUTTON);
        histTable.getTableHeader().setBackground(CLR_TAXI_YELLOW);
        histTable.getTableHeader().setForeground(CLR_TAXI_BLACK);
        histTable.getTableHeader().setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                JLabel lbl = new JLabel(v != null ? v.toString() : "");
                lbl.setFont(FONT_BUTTON);
                lbl.setBackground(CLR_TAXI_YELLOW);
                lbl.setForeground(CLR_TAXI_BLACK);
                lbl.setOpaque(true);
                lbl.setBorder(new EmptyBorder(8, 10, 8, 10));
                return lbl;
            }
        });

        JScrollPane sp = new JScrollPane(histTable);
        sp.getViewport().setBackground(CLR_CARD_BG);
        sp.setBorder(BorderFactory.createLineBorder(CLR_BORDER));
        p.add(sp, BorderLayout.CENTER);

        JButton btnBack = createModernButton("Back", CLR_TAXI_YELLOW, CLR_TAXI_BLACK);
        JButton btnRefresh = createModernButton("Refresh", CLR_CARD_BG, CLR_TAXI_YELLOW);
        btnRefresh.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(CLR_TAXI_YELLOW, 1, true), new EmptyBorder(12, 28, 12, 28)));

        Runnable refresh = () -> {
            histModel.setRowCount(0);
            List<Ride> rides = manager.getRides();
            int idx = 1;
            for (Ride r : rides) {
                if (currentUser != null && r.getPassenger().getId().equals(currentUser.getId())) {
                    histModel.addRow(new Object[] {
                            idx++,
                            r.getStartLoc(),
                            r.getEndLoc(),
                            r.getVehicleType().name(),
                            String.format("%.0f", r.getFareAmount() > 0 ? r.getFareAmount() : r.getPrice()),
                            r.getStatus().name()
                    });
                }
            }
            if (histModel.getRowCount() == 0)
                histModel.addRow(new Object[] { "-", "No rides yet", "", "", "", "" });
        };

        btnRefresh.addActionListener(e -> refresh.run());
        btnBack.addActionListener(e -> cardLayout.show(mainPanel, "PASSENGER_HOME"));

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        bottom.setBackground(CLR_BG);
        bottom.add(btnRefresh);
        bottom.add(btnBack);
        p.add(bottom, BorderLayout.SOUTH);

        // Refresh on show via panel hierarchy listener
        p.addHierarchyListener(e -> {
            if (p.isShowing())
                refresh.run();
        });
        return p;
    }

    // â”€â”€ EARNINGS HISTORY (Driver â€” Use Case: "View Earnings History")
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private JPanel createEarningsHistoryPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(CLR_BG);

        JLabel title = new JLabel("My Earnings", SwingConstants.CENTER);
        title.setFont(FONT_TITLE);
        title.setForeground(CLR_TAXI_YELLOW);
        title.setBorder(new EmptyBorder(20, 0, 15, 0));
        title.setOpaque(true);
        title.setBackground(CLR_BG);
        p.add(title, BorderLayout.NORTH);

        String[] cols = { "#", "From", "To", "Type", "Fare (TL)", "Rating", "Status" };
        DefaultTableModel earnModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable earnTable = new JTable(earnModel);
        earnTable.setRowHeight(32);
        earnTable.setBackground(CLR_CARD_BG);
        earnTable.setForeground(Color.WHITE);
        earnTable.setGridColor(CLR_BORDER);
        earnTable.setSelectionBackground(CLR_TAXI_YELLOW);
        earnTable.setSelectionForeground(CLR_TAXI_BLACK);
        earnTable.setFont(FONT_NORMAL);
        earnTable.getTableHeader().setFont(FONT_BUTTON);
        earnTable.getTableHeader().setBackground(CLR_TAXI_YELLOW);
        earnTable.getTableHeader().setForeground(CLR_TAXI_BLACK);
        earnTable.getTableHeader().setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                JLabel lbl = new JLabel(v != null ? v.toString() : "");
                lbl.setFont(FONT_BUTTON);
                lbl.setBackground(CLR_TAXI_YELLOW);
                lbl.setForeground(CLR_TAXI_BLACK);
                lbl.setOpaque(true);
                lbl.setBorder(new EmptyBorder(8, 10, 8, 10));
                return lbl;
            }
        });

        // Summary bar
        JLabel lblTotal = new JLabel("Total: 0 TL  |  Completed: 0 rides", SwingConstants.CENTER);
        lblTotal.setFont(FONT_SUBTITLE);
        lblTotal.setForeground(CLR_TAXI_YELLOW);
        lblTotal.setBorder(new EmptyBorder(8, 0, 8, 0));
        lblTotal.setOpaque(true);
        lblTotal.setBackground(CLR_CARD_BG);

        JScrollPane sp = new JScrollPane(earnTable);
        sp.getViewport().setBackground(CLR_CARD_BG);
        sp.setBorder(BorderFactory.createLineBorder(CLR_BORDER));

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(CLR_BG);
        center.add(lblTotal, BorderLayout.NORTH);
        center.add(sp, BorderLayout.CENTER);
        p.add(center, BorderLayout.CENTER);

        JButton btnBack = createModernButton("Back", CLR_TAXI_YELLOW, CLR_TAXI_BLACK);
        JButton btnRefresh = createModernButton("Refresh", CLR_CARD_BG, CLR_TAXI_YELLOW);
        btnRefresh.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(CLR_TAXI_YELLOW, 1, true), new EmptyBorder(12, 28, 12, 28)));

        Runnable refresh = () -> {
            earnModel.setRowCount(0);
            double total = 0;
            int completed = 0;
            int idx = 1;
            for (Ride r : manager.getRides()) {
                Driver d = r.getDriver();
                if (currentUser instanceof Driver && d != null
                        && d.getId().equals(currentUser.getId())) {
                    double fare = r.getFareAmount() > 0 ? r.getFareAmount() : r.getPrice();
                    String rating = r.getRating() > 0 ? "* " + r.getRating() : "-";
                    earnModel.addRow(new Object[] {
                            idx++, r.getStartLoc(), r.getEndLoc(),
                            r.getVehicleType().name(),
                            String.format("%.0f", fare),
                            rating, r.getStatus().name()
                    });
                    if (r.getStatus() == RideStatus.COMPLETED) {
                        total += fare;
                        completed++;
                    }
                }
            }
            if (earnModel.getRowCount() == 0)
                earnModel.addRow(new Object[] { "-", "No rides yet", "", "", "", "", "" });
            lblTotal.setText(String.format("Total: %.0f TL  |  Completed: %d rides", total, completed));
        };

        btnRefresh.addActionListener(e -> refresh.run());
        btnBack.addActionListener(e -> cardLayout.show(mainPanel, "DRIVER_HOME"));

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        bottom.setBackground(CLR_BG);
        bottom.add(btnRefresh);
        bottom.add(btnBack);
        p.add(bottom, BorderLayout.SOUTH);

        p.addHierarchyListener(e -> {
            if (p.isShowing())
                refresh.run();
        });
        return p;
    }

    public static void main(String[] args) {
        // Set dark theme for all Swing components
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            // Dark theme colors
            Color darkBg = new Color(28, 28, 30);
            Color darkBorder = new Color(56, 56, 58);
            Color yellowAccent = new Color(255, 209, 0);
            Color lightText = new Color(220, 220, 220);

            // JOptionPane dark theme
            UIManager.put("OptionPane.background", darkBg);
            UIManager.put("OptionPane.messageForeground", lightText);
            UIManager.put("Panel.background", darkBg);

            // Button styling
            UIManager.put("Button.background", yellowAccent);
            UIManager.put("Button.foreground", new Color(18, 18, 18));

            // TextField styling
            UIManager.put("TextField.background", darkBg);
            UIManager.put("TextField.foreground", lightText);
            UIManager.put("TextField.caretForeground", yellowAccent);

            // ComboBox styling
            UIManager.put("ComboBox.background", darkBg);
            UIManager.put("ComboBox.foreground", yellowAccent);
            UIManager.put("ComboBox.selectionBackground", yellowAccent);
            UIManager.put("ComboBox.selectionForeground", new Color(18, 18, 18));

            // List styling
            UIManager.put("List.background", darkBg);
            UIManager.put("List.foreground", lightText);

            // ScrollPane styling
            UIManager.put("ScrollPane.background", darkBg);
            UIManager.put("Viewport.background", darkBg);

            // TabbedPane styling
            UIManager.put("TabbedPane.background", darkBg);
            UIManager.put("TabbedPane.contentAreaColor", darkBg);
            UIManager.put("TabbedPane.selected", darkBg);
            UIManager.put("TabbedPane.foreground", yellowAccent);
            UIManager.put("TabbedPane.light", darkBg);
            UIManager.put("TabbedPane.shadow", darkBg);
            UIManager.put("TabbedPane.darkShadow", darkBorder);
            UIManager.put("TabbedPane.focus", yellowAccent);

            // Table styling
            UIManager.put("Table.background", darkBg);
            UIManager.put("Table.foreground", yellowAccent);
            UIManager.put("Table.gridColor", darkBorder);
            UIManager.put("TableHeader.background", yellowAccent);
            UIManager.put("TableHeader.foreground", new Color(18, 18, 18));

        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> new TaxiFinalApp().setVisible(true));
    }
}
