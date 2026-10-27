package hotelos.ui;
import hotelos.DBConnection;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;
public class GuestsPanel extends JPanel {
    private final JTable table=PanelTools.table(); private final JTextField name=field(),email=field(),phone=field(),address=field(),search=field();
    public GuestsPanel(){setLayout(new BorderLayout());setBackground(Theme.BG);setBorder(new EmptyBorder(22,24,22,24));JPanel head=new JPanel();head.setOpaque(false);head.setLayout(new BoxLayout(head,BoxLayout.Y_AXIS));JLabel h=new JLabel("Guest directory");h.setFont(Theme.TITLE);h.setForeground(Theme.TEXT);JLabel s=new JLabel("Manage guest details and search records.");s.setForeground(Theme.MUTED);head.add(h);head.add(Box.createVerticalStrut(5));head.add(s);add(head,BorderLayout.NORTH);
        JPanel body=new JPanel(new BorderLayout(0,12));body.setOpaque(false);JPanel form=PanelTools.card();form.setLayout(new GridLayout(2,4,10,8));form.add(labeled("Name",name));form.add(labeled("Email",email));form.add(labeled("Phone",phone));form.add(labeled("Address",address));
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.LEFT,7,0));buttons.setOpaque(false);JButton add=PanelTools.button("Add guest",true),update=PanelTools.button("Update selected",false),del=PanelTools.button("Delete selected",false),find=PanelTools.button("Search",false),clear=PanelTools.button("Clear",false);buttons.add(add);buttons.add(update);buttons.add(del);buttons.add(clear);buttons.add(search);buttons.add(find);form.add(buttons);body.add(form,BorderLayout.NORTH);
        JPanel list=PanelTools.card();list.add(new JLabel("Guest records"),BorderLayout.NORTH);list.add(new JScrollPane(table),BorderLayout.CENTER);body.add(list,BorderLayout.CENTER);add(body,BorderLayout.CENTER);refresh();
        table.getSelectionModel().addListSelectionListener(e->{if(!e.getValueIsAdjusting()&&table.getSelectedRow()>=0){int r=table.getSelectedRow();name.setText(v(r,1));email.setText(v(r,2));phone.setText(v(r,3));address.setText(v(r,4));}});
        add.addActionListener(e->{try{if(name.getText().isBlank()||phone.getText().isBlank())throw new IllegalArgumentException("Name and phone are required.");exec("INSERT INTO customers(name,email,phone,address) VALUES(?,?,?,?)",name.getText(),email.getText(),phone.getText(),address.getText());refresh();clear();}catch(Exception ex){PanelTools.error(this,ex);}});
        update.addActionListener(e->{int r=table.getSelectedRow();if(r<0)return;try{exec("UPDATE customers SET name=?,email=?,phone=?,address=? WHERE customer_id=?",name.getText(),email.getText(),phone.getText(),address.getText(),v(r,0));refresh();}catch(Exception ex){PanelTools.error(this,ex);}});
        del.addActionListener(e->{int r=table.getSelectedRow();if(r<0)return;if(JOptionPane.showConfirmDialog(this,"Delete selected guest?","Confirm",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;try{exec("DELETE FROM customers WHERE customer_id=?",v(r,0));refresh();clear();}catch(Exception ex){PanelTools.error(this,ex);}});
        clear.addActionListener(e->clear());find.addActionListener(e->PanelTools.query(table,"SELECT customer_id AS ID,name AS Name,email AS Email,phone AS Phone,address AS Address FROM customers WHERE name LIKE '%"+search.getText().replace("'","''")+"%' OR phone LIKE '%"+search.getText().replace("'","''")+"%' ORDER BY customer_id DESC"));
    }
    private JTextField field(){JTextField f=new JTextField();f.setPreferredSize(new Dimension(140,32));return f;}
    private JPanel labeled(String s,JTextField f){JPanel p=new JPanel(new BorderLayout(0,4));p.setOpaque(false);JLabel l=new JLabel(s);l.setForeground(Theme.MUTED);p.add(l,BorderLayout.NORTH);p.add(f,BorderLayout.CENTER);return p;}
    private void refresh(){PanelTools.query(table,"SELECT customer_id AS ID,name AS Name,email AS Email,phone AS Phone,address AS Address FROM customers ORDER BY customer_id DESC");}
    private String v(int r,int c){Object o=table.getValueAt(r,c);return o==null?"":o.toString();}
    private void clear(){name.setText("");email.setText("");phone.setText("");address.setText("");}
    private void exec(String sql,Object... a)throws SQLException{try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement(sql)){for(int i=0;i<a.length;i++)p.setObject(i+1,a[i]);p.executeUpdate();}}
}