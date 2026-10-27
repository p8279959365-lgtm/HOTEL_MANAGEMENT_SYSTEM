package hotelos.ui;

import hotelos.DBConnection;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;
import java.awt.*;
import java.io.File;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Database-backed executive overview with responsive filters, charts, search and CSV export. */
public class ReportsPanel extends JPanel {
    private final JTextField fromDate = new JTextField(LocalDate.now().withDayOfMonth(1).toString(), 10);
    private final JTextField toDate = new JTextField(LocalDate.now().toString(), 10);
    private final JTextField search = new JTextField(18);
    private final JComboBox<String> statusFilter = new JComboBox<>(new String[]{"All Statuses", "Paid", "Pending", "Partially Paid"});
    private final JLabel revenueValue = metricValue();
    private final JLabel paymentsValue = metricValue();
    private final JLabel staysValue = metricValue();
    private final JLabel pendingValue = metricValue();
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Payment ID", "Date", "Guest", "Room", "Type", "Amount (₹)", "Status"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable transactions = PanelTools.table();
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
    private final ChartPanel revenueChart = new ChartPanel("Revenue Trend", ChartPanel.Mode.LINE);
    private final ChartPanel occupancyChart = new ChartPanel("Room Occupancy", ChartPanel.Mode.DONUT);
    private final ChartPanel roomRevenueChart = new ChartPanel("Top Room Types by Revenue", ChartPanel.Mode.HORIZONTAL);

    public ReportsPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        JPanel page = PanelTools.page("Reports & Insights",
                "Executive overview of hotel revenue, occupancy and recent transactions.");

        JPanel main = new JPanel(new BorderLayout(0, 12));
        main.setOpaque(false);

        JPanel filters = buildFilters();
        JPanel cards = new JPanel(new GridLayout(1, 4, 12, 12));
        cards.setOpaque(false);
        cards.add(metric("PAID REVENUE", revenueValue, "Paid transactions in selected range", Theme.TEAL));
        cards.add(metric("TOTAL PAYMENTS", paymentsValue, "Payment records in selected range", Theme.BLUE));
        cards.add(metric("COMPLETED STAYS", staysValue, "Bookings checked out in range", Theme.PURPLE));
        cards.add(metric("PENDING BALANCE", pendingValue, "Outstanding booking balances", Theme.AMBER));

        JPanel charts = new JPanel(new GridLayout(1, 2, 12, 12));
        charts.setOpaque(false);
        charts.add(revenueChart);
        charts.add(occupancyChart);

        JPanel lower = new JPanel(new GridLayout(1, 2, 12, 12));
        lower.setOpaque(false);
        lower.add(roomRevenueChart);
        lower.add(buildTransactionsCard());

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        cards.setAlignmentX(Component.LEFT_ALIGNMENT);
        charts.setAlignmentX(Component.LEFT_ALIGNMENT);
        lower.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(cards);
        body.add(Box.createVerticalStrut(12));
        body.add(charts);
        body.add(Box.createVerticalStrut(12));
        body.add(lower);

        // A vertical scroll container prevents controls and charts from being clipped on smaller screens.
        JScrollPane bodyScroll = new JScrollPane(body);
        bodyScroll.setBorder(BorderFactory.createEmptyBorder());
        bodyScroll.setOpaque(false);
        bodyScroll.getViewport().setOpaque(false);
        bodyScroll.getVerticalScrollBar().setUnitIncrement(18);
        bodyScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        main.add(filters, BorderLayout.NORTH);
        main.add(bodyScroll, BorderLayout.CENTER);
        page.add(main, BorderLayout.CENTER);
        add(page, BorderLayout.CENTER);

