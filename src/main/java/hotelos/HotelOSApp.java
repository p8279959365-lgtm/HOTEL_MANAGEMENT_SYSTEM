
package hotelos;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.sql.Connection;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import java.util.prefs.Preferences;

import hotelos.ui.DashboardPanel;
import hotelos.ui.CheckInPanel;
import hotelos.ui.GuestsPanel;
import hotelos.ui.PaymentsPanel;
import hotelos.ui.ReportsPanel;
import hotelos.ui.ReservationsPanel;
import hotelos.ui.RoomsPanel;
import hotelos.ui.NewRoomPanel;
import hotelos.ui.MaintenancePanel;
import hotelos.ui.Theme;

public class HotelOSApp extends JFrame {

    private final CardLayout layout = new CardLayout();
    private final JPanel pages = new JPanel(layout);
    private final JLabel dbStatus = new JLabel("Checking database...");
    private final Preferences preferences = Preferences.userNodeForPackage(HotelOSApp.class);
    private String currentPage = "Dashboard";
    private JButton themeToggle;
    private boolean welcomeMode = true;

    public HotelOSApp() {
        super("HotelOS | Hotel Management");

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1320, 820);
        setMinimumSize(new Dimension(1080, 680));
        setLocationRelativeTo(null);

        Theme.apply(preferences.getBoolean("darkMode", false));
        showWelcomeScreen();
    }

    private void build() {
        pages.removeAll();
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        root.add(sidebar(), BorderLayout.WEST);

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(Theme.BG);

        JPanel top = new JPanel(new BorderLayout(16, 0));
        top.setBackground(Theme.NAVY2);
        top.setBorder(new EmptyBorder(15, 24, 15, 22));

        JLabel brand = new JLabel("HOT E L O S   /   HOTEL MANAGEMENT");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 17));
        brand.setForeground(Color.WHITE);
        top.add(brand, BorderLayout.WEST);

        JPanel right = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 14, 0));
        right.setOpaque(false);
        dbStatus.setForeground(new Color(153, 246, 228));
        dbStatus.setFont(new Font("Segoe UI", Font.BOLD, 13));
        themeToggle = new JButton(Theme.isDark() ? "☀  Light Mode" : "☾  Dark Mode");
        themeToggle.setUI(new BasicButtonUI());
        themeToggle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        themeToggle.setFocusPainted(false);
        themeToggle.setOpaque(true);
        themeToggle.setContentAreaFilled(true);
        themeToggle.setBorderPainted(true);
        themeToggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        themeToggle.setBackground(Theme.isDark() ? new Color(45, 190, 174) : new Color(37, 99, 235));
        themeToggle.setForeground(Color.WHITE);
        themeToggle.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(Theme.isDark() ? new Color(45, 190, 174) : new Color(37, 99, 235)),
                new EmptyBorder(9, 13, 9, 13)));
        themeToggle.addActionListener(e -> toggleTheme());
        right.add(themeToggle);
        right.add(dbStatus);
        top.add(right, BorderLayout.EAST);

        pages.setBackground(Theme.BG);
        pages.add(createPage("Dashboard"), "Dashboard");
        pages.add(createPage("Rooms"), "Rooms");
        pages.add(createPage("New Room"), "New Room");
        pages.add(createPage("Reservations"), "Reservations");
        pages.add(createPage("Guests"), "Guests");
        pages.add(createPage("Payments"), "Payments");
        pages.add(createPage("Reports"), "Reports");

        main.add(top, BorderLayout.NORTH);
        main.add(pages, BorderLayout.CENTER);
        root.add(main, BorderLayout.CENTER);
        setContentPane(root);
        layout.show(pages, currentPage);
        revalidate();
        repaint();
    }

    private void toggleTheme() {
        if (!welcomeMode) currentPage = getCurrentPage();
        Theme.apply(!Theme.isDark());
        preferences.putBoolean("darkMode", Theme.isDark());
        if (welcomeMode) {
            showWelcomeScreen();
        } else {
            build();
            checkDb();
            layout.show(pages, currentPage);
        }
    }

    private void showWelcomeScreen() {
        welcomeMode = true;
        setContentPane(new hotelos.ui.WelcomePanel(this::startApplication, this::toggleTheme, this::navigateFromWelcome));
        revalidate();
        repaint();
    }

    private void startApplication() {
        welcomeMode = false;
        currentPage = "Dashboard";
        build();
        checkDb();
        layout.show(pages, "Dashboard");
    }

    private void navigateFromWelcome(String pageName) {
        welcomeMode = false;
        currentPage = pageName;
        build();
        checkDb();
        showFreshPage(pageName);
    }

    private String getCurrentPage() {
        for (Component component : pages.getComponents()) {
            if (component.isVisible() && component.getName() != null) {
                return component.getName();
            }
        }
        return currentPage;
    }

    private JPanel sidebar() {
        JPanel p = new JPanel();

        p.setPreferredSize(new Dimension(235, 0));
        p.setBackground(Theme.NAVY);
        p.setBorder(new EmptyBorder(25, 14, 20, 14));
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        // Sidebar logo
        JLabel logo = new JLabel("◆ HotelOS");

        logo.setFont(new Font("Segoe UI", Font.BOLD, 25));
        logo.setForeground(new Color(94, 234, 212));
        logo.setBorder(new EmptyBorder(0, 8, 3, 0));

        p.add(logo);

        // Sidebar caption
        JLabel caption = new JLabel("    PROPERTY MANAGEMENT");

        caption.setForeground(new Color(190, 203, 222));
        caption.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        caption.setBorder(new EmptyBorder(7, 0, 28, 0));

        p.add(caption);

        // Navigation items
        String[][] items = {
                {"▦", "Dashboard"},
                {"▤", "Rooms"},
                {"＋", "New Room"},
                {"✓", "Check-in"},
                {"▣", "Reservations"},
                {"⚒", "Maintenance"},
                {"♙", "Guests"},
                {"＄", "Payments"},
                {"▥", "Reports"}
        };

        for (String[] item : items) {
            JButton b = new JButton(
                    item[0] + "    " + item[1]
            );

            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setFont(new Font("Segoe UI", Font.BOLD, 15));
            b.setForeground(Color.WHITE);
            b.setBackground(Theme.NAVY);

            b.setFocusPainted(false);
            b.setBorderPainted(false);
            b.setOpaque(true);
            b.setContentAreaFilled(true);
            b.setForeground(new Color(238, 244, 252));
b.setBackground(Theme.NAVY);
b.setFocusPainted(false);
b.setBorderPainted(false);
b.setOpaque(true);
b.setContentAreaFilled(true);
b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
b.setAlignmentX(Component.LEFT_ALIGNMENT);
b.setBorder(new EmptyBorder(13, 12, 13, 12));

            b.setBorder(
                    new EmptyBorder(14, 12, 14, 8)
            );

            b.setMaximumSize(
                    new Dimension(Integer.MAX_VALUE, 48)
            );

            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.setCursor(
                    Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            );

            // Recreate the selected page so its tables and metrics always
            // read the latest committed values from MySQL.
            b.addActionListener(e -> { currentPage = item[1]; showFreshPage(item[1]); });

            // Hover effect
            b.addMouseListener(new java.awt.event.MouseAdapter() {

                @Override
                public void mouseEntered(
                        java.awt.event.MouseEvent e
                ) {
                    b.setBackground(Theme.NAVY2);
                }

                @Override
                public void mouseExited(
                        java.awt.event.MouseEvent e
                ) {
                    b.setBackground(Theme.NAVY);
                }
            });

            p.add(b);
            p.add(Box.createVerticalStrut(6));
        }

        // Push administrator label to the bottom
        p.add(Box.createVerticalGlue());

        JLabel admin = new JLabel("●  Administrator");
        admin.setForeground(new Color(203, 213, 225));

        p.add(admin);

        return p;
    }


    private JPanel createPage(String name) {
        JPanel page = switch (name) {
            case "Dashboard" -> new DashboardPanel();
            case "Rooms" -> new RoomsPanel();
            case "New Room" -> new NewRoomPanel();
            case "Check-in" -> new CheckInPanel();
            case "Reservations" -> new ReservationsPanel();
            case "Maintenance" -> new MaintenancePanel();
            case "Guests" -> new GuestsPanel();
            case "Payments" -> new PaymentsPanel();
            case "Reports" -> new ReportsPanel();
            default -> throw new IllegalArgumentException("Unknown page: " + name);
        };
        page.setName(name);
        return page;
    }

    private void showFreshPage(String name) {
        Component oldPage = null;
        for (Component component : pages.getComponents()) {
            if (name.equals(component.getName())) {
                oldPage = component;
                break;
            }
        }
        if (oldPage != null) {
            pages.remove(oldPage);
        }
        JPanel freshPage = createPage(name);
        freshPage.setName(name);
        pages.add(freshPage, name);
        layout.show(pages, name);
        pages.revalidate();
        pages.repaint();
    }

    private void checkDb() {
        try (Connection c = DBConnection.getConnection()) {
            dbStatus.setText("● Database connected");

        } catch (Exception e) {
            dbStatus.setText("● Database disconnected");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {

            try {
                UIManager.setLookAndFeel(
                        UIManager.getSystemLookAndFeelClassName()
                );

            } catch (Exception ignored) {
            }

            new HotelOSApp().setVisible(true);
        });
    }
}
