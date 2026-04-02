package dev.nonvocal.launcher;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Launcher – Java Swing application entry point and UI shell.
 * <p>
 * Delegates to focused helper classes:
 * <ul>
 *   <li>{@link EntryLoader}          – scanning and sorting entries</li>
 *   <li>{@link EntryLauncher}        – launching scripts and app folders</li>
 *   <li>{@link FolderActions}        – explorer / editor / copy / delete / SVN</li>
 *   <li>{@link ListMouseHandler}     – click and hover behaviour on the list</li>
 *   <li>{@link EntryListTransferHandler} – drag-and-drop reordering</li>
 *   <li>{@link SettingsDialog}       – settings UI</li>
 *   <li>{@link ProcessOutputWindow}  – real-time process output window</li>
 *   <li>{@link EntryCellRenderer}    – list row rendering</li>
 *   <li>{@link LauncherConfig}       – config load / merge / save</li>
 * </ul>
 */
public class Launcher extends JFrame
{
    private static final long serialVersionUID = 1L;

    // ── Action keys ──────────────────────────────────────────────────────────

    public static final String EXPLORE_ACTION = "EXPLORE_ACTION";
    public static final String EDITOR_ACTION  = "EDITOR_ACTION";
    public static final String COPY_ACTION    = "COPY_ACTION";
    public static final String DELETE_ACTION  = "DELETE_ACTION";

    static final List<String> DEFAULT_ACTION_ORDER =
            List.of(EXPLORE_ACTION, EDITOR_ACTION, COPY_ACTION, DELETE_ACTION);

    // ── Toolbar action keys ──────────────────────────────────────────────────

    public static final String SVN_CHECKOUT_ACTION = "SVN_CHECKOUT_ACTION";
    public static final String SVN_BROWSER_ACTION  = "SVN_BROWSER_ACTION";

    static final List<String> DEFAULT_TOOLBAR_ACTIONS =
            List.of(SVN_CHECKOUT_ACTION, SVN_BROWSER_ACTION);

    // ── Button style keys ────────────────────────────────────────────────────

    public static final String BUTTON_STYLE_ICONS     = "ICONS";
    public static final String BUTTON_STYLE_HAMBURGER = "HAMBURGER";

    // ── Instance state ───────────────────────────────────────────────────────

    private final File   baseFolder;
    private final String launcherId;
    private LauncherConfig config;

    private DefaultListModel<LaunchEntry> listModel;
    private JList<LaunchEntry>            list;
    private JLabel                        hintLabel;
    private JLabel                        searchLabel;
    private transient String              searchQuery = "";
    private transient final List<LaunchEntry> allEntries = new ArrayList<>();

    // Resolved from config – updated when settings are saved
    private List<String>    effectiveActionOrder;
    private List<String>    effectiveToolbarActions;
    private String          effectiveButtonStyle;
    private boolean         showContextMenu;
    private EntryCellRenderer cellRenderer;

    /** Registered in buildUI (save + exit). Removed by setupTray() so only the tray adapter fires. */
    private WindowAdapter defaultCloseAdapter;

    // Collaborators
    private FolderActions  folderActions;
    private EntryLauncher  entryLauncher;

    // Toolbar state – populated in buildUI, updated by applyConfig
    private JPanel  toolbarLeftPanel;
    private JButton svnCheckoutBtn;
    private JButton svnBrowserBtn;

    // ── Constructor ──────────────────────────────────────────────────────────

    Launcher(File baseFolder, LauncherConfig config, String launcherId)
    {
        this.baseFolder  = baseFolder;
        this.config      = config;
        this.launcherId  = launcherId;
        buildUI();
    }

    // ── Config resolution ────────────────────────────────────────────────────

    private List<String> resolveActionOrder(LauncherConfig cfg)
    {
        List<String> order = cfg.actionOrder();
        if (order == null) return new ArrayList<>(DEFAULT_ACTION_ORDER);
        return order.stream().filter(DEFAULT_ACTION_ORDER::contains).collect(Collectors.toList());
    }

