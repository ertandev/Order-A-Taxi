package pack;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Calendar;

public class TimePickerDialog extends JDialog {
    private int selectedHour = 12;
    private int selectedMinute = 0;
    private boolean isConfirmed = false;
    private boolean pickingHours = true;

    // Minimum time restrictions
    private int minHour = -1;
    private int minMinute = -1;

    private final Color CLR_BG = new Color(18, 18, 18);
    private final Color CLR_CARD = new Color(28, 28, 30);
    private final Color CLR_YELLOW = new Color(255, 209, 0);
    private final Color CLR_TEXT = Color.WHITE;
    private final Color CLR_ERROR = new Color(255, 69, 58);
    private final Color CLR_DISABLED = new Color(60, 60, 60);

    private JLabel lblHourDisplay;
    private JLabel lblMinDisplay;
    private ClockPanel clockPanel;

    public TimePickerDialog(Frame parent, String initialTime, int minHour, int minMinute) {
        super(parent, "Select Time", true);
        this.minHour = minHour;
        this.minMinute = minMinute;
        
        // Parse initial time
        if (initialTime != null && initialTime.contains(":")) {
            try {
                String[] parts = initialTime.split(":");
                selectedHour = Integer.parseInt(parts[0]);
                selectedMinute = Integer.parseInt(parts[1]);
            } catch (Exception ignored) {}
        }
        
        // If initial time is now invalid due to restrictions, adjust it
        if (minHour != -1) {
            if (selectedHour < minHour) {
                selectedHour = minHour;
                selectedMinute = minMinute;
            } else if (selectedHour == minHour && selectedMinute < minMinute) {
                selectedMinute = minMinute;
            }
        }

        setLayout(new BorderLayout());
        getContentPane().setBackground(CLR_BG);
        setResizable(false);

        // Header
        JPanel header = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 20));
        header.setBackground(CLR_BG);

        lblHourDisplay = new JLabel(String.format("%02d", selectedHour), SwingConstants.RIGHT);
        lblHourDisplay.setFont(new Font("Segoe UI", Font.BOLD, 60));
        lblHourDisplay.setForeground(CLR_YELLOW);
        lblHourDisplay.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblHourDisplay.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setMode(true);
            }
        });

        JLabel lblSeparator = new JLabel(":", SwingConstants.CENTER);
        lblSeparator.setFont(new Font("Segoe UI", Font.BOLD, 60));
        lblSeparator.setForeground(CLR_YELLOW);

        lblMinDisplay = new JLabel(String.format("%02d", selectedMinute), SwingConstants.LEFT);
        lblMinDisplay.setFont(new Font("Segoe UI", Font.BOLD, 60));
        lblMinDisplay.setForeground(Color.GRAY);
        lblMinDisplay.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblMinDisplay.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setMode(false);
            }
        });

        header.add(lblHourDisplay);
        header.add(lblSeparator);
        header.add(lblMinDisplay);
        add(header, BorderLayout.NORTH);

        // Clock Face
        clockPanel = new ClockPanel();
        add(clockPanel, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        footer.setBackground(CLR_BG);

        JButton btnCancel = new JButton("Cancel");
        styleFooterBtn(btnCancel, false);
        btnCancel.addActionListener(e -> dispose());

        JButton btnOk = new JButton("Set Time");
        styleFooterBtn(btnOk, true);
        btnOk.addActionListener(e -> {
            isConfirmed = true;
            dispose();
        });

        footer.add(btnCancel);
        footer.add(btnOk);
        add(footer, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(parent);
    }

    private void setMode(boolean hours) {
        this.pickingHours = hours;
        lblHourDisplay.setForeground(hours ? CLR_YELLOW : Color.GRAY);
        lblMinDisplay.setForeground(hours ? Color.GRAY : CLR_YELLOW);
        clockPanel.repaint();
    }

    private void styleFooterBtn(JButton btn, boolean primary) {
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(primary ? Color.BLACK : CLR_YELLOW);
        btn.setBackground(primary ? CLR_YELLOW : CLR_BG);
        btn.setBorder(new LineBorder(CLR_YELLOW, 1, true));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(110, 40));
    }

    public boolean isConfirmed() { return isConfirmed; }
    public String getSelectedTime() { return String.format("%02d:%02d", selectedHour, selectedMinute); }

    private class ClockPanel extends JPanel {
        public ClockPanel() {
            setPreferredSize(new Dimension(320, 320));
            setBackground(CLR_BG);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    handleSelection(e.getPoint(), false);
                }
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (pickingHours) {
                        Timer timer = new Timer(400, ev -> setMode(false));
                        timer.setRepeats(false);
                        timer.start();
                    }
                }
            });
            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    handleSelection(e.getPoint(), false);
                }
            });
        }

        private void handleSelection(Point p, boolean isFinal) {
            double cx = getWidth() / 2.0;
            double cy = getHeight() / 2.0;
            double dx = p.x - cx;
            double dy = p.y - cy;
            double angle = Math.atan2(dy, dx);
            double dist = Math.sqrt(dx*dx + dy*dy);

            if (dist < 20 || dist > 150) return;

            double clockAngle = Math.toDegrees(angle) + 90;
            if (clockAngle < 0) clockAngle += 360;

            if (pickingHours) {
                int hour = (int) Math.round(clockAngle / 30.0);
                if (hour == 0) hour = 12;
                if (dist > 105) {
                    if (hour == 12) hour = 0;
                    else hour += 12;
                }
                
                // Restriction check
                if (minHour != -1 && hour < minHour) return;

                selectedHour = hour;
                lblHourDisplay.setText(String.format("%02d", selectedHour));
            } else {
                int min = (int) Math.round(clockAngle / 6.0);
                if (min == 60) min = 0;
                
                // Restriction check
                if (minHour != -1 && selectedHour == minHour && min < minMinute) return;

                selectedMinute = min;
                lblMinDisplay.setText(String.format("%02d", selectedMinute));
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            int radius = 130;

            // Clock Face
            g2.setColor(CLR_CARD);
            g2.fillOval(cx - radius - 5, cy - radius - 5, (radius + 5) * 2, (radius + 5) * 2);

            // Numbers
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            if (pickingHours) {
                drawClockNumbers(g2, cx, cy, 90, 1, 12, 30);
                drawClockNumbers(g2, cx, cy, 125, 13, 24, 30);
            } else {
                drawClockNumbers(g2, cx, cy, 115, 0, 55, 30);
            }

            // Selection Hand
            double targetValue = pickingHours ? selectedHour : selectedMinute;
            double angle;
            if (pickingHours) {
                angle = Math.toRadians((targetValue % 12) * 30 - 90);
                int r = (selectedHour >= 13 || selectedHour == 0) ? 125 : 90;
                drawHand(g2, cx, cy, angle, r, String.valueOf(selectedHour));
            } else {
                angle = Math.toRadians(targetValue * 6 - 90);
                drawHand(g2, cx, cy, angle, 115, String.valueOf(selectedMinute));
            }

            // Center Dot
            g2.setColor(CLR_YELLOW);
            g2.fillOval(cx - 4, cy - 4, 8, 8);
        }

        private void drawHand(Graphics2D g2, int cx, int cy, double angle, int r, String val) {
            int handX = (int) (cx + Math.cos(angle) * r);
            int handY = (int) (cy + Math.sin(angle) * r);
            g2.setColor(CLR_YELLOW);
            g2.setStroke(new BasicStroke(2));
            g2.drawLine(cx, cy, handX, handY);
            g2.fillOval(handX - 16, handY - 16, 32, 32);
            g2.setColor(Color.BLACK);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(val, handX - fm.stringWidth(val)/2, handY + fm.getAscent()/2 - 2);
        }

        private void drawClockNumbers(Graphics2D g2, int cx, int cy, int r, int start, int end, int stepAngle) {
            for (int i = 0; i < 12; i++) {
                int val;
                if (start == 0) val = i * 5;
                else if (start == 13) val = (i == 11 ? 0 : i + 13);
                else val = i + 1;
                
                double angle = Math.toRadians((i+1) * 30 - 90);
                int x = (int) (cx + Math.cos(angle) * r);
                int y = (int) (cy + Math.sin(angle) * r);

                boolean isSelected = (pickingHours && selectedHour == val) || (!pickingHours && selectedMinute == val);
                boolean isDisabled = false;
                if (minHour != -1) {
                    if (pickingHours && val < minHour) isDisabled = true;
                    if (!pickingHours && selectedHour == minHour && val < minMinute) isDisabled = true;
                }

                if (!isSelected) {
                    g2.setColor(isDisabled ? CLR_DISABLED : Color.GRAY);
                    String s = String.valueOf(val);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(s, x - fm.stringWidth(s)/2, y + fm.getAscent()/2 - 2);
                }
            }
        }
    }
}
