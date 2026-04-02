package dev.nonvocal.launcher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Modal settings dialog.
 * Calls {@code onSave} with the updated {@link LauncherConfig} when the user clicks Save.
 */
class SettingsDialog extends JDialog
{
    private final String             launcherId;
    private final LauncherConfig     config;
    private final List<String>       effectiveActionOrder;
    private final Consumer<LauncherConfig> onSave;

    SettingsDialog(JFrame owner, String launcherId, LauncherConfig config,
                   List<String> effectiveActionOrder, Consumer<LauncherConfig> onSave)
    {
        super(owner, "Settings  \u2013  " + launcherId, true);
        this.launcherId           = launcherId;
        this.config               = config;
        this.effectiveActionOrder = effectiveActionOrder;
        this.onSave               = onSave;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        buildContent();
        pack();
    }

    // ── Content ───────────────────────────────────────────────────────────────

    private void buildContent()
    {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(14, 16, 10, 16));

        // Config-file info
        root.add(sectionLabel("Configuration files"));
        root.add(Box.createVerticalStrut(4));
        root.add(infoRow("Launcher ID",     launcherId, null));
        root.add(Box.createVerticalStrut(2));
        root.add(infoRow("Global config",
                LauncherConfig.globalConfigFile().getAbsolutePath(),
                LauncherConfig.globalConfigFile().getParentFile()));
        root.add(Box.createVerticalStrut(2));
        root.add(infoRow("Instance config",
                LauncherConfig.instanceConfigFile(launcherId).getAbsolutePath(),
                LauncherConfig.instanceConfigFile(launcherId).getParentFile()));
        root.add(separator());

        // Startup
        root.add(sectionLabel("Startup"));
        root.add(Box.createVerticalStrut(6));
        JCheckBox cbMinimized = new JCheckBox(
                "Start minimized to system tray  (takes effect on next launch)");
        cbMinimized.setSelected(Boolean.TRUE.equals(config.startMinimized()));
        cbMinimized.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(cbMinimized);
        root.add(separator());

        // Commands
        root.add(sectionLabel("Commands"));
        root.add(Box.createVerticalStrut(6));
        JTextField tfExplorer = new JTextField(config.explorer() != null ? config.explorer() : "", 30);
        root.add(editRow("EXPLORER", tfExplorer,
                "File explorer command \u2013 blank uses the system default"));
        root.add(Box.createVerticalStrut(4));
        JTextField tfEditor = new JTextField(config.editor() != null ? config.editor() : "", 30);
        root.add(editRow("EDITOR", tfEditor,
                "Editor command \u2013 blank defaults to 'code'"));
        root.add(separator());

        // Action buttons
        root.add(sectionLabel("Action Buttons"));
        root.add(Box.createVerticalStrut(4));
        JLabel actHint = new JLabel("Check to show \u00b7 drag up/down to reorder");
        actHint.setFont(actHint.getFont().deriveFont(Font.ITALIC, 10f));
        actHint.setForeground(Color.GRAY);
        actHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(actHint);
        root.add(Box.createVerticalStrut(4));

        List<String> orderedKeys = new ArrayList<>(effectiveActionOrder);
        for (String k : Launcher.DEFAULT_ACTION_ORDER)
            if (!orderedKeys.contains(k)) orderedKeys.add(k);
        final Set<String> checked = new HashSet<>(effectiveActionOrder);

        DefaultListModel<String> actModel = new DefaultListModel<>();
        orderedKeys.forEach(actModel::addElement);

        JList<String> actList = new JList<>(actModel);
        actList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        actList.setFixedCellHeight(24);
        actList.setCellRenderer((lst, value, index, isSelected, focus) ->
        {
            JCheckBox cb = new JCheckBox(actionLabel(value));
            cb.setSelected(checked.contains(value));
            cb.setBackground(isSelected ? lst.getSelectionBackground() : lst.getBackground());
            cb.setForeground(isSelected ? lst.getSelectionForeground() : lst.getForeground());
            cb.setFont(lst.getFont());
            return cb;
        });
        actList.addMouseListener(new MouseAdapter()
        {
            @Override public void mouseClicked(MouseEvent ev)
            {
                int idx = actList.locationToIndex(ev.getPoint());
                if (idx < 0) return;
                String key = actModel.getElementAt(idx);
                if (checked.contains(key)) checked.remove(key); else checked.add(key);
                actList.repaint();
            }
        });

        JButton btnUp   = new JButton("\u2191");
        JButton btnDown = new JButton("\u2193");
        btnUp.addActionListener(ev ->
        {
            int i = actList.getSelectedIndex();
            if (i > 0) { String item = actModel.remove(i); actModel.add(i - 1, item); actList.setSelectedIndex(i - 1); }
        });
        btnDown.addActionListener(ev ->
        {
            int i = actList.getSelectedIndex();
            if (i >= 0 && i < actModel.getSize() - 1) { String item = actModel.remove(i); actModel.add(i + 1, item); actList.setSelectedIndex(i + 1); }
        });

        JPanel actButtons = new JPanel(new GridLayout(2, 1, 0, 2));
        actButtons.add(btnUp); actButtons.add(btnDown);

