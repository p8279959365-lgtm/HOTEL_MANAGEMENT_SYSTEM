package hotelos.ui;

import hotelos.DBConnection;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Guest lookup, reservation creation and walk-in check-in. */
public class CheckInPanel extends JPanel {
    private final JTextField phone = field(), name = field(), email = field(), address = field();
    private final JTextField idValue = field(), checkIn = field(), checkOut = field(), guestCount = field();
    private final JComboBox<String> idType = new JComboBox<>(new String[]{"Aadhaar Card", "PAN Card", "Driving Licence", "Passport", "Voter ID", "Other"});
    private final JComboBox<Item> room = new JComboBox<>();
    private final JTextField checkoutBookingId = field();
    private final JCheckBox verified = new JCheckBox("I physically checked the customer's ID document");
    private final JLabel lookupStatus = new JLabel("Enter phone number, then choose Find Guest.");
    private Integer customerId;
    private final Timer lookupTimer = new Timer(550, e -> lookupGuestAutomatically());
    private String lastAutoLookupPhone = "";

    public CheckInPanel() {
        setLayout(new BorderLayout(0, 14)); setBackground(Theme.BG); setBorder(new EmptyBorder(22,24,22,24));
        JPanel header = new JPanel(); header.setOpaque(false); header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Check-in & Guest Registration"); title.setFont(Theme.TITLE); title.setForeground(Theme.TEXT);
        JLabel sub = new JLabel("Find returning guests by phone, register new guests, create reservations or check in walk-ins."); sub.setForeground(Theme.MUTED);
        header.add(title); header.add(Box.createVerticalStrut(5)); header.add(sub); add(header, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0,12)); content.setOpaque(false);
        JPanel form = PanelTools.card(); form.setLayout(new GridLayout(0,3,12,10));
        JPanel phoneBox = labeled("Phone number", phone); JPanel findBox = new JPanel(new BorderLayout()); findBox.setOpaque(false); findBox.add(new JLabel("Guest lookup"),BorderLayout.NORTH);
        JButton find = PanelTools.button("Find Guest", true); findBox.add(find,BorderLayout.CENTER);
        form.add(phoneBox); form.add(findBox); form.add(labeled("Guest name",name));
        form.add(labeled("Email",email)); form.add(labeled("Address",address)); form.add(labeled("ID proof type",idType));
        form.add(labeled("ID number / details",idValue)); form.add(labeled("Check-in date",dateInput(checkIn))); form.add(labeled("Check-out date",dateInput(checkOut)));
        form.add(labeled("Room",room)); form.add(labeled("Number of guests",guestCount));
        JPanel verifyBox = new JPanel(new BorderLayout()); verifyBox.setOpaque(false); verifyBox.add(new JLabel("ID verification"),BorderLayout.NORTH); verified.setOpaque(false); verified.setForeground(Theme.TEXT); verifyBox.add(verified,BorderLayout.CENTER); form.add(verifyBox);
        lookupStatus.setForeground(Theme.MUTED); lookupStatus.setFont(Theme.BODY); form.add(lookupStatus);
        content.add(form,BorderLayout.NORTH);
        JPanel actions = PanelTools.card(); actions.setLayout(new FlowLayout(FlowLayout.LEFT,10,5));
        JButton reservation = PanelTools.button("Create Reservation", false); JButton walkIn = PanelTools.button("Walk-in Check-in", true); JButton clear = PanelTools.button("Clear Form", false); JButton refreshRooms = PanelTools.button("Refresh Rooms", false);
        actions.add(reservation); actions.add(walkIn); actions.add(new JLabel("Checkout booking ID:")); checkoutBookingId.setPreferredSize(new Dimension(90,34)); actions.add(checkoutBookingId); JButton checkout = PanelTools.button("Check Out", false); actions.add(checkout); actions.add(clear); actions.add(refreshRooms); content.add(actions,BorderLayout.CENTER); add(content,BorderLayout.CENTER);
        checkIn.setText(LocalDate.now().toString()); checkOut.setText(LocalDate.now().plusDays(1).toString()); guestCount.setText("1");
        lookupTimer.setRepeats(false);
        phone.getDocument().addDocumentListener(new DocumentListener() {
            private void changed() {
                String current = phone.getText().trim();
                if (current.length() < 7) {
                    lookupTimer.stop();
                    if (!current.equals(lastAutoLookupPhone)) {
                        customerId = null;
                        lookupStatus.setText("Enter at least 7 digits to search existing guests.");
                    }
                    return;
                }
                lookupTimer.restart();
            }
            @Override public void insertUpdate(DocumentEvent e) { changed(); }
            @Override public void removeUpdate(DocumentEvent e) { changed(); }
            @Override public void changedUpdate(DocumentEvent e) { changed(); }
        });
        find.addActionListener(e -> { lookupTimer.stop(); lookupGuest(); }); reservation.addActionListener(e -> saveBooking(false)); walkIn.addActionListener(e -> saveBooking(true)); checkout.addActionListener(e -> checkOutBooking()); clear.addActionListener(e -> clearForm()); refreshRooms.addActionListener(e -> loadRooms());
        checkIn.addActionListener(e -> loadRooms()); checkOut.addActionListener(e -> loadRooms()); loadRooms();
    }

    private static JTextField field(){ JTextField f=new JTextField(); f.setPreferredSize(new Dimension(170,34)); f.setFont(Theme.BODY); f.setForeground(Theme.TEXT); f.setBackground(Theme.INPUT_BG); return f; }
    private JPanel labeled(String text, JComponent component){ JPanel p=new JPanel(new BorderLayout(0,5)); p.setOpaque(false); JLabel l=new JLabel(text); l.setForeground(Theme.MUTED); l.setFont(Theme.BODY); p.add(l,BorderLayout.NORTH); component.setPreferredSize(new Dimension(170,34)); p.add(component,BorderLayout.CENTER); return p; }
    private JComponent dateInput(JTextField target) {
        target.setEditable(false);
        JPanel wrap = new JPanel(new BorderLayout(4, 0)); wrap.setOpaque(false);
        JButton pick = PanelTools.button("📅", false); pick.setPreferredSize(new Dimension(44, 34));
        pick.addActionListener(e -> {
            LocalDate current;
            try { current = LocalDate.parse(target.getText().trim()); } catch (Exception ex) { current = LocalDate.now(); }
            LocalDate chosen = CalendarPicker.choose(this, current);
            if (chosen != null) { target.setText(chosen.toString()); loadRooms(); }
        });
        wrap.add(target, BorderLayout.CENTER); wrap.add(pick, BorderLayout.EAST); return wrap;
    }

    private void lookupGuestAutomatically() {
        String p = phone.getText().trim();
        if (p.length() < 7 || p.length() > 15) return;
        if (p.equals(lastAutoLookupPhone)) return;
        lookupGuest(false);
    }

    private void lookupGuest() { lookupGuest(true); }

    private void lookupGuest(boolean showValidation) {
        String p=phone.getText().trim();
        if(p.isBlank()){ if(showValidation) PanelTools.info(this,"Enter the guest's phone number first."); return; }
        if(p.length()<7 || p.length()>15){ if(showValidation) PanelTools.info(this,"Enter a valid phone number (7–15 characters)."); return; }
        String digits = p.replaceAll("\\D", "");
        if (digits.length() < 7 || digits.length() > 15) {
            if (showValidation) PanelTools.info(this, "Enter a valid phone number (7–15 digits). ");
            return;
        }
        // Avoid REGEXP_REPLACE so lookup also works on older MySQL versions.
        // Compare normalized phone numbers in Java to tolerate spaces, +91, and punctuation.
        String sql = "SELECT customer_id,name,email,phone,address,id_proof_type,id_proof FROM customers ORDER BY customer_id DESC";
        try(Connection c=DBConnection.getConnection(); PreparedStatement ps=c.prepareStatement(sql)){
            Integer matchedId = null;
            String matchedName = "", matchedEmail = "", matchedAddress = "", matchedProof = "", matchedType = null;
            try(ResultSet rs=ps.executeQuery()){
                while(rs.next()) {
                    String storedPhone = s(rs, "phone").replaceAll("\\D", "");
                    boolean exact = storedPhone.equals(digits);
                    boolean sameIndianNumber = digits.length() >= 10 && storedPhone.length() >= 10
                            && storedPhone.substring(storedPhone.length()-10).equals(digits.substring(digits.length()-10));
                    if (exact || sameIndianNumber) {
                        matchedId = rs.getInt("customer_id");
                        matchedName = s(rs, "name"); matchedEmail = s(rs, "email");
                        matchedAddress = s(rs, "address"); matchedProof = s(rs, "id_proof");
                        matchedType = rs.getString("id_proof_type");
                        break;
                    }
                }
            }
            lastAutoLookupPhone = p;
            if(matchedId != null){
                customerId=matchedId; name.setText(matchedName); email.setText(matchedEmail); address.setText(matchedAddress);
                if(matchedType!=null) idType.setSelectedItem(matchedType); idValue.setText(matchedProof);
                verified.setSelected(false);
                lookupStatus.setText("Returning guest found — details auto-filled. Verify ID for this stay.");
            } else {
                customerId=null; name.setText(""); email.setText(""); address.setText(""); idValue.setText(""); verified.setSelected(false);
                lookupStatus.setText("No existing guest found — complete details for a new customer.");
            }
        }catch(Exception ex){ lookupStatus.setText("Guest lookup failed: " + ex.getMessage()); if(showValidation) PanelTools.error(this,ex); }
    }
    private String s(ResultSet rs,String col)throws SQLException{String x=rs.getString(col);return x==null?"":x;}

    private void loadRooms(){
        room.removeAllItems(); LocalDate start,end; try{start=LocalDate.parse(checkIn.getText().trim());end=LocalDate.parse(checkOut.getText().trim());if(!end.isAfter(start))return;}catch(Exception ex){return;}
        String sql="SELECT r.room_id,r.room_number FROM rooms r WHERE r.status='Available' AND NOT EXISTS (SELECT 1 FROM bookings b WHERE b.room_id=r.room_id AND b.booking_status IN ('Confirmed','Checked In') AND b.check_in_date < ? AND b.check_out_date > ?) ORDER BY r.room_number";
        try(Connection c=DBConnection.getConnection();PreparedStatement ps=c.prepareStatement(sql)){ps.setDate(1,Date.valueOf(end));ps.setDate(2,Date.valueOf(start));try(ResultSet rs=ps.executeQuery()){while(rs.next())room.addItem(new Item(rs.getInt(1),"Room "+rs.getString(2)));}}
        catch(Exception ex){PanelTools.error(this,ex);}
    }

    private void saveBooking(boolean walkIn){
        String guestName=name.getText().trim(), phoneValue=phone.getText().trim(), id=idValue.getText().trim();
        if(guestName.isBlank()||phoneValue.isBlank()) {PanelTools.info(this,"Guest name and phone number are required.");return;}
        if(id.isBlank()){PanelTools.info(this,"Enter the guest's ID details.");return;}
        if(!verified.isSelected()){PanelTools.info(this,"Physically check the ID document and tick the verification checkbox before proceeding.");return;}
        try{
            LocalDate start=LocalDate.parse(checkIn.getText().trim()), end=LocalDate.parse(checkOut.getText().trim()); int guests=Integer.parseInt(guestCount.getText().trim());
            if(!end.isAfter(start)||guests<1)throw new IllegalArgumentException("Check-out must be after check-in and guest count must be at least 1.");
            if(walkIn && !start.equals(LocalDate.now()))throw new IllegalArgumentException("For walk-in check-in, check-in date must be today.");
            Item selected=(Item)room.getSelectedItem(); if(selected==null)throw new IllegalArgumentException("No available room for the selected dates.");
            try(Connection c=DBConnection.getConnection()){
                c.setAutoCommit(false);
                try{
                    int cid=upsertCustomer(c,guestName,phoneValue,email.getText().trim(),address.getText().trim(),idType.getSelectedItem().toString(),id);
                    try(PreparedStatement lock=c.prepareStatement("SELECT status FROM rooms WHERE room_id=? FOR UPDATE")){lock.setInt(1,selected.id);try(ResultSet rs=lock.executeQuery()){if(!rs.next()||!"Available".equalsIgnoreCase(rs.getString(1)))throw new IllegalStateException("Room is no longer available. Refresh and choose another room.");}}
                    String overlap="SELECT COUNT(*) FROM bookings WHERE room_id=? AND booking_status IN ('Confirmed','Checked In') AND check_in_date < ? AND check_out_date > ?";
                    try(PreparedStatement ps=c.prepareStatement(overlap)){ps.setInt(1,selected.id);ps.setDate(2,Date.valueOf(end));ps.setDate(3,Date.valueOf(start));try(ResultSet rs=ps.executeQuery()){rs.next();if(rs.getInt(1)>0)throw new IllegalStateException("Room already has an overlapping booking.");}}
                    String status=walkIn?"Checked In":"Confirmed";
                    try(PreparedStatement ps=c.prepareStatement("INSERT INTO bookings(customer_id,room_id,check_in_date,check_out_date,number_of_guests,booking_status,id_proof_type,id_proof,id_verified,id_verified_at) VALUES(?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP)")){
                        ps.setInt(1,cid);ps.setInt(2,selected.id);ps.setDate(3,Date.valueOf(start));ps.setDate(4,Date.valueOf(end));ps.setInt(5,guests);ps.setString(6,status);ps.setString(7,idType.getSelectedItem().toString());ps.setString(8,id);ps.setBoolean(9,true);ps.executeUpdate();
                    }
                    if(walkIn){try(PreparedStatement ps=c.prepareStatement("UPDATE rooms SET status='Occupied' WHERE room_id=? AND status='Available'")){ps.setInt(1,selected.id);if(ps.executeUpdate()!=1)throw new IllegalStateException("Room status changed; please refresh.");}}
                    c.commit(); customerId=cid;
                }catch(Exception ex){c.rollback();throw ex;}finally{c.setAutoCommit(true);}
            }
            PanelTools.info(this,walkIn?"Walk-in guest checked in. No payment was required at check-in.":"Reservation created. Payment is not required now."); clearForm(); loadRooms();
        }catch(Exception ex){PanelTools.error(this,ex);}
    }
    private void checkOutBooking(){
        try{
            int bookingId=Integer.parseInt(checkoutBookingId.getText().trim());
            try(Connection c=DBConnection.getConnection()){
                c.setAutoCommit(false);
                try{
                    int roomId; LocalDate start; String status;
                    try(PreparedStatement ps=c.prepareStatement("SELECT room_id,check_in_date,booking_status FROM bookings WHERE booking_id=? FOR UPDATE")){ps.setInt(1,bookingId);try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new IllegalArgumentException("Booking ID not found.");roomId=rs.getInt("room_id");start=rs.getDate("check_in_date").toLocalDate();status=rs.getString("booking_status");}}
                    if(!"Checked In".equalsIgnoreCase(status))throw new IllegalStateException("Only Checked In bookings can be checked out.");
                    java.math.BigDecimal rate;
                    try(PreparedStatement ps=c.prepareStatement("SELECT COALESCE(r.room_rate,rt.price_per_night) FROM rooms r JOIN room_types rt ON rt.type_id=r.type_id WHERE r.room_id=?")){ps.setInt(1,roomId);try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new IllegalStateException("Room rate was not found.");rate=rs.getBigDecimal(1);}}
                    long nights=Math.max(1,java.time.temporal.ChronoUnit.DAYS.between(start,LocalDate.now()));java.math.BigDecimal roomTotal=rate.multiply(java.math.BigDecimal.valueOf(nights));java.math.BigDecimal services=java.math.BigDecimal.ZERO,paid=java.math.BigDecimal.ZERO;
                    try(PreparedStatement ps=c.prepareStatement("SELECT COALESCE(SUM(total_amount),0) FROM service_orders WHERE booking_id=?")){ps.setInt(1,bookingId);try(ResultSet rs=ps.executeQuery()){rs.next();services=rs.getBigDecimal(1);}}
                    try(PreparedStatement ps=c.prepareStatement("SELECT COALESCE(SUM(amount),0) FROM payments WHERE booking_id=? AND payment_status='Paid'")){ps.setInt(1,bookingId);try(ResultSet rs=ps.executeQuery()){rs.next();paid=rs.getBigDecimal(1);}}
                    java.math.BigDecimal total=roomTotal.add(services),due=total.subtract(paid);
                    if(due.signum()>0){c.rollback();PanelTools.info(this,"Checkout bill: ₹"+total+"\nPaid: ₹"+paid+"\nBalance due: ₹"+due+"\nRecord the remaining payment in Payments, then retry checkout.");return;}
                    try(PreparedStatement ps=c.prepareStatement("UPDATE bookings SET booking_status='Checked Out' WHERE booking_id=?")){ps.setInt(1,bookingId);ps.executeUpdate();}
                    try(PreparedStatement ps=c.prepareStatement("UPDATE rooms SET status='Available' WHERE room_id=?")){ps.setInt(1,roomId);ps.executeUpdate();}
                    c.commit();PanelTools.info(this,"Checkout complete. Total: ₹"+total+", paid: ₹"+paid+". Room is available again.");
                }catch(Exception ex){c.rollback();throw ex;}finally{c.setAutoCommit(true);}
            }
        }catch(Exception ex){PanelTools.error(this,ex);}
    }

    private int upsertCustomer(Connection c,String n,String p,String em,String addr,String type,String proof)throws SQLException{
        if(customerId!=null){try(PreparedStatement ps=c.prepareStatement("UPDATE customers SET name=?,email=?,phone=?,address=?,id_proof_type=?,id_proof=? WHERE customer_id=?")){ps.setString(1,n);ps.setString(2,em);ps.setString(3,p);ps.setString(4,addr);ps.setString(5,type);ps.setString(6,proof);ps.setInt(7,customerId);ps.executeUpdate();}return customerId;}
        try(PreparedStatement find=c.prepareStatement("SELECT customer_id FROM customers WHERE phone=? ORDER BY customer_id DESC LIMIT 1")){find.setString(1,p);try(ResultSet rs=find.executeQuery()){if(rs.next()){int id=rs.getInt(1);try(PreparedStatement ps=c.prepareStatement("UPDATE customers SET name=?,email=?,address=?,id_proof_type=?,id_proof=? WHERE customer_id=?")){ps.setString(1,n);ps.setString(2,em);ps.setString(3,addr);ps.setString(4,type);ps.setString(5,proof);ps.setInt(6,id);ps.executeUpdate();}return id;}}}
        try(PreparedStatement ps=c.prepareStatement("INSERT INTO customers(name,email,phone,address,id_proof_type,id_proof) VALUES(?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){ps.setString(1,n);ps.setString(2,em);ps.setString(3,p);ps.setString(4,addr);ps.setString(5,type);ps.setString(6,proof);ps.executeUpdate();try(ResultSet keys=ps.getGeneratedKeys()){if(keys.next())return keys.getInt(1);}}
        throw new SQLException("Customer was saved but its ID could not be read.");
    }
    private void clearForm(){lookupTimer.stop();lastAutoLookupPhone="";customerId=null;phone.setText("");name.setText("");email.setText("");address.setText("");idValue.setText("");idType.setSelectedIndex(0);verified.setSelected(false);checkIn.setText(LocalDate.now().toString());checkOut.setText(LocalDate.now().plusDays(1).toString());guestCount.setText("1");lookupStatus.setText("Enter phone number, then choose Find Guest.");}
    private record Item(int id,String label){@Override public String toString(){return label;}}
}
