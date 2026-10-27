package hotelos.ui;

import hotelos.DBConnection;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Monitor reservations and perform checkout for a selected checked-in booking. */
public class ReservationsPanel extends JPanel {
    private final JTable table = PanelTools.table();
    private final JTextField search = new JTextField(18);
    private final JComboBox<String> status = new JComboBox<>(new String[]{"All statuses", "Confirmed", "Checked In", "Checked Out", "Cancelled"});

    public ReservationsPanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(22, 24, 22, 24));
        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Reservations");
        title.setFont(Theme.TITLE);
        title.setForeground(Theme.TEXT);
        JLabel subtitle = new JLabel("Monitor bookings, guest details, room occupancy and balances; select a checked-in booking to check out.");
        subtitle.setForeground(Theme.MUTED);
        subtitle.setFont(Theme.BODY);
        head.add(title);
        head.add(Box.createVerticalStrut(5));
        head.add(subtitle);
        add(head, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        JPanel tools = PanelTools.card();
        tools.setLayout(new BorderLayout(0, 10));
        JPanel filter = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filter.setOpaque(false);
        JLabel sl = new JLabel("Search guest / phone / room:");
        sl.setForeground(Theme.MUTED);
        filter.add(sl);
        filter.add(search);
        filter.add(status);
        JButton find = PanelTools.button("Apply filters", true);
        JButton refresh = PanelTools.button("Refresh", false);
        JButton checkout = PanelTools.button("Check Out Selected", false);
        filter.add(find);
        filter.add(refresh);
        filter.add(checkout);
        tools.add(filter, BorderLayout.NORTH);
        tools.add(PanelTools.scrollPane(table), BorderLayout.CENTER);
        body.add(tools, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
        find.addActionListener(e -> reload());
        refresh.addActionListener(e -> { search.setText(""); status.setSelectedIndex(0); reload(); });
        search.addActionListener(e -> reload());
        status.addActionListener(e -> reload());
        checkout.addActionListener(e -> checkoutSelected());
        reload();
    }

    private void reload() {
        String q = search.getText().trim();
        String selected = (String) status.getSelectedItem();
        String sql = "SELECT b.booking_id AS 'Booking ID', c.name AS 'Guest', c.phone AS 'Phone', r.room_number AS 'Room', " +
                "b.check_in_date AS 'Check-in', b.check_out_date AS 'Check-out', b.number_of_guests AS 'Guests', " +
                "b.booking_status AS 'Booking Status', r.status AS 'Room Status', " +
                "COALESCE((SELECT SUM(p.amount) FROM payments p WHERE p.booking_id=b.booking_id AND p.payment_status='Paid'),0) AS 'Paid (₹)', " +
                "GREATEST(0, COALESCE((SELECT COALESCE(rr.room_rate,rt.price_per_night) * GREATEST(1,DATEDIFF(b.check_out_date,b.check_in_date)) FROM rooms rr JOIN room_types rt ON rt.type_id=rr.type_id WHERE rr.room_id=b.room_id),0) + " +
                "COALESCE((SELECT SUM(so.total_amount) FROM service_orders so WHERE so.booking_id=b.booking_id),0) - " +
                "COALESCE((SELECT SUM(p2.amount) FROM payments p2 WHERE p2.booking_id=b.booking_id AND p2.payment_status='Paid'),0)) AS 'Estimated Balance (₹)' " +
                "FROM bookings b JOIN customers c ON c.customer_id=b.customer_id JOIN rooms r ON r.room_id=b.room_id " +
                "WHERE (c.name LIKE ? OR c.phone LIKE ? OR r.room_number LIKE ?)";
        if (selected != null && !selected.equals("All statuses")) sql += " AND b.booking_status=?";
        sql += " ORDER BY b.booking_id DESC";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            String pattern = "%" + q + "%";
            ps.setString(1, pattern); ps.setString(2, pattern); ps.setString(3, pattern);
            if (selected != null && !selected.equals("All statuses")) ps.setString(4, selected);
            try (ResultSet rs = ps.executeQuery()) {
                java.util.Vector<String> cols = new java.util.Vector<>();
                ResultSetMetaData md = rs.getMetaData();
                for (int i = 1; i <= md.getColumnCount(); i++) cols.add(md.getColumnLabel(i));
                java.util.Vector<java.util.Vector<Object>> rows = new java.util.Vector<>();
                while (rs.next()) {
                    java.util.Vector<Object> row = new java.util.Vector<>();
                    for (int i = 1; i <= md.getColumnCount(); i++) row.add(rs.getObject(i));
                    rows.add(row);
                }
                table.setModel(new javax.swing.table.DefaultTableModel(rows, cols) {
                    @Override public boolean isCellEditable(int r, int c) { return false; }
                });
                table.setRowHeight(40);
            }
        } catch (Exception ex) { PanelTools.error(this, ex); }
    }

    private void checkoutSelected() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) { PanelTools.info(this, "Select a reservation row first."); return; }
        int modelRow = table.convertRowIndexToModel(selectedRow);
        int bookingId;
        try { bookingId = Integer.parseInt(table.getModel().getValueAt(modelRow, 0).toString()); }
        catch (Exception ex) { PanelTools.error(this, new IllegalStateException("Could not read the selected booking ID.")); return; }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Check out booking #" + bookingId + "? The system will calculate the bill and release the room only if the balance is fully paid.",
                "Confirm checkout", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int roomId;
                LocalDate checkIn;
                String bookingStatus;
                try (PreparedStatement ps = c.prepareStatement("SELECT room_id,check_in_date,booking_status FROM bookings WHERE booking_id=? FOR UPDATE")) {
                    ps.setInt(1, bookingId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Booking was not found.");
                        roomId = rs.getInt("room_id");
                        checkIn = rs.getDate("check_in_date").toLocalDate();
                        bookingStatus = rs.getString("booking_status");
                    }
                }
                if (!"Checked In".equalsIgnoreCase(bookingStatus)) throw new IllegalStateException("Only a Checked In booking can be checked out.");

                BigDecimal rate;
                try (PreparedStatement ps = c.prepareStatement("SELECT COALESCE(r.room_rate,rt.price_per_night) FROM rooms r JOIN room_types rt ON rt.type_id=r.type_id WHERE r.room_id=?")) {
                    ps.setInt(1, roomId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalStateException("Room rate was not found.");
                        rate = rs.getBigDecimal(1);
                    }
                }
                long nights = Math.max(1, ChronoUnit.DAYS.between(checkIn, LocalDate.now()));
                BigDecimal roomTotal = rate.multiply(BigDecimal.valueOf(nights));
                BigDecimal serviceTotal = sum(c, "SELECT COALESCE(SUM(total_amount),0) FROM service_orders WHERE booking_id=?", bookingId);
                BigDecimal paid = sum(c, "SELECT COALESCE(SUM(amount),0) FROM payments WHERE booking_id=? AND payment_status='Paid'", bookingId);
                BigDecimal total = roomTotal.add(serviceTotal);
                BigDecimal due = total.subtract(paid);
                if (due.signum() > 0) {
                    c.rollback();
                    PanelTools.info(this, "Checkout cannot complete yet.\nTotal bill: ₹" + total + "\nPaid: ₹" + paid + "\nBalance due: ₹" + due + "\nRecord the remaining payment in Payments, then retry checkout.");
                    return;
                }
                try (PreparedStatement ps = c.prepareStatement("UPDATE bookings SET booking_status='Checked Out' WHERE booking_id=? AND booking_status='Checked In'")) {
                    ps.setInt(1, bookingId);
                    if (ps.executeUpdate() != 1) throw new IllegalStateException("Booking status changed. Refresh and try again.");
                }
                try (PreparedStatement ps = c.prepareStatement("UPDATE rooms SET status='Available' WHERE room_id=? AND status='Occupied'")) {
                    ps.setInt(1, roomId);
                    if (ps.executeUpdate() != 1) throw new IllegalStateException("Room was not marked Occupied. Check room status before checkout.");
                }
                c.commit();
                PanelTools.info(this, "Checkout completed. Total: ₹" + total + ", paid: ₹" + paid + ". Room is available again.");
                reload();
            } catch (Exception ex) { c.rollback(); throw ex; }
            finally { c.setAutoCommit(true); }
        } catch (Exception ex) { PanelTools.error(this, ex); }
    }

    private BigDecimal sum(Connection c, String sql, int bookingId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); BigDecimal value = rs.getBigDecimal(1); return value == null ? BigDecimal.ZERO : value; }
        }
    }
}
