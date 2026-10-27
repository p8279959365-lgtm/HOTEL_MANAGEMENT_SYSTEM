package hotelos.ui;

import hotelos.DBConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.Consumer;

/** Split-layout HotelOS welcome page with live database statistics. */
public final class WelcomePanel extends JPanel {
    private final Runnable onGetStarted;
    private final Runnable onToggleTheme;
    private final Consumer<String> onNavigate;
    private final JLabel dbStatus = new JLabel("● Checking database...");
    private final JLabel roomsValue = new JLabel("—");
    private final JLabel guestsValue = new JLabel("—");
    private final JLabel bookingsValue = new JLabel("—");
    private final JLabel revenueValue = new JLabel("—");
    private final JLabel servicesValue = new JLabel("—");
    private final JLabel amenitiesValue = new JLabel("—");
    private final JButton themeButton = new JButton();

    public WelcomePanel(Runnable onGetStarted, Runnable onToggleTheme, Consumer<String> onNavigate) {
        this.onGetStarted = onGetStarted;
        this.onToggleTheme = onToggleTheme;
        this.onNavigate = onNavigate;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        add(buildTopBar(), BorderLayout.NORTH);

        JPanel body = new JPanel(new GridLayout(1, 2, 0, 0));
        body.setOpaque(false);
        body.add(new HotelIllustration());
        body.add(buildWelcomeContent());
        add(body, BorderLayout.CENTER);
        refreshThemeButton();
        refreshStatistics();
    }

