package dev.nonvocal.launcher;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.filechooser.FileSystemView;
import java.awt.Desktop;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.awt.Image;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.awt.AWTException;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

/**
 * Launcher – self-contained Java Swing application.
 *
 * Usage:
 *   java Launcher <rootFolder>
 *   java Launcher              (opens a folder-chooser dialog)
 *
 * The root folder is scanned at the FIRST level only:
 *   • Files with a recognised script extension are listed as scripts.
 *   • Sub-directories are listed as application folders.
 *
 * On double-click / Enter:
 *   Script      → executed in an appropriate interpreter/shell.
 *   App folder  → 1) first .lnk shortcut found at the folder's top level is
 *                    launched via the Windows shell, or
 *                 2) <appFolder>\basis\sys\win\bin\dsc_StartPlm.exe is run.
 *
 * Compile:  javac Launcher.java
 * Run:      java  Launcher  C:\path\to\rootFolder
 */
public class Launcher extends JFrame {

    private static final long serialVersionUID = 1L;

    /** Relative path of the fallback executable inside an application folder. */
    private static final String FALLBACK_EXE = "basis\\sys\\win\\bin\\dsc_StartPlm.exe";

    // =========================================================================
    //  Data model
    // =========================================================================

    enum EntryType { SCRIPT, APP_FOLDER, PLAIN_FOLDER }

    /**
     * @param iconFile For APP_FOLDER: the .lnk or fallback exe used as icon source; null otherwise.
     */
    record LaunchEntry(File file, EntryType type, File iconFile)
    {
            LaunchEntry(File file, EntryType type)
            {
                this(file, type, null);
            }

        @Override
            public String toString()
        {
            return file.getName();
        }
        }

    // =========================================================================
    //  Cell renderer
    // =========================================================================

    static final class EntryCellRenderer extends JPanel implements ListCellRenderer<LaunchEntry> {

        private static final long serialVersionUID = 1L;

        /** Width of each action-icon button (px). */
        static final int ACT_W    = 22;
        /** FlowLayout horizontal gap around / between action icons (px). */
        static final int ACT_HGAP = 3;
        /** Total width of the action bar: 5 gaps + 4 icons = 5*3 + 4*22 = 103 px. */
        static final int ACT_BAR_W = 5 * ACT_HGAP + 4 * ACT_W;

        // Row colours
        private static final Color ROW_EVEN  = new Color(0xF4, 0xF6, 0xF8);
        private static final Color ROW_ODD   = Color.WHITE;
        // Name foreground colours by entry type
        private static final Color FG_SCRIPT = new Color(0x1A, 0x5F, 0x7A); // dark teal
        private static final Color FG_FOLDER = new Color(0x2E, 0x6B, 0x2E); // dark green
        private static final Color FG_PLAIN  = new Color(0x66, 0x55, 0x44); // warm dark gray
        // Selection
        private static final Color SEL_BG    = new Color(0x00, 0x78, 0xD7); // Windows blue
        // Action icon colours – normal
        private static final Color ACT_FG    = new Color(0x33, 0x55, 0x99);
        private static final Color ACT_DEL   = new Color(0xAA, 0x22, 0x22);
        private static final Color ACT_BG    = new Color(0xE8, 0xEA, 0xF4);
        private static final Color ACT_BORD  = new Color(0xBB, 0xBB, 0xCC);
        // Action icon colours – selected row
        private static final Color SEL_ACT_BG   = new Color(0x40, 0x90, 0xD7);
        private static final Color SEL_ACT_BORD = new Color(0x80, 0xB8, 0xFF);

        private static final Font CELL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
        private static final Font ACT_FONT  = new Font(Font.SANS_SERIF, Font.BOLD, 9);

        /** Labels for the four action icons: Explorer · VS Code · Copy · Delete. */
        private static final String[] ACT_TEXT = { "E", "VS", "C", "\u2715" };
        private static final String[] ACT_TIPS = {
            "Open in File Explorer",
            "Open in VS Code",
            "Copy with Robocopy\u2026",
            "Delete"
        };

