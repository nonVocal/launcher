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
    private final List<String>       knownFolderNames;
    private final Consumer<LauncherConfig> onSave;

    SettingsDialog(JFrame owner, String launcherId, LauncherConfig config,
                   List<String> effectiveActionOrder, List<String> effectiveToolbarActions,
                   List<String> knownFolderNames,
                   Consumer<LauncherConfig> onSave)
    {
        super(owner, "Settings  \u2013  " + launcherId, true);
        this.launcherId              = launcherId;
        this.config                  = config;
        this.effectiveActionOrder    = effectiveActionOrder;
        this.effectiveToolbarActions = effectiveToolbarActions;
        this.knownFolderNames        = knownFolderNames;
        this.onSave                  = onSave;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        buildContent();
        pack();
    }

    // ── Content ───────────────────────────────────────────────────────────────

    private void buildContent()
    {
        JPanel main = new JPanel(new BorderLayout(0, 8));
        main.setBorder(new EmptyBorder(10, 14, 10, 14));

        JTabbedPane tabs = new JTabbedPane();

        // ── Tab 1: General ────────────────────────────────────────────────────
        JPanel tabGeneral = new JPanel();
        tabGeneral.setLayout(new BoxLayout(tabGeneral, BoxLayout.Y_AXIS));
        tabGeneral.setBorder(new EmptyBorder(10, 6, 6, 6));

        tabGeneral.add(sectionLabel("Configuration files"));
        tabGeneral.add(Box.createVerticalStrut(4));
        tabGeneral.add(infoRow("Launcher ID",     launcherId, null));
        tabGeneral.add(Box.createVerticalStrut(2));
        tabGeneral.add(infoRow("Global config",
                LauncherConfig.globalConfigFile().getAbsolutePath(),
                LauncherConfig.globalConfigFile().getParentFile()));
        tabGeneral.add(Box.createVerticalStrut(2));
        tabGeneral.add(infoRow("Instance config",
                LauncherConfig.instanceConfigFile(launcherId).getAbsolutePath(),
                LauncherConfig.instanceConfigFile(launcherId).getParentFile()));
        tabGeneral.add(separator());

        tabGeneral.add(sectionLabel("Startup"));
        tabGeneral.add(Box.createVerticalStrut(6));
        JCheckBox cbMinimized = new JCheckBox(
                "Start minimized to system tray  (takes effect on next launch)");
        cbMinimized.setSelected(Boolean.TRUE.equals(config.startMinimized()));
        cbMinimized.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabGeneral.add(cbMinimized);
        tabGeneral.add(separator());

        tabGeneral.add(sectionLabel("Appearance"));
        tabGeneral.add(Box.createVerticalStrut(4));
        JLabel themeHint = new JLabel("Choose between light mode, dark mode, or follow the system setting");
        themeHint.setFont(themeHint.getFont().deriveFont(Font.ITALIC, 10f));
        themeHint.setForeground(Color.GRAY);
        themeHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabGeneral.add(themeHint);
        tabGeneral.add(Box.createVerticalStrut(4));
        JRadioButton rbThemeSystem = new JRadioButton("System default  \u2013  follow the OS dark/light preference");
        JRadioButton rbThemeLight  = new JRadioButton("Light mode");
        JRadioButton rbThemeDark   = new JRadioButton("Dark mode");
        new ButtonGroup() {{ add(rbThemeSystem); add(rbThemeLight); add(rbThemeDark); }};
        if      (Launcher.THEME_DARK.equals(config.theme()))  rbThemeDark.setSelected(true);
        else if (Launcher.THEME_LIGHT.equals(config.theme())) rbThemeLight.setSelected(true);
        else                                                   rbThemeSystem.setSelected(true);
        rbThemeSystem.setAlignmentX(Component.LEFT_ALIGNMENT);
        rbThemeLight.setAlignmentX(Component.LEFT_ALIGNMENT);
        rbThemeDark.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabGeneral.add(rbThemeSystem);
        tabGeneral.add(rbThemeLight);
        tabGeneral.add(rbThemeDark);
        tabGeneral.add(Box.createVerticalStrut(6));

        // ── Accent colour ─────────────────────────────────────────────────────
        final Color defaultAccent = new Color(0x00, 0x78, 0xD7);
        final Color[] selectedAccent = { Launcher.parseHexColor(config.accentColor(), null) };

        JLabel accentLbl = new JLabel("Accent color:");
        accentLbl.setFont(accentLbl.getFont().deriveFont(Font.BOLD, 11f));
        accentLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Swatch – shows the current (or default) colour
        JPanel accentSwatch = new JPanel()
        {
            @Override protected void paintComponent(Graphics g)
            {
                super.paintComponent(g);
                g.setColor(selectedAccent[0] != null ? selectedAccent[0] : defaultAccent);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        accentSwatch.setPreferredSize(new Dimension(22, 22));
        accentSwatch.setMinimumSize(new Dimension(22, 22));
        accentSwatch.setMaximumSize(new Dimension(22, 22));
        accentSwatch.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor") != null
                ? UIManager.getColor("Component.borderColor") : Color.GRAY));
        accentSwatch.setOpaque(false);

        JButton accentChooseBtn = new JButton("Choose\u2026");
        accentChooseBtn.addActionListener(ev ->
        {
            Color initial = selectedAccent[0] != null ? selectedAccent[0] : defaultAccent;
            Color chosen  = JColorChooser.showDialog(this, "Choose Accent Color", initial);
            if (chosen != null) { selectedAccent[0] = chosen; accentSwatch.repaint(); }
        });

        JButton accentResetBtn = new JButton("Reset to default");
        accentResetBtn.addActionListener(ev -> { selectedAccent[0] = null; accentSwatch.repaint(); });

        JPanel accentRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        accentRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        accentRow.add(accentLbl);
        accentRow.add(accentSwatch);
        accentRow.add(accentChooseBtn);
        accentRow.add(accentResetBtn);
        tabGeneral.add(accentRow);
        tabGeneral.add(separator());

        tabGeneral.add(sectionLabel("Commands"));
        tabGeneral.add(Box.createVerticalStrut(6));
        JTextField tfExplorer = new JTextField(config.explorer() != null ? config.explorer() : "", 30);
        tabGeneral.add(editRow("EXPLORER", tfExplorer,
                "File explorer command \u2013 blank uses the system default"));
        tabGeneral.add(Box.createVerticalStrut(4));
        JTextField tfEditor = new JTextField(config.editor() != null ? config.editor() : "", 30);
        tabGeneral.add(editRow("EDITOR", tfEditor,
                "Editor command \u2013 blank defaults to 'code'"));
        tabGeneral.add(separator());

        tabGeneral.add(sectionLabel("Button Style"));
        tabGeneral.add(Box.createVerticalStrut(4));
        JLabel styleHint = new JLabel("Choose how entry action buttons appear in the list");
        styleHint.setFont(styleHint.getFont().deriveFont(Font.ITALIC, 10f));
        styleHint.setForeground(Color.GRAY);
        styleHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabGeneral.add(styleHint);
        tabGeneral.add(Box.createVerticalStrut(4));
        JRadioButton rbIcons     = new JRadioButton("Inline icons  \u2013 one small button per action");
        JRadioButton rbHamburger = new JRadioButton("Hamburger menu (\u2630) \u2013 single button opens a popup");
        new ButtonGroup() {{ add(rbIcons); add(rbHamburger); }};
        boolean isHamburger = Launcher.BUTTON_STYLE_HAMBURGER.equals(config.entryButtonStyle());
        rbHamburger.setSelected(isHamburger);
        rbIcons.setSelected(!isHamburger);
        rbIcons.setAlignmentX(Component.LEFT_ALIGNMENT);
        rbHamburger.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabGeneral.add(rbIcons);
        tabGeneral.add(rbHamburger);
        tabGeneral.add(Box.createVerticalStrut(6));

        JCheckBox cbContextMenu = new JCheckBox("Show right-click context menu for folder entries");
        cbContextMenu.setSelected(!Boolean.FALSE.equals(config.showContextMenu()));
        cbContextMenu.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabGeneral.add(cbContextMenu);

        tabs.addTab("General", new JScrollPane(tabGeneral));

        // ── Tab 2: Custom Actions ─────────────────────────────────────────────
        JPanel tabCustomActions = new JPanel();
        tabCustomActions.setLayout(new BoxLayout(tabCustomActions, BoxLayout.Y_AXIS));
        tabCustomActions.setBorder(new EmptyBorder(10, 6, 6, 6));

        tabCustomActions.add(sectionLabel("Custom Actions"));
        tabCustomActions.add(Box.createVerticalStrut(4));
        JLabel caHint = new JLabel("User-defined actions \u2013 appear in action bar and/or toolbar when added to those lists");
        caHint.setFont(caHint.getFont().deriveFont(Font.ITALIC, 10f));
        caHint.setForeground(Color.GRAY);
        caHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabCustomActions.add(caHint);
        tabCustomActions.add(Box.createVerticalStrut(4));

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

        JPanel caListPanel = new JPanel(new BorderLayout(6, 0));
        caListPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        caListPanel.add(new JScrollPane(caList), BorderLayout.CENTER);
        caListPanel.add(caBtnPanel, BorderLayout.EAST);
        tabCustomActions.add(caListPanel);

        tabs.addTab("Custom Actions", new JScrollPane(tabCustomActions));

        // ── Tab 3: Action Buttons ─────────────────────────────────────────────
        JPanel tabActions = new JPanel();
        tabActions.setLayout(new BoxLayout(tabActions, BoxLayout.Y_AXIS));
        tabActions.setBorder(new EmptyBorder(10, 6, 6, 6));

        tabActions.add(sectionLabel("Toolbar"));
        tabActions.add(Box.createVerticalStrut(4));
        JLabel tbHint = new JLabel("Check to show \u00b7 drag up/down to reorder");
        tbHint.setFont(tbHint.getFont().deriveFont(Font.ITALIC, 10f));
        tbHint.setForeground(Color.GRAY);
        tbHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabActions.add(tbHint);
        tabActions.add(Box.createVerticalStrut(4));

        List<String> tbOrdered = new ArrayList<>(effectiveToolbarActions);
        for (String k : Launcher.DEFAULT_TOOLBAR_ACTIONS)
            if (!tbOrdered.contains(k)) tbOrdered.add(k);
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
        tabActions.add(tbPanel);
        tabActions.add(Box.createVerticalStrut(8));
        tabActions.add(separator());

        tabActions.add(sectionLabel("Action Buttons"));
        tabActions.add(Box.createVerticalStrut(4));
        JLabel actHint = new JLabel("Check to show \u00b7 drag up/down to reorder");
        actHint.setFont(actHint.getFont().deriveFont(Font.ITALIC, 10f));
        actHint.setForeground(Color.GRAY);
        actHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabActions.add(actHint);
        tabActions.add(Box.createVerticalStrut(4));

        List<String> orderedKeys = new ArrayList<>(effectiveActionOrder);
        for (String k : Launcher.DEFAULT_ACTION_ORDER)
            if (!orderedKeys.contains(k)) orderedKeys.add(k);
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
        tabActions.add(actPanel);

        tabs.addTab("Action Buttons", new JScrollPane(tabActions));

        // ── Tab 4: App Types ──────────────────────────────────────────────────
        JPanel tabAppTypes = new JPanel();
        tabAppTypes.setLayout(new BoxLayout(tabAppTypes, BoxLayout.Y_AXIS));
        tabAppTypes.setBorder(new EmptyBorder(10, 6, 6, 6));

        tabAppTypes.add(sectionLabel("Application Types"));
        tabAppTypes.add(Box.createVerticalStrut(4));
        JLabel atHint = new JLabel("Define how to detect and display a category of application folder");
        atHint.setFont(atHint.getFont().deriveFont(Font.ITALIC, 10f));
        atHint.setForeground(Color.GRAY);
        atHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabAppTypes.add(atHint);
        tabAppTypes.add(Box.createVerticalStrut(4));

        List<AppType> appTypesList = config.appTypes() != null
                ? new ArrayList<>(config.appTypes()) : new ArrayList<>();
        DefaultListModel<AppType> atModel = new DefaultListModel<>();
        appTypesList.forEach(atModel::addElement);

        JList<AppType> atList = new JList<>(atModel);
        atList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        atList.setFixedCellHeight(24);
        atList.setCellRenderer((lst, value, index, isSelected, focus) ->
        {
            JLabel lbl = new JLabel(value.id()
                    + (value.executableNames() != null && !value.executableNames().isEmpty()
                       ? "  \u2192 " + String.join(", ", value.executableNames()) : ""));
            lbl.setFont(lst.getFont().deriveFont(11f));
            lbl.setOpaque(true);
            lbl.setBorder(new EmptyBorder(2, 6, 2, 6));
            lbl.setBackground(isSelected ? lst.getSelectionBackground() : lst.getBackground());
            lbl.setForeground(isSelected ? lst.getSelectionForeground() : lst.getForeground());
            return lbl;
        });

        JButton atBtnAdd    = new JButton("Add");
        JButton atBtnEdit   = new JButton("Edit");
        JButton atBtnRemove = new JButton("Remove");

        atBtnAdd.addActionListener(ev ->
        {
            AppType newType = showAppTypeEditor(null);
            if (newType == null) return;
            for (int i = 0; i < atModel.getSize(); i++)
            {
                if (atModel.getElementAt(i).id().equals(newType.id()))
                {
                    JOptionPane.showMessageDialog(this,
                            "An application type with ID \"" + newType.id() + "\" already exists.",
                            "Duplicate ID", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
            atModel.addElement(newType);
        });
        atBtnEdit.addActionListener(ev ->
        {
            int idx = atList.getSelectedIndex();
            if (idx < 0) return;
            AppType edited = showAppTypeEditor(atModel.getElementAt(idx));
            if (edited != null) atModel.set(idx, edited);
        });
        atBtnRemove.addActionListener(ev ->
        {
            int idx = atList.getSelectedIndex();
            if (idx >= 0) atModel.remove(idx);
        });

        JPanel atBtnPanel = new JPanel(new GridLayout(3, 1, 0, 2));
        atBtnPanel.add(atBtnAdd);
        atBtnPanel.add(atBtnEdit);
        atBtnPanel.add(atBtnRemove);

        JPanel atListPanel = new JPanel(new BorderLayout(6, 0));
        atListPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        atListPanel.add(new JScrollPane(atList), BorderLayout.CENTER);
        atListPanel.add(atBtnPanel, BorderLayout.EAST);
        atListPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 3 * 24 + 8));
        tabAppTypes.add(atListPanel);
        tabAppTypes.add(Box.createVerticalStrut(8));
        tabAppTypes.add(separator());

        tabAppTypes.add(sectionLabel("Application Type Assignments"));
        tabAppTypes.add(Box.createVerticalStrut(4));
        JLabel assHint = new JLabel("Manually assign a specific application type to a folder (overrides auto-detection)");
        assHint.setFont(assHint.getFont().deriveFont(Font.ITALIC, 10f));
        assHint.setForeground(Color.GRAY);
        assHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabAppTypes.add(assHint);
        tabAppTypes.add(Box.createVerticalStrut(4));

        List<String[]> assignmentList = new ArrayList<>();
        if (config.appTypeAssignments() != null)
            for (Map.Entry<String, String> e : config.appTypeAssignments().entrySet())
                assignmentList.add(new String[]{e.getKey(), e.getValue()});

        DefaultListModel<String[]> assModel = new DefaultListModel<>();
        assignmentList.forEach(assModel::addElement);

        JList<String[]> assList = new JList<>(assModel);
        assList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        assList.setFixedCellHeight(24);
        assList.setCellRenderer((lst, value, index, isSelected, focus) ->
        {
            JLabel lbl = new JLabel(value[0] + "  \u2192  " + value[1]);
            lbl.setFont(lst.getFont().deriveFont(11f));
            lbl.setOpaque(true);
            lbl.setBorder(new EmptyBorder(2, 6, 2, 6));
            lbl.setBackground(isSelected ? lst.getSelectionBackground() : lst.getBackground());
            lbl.setForeground(isSelected ? lst.getSelectionForeground() : lst.getForeground());
            return lbl;
        });

        JButton assBtnAdd    = new JButton("Add");
        JButton assBtnEdit   = new JButton("Edit");
        JButton assBtnRemove = new JButton("Remove");

        Runnable assAdd = () ->
        {
            String[] result = showAddAssignmentEditor(atModel);
            if (result == null) return;
            for (int i = 0; i < assModel.getSize(); i++)
                if (assModel.getElementAt(i)[0].equals(result[0]))
                {
                    JOptionPane.showMessageDialog(this,
                            "An assignment for \"" + result[0] + "\" already exists.",
                            "Duplicate", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            assModel.addElement(result);
        };
        assBtnAdd.addActionListener(ev -> assAdd.run());
        assBtnEdit.addActionListener(ev ->
        {
            int idx = assList.getSelectedIndex();
            if (idx < 0) return;
            String[] edited = showEditAssignmentEditor(assModel.getElementAt(idx), atModel);
            if (edited != null) assModel.set(idx, edited);
        });
        assBtnRemove.addActionListener(ev ->
        {
            int idx = assList.getSelectedIndex();
            if (idx >= 0) assModel.remove(idx);
        });

        JPanel assBtnPanel = new JPanel(new GridLayout(3, 1, 0, 2));
        assBtnPanel.add(assBtnAdd);
        assBtnPanel.add(assBtnEdit);
        assBtnPanel.add(assBtnRemove);

        JPanel assListPanel = new JPanel(new BorderLayout(6, 0));
        assListPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        assListPanel.add(new JScrollPane(assList), BorderLayout.CENTER);
        assListPanel.add(assBtnPanel, BorderLayout.EAST);
        assListPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 3 * 24 + 8));
        tabAppTypes.add(assListPanel);

        tabs.addTab("App Types", new JScrollPane(tabAppTypes));

        main.add(tabs, BorderLayout.CENTER);

        // ── Save / Cancel ─────────────────────────────────────────────────────
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton btnSave   = new JButton("Save");
        JButton btnCancel = new JButton("Cancel");
        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);
        main.add(btnPanel, BorderLayout.SOUTH);

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

            List<AppType> newAppTypes = new ArrayList<>();
            for (int i = 0; i < atModel.getSize(); i++) newAppTypes.add(atModel.getElementAt(i));

            Map<String, String> newAssignments = new LinkedHashMap<>();
            for (int i = 0; i < assModel.getSize(); i++)
            {
                String[] pair = assModel.getElementAt(i);
                newAssignments.put(pair[0], pair[1]);
            }

            String explorerVal    = tfExplorer.getText().trim();
            String editorVal      = tfEditor.getText().trim();
            String newButtonStyle = rbHamburger.isSelected()
                    ? Launcher.BUTTON_STYLE_HAMBURGER : Launcher.BUTTON_STYLE_ICONS;
            String newTheme = rbThemeDark.isSelected()   ? Launcher.THEME_DARK
                            : rbThemeLight.isSelected()  ? Launcher.THEME_LIGHT
                            :                              Launcher.THEME_SYSTEM;
            String newAccent = selectedAccent[0] == null ? null
                    : String.format("#%06X", selectedAccent[0].getRGB() & 0xFFFFFF);

            onSave.accept(new LauncherConfig(
                    config.rootFolder(), cbMinimized.isSelected(),
                    config.windowWidth(), config.windowHeight(),
                    config.priorityList(),
                    explorerVal.isEmpty() ? null : explorerVal,
                    editorVal.isEmpty()   ? null : editorVal,
                    newOrder.isEmpty()    ? null : newOrder,
                    newButtonStyle, cbContextMenu.isSelected(),
                    newToolbarActions.isEmpty()  ? null : newToolbarActions,
                    newCustomActions.isEmpty()   ? null : newCustomActions,
                    newAppTypes.isEmpty()        ? null : newAppTypes,
                    newAssignments.isEmpty()     ? null : newAssignments,
                    newTheme, newAccent));
            dispose();
        });
        btnCancel.addActionListener(e -> dispose());

        add(main);
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

    // ── Custom action editor ─────────────────────────────────────────────────

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

    // ── Application type editor ───────────────────────────────────────────────

    private AppType showAppTypeEditor(AppType existing)
    {
        boolean isNew = (existing == null);

        JTextField tfId   = new JTextField(isNew ? "" : existing.id(), 22);
        JTextField tfIcon = new JTextField(isNew ? "" : nvl(existing.iconPath()), 32);

        String pathsText = (existing != null && existing.executablePaths() != null)
                ? String.join("\n", existing.executablePaths()) : "";
        String namesText = (existing != null && existing.executableNames() != null)
                ? String.join("\n", existing.executableNames()) : "";

        JTextArea taPaths = new JTextArea(pathsText, 3, 32);
        JTextArea taNames = new JTextArea(namesText, 3, 32);
        taPaths.setFont(taPaths.getFont().deriveFont(11f));
        taNames.setFont(taNames.getFont().deriveFont(11f));

        if (!isNew) tfId.setEditable(false);

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

        JPanel iconRow = new JPanel(new BorderLayout(4, 0));
        iconRow.add(tfIcon, BorderLayout.CENTER);
        iconRow.add(browseIcon, BorderLayout.EAST);

        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints lc  = new GridBagConstraints();
        lc.anchor  = GridBagConstraints.NORTHWEST; lc.insets = new Insets(4, 0, 4, 8); lc.gridx = 0;
        GridBagConstraints fc2 = new GridBagConstraints();
        fc2.fill   = GridBagConstraints.HORIZONTAL; fc2.weightx = 1; fc2.insets = new Insets(4, 0, 4, 0); fc2.gridx = 1;

        String[]    lblTexts = {"ID (unique key):", "Icon image path:",
                                "Executable paths\n(one per line, highest priority first):",
                                "Executable names\n(one per line, highest priority first):"};
        Component[] fields   = {tfId, iconRow, new JScrollPane(taPaths), new JScrollPane(taNames)};
        for (int i = 0; i < lblTexts.length; i++)
        {
            lc.gridy = i; fc2.gridy = i;
            JLabel lbl = new JLabel("<html>" + lblTexts[i].replace("\n", "<br>") + "</html>");
            lbl.setFont(lbl.getFont().deriveFont(11f));
            p.add(lbl, lc);
            p.add(fields[i], fc2);
        }
        GridBagConstraints hc = new GridBagConstraints();
        hc.gridx = 0; hc.gridy = lblTexts.length; hc.gridwidth = 2; hc.anchor = GridBagConstraints.WEST;
        hc.insets = new Insets(4, 0, 0, 0);
        JLabel hint = new JLabel("<html><i>Use \"\" (empty string) as a path to search the app folder root.<br>"
                + "Names may include .lnk shortcuts and .exe files.</i></html>");
        hint.setFont(hint.getFont().deriveFont(10f));
        hint.setForeground(Color.GRAY);
        p.add(hint, hc);

        int result = JOptionPane.showConfirmDialog(this, p,
                isNew ? "Add Application Type" : "Edit Application Type",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return null;

        String id = tfId.getText().trim();
        if (id.isEmpty())
        {
            JOptionPane.showMessageDialog(this, "ID cannot be empty.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        List<String> paths = new ArrayList<>();
        for (String line : taPaths.getText().split("\\n", -1))
        {
            String t = line.trim();
            if (!t.isEmpty() || paths.isEmpty()) paths.add(t); // preserve empty root-path entry
        }
        // Clean up: if the single entry is empty and user left field blank, use null
        if (paths.size() == 1 && paths.get(0).isEmpty()) paths.clear();

        List<String> names = new ArrayList<>();
        for (String line : taNames.getText().split("\\n", -1))
        { String t = line.trim(); if (!t.isEmpty()) names.add(t); }

        String iconPath = tfIcon.getText().trim();
        return new AppType(id,
                iconPath.isEmpty() ? null : iconPath,
                paths.isEmpty()    ? null : paths,
                names.isEmpty()    ? null : names);
    }

    // ── Assignment editor ─────────────────────────────────────────────────────

    /**
     * Shows an "Add Assignment" dialog with an editable combo box for the folder name.
     * Returns the new {@code [folder, typeId]} pair, or {@code null} if cancelled.
     */
    private String[] showAddAssignmentEditor(DefaultListModel<AppType> atModel)
    {
        List<String> typeIds = resolveTypeIds(atModel);
        if (typeIds == null) return null;

        List<String> options = new ArrayList<>(knownFolderNames);
        JComboBox<String> cbFolder = new JComboBox<>(options.toArray(new String[0]));
        cbFolder.setEditable(true);
        cbFolder.setSelectedIndex(options.isEmpty() ? -1 : 0);

        JComboBox<String> cbType = new JComboBox<>(typeIds.toArray(new String[0]));

        int result = JOptionPane.showConfirmDialog(this,
                buildAssignmentPanel(cbFolder, cbType), "Add Assignment",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return null;

        Object sel = cbFolder.getSelectedItem();
        String folder = sel != null ? sel.toString().trim() : "";
        if (folder.isEmpty())
        {
            JOptionPane.showMessageDialog(this, "Folder name cannot be empty.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        return new String[]{folder, (String) cbType.getSelectedItem()};
    }

    /**
     * Shows an "Edit Assignment" dialog with a read-only folder name field.
     * Returns the updated {@code [folder, typeId]} pair, or {@code null} if cancelled.
     */
    private String[] showEditAssignmentEditor(String[] existing, DefaultListModel<AppType> atModel)
    {
        List<String> typeIds = resolveTypeIds(atModel);
        if (typeIds == null) return null;

        JTextField tfFolder = new JTextField(existing[0], 28);
        tfFolder.setEditable(false);

        JComboBox<String> cbType = new JComboBox<>(typeIds.toArray(new String[0]));
        if (existing[1] != null) cbType.setSelectedItem(existing[1]);

        int result = JOptionPane.showConfirmDialog(this,
                buildAssignmentPanel(tfFolder, cbType), "Edit Assignment",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return null;

        String folder = tfFolder.getText().trim();
        if (folder.isEmpty())
        {
            JOptionPane.showMessageDialog(this, "Folder name cannot be empty.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        return new String[]{folder, (String) cbType.getSelectedItem()};
    }

    /**
     * Returns the list of type IDs from {@code atModel}, or {@code null} (with a dialog shown)
     * if the model is empty.
     */
    private List<String> resolveTypeIds(DefaultListModel<AppType> atModel)
    {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < atModel.getSize(); i++) ids.add(atModel.getElementAt(i).id());
        if (ids.isEmpty())
        {
            JOptionPane.showMessageDialog(this,
                    "Please define at least one Application Type first.",
                    "No Types Available", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return ids;
    }

    /** Builds the two-row folder/type panel shared by both assignment editors. */
    private static JPanel buildAssignmentPanel(Component folderComponent, JComboBox<String> cbType)
    {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints lc  = new GridBagConstraints();
        lc.anchor  = GridBagConstraints.WEST;
        lc.insets  = new Insets(4, 0, 4, 8);
        lc.gridx   = 0;
        GridBagConstraints fc = new GridBagConstraints();
        fc.fill    = GridBagConstraints.HORIZONTAL;
        fc.weightx = 1;
        fc.insets  = new Insets(4, 0, 4, 0);
        fc.gridx   = 1;

        lc.gridy = 0; fc.gridy = 0;
        p.add(new JLabel("Folder name:"),      lc); p.add(folderComponent, fc);
        lc.gridy = 1; fc.gridy = 1;
        p.add(new JLabel("Application type:"), lc); p.add(cbType, fc);
        return p;
    }

    // ── Static UI helpers ─────────────────────────────────────────────────────

    private JLabel sectionLabel(String text)
    {
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 11f));
        lbl.setForeground(sectionLabelColor());
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    /**
     * Derives an appropriate section-label foreground colour from the current accent colour.
     * <ul>
     *   <li>Dark mode  – blends the accent 40 % toward white so it stays readable on dark panels.</li>
     *   <li>Light mode – blends the accent 35 % toward black so it contrasts on white/light panels.</li>
     * </ul>
     * Falls back to the Windows-blue default ({@code #0078D7}) when no accent is configured.
     */
    private Color sectionLabelColor()
    {
        Color accent = Launcher.parseHexColor(config.accentColor(), new Color(0x00, 0x78, 0xD7));
        if (ColorTheme.isDark())
        {
            // Lighten: blend 40 % toward white
            int r = (int) (accent.getRed()   + (255 - accent.getRed())   * 0.4);
            int g = (int) (accent.getGreen() + (255 - accent.getGreen()) * 0.4);
            int b = (int) (accent.getBlue()  + (255 - accent.getBlue())  * 0.4);
            return new Color(clamp(r), clamp(g), clamp(b));
        }
        else
        {
            // Darken: blend 35 % toward black
            int r = (int) (accent.getRed()   * 0.65);
            int g = (int) (accent.getGreen() * 0.65);
            int b = (int) (accent.getBlue()  * 0.65);
            return new Color(clamp(r), clamp(g), clamp(b));
        }
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

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