    private JPanel buildTopBar() {
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Theme.NAVY2);
        top.setBorder(new EmptyBorder(15, 24, 15, 24));
        JLabel brand = new JLabel("▦  HotelOS   |   Hotel Management System");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 16));
        brand.setForeground(Color.WHITE);
        top.add(brand, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        right.setOpaque(false);
        dbStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        dbStatus.setForeground(new Color(153, 246, 228));
        themeButton.setFont(Theme.BUTTON);
        themeButton.setFocusPainted(false);
        themeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        themeButton.addActionListener(e -> onToggleTheme.run());
        right.add(themeButton);
        right.add(dbStatus);
        top.add(right, BorderLayout.EAST);
        return top;
    }

    private JPanel buildWelcomeContent() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Theme.BG);
        content.setBorder(new EmptyBorder(30, 34, 28, 34));

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JLabel eyebrow = new JLabel("WELCOME TO YOUR PROPERTY WORKSPACE");
        eyebrow.setFont(new Font("Segoe UI", Font.BOLD, 11));
        eyebrow.setForeground(Theme.TEAL);
        eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel heading = new JLabel("Hotel Management");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 32));
        heading.setForeground(Theme.TEXT);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel heading2 = new JLabel("System");
        heading2.setFont(new Font("Segoe UI", Font.BOLD, 32));
        heading2.setForeground(Theme.TEAL);
        heading2.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel intro = new JLabel("Manage rooms, guests, bookings and services");
        intro.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        intro.setForeground(Theme.MUTED);
        intro.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel intro2 = new JLabel("from one organized workspace.");
        intro2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        intro2.setForeground(Theme.MUTED);
        intro2.setAlignmentX(Component.LEFT_ALIGNMENT);

        center.add(eyebrow);
        center.add(Box.createVerticalStrut(14));
        center.add(heading);
        center.add(heading2);
        center.add(Box.createVerticalStrut(12));
        center.add(intro);
        center.add(intro2);
        center.add(Box.createVerticalStrut(24));

        JPanel stats = new JPanel(new GridLayout(2, 3, 10, 10));
        stats.setOpaque(false);
        stats.setAlignmentX(Component.LEFT_ALIGNMENT);
        stats.add(statCard("▰", "TOTAL ROOMS", roomsValue, Theme.TEAL, "Rooms", "View room inventory"));
        stats.add(statCard("♙", "REGISTERED GUESTS", guestsValue, Theme.BLUE, "Guests", "Open guest records"));
        stats.add(statCard("▦", "ACTIVE BOOKINGS", bookingsValue, Theme.PURPLE, "Reservations", "Manage reservations"));
        stats.add(statCard("✦", "HOTEL SERVICES", servicesValue, Theme.AMBER, "Reports", "View hotel insights"));
        stats.add(statCard("◇", "AMENITIES", amenitiesValue, Theme.TEAL, "Reports", "View hotel insights"));
        stats.add(statCard("₹", "TODAY'S REVENUE", revenueValue, Theme.AMBER, "Payments", "Open payments and balances"));
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));
        center.add(stats);
        center.add(Box.createVerticalStrut(22));

        JLabel servicesTitle = new JLabel("Everything you need to run the hotel");
        servicesTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        servicesTitle.setForeground(Theme.TEXT);
        servicesTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(servicesTitle);
        center.add(Box.createVerticalStrut(10));

        JPanel features = new JPanel(new GridLayout(2, 3, 8, 8));
        features.setOpaque(false);
        features.setAlignmentX(Component.LEFT_ALIGNMENT);
        features.add(feature("▰  Rooms", "Rooms", "View and manage room inventory"));
        features.add(feature("▦  Reservations", "Reservations", "Review current and upcoming bookings"));
        features.add(feature("♙  Guests", "Guests", "Find guest records"));
        features.add(feature("₹  Payments", "Payments", "Record payments and review balances"));
        features.add(feature("✦  Services", "Reports", "Explore hotel performance"));
        features.add(feature("✓  Amenities", "Reports", "Explore hotel insights"));
        features.setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));
        center.add(features);
        center.add(Box.createVerticalStrut(24));

        JButton start = new JButton("Get Started   →");
        start.setFont(new Font("Segoe UI", Font.BOLD, 16));
        start.setForeground(new Color(5, 30, 43));
        start.setBackground(Theme.TEAL);
        start.setOpaque(true);
        start.setContentAreaFilled(true);
        start.setBorderPainted(false);
        start.setFocusPainted(false);
        start.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        start.setBorder(new EmptyBorder(14, 26, 14, 26));
        start.setAlignmentX(Component.LEFT_ALIGNMENT);
        start.addActionListener(e -> onGetStarted.run());
        center.add(start);
        center.add(Box.createVerticalStrut(10));

        JButton refresh = new JButton("↻  Refresh live statistics");
        refresh.setFont(new Font("Segoe UI", Font.BOLD, 12));
        refresh.setForeground(Theme.TEXT);
        refresh.setBackground(Theme.SURFACE);
        refresh.setOpaque(true);
        refresh.setFocusPainted(false);
        refresh.setBorderPainted(true);
        refresh.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER), new EmptyBorder(8, 12, 8, 12)));
        refresh.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refresh.setAlignmentX(Component.LEFT_ALIGNMENT);
        refresh.setToolTipText("Reload counts and today's revenue from MySQL");
        refresh.addActionListener(e -> refreshStatistics());
        center.add(refresh);
        center.add(Box.createVerticalStrut(10));

        JLabel footer = new JLabel("Your hotel. One workspace. Better hospitality.");
        footer.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        footer.setForeground(Theme.MUTED);
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(footer);

        JScrollPane scroll = new JScrollPane(center);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        content.add(scroll, BorderLayout.CENTER);
        return content;
    }

    private JPanel statCard(String icon, String label, JLabel value, Color accent, String target, String tooltip) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Theme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                new EmptyBorder(12, 13, 11, 12)));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setToolTipText(tooltip + " — click to open");
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { onNavigate.accept(target); }
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(accent, 2), new EmptyBorder(11, 12, 10, 11)));
                card.setBackground(Theme.isDark() ? new Color(35, 53, 75) : new Color(239, 250, 250));
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER), new EmptyBorder(12, 13, 11, 12)));
                card.setBackground(Theme.SURFACE);
            }
        });
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Symbol", Font.BOLD, 20));
        iconLabel.setForeground(accent);
        JLabel title = new JLabel(label);
        title.setFont(new Font("Segoe UI", Font.BOLD, 10));
        title.setForeground(Theme.MUTED);
        value.setFont(new Font("Segoe UI", Font.BOLD, 23));
        value.setForeground(Theme.TEXT);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(iconLabel, BorderLayout.WEST);
        top.add(title, BorderLayout.SOUTH);
        card.add(top, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private JPanel feature(String text, String target, String tooltip) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.SURFACE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                new EmptyBorder(8, 7, 8, 7)));
        p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        p.setToolTipText(tooltip + " — click to open");
        p.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { onNavigate.accept(target); }
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                p.setBackground(Theme.isDark() ? new Color(35, 53, 75) : new Color(239, 250, 250));
                p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.TEAL, 2), new EmptyBorder(7, 6, 7, 6)));
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                p.setBackground(Theme.SURFACE);
                p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER), new EmptyBorder(8, 7, 8, 7)));
            }
        });
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setForeground(Theme.TEXT);
        p.add(label, BorderLayout.CENTER);
        return p;
    }

    private void refreshThemeButton() {
        themeButton.setText(Theme.isDark() ? "☀  Light Mode" : "☾  Dark Mode");
        themeButton.setBackground(Theme.TEAL);
        themeButton.setForeground(Theme.isDark() ? new Color(7, 30, 43) : Color.WHITE);
        themeButton.setOpaque(true);
        themeButton.setContentAreaFilled(true);
        themeButton.setBorderPainted(false);
        themeButton.setBorder(new EmptyBorder(9, 13, 9, 13));
    }

    private void refreshStatistics() {
        try (Connection c = DBConnection.getConnection()) {
            roomsValue.setText(String.valueOf(count(c, "SELECT COUNT(*) FROM rooms")));
            guestsValue.setText(String.valueOf(count(c, "SELECT COUNT(*) FROM customers")));
            bookingsValue.setText(String.valueOf(count(c,
                    "SELECT COUNT(*) FROM bookings WHERE LOWER(REPLACE(booking_status,'_',' ')) NOT IN ('checked out','cancelled','completed')")));
            servicesValue.setText(String.valueOf(count(c, "SELECT COUNT(*) FROM services")));
            amenitiesValue.setText(tableExists(c, "amenities") ? String.valueOf(count(c, "SELECT COUNT(*) FROM amenities")) : "N/A");
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COALESCE(SUM(amount),0) FROM payments WHERE payment_date >= CURDATE() AND payment_date < CURDATE() + INTERVAL 1 DAY AND LOWER(payment_status) IN ('paid','completed')");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) revenueValue.setText("₹ " + NumberFormat.getNumberInstance(new Locale("en", "IN")).format(rs.getBigDecimal(1)));
            }
            dbStatus.setText("● Database connected");
            dbStatus.setForeground(new Color(153, 246, 228));
        } catch (Exception ex) {
            dbStatus.setText("● Database unavailable");
            dbStatus.setForeground(new Color(252, 165, 165));
            if ("—".equals(roomsValue.getText())) roomsValue.setText("N/A");
            if ("—".equals(guestsValue.getText())) guestsValue.setText("N/A");
            if ("—".equals(bookingsValue.getText())) bookingsValue.setText("N/A");
            if ("—".equals(servicesValue.getText())) servicesValue.setText("N/A");
            if ("—".equals(amenitiesValue.getText())) amenitiesValue.setText("N/A");
            if ("—".equals(revenueValue.getText())) revenueValue.setText("N/A");
        }
    }

    private boolean tableExists(Connection c, String tableName) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?")) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1) > 0;
            }
        }
    }

    private long count(Connection c, String sql) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    /** Vector-drawn hotel exterior keeps the welcome screen self-contained/offline. */
    private static final class HotelIllustration extends JPanel {
        HotelIllustration() { setOpaque(true); setBackground(new Color(7, 20, 38)); }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            GradientPaint sky = new GradientPaint(0, 0, new Color(5, 19, 39), 0, h, new Color(15, 63, 80));
            g.setPaint(sky); g.fillRect(0, 0, w, h);
            // Soft ambient glow
            g.setColor(new Color(36, 174, 168, 28)); g.fillOval(w / 5, h / 7, w * 3 / 4, h / 2);
            int base = (int)(h * .72);
            // Hotel wings
            g.setColor(new Color(14, 32, 53)); g.fillRoundRect((int)(w*.08), (int)(h*.22), (int)(w*.44), (int)(h*.58), 8, 8);
            g.setColor(new Color(19, 44, 67)); g.fillRoundRect((int)(w*.43), (int)(h*.13), (int)(w*.48), (int)(h*.67), 8, 8);
            // Floor bands
            g.setColor(new Color(68, 103, 122, 140));
            for (int y=(int)(h*.29); y<base; y+=(int)(h*.095)) {
                g.fillRect((int)(w*.09), y, (int)(w*.81), Math.max(2, h/250));
            }
            // Windows
            for (int row=0; row<5; row++) {
                for (int col=0; col<8; col++) {
                    int x=(int)(w*.12)+col*(int)(w*.095);
                    int y=(int)(h*.19)+row*(int)(h*.095);
                    if (x+(int)(w*.035) < w*.9 && y < base) {
                        g.setColor((row+col)%3==0 ? new Color(255, 202, 118, 220) : new Color(67, 117, 139, 210));
                        g.fillRoundRect(x, y, Math.max(8,(int)(w*.035)), Math.max(12,(int)(h*.045)), 4, 4);
                    }
                }
            }
            // Lobby entrance
            g.setColor(new Color(3, 15, 29)); g.fillRoundRect((int)(w*.37), (int)(h*.55), (int)(w*.25), (int)(h*.27), 8, 8);
            g.setColor(new Color(41, 92, 108)); g.fillRect((int)(w*.395), (int)(h*.575), (int)(w*.10), (int)(h*.24));
            g.fillRect((int)(w*.515), (int)(h*.575), (int)(w*.08), (int)(h*.24));
            g.setColor(new Color(255, 202, 118, 200)); g.fillRect((int)(w*.5), (int)(h*.58), 3, (int)(h*.23));
            // Driveway
            Polygon drive = new Polygon();
            drive.addPoint((int)(w*.25), base); drive.addPoint((int)(w*.78), base);
            drive.addPoint(w, h); drive.addPoint(0, h); g.setColor(new Color(18, 33, 49)); g.fillPolygon(drive);
            g.setColor(new Color(45, 190, 174, 150)); g.setStroke(new BasicStroke(2f));
            g.drawLine((int)(w*.5), base, (int)(w*.36), h);
            g.drawLine((int)(w*.59), base, (int)(w*.75), h);
            // Decorative plants
            for (int x : new int[]{(int)(w*.08),(int)(w*.84),(int)(w*.93)}) {
                g.setColor(new Color(8, 35, 39)); g.fillRect(x, (int)(h*.72), 5, (int)(h*.14));
                g.setColor(new Color(19, 117, 96));
                for(int i=0;i<5;i++) g.fillOval(x-18+i*5, (int)(h*.67)-i%2*10, 23, 37);
            }
            // Brand and tagline overlay
            g.setColor(new Color(4, 15, 30, 205)); g.fillRoundRect(28, h-142, Math.min(w-56, 430), 108, 18, 18);
            g.setColor(new Color(94, 234, 212)); g.setFont(new Font("Segoe UI", Font.BOLD, 18));
            g.drawString("HotelOS", 48, h-103);
            g.setColor(Color.WHITE); g.setFont(new Font("Segoe UI", Font.BOLD, 27));
            g.drawString("A better stay starts here.", 48, h-69);
            g.setColor(new Color(199, 216, 232)); g.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            g.drawString("Comfort  •  Service  •  Hospitality", 48, h-46);
            g.dispose();
        }
    }
}
