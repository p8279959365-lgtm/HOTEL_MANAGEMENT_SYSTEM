package hotelos.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

/** Lightweight themed calendar picker; no extra library required. */
public final class CalendarPicker {
    private CalendarPicker() {}

    public static LocalDate choose(Component parent, LocalDate initial) {
        final LocalDate[] selected = {null};
        final YearMonth[] shown = {YearMonth.from(initial == null ? LocalDate.now() : initial)};
        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, "Select date", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JPanel root = new JPanel(new BorderLayout(8, 10));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));
        root.setBackground(Theme.SURFACE);
        JPanel top = new JPanel(new BorderLayout(8, 0)); top.setOpaque(false);
        JButton prev = new JButton("‹"), next = new JButton("›");
        JLabel monthLabel = new JLabel("", SwingConstants.CENTER); monthLabel.setForeground(Theme.TEXT); monthLabel.setFont(Theme.HEADING);
        top.add(prev, BorderLayout.WEST); top.add(monthLabel, BorderLayout.CENTER); top.add(next, BorderLayout.EAST);
        JPanel days = new JPanel(new GridLayout(0, 7, 4, 4)); days.setOpaque(false);
        JPanel calendar = new JPanel(new BorderLayout(0, 6)); calendar.setOpaque(false);
        calendar.add(days, BorderLayout.CENTER);
        Runnable render = () -> {
            monthLabel.setText(shown[0].getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + shown[0].getYear());
            days.removeAll();
            for (String d : new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"}) {
                JLabel label = new JLabel(d, SwingConstants.CENTER); label.setForeground(Theme.MUTED); label.setFont(Theme.BODY); days.add(label);
            }
            LocalDate first = shown[0].atDay(1);
            int offset = first.getDayOfWeek().getValue() - 1;
            for (int i = 0; i < offset; i++) days.add(new JLabel(""));
            for (int day = 1; day <= shown[0].lengthOfMonth(); day++) {
                LocalDate date = shown[0].atDay(day);
                JButton b = new JButton(Integer.toString(day)); b.setFocusPainted(false); b.setFont(Theme.BODY);
                b.setBackground(date.equals(initial) ? Theme.TEAL : Theme.INPUT_BG);
                b.setForeground(date.equals(initial) ? Color.WHITE : Theme.TEXT);
                b.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
                b.addActionListener(e -> { selected[0] = date; dialog.dispose(); });
                days.add(b);
            }
            days.revalidate(); days.repaint();
        };
        prev.addActionListener(e -> { shown[0] = shown[0].minusMonths(1); render.run(); });
        next.addActionListener(e -> { shown[0] = shown[0].plusMonths(1); render.run(); });
        render.run();
        JButton today = new JButton("Today"); today.addActionListener(e -> { selected[0] = LocalDate.now(); dialog.dispose(); });
        JButton cancel = new JButton("Cancel"); cancel.addActionListener(e -> dialog.dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT)); bottom.setOpaque(false); bottom.add(today); bottom.add(cancel);
        root.add(top, BorderLayout.NORTH); root.add(calendar, BorderLayout.CENTER); root.add(bottom, BorderLayout.SOUTH);
        dialog.setContentPane(root); dialog.pack(); dialog.setMinimumSize(new Dimension(330, 330)); dialog.setLocationRelativeTo(parent); dialog.setVisible(true);
        return selected[0];
    }
}
