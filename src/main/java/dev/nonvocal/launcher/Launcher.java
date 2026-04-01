package dev.nonvocal.launcher;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Launcher – self-contained Java Swing application.
 * <p>
 * Usage:
 * java Launcher <rootFolder>
 * java Launcher              (opens a folder-chooser dialog)
 * <p>
 * The root folder is scanned at the FIRST level only:
 * • Files with a recognised script extension are listed as scripts.
 * • Sub-directories are listed as application folders.
 * <p>
 * On double-click / Enter:
 * Script      → executed in an appropriate interpreter/shell.
 * App folder  → 1) first .lnk shortcut found at the folder's top level is
 * launched via the Windows shell, or
 * 2) <appFolder>\basis\sys\win\bin\dsc_StartPlm.exe is run.
 * <p>
 * Compile:  javac Launcher.java
 * Run:      java  Launcher  C:\path\to\rootFolder
 */
public class Launcher extends JFrame
{

    private static final long serialVersionUID = 1L;

    /**
     * Relative path of the fallback executable inside an application folder.
     */
    private static final String FALLBACK_EXE = "basis\\sys\\win\\bin\\dsc_StartPlm.exe";

    // =========================================================================
    //  Data model
    // =========================================================================

    enum EntryType
    {SCRIPT, APP_FOLDER, PLAIN_FOLDER}

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
    //  Configuration
    // =========================================================================

    /**
     * Three-level configuration:
     * <ol>
     *   <li>Global:   {@code %APPDATA%\nvLauncher\config.json}</li>
     *   <li>Instance: {@code %APPDATA%\nvLauncher\{launcherId}\config.json}</li>
     *   <li>Explicit: path supplied via {@code --config=<path>}</li>
     * </ol>
     * Each level overrides only the fields that are explicitly present.
     * All fields are nullable; {@code null} means "not set at this level".
     * Call {@link #withDefaults()} on the final merged config to fill gaps.
     */
    record LauncherConfig(
            String       rootFolder,
            Boolean      startMinimized,
            Integer      windowWidth,
            Integer      windowHeight,
            List<String> priorityList)
    {
        // ── Static paths ───────────────────────────────────────────────────

        /** Base config directory: {@code %APPDATA%\nvLauncher} */
        static final File CONFIG_DIR;
        static
        {
            String appData = System.getenv("APPDATA");
            if (appData == null) appData = System.getProperty("user.home");
            CONFIG_DIR = new File(appData, "nvLauncher");
        }

        static File globalConfigFile()
        {
            return new File(CONFIG_DIR, "config.json");
        }

        static File instanceConfigFile(String launcherId)
        {
            return new File(new File(CONFIG_DIR, launcherId), "config.json");
        }

        // ── Factory methods ────────────────────────────────────────────────

        /** All fields null – represents "nothing set at this level". */
        static LauncherConfig empty()
        {
            return new LauncherConfig(null, null, null, null, null);
        }

        /** Hardcoded application defaults (all fields non-null). */
        static LauncherConfig defaults()
        {
            return new LauncherConfig(null, false, 560, 680, null);
        }

        /**
         * Loads a config from {@code file}.
         * Returns {@link #empty()} if the file does not exist or cannot be read.
         * Fields missing in the file are left null.
         */
        static LauncherConfig loadFile(File file)
        {
            if (!file.exists()) return empty();
            try
            {
                return parse(Files.readString(file.toPath()));
            }
            catch (IOException e)
            {
                return empty();
            }
        }

        // ── Merging ────────────────────────────────────────────────────────

        /**
         * Returns a new config in which every non-null field of {@code this}
         * overrides the corresponding field from {@code base}.
         * Use as: {@code override.mergeOver(base)}.
         */
        LauncherConfig mergeOver(LauncherConfig base)
        {
            return new LauncherConfig(
                    rootFolder     != null ? rootFolder     : base.rootFolder,
                    startMinimized != null ? startMinimized : base.startMinimized,
                    windowWidth    != null ? windowWidth    : base.windowWidth,
                    windowHeight   != null ? windowHeight   : base.windowHeight,
                    priorityList   != null ? priorityList   : base.priorityList);
        }

        /**
         * Returns a new config where every field that is still null is filled
         * with the hardcoded application default.
         */
        LauncherConfig withDefaults()
        {
            return new LauncherConfig(
                    rootFolder,
                    startMinimized != null ? startMinimized : false,
                    windowWidth    != null ? windowWidth    : 560,
                    windowHeight   != null ? windowHeight   : 680,
                    priorityList);
        }

        // ── Persistence ────────────────────────────────────────────────────

        /**
         * Saves this config to {@code file}, creating parent directories as needed.
         * Only non-null fields are written; silently ignores I/O errors.
         */
        void save(File file)
        {
            try
            {
                file.getParentFile().mkdirs();
                Files.writeString(file.toPath(), toJson());
            }
            catch (IOException ignored) {}
        }

        // ── Serialisation ──────────────────────────────────────────────────

        private String toJson()
        {
            List<String> lines = new ArrayList<>();
            if (rootFolder     != null) lines.add("  \"rootFolder\": "     + jsonStr(rootFolder));
            if (startMinimized != null) lines.add("  \"startMinimized\": " + startMinimized);
            if (windowWidth    != null) lines.add("  \"windowWidth\": "    + windowWidth);
            if (windowHeight   != null) lines.add("  \"windowHeight\": "   + windowHeight);
            if (priorityList   != null && !priorityList.isEmpty())
            {
                StringBuilder sb = new StringBuilder("  \"priorityList\": [\n");
                for (int i = 0; i < priorityList.size(); i++)
                {
                    sb.append("    ").append(jsonStr(priorityList.get(i)));
                    if (i < priorityList.size() - 1) sb.append(",");
                    sb.append("\n");
                }
                sb.append("  ]");
                lines.add(sb.toString());
            }
            return "{\n" + String.join(",\n", lines) + "\n}";
        }

        private static String jsonStr(String s)
        {
            if (s == null) return "null";
            return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }

        // ── Deserialisation (no external library) ──────────────────────────

        private static LauncherConfig parse(String json)
        {
            return new LauncherConfig(
                    parseStr    (json, "rootFolder"),
                    parseBool   (json, "startMinimized"),
                    parseInt    (json, "windowWidth"),
                    parseInt    (json, "windowHeight"),
                    parseStrList(json, "priorityList"));
        }

        private static String parseStr(String json, String key)
        {
            Matcher m = Pattern.compile(
                    "\"" + key + "\"\\s*:\\s*(?:null|\"((?:[^\"\\\\]|\\\\.)*)\")").matcher(json);
            if (!m.find()) return null;
            String g = m.group(1);
            return g == null ? null : g.replace("\\\\", "\\").replace("\\\"", "\"");
        }

        private static Boolean parseBool(String json, String key)
        {
            Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(true|false)").matcher(json);
            return m.find() ? Boolean.parseBoolean(m.group(1)) : null;
        }

        private static Integer parseInt(String json, String key)
        {
            Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?\\d+)").matcher(json);
            return m.find() ? Integer.parseInt(m.group(1)) : null;
        }

