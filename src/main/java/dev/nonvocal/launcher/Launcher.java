package dev.nonvocal.launcher;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Launcher – self-contained Java Swing application.
 * <p>
 * The root folder is scanned at the FIRST level only:
 * • Files with a recognised script extension are listed as scripts.
 * • Sub-directories are listed as application folders or plain folders.
 * <p>
 * Usage:
 * <pre>
 *   java -jar launcher.jar [rootFolder] [--minimized] [--launcherId=id] [--config=path]
 * </pre>
 */
public class Launcher extends JFrame
{
    private static final long serialVersionUID = 1L;

    /** Relative path of the fallback executable inside an application folder. */
    private static final String FALLBACK_EXE = "basis\\sys\\win\\bin\\dsc_StartPlm.exe";

    // ── Action keys ──────────────────────────────────────────────────────────

    public static final String EXPLORE_ACTION = "EXPLORE_ACTION";
    public static final String EDITOR_ACTION  = "EDITOR_ACTION";
    public static final String COPY_ACTION    = "COPY_ACTION";
    public static final String DELETE_ACTION  = "DELETE_ACTION";

    static final List<String> DEFAULT_ACTION_ORDER =
            List.of(EXPLORE_ACTION, EDITOR_ACTION, COPY_ACTION, DELETE_ACTION);

    // ── Button style keys ────────────────────────────────────────────────────

    public static final String BUTTON_STYLE_ICONS     = "ICONS";
    public static final String BUTTON_STYLE_HAMBURGER = "HAMBURGER";

    // ── Instance state ───────────────────────────────────────────────────────

    private final File baseFolder;
    private LauncherConfig config;
    private final String launcherId;
    private DefaultListModel<LaunchEntry> listModel;
    private JList<LaunchEntry> list;
    private JLabel hintLabel;
    private JLabel searchLabel;
    private transient String searchQuery = "";
    private transient final List<LaunchEntry> allEntries = new ArrayList<>();
    private List<String> effectiveActionOrder;
    private String effectiveButtonStyle;
    private boolean showContextMenu;
    private EntryCellRenderer cellRenderer;
    /** Registered in buildUI (save + exit). Removed by setupTray() so only the tray adapter fires. */
    private WindowAdapter defaultCloseAdapter;

    // ── Constructor ──────────────────────────────────────────────────────────

    Launcher(File baseFolder, LauncherConfig config, String launcherId)
    {
        this.baseFolder  = baseFolder;
        this.config      = config;
        this.launcherId  = launcherId;
        buildUI();
    }

    // ── Config resolution helpers ────────────────────────────────────────────

    private List<String> resolveActionOrder(LauncherConfig cfg)
    {
        List<String> order = cfg.actionOrder();
        if (order == null) return new ArrayList<>(DEFAULT_ACTION_ORDER);
        return order.stream().filter(DEFAULT_ACTION_ORDER::contains).collect(Collectors.toList());
    }

    private static String resolveButtonStyle(LauncherConfig cfg)
    {
        return BUTTON_STYLE_HAMBURGER.equals(cfg.entryButtonStyle())
                ? BUTTON_STYLE_HAMBURGER : BUTTON_STYLE_ICONS;
    }

    private static boolean resolveShowContextMenu(LauncherConfig cfg)
    {
        return !Boolean.FALSE.equals(cfg.showContextMenu());
    }

    /** Human-readable label for an action key (used in the settings dialog). */
    private static String actionLabel(String key)
    {
        return switch (key)
        {
            case EXPLORE_ACTION -> "Open in File Explorer";
            case EDITOR_ACTION  -> "Open in Editor";
            case COPY_ACTION    -> "Copy with Robocopy";
            case DELETE_ACTION  -> "Delete";
            default             -> key;
        };
    }

    // ── List management ──────────────────────────────────────────────────────

    private void refreshList()
    {
        allEntries.clear();
        allEntries.addAll(loadEntries());
        applyFilter();
    }

    private void applyFilter()
    {
        listModel.clear();
        String q = searchQuery.toLowerCase(Locale.ROOT);
        for (LaunchEntry entry : allEntries)
        {
            if (q.isEmpty() || entry.file().getName().toLowerCase(Locale.ROOT).contains(q))
                listModel.addElement(entry);
        }
        if (searchQuery.isEmpty())
        {
            searchLabel.setText("");
            hintLabel.setText(listModel.size() + " entries   |   Double-click or Enter to launch");
        }
        else
        {
            searchLabel.setText("  Filter: " + searchQuery + "\u258C");
            hintLabel.setText(listModel.size() + " of " + allEntries.size()
                    + "   |   Esc to clear  \u00b7  DnD disabled while filtering");
        }
        if (list != null) list.setDragEnabled(searchQuery.isEmpty());
    }

    private void clearSearch()
    {
        searchQuery = "";
        applyFilter();
    }

    // ── UI construction ──────────────────────────────────────────────────────