        /** PNG icons for the four action buttons (null → fall back to ACT_TEXT). */
        private static final ImageIcon[] ACT_ICON_IMGS = {
            Launcher.loadScaledIcon("folder.png",        14, 14),  // 0 – Explorer
            Launcher.loadScaledIcon("edit-document.png", 14, 14),  // 1 – VS Code
            Launcher.loadScaledIcon("copy.png",          14, 14),  // 2 – Robocopy
            Launcher.loadScaledIcon("bin.png",           14, 14),  // 3 – Delete
        };

        private final JLabel nameLabel = new JLabel();
        private final JPanel actionBar  = new JPanel(new FlowLayout(FlowLayout.LEFT, ACT_HGAP, 0));
        private final JLabel[] actIcons = new JLabel[4];

        private transient final FileSystemView fsv = FileSystemView.getFileSystemView();

        EntryCellRenderer() {
            setLayout(new BorderLayout());
            setOpaque(true);

            nameLabel.setFont(CELL_FONT);
            nameLabel.setBorder(new EmptyBorder(5, 8, 5, 8));
            nameLabel.setOpaque(false);
            add(nameLabel, BorderLayout.CENTER);

            actionBar.setOpaque(false);
            // EmptyBorder top/bottom = (36 - 18) / 2 = 9 px → centres 18 px icons in 36 px row
            actionBar.setBorder(new EmptyBorder(9, 0, 9, 0));

            for (int i = 0; i < 4; i++) {
                ImageIcon img = ACT_ICON_IMGS[i];
                JLabel lbl = (img != null)
                        ? new JLabel(img, JLabel.CENTER)
                        : new JLabel(ACT_TEXT[i], JLabel.CENTER);
                lbl.setFont(ACT_FONT);
                lbl.setForeground(i == 3 ? ACT_DEL : ACT_FG);
                lbl.setBackground(ACT_BG);
                lbl.setOpaque(true);
                lbl.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ACT_BORD, 1),
                        BorderFactory.createEmptyBorder(1, 3, 1, 3)));
                lbl.setToolTipText(ACT_TIPS[i]);
                lbl.setPreferredSize(new Dimension(ACT_W, ACT_W - 4)); // 22 × 18
                actIcons[i] = lbl;
                actionBar.add(lbl);
            }
            add(actionBar, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(
                JList<? extends LaunchEntry> list, LaunchEntry e, int index,
                boolean selected, boolean cellHasFocus) {

            nameLabel.setText("  " + e.file.getName());
            nameLabel.setToolTipText(e.file.getAbsolutePath());

            // For app folders use the icon of the .lnk / exe
            File iconSrc = (e.iconFile != null) ? e.iconFile : e.file;
            try { nameLabel.setIcon(fsv.getSystemIcon(iconSrc)); }
            catch (Exception ignored) { nameLabel.setIcon(null); }

            // Action icons shown only for folder entries (not scripts)
            actionBar.setVisible(e.type != EntryType.SCRIPT);

            if (selected) {
                setBackground(SEL_BG);
                nameLabel.setForeground(Color.WHITE);
                for (JLabel icon : actIcons) {
                    icon.setBackground(SEL_ACT_BG);
                    icon.setForeground(Color.WHITE);
                    icon.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(SEL_ACT_BORD, 1),
                            BorderFactory.createEmptyBorder(1, 3, 1, 3)));
                }
            } else {
                setBackground(index % 2 == 0 ? ROW_EVEN : ROW_ODD);
                nameLabel.setForeground(e.type == EntryType.SCRIPT ? FG_SCRIPT
                                      : e.type == EntryType.APP_FOLDER ? FG_FOLDER
                                      : FG_PLAIN);
                for (int i = 0; i < 4; i++) {
                    actIcons[i].setBackground(ACT_BG);
                    actIcons[i].setForeground(i == 3 ? ACT_DEL : ACT_FG);
                    actIcons[i].setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(ACT_BORD, 1),
                            BorderFactory.createEmptyBorder(1, 3, 1, 3)));
                }
            }
            return this;
        }
    }

    // =========================================================================
    //  Constructor / UI
    // =========================================================================

    private final File baseFolder;
    private DefaultListModel<LaunchEntry> listModel;
    private JLabel hintLabel;
    private JLabel searchLabel;
    private transient String searchQuery = "";
    private transient final List<LaunchEntry> allEntries = new ArrayList<>();

    Launcher(File baseFolder, boolean startMinimized) {
        this.baseFolder = baseFolder;
        buildUI();
    }

    /** Reloads all entries from disk and re-applies the current search filter. */
    private void refreshList() {
        allEntries.clear();
        allEntries.addAll(loadEntries());
        applyFilter();
    }

    /** Filters listModel to entries whose name contains searchQuery (case-insensitive). */
    private void applyFilter() {
        listModel.clear();
        String q = searchQuery.toLowerCase(Locale.ROOT);
        for (LaunchEntry entry : allEntries) {
            if (q.isEmpty() || entry.file.getName().toLowerCase(Locale.ROOT).contains(q)) {
                listModel.addElement(entry);
            }
        }
        if (searchQuery.isEmpty()) {
            searchLabel.setText("");
            hintLabel.setText(listModel.size() + " entries   |   Double-click or Enter to launch");
        } else {
            searchLabel.setText("  Filter: " + searchQuery + "\u258C");
            hintLabel.setText(listModel.size() + " of " + allEntries.size() + "   |   Esc to clear");
        }
    }

    /** Clears the search query and shows all entries. */
    private void clearSearch() {
        searchQuery = "";
        applyFilter();
    }

    private void buildUI() {
        setTitle("Launcher  –  " + baseFolder.getAbsolutePath());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(560, 680);
        setMinimumSize(new Dimension(320, 200));
        setLocationRelativeTo(null);

        // Application window icon
        ImageIcon appIcon = loadScaledIcon("apps.png", 32, 32);
        if (appIcon != null) setIconImage(appIcon.getImage());

        // ── Header bar ───────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0x00, 0x78, 0xD7));
        header.setBorder(new EmptyBorder(10, 14, 10, 14));

        JLabel dirLabel = new JLabel(baseFolder.getName()
                + "   —   " + baseFolder.getAbsolutePath());
        dirLabel.setFont(dirLabel.getFont().deriveFont(Font.BOLD, 13f));
        dirLabel.setForeground(Color.WHITE);
        header.add(dirLabel, BorderLayout.CENTER);

        // ── Entry list ───────────────────────────────────────────────────────
        listModel = new DefaultListModel<>();
        allEntries.addAll(loadEntries());
        allEntries.forEach(listModel::addElement);

        JList<LaunchEntry> list = new JList<>(listModel);
        list.setCellRenderer(new EntryCellRenderer());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(36);

        // Click-to-launch, action-icon clicks, right-click context menu
        MouseAdapter mouseHandler = new MouseAdapter() {
            private void handlePopup(MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                int idx = list.locationToIndex(e.getPoint());
                if (idx < 0) return;
                list.setSelectedIndex(idx);
                LaunchEntry sel = listModel.getElementAt(idx);
                if (sel.type == EntryType.SCRIPT) return;

                JPopupMenu menu = new JPopupMenu();

                JMenuItem miExplorer = new JMenuItem("Open in File Explorer");
                miExplorer.addActionListener(ev -> openInExplorer(sel.file));
                menu.add(miExplorer);

                JMenuItem miVSCode = new JMenuItem("Open in VS Code");
                miVSCode.addActionListener(ev -> openInVSCode(sel.file));
                menu.add(miVSCode);

                menu.addSeparator();

                JMenuItem miCopy = new JMenuItem("Copy with Robocopy...");
                miCopy.addActionListener(ev -> copyWithRobocopy(sel.file));
                menu.add(miCopy);

                JMenuItem miDelete = new JMenuItem("Delete");
                miDelete.addActionListener(ev -> deleteFolder(sel.file));
                menu.add(miDelete);

                menu.addSeparator();

                JMenuItem miSVNCheckout = new JMenuItem("SVN Checkout...");
                miSVNCheckout.addActionListener(ev -> svnCheckout(sel.file));
                menu.add(miSVNCheckout);

                menu.show(list, e.getX(), e.getY());
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                int idx = list.locationToIndex(e.getPoint());
                if (idx < 0) return;
                LaunchEntry sel = listModel.getElementAt(idx);

                // Check if a rendered action icon was clicked (folder entries only)
                if (sel.type != EntryType.SCRIPT) {
                    int actionIdx = hitActionIcon(e.getPoint(), list, idx);
                    if (actionIdx >= 0) {
                        if (e.getClickCount() == 1) {
                            switch (actionIdx) {
                                case 0 -> openInExplorer(sel.file);
                                case 1 -> openInVSCode(sel.file);
                                case 2 -> copyWithRobocopy(sel.file);
                                case 3 -> deleteFolder(sel.file);
                            }
                        }
                        return; // never propagate to launch on action-icon area
                    }
                }

                if (e.getClickCount() == 2) launch(sel);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                int idx = list.locationToIndex(e.getPoint());
                boolean overIcon = idx >= 0
                        && listModel.getElementAt(idx).type != EntryType.SCRIPT
                        && hitActionIcon(e.getPoint(), list, idx) >= 0;
                list.setCursor(overIcon
                        ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        : Cursor.getDefaultCursor());
            }

            @Override public void mousePressed(MouseEvent e)  { handlePopup(e); }
            @Override public void mouseReleased(MouseEvent e) { handlePopup(e); }
        };
        list.addMouseListener(mouseHandler);
        list.addMouseMotionListener(mouseHandler);

        // Enter key to launch
        list.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "launch");
        list.getActionMap().put("launch", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                LaunchEntry sel = list.getSelectedValue();
                if (sel != null) launch(sel);
            }
        });

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0,
                new Color(0xCC, 0xCC, 0xCC)));

        // ── Legend ───────────────────────────────────────────────────────────
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        legend.setBackground(new Color(0xF0, 0xF0, 0xF0));
        legend.setBorder(new MatteBorder(1, 0, 0, 0, new Color(0xCC, 0xCC, 0xCC)));

        legend.add(coloredLabel("Scripts", new Color(0x1A, 0x5F, 0x7A)));
        legend.add(coloredLabel("Application folders", new Color(0x2E, 0x6B, 0x2E)));
        legend.add(coloredLabel("Folders", new Color(0x66, 0x55, 0x44)));

        searchLabel = new JLabel();
        searchLabel.setForeground(new Color(0x00, 0x50, 0xA0));
        searchLabel.setFont(searchLabel.getFont().deriveFont(Font.BOLD | Font.ITALIC, 11f));
        searchLabel.setHorizontalAlignment(JLabel.CENTER);

        hintLabel = new JLabel(listModel.size() + " entries   |   "
                + "Double-click or Enter to launch");
        hintLabel.setForeground(Color.GRAY);
        hintLabel.setFont(hintLabel.getFont().deriveFont(11f));

        JPanel south = new JPanel(new BorderLayout());
        south.add(legend, BorderLayout.WEST);
        south.add(searchLabel, BorderLayout.CENTER);
        south.add(hintLabel, BorderLayout.EAST);
        south.setBorder(new EmptyBorder(0, 10, 0, 10));
        south.setBackground(new Color(0xF0, 0xF0, 0xF0));

        add(header, BorderLayout.NORTH);
        add(scroll,  BorderLayout.CENTER);
        add(south,   BorderLayout.SOUTH);

        // ── Type-to-search: capture keystrokes while this window is focused ──
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(event -> {
            if (event.getID() != KeyEvent.KEY_TYPED) return false;
            Window focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
            if (focused != Launcher.this) return false;
            char c = event.getKeyChar();
            if (c == KeyEvent.CHAR_UNDEFINED) return false;
            if (c == '\u001b') {                    // Escape – clear filter
                clearSearch();
            } else if (c == '\b') {                 // Backspace – delete last char
                if (!searchQuery.isEmpty()) {
                    searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                    applyFilter();
                }
            } else if (c == '\r' || c == '\n') {    // Enter – let existing handler fire
                return false;
            } else if (c >= 32) {                   // Any other printable character
                searchQuery += c;
                applyFilter();
            }
            return false;
        });
    }

    // =========================================================================
    //  System tray
    // =========================================================================

    /**
     * Installs a system-tray icon and starts the window hidden.
     * Double-clicking the tray icon or choosing "Show / Hide" toggles visibility.
     * Called when {@code --minimized} is passed on the command line.
     * Falls back to a normal visible window if the system tray is not supported.
     */
    void setupTray() {
        if (!SystemTray.isSupported()) {
            setVisible(true);
            return;
        }

        // Load apps.png as the tray icon; fall back to programmatic icon if unavailable
        Image trayImage;
        ImageIcon trayIconRes = loadScaledIcon("apps.png", 16, 16);
        if (trayIconRes != null) {
            trayImage = trayIconRes.getImage();
        } else {
            // Programmatic fallback: blue rounded square with white "L"
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

        TrayIcon trayIcon = new TrayIcon(trayImage,
                "Launcher – " + baseFolder.getName(), popup);
        trayIcon.setImageAutoSize(true);
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    toggleVisibility();
                }
            }
        });

        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException ex) {
            setVisible(true);
            return;
        }

        // Clicking the window's close button hides to tray instead of exiting
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { setVisible(false); }
        });

        // Start hidden in the tray
        setVisible(false);
    }

    /** Toggles the main window between visible (restored) and hidden. */
    private void toggleVisibility() {
        if (isVisible()) {
            setVisible(false);
        } else {
            setVisible(true);
            setExtendedState(NORMAL);
            toFront();
            requestFocus();
        }
    }

    /** Small coloured label for the legend. */
    private static JLabel coloredLabel(String text, Color color) {
        JLabel lbl = new JLabel("■ " + text);
        lbl.setForeground(color);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 11f));
        return lbl;
    }

    // =========================================================================
    //  Icon loading
    // =========================================================================

    /**
     * Loads a PNG resource from the classpath, scales it to {@code w × h} pixels,
     * and returns it as an {@link ImageIcon}.  Returns {@code null} if the resource
     * cannot be found or read (callers should fall back to text labels).
     */
    static ImageIcon loadScaledIcon(String resourceName, int w, int h) {
        try (InputStream is = Launcher.class.getResourceAsStream("/" + resourceName)) {
            if (is == null) return null;
            BufferedImage raw = ImageIO.read(is);
            if (raw == null) return null;
            Image scaled = raw.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (IOException e) {
            return null;
        }
    }

    // =========================================================================
    //  Action-icon hit testing
    // =========================================================================

    /**
     * Returns the index (0 = Explorer, 1 = VS Code, 2 = Copy, 3 = Delete) of the
     * action icon at the given point in the list coordinate space, or -1 if the
     * point does not fall on any action icon.
     *
     * The action bar occupies the rightmost {@link EntryCellRenderer#ACT_BAR_W}
     * pixels of a cell.  Inside the bar, FlowLayout places icons as:
     *   gap | icon0 | gap | icon1 | gap | icon2 | gap | icon3 | gap
     */
    private static int hitActionIcon(Point p, JList<LaunchEntry> list, int idx) {
        Rectangle cell = list.getCellBounds(idx, idx);
        if (cell == null) return -1;
        int xInCell = p.x - cell.x;
        int actionBarStart = cell.width - EntryCellRenderer.ACT_BAR_W;
        if (xInCell < actionBarStart) return -1;
        int xInBar = xInCell - actionBarStart;
        for (int i = 0; i < 4; i++) {
            int iconStart = EntryCellRenderer.ACT_HGAP
                    + i * (EntryCellRenderer.ACT_W + EntryCellRenderer.ACT_HGAP);
            if (xInBar >= iconStart && xInBar < iconStart + EntryCellRenderer.ACT_W) {
                return i;
            }
        }
        return -1;
    }

    // =========================================================================
    //  Entry loading
    // =========================================================================

    /**
     * Scans the base folder one level deep and returns:
     *   scripts first (sorted A-Z), then application folders (sorted A-Z),
     *   then plain folders (sorted A-Z).
     */
    private List<LaunchEntry> loadEntries() {
        List<LaunchEntry> scripts = new ArrayList<>();
        List<LaunchEntry> apps    = new ArrayList<>();
        List<LaunchEntry> plain   = new ArrayList<>();

        File[] children = baseFolder.listFiles();
        if (children != null) {
            Arrays.sort(children,
                    Comparator.comparing(f -> f.getName().toLowerCase(Locale.ROOT)));
            for (File f : children) {
                if (f.isDirectory()) {
                    File iconSrc = findAppIconSource(f);
                    if (iconSrc != null) {
                        apps.add(new LaunchEntry(f, EntryType.APP_FOLDER, iconSrc));
                    } else {
                        plain.add(new LaunchEntry(f, EntryType.PLAIN_FOLDER));
                    }
                } else if (isScript(f)) {
                    scripts.add(new LaunchEntry(f, EntryType.SCRIPT));
                }
            }
        }

        List<LaunchEntry> all = new ArrayList<>(scripts);
        all.addAll(apps);
        all.addAll(plain);
        return all;
    }

    /**
     * If the directory qualifies as an application folder, returns the file to use
     * as the icon source (the first .lnk at the top level, or the fallback exe).
     * Returns null if the directory is a plain folder.
     */
    private static File findAppIconSource(File dir) {
        File[] children = dir.listFiles();
        if (children != null) {
            for (File f : children) {
                if (f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".lnk")) {
                    return f;
                }
            }
        }
        File fallback = new File(dir, FALLBACK_EXE);
        return fallback.isFile() ? fallback : null;
    }

    /** Returns true for files with a recognised script extension. */
    private static boolean isScript(File f) {
        String n = f.getName().toLowerCase(Locale.ROOT);
        return n.endsWith(".bat") || n.endsWith(".cmd")
            || n.endsWith(".ps1") || n.endsWith(".vbs")
            || n.endsWith(".sh")  || n.endsWith(".js");
    }

    // =========================================================================
    //  Launching
    // =========================================================================

    private void launch(LaunchEntry entry) {
        try {
            if (entry.type == EntryType.SCRIPT) {
                launchScript(entry.file);
            } else if (entry.type == EntryType.APP_FOLDER) {
                launchAppFolder(entry.file);
            } else {
                // PLAIN_FOLDER – no launcher found; open in File Explorer
                openInExplorer(entry.file);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to launch:\n" + ex.getMessage(),
                    "Launcher Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Execute a script in an appropriate interpreter / shell window.
     *
     *  .bat / .cmd  →  new console window  (cmd /c start)
     *  .ps1         →  PowerShell  (-ExecutionPolicy Bypass -NoExit)
     *  .vbs / .js   →  Windows Script Host  (wscript)
     *  .sh          →  bash
     */
    private static void launchScript(File script) throws IOException {
        String name = script.getName().toLowerCase(Locale.ROOT);
        ProcessBuilder pb;

        if (name.endsWith(".ps1")) {
            // PowerShell – keep window open after script finishes
            pb = new ProcessBuilder(
                    "powershell", "-ExecutionPolicy", "Bypass",
                    "-NoExit", "-File", script.getAbsolutePath());

        } else if (name.endsWith(".vbs") || name.endsWith(".js")) {
            // Windows Script Host
            pb = new ProcessBuilder("wscript", script.getAbsolutePath());

        } else if (name.endsWith(".sh")) {
            // Bash (WSL or Git Bash must be on PATH)
            pb = new ProcessBuilder("bash", script.getAbsolutePath());

        } else {
            // .bat / .cmd  – open a new console window and run the script
            pb = new ProcessBuilder(
                    "cmd", "/c", "start", script.getName(),
                    "cmd", "/k", script.getAbsolutePath());
        }

        pb.directory(script.getParentFile());
        pb.start();
    }

    /**
     * Launch an application folder:
     *   1. Execute the first .lnk shortcut found at the folder's top level, or
     *   2. Execute <appFolder>\basis\sys\win\bin\dsc_StartPlm.exe.
     */
    private void launchAppFolder(File appFolder) throws IOException {
        // Step 1 – look for a Windows shortcut (.lnk) at the top level
        File[] children = appFolder.listFiles();
        if (children != null) {
            for (File f : children) {
                if (f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".lnk")) {
                    // Delegate to the Windows shell so the shortcut is resolved
                    new ProcessBuilder("cmd", "/c", "start", "", f.getAbsolutePath())
                            .directory(appFolder)
                            .start();
                    return;
                }
            }
        }

        // Step 2 – fallback: dsc_StartPlm.exe inside the application folder
        File fallback = new File(appFolder, FALLBACK_EXE);
        if (fallback.isFile()) {
            new ProcessBuilder(fallback.getAbsolutePath())
                    .directory(appFolder)
                    .start();
        } else {
            JOptionPane.showMessageDialog(this,
                    "<html>No shortcut (.lnk) found in:<br>&nbsp;&nbsp;<b>"
                    + appFolder.getAbsolutePath()
                    + "</b><br><br>Fallback executable not found at:<br>&nbsp;&nbsp;<b>"
                    + fallback.getAbsolutePath() + "</b></html>",
                    "Launcher – Nothing to start", JOptionPane.WARNING_MESSAGE);
        }
    }

    /** Opens an application folder in Windows File Explorer. */
    private void openInExplorer(File folder) {
        try {
            Desktop.getDesktop().open(folder);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not open File Explorer:\n" + ex.getMessage(),
                    "Launcher Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Opens an application folder in VS Code (requires 'code' on PATH). */
    private void openInVSCode(File folder) {
        try {
            // 'code' is a .cmd script, so it must be invoked via cmd /c;
            // ProcessBuilder cannot resolve .cmd extensions on its own.
            new ProcessBuilder("cmd", "/c", "code", folder.getAbsolutePath())
                    .directory(folder)
                    .start();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not open VS Code.\n"
                    + "Make sure 'code' is on your PATH.\n\n" + ex.getMessage(),
                    "Launcher Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Copies the selected application folder using robocopy into the active launcher folder.
     * Prompts the user for a new name and validates that it doesn't already exist.
     */
    private void copyWithRobocopy(File srcFolder) {
        String newName = JOptionPane.showInputDialog(this,
                "Enter new folder name for the copy:",
                srcFolder.getName() + "_copy");

        if (newName == null || newName.trim().isEmpty()) {
            return;
        }

        newName = newName.trim();

        // Check if name already exists in the active launcher folder
        File destPath = new File(baseFolder, newName);
        if (destPath.exists()) {
            JOptionPane.showMessageDialog(this,
                    "<html>A folder with the name <b>" + newName + "</b> already exists<br>"
                    + "in the active launcher folder:<br><br><b>" + baseFolder.getAbsolutePath() + "</b></html>",
                    "Name Already Exists", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // robocopy source destination /MIR (mirror - copies recursively)
            ProcessBuilder pb = new ProcessBuilder(
                    "robocopy", srcFolder.getAbsolutePath(), destPath.getAbsolutePath(), "/MIR");
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Show output; auto-close window and refresh list when done
            showProcessOutput(process, "Robocopy: " + srcFolder.getName() + " → " + newName, this::refreshList);

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not start robocopy:\n" + ex.getMessage(),
                    "Launcher Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Deletes a folder and all its contents after confirmation.
     * Refreshes the list immediately after successful deletion.
     */
    private void deleteFolder(File folder) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "<html>Permanently delete:<br><b>" + folder.getAbsolutePath() + "</b><br><br>"
                + "This cannot be undone.</html>",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            deleteDirectory(folder);
            refreshList();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not delete folder:\n" + ex.getMessage(),
                    "Launcher Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Recursively deletes a directory and all its contents.
     */
    private static void deleteDirectory(File dir) throws IOException {
        if (!dir.exists()) {
            return;
        }

        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteDirectory(child);
                }
            }
        }

        if (!dir.delete()) {
            throw new IOException("Failed to delete: " + dir.getAbsolutePath());
        }
    }

    /**
     * Opens a dialog to enter an SVN repository URL and checks it out
     * into the application folder (creating a subfolder with the repo name).
     */
    private void svnCheckout(File targetFolder) {
        String url = JOptionPane.showInputDialog(this,
                "Enter SVN repository URL:",
                "SVN Checkout",
                JOptionPane.QUESTION_MESSAGE);

        if (url == null || url.trim().isEmpty()) {
            return;
        }

        url = url.trim();

        try {
            // Extract repository name from URL (last part)
            String repoName = url.replaceAll(".*/+", "").replaceAll("\\..*", "");
            if (repoName.isEmpty()) {
                repoName = "checkout";
            }

            File checkoutDir = new File(targetFolder, repoName);

            ProcessBuilder pb = new ProcessBuilder("svn", "checkout", url, checkoutDir.getAbsolutePath());
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Show output in a new window (keep open; user can close manually)
            showProcessOutput(process, "SVN Checkout: " + url, null);

            // After SVN completes, offer to refresh the list
            new Thread(() -> {
                try {
                    process.waitFor();
                    SwingUtilities.invokeLater(() -> {
                        int refresh = JOptionPane.showConfirmDialog(this,
                                "Checkout completed. Refresh the file list?",
                                "SVN Checkout", JOptionPane.YES_NO_OPTION);
                        if (refresh == JOptionPane.YES_OPTION) {
                            setTitle("Launcher  –  " + baseFolder.getAbsolutePath());
                        }
                    });
                } catch (InterruptedException ignored) { }
            }).start();

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not start SVN:\n" + ex.getMessage() + "\n\n"
                    + "Make sure 'svn' is installed and on your PATH.",
                    "Launcher Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Displays the output of a running process in a new window.
     * Updates in real-time as the process runs.
     *
     * @param onComplete  if non-null, the window is closed automatically when the
     *                    process finishes and this callback is invoked on the EDT.
     *                    If null, a "Process completed." line is appended instead.
     */
    private static void showProcessOutput(Process process, String title, Runnable onComplete) {
        SwingUtilities.invokeLater(() -> {
            JFrame outputFrame = new JFrame(title);
            outputFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            outputFrame.setSize(800, 600);
            outputFrame.setLocationRelativeTo(null);

            JList<String> outputList = new JList<>(new DefaultListModel<>());
            outputList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
            JScrollPane scroll = new JScrollPane(outputList);
            outputFrame.add(scroll, BorderLayout.CENTER);

            outputFrame.setVisible(true);

            DefaultListModel<String> model = (DefaultListModel<String>) outputList.getModel();

            new Thread(() -> {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        final String output = line;
                        SwingUtilities.invokeLater(() -> {
                            model.addElement(output);
                            outputList.ensureIndexIsVisible(model.getSize() - 1);
                        });
                    }
                } catch (IOException ignored) { }

                SwingUtilities.invokeLater(() -> {
                    if (onComplete != null) {
                        outputFrame.dispose();
                        onComplete.run();
                    } else {
                        model.addElement("");
                        model.addElement("Process completed.");
                        outputList.ensureIndexIsVisible(model.getSize() - 1);
                    }
                });
            }).start();
        });
    }

    // =========================================================================
    //  Entry point
    // =========================================================================

    public static void main(String[] args) {

        // Native look and feel for a clean Windows appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { /* fall back to Metal */ }

        // ── Parse CLI flags ──────────────────────────────────────────────────
        // Usage:
        //   java Launcher [<rootFolder>] [--minimized]
        //
        //   --minimized   Start hidden in the system tray.
        //                 Double-click the tray icon or use its menu to show/exit.
        boolean startMinimized = false;
        String  folderArg      = null;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("--minimized") || arg.equalsIgnoreCase("-minimized")) {
                startMinimized = true;
            } else if (folderArg == null) {
                folderArg = arg;
            }
        }

        // Determine the root folder
        final File folder;

        if (folderArg == null) {
            // No argument supplied → open a folder-chooser dialog
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select launcher root folder");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);
            if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
                System.exit(0);
            }
            folder = chooser.getSelectedFile();
        } else {
            folder = new File(folderArg);
        }

        if (!folder.isDirectory()) {
            JOptionPane.showMessageDialog(null,
                    "Not a valid directory:\n" + folder.getAbsolutePath(),
                    "Launcher", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        final boolean minimized = startMinimized;
        SwingUtilities.invokeLater(() -> {
            Launcher launcher = new Launcher(folder, minimized);
            if (minimized) {
                launcher.setupTray();   // starts hidden; tray icon allows show/exit
            } else {
                launcher.setVisible(true);
            }
        });
    }
}
