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
    private static final EmptyBorder TAB_PANEL_BORDER = new EmptyBorder(10, 6, 6, 6);
    private static final EmptyBorder CELL_ITEM_BORDER = new EmptyBorder(2, 6, 2, 6);

    private final String launcherId;
    private final LauncherConfig config;
    private final List<String> effectiveActionOrder;
    private final List<String> effectiveToolbarActions;
    private final List<String> knownFolderNames;
    private final Consumer<LauncherConfig> onSave;

    SettingsDialog(JFrame owner, String launcherId, LauncherConfig config,
                   List<String> effectiveActionOrder, List<String> effectiveToolbarActions,
                   List<String> knownFolderNames,
                   Consumer<LauncherConfig> onSave)
    {
        super(owner, "Settings  \u2013  " + launcherId, true);
        this.launcherId = launcherId;
        this.config = config;
        this.effectiveActionOrder = effectiveActionOrder;
        this.effectiveToolbarActions = effectiveToolbarActions;
        this.knownFolderNames = knownFolderNames;
        this.onSave = onSave;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(true);
        buildContent();
        pack();
        // Cap the dialog height so it fits on screen and the tab JScrollPanes actually scroll
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int maxH = (int) (screen.height * 0.78);
        if (getHeight() > maxH)
            setSize(getWidth(), maxH);
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
        tabGeneral.add(infoRow("Launcher ID", launcherId, null));
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
        JRadioButton rbThemeLight = new JRadioButton("Light mode");
        JRadioButton rbThemeDark = new JRadioButton("Dark mode");
        new ButtonGroup()
        {{
            add(rbThemeSystem);
            add(rbThemeLight);
            add(rbThemeDark);
        }};
        if (Launcher.THEME_DARK.equals(config.theme())) rbThemeDark.setSelected(true);
        else if (Launcher.THEME_LIGHT.equals(config.theme())) rbThemeLight.setSelected(true);
        else rbThemeSystem.setSelected(true);
        rbThemeSystem.setAlignmentX(Component.LEFT_ALIGNMENT);
        rbThemeLight.setAlignmentX(Component.LEFT_ALIGNMENT);
        rbThemeDark.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabGeneral.add(rbThemeSystem);
        tabGeneral.add(rbThemeLight);
        tabGeneral.add(rbThemeDark);
        tabGeneral.add(Box.createVerticalStrut(6));

        // ── Accent colour ─────────────────────────────────────────────────────
        final Color defaultAccent = Launcher.DEFAULT_ACCENT;
        final Color[] selectedAccent = {Launcher.parseHexColor(config.accentColor(), null)};

        JLabel accentLbl = new JLabel("Accent color:");
        accentLbl.setFont(accentLbl.getFont().deriveFont(Font.BOLD, 11f));
        accentLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Swatch – shows the current (or default) colour
        JPanel accentSwatch = new JPanel()
        {
            @Override
            protected void paintComponent(Graphics g)
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
            Color chosen = JColorChooser.showDialog(this, "Choose Accent Color", initial);
            if (chosen != null)
            {
                selectedAccent[0] = chosen;
                accentSwatch.repaint();
            }
        });

        JButton accentResetBtn = new JButton("Reset to default");
        accentResetBtn.addActionListener(ev ->
        {
            selectedAccent[0] = null;
            accentSwatch.repaint();
        });

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
        JRadioButton rbIcons = new JRadioButton("Inline icons  \u2013 one small button per action");
        JRadioButton rbHamburger = new JRadioButton("Hamburger menu (\u2630) \u2013 single button opens a popup");
        new ButtonGroup()
        {{
            add(rbIcons);
            add(rbHamburger);
        }};
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

        // ── Tab 5: Hidden Entries ─────────────────────────────────────────────
        JPanel tabHidden = new JPanel();
        tabHidden.setLayout(new BoxLayout(tabHidden, BoxLayout.Y_AXIS));
        tabHidden.setBorder(TAB_PANEL_BORDER);

        tabHidden.add(sectionLabel("Hidden Entries"));
        tabHidden.add(Box.createVerticalStrut(4));
        JLabel hiddenHint = new JLabel("Entries listed here are excluded from the launcher list");
        hiddenHint.setFont(hiddenHint.getFont().deriveFont(Font.ITALIC, 10f));
        hiddenHint.setForeground(Color.GRAY);
        hiddenHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabHidden.add(hiddenHint);
        tabHidden.add(Box.createVerticalStrut(4));

        DefaultListModel<String> hiddenModel = new DefaultListModel<>();
        if (config.hiddenEntries() != null)
            config.hiddenEntries().forEach(hiddenModel::addElement);

        JList<String> hiddenList = new JList<>(hiddenModel);
        hiddenList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        hiddenList.setFixedCellHeight(22);
        hiddenList.setCellRenderer((lst, value, index, isSelected, focus) ->
        {
            JLabel lbl = new JLabel(value);
            lbl.setFont(lst.getFont().deriveFont(11f));
            lbl.setOpaque(true);
            lbl.setBorder(CELL_ITEM_BORDER);
            if (isSelected) { lbl.setBackground(lst.getSelectionBackground()); lbl.setForeground(lst.getSelectionForeground()); }
            else            { lbl.setBackground(lst.getBackground());           lbl.setForeground(lst.getForeground()); }
            return lbl;
        });

        JButton hiddenBtnAdd    = new JButton("Add");
        JButton hiddenBtnRemove = new JButton("Remove");

        hiddenBtnAdd.addActionListener(ev ->
        {
            List<String> allNames = new ArrayList<>(knownFolderNames);
            JComboBox<String> cbName = new JComboBox<>(allNames.toArray(new String[0]));
            cbName.setEditable(true);
            cbName.setSelectedIndex(allNames.isEmpty() ? -1 : 0);
            int res = JOptionPane.showConfirmDialog(this, cbName,
                    "Add Hidden Entry", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res != JOptionPane.OK_OPTION) return;
            Object sel = cbName.getSelectedItem();
            String name = sel != null ? sel.toString().trim() : "";
            if (name.isEmpty()) return;
            for (int i = 0; i < hiddenModel.getSize(); i++)
                if (hiddenModel.getElementAt(i).equals(name)) return; // already present
            hiddenModel.addElement(name);
        });
        hiddenBtnRemove.addActionListener(ev ->
        {
            int idx = hiddenList.getSelectedIndex();
            if (idx >= 0) hiddenModel.remove(idx);
        });

        JPanel hiddenBtnPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        hiddenBtnPanel.add(hiddenBtnAdd);
        hiddenBtnPanel.add(hiddenBtnRemove);

        JPanel hiddenListPanel = new JPanel(new BorderLayout(6, 0));
        hiddenListPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        hiddenListPanel.add(new JScrollPane(hiddenList), BorderLayout.CENTER);
        hiddenListPanel.add(hiddenBtnPanel, BorderLayout.EAST);

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
            String scopeTag = switch (value.scope())
            {
                case CustomAction.SCOPE_ENTRY -> " [entry]";
                case CustomAction.SCOPE_TOOLBAR -> " [toolbar]";
                default -> " [both]";
            };
            JLabel lbl = new JLabel(value.effectiveLabel() + "  (" + value.id() + ")" + scopeTag);
            lbl.setFont(lst.getFont().deriveFont(11f));
            lbl.setOpaque(true);
            lbl.setBorder(new EmptyBorder(2, 6, 2, 6));
            if (isSelected)
            {
                lbl.setBackground(lst.getSelectionBackground());
                lbl.setForeground(lst.getSelectionForeground());
            }
            else
            {
                lbl.setBackground(lst.getBackground());
                lbl.setForeground(lst.getForeground());
            }
            return lbl;
        });

        JButton caBtnAdd = new JButton("Add");
        JButton caBtnEdit = new JButton("Edit");
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
            if (isSelected)
            {
                cb.setBackground(lst.getSelectionBackground());
                cb.setForeground(lst.getSelectionForeground());
            }
            else
            {
                cb.setBackground(lst.getBackground());
                cb.setForeground(lst.getForeground());
            }
            cb.setFont(lst.getFont());
            return cb;
        });
        tbList.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent ev)
            {
                int idx = tbList.locationToIndex(ev.getPoint());
                if (idx < 0) return;
                String key = tbModel.getElementAt(idx);
                if (tbChecked.contains(key)) tbChecked.remove(key);
                else tbChecked.add(key);
                tbList.repaint();
            }
        });

        JButton tbBtnUp = new JButton("\u2191");
        JButton tbBtnDown = new JButton("\u2193");
        tbBtnUp.addActionListener(ev ->
        {
            int i = tbList.getSelectedIndex();
            if (i > 0)
            {
                String item = tbModel.remove(i);
                tbModel.add(i - 1, item);
                tbList.setSelectedIndex(i - 1);
            }
        });
        tbBtnDown.addActionListener(ev ->
        {
            int i = tbList.getSelectedIndex();
            if (i >= 0 && i < tbModel.getSize() - 1)
            {
                String item = tbModel.remove(i);
                tbModel.add(i + 1, item);
                tbList.setSelectedIndex(i + 1);
            }
        });

        JPanel tbActButtons = new JPanel(new GridLayout(2, 1, 0, 2));
        tbActButtons.add(tbBtnUp);
        tbActButtons.add(tbBtnDown);

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
            if (isSelected)
            {
                cb.setBackground(lst.getSelectionBackground());
                cb.setForeground(lst.getSelectionForeground());
            }
            else
            {
                cb.setBackground(lst.getBackground());
                cb.setForeground(lst.getForeground());
            }
            cb.setFont(lst.getFont());
            return cb;
        });
        actList.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent ev)
            {
                int idx = actList.locationToIndex(ev.getPoint());
                if (idx < 0) return;
                String key = actModel.getElementAt(idx);
                if (checked.contains(key)) checked.remove(key);
                else checked.add(key);
                actList.repaint();
            }
        });

        JButton btnUp = new JButton("\u2191");
        JButton btnDown = new JButton("\u2193");
        btnUp.addActionListener(ev ->
        {
            int i = actList.getSelectedIndex();
            if (i > 0)
            {
                String item = actModel.remove(i);
                actModel.add(i - 1, item);
                actList.setSelectedIndex(i - 1);
            }
        });
        btnDown.addActionListener(ev ->
        {
            int i = actList.getSelectedIndex();
            if (i >= 0 && i < actModel.getSize() - 1)
            {
                String item = actModel.remove(i);
                actModel.add(i + 1, item);
                actList.setSelectedIndex(i + 1);
            }
        });

        JPanel actButtons = new JPanel(new GridLayout(2, 1, 0, 2));
        actButtons.add(btnUp);
        actButtons.add(btnDown);

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
            if (isSelected)
            {
                lbl.setBackground(lst.getSelectionBackground());
                lbl.setForeground(lst.getSelectionForeground());
            }
            else
            {
                lbl.setBackground(lst.getBackground());
                lbl.setForeground(lst.getForeground());
            }
            return lbl;
        });

        JButton atBtnAdd = new JButton("Add");
        JButton atBtnEdit = new JButton("Edit");
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

        DefaultListModel<String[]> assModel = new DefaultListModel<>();
        if (config.appTypeAssignments() != null)
        {
            assModel.ensureCapacity(config.appTypeAssignments().size());
            for (Map.Entry<String, String> e : config.appTypeAssignments().entrySet())
                assModel.addElement(new String[]{e.getKey(), e.getValue()});
        }

        JList<String[]> assList = new JList<>(assModel);
        assList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        assList.setFixedCellHeight(24);
        assList.setCellRenderer((lst, value, index, isSelected, focus) ->
        {
            JLabel lbl = new JLabel(value[0] + "  \u2192  " + value[1]);
            lbl.setFont(lst.getFont().deriveFont(11f));
            lbl.setOpaque(true);
            lbl.setBorder(new EmptyBorder(2, 6, 2, 6));
            if (isSelected)
            {
                lbl.setBackground(lst.getSelectionBackground());
                lbl.setForeground(lst.getSelectionForeground());
            }
            else
            {
                lbl.setBackground(lst.getBackground());
                lbl.setForeground(lst.getForeground());
            }
            return lbl;
        });

        JButton assBtnAdd = new JButton("Add");
        JButton assBtnEdit = new JButton("Edit");
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

        tabHidden.add(hiddenListPanel);
        tabs.addTab("Hidden Entries", new JScrollPane(tabHidden));

        // ── Tab 6: Colors ─────────────────────────────────────────────────────
        JPanel tabColors = new JPanel();
        tabColors.setLayout(new BoxLayout(tabColors, BoxLayout.Y_AXIS));
        tabColors.setBorder(TAB_PANEL_BORDER);

        tabColors.add(sectionLabel("Custom Theme Colors"));
        tabColors.add(Box.createVerticalStrut(4));
        JLabel colorsHint = new JLabel(
                "Override individual theme colors. A hatched swatch means \u201cuse theme default\u201d.");
        colorsHint.setFont(colorsHint.getFont().deriveFont(Font.ITALIC, 10f));
        colorsHint.setForeground(Color.GRAY);
        colorsHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabColors.add(colorsHint);
        tabColors.add(Box.createVerticalStrut(6));

        // Ordered list of (configKey, human-readable label) pairs
        String[][] colorDefs = {
            {"rowEven",    "Even row background"},
            {"rowOdd",     "Odd row background"},
            {"fgScript",   "Script text color"},
            {"fgFolder",   "App folder text color"},
            {"fgPlain",    "Plain folder text color"},
            {"selBg",      "Selection background"},
            {"actFg",      "Action button text"},
            {"actDel",     "Delete button text"},
            {"actBg",      "Action button background"},
            {"actBord",    "Action button border"},
            {"selActBg",   "Selected action background"},
            {"selActBord", "Selected action border"},
            {"sepColor",   "Separator / border color"},
            {"searchFg",   "Search label color"}
        };

        // Mutable map of currently chosen colors (null value → use theme default)
        Map<String, Color> customColorSelections = new LinkedHashMap<>();
        if (config.customThemeColors() != null)
        {
            config.customThemeColors().forEach((k, v) ->
            {
                Color c = Launcher.parseHexColor(v, null);
                if (c != null) customColorSelections.put(k, c);
            });
        }

        JPanel colorGrid = new JPanel(new GridBagLayout());
        colorGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

        List<Runnable> colorResetActions = new ArrayList<>();

        for (int ci = 0; ci < colorDefs.length; ci++)
        {
            String colorKey   = colorDefs[ci][0];
            String colorLabel = colorDefs[ci][1];
            Color[] currentColor = { customColorSelections.get(colorKey) };

            GridBagConstraints glc = new GridBagConstraints();
            glc.gridx = 0; glc.gridy = ci;
            glc.anchor = GridBagConstraints.WEST;
            glc.insets = new Insets(2, 0, 2, 8);
            JLabel clbl = new JLabel(colorLabel + ":");
            clbl.setFont(clbl.getFont().deriveFont(11f));
            colorGrid.add(clbl, glc);

            JPanel swatch = new JPanel()
            {
                @Override protected void paintComponent(Graphics g)
                {
                    super.paintComponent(g);
                    if (currentColor[0] != null)
                    {
                        g.setColor(currentColor[0]);
                        g.fillRect(0, 0, getWidth(), getHeight());
                    }
                    else
                    {
                        // Hatched pattern = "using theme default"
                        g.setColor(UIManager.getColor("Panel.background") != null
                                ? UIManager.getColor("Panel.background") : Color.LIGHT_GRAY);
                        g.fillRect(0, 0, getWidth(), getHeight());
                        g.setColor(Color.GRAY);
                        for (int dx = -getHeight(); dx < getWidth(); dx += 4)
                            g.drawLine(dx, 0, dx + getHeight(), getHeight());
                    }
                }
            };
            swatch.setPreferredSize(new Dimension(22, 22));
            swatch.setMinimumSize(new Dimension(22, 22));
            swatch.setMaximumSize(new Dimension(22, 22));
            swatch.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            swatch.setOpaque(false);

            JButton pickBtn  = new JButton("Pick\u2026");
            JButton resetBtn = new JButton("Reset");

            pickBtn.addActionListener(ev ->
            {
                Color initial = currentColor[0] != null ? currentColor[0] : Color.GRAY;
                Color chosen  = JColorChooser.showDialog(this,
                        "Choose color for \u201c" + colorLabel + "\u201d", initial);
                if (chosen != null)
                {
                    currentColor[0] = chosen;
                    customColorSelections.put(colorKey, chosen);
                    swatch.repaint();
                }
            });
            resetBtn.addActionListener(ev ->
            {
                currentColor[0] = null;
                customColorSelections.remove(colorKey);
                swatch.repaint();
            });
            colorResetActions.add(() ->
            {
                currentColor[0] = null;
                customColorSelections.remove(colorKey);
                swatch.repaint();
            });

            GridBagConstraints gsc = new GridBagConstraints();
            gsc.gridx = 1; gsc.gridy = ci; gsc.insets = new Insets(2, 0, 2, 4);
            colorGrid.add(swatch, gsc);

            GridBagConstraints gpc = new GridBagConstraints();
            gpc.gridx = 2; gpc.gridy = ci; gpc.insets = new Insets(2, 0, 2, 2);
            colorGrid.add(pickBtn, gpc);

            GridBagConstraints grc = new GridBagConstraints();
            grc.gridx = 3; grc.gridy = ci; grc.insets = new Insets(2, 0, 2, 0);
            colorGrid.add(resetBtn, grc);
        }

        JButton resetAllColorsBtn = new JButton("Reset All to Theme Defaults");
        resetAllColorsBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        resetAllColorsBtn.addActionListener(ev ->
        {
            customColorSelections.clear();
            colorResetActions.forEach(Runnable::run);
        });

        tabColors.add(resetAllColorsBtn);
        tabColors.add(Box.createVerticalStrut(8));
        tabColors.add(colorGrid);

        // ── LAF color overrides sub-section ──────────────────────────────────
        tabColors.add(separator());
        tabColors.add(sectionLabel("Look-and-Feel Color Overrides"));
        tabColors.add(Box.createVerticalStrut(4));
        JLabel lafHint = new JLabel("<html>Override FlatLaf theme variables (e.g. <b>@background</b>) or UIManager keys.<br>"
                + "These affect <i>all</i> Swing components globally and cascade through the LAF.</html>");
        lafHint.setFont(lafHint.getFont().deriveFont(Font.ITALIC, 10f));
        lafHint.setForeground(Color.GRAY);
        lafHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabColors.add(lafHint);
        tabColors.add(Box.createVerticalStrut(6));

        // Common FlatLaf @variables with color pickers
        String[][] lafDefs = {
            {"@background",          "Window / panel background"},
            {"@foreground",          "Default text color"},
            {"@selectionBackground", "Selection background (lists, text)"},
            {"@selectionForeground", "Selection text color"},
            {"@textBackground",      "Text input field background"},
            {"@buttonBackground",    "Button background"},
            {"@menuBackground",      "Menu / menu-bar background"},
        };

        // Mutable map for the common variables
        Map<String, Color> lafColorSelections = new LinkedHashMap<>();
        // Additional free-form entries (key → raw string value)
        DefaultListModel<String[]> lafExtraModel = new DefaultListModel<>();

        if (config.customLafDefaults() != null)
        {
            Set<String> knownLafKeys = new java.util.HashSet<>();
            for (String[] def : lafDefs) knownLafKeys.add(def[0]);

            config.customLafDefaults().forEach((k, v) ->
            {
                if (knownLafKeys.contains(k))
                {
                    Color c = Launcher.parseHexColor(v, null);
                    if (c != null) lafColorSelections.put(k, c);
                }
                else
                {
                    lafExtraModel.addElement(new String[]{k, v});
                }
            });
        }

        JPanel lafColorGrid = new JPanel(new GridBagLayout());
        lafColorGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        List<Runnable> lafColorResetActions = new ArrayList<>();

        for (int ci = 0; ci < lafDefs.length; ci++)
        {
            String lafKey   = lafDefs[ci][0];
            String lafLabel = lafDefs[ci][1];
            Color[] lafCurrent = { lafColorSelections.get(lafKey) };

            GridBagConstraints glc = new GridBagConstraints();
            glc.gridx = 0; glc.gridy = ci; glc.anchor = GridBagConstraints.WEST;
            glc.insets = new Insets(2, 0, 2, 8);
            JLabel clbl = new JLabel(lafLabel + "  (" + lafKey + "):");
            clbl.setFont(clbl.getFont().deriveFont(11f));
            lafColorGrid.add(clbl, glc);

            JPanel swatch = new JPanel()
            {
                @Override protected void paintComponent(Graphics g)
                {
                    super.paintComponent(g);
                    if (lafCurrent[0] != null) { g.setColor(lafCurrent[0]); g.fillRect(0, 0, getWidth(), getHeight()); }
                    else
                    {
                        g.setColor(UIManager.getColor("Panel.background") != null
                                ? UIManager.getColor("Panel.background") : Color.LIGHT_GRAY);
                        g.fillRect(0, 0, getWidth(), getHeight());
                        g.setColor(Color.GRAY);
                        for (int dx = -getHeight(); dx < getWidth(); dx += 4)
                            g.drawLine(dx, 0, dx + getHeight(), getHeight());
                    }
                }
            };
            swatch.setPreferredSize(new Dimension(22, 22));
            swatch.setMinimumSize(new Dimension(22, 22));
            swatch.setMaximumSize(new Dimension(22, 22));
            swatch.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            swatch.setOpaque(false);

            JButton pickBtn  = new JButton("Pick\u2026");
            JButton resetBtn = new JButton("Reset");
            pickBtn.addActionListener(ev ->
            {
                Color initial = lafCurrent[0] != null ? lafCurrent[0] : Color.GRAY;
                Color chosen  = JColorChooser.showDialog(this, "Choose \u201c" + lafLabel + "\u201d", initial);
                if (chosen != null) { lafCurrent[0] = chosen; lafColorSelections.put(lafKey, chosen); swatch.repaint(); }
            });
            resetBtn.addActionListener(ev -> { lafCurrent[0] = null; lafColorSelections.remove(lafKey); swatch.repaint(); });
            lafColorResetActions.add(() -> { lafCurrent[0] = null; lafColorSelections.remove(lafKey); swatch.repaint(); });

            GridBagConstraints gsc = new GridBagConstraints(); gsc.gridx = 1; gsc.gridy = ci; gsc.insets = new Insets(2, 0, 2, 4);
            lafColorGrid.add(swatch, gsc);
            GridBagConstraints gpc = new GridBagConstraints(); gpc.gridx = 2; gpc.gridy = ci; gpc.insets = new Insets(2, 0, 2, 2);
            lafColorGrid.add(pickBtn, gpc);
            GridBagConstraints grc = new GridBagConstraints(); grc.gridx = 3; grc.gridy = ci; grc.insets = new Insets(2, 0, 2, 0);
            lafColorGrid.add(resetBtn, grc);
        }

        JButton resetAllLafBtn = new JButton("Reset All LAF Overrides");
        resetAllLafBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        resetAllLafBtn.addActionListener(ev ->
        {
            lafColorSelections.clear();
            lafColorResetActions.forEach(Runnable::run);
            lafExtraModel.clear();
        });
        tabColors.add(resetAllLafBtn);
        tabColors.add(Box.createVerticalStrut(4));
        tabColors.add(lafColorGrid);

        // Free-form entries for any other LAF key
        tabColors.add(Box.createVerticalStrut(8));
        JLabel extraLafHint = new JLabel("Additional overrides \u2013 any FlatLaf variable or UIManager key:");
        extraLafHint.setFont(extraLafHint.getFont().deriveFont(10f));
        extraLafHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabColors.add(extraLafHint);
        tabColors.add(Box.createVerticalStrut(4));

        JList<String[]> lafExtraList = new JList<>(lafExtraModel);
        lafExtraList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lafExtraList.setFixedCellHeight(22);
        lafExtraList.setCellRenderer((lst, value, index, isSelected, focus) ->
        {
            JLabel lbl = new JLabel(value[0] + ": " + value[1]);
            lbl.setFont(lst.getFont().deriveFont(11f));
            lbl.setOpaque(true);
            lbl.setBorder(CELL_ITEM_BORDER);
            if (isSelected) { lbl.setBackground(lst.getSelectionBackground()); lbl.setForeground(lst.getSelectionForeground()); }
            else            { lbl.setBackground(lst.getBackground());           lbl.setForeground(lst.getForeground()); }
            return lbl;
        });

        JButton lafAddBtn    = new JButton("Add");
        JButton lafEditBtn   = new JButton("Edit");
        JButton lafRemoveBtn = new JButton("Remove");

        lafAddBtn.addActionListener(ev ->
        {
            String[] entry = showLafEntryEditor(null);
            if (entry != null) lafExtraModel.addElement(entry);
        });
        lafEditBtn.addActionListener(ev ->
        {
            int idx = lafExtraList.getSelectedIndex();
            if (idx < 0) return;
            String[] edited = showLafEntryEditor(lafExtraModel.getElementAt(idx));
            if (edited != null) lafExtraModel.set(idx, edited);
        });
        lafRemoveBtn.addActionListener(ev ->
        {
            int idx = lafExtraList.getSelectedIndex();
            if (idx >= 0) lafExtraModel.remove(idx);
        });

        JPanel lafExtraBtns = new JPanel(new GridLayout(3, 1, 0, 2));
        lafExtraBtns.add(lafAddBtn); lafExtraBtns.add(lafEditBtn); lafExtraBtns.add(lafRemoveBtn);

        JPanel lafExtraPanel = new JPanel(new BorderLayout(6, 0));
        lafExtraPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        lafExtraPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4 * 22 + 8));
        lafExtraPanel.add(new JScrollPane(lafExtraList), BorderLayout.CENTER);
        lafExtraPanel.add(lafExtraBtns, BorderLayout.EAST);
        tabColors.add(lafExtraPanel);

        tabs.addTab("Colors", new JScrollPane(tabColors));

        main.add(tabs, BorderLayout.CENTER);

        // ── Save / Cancel ─────────────────────────────────────────────────────
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton btnSave = new JButton("Save");
        JButton btnCancel = new JButton("Cancel");
        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);
        main.add(btnPanel, BorderLayout.SOUTH);

        btnSave.addActionListener(e ->
        {
            List<String> newOrder = new ArrayList<>(actModel.getSize());
            for (int i = 0; i < actModel.getSize(); i++)
            {
                String k = actModel.getElementAt(i);
                if (checked.contains(k)) newOrder.add(k);
            }
            List<String> newToolbarActions = new ArrayList<>(tbModel.getSize());
            for (int i = 0; i < tbModel.getSize(); i++)
            {
                String k = tbModel.getElementAt(i);
                if (tbChecked.contains(k)) newToolbarActions.add(k);
            }
            List<CustomAction> newCustomActions = new ArrayList<>(caModel.getSize());
            for (int i = 0; i < caModel.getSize(); i++) newCustomActions.add(caModel.getElementAt(i));

            List<AppType> newAppTypes = new ArrayList<>(atModel.getSize());
            for (int i = 0; i < atModel.getSize(); i++) newAppTypes.add(atModel.getElementAt(i));

            Map<String, String> newAssignments = LinkedHashMap.newLinkedHashMap(assModel.getSize());
            for (int i = 0; i < assModel.getSize(); i++)
            {
                String[] pair = assModel.getElementAt(i);
                newAssignments.put(pair[0], pair[1]);
            }

            String explorerVal = tfExplorer.getText().trim();
            String editorVal = tfEditor.getText().trim();
            String newButtonStyle = rbHamburger.isSelected()
                    ? Launcher.BUTTON_STYLE_HAMBURGER : Launcher.BUTTON_STYLE_ICONS;
            String newTheme = rbThemeDark.isSelected() ? Launcher.THEME_DARK
                    : rbThemeLight.isSelected() ? Launcher.THEME_LIGHT
                      : Launcher.THEME_SYSTEM;
            String newAccent = selectedAccent[0] == null ? null
                    : String.format("#%06X", selectedAccent[0].getRGB() & 0xFFFFFF);

            List<String> newHiddenEntries = new ArrayList<>(hiddenModel.getSize());
            for (int i = 0; i < hiddenModel.getSize(); i++) newHiddenEntries.add(hiddenModel.getElementAt(i));

            // Collect custom theme color overrides as hex strings
            Map<String, String> newCustomThemeColors = null;
            if (!customColorSelections.isEmpty())
            {
                newCustomThemeColors = new LinkedHashMap<>();
                for (Map.Entry<String, Color> ce : customColorSelections.entrySet())
                    newCustomThemeColors.put(ce.getKey(),
                            String.format("#%06X", ce.getValue().getRGB() & 0xFFFFFF));
            }

            onSave.accept(new LauncherConfig(
                    config.rootFolder(), cbMinimized.isSelected(),
                    config.windowWidth(), config.windowHeight(),
                    config.priorityList(),
                    explorerVal.isEmpty() ? null : explorerVal,
                    editorVal.isEmpty() ? null : editorVal,
                    newOrder.isEmpty() ? null : newOrder,
                    newButtonStyle, cbContextMenu.isSelected(),
                    newToolbarActions.isEmpty() ? null : newToolbarActions,
                    newCustomActions.isEmpty() ? null : newCustomActions,
                    newAppTypes.isEmpty() ? null : newAppTypes,
                    newAssignments.isEmpty() ? null : newAssignments,
                    newTheme, newAccent,
                    newHiddenEntries.isEmpty() ? null : newHiddenEntries,
                    newCustomThemeColors,
                    buildNewLafDefaults(lafColorSelections, lafExtraModel)));
            dispose();
        });
        btnCancel.addActionListener(e -> dispose());

        add(main);
    }

    // ── Static UI helpers ─────────────────────────────────────────────────────

    /** Merges common LAF color selections and free-form extras into one map. */
    private static Map<String, String> buildNewLafDefaults(
            Map<String, Color> colorSels, DefaultListModel<String[]> extraModel)
    {
        Map<String, String> result = new LinkedHashMap<>();
        colorSels.forEach((k, v) -> result.put(k, String.format("#%06X", v.getRGB() & 0xFFFFFF)));
        for (int i = 0; i < extraModel.getSize(); i++)
        {
            String[] pair = extraModel.getElementAt(i);
            result.put(pair[0], pair[1]);
        }
        return result.isEmpty() ? null : result;
    }

    /** Shows a dialog to add or edit a free-form LAF key→value entry. */
    private String[] showLafEntryEditor(String[] existing)
    {
        JTextField tfKey   = new JTextField(existing != null ? existing[0] : "@", 28);
        JTextField tfValue = new JTextField(existing != null ? existing[1] : "", 18);

        JButton pickColor = new JButton("Pick color\u2026");
        pickColor.addActionListener(ev ->
        {
            Color initial = Launcher.parseHexColor(tfValue.getText().trim(), Color.GRAY);
            Color chosen  = JColorChooser.showDialog(this, "Choose Color", initial);
            if (chosen != null) tfValue.setText(String.format("#%06X", chosen.getRGB() & 0xFFFFFF));
        });

        JPanel valueRow = new JPanel(new BorderLayout(4, 0));
        valueRow.add(tfValue, BorderLayout.CENTER);
        valueRow.add(pickColor, BorderLayout.EAST);

        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints lc = new GridBagConstraints();
        lc.anchor = GridBagConstraints.WEST; lc.insets = new Insets(4, 0, 4, 8); lc.gridx = 0;
        GridBagConstraints fc = new GridBagConstraints();
        fc.fill = GridBagConstraints.HORIZONTAL; fc.weightx = 1; fc.insets = new Insets(4, 0, 4, 0); fc.gridx = 1;

        lc.gridy = 0; fc.gridy = 0;
        p.add(new JLabel("Key (e.g. @background, Panel.background):"), lc); p.add(tfKey, fc);
        lc.gridy = 1; fc.gridy = 1;
        p.add(new JLabel("Value (e.g. #1E1E2E or any FlatLaf value):"), lc); p.add(valueRow, fc);

        int res = JOptionPane.showConfirmDialog(this, p,
                existing == null ? "Add LAF Override" : "Edit LAF Override",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return null;

        String k = tfKey.getText().trim();
        String v = tfValue.getText().trim();
        if (k.isEmpty() || v.isEmpty())
        {
            JOptionPane.showMessageDialog(this, "Key and value must not be empty.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        return new String[]{k, v};
    }

    private String toolbarLabel(String key)
    {
        return switch (key)
        {
            case Launcher.SVN_CHECKOUT_ACTION -> "SVN Checkout";
            case Launcher.SVN_BROWSER_ACTION -> "SVN Repository Browser";
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
            case Launcher.EDITOR_ACTION -> "Open in Editor";
            case Launcher.COPY_ACTION -> "Copy with Robocopy";
            case Launcher.DELETE_ACTION -> "Delete";
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

        JTextField tfId = new JTextField(isNew ? "" : existing.id(), 24);
        JTextField tfLabel = new JTextField(isNew ? "" : nvl(existing.label()), 24);
        JTextField tfScript = new JTextField(isNew ? "" : nvl(existing.scriptPath()), 32);
        JTextField tfIcon = new JTextField(isNew ? "" : nvl(existing.iconPath()), 32);
        JTextField tfTip = new JTextField(isNew ? "" : nvl(existing.tooltip()), 32);

        if (!isNew) tfId.setEditable(false); // ID must stay stable once created

        // Scope radio buttons
        JRadioButton rbEntry = new JRadioButton("Entry action bar only");
        JRadioButton rbToolbar = new JRadioButton("Toolbar only");
        JRadioButton rbBoth = new JRadioButton("Both (entry bar and toolbar)");
        ButtonGroup bgScope = new ButtonGroup();
        bgScope.add(rbEntry);
        bgScope.add(rbToolbar);
        bgScope.add(rbBoth);

        String currentScope = isNew ? null : existing.scope();
        if (CustomAction.SCOPE_ENTRY.equals(currentScope)) rbEntry.setSelected(true);
        else if (CustomAction.SCOPE_TOOLBAR.equals(currentScope)) rbToolbar.setSelected(true);
        else rbBoth.setSelected(true);

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
        lc.anchor = GridBagConstraints.NORTHWEST;
        lc.insets = new Insets(3, 0, 3, 8);
        lc.gridx = 0;

        GridBagConstraints fc2 = new GridBagConstraints();
        fc2.fill = GridBagConstraints.HORIZONTAL;
        fc2.weightx = 1;
        fc2.insets = new Insets(3, 0, 3, 0);
        fc2.gridx = 1;

        String[] lbls = {"ID (unique key):", "Scope *:", "Label:", "Script / Executable:", "Icon image path:", "Tooltip:"};
        Component[] flds = {tfId, scopePanel, tfLabel, scriptRow, iconRow, tfTip};
        for (int i = 0; i < lbls.length; i++)
        {
            lc.gridy = i;
            fc2.gridy = i;
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

        String scope = rbEntry.isSelected() ? CustomAction.SCOPE_ENTRY
                : rbToolbar.isSelected() ? CustomAction.SCOPE_TOOLBAR
                  : CustomAction.SCOPE_BOTH;
        String scriptPath = tfScript.getText().trim();
        String iconPath = tfIcon.getText().trim();
        String label = tfLabel.getText().trim();
        String tooltip = tfTip.getText().trim();

        return new CustomAction(id, scope,
                iconPath.isEmpty() ? null : iconPath,
                scriptPath.isEmpty() ? null : scriptPath,
                label.isEmpty() ? null : label,
                tooltip.isEmpty() ? null : tooltip);
    }

    private static String nvl(String s)
    {
        return s != null ? s : "";
    }

    // ── Application type editor ───────────────────────────────────────────────

    private AppType showAppTypeEditor(AppType existing)
    {
        boolean isNew = (existing == null);

        JTextField tfId = new JTextField(isNew ? "" : existing.id(), 22);
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
        GridBagConstraints lc = new GridBagConstraints();
        lc.anchor = GridBagConstraints.NORTHWEST;
        lc.insets = new Insets(4, 0, 4, 8);
        lc.gridx = 0;
        GridBagConstraints fc2 = new GridBagConstraints();
        fc2.fill = GridBagConstraints.HORIZONTAL;
        fc2.weightx = 1;
        fc2.insets = new Insets(4, 0, 4, 0);
        fc2.gridx = 1;

        String[] lblTexts = {"ID (unique key):", "Icon image path:",
                "Executable paths\n(one per line, highest priority first):",
                "Executable names\n(one per line, highest priority first):"};
        Component[] fields = {tfId, iconRow, new JScrollPane(taPaths), new JScrollPane(taNames)};
        for (int i = 0; i < lblTexts.length; i++)
        {
            lc.gridy = i;
            fc2.gridy = i;
            JLabel lbl = new JLabel("<html>" + lblTexts[i].replace("\n", "<br>") + "</html>");
            lbl.setFont(lbl.getFont().deriveFont(11f));
            p.add(lbl, lc);
            p.add(fields[i], fc2);
        }
        GridBagConstraints hc = new GridBagConstraints();
        hc.gridx = 0;
        hc.gridy = lblTexts.length;
        hc.gridwidth = 2;
        hc.anchor = GridBagConstraints.WEST;
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

        String[] pathLines = taPaths.getText().split("\\n", -1);
        List<String> paths = new ArrayList<>(pathLines.length);
        for (String line : pathLines)
        {
            String t = line.trim();
            if (!t.isEmpty() || paths.isEmpty()) paths.add(t); // preserve empty root-path entry
        }
        // Clean up: if the single entry is empty and user left field blank, use null
        if (paths.size() == 1 && paths.getFirst().isEmpty()) paths.clear();

        String[] nameLines = taNames.getText().split("\\n", -1);
        List<String> names = new ArrayList<>(nameLines.length);
        for (String line : nameLines)
        {
            String t = line.trim();
            if (!t.isEmpty()) names.add(t);
        }

        String iconPath = tfIcon.getText().trim();
        return new AppType(id,
                iconPath.isEmpty() ? null : iconPath,
                paths.isEmpty() ? null : paths,
                names.isEmpty() ? null : names);
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
        List<String> ids = new ArrayList<>(atModel.getSize());
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

    /**
     * Builds the two-row folder/type panel shared by both assignment editors.
     */
    private static JPanel buildAssignmentPanel(Component folderComponent, JComboBox<String> cbType)
    {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints lc = new GridBagConstraints();
        lc.anchor = GridBagConstraints.WEST;
        lc.insets = new Insets(4, 0, 4, 8);
        lc.gridx = 0;
        GridBagConstraints fc = new GridBagConstraints();
        fc.fill = GridBagConstraints.HORIZONTAL;
        fc.weightx = 1;
        fc.insets = new Insets(4, 0, 4, 0);
        fc.gridx = 1;

        lc.gridy = 0;
        fc.gridy = 0;
        p.add(new JLabel("Folder name:"), lc);
        p.add(folderComponent, fc);
        lc.gridy = 1;
        fc.gridy = 1;
        p.add(new JLabel("Application type:"), lc);
        p.add(cbType, fc);
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
private Color sectionLabelColor() {
    Color accent = Launcher.parseHexColor(config.accentColor(), Launcher.DEFAULT_ACCENT);
    int base = accent.getRGB();

    // Pack R, G, B into a long: [0][R:16][G:16][B:16]
    // Each channel gets 16 bits so overflow from multiply can't bleed into neighbours
    long rgb = ((long) (base >> 16 & 0xFF) << 32)
            | ((long) (base >>  8 & 0xFF) << 16)
            |         (base       & 0xFF);

    long result;
    if (ColorTheme.isDark()) {
        // Lighten: blend 40% toward white  →  ch + (255 - ch) * 0.4
        //        = ch * 0.6 + 255 * 0.4
        //        = ch * 6 / 10 + 102
        // Multiply every packed channel by 6 (no inter-channel overflow in 16-bit lanes)
        long scaled = rgb * 6;                              // ch * 6, still in 16-bit lanes
        // Add 102 (= floor(255 * 0.4)) to every lane simultaneously
        long bias   = (102L << 32) | (102L << 16) | 102L;
        long sum    = scaled + bias;                        // ch*6 + 102, no carry bleed
        // Divide each lane by 10 – extract, divide, repack
        result = divLanes10(sum);
    } else {
        // Darken: blend 35% toward black  →  ch * 0.65 = ch * 13 / 20
        long scaled = rgb * 13;
        result = divLanes20(scaled);
    }

    // Unpack and clamp (clamp guards against the integer-division rounding)
    int r = clamp((int) (result >> 32 & 0xFF));
    int g = clamp((int) (result >> 16 & 0xFF));
    int b = clamp((int) (result       & 0xFF));
    return new Color(r, g, b);
}

// ── helpers ──────────────────────────────────────────────────────────────────

    /** Divide each 16-bit lane in a packed [R|G|B] long by 10. */
    private static long divLanes10(long v) {
        int r = (int) (v >> 32 & 0xFFFF) / 10;
        int g = (int) (v >> 16 & 0xFFFF) / 10;
        int b = (int) (v       & 0xFFFF) / 10;
        return ((long) r << 32) | ((long) g << 16) | b;
    }

    /** Divide each 16-bit lane in a packed [R|G|B] long by 20. */
    private static long divLanes20(long v) {
        int r = (int) (v >> 32 & 0xFFFF) / 20;
        int g = (int) (v >> 16 & 0xFFFF) / 20;
        int b = (int) (v       & 0xFFFF) / 20;
        return ((long) r << 32) | ((long) g << 16) | b;
    }

    private static int clamp(int v)
    {
        return Math.clamp(v, 0, 255);
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
                try
                {
                    Desktop.getDesktop().open(dir);
                } catch (Exception ignored)
                {
                }
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