    private void buildUI()
    {
        effectiveActionOrder = resolveActionOrder(config);
        effectiveButtonStyle = resolveButtonStyle(config);
        showContextMenu      = resolveShowContextMenu(config);

        setTitle("Launcher  \u2013  " + baseFolder.getAbsolutePath());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(config.windowWidth(), config.windowHeight());
        setMinimumSize(new Dimension(320, 200));
        setLocationRelativeTo(null);

        defaultCloseAdapter = new WindowAdapter()
        {
            @Override public void windowClosing(WindowEvent e) { saveConfig(); System.exit(0); }
        };
        addWindowListener(defaultCloseAdapter);

        ImageIcon appIcon = loadScaledIcon("apps.png", 32, 32);
        if (appIcon != null) setIconImage(appIcon.getImage());

        // ── Header ───────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0x00, 0x78, 0xD7));
        header.setBorder(new EmptyBorder(10, 14, 10, 14));
        JLabel dirLabel = new JLabel(baseFolder.getName() + "   \u2014   " + baseFolder.getAbsolutePath());
        dirLabel.setFont(dirLabel.getFont().deriveFont(Font.BOLD, 13f));
        dirLabel.setForeground(Color.WHITE);
        header.add(dirLabel, BorderLayout.CENTER);

        // ── Entry list ───────────────────────────────────────────────────────
        listModel = new DefaultListModel<>();
        allEntries.addAll(loadEntries());
        allEntries.forEach(listModel::addElement);

        JList<LaunchEntry> list = this.list = new JList<>(listModel)
        {
            @Override
            public String getToolTipText(MouseEvent e)
            {
                int idx = locationToIndex(e.getPoint());
                if (idx >= 0 && listModel.getElementAt(idx).type() != EntryType.SCRIPT)
                {
                    if (BUTTON_STYLE_HAMBURGER.equals(effectiveButtonStyle))
                    {
                        int n = effectiveActionOrder.isEmpty() ? 0 : 1;
                        if (hitActionIcon(e.getPoint(), this, idx, n) == 0) return "Actions";
                    }
                    else
                    {
                        int ai = hitActionIcon(e.getPoint(), this, idx, effectiveActionOrder.size());
                        if (ai >= 0)
                        {
                            String key = effectiveActionOrder.get(ai);
                            return EntryCellRenderer.ACT_TIP_MAP.getOrDefault(key, key);
                        }
                    }
                }
                return null;
            }
        };
        ToolTipManager.sharedInstance().registerComponent(list);
        cellRenderer = new EntryCellRenderer(effectiveActionOrder, effectiveButtonStyle);
        list.setCellRenderer(cellRenderer);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(36);

        list.addMouseListener(buildMouseHandler(list));
        list.addMouseMotionListener(buildMouseHandler(list));

        setupDragAndDrop(list);

        list.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "launch");
        list.getActionMap().put("launch", new AbstractAction()
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                LaunchEntry sel = list.getSelectedValue();
                if (sel != null) launch(sel);
            }
        });

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(0xCC, 0xCC, 0xCC)));

        // ── Footer ───────────────────────────────────────────────────────────
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        legend.setBackground(new Color(0xF0, 0xF0, 0xF0));
        legend.setBorder(new MatteBorder(1, 0, 0, 0, new Color(0xCC, 0xCC, 0xCC)));
        legend.add(coloredLabel("Scripts",              new Color(0x1A, 0x5F, 0x7A)));
        legend.add(coloredLabel("Application folders",  new Color(0x2E, 0x6B, 0x2E)));
        legend.add(coloredLabel("Folders",              new Color(0x66, 0x55, 0x44)));

        searchLabel = new JLabel();
        searchLabel.setForeground(new Color(0x00, 0x50, 0xA0));
        searchLabel.setFont(searchLabel.getFont().deriveFont(Font.BOLD | Font.ITALIC, 11f));
        searchLabel.setHorizontalAlignment(JLabel.CENTER);

        hintLabel = new JLabel(listModel.size() + " entries   |   Double-click or Enter to launch");
        hintLabel.setForeground(Color.GRAY);
        hintLabel.setFont(hintLabel.getFont().deriveFont(11f));

        JPanel south = new JPanel(new BorderLayout());
        south.add(legend, BorderLayout.WEST);
        south.add(searchLabel, BorderLayout.CENTER);
        south.add(hintLabel, BorderLayout.EAST);
        south.setBorder(new EmptyBorder(0, 10, 0, 10));
        south.setBackground(new Color(0xF0, 0xF0, 0xF0));

        // ── Toolbar ──────────────────────────────────────────────────────────
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBackground(new Color(0xF0, 0xF2, 0xF5));
        toolbar.setBorder(new MatteBorder(0, 0, 1, 0, new Color(0xCC, 0xCC, 0xCC)));

        ImageIcon svnIcon = loadScaledIcon("apps-add.png", 20, 20);
        JButton svnButton = svnIcon != null ? new JButton(svnIcon) : new JButton("SVN");
        svnButton.setToolTipText("SVN Checkout \u2013 check out a repository into the selected folder");
        svnButton.setFocusPainted(false);
        svnButton.addActionListener(ev ->
        {
            LaunchEntry sel = list.getSelectedValue();
            svnCheckout((sel != null && sel.type() != EntryType.SCRIPT) ? sel.file() : baseFolder);
        });
        toolbar.add(svnButton);
        toolbar.add(Box.createHorizontalGlue());

        ImageIcon settingsIcon = loadScaledIcon("setting.png", 20, 20);
        JButton settingsButton = settingsIcon != null ? new JButton(settingsIcon) : new JButton("\u2699");
        settingsButton.setToolTipText("Settings");
        settingsButton.setFocusPainted(false);
        settingsButton.addActionListener(ev -> showSettings());
        toolbar.add(settingsButton);

        JPanel topArea = new JPanel(new BorderLayout());
        topArea.add(header,  BorderLayout.NORTH);
        topArea.add(toolbar, BorderLayout.SOUTH);

        add(topArea, BorderLayout.NORTH);
        add(scroll,  BorderLayout.CENTER);
        add(south,   BorderLayout.SOUTH);

        // ── Type-to-search ───────────────────────────────────────────────────
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(event ->
        {
            if (event.getID() != KeyEvent.KEY_TYPED) return false;
            if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow() != Launcher.this)
                return false;
            char c = event.getKeyChar();
            if (c == KeyEvent.CHAR_UNDEFINED) return false;
            if      (c == '\u001b') { clearSearch(); }
            else if (c == '\b')     { if (!searchQuery.isEmpty()) { searchQuery = searchQuery.substring(0, searchQuery.length() - 1); applyFilter(); } }
            else if (c == '\r' || c == '\n') { return false; }
            else if (c >= 32)       { searchQuery += c; applyFilter(); }
            return false;
        });
    }

    /** Builds a mouse adapter that handles click actions and the right-click context menu. */
    private MouseAdapter buildMouseHandler(JList<LaunchEntry> list)
    {
        return new MouseAdapter()
        {
            private void handlePopup(MouseEvent e)
            {
                if (!e.isPopupTrigger() || !showContextMenu) return;
                int idx = list.locationToIndex(e.getPoint());
                if (idx < 0) return;
                list.setSelectedIndex(idx);
                LaunchEntry sel = listModel.getElementAt(idx);
                if (sel.type() != EntryType.SCRIPT) showActionsPopup(sel, list, e.getX(), e.getY());
            }

            @Override
            public void mouseClicked(MouseEvent e)
            {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                int idx = list.locationToIndex(e.getPoint());
                if (idx < 0) return;
                LaunchEntry sel = listModel.getElementAt(idx);

                if (sel.type() != EntryType.SCRIPT)
                {
                    if (BUTTON_STYLE_HAMBURGER.equals(effectiveButtonStyle))
                    {
                        int n = effectiveActionOrder.isEmpty() ? 0 : 1;
                        if (hitActionIcon(e.getPoint(), list, idx, n) == 0 && e.getClickCount() == 1)
                        {
                            showActionsPopup(sel, list, e.getX(), e.getY());
                            return;
                        }
                    }
                    else
                    {
                        int ai = hitActionIcon(e.getPoint(), list, idx, effectiveActionOrder.size());
                        if (ai >= 0)
                        {
                            if (e.getClickCount() == 1)
                            {
                                switch (effectiveActionOrder.get(ai))
                                {
                                    case EXPLORE_ACTION -> openInExplorer(sel.file());
                                    case EDITOR_ACTION  -> openInEditor(sel.file());
                                    case COPY_ACTION    -> copyWithRobocopy(sel.file());
                                    case DELETE_ACTION  -> deleteFolder(sel.file());
                                }
                            }
                            return;
                        }
                    }
                }
                if (e.getClickCount() == 2) launch(sel);
            }

            @Override
            public void mouseMoved(MouseEvent e)
            {
                int idx = list.locationToIndex(e.getPoint());
                boolean over = false;
                if (idx >= 0 && listModel.getElementAt(idx).type() != EntryType.SCRIPT)
                {
                    int n = BUTTON_STYLE_HAMBURGER.equals(effectiveButtonStyle)
                            ? (effectiveActionOrder.isEmpty() ? 0 : 1)
                            : effectiveActionOrder.size();
                    over = hitActionIcon(e.getPoint(), list, idx, n) >= 0;
                }
                list.setCursor(over ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            }

            @Override public void mousePressed(MouseEvent e)  { handlePopup(e); }
            @Override public void mouseReleased(MouseEvent e) { handlePopup(e); }
        };
    }

    /** Sets up drag-and-drop reordering on the list. */
    private void setupDragAndDrop(JList<LaunchEntry> list)
    {
        list.setDragEnabled(true);
        list.setDropMode(DropMode.INSERT);
        list.setTransferHandler(new TransferHandler()
        {
            private final DataFlavor entryFlavor = new DataFlavor(Integer.class, "Row Index");
            private int dragIndex = -1;

            @Override public int getSourceActions(JComponent c) { return MOVE; }

            @Override
            protected Transferable createTransferable(JComponent c)
            {
                dragIndex = list.getSelectedIndex();
                final int idx = dragIndex;
                return new Transferable()
                {
                    @Override public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[]{entryFlavor}; }
                    @Override public boolean isDataFlavorSupported(DataFlavor f) { return entryFlavor.equals(f); }
                    @Override public Object getTransferData(DataFlavor f)  { return idx; }
                };
            }

            @Override
            public boolean canImport(TransferSupport s)
            {
                return s.isDrop() && searchQuery.isEmpty() && s.isDataFlavorSupported(entryFlavor);
            }

            @Override
            public boolean importData(TransferSupport support)
            {
                if (!canImport(support)) return false;
                try
                {
                    int dropIndex = ((JList.DropLocation) support.getDropLocation()).getIndex();
                    int src = dragIndex;
                    if (src < 0 || src >= listModel.getSize()) return false;
                    LaunchEntry moved = listModel.getElementAt(src);
                    listModel.remove(src);
                    int target = (src < dropIndex) ? dropIndex - 1 : dropIndex;
                    target = Math.max(0, Math.min(target, listModel.getSize()));
                    listModel.add(target, moved);
                    allEntries.clear();
                    for (int i = 0; i < listModel.getSize(); i++) allEntries.add(listModel.getElementAt(i));
                    savePriorityList();
                    list.setSelectedIndex(target);
                    return true;
                }
                catch (Exception ex) { return false; }
            }

            @Override protected void exportDone(JComponent s, Transferable d, int a) { dragIndex = -1; }
        });
    }

    // ── System tray ──────────────────────────────────────────────────────────

    /**
     * Installs a system-tray icon and starts the window hidden.
     * Falls back to a normal visible window if the system tray is not supported.
     */
    void setupTray()
    {
        if (!SystemTray.isSupported()) { setVisible(true); return; }

        Image trayImage;
        ImageIcon res = loadScaledIcon("apps.png", 16, 16);
        if (res != null)
        {
            trayImage = res.getImage();
        }
        else
        {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0x00, 0x78, 0xD7));
            g.fillRoundRect(0, 0, 15, 15, 4, 4);
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
            g.drawString("L", 4, 12);
            g.dispose();
            trayImage = img;
        }

        PopupMenu popup = new PopupMenu();
        MenuItem showItem = new MenuItem("Show / Hide");
        showItem.addActionListener(e -> toggleVisibility());
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        popup.add(showItem);
        popup.addSeparator();
        popup.add(exitItem);

        TrayIcon trayIcon = new TrayIcon(trayImage, "Launcher \u2013 " + baseFolder.getName(), popup);
        trayIcon.setImageAutoSize(true);
        trayIcon.addMouseListener(new MouseAdapter()
        {
            @Override public void mouseClicked(MouseEvent e)
            {
                if (e.getButton() == MouseEvent.BUTTON1) toggleVisibility();
            }
        });

        try { SystemTray.getSystemTray().add(trayIcon); }
        catch (AWTException ex) { setVisible(true); return; }

        // Replace the default exit-on-close adapter with one that hides to tray
        if (defaultCloseAdapter != null) { removeWindowListener(defaultCloseAdapter); defaultCloseAdapter = null; }
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter()
        {
            @Override public void windowClosing(WindowEvent e) { setVisible(false); }
        });

        setVisible(false);
    }

    private void toggleVisibility()
    {
        if (isVisible()) { setVisible(false); }
        else             { setVisible(true); setExtendedState(NORMAL); toFront(); requestFocus(); }
    }

    // ── Persistence ──────────────────────────────────────────────────────────

    private void saveConfig()
    {
        config = new LauncherConfig(
                baseFolder.getAbsolutePath(), config.startMinimized(),
                getWidth(), getHeight(),
                config.priorityList(), config.explorer(), config.editor(),
                config.actionOrder(), config.entryButtonStyle(), config.showContextMenu());
        config.save(LauncherConfig.instanceConfigFile(launcherId));
    }

    private void savePriorityList()
    {
        List<String> names = new ArrayList<>();
        for (LaunchEntry e : allEntries) names.add(e.file().getName());
        config = new LauncherConfig(
                config.rootFolder(), config.startMinimized(),
                config.windowWidth(), config.windowHeight(),
                names, config.explorer(), config.editor(),
                config.actionOrder(), config.entryButtonStyle(), config.showContextMenu());
        config.save(LauncherConfig.instanceConfigFile(launcherId));
    }

    // ── Settings dialog ──────────────────────────────────────────────────────

    private void showSettings()
    {
        JDialog dlg = new JDialog(this, "Settings  \u2013  " + launcherId, true);
        dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dlg.setResizable(false);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(14, 16, 10, 16));

        // Config-file info
        root.add(settingsSectionLabel("Configuration files"));
        root.add(Box.createVerticalStrut(4));
        root.add(settingsInfoRow("Launcher ID",     launcherId,                                           null));
        root.add(Box.createVerticalStrut(2));
        root.add(settingsInfoRow("Global config",   LauncherConfig.globalConfigFile().getAbsolutePath(),
                                                    LauncherConfig.globalConfigFile().getParentFile()));
        root.add(Box.createVerticalStrut(2));
        root.add(settingsInfoRow("Instance config", LauncherConfig.instanceConfigFile(launcherId).getAbsolutePath(),
                                                    LauncherConfig.instanceConfigFile(launcherId).getParentFile()));
        root.add(settingsSeparator());

        // Startup
        root.add(settingsSectionLabel("Startup"));
        root.add(Box.createVerticalStrut(6));
        JCheckBox cbMinimized = new JCheckBox("Start minimized to system tray  (takes effect on next launch)");
        cbMinimized.setSelected(Boolean.TRUE.equals(config.startMinimized()));
        cbMinimized.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(cbMinimized);
        root.add(settingsSeparator());

        // Commands
        root.add(settingsSectionLabel("Commands"));
        root.add(Box.createVerticalStrut(6));
        JTextField tfExplorer = new JTextField(config.explorer() != null ? config.explorer() : "", 30);
        tfExplorer.setToolTipText("File explorer executable (blank = use system default)");
        root.add(settingsEditRow("EXPLORER", tfExplorer, "File explorer command \u2013 blank uses the system default"));
        root.add(Box.createVerticalStrut(4));
        JTextField tfEditor = new JTextField(config.editor() != null ? config.editor() : "", 30);
        tfEditor.setToolTipText("Editor executable, e.g. code, notepad++");
        root.add(settingsEditRow("EDITOR", tfEditor, "Editor command \u2013 blank defaults to 'code' (VS Code)"));
        root.add(settingsSeparator());

        // Action buttons
        root.add(settingsSectionLabel("Action Buttons"));
        root.add(Box.createVerticalStrut(4));
        JLabel actHint = new JLabel("Check to show \u00b7 drag up/down to reorder");
        actHint.setFont(actHint.getFont().deriveFont(Font.ITALIC, 10f));
        actHint.setForeground(Color.GRAY);
        actHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(actHint);
        root.add(Box.createVerticalStrut(4));

        List<String> orderedKeys = new ArrayList<>(effectiveActionOrder);
        for (String k : DEFAULT_ACTION_ORDER) { if (!orderedKeys.contains(k)) orderedKeys.add(k); }
        final Set<String> checkedActions = new HashSet<>(effectiveActionOrder);

        DefaultListModel<String> actModel = new DefaultListModel<>();
        orderedKeys.forEach(actModel::addElement);

        JList<String> actList = new JList<>(actModel);
        actList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        actList.setFixedCellHeight(24);
        actList.setCellRenderer((lst, value, index, isSelected, focus) ->
        {
            JCheckBox cb = new JCheckBox(actionLabel(value));
            cb.setSelected(checkedActions.contains(value));
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
                if (checkedActions.contains(key)) checkedActions.remove(key); else checkedActions.add(key);
                actList.repaint();
            }
        });

        JButton btnUp   = new JButton("\u2191");
        JButton btnDown = new JButton("\u2193");
        btnUp.addActionListener(ev ->
        {
            int idx = actList.getSelectedIndex();
            if (idx > 0) { String item = actModel.remove(idx); actModel.add(idx - 1, item); actList.setSelectedIndex(idx - 1); }
        });
        btnDown.addActionListener(ev ->
        {
            int idx = actList.getSelectedIndex();
            if (idx >= 0 && idx < actModel.getSize() - 1) { String item = actModel.remove(idx); actModel.add(idx + 1, item); actList.setSelectedIndex(idx + 1); }
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
        root.add(settingsSectionLabel("Button Style"));
        root.add(Box.createVerticalStrut(4));
        JLabel styleHint = new JLabel("Choose how entry action buttons appear in the list");
        styleHint.setFont(styleHint.getFont().deriveFont(Font.ITALIC, 10f));
        styleHint.setForeground(Color.GRAY);
        styleHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(styleHint);
        root.add(Box.createVerticalStrut(4));
        JRadioButton rbIcons     = new JRadioButton("Inline icons  \u2013 one small button per action");
        JRadioButton rbHamburger = new JRadioButton("Hamburger menu  (\u2630) \u2013 single button opens a popup");
        ButtonGroup styleGroup = new ButtonGroup();
        styleGroup.add(rbIcons); styleGroup.add(rbHamburger);
        boolean isHamburgerNow = BUTTON_STYLE_HAMBURGER.equals(config.entryButtonStyle());
        rbHamburger.setSelected(isHamburgerNow); rbIcons.setSelected(!isHamburgerNow);
        rbIcons.setAlignmentX(Component.LEFT_ALIGNMENT);
        rbHamburger.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(rbIcons); root.add(rbHamburger);
        root.add(Box.createVerticalStrut(6));

        // Context menu
        JCheckBox cbContextMenu = new JCheckBox("Show right-click context menu for folder entries");
        cbContextMenu.setSelected(resolveShowContextMenu(config));
        cbContextMenu.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(cbContextMenu);
        root.add(settingsSeparator());

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton btnSave   = new JButton("Save");
        JButton btnCancel = new JButton("Cancel");
        btnPanel.add(btnSave); btnPanel.add(btnCancel);
        root.add(btnPanel);

        btnSave.addActionListener(e ->
        {
            List<String> newOrder = new ArrayList<>();
            for (int i = 0; i < actModel.getSize(); i++)
            {
                String k = actModel.getElementAt(i);
                if (checkedActions.contains(k)) newOrder.add(k);
            }
            String explorerVal    = tfExplorer.getText().trim();
            String editorVal      = tfEditor.getText().trim();
            String newButtonStyle = rbHamburger.isSelected() ? BUTTON_STYLE_HAMBURGER : BUTTON_STYLE_ICONS;

            config = new LauncherConfig(
                    config.rootFolder(), cbMinimized.isSelected(),
                    config.windowWidth(), config.windowHeight(),
                    config.priorityList(),
                    explorerVal.isEmpty() ? null : explorerVal,
                    editorVal.isEmpty()   ? null : editorVal,
                    newOrder.isEmpty()    ? null : newOrder,
                    newButtonStyle, cbContextMenu.isSelected());
            config.save(LauncherConfig.instanceConfigFile(launcherId));

            effectiveActionOrder = resolveActionOrder(config);
            effectiveButtonStyle = resolveButtonStyle(config);
            showContextMenu      = resolveShowContextMenu(config);
            cellRenderer = new EntryCellRenderer(effectiveActionOrder, effectiveButtonStyle);
            list.setCellRenderer(cellRenderer);
            list.repaint();
            dlg.dispose();
        });
        btnCancel.addActionListener(e -> dlg.dispose());

        dlg.add(root);
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    // ── Settings dialog helpers ───────────────────────────────────────────────

    private static JLabel settingsSectionLabel(String text)
    {
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 11f));
        lbl.setForeground(new Color(0x00, 0x50, 0x99));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private static JSeparator settingsSeparator()
    {
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        return sep;
    }

    private JPanel settingsInfoRow(String label, String value, File openDir)
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
            ImageIcon ico = loadScaledIcon("folder.png", 14, 14);
            JButton btn = ico != null ? new JButton(ico) : new JButton("\uD83D\uDCC2");
            btn.setToolTipText("Open folder");
            btn.setFocusPainted(false);
            btn.setMargin(new Insets(1, 4, 1, 4));
            final File dir = openDir;
            btn.addActionListener(e -> openInExplorer(dir));
            row.add(btn, BorderLayout.EAST);
        }
        return row;
    }

    private static JPanel settingsEditRow(String label, JTextField field, String tooltip)
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

    private static JLabel coloredLabel(String text, Color color)
    {
        JLabel lbl = new JLabel("\u25a0 " + text);
        lbl.setForeground(color);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 11f));
        return lbl;
    }

    // ── Icon loading ─────────────────────────────────────────────────────────

    /**
     * Loads a PNG resource from the classpath, scales it to {@code w × h} pixels.
     * Returns {@code null} if the resource cannot be found or read.
     */
    public static ImageIcon loadScaledIcon(String name, int w, int h)
    {
        try (InputStream is = Launcher.class.getResourceAsStream("/" + name))
        {
            if (is == null) return null;
            BufferedImage raw = ImageIO.read(is);
            if (raw == null) return null;
            return new ImageIcon(raw.getScaledInstance(w, h, Image.SCALE_SMOOTH));
        }
        catch (IOException e) { return null; }
    }

    // ── Action-icon hit testing ───────────────────────────────────────────────

    /**
     * Returns the 0-based index of the action button at point {@code p},
     * or -1 if no button was hit.
     */
    static int hitActionIcon(Point p, JList<LaunchEntry> list, int idx, int numActions)
    {
        if (numActions == 0) return -1;
        Rectangle cell = list.getCellBounds(idx, idx);
        if (cell == null) return -1;
        int xInCell       = p.x - cell.x;
        int actionBarStart = cell.width - EntryCellRenderer.barWidth(numActions);
        if (xInCell < actionBarStart) return -1;
        int xInBar = xInCell - actionBarStart;
        for (int i = 0; i < numActions; i++)
        {
            int iconStart = EntryCellRenderer.ACT_HGAP
                    + i * (EntryCellRenderer.ACT_W + EntryCellRenderer.ACT_HGAP);
            if (xInBar >= iconStart && xInBar < iconStart + EntryCellRenderer.ACT_W) return i;
        }
        return -1;
    }

    // ── Entry loading ─────────────────────────────────────────────────────────

    private List<LaunchEntry> loadEntries()
    {
        List<LaunchEntry> scripts = new ArrayList<>();
        List<LaunchEntry> apps    = new ArrayList<>();
        List<LaunchEntry> plain   = new ArrayList<>();

        File[] children = baseFolder.listFiles();
        if (children != null)
        {
            Arrays.sort(children, Comparator.comparing(f -> f.getName().toLowerCase(Locale.ROOT)));
            for (File f : children)
            {
                if (f.isDirectory())
                {
                    File iconSrc = findAppIconSource(f);
                    if (iconSrc != null) apps.add(new LaunchEntry(f, EntryType.APP_FOLDER, iconSrc));
                    else                 plain.add(new LaunchEntry(f, EntryType.PLAIN_FOLDER));
                }
                else if (isScript(f))
                {
                    scripts.add(new LaunchEntry(f, EntryType.SCRIPT));
                }
            }
        }

        List<LaunchEntry> natural = new ArrayList<>(scripts);
        natural.addAll(apps);
        natural.addAll(plain);

        List<String> priority = config.priorityList();
        if (priority == null || priority.isEmpty()) return natural;

        Map<String, LaunchEntry> byName = new LinkedHashMap<>();
        for (LaunchEntry e : natural) byName.put(e.file().getName(), e);

        List<LaunchEntry> result = new ArrayList<>();
        for (String name : priority) { LaunchEntry e = byName.remove(name); if (e != null) result.add(e); }
        result.addAll(byName.values());
        return result;
    }

    private static File findAppIconSource(File dir)
    {
        File[] children = dir.listFiles();
        if (children != null)
        {
            for (File f : children)
                if (f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".lnk")) return f;
        }
        File fallback = new File(dir, FALLBACK_EXE);
        return fallback.isFile() ? fallback : null;
    }

    private static boolean isScript(File f)
    {
        String n = f.getName().toLowerCase(Locale.ROOT);
        return n.endsWith(".bat") || n.endsWith(".cmd") || n.endsWith(".ps1")
                || n.endsWith(".vbs") || n.endsWith(".sh") || n.endsWith(".js");
    }

    // ── Launching ─────────────────────────────────────────────────────────────

    private void launch(LaunchEntry entry)
    {
        try
        {
            if      (entry.type() == EntryType.SCRIPT)      launchScript(entry.file());
            else if (entry.type() == EntryType.APP_FOLDER)  launchAppFolder(entry.file());
            else                                           openInExplorer(entry.file());
        }
        catch (IOException ex)
        {
            JOptionPane.showMessageDialog(this, "Failed to launch:\n" + ex.getMessage(),
                    "Launcher Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void launchScript(File script) throws IOException
    {
        String name = script.getName().toLowerCase(Locale.ROOT);
        ProcessBuilder pb;
        if      (name.endsWith(".ps1"))                     pb = new ProcessBuilder("powershell", "-ExecutionPolicy", "Bypass", "-NoExit", "-File", script.getAbsolutePath());
        else if (name.endsWith(".vbs") || name.endsWith(".js")) pb = new ProcessBuilder("wscript", script.getAbsolutePath());
        else if (name.endsWith(".sh"))                      pb = new ProcessBuilder("bash", script.getAbsolutePath());
        else                                                pb = new ProcessBuilder("cmd", "/c", "start", script.getName(), "cmd", "/k", script.getAbsolutePath());
        pb.directory(script.getParentFile());
        pb.start();
    }

    private void launchAppFolder(File appFolder) throws IOException
    {
        File[] children = appFolder.listFiles();
        if (children != null)
        {
            for (File f : children)
            {
                if (f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".lnk"))
                {
                    new ProcessBuilder("cmd", "/c", "start", "", f.getAbsolutePath())
                            .directory(appFolder).start();
                    return;
                }
            }
        }
        File fallback = new File(appFolder, FALLBACK_EXE);
        if (fallback.isFile())
        {
            new ProcessBuilder(fallback.getAbsolutePath()).directory(appFolder).start();
        }
        else
        {
            JOptionPane.showMessageDialog(this,
                    "<html>No shortcut (.lnk) found in:<br>&nbsp;&nbsp;<b>" + appFolder.getAbsolutePath()
                    + "</b><br><br>Fallback executable not found at:<br>&nbsp;&nbsp;<b>"
                    + fallback.getAbsolutePath() + "</b></html>",
                    "Launcher \u2013 Nothing to start", JOptionPane.WARNING_MESSAGE);
        }
    }

    // ── Folder actions ────────────────────────────────────────────────────────

    private void showActionsPopup(LaunchEntry sel, Component invoker, int x, int y)
    {
        JPopupMenu menu = new JPopupMenu();
        for (String key : effectiveActionOrder)
        {
            JMenuItem mi = switch (key)
            {
                case EXPLORE_ACTION -> new JMenuItem("Open in File Explorer");
                case EDITOR_ACTION  -> new JMenuItem("Open in Editor");
                case COPY_ACTION    -> new JMenuItem("Copy with Robocopy...");
                case DELETE_ACTION  -> new JMenuItem("Delete");
                default -> null;
            };
            if (mi == null) continue;
            switch (key)
            {
                case EXPLORE_ACTION -> mi.addActionListener(e -> openInExplorer(sel.file()));
                case EDITOR_ACTION  -> mi.addActionListener(e -> openInEditor(sel.file()));
                case COPY_ACTION    -> mi.addActionListener(e -> copyWithRobocopy(sel.file()));
                case DELETE_ACTION  -> mi.addActionListener(e -> deleteFolder(sel.file()));
            }
            menu.add(mi);
        }
        menu.addSeparator();
        JMenuItem miSVN = new JMenuItem("SVN Checkout...");
        miSVN.addActionListener(e -> svnCheckout(sel.file()));
        menu.add(miSVN);
        menu.show(invoker, x, y);
    }

    private void openInExplorer(File folder)
    {
        String cmd = config.explorer();
        try
        {
            if (cmd != null && !cmd.isBlank()) new ProcessBuilder(cmd, folder.getAbsolutePath()).start();
            else                               Desktop.getDesktop().open(folder);
        }
        catch (IOException ex)
        {
            JOptionPane.showMessageDialog(this, "Could not open file explorer:\n" + ex.getMessage(),
                    "Launcher Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openInEditor(File folder)
    {
        String editorCmd = (config.editor() != null && !config.editor().isBlank()) ? config.editor() : "code";
        try
        {
            new ProcessBuilder("cmd", "/c", editorCmd, folder.getAbsolutePath()).directory(folder).start();
        }
        catch (IOException ex)
        {
            JOptionPane.showMessageDialog(this,
                    "Could not open editor.\nMake sure '" + editorCmd + "' is on your PATH.\n\n" + ex.getMessage(),
                    "Launcher Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void copyWithRobocopy(File srcFolder)
    {
        String newName = JOptionPane.showInputDialog(this,
                "Enter new folder name for the copy:", srcFolder.getName() + "_copy");
        if (newName == null || newName.trim().isEmpty()) return;
        newName = newName.trim();
        File destPath = new File(baseFolder, newName);
        if (destPath.exists())
        {
            JOptionPane.showMessageDialog(this,
                    "<html>A folder with the name <b>" + newName + "</b> already exists<br>"
                    + "in the active launcher folder:<br><br><b>" + baseFolder.getAbsolutePath() + "</b></html>",
                    "Name Already Exists", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try
        {
            ProcessBuilder pb = new ProcessBuilder("robocopy", srcFolder.getAbsolutePath(), destPath.getAbsolutePath(), "/MIR");
            pb.redirectErrorStream(true);
            showProcessOutput(pb.start(), "Robocopy: " + srcFolder.getName() + " \u2192 " + newName, this::refreshList);
        }
        catch (IOException ex)
        {
            JOptionPane.showMessageDialog(this, "Could not start robocopy:\n" + ex.getMessage(),
                    "Launcher Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteFolder(File folder)
    {
        int confirm = JOptionPane.showConfirmDialog(this,
                "<html>Permanently delete:<br><b>" + folder.getAbsolutePath()
                + "</b><br><br>This cannot be undone.</html>",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        try
        {
            deleteDirectory(folder);
            refreshList();
        }
        catch (IOException ex)
        {
            JOptionPane.showMessageDialog(this, "Could not delete folder:\n" + ex.getMessage(),
                    "Launcher Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void deleteDirectory(File dir) throws IOException
    {
        if (!dir.exists()) return;
        if (dir.isDirectory())
        {
            File[] children = dir.listFiles();
            if (children != null) for (File child : children) deleteDirectory(child);
        }
        if (!dir.delete()) throw new IOException("Failed to delete: " + dir.getAbsolutePath());
    }

    private void svnCheckout(File targetFolder)
    {
        String url = JOptionPane.showInputDialog(this, "Enter SVN repository URL:",
                "SVN Checkout", JOptionPane.QUESTION_MESSAGE);
        if (url == null || url.trim().isEmpty()) return;
        url = url.trim();
        try
        {
            String repoName = url.replaceAll(".*/+", "").replaceAll("\\..*", "");
            if (repoName.isEmpty()) repoName = "checkout";
            File checkoutDir = new File(targetFolder, repoName);
            ProcessBuilder pb = new ProcessBuilder("svn", "checkout", url, checkoutDir.getAbsolutePath());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            showProcessOutput(process, "SVN Checkout: " + url, null);
            final String finalUrl = url;
            new Thread(() ->
            {
                try
                {
                    process.waitFor();
                    SwingUtilities.invokeLater(() ->
                    {
                        int r = JOptionPane.showConfirmDialog(this,
                                "Checkout completed. Refresh the file list?",
                                "SVN Checkout", JOptionPane.YES_NO_OPTION);
                        if (r == JOptionPane.YES_OPTION)
                            setTitle("Launcher  \u2013  " + baseFolder.getAbsolutePath());
                    });
                }
                catch (InterruptedException ignored) {}
            }).start();
        }
        catch (IOException ex)
        {
            JOptionPane.showMessageDialog(this,
                    "Could not start SVN:\n" + ex.getMessage() + "\n\nMake sure 'svn' is installed and on your PATH.",
                    "Launcher Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void showProcessOutput(Process process, String title, Runnable onComplete)
    {
        SwingUtilities.invokeLater(() ->
        {
            JFrame outputFrame = new JFrame(title);
            outputFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            outputFrame.setSize(800, 600);
            outputFrame.setLocationRelativeTo(null);

            DefaultListModel<String> model = new DefaultListModel<>();
            JList<String> outputList = new JList<>(model);
            outputList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
            outputFrame.add(new JScrollPane(outputList), BorderLayout.CENTER);
            outputFrame.setVisible(true);

            new Thread(() ->
            {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream())))
                {
                    String line;
                    while ((line = reader.readLine()) != null)
                    {
                        final String out = line;
                        SwingUtilities.invokeLater(() -> { model.addElement(out); outputList.ensureIndexIsVisible(model.getSize() - 1); });
                    }
                }
                catch (IOException ignored) {}
                SwingUtilities.invokeLater(() ->
                {
                    if (onComplete != null) { outputFrame.dispose(); onComplete.run(); }
                    else { model.addElement(""); model.addElement("Process completed."); outputList.ensureIndexIsVisible(model.getSize() - 1); }
                });
            }).start();
        });
    }

    // ── Entry point ───────────────────────────────────────────────────────────

    public static void main(String[] args)
    {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}

        boolean minimizedCli  = false;
        String  folderArg     = null;
        String  launcherIdArg = null;
        String  configPathArg = null;

        for (String arg : args)
        {
            if      (arg.equalsIgnoreCase("--minimized") || arg.equalsIgnoreCase("-minimized")) minimizedCli = true;
            else if (arg.startsWith("--launcherId=")) launcherIdArg = arg.substring("--launcherId=".length()).trim();
            else if (arg.startsWith("--config="))     configPathArg = arg.substring("--config=".length()).trim();
            else if (folderArg == null && !arg.startsWith("--")) folderArg = arg;
        }

        File globalFile = LauncherConfig.globalConfigFile();
        LauncherConfig globalConfig = LauncherConfig.loadFile(globalFile);
        if (!globalFile.exists()) LauncherConfig.defaults().save(globalFile);

        String folderStr = (folderArg != null) ? folderArg : globalConfig.rootFolder();
        final File folder;
        if (folderStr != null)
        {
            folder = new File(folderStr);
        }
        else
        {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select launcher root folder");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);
            if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) System.exit(0);
            folder = chooser.getSelectedFile();
        }

        if (!folder.isDirectory())
        {
            JOptionPane.showMessageDialog(null, "Not a valid directory:\n" + folder.getAbsolutePath(),
                    "Launcher", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        final String launcherId = (launcherIdArg != null && !launcherIdArg.isEmpty())
                ? launcherIdArg
                : String.format("%08x", folder.getAbsolutePath().hashCode() & 0xFFFFFFFFL);

        LauncherConfig instanceConfig = LauncherConfig.loadFile(LauncherConfig.instanceConfigFile(launcherId));
        LauncherConfig explicitConfig = (configPathArg != null)
                ? LauncherConfig.loadFile(new File(configPathArg)) : LauncherConfig.empty();

        LauncherConfig merged = explicitConfig
                .mergeOver(instanceConfig.mergeOver(globalConfig))
                .withDefaults();

        boolean startMinimized = minimizedCli || Boolean.TRUE.equals(merged.startMinimized());
        final File resolvedFolder = (folderArg != null) ? new File(folderArg) : folder;

        final LauncherConfig resolvedConfig = new LauncherConfig(
                resolvedFolder.getAbsolutePath(), startMinimized,
                merged.windowWidth(), merged.windowHeight(),
                merged.priorityList(), merged.explorer(), merged.editor(),
                merged.actionOrder(), merged.entryButtonStyle(), merged.showContextMenu());
        resolvedConfig.save(LauncherConfig.instanceConfigFile(launcherId));

        final boolean minimized = startMinimized;
        SwingUtilities.invokeLater(() ->
        {
            Launcher launcher = new Launcher(resolvedFolder, resolvedConfig, launcherId);
            if (minimized) launcher.setupTray();
            else           launcher.setVisible(true);
        });
    }
}
