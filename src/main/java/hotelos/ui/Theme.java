package hotelos.ui;

import java.awt.Color;
import java.awt.Font;

/** Central color and typography palette for both HotelOS themes. */
public final class Theme {
    private Theme() {}

    private static boolean dark = false;

    public static Color NAVY;
    public static Color NAVY2;
    public static Color TEAL;
    public static Color BG;
    public static Color SURFACE;
    public static Color TEXT;
    public static Color MUTED;
    public static Color BORDER;
    public static Color INPUT_BG;
    public static Color TABLE_ALT;
    public static Color TABLE_SELECTED;
    public static Color TABLE_HEADER;
    public static Color TABLE_GRID;
    public static Color BUTTON_BG;
    public static Color BUTTON_TEXT;

    public static final Color BLUE = new Color(59, 130, 246);
    public static final Color PURPLE = new Color(139, 92, 246);
    public static final Color AMBER = new Color(245, 158, 11);

    public static Font TITLE = new Font("Segoe UI", Font.BOLD, 25);
    public static Font HEADING = new Font("Segoe UI", Font.BOLD, 18);
    public static Font BODY = new Font("Segoe UI", Font.PLAIN, 15);
    public static Font BUTTON = new Font("Segoe UI", Font.BOLD, 14);
    public static Font TABLE = new Font("Segoe UI", Font.PLAIN, 14);

    static { apply(false); }

    public static boolean isDark() { return dark; }

    public static void apply(boolean useDark) {
        dark = useDark;
        if (dark) {
            NAVY = new Color(13, 22, 39);
            NAVY2 = new Color(24, 37, 59);
            TEAL = new Color(45, 190, 174);
            BG = new Color(20, 29, 45);
            SURFACE = new Color(30, 42, 61);
            TEXT = new Color(239, 244, 251);
            MUTED = new Color(183, 196, 214);
            BORDER = new Color(61, 78, 101);
            INPUT_BG = new Color(37, 51, 72);
            TABLE_ALT = new Color(35, 49, 69);
            TABLE_SELECTED = new Color(35, 91, 91);
            TABLE_HEADER = new Color(17, 29, 48);
            TABLE_GRID = new Color(56, 72, 94);
            BUTTON_BG = new Color(42, 57, 78);
            BUTTON_TEXT = new Color(240, 245, 252);
        } else {
            NAVY = new Color(12, 23, 42);
            NAVY2 = new Color(23, 40, 65);
            TEAL = new Color(20, 150, 140);
            BG = new Color(239, 244, 250);
            SURFACE = new Color(255, 255, 255);
            TEXT = new Color(22, 37, 62);
            MUTED = new Color(77, 93, 116);
            BORDER = new Color(203, 216, 232);
            INPUT_BG = Color.WHITE;
            TABLE_ALT = new Color(243, 247, 252);
            TABLE_SELECTED = new Color(204, 251, 241);
            TABLE_HEADER = new Color(23, 43, 77);
            TABLE_GRID = new Color(216, 226, 239);
            BUTTON_BG = new Color(246, 249, 253);
            BUTTON_TEXT = TEXT;
        }
    }
}