    private List<String> resolveToolbarActions(LauncherConfig cfg)
    {
        List<String> order = cfg.toolbarActions();
        if (order == null) return new ArrayList<>(DEFAULT_TOOLBAR_ACTIONS);
        return order.stream().filter(DEFAULT_TOOLBAR_ACTIONS::contains).collect(Collectors.toList());
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

    private void applyConfig(LauncherConfig cfg)
    {
        config                  = cfg;
        effectiveActionOrder    = resolveActionOrder(cfg);
        effectiveToolbarActions = resolveToolbarActions(cfg);
        effectiveButtonStyle    = resolveButtonStyle(cfg);
        showContextMenu         = resolveShowContextMenu(cfg);
        cellRenderer            = new EntryCellRenderer(effectiveActionOrder, effectiveButtonStyle);
        if (list != null)
        {
            list.setCellRenderer(cellRenderer);
            list.repaint();
        }
        updateToolbarButtons();
    }

    /**
     * Rebuilds the configurable section of the toolbar to match
     * {@link #effectiveToolbarActions} (order + visibility).
     */
    private void updateToolbarButtons()
    {
        if (toolbarLeftPanel == null) return;
        toolbarLeftPanel.removeAll();
        for (String key : effectiveToolbarActions)
        {
            if (SVN_CHECKOUT_ACTION.equals(key) && svnCheckoutBtn != null)
                toolbarLeftPanel.add(svnCheckoutBtn);
            else if (SVN_BROWSER_ACTION.equals(key) && svnBrowserBtn != null)
                toolbarLeftPanel.add(svnBrowserBtn);
        }
        toolbarLeftPanel.revalidate();
        toolbarLeftPanel.repaint();
    }

    // ── List management ──────────────────────────────────────────────────────

    private void refreshList()
    {
        allEntries.clear();
        allEntries.addAll(EntryLoader.load(baseFolder, config));
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

    private void clearSearch() { searchQuery = ""; applyFilter(); }

    // ── UI construction ──────────────────────────────────────────────────────

    private void buildUI()
    {
        applyConfig(config);

        folderActions = new FolderActions(this, baseFolder, () -> config, this::refreshList);
        entryLauncher = new EntryLauncher(this);

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
        allEntries.addAll(EntryLoader.load(baseFolder, config));
        allEntries.forEach(listModel::addElement);

        list = new JList<>(listModel)
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
                            return EntryCellRenderer.ACT_TIP_MAP.getOrDefault(effectiveActionOrder.get(ai), "");
                    }
                }
                return null;
            }
        };
        ToolTipManager.sharedInstance().registerComponent(list);
        list.setCellRenderer(cellRenderer);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(36);

        ListMouseHandler mouseHandler = new ListMouseHandler(
                list, listModel,
                () -> effectiveButtonStyle,
                () -> effectiveActionOrder,
                () -> showContextMenu,
                this::showActionsPopup,
                sel -> entryLauncher.launch(sel, folderActions::openInExplorer),
                folderActions);
        list.addMouseListener(mouseHandler);
        list.addMouseMotionListener(mouseHandler);

        list.setTransferHandler(new EntryListTransferHandler(
                list, listModel, allEntries,
                () -> !searchQuery.isEmpty(),
                this::savePriorityList));
        list.setDragEnabled(true);
        list.setDropMode(DropMode.INSERT);

        list.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "launch");
        list.getActionMap().put("launch", new AbstractAction()
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                LaunchEntry sel = list.getSelectedValue();
                if (sel != null) entryLauncher.launch(sel, folderActions::openInExplorer);
            }
        });

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(0xCC, 0xCC, 0xCC)));

        // ── Footer ───────────────────────────────────────────────────────────
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        legend.setBackground(new Color(0xF0, 0xF0, 0xF0));
        legend.setBorder(new MatteBorder(1, 0, 0, 0, new Color(0xCC, 0xCC, 0xCC)));
        legend.add(coloredLabel("Scripts",             new Color(0x1A, 0x5F, 0x7A)));
        legend.add(coloredLabel("Application folders", new Color(0x2E, 0x6B, 0x2E)));
        legend.add(coloredLabel("Folders",             new Color(0x66, 0x55, 0x44)));

        searchLabel = new JLabel();
        searchLabel.setForeground(new Color(0x00, 0x50, 0xA0));
        searchLabel.setFont(searchLabel.getFont().deriveFont(Font.BOLD | Font.ITALIC, 11f));
        searchLabel.setHorizontalAlignment(JLabel.CENTER);

        hintLabel = new JLabel(listModel.size() + " entries   |   Double-click or Enter to launch");
        hintLabel.setForeground(Color.GRAY);
        hintLabel.setFont(hintLabel.getFont().deriveFont(11f));

        JPanel south = new JPanel(new BorderLayout());
        south.add(legend,      BorderLayout.WEST);
        south.add(searchLabel, BorderLayout.CENTER);
        south.add(hintLabel,   BorderLayout.EAST);
        south.setBorder(new EmptyBorder(0, 10, 0, 10));
        south.setBackground(new Color(0xF0, 0xF0, 0xF0));

        // ── Toolbar ──────────────────────────────────────────────────────────
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBackground(new Color(0xF0, 0xF2, 0xF5));
        toolbar.setBorder(new MatteBorder(0, 0, 1, 0, new Color(0xCC, 0xCC, 0xCC)));

        // Configurable left-side panel – repopulated by updateToolbarButtons()
        toolbarLeftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        toolbarLeftPanel.setOpaque(false);

        ImageIcon svnIcon = loadScaledIcon("apps-add.png", 20, 20);

        svnCheckoutBtn = svnIcon != null ? new JButton(svnIcon) : new JButton("SVN");
        svnCheckoutBtn.setToolTipText("SVN Checkout \u2013 check out a repository into the selected folder");
        svnCheckoutBtn.setFocusPainted(false);
        svnCheckoutBtn.addActionListener(ev ->
        {
            LaunchEntry sel = list.getSelectedValue();
            folderActions.svnCheckout(
                    (sel != null && sel.type() != EntryType.SCRIPT) ? sel.file() : baseFolder);
        });

        svnBrowserBtn = svnIcon != null ? new JButton(svnIcon) : new JButton("SVN Browser");
        svnBrowserBtn.setToolTipText("SVN Repository Browser \u2013 browse the repository and check out projects");
        svnBrowserBtn.setFocusPainted(false);
        svnBrowserBtn.addActionListener(ev ->
        {
            LaunchEntry sel = list.getSelectedValue();
            folderActions.svnCheckoutWithRepoBrowser(
                    (sel != null && sel.type() != EntryType.SCRIPT) ? sel.file() : baseFolder);
        });

        // Populate the left panel for the first time
        updateToolbarButtons();

        toolbar.add(toolbarLeftPanel);
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
            if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow() != this)
                return false;
            char c = event.getKeyChar();
            if (c == KeyEvent.CHAR_UNDEFINED) return false;
            if      (c == '\u001b') { clearSearch(); }
            else if (c == '\b')
            {
                if (!searchQuery.isEmpty())
                { searchQuery = searchQuery.substring(0, searchQuery.length() - 1); applyFilter(); }
            }
            else if (c == '\r' || c == '\n') { return false; }
            else if (c >= 32) { searchQuery += c; applyFilter(); }
            return false;
        });
    }

    // ── Actions popup ────────────────────────────────────────────────────────

    /** Builds and shows a popup menu with all currently enabled actions. */
    private void showActionsPopup(LaunchEntry sel, int x, int y)
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
                case EXPLORE_ACTION -> mi.addActionListener(e -> folderActions.openInExplorer(sel.file()));
                case EDITOR_ACTION  -> mi.addActionListener(e -> folderActions.openInEditor(sel.file()));
                case COPY_ACTION    -> mi.addActionListener(e -> folderActions.copyWithRobocopy(sel.file()));
                case DELETE_ACTION  -> mi.addActionListener(e -> folderActions.deleteFolder(sel.file()));
            }
            menu.add(mi);
        }
        menu.show(list, x, y);
    }

    // ── Settings ─────────────────────────────────────────────────────────────

    private void showSettings()
    {
        SettingsDialog dlg = new SettingsDialog(
                this, launcherId, config, effectiveActionOrder, effectiveToolbarActions,
                updatedConfig ->
                {
                    applyConfig(updatedConfig);
                    updatedConfig.save(LauncherConfig.instanceConfigFile(launcherId));
                });
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    // ── System tray ──────────────────────────────────────────────────────────

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

        // Replace exit-on-close with hide-to-tray
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
                config.actionOrder(), config.entryButtonStyle(), config.showContextMenu(),
                config.toolbarActions());
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
                config.actionOrder(), config.entryButtonStyle(), config.showContextMenu(),
                config.toolbarActions());
        config.save(LauncherConfig.instanceConfigFile(launcherId));
    }

    // ── Static utilities ──────────────────────────────────────────────────────

    /** Creates a small coloured legend label. */
    private static JLabel coloredLabel(String text, Color color)
    {
        JLabel lbl = new JLabel("\u25A0  " + text);
        lbl.setForeground(color);
        lbl.setFont(lbl.getFont().deriveFont(11f));
        return lbl;
    }

    /**
     * Loads a PNG resource from the classpath and scales it to {@code w × h} pixels.
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

    /**
     * Returns the 0-based index of the action button hit by point {@code p}, or -1.
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
            int start = EntryCellRenderer.ACT_HGAP + i * (EntryCellRenderer.ACT_W + EntryCellRenderer.ACT_HGAP);
            if (xInBar >= start && xInBar < start + EntryCellRenderer.ACT_W) return i;
        }
        return -1;
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
            if      (arg.equalsIgnoreCase("--minimized") || arg.equalsIgnoreCase("-minimized"))
                minimizedCli = true;
            else if (arg.startsWith("--launcherId="))
                launcherIdArg = arg.substring("--launcherId=".length()).trim();
            else if (arg.startsWith("--config="))
                configPathArg = arg.substring("--config=".length()).trim();
            else if (folderArg == null && !arg.startsWith("--"))
                folderArg = arg;
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
            JOptionPane.showMessageDialog(null,
                    "Not a valid directory:\n" + folder.getAbsolutePath(),
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
                merged.actionOrder(), merged.entryButtonStyle(), merged.showContextMenu(),
                merged.toolbarActions());
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
