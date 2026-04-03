package dev.nonvocal.launcher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.File;
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

    // ── Per-action metadata record ────────────────────────────────────────────

    /** Packs the text label, tooltip and icon for one built-in action. */
    record ActionMeta(String text, String tip, ImageIcon icon) {}

    /**
     * Fixed-size (4-element) ordered carrier for the built-in action definitions.
     * Replaces the three parallel {@code LinkedHashMap}s.
     * Component names match the action-key constants they represent.
     */
    record ActionCatalog(
            ActionMeta EXPLORE_ACTION,
            ActionMeta EDITOR_ACTION,
            ActionMeta COPY_ACTION,
            ActionMeta DELETE_ACTION)
    {
        /** @return the {@link ActionMeta} for {@code key}, or {@code null} if not registered. */
        ActionMeta get(String key)
        {
            return switch (key)
            {
                case Launcher.EXPLORE_ACTION -> EXPLORE_ACTION();
                case Launcher.EDITOR_ACTION  -> EDITOR_ACTION();
                case Launcher.COPY_ACTION    -> COPY_ACTION();
                case Launcher.DELETE_ACTION  -> DELETE_ACTION();
                default                      -> null;
            };
        }

        /** @return {@code true} if {@code key} has a registered entry. */
        boolean contains(String key) { return get(key) != null; }
    }

    // ── Per-action metadata (keyed by action key constant) ────────────────────
    static final ActionCatalog ACT_CATALOG = new ActionCatalog(
            new ActionMeta("E",      "Open in File Explorer",    Launcher.loadScaledIcon("folder.png",        14, 14)),
            new ActionMeta("ED",     "Open in Editor",           Launcher.loadScaledIcon("edit-document.png", 14, 14)),
            new ActionMeta("C",      "Copy with Robocopy\u2026", Launcher.loadScaledIcon("copy.png",          14, 14)),
            new ActionMeta("\u2715", "Delete",                   Launcher.loadScaledIcon("bin.png",           14, 14)));

    private static final Font     CELL_FONT   = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    static final Font              ACT_FONT    = new Font(Font.SANS_SERIF, Font.BOLD,  9);
    private static final Dimension BUTTON_SIZE = new Dimension(ACT_W, ACT_W - 4);

    // ── Instance ──────────────────────────────────────────────────────────────
    private final ColorTheme theme;
    private final javax.swing.border.Border btnNormalBorder;
    private final javax.swing.border.Border btnSelectedBorder;
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

        // ── Resolve theme-aware colours against the active Look-and-Feel ──────
        theme = ColorTheme.forCurrentLaf();
        btnNormalBorder   = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.actBord, 1),
                BorderFactory.createEmptyBorder(1, 3, 1, 3));
        btnSelectedBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.selActBord, 1),
                BorderFactory.createEmptyBorder(1, 3, 1, 3));

        // ── Build child components ────────────────────────────────────────────
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
                JLabel lbl = switch (ACT_CATALOG.get(key))
                {
                    case ActionMeta(var txt, var tip, var img) ->
                            (img != null) ? makeIconButton(img, tip) : makeButton(txt, tip, Launcher.DELETE_ACTION.equals(key));
                    case null ->
                        // Fall back to custom action definition when not a built-in key
                        switch (customActionMap.get(key))
                        {
                            case CustomAction ca ->
                            {
                                ImageIcon img  = ca.loadIcon(14, 14);
                                String    tip  = ca.effectiveTooltip();
                                String    txt  = ca.effectiveLabel().length() <= 3
                                        ? ca.effectiveLabel()
                                        : ca.effectiveLabel().substring(0, 2) + "\u2026";
                                yield (img != null) ? makeIconButton(img, tip) : makeButton(txt, tip, false);
                            }
                            case null -> makeButton("?", key, false);
                        };
                };

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
            setBackground(theme.selBg);
            nameLabel.setForeground(Color.WHITE);
            for (JLabel icon : actIcons)
            {
                icon.setBackground(theme.selActBg);
                icon.setForeground(Color.WHITE);
                icon.setBorder(selectedBorder());
            }
        }
        else
        {
            setBackground((index & 1) == 0 ? theme.rowEven : theme.rowOdd);
            nameLabel.setForeground(switch (e.type())
            {
                case SCRIPT     -> theme.fgScript;
                case APP_FOLDER -> theme.fgFolder;
                default         -> theme.fgPlain;
            });

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
        lbl.setForeground(isDelete ? theme.actDel : theme.actFg);
        lbl.setBackground(theme.actBg);
        lbl.setOpaque(true);
        lbl.setBorder(normalBorder());
        lbl.setToolTipText(tooltip);
        lbl.setPreferredSize(BUTTON_SIZE);
        return lbl;
    }

    private JLabel makeIconButton(ImageIcon icon, String tooltip)
    {
        JLabel lbl = new JLabel(icon, JLabel.CENTER);
        lbl.setFont(ACT_FONT);
        lbl.setForeground(theme.actFg);
        lbl.setBackground(theme.actBg);
        lbl.setOpaque(true);
        lbl.setBorder(normalBorder());
        lbl.setToolTipText(tooltip);
        lbl.setPreferredSize(BUTTON_SIZE);
        return lbl;
    }

    private void resetButton(JLabel lbl, boolean isDelete)
    {
        lbl.setBackground(theme.actBg);
        lbl.setForeground(isDelete ? theme.actDel : theme.actFg);
        lbl.setBorder(normalBorder());
    }

    private javax.swing.border.Border normalBorder()
    {
        return btnNormalBorder;
    }

    private javax.swing.border.Border selectedBorder()
    {
        return btnSelectedBorder;
    }

}
