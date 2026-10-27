package hotelos.ui;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
public class DashboardPanel extends JPanel {
    public DashboardPanel() {
        setLayout(new BorderLayout(0,18)); setBackground(Theme.BG); setBorder(new EmptyBorder(22,24,22,24));
        JPanel heading=new JPanel(); heading.setOpaque(false); heading.setLayout(new BoxLayout(heading,BoxLayout.Y_AXIS));
        JLabel title=new JLabel("Good evening, Administrator"); title.setFont(Theme.TITLE); title.setForeground(Theme.TEXT);
        JLabel sub=new JLabel("Your hotel's live operational overview"); sub.setForeground(Theme.MUTED);
        heading.add(title); heading.add(Box.createVerticalStrut(5)); heading.add(sub); add(heading,BorderLayout.NORTH);
        JPanel body=new JPanel(new BorderLayout(0,16)); body.setOpaque(false);
        JPanel cards=new JPanel(new GridLayout(1,4,14,0)); cards.setOpaque(false);
        cards.add(metric("TOTAL ROOMS",PanelTools.scalar("SELECT COUNT(*) FROM rooms"),new Color(59,130,246)));
        cards.add(metric("AVAILABLE",PanelTools.scalar("SELECT COUNT(*) FROM rooms WHERE status='Available'"),Theme.TEAL));
        cards.add(metric("BOOKINGS",PanelTools.scalar("SELECT COUNT(*) FROM bookings"),new Color(139,92,246)));
        cards.add(metric("GUESTS",PanelTools.scalar("SELECT COUNT(*) FROM customers"),new Color(245,158,11)));
        body.add(cards,BorderLayout.NORTH);
        JPanel recent=PanelTools.card(); recent.add(new JLabel("Recent reservations"),BorderLayout.NORTH); JTable t=PanelTools.table();
        PanelTools.query(t,"SELECT b.booking_id AS 'Booking ID',c.name AS Guest,r.room_number AS Room,b.check_in_date AS 'Check-in',b.check_out_date AS 'Check-out',b.booking_status AS Status FROM bookings b JOIN customers c ON c.customer_id=b.customer_id JOIN rooms r ON r.room_id=b.room_id ORDER BY b.booking_id DESC LIMIT 10");
        recent.add(new JScrollPane(t),BorderLayout.CENTER); body.add(recent,BorderLayout.CENTER); add(body,BorderLayout.CENTER);
    }
    private JPanel metric(String label,String value,Color accent){ JPanel p=new JPanel(new BorderLayout(0,12));p.setBackground(Theme.SURFACE);p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,5,0,0,accent),new EmptyBorder(18,18,18,14)));JLabel l=new JLabel(label);l.setForeground(Theme.MUTED);l.setFont(new Font("Segoe UI",Font.BOLD,12));JLabel v=new JLabel(value);v.setForeground(Theme.TEXT);v.setFont(new Font("Segoe UI",Font.BOLD,31));p.add(l,BorderLayout.NORTH);p.add(v,BorderLayout.CENTER);return p;}
}