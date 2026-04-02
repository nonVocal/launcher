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
 * <p>
 * All colours are computed from the active Look-and-Feel at construction time
 * so that they automatically reflect the current light / dark theme.
 * The renderer is recreated whenever the theme changes (via {@link Launcher#applyConfig}).
 */
final class EntryCellRenderer extends JPanel implements ListCellRenderer<LaunchEntry>
{
    private static final long serialVersionUID = 1L;

    /** Width of each action button (px). */
    static final int ACT_W    = 22;
    /** FlowLayout horizontal gap around / between action buttons (px). */
    static final int ACT_HGAP = 3;

    /** Total action-bar width for {@code n} buttons. */
    static int barWidth(int n) { return (n + 1) * ACT_HGAP + n * ACT_W; }

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

    private static final Font CELL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    static final Font          ACT_FONT  = new Font(Font.SANS_SERIF, Font.BOLD,  9);

    // ── Theme-aware colours – computed per-instance at construction time ───────
    private final Color rowEven, rowOdd;
    private final Color fgScript, fgFolder, fgPlain;
    private final Color selBg;
    private final Color actFg, actDel, actBg, actBord;
    private final Color selActBg, selActBord;

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

        // ── Compute theme-aware colours BEFORE building child components ──────
        boolean dark   = isDark();
        Color   listBg = UIManager.getColor("List.background");
        rowOdd     = listBg != null ? listBg : (dark ? new Color(0x2B, 0x2D, 0x30) : Color.WHITE);
        rowEven    = dark ? blend(rowOdd, Color.WHITE, 0.04f) : new Color(0xF4, 0xF6, 0xF8);

        fgScript   = dark ? new Color(0x4F, 0xC1, 0xDA) : new Color(0x1A, 0x5F, 0x7A);
        fgFolder   = dark ? new Color(0x85, 0xBE, 0x6C) : new Color(0x2E, 0x6B, 0x2E);
        fgPlain    = dark ? new Color(0xC8, 0xB8, 0xA6) : new Color(0x66, 0x55, 0x44);

        Color selBgRaw = UIManager.getColor("List.selectionBackground");
        selBg      = selBgRaw != null ? selBgRaw : new Color(0x00, 0x78, 0xD7);

        actFg      = dark ? new Color(0x88, 0xBB, 0xFF) : new Color(0x33, 0x55, 0x99);
        actDel     = dark ? new Color(0xFF, 0x70, 0x70) : new Color(0xAA, 0x22, 0x22);
        actBg      = dark ? new Color(0x3A, 0x3C, 0x47) : new Color(0xE8, 0xEA, 0xF4);
        actBord    = dark ? new Color(0x50, 0x52, 0x60) : new Color(0xBB, 0xBB, 0xCC);
        selActBg   = dark ? new Color(0x2D, 0x6A, 0xB4) : new Color(0x40, 0x90, 0xD7);
        selActBord = dark ? new Color(0x4D, 0x8A, 0xD4) : new Color(0x80, 0xB8, 0xFF);

        // ── Build child components (colours already set above) ────────────────
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
            JLabel burger = makeButton("\u2630", "Actions", false);
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
                               : ca.effectiveLabel().substring(0, 2) + "\u2026";
                        isDel = false;
                    }
                }

                JLabel lbl = (img != null) ? makeIconButton(img, tip) : makeButton(txt, tip, isDel);
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

        // Custom icon from app type takes precedence over system icon
        if (e.appType() != null && e.appType().iconPath() != null)
        {
            ImageIcon customIcon = e.appType().loadIcon(16, 16);
            nameLabel.setIcon(customIcon);
        }
        else
        {
            File iconSrc = (e.iconFile() != null) ? e.iconFile() : e.file();
            try   { nameLabel.setIcon(fsv.getSystemIcon(iconSrc)); }
            catch (Exception ignored) { nameLabel.setIcon(null); }
        }

        actionBar.setVisible(e.type() != EntryType.SCRIPT && !actionOrder.isEmpty());

        if (selected)
        {
            setBackground(selBg);
            nameLabel.setForeground(Color.WHITE);
            for (JLabel icon : actIcons)
            {
                icon.setBackground(selActBg);
                icon.setForeground(Color.WHITE);
                icon.setBorder(selectedBorder());
            }
        }
        else
        {
            setBackground(index % 2 == 0 ? rowEven : rowOdd);
            nameLabel.setForeground(
                    e.type() == EntryType.SCRIPT     ? fgScript :
                    e.type() == EntryType.APP_FOLDER ? fgFolder : fgPlain);

            if (Launcher.BUTTON_STYLE_HAMBURGER.equals(buttonStyle))
            {
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

    // ── Instance helpers (use instance colour fields) ─────────────────────────

    private JLabel makeButton(String text, String tooltip, boolean isDelete)
    {
        JLabel lbl = new JLabel(text, JLabel.CENTER);
        lbl.setFont(ACT_FONT);
        lbl.setForeground(isDelete ? actDel : actFg);
        lbl.setBackground(actBg);
        lbl.setOpaque(true);
        lbl.setBorder(normalBorder());
        lbl.setToolTipText(tooltip);
        lbl.setPreferredSize(new Dimension(ACT_W, ACT_W - 4));
        return lbl;
    }

    private JLabel makeIconButton(ImageIcon icon, String tooltip)
    {
        JLabel lbl = new JLabel(icon, JLabel.CENTER);
        lbl.setFont(ACT_FONT);
        lbl.setForeground(actFg);
        lbl.setBackground(actBg);
        lbl.setOpaque(true);
        lbl.setBorder(normalBorder());
        lbl.setToolTipText(tooltip);
        lbl.setPreferredSize(new Dimension(ACT_W, ACT_W - 4));
        return lbl;
    }

    private void resetButton(JLabel lbl, boolean isDelete)
    {
        lbl.setBackground(actBg);
        lbl.setForeground(isDelete ? actDel : actFg);
        lbl.setBorder(normalBorder());
    }

    private javax.swing.border.Border normalBorder()
    {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(actBord, 1),
                BorderFactory.createEmptyBorder(1, 3, 1, 3));
    }

    private javax.swing.border.Border selectedBorder()
    {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(selActBord, 1),
                BorderFactory.createEmptyBorder(1, 3, 1, 3));
    }

    // ── Theme detection ───────────────────────────────────────────────────────

    /**
     * Returns {@code true} when the active Look-and-Feel is a dark theme.
     * Uses FlatLaf's API; falls back to {@code false} if FlatLaf is not on the classpath.
     */
    static boolean isDark()
    {
        try { return com.formdev.flatlaf.FlatLaf.isLafDark(); }
        catch (Throwable ignored) { return false; }
    }

    /**
     * Linear colour blend: 0.0 → pure {@code base}, 1.0 → pure {@code overlay}.
     */
    private static Color blend(Color base, Color overlay, float ratio)
    {
        float r = 1f - ratio;
        return new Color(
                Math.min(255, (int)(base.getRed()   * r + overlay.getRed()   * ratio)),
                Math.min(255, (int)(base.getGreen() * r + overlay.getGreen() * ratio)),
                Math.min(255, (int)(base.getBlue()  * r + overlay.getBlue()  * ratio)));
    }
}
