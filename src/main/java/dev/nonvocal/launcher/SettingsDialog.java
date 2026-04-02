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
    private final List<String>       effectiveToolbarActions;
    private final Consumer<LauncherConfig> onSave;

    SettingsDialog(JFrame owner, String launcherId, LauncherConfig config,
                   List<String> effectiveActionOrder, List<String> effectiveToolbarActions,
                   Consumer<LauncherConfig> onSave)
    {
        super(owner, "Settings  \u2013  " + launcherId, true);
        this.launcherId              = launcherId;
        this.config                  = config;
        this.effectiveActionOrder    = effectiveActionOrder;
        this.effectiveToolbarActions = effectiveToolbarActions;
        this.onSave                  = onSave;
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

        // ── Custom Actions ────────────────────────────────────────────────────
        root.add(sectionLabel("Custom Actions"));
        root.add(Box.createVerticalStrut(4));
        JLabel caHint = new JLabel("User-defined actions \u2013 appear in action bar and/or toolbar when added to those lists");
        caHint.setFont(caHint.getFont().deriveFont(Font.ITALIC, 10f));
        caHint.setForeground(Color.GRAY);
        caHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(caHint);
        root.add(Box.createVerticalStrut(4));

        List<CustomAction> customActionsList = config.customActions() != null
                ? new ArrayList<>(config.customActions()) : new ArrayList<>();
        DefaultListModel<CustomAction> caModel = new DefaultListModel<>();
        customActionsList.forEach(caModel::addElement);

        JList<CustomAction> caList = new JList<>(caModel);
        caList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        caList.setFixedCellHeight(24);
        caList.setCellRenderer((lst, value, index, isSelected, focus) ->
        {
            String scopeTag = CustomAction.SCOPE_ENTRY.equals(value.scope())   ? " [entry]"
                            : CustomAction.SCOPE_TOOLBAR.equals(value.scope()) ? " [toolbar]"
                            :                                                    " [both]";
            JLabel lbl = new JLabel(value.effectiveLabel() + "  (" + value.id() + ")" + scopeTag);
            lbl.setFont(lst.getFont().deriveFont(11f));
            lbl.setOpaque(true);
            lbl.setBorder(new EmptyBorder(2, 6, 2, 6));
            lbl.setBackground(isSelected ? lst.getSelectionBackground() : lst.getBackground());
            lbl.setForeground(isSelected ? lst.getSelectionForeground() : lst.getForeground());
            return lbl;
        });

        JButton caBtnAdd    = new JButton("Add");
        JButton caBtnEdit   = new JButton("Edit");
        JButton caBtnRemove = new JButton("Remove");

        caBtnAdd.addActionListener(ev ->
        {
            CustomAction newAction = showCustomActionEditor(null);
            if (newAction == null) return;
            for (int i = 0; i < caModel.getSize(); i++)
            {
                if (caModel.getElementAt(i).id().equals(newAction.id()))
                {
                    JOptionPane.showMessageDialog(this,
                            "An action with ID \"" + newAction.id() + "\" already exists.",
                            "Duplicate ID", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
            caModel.addElement(newAction);
        });
        caBtnEdit.addActionListener(ev ->
        {
            int idx = caList.getSelectedIndex();
            if (idx < 0) return;
            CustomAction edited = showCustomActionEditor(caModel.getElementAt(idx));
            if (edited != null) caModel.set(idx, edited);
        });
        caBtnRemove.addActionListener(ev ->
        {
            int idx = caList.getSelectedIndex();
            if (idx >= 0) caModel.remove(idx);
        });

        JPanel caBtnPanel = new JPanel(new GridLayout(3, 1, 0, 2));
        caBtnPanel.add(caBtnAdd);
        caBtnPanel.add(caBtnEdit);
        caBtnPanel.add(caBtnRemove);

        JPanel caPanel = new JPanel(new BorderLayout(6, 0));
        caPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        caPanel.add(new JScrollPane(caList), BorderLayout.CENTER);
        caPanel.add(caBtnPanel, BorderLayout.EAST);
        caPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 3 * 24 + 8));
        root.add(caPanel);
        root.add(Box.createVerticalStrut(8));
        root.add(separator());
        root.add(sectionLabel("Toolbar Buttons"));
        root.add(Box.createVerticalStrut(4));
        JLabel tbHint = new JLabel("Check to show \u00b7 drag up/down to reorder");
        tbHint.setFont(tbHint.getFont().deriveFont(Font.ITALIC, 10f));
        tbHint.setForeground(Color.GRAY);
        tbHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(tbHint);
        root.add(Box.createVerticalStrut(4));

        List<String> tbOrdered = new ArrayList<>(effectiveToolbarActions);
        for (String k : Launcher.DEFAULT_TOOLBAR_ACTIONS)
            if (!tbOrdered.contains(k)) tbOrdered.add(k);
        // Include only custom actions whose scope allows toolbar use
        if (config.customActions() != null)
            for (CustomAction ca : config.customActions())
                if (ca.appliesToToolbar() && !tbOrdered.contains(ca.id())) tbOrdered.add(ca.id());
        final Set<String> tbChecked = new HashSet<>(effectiveToolbarActions);

        DefaultListModel<String> tbModel = new DefaultListModel<>();
        tbOrdered.forEach(tbModel::addElement);

        JList<String> tbList = new JList<>(tbModel);
        tbList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tbList.setFixedCellHeight(24);
        tbList.setCellRenderer((lst, value, index, isSelected, focus) ->
        {
            JCheckBox cb = new JCheckBox(toolbarLabel(value));
            cb.setSelected(tbChecked.contains(value));
            cb.setBackground(isSelected ? lst.getSelectionBackground() : lst.getBackground());
            cb.setForeground(isSelected ? lst.getSelectionForeground() : lst.getForeground());
            cb.setFont(lst.getFont());
            return cb;
        });
        tbList.addMouseListener(new MouseAdapter()
        {
            @Override public void mouseClicked(MouseEvent ev)
            {
                int idx = tbList.locationToIndex(ev.getPoint());
                if (idx < 0) return;
                String key = tbModel.getElementAt(idx);
                if (tbChecked.contains(key)) tbChecked.remove(key); else tbChecked.add(key);
                tbList.repaint();
            }
        });

        JButton tbBtnUp   = new JButton("\u2191");
        JButton tbBtnDown = new JButton("\u2193");
        tbBtnUp.addActionListener(ev ->
        {
            int i = tbList.getSelectedIndex();
            if (i > 0) { String item = tbModel.remove(i); tbModel.add(i - 1, item); tbList.setSelectedIndex(i - 1); }
        });
        tbBtnDown.addActionListener(ev ->
        {
            int i = tbList.getSelectedIndex();
            if (i >= 0 && i < tbModel.getSize() - 1) { String item = tbModel.remove(i); tbModel.add(i + 1, item); tbList.setSelectedIndex(i + 1); }
        });

        JPanel tbActButtons = new JPanel(new GridLayout(2, 1, 0, 2));
        tbActButtons.add(tbBtnUp); tbActButtons.add(tbBtnDown);

        JPanel tbPanel = new JPanel(new BorderLayout(6, 0));
        tbPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        tbPanel.add(new JScrollPane(tbList), BorderLayout.CENTER);
        tbPanel.add(tbActButtons, BorderLayout.EAST);
        tbPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2 * 24 + 8));
        root.add(tbPanel);
        root.add(Box.createVerticalStrut(8));
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
        // Include only custom actions whose scope allows entry-bar use
        if (config.customActions() != null)
            for (CustomAction ca : config.customActions())
                if (ca.appliesToEntry() && !orderedKeys.contains(ca.id())) orderedKeys.add(ca.id());
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
            List<String> newToolbarActions = new ArrayList<>();
            for (int i = 0; i < tbModel.getSize(); i++)
            {
                String k = tbModel.getElementAt(i);
                if (tbChecked.contains(k)) newToolbarActions.add(k);
            }
            List<CustomAction> newCustomActions = new ArrayList<>();
            for (int i = 0; i < caModel.getSize(); i++) newCustomActions.add(caModel.getElementAt(i));

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
                    newButtonStyle, cbContextMenu.isSelected(),
                    newToolbarActions.isEmpty()  ? null : newToolbarActions,
                    newCustomActions.isEmpty()   ? null : newCustomActions));
            dispose();
        });
        btnCancel.addActionListener(e -> dispose());

        add(root);
    }

    // ── Static UI helpers ─────────────────────────────────────────────────────

    private String toolbarLabel(String key)
    {
        return switch (key)
        {
            case Launcher.SVN_CHECKOUT_ACTION -> "SVN Checkout";
            case Launcher.SVN_BROWSER_ACTION  -> "SVN Repository Browser";
            default ->
            {
                if (config.customActions() != null)
                    for (CustomAction ca : config.customActions())
                        if (ca.id().equals(key)) yield ca.effectiveLabel() + "  (" + key + ")";
                yield key;
            }
        };
    }

    private String actionLabel(String key)
    {
        return switch (key)
        {
            case Launcher.EXPLORE_ACTION -> "Open in File Explorer";
            case Launcher.EDITOR_ACTION  -> "Open in Editor";
            case Launcher.COPY_ACTION    -> "Copy with Robocopy";
            case Launcher.DELETE_ACTION  -> "Delete";
            default ->
            {
                if (config.customActions() != null)
                    for (CustomAction ca : config.customActions())
                        if (ca.id().equals(key)) yield ca.effectiveLabel() + "  (" + key + ")";
                yield key;
            }
        };
    }

    // ── Custom action editor ──────────────────────────────────────────────────

    /**
     * Shows a dialog for adding or editing a {@link CustomAction}.
     * Pass {@code null} to create a new action.
     * Returns the updated/new action, or {@code null} if the user cancelled.
     */
    private CustomAction showCustomActionEditor(CustomAction existing)
    {
        boolean isNew = (existing == null);

        JTextField tfId     = new JTextField(isNew ? "" : existing.id(), 24);
        JTextField tfLabel  = new JTextField(isNew ? "" : nvl(existing.label()),  24);
        JTextField tfScript = new JTextField(isNew ? "" : nvl(existing.scriptPath()), 32);
        JTextField tfIcon   = new JTextField(isNew ? "" : nvl(existing.iconPath()),   32);
        JTextField tfTip    = new JTextField(isNew ? "" : nvl(existing.tooltip()),    32);

        if (!isNew) tfId.setEditable(false); // ID must stay stable once created

        // Scope radio buttons
        JRadioButton rbEntry   = new JRadioButton("Entry action bar only");
        JRadioButton rbToolbar = new JRadioButton("Toolbar only");
        JRadioButton rbBoth    = new JRadioButton("Both (entry bar and toolbar)");
        ButtonGroup  bgScope   = new ButtonGroup();
        bgScope.add(rbEntry); bgScope.add(rbToolbar); bgScope.add(rbBoth);

        String currentScope = isNew ? null : existing.scope();
        if      (CustomAction.SCOPE_ENTRY.equals(currentScope))   rbEntry.setSelected(true);
        else if (CustomAction.SCOPE_TOOLBAR.equals(currentScope)) rbToolbar.setSelected(true);
        else                                                       rbBoth.setSelected(true);

        JPanel scopePanel = new JPanel(new GridLayout(3, 1, 0, 2));
        scopePanel.add(rbEntry);
        scopePanel.add(rbToolbar);
        scopePanel.add(rbBoth);

        JButton browseScript = new JButton("\u2026");
        browseScript.setToolTipText("Browse\u2026");
        browseScript.addActionListener(ev ->
        {
            JFileChooser fc = new JFileChooser(tfScript.getText().trim().isEmpty()
                    ? System.getProperty("user.home") : tfScript.getText().trim());
            fc.setDialogTitle("Select Script or Executable");
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
                tfScript.setText(fc.getSelectedFile().getAbsolutePath());
        });

        JButton browseIcon = new JButton("\u2026");
        browseIcon.setToolTipText("Browse\u2026");
        browseIcon.addActionListener(ev ->
        {
            JFileChooser fc = new JFileChooser(tfIcon.getText().trim().isEmpty()
                    ? System.getProperty("user.home") : tfIcon.getText().trim());
            fc.setDialogTitle("Select Icon Image");
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
                tfIcon.setText(fc.getSelectedFile().getAbsolutePath());
        });

        JPanel scriptRow = new JPanel(new BorderLayout(4, 0));
        scriptRow.add(tfScript, BorderLayout.CENTER);
        scriptRow.add(browseScript, BorderLayout.EAST);

        JPanel iconRow = new JPanel(new BorderLayout(4, 0));
        iconRow.add(tfIcon, BorderLayout.CENTER);
        iconRow.add(browseIcon, BorderLayout.EAST);

        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints lc = new GridBagConstraints();
        lc.anchor  = GridBagConstraints.NORTHWEST;
        lc.insets  = new Insets(3, 0, 3, 8);
        lc.gridx   = 0;

        GridBagConstraints fc2 = new GridBagConstraints();
        fc2.fill    = GridBagConstraints.HORIZONTAL;
        fc2.weightx = 1;
        fc2.insets  = new Insets(3, 0, 3, 0);
        fc2.gridx   = 1;

        String[]    lbls = {"ID (unique key):", "Scope *:", "Label:", "Script / Executable:", "Icon image path:", "Tooltip:"};
        Component[] flds = {tfId, scopePanel, tfLabel, scriptRow, iconRow, tfTip};
        for (int i = 0; i < lbls.length; i++)
        {
            lc.gridy = i; fc2.gridy = i;
            p.add(new JLabel(lbls[i]), lc);
            p.add(flds[i], fc2);
        }

        int result = JOptionPane.showConfirmDialog(this, p,
                isNew ? "Add Custom Action" : "Edit Custom Action",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return null;

        String id = tfId.getText().trim();
        if (id.isEmpty())
        {
            JOptionPane.showMessageDialog(this, "ID cannot be empty.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        if (!rbEntry.isSelected() && !rbToolbar.isSelected() && !rbBoth.isSelected())
        {
            JOptionPane.showMessageDialog(this, "Please select a scope.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        String scope      = rbEntry.isSelected()   ? CustomAction.SCOPE_ENTRY
                          : rbToolbar.isSelected() ? CustomAction.SCOPE_TOOLBAR
                          :                          CustomAction.SCOPE_BOTH;
        String scriptPath = tfScript.getText().trim();
        String iconPath   = tfIcon.getText().trim();
        String label      = tfLabel.getText().trim();
        String tooltip    = tfTip.getText().trim();

        return new CustomAction(id, scope,
                iconPath.isEmpty()   ? null : iconPath,
                scriptPath.isEmpty() ? null : scriptPath,
                label.isEmpty()      ? null : label,
                tooltip.isEmpty()    ? null : tooltip);
    }

    private static String nvl(String s) { return s != null ? s : ""; }

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

