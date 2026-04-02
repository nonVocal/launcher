package dev.nonvocal.launcher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * List-cell renderer for {@link LaunchEntry}.
 * <p>
 * Renders each row with a name label (left) and an action bar (right).
 * The action bar shows either individual icon buttons ({@code ICONS} mode)
 * or a single hamburger button that opens a popup ({@code HAMBURGER} mode).
 */
final class EntryCellRenderer extends JPanel implements ListCellRenderer<LaunchEntry>
{
    private static final long serialVersionUID = 1L;

    /** Width of each action button (px). */
    static final int ACT_W    = 22;
    /** FlowLayout horizontal gap around / between action buttons (px). */
    static final int ACT_HGAP = 3;

    /** Total action-bar width for {@code n} buttons. */
    static int barWidth(int n)
    {
        return (n + 1) * ACT_HGAP + n * ACT_W;
    }

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final Color ROW_EVEN     = new Color(0xF4, 0xF6, 0xF8);
    private static final Color ROW_ODD      = Color.WHITE;
    private static final Color FG_SCRIPT    = new Color(0x1A, 0x5F, 0x7A);
    private static final Color FG_FOLDER    = new Color(0x2E, 0x6B, 0x2E);
    private static final Color FG_PLAIN     = new Color(0x66, 0x55, 0x44);
    private static final Color SEL_BG       = new Color(0x00, 0x78, 0xD7);
    static final Color         ACT_FG       = new Color(0x33, 0x55, 0x99);
    static final Color         ACT_DEL      = new Color(0xAA, 0x22, 0x22);
    static final Color         ACT_BG       = new Color(0xE8, 0xEA, 0xF4);
    static final Color         ACT_BORD     = new Color(0xBB, 0xBB, 0xCC);
    private static final Color SEL_ACT_BG   = new Color(0x40, 0x90, 0xD7);
    private static final Color SEL_ACT_BORD = new Color(0x80, 0xB8, 0xFF);

    private static final Font CELL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    static final Font          ACT_FONT  = new Font(Font.SANS_SERIF, Font.BOLD, 9);

    // ── Per-action metadata (keyed by action key constant) ────────────────────
    static final Map<String, String>    ACT_TEXT_MAP = new LinkedHashMap<>();
    static final Map<String, String>    ACT_TIP_MAP  = new LinkedHashMap<>();
    static final Map<String, ImageIcon> ACT_ICON_MAP = new LinkedHashMap<>();

    static
    {
        ACT_TEXT_MAP.put(Launcher.EXPLORE_ACTION, "E");
        ACT_TEXT_MAP.put(Launcher.EDITOR_ACTION,  "ED");
        ACT_TEXT_MAP.put(Launcher.COPY_ACTION,    "C");
        ACT_TEXT_MAP.put(Launcher.DELETE_ACTION,  "\u2715");

        ACT_TIP_MAP.put(Launcher.EXPLORE_ACTION, "Open in File Explorer");
        ACT_TIP_MAP.put(Launcher.EDITOR_ACTION,  "Open in Editor");
        ACT_TIP_MAP.put(Launcher.COPY_ACTION,    "Copy with Robocopy\u2026");
        ACT_TIP_MAP.put(Launcher.DELETE_ACTION,  "Delete");

        ACT_ICON_MAP.put(Launcher.EXPLORE_ACTION, Launcher.loadScaledIcon("folder.png",        14, 14));
        ACT_ICON_MAP.put(Launcher.EDITOR_ACTION,  Launcher.loadScaledIcon("edit-document.png", 14, 14));
        ACT_ICON_MAP.put(Launcher.COPY_ACTION,    Launcher.loadScaledIcon("copy.png",          14, 14));
        ACT_ICON_MAP.put(Launcher.DELETE_ACTION,  Launcher.loadScaledIcon("bin.png",           14, 14));
    }

