
package hotelos.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import hotelos.DBConnection;

public final class PanelTools {

    private static Color NAVY = Theme.NAVY;

    private static Color NAVY_HEADER = Theme.TABLE_HEADER;

    private static Color TEXT = Theme.TEXT;

    private static Color ROW_WHITE = Theme.SURFACE;

    private static Color ROW_ALTERNATE = Theme.TABLE_ALT;

    private static Color TEAL_SELECTION = Theme.TABLE_SELECTED;

    private static Color GRID = Theme.TABLE_GRID;

    private PanelTools() {
        // Prevent object creation
    }

    // --------------------------------------------------
    // Page layout
    // --------------------------------------------------

    public static JPanel page(String title, String subtitle) {

        JPanel p = new JPanel(new BorderLayout(0, 15));

        p.setBackground(Theme.BG);
        p.setBorder(new EmptyBorder(22, 24, 22, 24));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.TITLE);
        titleLabel.setForeground(Theme.TEXT);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(Theme.BODY);
        subtitleLabel.setForeground(Theme.MUTED);

        header.add(titleLabel);
        header.add(Box.createVerticalStrut(5));
        header.add(subtitleLabel);

        p.add(header, BorderLayout.NORTH);

        return p;
    }

    // --------------------------------------------------
    // Card styling
    // --------------------------------------------------

    public static JPanel card() {

        JPanel p = new JPanel(new BorderLayout(0, 12));

        p.setBackground(Theme.SURFACE);

        p.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Theme.BORDER),
                        new EmptyBorder(16, 16, 16, 16)
                )
        );

        return p;
    }

    // --------------------------------------------------
    // Button styling
    // --------------------------------------------------

    public static JButton button(String text, boolean primary) {

        JButton b = new JButton(text);

        b.setFont(Theme.BUTTON);

        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);

        b.setBackground(
                primary ? Theme.TEAL : Theme.BUTTON_BG
        );

        b.setForeground(
                primary ? (Theme.isDark() ? new Color(8, 30, 42) : Color.WHITE) : Theme.BUTTON_TEXT
        );

        b.setBorder(
                new EmptyBorder(10, 14, 10, 14)
        );

        return b;
    }

    // --------------------------------------------------
    // Table styling
    // --------------------------------------------------
        
public static JTable table() {
    JTable t = new JTable();

    t.setRowHeight(40);
    t.setFont(Theme.TABLE);
    t.setForeground(Theme.TEXT);
    t.setBackground(Theme.SURFACE);
    t.setSelectionBackground(Theme.TABLE_SELECTED);
    t.setSelectionForeground(Theme.TEXT);
    t.setGridColor(Theme.TABLE_GRID);
    t.setShowGrid(true);
    t.setShowVerticalLines(false);
    t.setShowHorizontalLines(true);
    t.setIntercellSpacing(new Dimension(0, 1));
    t.setFillsViewportHeight(true);
    t.setSelectionMode(
        javax.swing.ListSelectionModel.SINGLE_SELECTION
    );
    t.setAutoCreateRowSorter(true);

    
javax.swing.table.JTableHeader header = t.getTableHeader();

header.setFont(new Font("Segoe UI", Font.BOLD, 14));
header.setBackground(Theme.TABLE_HEADER);
header.setForeground(Color.WHITE);
header.setOpaque(true);
header.setDefaultRenderer(
    new javax.swing.table.DefaultTableCellRenderer() {
        @Override
        public java.awt.Component getTableCellRendererComponent(
                JTable table, Object value,
                boolean isSelected, boolean hasFocus,
                int row, int column) {

            JLabel label = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column
            );

            label.setBackground(Theme.TABLE_HEADER);
            label.setForeground(Color.WHITE);
            label.setFont(new Font("Segoe UI", Font.BOLD, 14));
            label.setOpaque(true);
            label.setHorizontalAlignment(JLabel.LEFT);
            label.setBorder(
                BorderFactory.createEmptyBorder(0, 12, 0, 8)
            );

            return label;
        }
    }
);

header.setPreferredSize(new Dimension(0, 44));
header.setBorder(
    BorderFactory.createMatteBorder(
        0, 0, 2, 0, Theme.TEAL
    )
);

    header.setFont(new Font("Segoe UI", Font.BOLD, 13));
    header.setBackground(Theme.TABLE_HEADER);
    header.setForeground(Color.WHITE);
    header.setOpaque(true);
    header.setPreferredSize(new Dimension(0, 44));
    header.setBorder(
        BorderFactory.createMatteBorder(
            0, 0, 2, 0, Theme.TEAL
        )
    );

    javax.swing.table.DefaultTableCellRenderer renderer =
        new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {

                java.awt.Component c =
                    super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column
                    );

                if (isSelected) {
                    c.setBackground(Theme.TABLE_SELECTED);
                    c.setForeground(Theme.TEXT);
                } else {
                    c.setBackground(
                        row % 2 == 0
                            ? Theme.SURFACE
                            : Theme.TABLE_ALT
                    );
                    c.setForeground(Theme.TEXT);
                }

                setBorder(
                    BorderFactory.createEmptyBorder(0, 12, 0, 8)
                );

                return c;
            }
        };

    t.setDefaultRenderer(Object.class, renderer);
    t.setDefaultRenderer(Number.class, renderer);

    return t;

    }

    // --------------------------------------------------
    // Scroll pane styling
    // --------------------------------------------------

    public static JScrollPane scrollPane(JTable table) {

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(Theme.SURFACE);

scrollPane.setBorder(
    BorderFactory.createLineBorder(
        Theme.BORDER
    )
);

        scrollPane.setBorder(
                BorderFactory.createLineBorder(GRID)
        );
        

        scrollPane.getViewport().setBackground(Theme.SURFACE);

        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        return scrollPane;
    }

    // --------------------------------------------------
    // Database query and table loading
    // --------------------------------------------------

    public static void query(JTable table, String sql) {

        try (
                Connection connection = DBConnection.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)
        ) {

            ResultSetMetaData metadata =
                    resultSet.getMetaData();

            Vector<String> columns = new Vector<>();

            for (int i = 1; i <= metadata.getColumnCount(); i++) {
                columns.add(metadata.getColumnLabel(i));
            }

            Vector<Vector<Object>> rows = new Vector<>();

            while (resultSet.next()) {

                Vector<Object> row = new Vector<>();

                for (int i = 1; i <= metadata.getColumnCount(); i++) {
                    row.add(resultSet.getObject(i));
                }

                rows.add(row);
            }

            DefaultTableModel model =
                    new DefaultTableModel(rows, columns) {

                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            table.setModel(model);

        } catch (Exception e) {

            error(table, e);
        }
    }

    // --------------------------------------------------
    // Single-value database query
    // --------------------------------------------------

    public static String scalar(String sql) {

        try (
                Connection connection = DBConnection.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)
        ) {

            return resultSet.next()
                    ? String.valueOf(resultSet.getObject(1))
                    : "0";

        } catch (Exception e) {

            return "—";
        }
    }

    // --------------------------------------------------
    // Error message
    // --------------------------------------------------

    public static void error(Component parent, Exception e) {

        JOptionPane.showMessageDialog(
                parent,
                "Operation failed:\n" + e.getMessage(),
                "HotelOS - Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    // --------------------------------------------------
    // Information message
    // --------------------------------------------------

    public static void info(Component parent, String message) {

        JOptionPane.showMessageDialog(
                parent,
                message,
                "HotelOS",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}
