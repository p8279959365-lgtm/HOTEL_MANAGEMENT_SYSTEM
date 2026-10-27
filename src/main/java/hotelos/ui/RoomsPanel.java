package hotelos.ui;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
public class RoomsPanel extends JPanel {
    private final JTable table=PanelTools.table();
    public RoomsPanel(){ JPanel base=PanelTools.page("Room inventory","Monitor room types, floors and availability."); setLayout(new BorderLayout());setBackground(Theme.BG);setBorder(new EmptyBorder(0,0,0,0));add(base,BorderLayout.NORTH);
        JPanel card=PanelTools.card(); JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));actions.setOpaque(false);
        for(String status:new String[]{"Available","Occupied","Maintenance"}){JButton b=PanelTools.button("Mark "+status,false);b.addActionListener(e->{int r=table.getSelectedRow();if(r<0){JOptionPane.showMessageDialog(this,"Select a room first.");return;}try(java.sql.Connection c=hotelos.DBConnection.getConnection()){
                Object roomId=table.getValueAt(r,0);
                if("Available".equals(status)){
                    try(java.sql.PreparedStatement check=c.prepareStatement("SELECT COUNT(*) FROM bookings WHERE room_id=? AND booking_status='Checked In'")){
                        check.setObject(1,roomId);
                        try(java.sql.ResultSet rs=check.executeQuery()){
                            rs.next();
                            if(rs.getInt(1)>0) throw new IllegalStateException("This room has an active checked-in guest. Check out the guest first.");
                        }
                    }
                }
                try(java.sql.PreparedStatement ps=c.prepareStatement("UPDATE rooms SET status=? WHERE room_id=?")){
                    ps.setString(1,status);ps.setObject(2,roomId);ps.executeUpdate();
                }
                refresh();
            }catch(Exception ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"HotelOS",JOptionPane.ERROR_MESSAGE);}});actions.add(b);}
        JButton refresh=PanelTools.button("Refresh",true);refresh.addActionListener(e->refresh());actions.add(refresh);card.add(actions,BorderLayout.NORTH);card.add(new JScrollPane(table),BorderLayout.CENTER);base.add(card,BorderLayout.CENTER);refresh();}
    private void refresh(){PanelTools.query(table,"SELECT r.room_id AS ID,r.room_number AS Room,rt.type_name AS Type,COALESCE(r.room_rate,rt.price_per_night) AS 'Rate / Night',r.floor AS Floor,r.status AS Status FROM rooms r JOIN room_types rt ON rt.type_id=r.type_id ORDER BY r.room_number");}
}