        loadReport();
    }

    private JPanel buildFilters() {
        JPanel card = PanelTools.card();
        card.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridy = 0;

        addFilter(card, g, 0, "From date", fromDate, 0);
        addFilter(card, g, 1, "To date", toDate, 0);
        addFilter(card, g, 2, "Payment status", statusFilter, 0);

        g.gridy = 1;
        JLabel searchLabel = label("Search guest / room / payment");
        g.gridx = 0; g.weightx = 0; card.add(searchLabel, g);
        styleInput(search);
        g.gridx = 1; g.weightx = 1; card.add(search, g);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton apply = PanelTools.button("Apply Filters", true);
        JButton export = PanelTools.button("Export CSV", false);
        JButton refresh = PanelTools.button("Refresh", false);
        actions.add(apply); actions.add(export); actions.add(refresh);
        g.gridx = 2; g.gridwidth = 2; g.weightx = 0; card.add(actions, g);
        g.gridwidth = 1;

        apply.addActionListener(e -> loadReport());
        refresh.addActionListener(e -> loadReport());
        export.addActionListener(e -> exportCsv());
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterTable(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterTable(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterTable(); }
        });
        statusFilter.addActionListener(e -> filterTable());
        return card;
    }

    private void addFilter(JPanel card, GridBagConstraints g, int x, String title, JComponent field, int row) {
        JPanel pair = new JPanel(new BorderLayout(6, 0));
        pair.setOpaque(false);
        pair.add(label(title), BorderLayout.NORTH);
        if (field instanceof JTextField tf) styleInput(tf);
        else styleCombo((JComboBox<?>) field);
        pair.add(field, BorderLayout.CENTER);
        g.gridx = x; g.gridy = row; g.weightx = 1; g.gridwidth = 1;
        card.add(pair, g);
    }

    private JPanel buildTransactionsCard() {
        JPanel tx = PanelTools.card();
        tx.setLayout(new BorderLayout(0, 8));
        JLabel title = new JLabel("Recent Transactions");
        title.setFont(Theme.HEADING);
        title.setForeground(Theme.TEXT);
        tx.add(title, BorderLayout.NORTH);

        transactions.setModel(tableModel);
        transactions.setRowSorter(sorter);
        transactions.setFillsViewportHeight(true);
        transactions.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        transactions.setRowHeight(34);
        transactions.setPreferredScrollableViewportSize(new Dimension(480, 250));
        JScrollPane scroll = new JScrollPane(transactions);
        scroll.getViewport().setBackground(Theme.SURFACE);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        tx.add(scroll, BorderLayout.CENTER);
        return tx;
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Theme.MUTED);
        l.setFont(Theme.BODY.deriveFont(Font.BOLD, 12f));
        return l;
    }

    private JLabel metricValue() {
        JLabel l = new JLabel("—");
        l.setForeground(Theme.TEXT);
        l.setFont(new Font("Segoe UI", Font.BOLD, 24));
        return l;
    }

    private JPanel metric(String title, JLabel value, String note, Color accent) {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(Theme.SURFACE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                new EmptyBorder(14, 15, 14, 12)));
        JLabel t = label(title);
        t.setForeground(Theme.MUTED);
        p.add(t, BorderLayout.NORTH);
        p.add(value, BorderLayout.CENTER);
        JLabel n = label(note);
        n.setFont(Theme.BODY.deriveFont(11f));
        p.add(n, BorderLayout.SOUTH);
        p.setMinimumSize(new Dimension(120, 125));
        p.setPreferredSize(new Dimension(190, 132));
        return p;
    }

    private void styleInput(JTextField f) {
        f.setFont(Theme.BODY);
        f.setForeground(Theme.TEXT);
        f.setBackground(Theme.INPUT_BG);
        f.setCaretColor(Theme.TEXT);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER), new EmptyBorder(7, 8, 7, 8)));
    }

    private void styleCombo(JComboBox<?> c) {
        c.setFont(Theme.BODY);
        c.setForeground(Theme.TEXT);
        c.setBackground(Theme.INPUT_BG);
        c.setOpaque(true);
        c.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean selected, boolean focus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                l.setOpaque(true);
                l.setBackground(selected ? Theme.TABLE_SELECTED : Theme.INPUT_BG);
                l.setForeground(Theme.TEXT);
                return l;
            }
        });
    }

    private boolean validDates() {
        try {
            LocalDate a = LocalDate.parse(fromDate.getText().trim());
            LocalDate b = LocalDate.parse(toDate.getText().trim());
            if (b.isBefore(a)) throw new IllegalArgumentException();
            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Enter valid dates in YYYY-MM-DD format; To date must not be before From date.",
                    "Invalid Date Range", JOptionPane.WARNING_MESSAGE);
            return false;
        }
    }

    private void loadReport() {
        if (!validDates()) return;
        LocalDate from = LocalDate.parse(fromDate.getText().trim());
        LocalDate to = LocalDate.parse(toDate.getText().trim());
        try (Connection c = DBConnection.getConnection()) {
            String where = "DATE(p.payment_date) BETWEEN ? AND ?";
            revenueValue.setText("₹ " + money(scalar(c,
                    "SELECT COALESCE(SUM(p.amount),0) FROM payments p WHERE p.payment_status='Paid' AND " + where, from, to)));
            paymentsValue.setText(scalar(c,
                    "SELECT COUNT(*) FROM payments p WHERE " + where, from, to));
            staysValue.setText(scalar(c,
                    "SELECT COUNT(*) FROM bookings b WHERE b.booking_status='Checked Out' AND DATE(b.check_out_date) BETWEEN ? AND ?",
                    from, to));
            pendingValue.setText("₹ " + money(scalar(c,
                    "SELECT COALESCE(SUM(GREATEST(0,(GREATEST(1,DATEDIFF(b.check_out_date,b.check_in_date))*COALESCE(r.room_rate,rt.price_per_night)+COALESCE((SELECT SUM(so.total_amount) FROM service_orders so WHERE so.booking_id=b.booking_id),0))-COALESCE((SELECT SUM(p2.amount) FROM payments p2 WHERE p2.booking_id=b.booking_id AND p2.payment_status='Paid'),0))),0) FROM bookings b JOIN rooms r ON r.room_id=b.room_id JOIN room_types rt ON rt.type_id=r.type_id WHERE b.booking_status IN ('Confirmed','Checked In','Checked Out') AND DATE(b.check_in_date)<=? AND DATE(b.check_out_date)>=?",
                    to, from)));
            loadRevenue(c, from, to);
            loadOccupancy(c);
            loadRoomRevenue(c, from, to);
            loadTransactions(c, from, to);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not load reports: " + ex.getMessage(),
                    "Reports Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String scalar(Connection c, String sql, LocalDate a, LocalDate b) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setDate(1, Date.valueOf(a));
            p.setDate(2, Date.valueOf(b));
            try (ResultSet r = p.executeQuery()) { r.next(); return r.getString(1); }
        }
    }

    private String money(String v) {
        try { return String.format("%,.2f", new BigDecimal(v)); }
        catch (Exception e) { return v; }
    }

    private void loadRevenue(Connection c, LocalDate from, LocalDate to) throws SQLException {
        revenueChart.clear();
        String sql = "SELECT DATE(p.payment_date) d, COALESCE(SUM(p.amount),0) total FROM payments p " +
                "WHERE p.payment_status='Paid' AND DATE(p.payment_date) BETWEEN ? AND ? " +
                "GROUP BY DATE(p.payment_date) ORDER BY d";
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setDate(1, Date.valueOf(from)); p.setDate(2, Date.valueOf(to));
            try (ResultSet r = p.executeQuery()) {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM");
                while (r.next()) revenueChart.addPoint(r.getDate("d").toLocalDate().format(fmt), r.getDouble("total"));
            }
        }
        revenueChart.repaint();
    }

    private void loadOccupancy(Connection c) throws SQLException {
        occupancyChart.clear();
        String sql = "SELECT status, COUNT(*) cnt FROM rooms GROUP BY status ORDER BY status";
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery(sql)) {
            while (r.next()) occupancyChart.addPoint(r.getString(1), r.getDouble(2));
        }
        occupancyChart.repaint();
    }

    private void loadRoomRevenue(Connection c, LocalDate from, LocalDate to) throws SQLException {
        roomRevenueChart.clear();
        String sql = "SELECT rt.type_name, COALESCE(SUM(p.amount),0) total FROM room_types rt " +
                "JOIN rooms r ON r.type_id=rt.type_id JOIN bookings b ON b.room_id=r.room_id " +
                "JOIN payments p ON p.booking_id=b.booking_id WHERE p.payment_status='Paid' " +
                "AND DATE(p.payment_date) BETWEEN ? AND ? GROUP BY rt.type_id,rt.type_name ORDER BY total DESC LIMIT 6";
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setDate(1, Date.valueOf(from)); p.setDate(2, Date.valueOf(to));
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) roomRevenueChart.addPoint(r.getString(1), r.getDouble(2));
            }
        }
        roomRevenueChart.repaint();
    }

    private void loadTransactions(Connection c, LocalDate from, LocalDate to) throws SQLException {
        tableModel.setRowCount(0);
        String sql = "SELECT p.payment_id, DATE_FORMAT(p.payment_date,'%Y-%m-%d'), c.name, r.room_number, " +
                "rt.type_name, p.amount, p.payment_status FROM payments p JOIN bookings b ON b.booking_id=p.booking_id " +
                "JOIN customers c ON c.customer_id=b.customer_id JOIN rooms r ON r.room_id=b.room_id " +
                "JOIN room_types rt ON rt.type_id=r.type_id WHERE DATE(p.payment_date) BETWEEN ? AND ? " +
                "ORDER BY p.payment_date DESC,p.payment_id DESC";
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setDate(1, Date.valueOf(from)); p.setDate(2, Date.valueOf(to));
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) tableModel.addRow(new Object[]{r.getInt(1), r.getString(2), r.getString(3),
                        r.getString(4), r.getString(5), money(r.getString(6)), r.getString(7)});
            }
        }
        setColumnWidths();
        filterTable();
    }

    private void setColumnWidths() {
        int[] widths = {70, 95, 145, 65, 100, 100, 105};
        for (int i = 0; i < widths.length; i++) {
            transactions.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    private void filterTable() {
        String text = search.getText().trim();
        List<RowFilter<Object, Object>> filters = new ArrayList<>();
        if (!text.isEmpty()) filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
        String status = (String) statusFilter.getSelectedItem();
        if (status != null && !status.equals("All Statuses"))
            filters.add(RowFilter.regexFilter("(?i)^" + Pattern.quote(status) + "$", 6));
        sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
    }

    private void exportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export report as CSV");
        chooser.setSelectedFile(new File("HotelOS-Report-" + LocalDate.now() + ".csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = chooser.getSelectedFile();
        try (PrintWriter w = new PrintWriter(f, "UTF-8")) {
            for (int c = 0; c < tableModel.getColumnCount(); c++) {
                if (c > 0) w.print(',');
                w.print(csv(tableModel.getColumnName(c)));
            }
            w.println();
            for (int r = 0; r < transactions.getRowCount(); r++) {
                if (r > 0) { /* row separation is handled below */ }
                for (int c = 0; c < transactions.getColumnCount(); c++) {
                    if (c > 0) w.print(',');
                    w.print(csv(String.valueOf(transactions.getValueAt(r, c))));
                }
                w.println();
            }
            JOptionPane.showMessageDialog(this, "Report exported to:\n" + f.getAbsolutePath(),
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String csv(String s) { return "\"" + s.replace("\"", "\"\"") + "\""; }

    private static class ChartPanel extends JPanel {
        enum Mode { LINE, DONUT, HORIZONTAL }
        private final String title;
        private final Mode mode;
        private final List<String> labels = new ArrayList<>();
        private final List<Double> values = new ArrayList<>();

        ChartPanel(String title, Mode mode) {
            this.title = title; this.mode = mode;
            setLayout(new BorderLayout());
            setBackground(Theme.SURFACE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.BORDER), new EmptyBorder(12, 14, 12, 14)));
            setPreferredSize(new Dimension(300, 230));
            setMinimumSize(new Dimension(220, 210));
        }

        void clear() { labels.clear(); values.clear(); }
        void addPoint(String label, double value) { labels.add(label); values.add(value); }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Theme.TEXT);
            g.setFont(new Font("Segoe UI", Font.BOLD, 15));
            g.drawString(title, 14, 23);
            int x = 18, y = 42, w = Math.max(100, getWidth() - 36), h = Math.max(80, getHeight() - 55);
            if (values.isEmpty()) {
                g.setColor(Theme.MUTED);
                g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                g.drawString("No data for selected range", x + 10, y + h / 2);
                g.dispose(); return;
            }
            Color[] colors = {Theme.TEAL, Theme.BLUE, Theme.PURPLE, Theme.AMBER,
                    new Color(236, 72, 153), new Color(14, 165, 233)};
            double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(1);
            if (max <= 0) max = 1;

            if (mode == Mode.DONUT) {
                int size = Math.max(55, Math.min(h - 6, 125));
                int dx = x + 8, dy = y + 4;
                double total = values.stream().mapToDouble(Double::doubleValue).sum();
                if (total <= 0) total = 1;
                int start = 90;
                for (int i = 0; i < values.size(); i++) {
                    int arc = (int) Math.round(values.get(i) / total * 360);
                    g.setColor(colors[i % colors.length]);
                    g.fillArc(dx, dy, size, size, start, arc);
                    start += arc;
                }
                g.setColor(Theme.SURFACE);
                g.fillOval(dx + size / 3, dy + size / 3, size / 3, size / 3);
                int ly = dy + 14;
                for (int i = 0; i < labels.size(); i++) {
                    g.setColor(colors[i % colors.length]); g.fillRoundRect(dx + size + 14, ly - 9, 9, 9, 3, 3);
                    g.setColor(Theme.TEXT); g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    g.drawString(labels.get(i) + "  " + String.format("%.0f%%", values.get(i) / total * 100),
                            dx + size + 29, ly);
                    ly += 20;
                }
            } else if (mode == Mode.HORIZONTAL) {
                int labelW = Math.min(100, Math.max(62, w / 4));
                int barX = x + labelW + 10;
                int barW = Math.max(50, w - labelW - 85);
                int rowH = Math.max(26, Math.min(42, (h - 8) / Math.max(1, values.size())));
                int baseY = y + 6;
                for (int i = 0; i < values.size(); i++) {
                    int yy = baseY + i * rowH;
                    g.setColor(Theme.TEXT); g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    String name = labels.get(i);
                    if (name.length() > 13) name = name.substring(0, 12) + "…";
                    g.drawString(name, x, yy + 15);
                    g.setColor(Theme.TABLE_GRID); g.fillRoundRect(barX, yy + 2, barW, 14, 7, 7);
                    int fill = (int) Math.round(Math.max(0, values.get(i)) / max * barW);
                    g.setColor(colors[i % colors.length]); g.fillRoundRect(barX, yy + 2, fill, 14, 7, 7);
                    g.setColor(Theme.MUTED); g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                    g.drawString(String.format("₹%.0f", values.get(i)), barX + barW + 7, yy + 14);
                }
            } else {
                int left = 38, right = 12, top = y + 6, bottom = y + h - 25;
                int plotW = Math.max(40, w - left - right), plotH = Math.max(35, bottom - top);
                g.setColor(Theme.TABLE_GRID);
                for (int i = 0; i < 4; i++) {
                    int gy = top + i * plotH / 3;
                    g.drawLine(x + left, gy, x + left + plotW, gy);
                }
                if (values.size() == 1) {
                    int px = x + left + plotW / 2;
                    int py = bottom - (int) (values.get(0) / max * plotH);
                    g.setColor(Theme.TEAL); g.fillOval(px - 4, py - 4, 8, 8);
                    g.setColor(Theme.MUTED); g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                    g.drawString(labels.get(0), px - 25, bottom + 16);
                } else {
                    int prevX = -1, prevY = -1;
                    for (int i = 0; i < values.size(); i++) {
                        int px = x + left + (values.size() == 1 ? plotW / 2 : i * plotW / (values.size() - 1));
                        int py = bottom - (int) (values.get(i) / max * plotH);
                        if (prevX >= 0) {
                            g.setColor(Theme.TEAL); g.setStroke(new BasicStroke(2.5f)); g.drawLine(prevX, prevY, px, py);
                        }
                        g.setColor(Theme.TEAL); g.fillOval(px - 4, py - 4, 8, 8);
                        if (values.size() <= 10 || i == 0 || i == values.size() - 1) {
                            g.setColor(Theme.MUTED); g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                            String label = labels.get(i);
                            int tx = Math.max(x, Math.min(x + w - 35, px - 18));
                            g.drawString(label, tx, bottom + 16);
                        }
                        prevX = px; prevY = py;
                    }
                }
            }
            g.dispose();
        }
    }
}
