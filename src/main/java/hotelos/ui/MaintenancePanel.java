package hotelos.ui;

import hotelos.DBConnection;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;

/** Dedicated room maintenance workflow. */
public class MaintenancePanel extends JPanel {
    private final JTable table = PanelTools.table();

    public MaintenancePanel() {
        setLayout(new BorderLayout(0, 14));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(22, 24, 22, 24));
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Room Maintenance");
        title.setFont(Theme.TITLE);
        title.setForeground(Theme.TEXT);
        JLabel subtitle = new JLabel("Mark rooms unavailable for repair, cleaning or inspection, then return them to service.");
        subtitle.setFont(Theme.BODY);
        subtitle.setForeground(Theme.MUTED);
        header.add(title);
        header.add(Box.createVerticalStrut(5));
        header.add(subtitle);
        add(header, BorderLayout.NORTH);

        JPanel card = PanelTools.card();
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        JButton maintenance = PanelTools.button("Mark Maintenance", true);
        JButton available = PanelTools.button("Return to Available", false);
        JButton refresh = PanelTools.button("Refresh", false);
        maintenance.addActionListener(e -> updateSelectedRoom("Maintenance"));
        available.addActionListener(e -> updateSelectedRoom("Available"));
        refresh.addActionListener(e -> reload());
        actions.add(maintenance);
        actions.add(available);
        actions.add(refresh);
        card.add(actions, BorderLayout.NORTH);
        card.add(new JScrollPane(table), BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);
        reload();
    }

    private void updateSelectedRoom(String newStatus) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            PanelTools.info(this, "Select a room from the table first.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(selectedRow);
        Object roomId = table.getModel().getValueAt(modelRow, 0);
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement lock = c.prepareStatement("SELECT status FROM rooms WHERE room_id=? FOR UPDATE")) {
                    lock.setObject(1, roomId);
                    try (ResultSet rs = lock.executeQuery()) {
                        if (!rs.next()) throw new IllegalStateException("Room not found.");
                        if ("Available".equals(newStatus) && "Occupied".equalsIgnoreCase(rs.getString(1)))
                            throw new IllegalStateException("An occupied room cannot be made available. Check out the guest first.");
                    }
                }
                if ("Maintenance".equals(newStatus)) {
                    String active = "SELECT COUNT(*) FROM bookings WHERE room_id=? AND booking_status='Checked In'";
                    try (PreparedStatement ps = c.prepareStatement(active)) {
                        ps.setObject(1, roomId);
                        try (ResultSet rs = ps.executeQuery()) {
                            rs.next();
                            if (rs.getInt(1) > 0) throw new IllegalStateException("This room has a checked-in guest. Check out the guest before maintenance.");
                        }
                    }
                }
                try (PreparedStatement ps = c.prepareStatement("UPDATE rooms SET status=? WHERE room_id=?")) {
                    ps.setString(1, newStatus);
                    ps.setObject(2, roomId);
                    if (ps.executeUpdate() != 1) throw new IllegalStateException("Room status was not updated.");
                }
                c.commit();
            } catch (Exception ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
            reload();
            PanelTools.info(this, "Room status updated to " + newStatus + ".");
        } catch (Exception ex) {
            PanelTools.error(this, ex);
        }
    }

    private void reload() {
        PanelTools.query(table, "SELECT r.room_id AS ID,r.room_number AS Room,rt.type_name AS Type,COALESCE(r.room_rate,rt.price_per_night) AS 'Rate / Night',r.floor AS Floor,r.status AS Status FROM rooms r JOIN room_types rt ON rt.type_id=r.type_id ORDER BY r.room_number");
    }
}