        private static List<String> parseStrList(String json, String key)
        {
            Matcher m = Pattern.compile(
                    "\"" + key + "\"\\s*:\\s*\\[([^\\]]*?)\\]",
                    Pattern.DOTALL).matcher(json);
            if (!m.find()) return null;
            String inner = m.group(1).trim();
            if (inner.isEmpty()) return new ArrayList<>();
            List<String> result = new ArrayList<>();
            Matcher em = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(inner);
            while (em.find())
            {
                result.add(em.group(1).replace("\\\\", "\\").replace("\\\"", "\""));
            }
            return result;
        }
    }

    // =========================================================================
    //  Cell renderer
    // =========================================================================

    static final class EntryCellRenderer extends JPanel implements ListCellRenderer<LaunchEntry>
    {

        private static final long serialVersionUID = 1L;

        /**
         * Width of each action-icon button (px).
         */
        static final int ACT_W = 22;
        /**
         * FlowLayout horizontal gap around / between action icons (px).
         */
        static final int ACT_HGAP = 3;
        /**
         * Total width of the action bar: 5 gaps + 4 icons = 5*3 + 4*22 = 103 px.
         */
        static final int ACT_BAR_W = 5 * ACT_HGAP + 4 * ACT_W;

        // Row colours
        private static final Color ROW_EVEN = new Color(0xF4, 0xF6, 0xF8);
        private static final Color ROW_ODD = Color.WHITE;
        // Name foreground colours by entry type
        private static final Color FG_SCRIPT = new Color(0x1A, 0x5F, 0x7A); // dark teal
        private static final Color FG_FOLDER = new Color(0x2E, 0x6B, 0x2E); // dark green
        private static final Color FG_PLAIN = new Color(0x66, 0x55, 0x44); // warm dark gray
        // Selection
        private static final Color SEL_BG = new Color(0x00, 0x78, 0xD7); // Windows blue
        // Action icon colours – normal
        private static final Color ACT_FG = new Color(0x33, 0x55, 0x99);
        private static final Color ACT_DEL = new Color(0xAA, 0x22, 0x22);
        private static final Color ACT_BG = new Color(0xE8, 0xEA, 0xF4);
        private static final Color ACT_BORD = new Color(0xBB, 0xBB, 0xCC);
        // Action icon colours – selected row
        private static final Color SEL_ACT_BG = new Color(0x40, 0x90, 0xD7);
        private static final Color SEL_ACT_BORD = new Color(0x80, 0xB8, 0xFF);

        private static final Font CELL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
        private static final Font ACT_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 9);

        /**
         * Labels for the four action icons: Explorer · VS Code · Copy · Delete.
         */
        private static final String[] ACT_TEXT = {"E", "VS", "C", "\u2715"};
        static final String[] ACT_TIPS = {
                "Open in File Explorer",
                "Open in VS Code",
                "Copy with Robocopy\u2026",
                "Delete"
        };

        /**
         * PNG icons for the four action buttons (null → fall back to ACT_TEXT).
         */
        private static final ImageIcon[] ACT_ICON_IMGS = {
                Launcher.loadScaledIcon("folder.png", 14, 14),  // 0 – Explorer
                Launcher.loadScaledIcon("edit-document.png", 14, 14),  // 1 – VS Code
                Launcher.loadScaledIcon("copy.png", 14, 14),  // 2 – Robocopy
                Launcher.loadScaledIcon("bin.png", 14, 14),  // 3 – Delete
        };

        private final JLabel nameLabel = new JLabel();
        private final JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, ACT_HGAP, 0));
        private final JLabel[] actIcons = new JLabel[4];

        private transient final FileSystemView fsv = FileSystemView.getFileSystemView();

        EntryCellRenderer()
        {
            setLayout(new BorderLayout());
            setOpaque(true);

            nameLabel.setFont(CELL_FONT);
            nameLabel.setBorder(new EmptyBorder(5, 8, 5, 8));
            nameLabel.setOpaque(false);
            add(nameLabel, BorderLayout.CENTER);

            actionBar.setOpaque(false);
            // EmptyBorder top/bottom = (36 - 18) / 2 = 9 px → centres 18 px icons in 36 px row
            actionBar.setBorder(new EmptyBorder(9, 0, 9, 0));

            for (int i = 0; i < 4; i++)
            {
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
                boolean selected, boolean cellHasFocus)
        {

            nameLabel.setText("  " + e.file.getName());
            nameLabel.setToolTipText(e.file.getAbsolutePath());

            // For app folders use the icon of the .lnk / exe
            File iconSrc = (e.iconFile != null) ? e.iconFile : e.file;
            try
            {
                nameLabel.setIcon(fsv.getSystemIcon(iconSrc));
            } catch (Exception ignored)
            {
                nameLabel.setIcon(null);
            }

            // Action icons shown only for folder entries (not scripts)
            actionBar.setVisible(e.type != EntryType.SCRIPT);

            if (selected)
            {
                setBackground(SEL_BG);
                nameLabel.setForeground(Color.WHITE);
                for (JLabel icon : actIcons)
                {
                    icon.setBackground(SEL_ACT_BG);
                    icon.setForeground(Color.WHITE);
                    icon.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(SEL_ACT_BORD, 1),
                            BorderFactory.createEmptyBorder(1, 3, 1, 3)));
                }
            }
            else
            {
                setBackground(index % 2 == 0 ? ROW_EVEN : ROW_ODD);
                nameLabel.setForeground(e.type == EntryType.SCRIPT ? FG_SCRIPT
                        : e.type == EntryType.APP_FOLDER ? FG_FOLDER
                          : FG_PLAIN);
                for (int i = 0; i < 4; i++)
                {
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
    private LauncherConfig config;
    private final String launcherId;
    private DefaultListModel<LaunchEntry> listModel;
    private JList<LaunchEntry> list;
    private JLabel hintLabel;
    private JLabel searchLabel;
    private transient String searchQuery = "";
    private transient final List<LaunchEntry> allEntries = new ArrayList<>();

    Launcher(File baseFolder, LauncherConfig config, String launcherId)
    {
        this.baseFolder = baseFolder;
        this.config = config;
        this.launcherId = launcherId;
        buildUI();
    }

    /**
     * Reloads all entries from disk and re-applies the current search filter.
     */
    private void refreshList()
    {
        allEntries.clear();
        allEntries.addAll(loadEntries());
        applyFilter();
    }

    /**
     * Filters listModel to entries whose name contains searchQuery (case-insensitive).
     * Drag-and-drop reordering is disabled while a filter is active.
     */
    private void applyFilter()
    {
        listModel.clear();
        String q = searchQuery.toLowerCase(Locale.ROOT);
        for (LaunchEntry entry : allEntries)
        {
            if (q.isEmpty() || entry.file.getName().toLowerCase(Locale.ROOT).contains(q))
            {
                listModel.addElement(entry);
            }
        }
        if (searchQuery.isEmpty())
        {
            searchLabel.setText("");
            hintLabel.setText(listModel.size() + " entries   |   Double-click or Enter to launch");
        }
        else
        {
            searchLabel.setText("  Filter: " + searchQuery + "\u258C");
            hintLabel.setText(listModel.size() + " of " + allEntries.size() + "   |   Esc to clear  ·  DnD disabled while filtering");
        }
        // DnD reordering only makes sense on the unfiltered full list
        if (list != null) list.setDragEnabled(searchQuery.isEmpty());
    }

    /**
     * Clears the search query and shows all entries.
     */
    private void clearSearch()
    {
        searchQuery = "";
        applyFilter();
    }

    private void buildUI()
    {
        setTitle("Launcher  –  " + baseFolder.getAbsolutePath());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(config.windowWidth(), config.windowHeight());
        setMinimumSize(new Dimension(320, 200));
        setLocationRelativeTo(null);

        // Save config (with current window size) when the window is closed
        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                saveConfig();
                System.exit(0);
            }
        });

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

        JList<LaunchEntry> list = this.list = new JList<>(listModel)
        {
            @Override
            public String getToolTipText(MouseEvent e)
            {
                int idx = locationToIndex(e.getPoint());
                if (idx >= 0 && listModel.getElementAt(idx).type != EntryType.SCRIPT)
                {
                    int actionIdx = hitActionIcon(e.getPoint(), this, idx);
                    if (actionIdx >= 0)
                    {
                        return EntryCellRenderer.ACT_TIPS[actionIdx];
                    }
                }
                return null;
            }
        };
        ToolTipManager.sharedInstance().registerComponent(list);
        list.setCellRenderer(new EntryCellRenderer());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(36);

        // Click-to-launch, action-icon clicks, right-click context menu
        MouseAdapter mouseHandler = new MouseAdapter()
        {
            private void handlePopup(MouseEvent e)
            {
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
            public void mouseClicked(MouseEvent e)
            {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                int idx = list.locationToIndex(e.getPoint());
                if (idx < 0) return;
                LaunchEntry sel = listModel.getElementAt(idx);

                // Check if a rendered action icon was clicked (folder entries only)
                if (sel.type != EntryType.SCRIPT)
                {
                    int actionIdx = hitActionIcon(e.getPoint(), list, idx);
                    if (actionIdx >= 0)
                    {
                        if (e.getClickCount() == 1)
                        {
                            switch (actionIdx)
                            {
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
            public void mouseMoved(MouseEvent e)
            {
                int idx = list.locationToIndex(e.getPoint());
                boolean overIcon = idx >= 0
                        && listModel.getElementAt(idx).type != EntryType.SCRIPT
                        && hitActionIcon(e.getPoint(), list, idx) >= 0;
                list.setCursor(overIcon
                        ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        : Cursor.getDefaultCursor());
            }

            @Override
            public void mousePressed(MouseEvent e)
            {
                handlePopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e)
            {
                handlePopup(e);
            }
        };
        list.addMouseListener(mouseHandler);
        list.addMouseMotionListener(mouseHandler);

        // ── Drag-and-drop reordering ─────────────────────────────────────────
        list.setDragEnabled(true);
        list.setDropMode(DropMode.INSERT);
        list.setTransferHandler(new TransferHandler()
        {
            private final DataFlavor entryFlavor =
                    new DataFlavor(Integer.class, "Row Index");
            private int dragIndex = -1;

            @Override
            public int getSourceActions(JComponent c)
            {
                return MOVE;
            }

            @Override
            protected Transferable createTransferable(JComponent c)
            {
                dragIndex = list.getSelectedIndex();
                final int idx = dragIndex;
                return new Transferable()
                {
                    @Override
                    public DataFlavor[] getTransferDataFlavors()
                    {
                        return new DataFlavor[]{entryFlavor};
                    }

                    @Override
                    public boolean isDataFlavorSupported(DataFlavor flavor)
                    {
                        return entryFlavor.equals(flavor);
                    }

                    @Override
                    public Object getTransferData(DataFlavor flavor)
                    {
                        return idx;
                    }
                };
            }

            @Override
            public boolean canImport(TransferSupport support)
            {
                // Only allow reordering when no search filter is active
                return support.isDrop()
                        && searchQuery.isEmpty()
                        && support.isDataFlavorSupported(entryFlavor);
            }

            @Override
            public boolean importData(TransferSupport support)
            {
                if (!canImport(support)) return false;
                try
                {
                    JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
                    int dropIndex = dl.getIndex();
                    int src = dragIndex;
                    if (src < 0 || src >= listModel.getSize()) return false;

                    LaunchEntry moved = listModel.getElementAt(src);
                    listModel.remove(src);

                    int target = dropIndex;
                    if (src < dropIndex) target--;   // adjust for removed element
                    if (target < 0) target = 0;
                    if (target > listModel.getSize()) target = listModel.getSize();
                    listModel.add(target, moved);

                    // Rebuild allEntries to match the new list order
                    allEntries.clear();
                    for (int i = 0; i < listModel.getSize(); i++)
                    {
                        allEntries.add(listModel.getElementAt(i));
                    }

                    savePriorityList();
                    list.setSelectedIndex(target);
                    return true;
                }
                catch (Exception ex)
                {
                    return false;
                }
            }

            @Override
            protected void exportDone(JComponent source, Transferable data, int action)
            {
                dragIndex = -1;
            }
        });

        // Enter key to launch
        list.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "launch");
        list.getActionMap().put("launch", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
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

        // ── Toolbar ──────────────────────────────────────────────────────────
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBackground(new Color(0xF0, 0xF2, 0xF5));
        toolbar.setBorder(new MatteBorder(0, 0, 1, 0, new Color(0xCC, 0xCC, 0xCC)));

        ImageIcon svnIcon = loadScaledIcon("apps-add.png", 20, 20);
        JButton svnButton = svnIcon != null
                ? new JButton(svnIcon)
                : new JButton("SVN");
        svnButton.setToolTipText("SVN Checkout – check out a repository into the selected folder");
        svnButton.setFocusPainted(false);
        svnButton.addActionListener(ev ->
        {
            LaunchEntry sel = list.getSelectedValue();
            File target = (sel != null && sel.type != EntryType.SCRIPT) ? sel.file : baseFolder;
            svnCheckout(target);
        });
        toolbar.add(svnButton);

        // Push the settings button to the right
        toolbar.add(Box.createHorizontalGlue());

        ImageIcon settingsIcon = loadScaledIcon("setting.png", 20, 20);
        JButton settingsButton = settingsIcon != null
                ? new JButton(settingsIcon)
                : new JButton("\u2699");
        settingsButton.setToolTipText("Settings");
        settingsButton.setFocusPainted(false);
        settingsButton.addActionListener(ev -> showSettings());
        toolbar.add(settingsButton);

        // ── Combine header + toolbar in a single north area ───────────────────
        JPanel topArea = new JPanel(new BorderLayout());
        topArea.add(header, BorderLayout.NORTH);
        topArea.add(toolbar, BorderLayout.SOUTH);

        add(topArea, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        // ── Type-to-search: capture keystrokes while this window is focused ──
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(event ->
        {
            if (event.getID() != KeyEvent.KEY_TYPED) return false;
            Window focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
            if (focused != Launcher.this) return false;
            char c = event.getKeyChar();
            if (c == KeyEvent.CHAR_UNDEFINED) return false;
            if (c == '\u001b')
            {                    // Escape – clear filter
                clearSearch();
            }
            else if (c == '\b')
            {                 // Backspace – delete last char
                if (!searchQuery.isEmpty())
                {
                    searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                    applyFilter();
                }
            }
            else if (c == '\r' || c == '\n')
            {    // Enter – let existing handler fire
                return false;
            }
            else if (c >= 32)
            {                   // Any other printable character
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
    void setupTray()
    {
        if (!SystemTray.isSupported())
        {
            setVisible(true);
            return;
        }

        // Load apps.png as the tray icon; fall back to programmatic icon if unavailable
        Image trayImage;
        ImageIcon trayIconRes = loadScaledIcon("apps.png", 16, 16);
        if (trayIconRes != null)
        {
            trayImage = trayIconRes.getImage();
        }
        else
        {
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
        trayIcon.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if (e.getButton() == MouseEvent.BUTTON1)
                {
                    toggleVisibility();
                }
            }
        });

        try
        {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException ex)
        {
            setVisible(true);
            return;
        }

        // Clicking the window's close button hides to tray instead of exiting
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                setVisible(false);
            }
        });

        // Start hidden in the tray
        setVisible(false);
    }

    /**
     * Toggles the main window between visible (restored) and hidden.
     */
    private void toggleVisibility()
    {
        if (isVisible())
        {
            setVisible(false);
        }
        else
        {
            setVisible(true);
            setExtendedState(NORMAL);
            toFront();
            requestFocus();
        }
    }

    /**
     * Saves the current config with up-to-date window dimensions.
     */
    private void saveConfig()
    {
        config = new LauncherConfig(
                baseFolder.getAbsolutePath(),
                config.startMinimized(),
                getWidth(),
                getHeight(),
                config.priorityList());
        config.save(LauncherConfig.instanceConfigFile(launcherId));
    }

    /**
     * Persists the current allEntries order as the priority list in the instance config.
     * Called after a DnD reorder so the new order survives restarts.
     */
    private void savePriorityList()
    {
        List<String> names = new ArrayList<>();
        for (LaunchEntry e : allEntries)
        {
            names.add(e.file.getName());
        }
        config = new LauncherConfig(
                config.rootFolder(),
                config.startMinimized(),
                config.windowWidth(),
                config.windowHeight(),
                names);
        config.save(LauncherConfig.instanceConfigFile(launcherId));
    }

    /**
     * Opens the settings dialog where the user can inspect config-file locations
     * and change persistent settings.
     */
    private void showSettings()
    {
        JDialog dlg = new JDialog(this, "Settings  –  " + launcherId, true);
        dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dlg.setResizable(false);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(14, 16, 10, 16));

        // ── Helper: section header ────────────────────────────────────────────
        // ── Config-file info ─────────────────────────────────────────────────
        root.add(settingsSectionLabel("Configuration files"));
        root.add(Box.createVerticalStrut(4));
        root.add(settingsInfoRow("Launcher ID",       launcherId,                                                              null));
        root.add(Box.createVerticalStrut(2));
        root.add(settingsInfoRow("Global config",
                LauncherConfig.globalConfigFile().getAbsolutePath(),
                LauncherConfig.globalConfigFile().getParentFile()));
        root.add(Box.createVerticalStrut(2));
        root.add(settingsInfoRow("Instance config",
                LauncherConfig.instanceConfigFile(launcherId).getAbsolutePath(),
                LauncherConfig.instanceConfigFile(launcherId).getParentFile()));

        root.add(settingsSeparator());

        // ── Editable settings ─────────────────────────────────────────────────
        root.add(settingsSectionLabel("Startup"));
        root.add(Box.createVerticalStrut(6));

        JCheckBox cbMinimized = new JCheckBox(
                "Start minimized to system tray  (takes effect on next launch)");
        cbMinimized.setSelected(Boolean.TRUE.equals(config.startMinimized()));
        cbMinimized.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(cbMinimized);

        root.add(settingsSeparator());

        // ── Buttons ───────────────────────────────────────────────────────────
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnSave   = new JButton("Save");
        JButton btnCancel = new JButton("Cancel");
        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);
        root.add(btnPanel);

        btnSave.addActionListener(e ->
        {
            config = new LauncherConfig(
                    config.rootFolder(),
                    cbMinimized.isSelected(),
                    config.windowWidth(),
                    config.windowHeight(),
                    config.priorityList());
            config.save(LauncherConfig.instanceConfigFile(launcherId));
            dlg.dispose();
        });
        btnCancel.addActionListener(e -> dlg.dispose());

        dlg.add(root);
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    /** Bold section header label for the settings dialog. */
    private static JLabel settingsSectionLabel(String text)
    {
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 11f));
        lbl.setForeground(new Color(0x00, 0x50, 0x99));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    /** Horizontal separator for the settings dialog. */
    private static JSeparator settingsSeparator()
    {
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        return sep;
    }

    /**
     * A single read-only info row for the settings dialog.
     * If {@code openDir} is non-null a small folder button opens that directory.
     */
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
            ImageIcon folderIco = loadScaledIcon("folder.png", 14, 14);
            JButton btn = folderIco != null ? new JButton(folderIco) : new JButton("📂");
            btn.setToolTipText("Open folder");
            btn.setFocusPainted(false);
            btn.setMargin(new Insets(1, 4, 1, 4));
            final File dir = openDir;
            btn.addActionListener(e -> openInExplorer(dir));
            row.add(btn, BorderLayout.EAST);
        }

        return row;
    }

    /** Small coloured label for the legend. */
    private static JLabel coloredLabel(String text, Color color)
    {
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
    static ImageIcon loadScaledIcon(String resourceName, int w, int h)
    {
        try (InputStream is = Launcher.class.getResourceAsStream("/" + resourceName))
        {
            if (is == null) return null;
            BufferedImage raw = ImageIO.read(is);
            if (raw == null) return null;
            Image scaled = raw.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (IOException e)
        {
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
     * <p>
     * The action bar occupies the rightmost {@link EntryCellRenderer#ACT_BAR_W}
     * pixels of a cell.  Inside the bar, FlowLayout places icons as:
     * gap | icon0 | gap | icon1 | gap | icon2 | gap | icon3 | gap
     */
    private static int hitActionIcon(Point p, JList<LaunchEntry> list, int idx)
    {
        Rectangle cell = list.getCellBounds(idx, idx);
        if (cell == null) return -1;
        int xInCell = p.x - cell.x;
        int actionBarStart = cell.width - EntryCellRenderer.ACT_BAR_W;
        if (xInCell < actionBarStart) return -1;
        int xInBar = xInCell - actionBarStart;
        for (int i = 0; i < 4; i++)
        {
            int iconStart = EntryCellRenderer.ACT_HGAP
                    + i * (EntryCellRenderer.ACT_W + EntryCellRenderer.ACT_HGAP);
            if (xInBar >= iconStart && xInBar < iconStart + EntryCellRenderer.ACT_W)
            {
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
     * scripts first (sorted A-Z), then application folders (sorted A-Z),
     * then plain folders (sorted A-Z).
     */
    private List<LaunchEntry> loadEntries()
    {
        List<LaunchEntry> scripts = new ArrayList<>();
        List<LaunchEntry> apps = new ArrayList<>();
        List<LaunchEntry> plain = new ArrayList<>();

        File[] children = baseFolder.listFiles();
        if (children != null)
        {
            Arrays.sort(children,
                    Comparator.comparing(f -> f.getName().toLowerCase(Locale.ROOT)));
            for (File f : children)
            {
                if (f.isDirectory())
                {
                    File iconSrc = findAppIconSource(f);
                    if (iconSrc != null)
                    {
                        apps.add(new LaunchEntry(f, EntryType.APP_FOLDER, iconSrc));
                    }
                    else
                    {
                        plain.add(new LaunchEntry(f, EntryType.PLAIN_FOLDER));
                    }
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

        // Apply priority list: listed items come first (in priority order),
        // all remaining items follow in their natural order.
        List<String> priority = config.priorityList();
        if (priority == null || priority.isEmpty())
        {
            return natural;
        }

        Map<String, LaunchEntry> byName = new LinkedHashMap<>();
        for (LaunchEntry e : natural)
        {
            byName.put(e.file.getName(), e);
        }

        List<LaunchEntry> result = new ArrayList<>();
        for (String name : priority)
        {
            LaunchEntry e = byName.remove(name);
            if (e != null) result.add(e);
        }
        result.addAll(byName.values());  // remainder in natural order
        return result;
    }

    /**
     * If the directory qualifies as an application folder, returns the file to use
     * as the icon source (the first .lnk at the top level, or the fallback exe).
     * Returns null if the directory is a plain folder.
     */
    private static File findAppIconSource(File dir)
    {
        File[] children = dir.listFiles();
        if (children != null)
        {
            for (File f : children)
            {
                if (f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".lnk"))
                {
                    return f;
                }
            }
        }
        File fallback = new File(dir, FALLBACK_EXE);
        return fallback.isFile() ? fallback : null;
    }

    /**
     * Returns true for files with a recognised script extension.
     */
    private static boolean isScript(File f)
    {
        String n = f.getName().toLowerCase(Locale.ROOT);
        return n.endsWith(".bat") || n.endsWith(".cmd")
                || n.endsWith(".ps1") || n.endsWith(".vbs")
                || n.endsWith(".sh") || n.endsWith(".js");
    }

    // =========================================================================
    //  Launching
    // =========================================================================

    private void launch(LaunchEntry entry)
    {
        try
        {
            if (entry.type == EntryType.SCRIPT)
            {
                launchScript(entry.file);
            }
            else if (entry.type == EntryType.APP_FOLDER)
            {
                launchAppFolder(entry.file);
            }
            else
            {
                // PLAIN_FOLDER – no launcher found; open in File Explorer
                openInExplorer(entry.file);
            }
        } catch (IOException ex)
        {
            JOptionPane.showMessageDialog(this,
                    "Failed to launch:\n" + ex.getMessage(),
                    "Launcher Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Execute a script in an appropriate interpreter / shell window.
     * <p>
     * .bat / .cmd  →  new console window  (cmd /c start)
     * .ps1         →  PowerShell  (-ExecutionPolicy Bypass -NoExit)
     * .vbs / .js   →  Windows Script Host  (wscript)
     * .sh          →  bash
     */
    private static void launchScript(File script) throws IOException
    {
        String name = script.getName().toLowerCase(Locale.ROOT);
        ProcessBuilder pb;

        if (name.endsWith(".ps1"))
        {
            // PowerShell – keep window open after script finishes
            pb = new ProcessBuilder(
                    "powershell", "-ExecutionPolicy", "Bypass",
                    "-NoExit", "-File", script.getAbsolutePath());

        }
        else if (name.endsWith(".vbs") || name.endsWith(".js"))
        {
            // Windows Script Host
            pb = new ProcessBuilder("wscript", script.getAbsolutePath());

        }
        else if (name.endsWith(".sh"))
        {
            // Bash (WSL or Git Bash must be on PATH)
            pb = new ProcessBuilder("bash", script.getAbsolutePath());

        }
        else
        {
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
     * 1. Execute the first .lnk shortcut found at the folder's top level, or
     * 2. Execute <appFolder>\basis\sys\win\bin\dsc_StartPlm.exe.
     */
    private void launchAppFolder(File appFolder) throws IOException
    {
        // Step 1 – look for a Windows shortcut (.lnk) at the top level
        File[] children = appFolder.listFiles();
        if (children != null)
        {
            for (File f : children)
            {
                if (f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".lnk"))
                {
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
        if (fallback.isFile())
        {
            new ProcessBuilder(fallback.getAbsolutePath())
                    .directory(appFolder)
                    .start();
        }
        else
        {
            JOptionPane.showMessageDialog(this,
                    "<html>No shortcut (.lnk) found in:<br>&nbsp;&nbsp;<b>"
                            + appFolder.getAbsolutePath()
                            + "</b><br><br>Fallback executable not found at:<br>&nbsp;&nbsp;<b>"
                            + fallback.getAbsolutePath() + "</b></html>",
                    "Launcher – Nothing to start", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Opens an application folder in Windows File Explorer.
     */
    private void openInExplorer(File folder)
    {
        try
        {
            Desktop.getDesktop().open(folder);
        } catch (IOException ex)
        {
            JOptionPane.showMessageDialog(this,
                    "Could not open File Explorer:\n" + ex.getMessage(),
                    "Launcher Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Opens an application folder in VS Code (requires 'code' on PATH).
     */
    private void openInVSCode(File folder)
    {
        try
        {
            // 'code' is a .cmd script, so it must be invoked via cmd /c;
            // ProcessBuilder cannot resolve .cmd extensions on its own.
            new ProcessBuilder("cmd", "/c", "code", folder.getAbsolutePath())
                    .directory(folder)
                    .start();
        } catch (IOException ex)
        {
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
    private void copyWithRobocopy(File srcFolder)
    {
        String newName = JOptionPane.showInputDialog(this,
                "Enter new folder name for the copy:",
                srcFolder.getName() + "_copy");

        if (newName == null || newName.trim().isEmpty())
        {
            return;
        }

        newName = newName.trim();

        // Check if name already exists in the active launcher folder
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
            // robocopy source destination /MIR (mirror - copies recursively)
            ProcessBuilder pb = new ProcessBuilder(
                    "robocopy", srcFolder.getAbsolutePath(), destPath.getAbsolutePath(), "/MIR");
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Show output; auto-close window and refresh list when done
            showProcessOutput(process, "Robocopy: " + srcFolder.getName() + " → " + newName, this::refreshList);

        } catch (IOException ex)
        {
            JOptionPane.showMessageDialog(this,
                    "Could not start robocopy:\n" + ex.getMessage(),
                    "Launcher Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Deletes a folder and all its contents after confirmation.
     * Refreshes the list immediately after successful deletion.
     */
    private void deleteFolder(File folder)
    {
        int confirm = JOptionPane.showConfirmDialog(this,
                "<html>Permanently delete:<br><b>" + folder.getAbsolutePath() + "</b><br><br>"
                        + "This cannot be undone.</html>",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION)
        {
            return;
        }

        try
        {
            deleteDirectory(folder);
            refreshList();
        } catch (IOException ex)
        {
            JOptionPane.showMessageDialog(this,
                    "Could not delete folder:\n" + ex.getMessage(),
                    "Launcher Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Recursively deletes a directory and all its contents.
     */
    private static void deleteDirectory(File dir) throws IOException
    {
        if (!dir.exists())
        {
            return;
        }

        if (dir.isDirectory())
        {
            File[] children = dir.listFiles();
            if (children != null)
            {
                for (File child : children)
                {
                    deleteDirectory(child);
                }
            }
        }

        if (!dir.delete())
        {
            throw new IOException("Failed to delete: " + dir.getAbsolutePath());
        }
    }

    /**
     * Opens a dialog to enter an SVN repository URL and checks it out
     * into the application folder (creating a subfolder with the repo name).
     */
    private void svnCheckout(File targetFolder)
    {
        String url = JOptionPane.showInputDialog(this,
                "Enter SVN repository URL:",
                "SVN Checkout",
                JOptionPane.QUESTION_MESSAGE);

        if (url == null || url.trim().isEmpty())
        {
            return;
        }

        url = url.trim();

        try
        {
            // Extract repository name from URL (last part)
            String repoName = url.replaceAll(".*/+", "").replaceAll("\\..*", "");
            if (repoName.isEmpty())
            {
                repoName = "checkout";
            }

            File checkoutDir = new File(targetFolder, repoName);

            ProcessBuilder pb = new ProcessBuilder("svn", "checkout", url, checkoutDir.getAbsolutePath());
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Show output in a new window (keep open; user can close manually)
            showProcessOutput(process, "SVN Checkout: " + url, null);

            // After SVN completes, offer to refresh the list
            new Thread(() ->
            {
                try
                {
                    process.waitFor();
                    SwingUtilities.invokeLater(() ->
                    {
                        int refresh = JOptionPane.showConfirmDialog(this,
                                "Checkout completed. Refresh the file list?",
                                "SVN Checkout", JOptionPane.YES_NO_OPTION);
                        if (refresh == JOptionPane.YES_OPTION)
                        {
                            setTitle("Launcher  –  " + baseFolder.getAbsolutePath());
                        }
                    });
                } catch (InterruptedException ignored)
                {
                }
            }).start();

        } catch (IOException ex)
        {
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
     * @param onComplete if non-null, the window is closed automatically when the
     *                   process finishes and this callback is invoked on the EDT.
     *                   If null, a "Process completed." line is appended instead.
     */
    private static void showProcessOutput(Process process, String title, Runnable onComplete)
    {
        SwingUtilities.invokeLater(() ->
        {
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

            new Thread(() ->
            {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream())))
                {
                    String line;
                    while ((line = reader.readLine()) != null)
                    {
                        final String output = line;
                        SwingUtilities.invokeLater(() ->
                        {
                            model.addElement(output);
                            outputList.ensureIndexIsVisible(model.getSize() - 1);
                        });
                    }
                } catch (IOException ignored)
                {
                }

                SwingUtilities.invokeLater(() ->
                {
                    if (onComplete != null)
                    {
                        outputFrame.dispose();
                        onComplete.run();
                    }
                    else
                    {
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

    public static void main(String[] args)
    {

        // Native look and feel for a clean Windows appearance
        try
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored)
        { /* fall back to Metal */ }

        // ── Parse CLI flags ──────────────────────────────────────────────────
        // Usage:
        //   java Launcher [<rootFolder>]
        //                 [--minimized]
        //                 [--launcherId=<id>]
        //                 [--config=<path>]
        //
        //   --minimized         Start hidden in the system tray.
        //   --launcherId=<id>   Use this string as the launcher instance ID.
        //                       Defaults to an 8-char hex hash of the root folder path.
        //   --config=<path>     Load an additional config file whose values override
        //                       both the global and instance configs.
        boolean minimizedCli  = false;
        String  folderArg     = null;
        String  launcherIdArg = null;
        String  configPathArg = null;

        for (String arg : args)
        {
            if (arg.equalsIgnoreCase("--minimized") || arg.equalsIgnoreCase("-minimized"))
            {
                minimizedCli = true;
            }
            else if (arg.startsWith("--launcherId="))
            {
                launcherIdArg = arg.substring("--launcherId=".length()).trim();
            }
            else if (arg.startsWith("--config="))
            {
                configPathArg = arg.substring("--config=".length()).trim();
            }
            else if (folderArg == null && !arg.startsWith("--"))
            {
                folderArg = arg;
            }
        }

        // ── Level 1: global config (%APPDATA%\nvLauncher\config.json) ────────
        File globalFile = LauncherConfig.globalConfigFile();
        LauncherConfig globalConfig = LauncherConfig.loadFile(globalFile);
        if (!globalFile.exists())
        {
            // Create global config with defaults on first run
            LauncherConfig.defaults().save(globalFile);
        }

        // ── Determine root folder (needed for launcherId hash) ───────────────
        String folderStr = (folderArg != null) ? folderArg : globalConfig.rootFolder();
        final File folder;
        if (folderStr != null)
        {
            folder = new File(folderStr);
        }
        else
        {
            // No folder known yet – show file chooser
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select launcher root folder");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);
            if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
            {
                System.exit(0);
            }
            folder = chooser.getSelectedFile();
        }

        if (!folder.isDirectory())
        {
            JOptionPane.showMessageDialog(null,
                    "Not a valid directory:\n" + folder.getAbsolutePath(),
                    "Launcher", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // ── Determine launcher instance ID ───────────────────────────────────
        final String launcherId = (launcherIdArg != null && !launcherIdArg.isEmpty())
                ? launcherIdArg
                : String.format("%08x", folder.getAbsolutePath().hashCode() & 0xFFFFFFFFL);

        // ── Level 2: instance config (%APPDATA%\nvLauncher\{id}\config.json) ─
        LauncherConfig instanceConfig = LauncherConfig.loadFile(
                LauncherConfig.instanceConfigFile(launcherId));

        // ── Level 3: explicit config (--config=<path>) ───────────────────────
        LauncherConfig explicitConfig = (configPathArg != null)
                ? LauncherConfig.loadFile(new File(configPathArg))
                : LauncherConfig.empty();

        // ── Merge: global ← instance ← explicit, then fill defaults ─────────
        LauncherConfig merged = explicitConfig
                .mergeOver(instanceConfig
                .mergeOver(globalConfig))
                .withDefaults();

        // ── CLI overrides (highest priority) ─────────────────────────────────
        boolean startMinimized = minimizedCli || Boolean.TRUE.equals(merged.startMinimized());
        // CLI folder arg overrides config rootFolder
        final File resolvedFolder = (folderArg != null) ? new File(folderArg) : folder;

        // ── Build final resolved config and persist to instance file ─────────
        final LauncherConfig resolvedConfig = new LauncherConfig(
                resolvedFolder.getAbsolutePath(),
                startMinimized,
                merged.windowWidth(),
                merged.windowHeight(),
                merged.priorityList());
        resolvedConfig.save(LauncherConfig.instanceConfigFile(launcherId));

        final boolean minimized = startMinimized;
        SwingUtilities.invokeLater(() ->
        {
            Launcher launcher = new Launcher(resolvedFolder, resolvedConfig, launcherId);
            if (minimized)
            {
                launcher.setupTray();   // starts hidden; tray icon allows show/exit
            }
            else
            {
                launcher.setVisible(true);
            }
        });
    }
}
