package dev.nonvocal.launcher;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.filechooser.FileSystemView;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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

    /** Relative path of the fallback executable inside an application folder. */
    private static final String FALLBACK_EXE = "basis\\sys\\win\\bin\\dsc_StartPlm.exe";

    // =========================================================================
    //  Data model
    // =========================================================================

    enum EntryType { SCRIPT, APP_FOLDER }

    static final class LaunchEntry {
        final File      file;
        final EntryType type;

        LaunchEntry(File file, EntryType type) {
            this.file = file;
            this.type = type;
        }

        @Override
        public String toString() { return file.getName(); }
    }

    // =========================================================================
    //  Cell renderer
    // =========================================================================

    static final class EntryCellRenderer extends DefaultListCellRenderer {

        // Alternating row colours
        private static final Color ROW_EVEN  = new Color(0xF4, 0xF6, 0xF8);
        private static final Color ROW_ODD   = Color.WHITE;
        // Foreground colours by type
        private static final Color FG_SCRIPT = new Color(0x1A, 0x5F, 0x7A); // dark teal
        private static final Color FG_FOLDER = new Color(0x2E, 0x6B, 0x2E); // dark green
        // Selection
        private static final Color SEL_BG    = new Color(0x00, 0x78, 0xD7); // Windows blue

        private static final Font CELL_FONT  = new Font(Font.SANS_SERIF, Font.PLAIN, 13);

        private final FileSystemView fsv = FileSystemView.getFileSystemView();

        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index,
                boolean selected, boolean cellHasFocus) {

            super.getListCellRendererComponent(list, value, index, selected, cellHasFocus);

            LaunchEntry e = (LaunchEntry) value;
            setText("  " + e.file.getName());
            setFont(CELL_FONT);
            setToolTipText(e.file.getAbsolutePath());
            setBorder(new EmptyBorder(5, 8, 5, 8));

            // Use the OS-provided icon for realistic visuals
            try { setIcon(fsv.getSystemIcon(e.file)); }
            catch (Exception ignored) { setIcon(null); }

            if (selected) {
                setBackground(SEL_BG);
                setForeground(Color.WHITE);
            } else {
                setBackground(index % 2 == 0 ? ROW_EVEN : ROW_ODD);
                setForeground(e.type == EntryType.SCRIPT ? FG_SCRIPT : FG_FOLDER);
            }
            return this;
        }
    }

    // =========================================================================
    //  Constructor / UI
    // =========================================================================

    private final File baseFolder;

    Launcher(File baseFolder) {
        this.baseFolder = baseFolder;
        buildUI();
    }

    private void buildUI() {
        setTitle("Launcher  \u2013  " + baseFolder.getAbsolutePath());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(560, 680);
        setMinimumSize(new Dimension(320, 200));
        setLocationRelativeTo(null);

        // ── Header bar ───────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0x00, 0x78, 0xD7));
        header.setBorder(new EmptyBorder(10, 14, 10, 14));

        JLabel dirLabel = new JLabel(baseFolder.getName()
                + "   \u2014   " + baseFolder.getAbsolutePath());
        dirLabel.setFont(dirLabel.getFont().deriveFont(Font.BOLD, 13f));
        dirLabel.setForeground(Color.WHITE);
        header.add(dirLabel, BorderLayout.CENTER);

        // ── Entry list ───────────────────────────────────────────────────────
        DefaultListModel<LaunchEntry> model = new DefaultListModel<>();
        loadEntries().forEach(model::addElement);

        JList<LaunchEntry> list = new JList<>(model);
        list.setCellRenderer(new EntryCellRenderer());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(36);

        // Double-click to launch
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    LaunchEntry sel = list.getSelectedValue();
                    if (sel != null) launch(sel);
                }
            }
        });

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

        JLabel hint = new JLabel(model.size() + " entries   |   "
                + "Double-click or Enter to launch");
        hint.setForeground(Color.GRAY);
        hint.setFont(hint.getFont().deriveFont(11f));

        JPanel south = new JPanel(new BorderLayout());
        south.add(legend, BorderLayout.WEST);
        south.add(hint,   BorderLayout.EAST);
        south.setBorder(new EmptyBorder(0, 10, 0, 10));
        south.setBackground(new Color(0xF0, 0xF0, 0xF0));

        add(header, BorderLayout.NORTH);
        add(scroll,  BorderLayout.CENTER);
        add(south,   BorderLayout.SOUTH);
    }

    /** Small coloured label for the legend. */
    private static JLabel coloredLabel(String text, Color color) {
        JLabel lbl = new JLabel("\u25A0 " + text);
        lbl.setForeground(color);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 11f));
        return lbl;
    }

    // =========================================================================
    //  Entry loading
    // =========================================================================

    /**
     * Scans the base folder one level deep and returns:
     *   scripts first (sorted A-Z), then application folders (sorted A-Z).
     */
    private List<LaunchEntry> loadEntries() {
        List<LaunchEntry> scripts = new ArrayList<>();
        List<LaunchEntry> apps    = new ArrayList<>();

        File[] children = baseFolder.listFiles();
        if (children != null) {
            Arrays.sort(children,
                    Comparator.comparing(f -> f.getName().toLowerCase(Locale.ROOT)));
            for (File f : children) {
                if (f.isDirectory()) {
                    apps.add(new LaunchEntry(f, EntryType.APP_FOLDER));
                } else if (isScript(f)) {
                    scripts.add(new LaunchEntry(f, EntryType.SCRIPT));
                }
            }
        }

        List<LaunchEntry> all = new ArrayList<>(scripts);
        all.addAll(apps);
        return all;
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
            } else {
                launchAppFolder(entry.file);
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

    // =========================================================================
    //  Entry point
    // =========================================================================

    public static void main(String[] args) {

        // Native look and feel for a clean Windows appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { /* fall back to Metal */ }

        // Determine the root folder
        final File folder;

        if (args.length == 0) {
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
            folder = new File(args[0]);
        }

        if (!folder.isDirectory()) {
            JOptionPane.showMessageDialog(null,
                    "Not a valid directory:\n" + folder.getAbsolutePath(),
                    "Launcher", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        SwingUtilities.invokeLater(() -> new Launcher(folder).setVisible(true));
    }
}