        JPanel actPanel = new JPanel(new BorderLayout(6, 0));
        actPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        actPanel.add(new JScrollPane(actList), BorderLayout.CENTER);
        actPanel.add(actButtons, BorderLayout.EAST);
        actPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4 * 24 + 8));
        root.add(actPanel);
        root.add(Box.createVerticalStrut(8));

        // Button style
        root.add(sectionLabel("Button Style"));
        root.add(Box.createVerticalStrut(4));
        JLabel styleHint = new JLabel("Choose how entry action buttons appear in the list");
        styleHint.setFont(styleHint.getFont().deriveFont(Font.ITALIC, 10f));
        styleHint.setForeground(Color.GRAY);
        styleHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(styleHint);
        root.add(Box.createVerticalStrut(4));
        JRadioButton rbIcons     = new JRadioButton("Inline icons  \u2013 one small button per action");
        JRadioButton rbHamburger = new JRadioButton("Hamburger menu (\u2630) \u2013 single button opens a popup");
        new ButtonGroup() {{ add(rbIcons); add(rbHamburger); }};
        boolean isHamburger = Launcher.BUTTON_STYLE_HAMBURGER.equals(config.entryButtonStyle());
        rbHamburger.setSelected(isHamburger);
        rbIcons.setSelected(!isHamburger);
        rbIcons.setAlignmentX(Component.LEFT_ALIGNMENT);
        rbHamburger.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(rbIcons);
        root.add(rbHamburger);
        root.add(Box.createVerticalStrut(6));

        // Context menu toggle
        JCheckBox cbContextMenu = new JCheckBox("Show right-click context menu for folder entries");
        cbContextMenu.setSelected(!Boolean.FALSE.equals(config.showContextMenu()));
        cbContextMenu.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(cbContextMenu);
        root.add(separator());

        // Save / Cancel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton btnSave   = new JButton("Save");
        JButton btnCancel = new JButton("Cancel");
        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);
        root.add(btnPanel);

        btnSave.addActionListener(e ->
        {
            List<String> newOrder = new ArrayList<>();
            for (int i = 0; i < actModel.getSize(); i++)
            {
                String k = actModel.getElementAt(i);
                if (checked.contains(k)) newOrder.add(k);
            }
            String explorerVal    = tfExplorer.getText().trim();
            String editorVal      = tfEditor.getText().trim();
            String newButtonStyle = rbHamburger.isSelected()
                    ? Launcher.BUTTON_STYLE_HAMBURGER : Launcher.BUTTON_STYLE_ICONS;

            onSave.accept(new LauncherConfig(
                    config.rootFolder(), cbMinimized.isSelected(),
                    config.windowWidth(), config.windowHeight(),
                    config.priorityList(),
                    explorerVal.isEmpty() ? null : explorerVal,
                    editorVal.isEmpty()   ? null : editorVal,
                    newOrder.isEmpty()    ? null : newOrder,
                    newButtonStyle, cbContextMenu.isSelected()));
            dispose();
        });
        btnCancel.addActionListener(e -> dispose());

        add(root);
    }

    // ── Static UI helpers ─────────────────────────────────────────────────────

    private static String actionLabel(String key)
    {
        return switch (key)
        {
            case Launcher.EXPLORE_ACTION -> "Open in File Explorer";
            case Launcher.EDITOR_ACTION  -> "Open in Editor";
            case Launcher.COPY_ACTION    -> "Copy with Robocopy";
            case Launcher.DELETE_ACTION  -> "Delete";
            default -> key;
        };
    }

    private static JLabel sectionLabel(String text)
    {
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 11f));
        lbl.setForeground(new Color(0x00, 0x50, 0x99));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private static JSeparator separator()
    {
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        return sep;
    }

    private static JPanel infoRow(String label, String value, File openDir)
    {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

        JLabel lbl = new JLabel(label + ":");
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 11f));
        lbl.setPreferredSize(new Dimension(110, 20));
        row.add(lbl, BorderLayout.WEST);

        JTextField tf = new JTextField(value);
        tf.setEditable(false);
        tf.setFont(tf.getFont().deriveFont(11f));
        tf.setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 4));
        tf.setBackground(UIManager.getColor("Panel.background"));
        row.add(tf, BorderLayout.CENTER);

        if (openDir != null)
        {
            ImageIcon ico = Launcher.loadScaledIcon("folder.png", 14, 14);
            JButton btn = ico != null ? new JButton(ico) : new JButton("\uD83D\uDCC2");
            btn.setToolTipText("Open folder");
            btn.setFocusPainted(false);
            btn.setMargin(new Insets(1, 4, 1, 4));
            final File dir = openDir;
            btn.addActionListener(e ->
            {
                try { Desktop.getDesktop().open(dir); } catch (Exception ignored) {}
            });
            row.add(btn, BorderLayout.EAST);
        }
        return row;
    }

    private static JPanel editRow(String label, JTextField field, String tooltip)
    {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        JLabel lbl = new JLabel(label + ":");
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 11f));
        lbl.setPreferredSize(new Dimension(110, 20));
        lbl.setToolTipText(tooltip);
        row.add(lbl, BorderLayout.WEST);
        field.setFont(field.getFont().deriveFont(11f));
        field.setToolTipText(tooltip);
        row.add(field, BorderLayout.CENTER);
        return row;
    }
}

