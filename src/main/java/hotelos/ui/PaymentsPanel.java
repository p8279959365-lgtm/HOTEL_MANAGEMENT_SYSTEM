package hotelos.ui;

import hotelos.DBConnection;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class PaymentsPanel extends JPanel {
    private final JTable table = PanelTools.table();
    private final JTable pendingTable = PanelTools.table();
    private final JTextField booking = new JTextField();
    private final JTextField amount = new JTextField();
    private final JComboBox<String> method = new JComboBox<>(new String[]{"UPI", "Cash", "Card", "Bank Transfer"});

    public PaymentsPanel() {
        setLayout(new BorderLayout(0, 14));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(22, 24, 22, 24));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Payments & receipts");
        title.setFont(Theme.TITLE);
        title.setForeground(Theme.TEXT);
        JLabel subtitle = new JLabel("Record full or partial payments, track outstanding balances, and generate guest bills.");
        subtitle.setForeground(Theme.MUTED);
        heading.add(title);
        heading.add(Box.createVerticalStrut(5));
        heading.add(subtitle);
        add(heading, BorderLayout.NORTH);

        JPanel card = PanelTools.card();
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 4));
        form.setOpaque(false);
        styleField(booking, 90);
        styleField(amount, 110);
        styleCombo(method);
        addFormLabel(form, "Booking ID"); form.add(booking);
        addFormLabel(form, "Amount ₹"); form.add(amount);
        form.add(method);

        JButton record = PanelTools.button("Record payment", true);
        JButton balance = PanelTools.button("Check balance", false);
        JButton receipt = PanelTools.button("Print receipt", false);
        JButton downloadBill = PanelTools.button("Generate / Download Bill", true);
        JButton usePending = PanelTools.button("Use selected pending booking", false);
        form.add(record); form.add(balance); form.add(receipt); form.add(downloadBill); form.add(usePending);
        card.add(form, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabs.setForeground(Theme.TEXT);
        tabs.setBackground(Theme.SURFACE);
        tabs.setOpaque(true);
        tabs.setFocusable(false);
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        tabs.setUI(new BasicTabbedPaneUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                lightHighlight = Theme.BORDER;
                highlight = Theme.BORDER;
                shadow = Theme.BORDER;
                darkShadow = Theme.BORDER;
                focus = Theme.TEAL;
            }

            @Override
            protected void paintTabBackground(Graphics g, int placement, int index,
                    int x, int y, int w, int h, boolean selected) {
                g.setColor(selected ? Theme.TABLE_HEADER : Theme.NAVY2);
                g.fillRect(x, y, w, h);
            }

            @Override
            protected void paintText(Graphics g, int placement, Font font, FontMetrics metrics,
                    int index, String title, Rectangle textRect, boolean selected) {
                g.setFont(font);
                g.setColor(selected ? Theme.TEXT : Theme.MUTED);
                super.paintText(g, placement, font, metrics, index, title, textRect, selected);
            }
        });
        tabs.addTab("Pending Payments", PanelTools.scrollPane(pendingTable));
        tabs.addTab("Payment History", PanelTools.scrollPane(table));
        styleTable(pendingTable);
        styleTable(table);
        card.add(tabs, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);

        record.addActionListener(e -> recordPayment());
        balance.addActionListener(e -> showBalance());
        receipt.addActionListener(e -> printReceipt());
        downloadBill.addActionListener(e -> generateBill());
        usePending.addActionListener(e -> useSelectedPending());
        pendingTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && pendingTable.getSelectedRow() >= 0) {
                int row = pendingTable.convertRowIndexToModel(pendingTable.getSelectedRow());
                booking.setText(String.valueOf(pendingTable.getModel().getValueAt(row, 0)));
            }
        });
        refresh();
    }

    private void addFormLabel(JPanel form, String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Theme.TEXT);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        form.add(label);
    }

    private void styleField(JTextField field, int width) {
        field.setPreferredSize(new Dimension(width, 36));
        field.setFont(Theme.BODY);
        field.setForeground(Theme.TEXT);
        field.setBackground(Theme.INPUT_BG);
        field.setCaretColor(Theme.TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER), new EmptyBorder(6, 8, 6, 8)));
    }

    private void styleCombo(JComboBox<?> combo) {
        combo.setFont(Theme.BODY);
        combo.setForeground(Theme.TEXT);
        combo.setBackground(Theme.INPUT_BG);
        combo.setPreferredSize(new Dimension(145, 36));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean selected, boolean focus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                label.setOpaque(true);
                label.setForeground(Theme.TEXT);
                label.setBackground(selected ? Theme.TABLE_SELECTED : Theme.INPUT_BG);
                label.setFont(Theme.BODY);
                label.setBorder(new EmptyBorder(5, 7, 5, 7));
                return label;
            }
        });
    }

    private void styleTable(JTable t) {
        t.setFont(Theme.TABLE);
        t.setForeground(Theme.TEXT);
        t.setBackground(Theme.SURFACE);
        t.setSelectionBackground(Theme.TABLE_SELECTED);
        t.setSelectionForeground(Theme.TEXT);
        t.setRowHeight(42);
        t.setFillsViewportHeight(true);
        t.setShowVerticalLines(false);
        t.setIntercellSpacing(new Dimension(0, 1));
        // Let the table occupy the full viewport width instead of stopping after its columns.
        t.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        t.setFillsViewportHeight(true);
        t.getTableHeader().setReorderingAllowed(false);
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                    boolean focus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                label.setOpaque(true);
                label.setFont(Theme.TABLE);
                label.setForeground(Theme.TEXT);
                label.setBackground(selected ? Theme.TABLE_SELECTED : (row % 2 == 0 ? Theme.SURFACE : Theme.TABLE_ALT));
                label.setBorder(new EmptyBorder(0, 10, 0, 8));
                return label;
            }
        });
    }

    private int bookingId() {
        int id = Integer.parseInt(booking.getText().trim());
        if (id < 1) throw new IllegalArgumentException("Enter a valid Booking ID.");
        return id;
    }

    private BigDecimal bill(Connection c, int id) throws SQLException {
        String sql = "SELECT GREATEST(1,DATEDIFF(b.check_out_date,b.check_in_date)) * COALESCE(r.room_rate,rt.price_per_night) " +
                "FROM bookings b JOIN rooms r ON r.room_id=b.room_id JOIN room_types rt ON rt.type_id=r.type_id WHERE b.booking_id=?";
        BigDecimal roomTotal;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Booking ID not found.");
                roomTotal = rs.getBigDecimal(1);
            }
        }
        BigDecimal services;
        try (PreparedStatement ps = c.prepareStatement("SELECT COALESCE(SUM(total_amount),0) FROM service_orders WHERE booking_id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); services = rs.getBigDecimal(1); }
        }
        return roomTotal.add(services == null ? BigDecimal.ZERO : services);
    }

    private BigDecimal paid(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COALESCE(SUM(amount),0) FROM payments WHERE booking_id=? AND payment_status='Paid'")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getBigDecimal(1); }
        }
    }

    private void recordPayment() {
        try {
            int id = bookingId();
            BigDecimal value = new BigDecimal(amount.getText().trim());
            if (value.signum() <= 0) throw new IllegalArgumentException("Payment amount must be greater than zero.");
            try (Connection c = DBConnection.getConnection()) {
                c.setAutoCommit(false);
                try {
                    BigDecimal total = bill(c, id);
                    BigDecimal alreadyPaid = paid(c, id);
                    BigDecimal due = total.subtract(alreadyPaid);
                    if (due.signum() <= 0) throw new IllegalStateException("This booking is already fully paid. Bill: ₹" + total);
                    if (value.compareTo(due) > 0) throw new IllegalArgumentException("Amount exceeds remaining balance of ₹" + due);
                    String sql = "INSERT INTO payments(booking_id,amount,payment_date,payment_method,payment_status) VALUES(?,?,CURRENT_TIMESTAMP,?,'Paid')";
                    try (PreparedStatement ps = c.prepareStatement(sql)) {
                        ps.setInt(1, id);
                        ps.setBigDecimal(2, value);
                        ps.setString(3, String.valueOf(method.getSelectedItem()));
                        ps.executeUpdate();
                    }
                    c.commit();
                    BigDecimal newPaid = alreadyPaid.add(value);
                    JOptionPane.showMessageDialog(this, "Payment saved successfully.\nBill total: ₹" + total +
                            "\nPaid so far: ₹" + newPaid + "\nBalance due: ₹" + total.subtract(newPaid),
                            "Payment Recorded", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    c.rollback();
                    throw ex;
                } finally { c.setAutoCommit(true); }
            }
            refresh();
        } catch (Exception ex) { PanelTools.error(this, ex); }
    }

    private void showBalance() {
        try (Connection c = DBConnection.getConnection()) {
            int id = bookingId();
            BigDecimal total = bill(c, id);
            BigDecimal paidAmount = paid(c, id);
            JOptionPane.showMessageDialog(this, "Booking #" + id + "\nBill total: ₹" + total +
                    "\nPaid: ₹" + paidAmount + "\nBalance due: ₹" + total.subtract(paidAmount).max(BigDecimal.ZERO),
                    "Booking Balance", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) { PanelTools.error(this, ex); }
    }

    private void useSelectedPending() {
        int row = pendingTable.getSelectedRow();
        if (row < 0) { PanelTools.info(this, "Select a pending booking first."); return; }
        row = pendingTable.convertRowIndexToModel(row);
        booking.setText(String.valueOf(pendingTable.getModel().getValueAt(row, 0)));
        amount.requestFocusInWindow();
    }

    private void printReceipt() {
        int row = table.getSelectedRow();
        if (row < 0) { PanelTools.info(this, "Select a payment row to print its receipt."); return; }
        row = table.convertRowIndexToModel(row);
        String text = "HOT E L O S  |  PAYMENT RECEIPT\n\n"
                + "Payment ID: " + table.getModel().getValueAt(row, 0) + "\n"
                + "Booking ID: " + table.getModel().getValueAt(row, 1) + "\n"
                + "Guest: " + table.getModel().getValueAt(row, 2) + "\n"
                + "Amount: ₹" + table.getModel().getValueAt(row, 3) + "\n"
                + "Method: " + table.getModel().getValueAt(row, 4) + "\n"
                + "Status: " + table.getModel().getValueAt(row, 5) + "\n"
                + "Date: " + table.getModel().getValueAt(row, 6) + "\n";
        JTextArea area = new JTextArea(text);
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        try { area.print(); } catch (Exception ex) { PanelTools.error(this, ex); }
    }

    private void generateBill() {
        try {
            int id = bookingId();
            String html;
            try (Connection c = DBConnection.getConnection()) { html = buildBillHtml(c, id); }
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save HotelOS Bill");
            chooser.setSelectedFile(new java.io.File("HotelOS-Bill-Booking-" + id + ".html"));
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            java.io.File selected = chooser.getSelectedFile();
            if (!selected.getName().toLowerCase().endsWith(".html")) selected = new java.io.File(selected.getParentFile(), selected.getName() + ".html");
            Files.writeString(selected.toPath(), html, StandardCharsets.UTF_8);
            int choice = JOptionPane.showConfirmDialog(this,
                    "Bill saved successfully.\n" + selected.getAbsolutePath() + "\n\nOpen it now to print or save as PDF?",
                    "Bill Generated", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (choice == JOptionPane.YES_OPTION && Desktop.isDesktopSupported()) Desktop.getDesktop().browse(selected.toURI());
        } catch (Exception ex) { PanelTools.error(this, ex); }
    }

    private String buildBillHtml(Connection c, int id) throws SQLException {
        String guest, phone, room, status, checkIn, checkOut, type;
        BigDecimal rate;
        String sql = "SELECT c.name,c.phone,r.room_number,b.booking_status,b.check_in_date,b.check_out_date,rt.type_name,COALESCE(r.room_rate,rt.price_per_night) " +
                "FROM bookings b JOIN customers c ON c.customer_id=b.customer_id JOIN rooms r ON r.room_id=b.room_id " +
                "JOIN room_types rt ON rt.type_id=r.type_id WHERE b.booking_id=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Booking ID not found.");
                guest = rs.getString(1); phone = rs.getString(2); room = rs.getString(3); status = rs.getString(4);
                checkIn = String.valueOf(rs.getDate(5)); checkOut = String.valueOf(rs.getDate(6)); type = rs.getString(7); rate = rs.getBigDecimal(8);
            }
        }
        long nights;
        try { nights = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(checkIn), LocalDate.parse(checkOut))); }
        catch (Exception ex) { nights = 1; }
        BigDecimal roomTotal = rate.multiply(BigDecimal.valueOf(nights));
        BigDecimal services = BigDecimal.ZERO;
        try (PreparedStatement ps = c.prepareStatement("SELECT COALESCE(SUM(total_amount),0) FROM service_orders WHERE booking_id=?")) {
            ps.setInt(1, id); try (ResultSet rs = ps.executeQuery()) { rs.next(); services = rs.getBigDecimal(1); }
        }
        BigDecimal total = roomTotal.add(services == null ? BigDecimal.ZERO : services);
        BigDecimal paidAmount = paid(c, id);
        BigDecimal due = total.subtract(paidAmount).max(BigDecimal.ZERO);
        StringBuilder payments = new StringBuilder();
        try (PreparedStatement ps = c.prepareStatement("SELECT payment_date,payment_method,amount,payment_status FROM payments WHERE booking_id=? ORDER BY payment_id")) {
            ps.setInt(1, id); try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) payments.append("<tr><td>").append(esc(String.valueOf(rs.getTimestamp(1)))).append("</td><td>")
                        .append(esc(rs.getString(2))).append("</td><td>₹").append(rs.getBigDecimal(3)).append("</td><td>")
                        .append(esc(rs.getString(4))).append("</td></tr>");
            }
        }
        return "<!doctype html><html><head><meta charset='utf-8'><title>HotelOS Bill #" + id + "</title><style>" +
                "body{font-family:Arial,sans-serif;background:#f1f5f9;color:#172033;margin:0;padding:32px}.bill{max-width:820px;margin:auto;background:white;padding:38px;border-radius:12px;box-shadow:0 8px 28px #0001}" +
                "header{display:flex;justify-content:space-between;border-bottom:3px solid #2dbeae;padding-bottom:20px}h1{margin:0;color:#0f2744}h2{margin-top:28px;color:#0f2744}table{width:100%;border-collapse:collapse;margin-top:12px}td,th{padding:11px;border-bottom:1px solid #dbe3ee;text-align:left}th{background:#12243d;color:white}.total{font-size:18px;font-weight:bold}.due{color:#b42318;font-size:20px;font-weight:bold}.muted{color:#64748b}.print{padding:10px 16px;background:#18a999;color:white;border:0;border-radius:6px;cursor:pointer}@media print{body{background:white;padding:0}.bill{box-shadow:none}}</style></head><body><main class='bill'><header><div><h1>HotelOS</h1><p class='muted'>Hotel Management · Guest Bill</p></div><div><strong>Bill / Booking #" + id + "</strong><p>Generated: " + esc(java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))) + "</p></div></header>" +
                "<h2>Guest & Stay Details</h2><table><tr><th>Guest</th><td>" + esc(guest) + "</td><th>Phone</th><td>" + esc(phone) + "</td></tr>" +
                "<tr><th>Room</th><td>" + esc(room) + " (" + esc(type) + ")</td><th>Status</th><td>" + esc(status) + "</td></tr>" +
                "<tr><th>Check-in</th><td>" + esc(checkIn) + "</td><th>Check-out</th><td>" + esc(checkOut) + "</td></tr></table>" +
                "<h2>Bill Summary</h2><table><tr><th>Description</th><th>Calculation</th><th>Amount</th></tr><tr><td>Room charges</td><td>" + nights + " night(s) × ₹" + rate + "</td><td>₹" + roomTotal + "</td></tr>" +
                "<tr><td>Service charges</td><td>Orders during stay</td><td>₹" + (services == null ? BigDecimal.ZERO : services) + "</td></tr><tr class='total'><td colspan='2'>Bill Total</td><td>₹" + total + "</td></tr>" +
                "<tr><td colspan='2'>Total Paid</td><td>₹" + paidAmount + "</td></tr><tr class='due'><td colspan='2'>Balance Due</td><td>₹" + due + "</td></tr></table>" +
                "<h2>Payment History</h2><table><tr><th>Date</th><th>Method</th><th>Amount</th><th>Status</th></tr>" + payments + "</table><p class='muted'>Thank you for staying with HotelOS.</p><button class='print' onclick='window.print()'>Print / Save as PDF</button></main></body></html>";
    }

    private String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private void refresh() {
        PanelTools.query(table, "SELECT p.payment_id AS 'Payment ID',p.booking_id AS 'Booking ID',c.name AS Guest,p.amount AS Amount,p.payment_method AS Method,p.payment_status AS Status,p.payment_date AS Date FROM payments p JOIN bookings b ON b.booking_id=p.booking_id JOIN customers c ON c.customer_id=b.customer_id ORDER BY p.payment_id DESC");
        String pendingSql = "SELECT b.booking_id AS 'Booking ID', c.name AS Guest, c.phone AS Phone, r.room_number AS Room, " +
                "b.booking_status AS Status, b.check_out_date AS 'Check-out Date', " +
                "(GREATEST(1,DATEDIFF(b.check_out_date,b.check_in_date))*COALESCE(r.room_rate,rt.price_per_night) + COALESCE((SELECT SUM(so.total_amount) FROM service_orders so WHERE so.booking_id=b.booking_id),0)) AS 'Bill Total', " +
                "COALESCE((SELECT SUM(p.amount) FROM payments p WHERE p.booking_id=b.booking_id AND p.payment_status='Paid'),0) AS 'Paid', " +
                "GREATEST(0,(GREATEST(1,DATEDIFF(b.check_out_date,b.check_in_date))*COALESCE(r.room_rate,rt.price_per_night) + COALESCE((SELECT SUM(so2.total_amount) FROM service_orders so2 WHERE so2.booking_id=b.booking_id),0)) - " +
                "COALESCE((SELECT SUM(p2.amount) FROM payments p2 WHERE p2.booking_id=b.booking_id AND p2.payment_status='Paid'),0)) AS 'Balance Due' " +
                "FROM bookings b JOIN customers c ON c.customer_id=b.customer_id JOIN rooms r ON r.room_id=b.room_id JOIN room_types rt ON rt.type_id=r.type_id " +
                "WHERE b.booking_status IN ('Confirmed','Checked In','Checked Out') AND " +
                "(GREATEST(1,DATEDIFF(b.check_out_date,b.check_in_date))*COALESCE(r.room_rate,rt.price_per_night) + COALESCE((SELECT SUM(so3.total_amount) FROM service_orders so3 WHERE so3.booking_id=b.booking_id),0)) > " +
                "COALESCE((SELECT SUM(p3.amount) FROM payments p3 WHERE p3.booking_id=b.booking_id AND p3.payment_status='Paid'),0) " +
                "ORDER BY b.check_out_date, b.booking_id DESC";
        PanelTools.query(pendingTable, pendingSql);
    }
}