    // ── Instance ──────────────────────────────────────────────────────────────
    private final JLabel nameLabel = new JLabel();
    private final JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, ACT_HGAP, 0));
    private final List<String> actionOrder;
    private final String buttonStyle;
    private final JLabel[] actIcons;

    private final transient FileSystemView fsv = FileSystemView.getFileSystemView();

    EntryCellRenderer(List<String> actionOrder, String buttonStyle,
                      Map<String, CustomAction> customActionMap)
    {
        this.actionOrder = actionOrder;
        this.buttonStyle = buttonStyle;

        setLayout(new BorderLayout());
        setOpaque(true);

        nameLabel.setFont(CELL_FONT);
        nameLabel.setBorder(new EmptyBorder(5, 8, 5, 8));
        nameLabel.setOpaque(false);
        add(nameLabel, BorderLayout.CENTER);

        actionBar.setOpaque(false);
        // top/bottom padding: (36 - 18) / 2 = 9 px – centres 18 px buttons in 36 px row
        actionBar.setBorder(new EmptyBorder(9, 0, 9, 0));

        if (Launcher.BUTTON_STYLE_HAMBURGER.equals(buttonStyle))
        {
            // Single ☰ button – clicking opens the action popup
            actIcons = new JLabel[1];
            JLabel burger = makeButton("\u2630", "Actions", false); // ☰
            actIcons[0] = burger;
            actionBar.add(burger);
        }
        else
        {
            // One icon button per action (ICONS mode / default)
            int n = actionOrder.size();
            actIcons = new JLabel[n];
            for (int i = 0; i < n; i++)
            {
                String key = actionOrder.get(i);
                ImageIcon img = ACT_ICON_MAP.get(key);
                String    tip = ACT_TIP_MAP.getOrDefault(key, key);
                String    txt = ACT_TEXT_MAP.getOrDefault(key, "?");
                boolean isDel = Launcher.DELETE_ACTION.equals(key);

                // Fall back to custom action definition when not a built-in key
                if (img == null && !ACT_ICON_MAP.containsKey(key))
                {
                    CustomAction ca = customActionMap.get(key);
                    if (ca != null)
                    {
                        img  = ca.loadIcon(14, 14);
                        tip  = ca.effectiveTooltip();
                        txt  = ca.effectiveLabel().length() <= 3
                               ? ca.effectiveLabel()
                               : ca.effectiveLabel().substring(0, 2) + "…";
                        isDel = false;
                    }
                }

                JLabel lbl = (img != null)
                        ? makeIconButton(img, tip)
                        : makeButton(txt, tip, isDel);
                actIcons[i] = lbl;
                actionBar.add(lbl);
            }
        }
        add(actionBar, BorderLayout.EAST);
    }

    // ── ListCellRenderer ──────────────────────────────────────────────────────

    @Override
    public Component getListCellRendererComponent(
            JList<? extends LaunchEntry> list, LaunchEntry e, int index,
            boolean selected, boolean cellHasFocus)
    {
        nameLabel.setText("  " + e.file().getName());
        nameLabel.setToolTipText(e.file().getAbsolutePath());

        File iconSrc = (e.iconFile() != null) ? e.iconFile() : e.file();
        try   { nameLabel.setIcon(fsv.getSystemIcon(iconSrc)); }
        catch (Exception ignored) { nameLabel.setIcon(null); }

        actionBar.setVisible(e.type() != EntryType.SCRIPT && !actionOrder.isEmpty());

        if (selected)
        {
            setBackground(SEL_BG);
            nameLabel.setForeground(Color.WHITE);
            for (JLabel icon : actIcons)
            {
                icon.setBackground(SEL_ACT_BG);
                icon.setForeground(Color.WHITE);
                icon.setBorder(selectedBorder());
            }
        }
        else
        {
            setBackground(index % 2 == 0 ? ROW_EVEN : ROW_ODD);
            nameLabel.setForeground(
                    e.type() == EntryType.SCRIPT     ? FG_SCRIPT :
                    e.type() == EntryType.APP_FOLDER ? FG_FOLDER : FG_PLAIN);

            if (Launcher.BUTTON_STYLE_HAMBURGER.equals(buttonStyle))
            {
                // Hamburger button is always neutral (never delete-red)
                if (actIcons.length > 0) resetButton(actIcons[0], false);
            }
            else
            {
                for (int i = 0; i < actIcons.length; i++)
                    resetButton(actIcons[i], Launcher.DELETE_ACTION.equals(actionOrder.get(i)));
            }
        }
        return this;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static JLabel makeButton(String text, String tooltip, boolean isDelete)
    {
        JLabel lbl = new JLabel(text, JLabel.CENTER);
        lbl.setFont(ACT_FONT);
        lbl.setForeground(isDelete ? ACT_DEL : ACT_FG);
        lbl.setBackground(ACT_BG);
        lbl.setOpaque(true);
        lbl.setBorder(normalBorder());
        lbl.setToolTipText(tooltip);
        lbl.setPreferredSize(new Dimension(ACT_W, ACT_W - 4));
        return lbl;
    }

    private static JLabel makeIconButton(ImageIcon icon, String tooltip)
    {
        JLabel lbl = new JLabel(icon, JLabel.CENTER);
        lbl.setFont(ACT_FONT);
        lbl.setForeground(ACT_FG);
        lbl.setBackground(ACT_BG);
        lbl.setOpaque(true);
        lbl.setBorder(normalBorder());
        lbl.setToolTipText(tooltip);
        lbl.setPreferredSize(new Dimension(ACT_W, ACT_W - 4));
        return lbl;
    }

    private static void resetButton(JLabel lbl, boolean isDelete)
    {
        lbl.setBackground(ACT_BG);
        lbl.setForeground(isDelete ? ACT_DEL : ACT_FG);
        lbl.setBorder(normalBorder());
    }

    private static javax.swing.border.Border normalBorder()
    {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACT_BORD, 1),
                BorderFactory.createEmptyBorder(1, 3, 1, 3));
    }

    private static javax.swing.border.Border selectedBorder()
    {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SEL_ACT_BORD, 1),
                BorderFactory.createEmptyBorder(1, 3, 1, 3));
    }
}



