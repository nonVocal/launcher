package dev.nonvocal.launcher;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Launches {@link LaunchEntry} objects (scripts and application folders).
 * Shows an error dialog on the EDT if the launch fails.
 */
class EntryLauncher
{
    private final Component parent;

    EntryLauncher(Component parent)
    {
        this.parent = parent;
    }

    /**
     * Launches the given entry.
     * Plain folders are handled by {@code explorerOpener} (usually {@code FolderActions::openInExplorer}).
     */
    void launch(LaunchEntry entry, Consumer<File> explorerOpener)
    {
        try
        {
            if      (entry.type() == EntryType.SCRIPT)     launchScript(entry.file());
            else if (entry.type() == EntryType.APP_FOLDER) launchAppFolder(entry.file());
            else                                            explorerOpener.accept(entry.file());
        }
        catch (IOException ex)
        {
            JOptionPane.showMessageDialog(parent,
                    "Failed to launch:\n" + ex.getMessage(),
                    "Launcher Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Script ────────────────────────────────────────────────────────────────

    private static void launchScript(File script) throws IOException
    {
        String name = script.getName().toLowerCase(Locale.ROOT);
        ProcessBuilder pb;
        if      (name.endsWith(".ps1"))
            pb = new ProcessBuilder("powershell", "-ExecutionPolicy", "Bypass", "-NoExit", "-File", script.getAbsolutePath());
        else if (name.endsWith(".vbs") || name.endsWith(".js"))
            pb = new ProcessBuilder("wscript", script.getAbsolutePath());
        else if (name.endsWith(".sh"))
            pb = new ProcessBuilder("bash", script.getAbsolutePath());
        else
            pb = new ProcessBuilder("cmd", "/c", "start", script.getName(), "cmd", "/k", script.getAbsolutePath());
        pb.directory(script.getParentFile());
        pb.start();
    }

    // ── Application folder ────────────────────────────────────────────────────

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
        File fallback = new File(appFolder, EntryLoader.FALLBACK_EXE);
        if (fallback.isFile())
        {
            new ProcessBuilder(fallback.getAbsolutePath()).directory(appFolder).start();
        }
        else
        {
            JOptionPane.showMessageDialog(parent,
                    "<html>No shortcut (.lnk) found in:<br>&nbsp;&nbsp;<b>" + appFolder.getAbsolutePath()
                    + "</b><br><br>Fallback executable not found at:<br>&nbsp;&nbsp;<b>"
                    + fallback.getAbsolutePath() + "</b></html>",
                    "Launcher \u2013 Nothing to start", JOptionPane.WARNING_MESSAGE);
        }
    }
}

