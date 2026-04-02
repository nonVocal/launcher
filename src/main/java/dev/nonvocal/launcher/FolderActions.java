package dev.nonvocal.launcher;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;

/**
 * Handles all folder-level actions: open in explorer/editor, copy with robocopy,
 * delete, and SVN checkout.
 * Uses a {@code Supplier<LauncherConfig>} so it always reads the most recent config.
 */
class FolderActions
{
    private final Component          parent;
    private final File               baseFolder;
    private final Supplier<LauncherConfig> config;
    private final Runnable           onRefresh;

    FolderActions(Component parent, File baseFolder,
                  Supplier<LauncherConfig> config, Runnable onRefresh)
    {
        this.parent     = parent;
        this.baseFolder = baseFolder;
        this.config     = config;
        this.onRefresh  = onRefresh;
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    void openInExplorer(File folder)
    {
        String cmd = config.get().explorer();
        try
        {
            if (cmd != null && !cmd.isBlank())
                new ProcessBuilder(cmd, folder.getAbsolutePath()).start();
            else
                Desktop.getDesktop().open(folder);
        }
        catch (IOException ex)
        {
            JOptionPane.showMessageDialog(parent,
                    "Could not open file explorer:\n" + ex.getMessage(),
                    "Launcher Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    void openInEditor(File folder)
    {
        String cmd = config.get().editor();
        if (cmd == null || cmd.isBlank()) cmd = "code";
        try
        {
            new ProcessBuilder("cmd", "/c", cmd, folder.getAbsolutePath())
                    .directory(folder).start();
        }
        catch (IOException ex)
        {
            JOptionPane.showMessageDialog(parent,
                    "Could not open editor.\nMake sure '" + cmd + "' is on your PATH.\n\n" + ex.getMessage(),
                    "Launcher Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    void copyWithRobocopy(File srcFolder)
    {
        String newName = JOptionPane.showInputDialog(parent,
                "Enter new folder name for the copy:", srcFolder.getName() + "_copy");
        if (newName == null || newName.trim().isEmpty()) return;
        newName = newName.trim();

        File dest = new File(baseFolder, newName);
        if (dest.exists())
        {
            JOptionPane.showMessageDialog(parent,
                    "<html>A folder named <b>" + newName + "</b> already exists in:<br><b>"
                    + baseFolder.getAbsolutePath() + "</b></html>",
                    "Name Already Exists", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try
        {
            ProcessBuilder pb = new ProcessBuilder(
                    "robocopy", srcFolder.getAbsolutePath(), dest.getAbsolutePath(), "/MIR");
            pb.redirectErrorStream(true);
            ProcessOutputWindow.show(pb.start(),
                    "Robocopy: " + srcFolder.getName() + " \u2192 " + newName, onRefresh);
        }
        catch (IOException ex)
        {
            JOptionPane.showMessageDialog(parent,
                    "Could not start robocopy:\n" + ex.getMessage(),
                    "Launcher Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    void deleteFolder(File folder)
    {
        int confirm = JOptionPane.showConfirmDialog(parent,
                "<html>Permanently delete:<br><b>" + folder.getAbsolutePath()
                + "</b><br><br>This cannot be undone.</html>",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        try
        {
            deleteDirectory(folder);
            onRefresh.run();
        }
        catch (IOException ex)
        {
            JOptionPane.showMessageDialog(parent,
                    "Could not delete folder:\n" + ex.getMessage(),
                    "Launcher Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    void svnCheckout(File targetFolder)
    {
        String url = JOptionPane.showInputDialog(parent,
                "Enter SVN repository URL:", "SVN Checkout", JOptionPane.QUESTION_MESSAGE);
        if (url == null || url.trim().isEmpty()) return;
        url = url.trim();
        try
        {
            String repoName = url.replaceAll(".*/+", "").replaceAll("\\..*", "");
            if (repoName.isEmpty()) repoName = "checkout";

            ProcessBuilder pb = new ProcessBuilder(
                    "svn", "checkout", url, new File(targetFolder, repoName).getAbsolutePath());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            ProcessOutputWindow.show(process, "SVN Checkout: " + url, null);

            new Thread(() ->
            {
                try
                {
                    process.waitFor();
                    SwingUtilities.invokeLater(() ->
                    {
                        int r = JOptionPane.showConfirmDialog(parent,
                                "Checkout completed. Refresh the file list?",
                                "SVN Checkout", JOptionPane.YES_NO_OPTION);
                        if (r == JOptionPane.YES_OPTION) onRefresh.run();
                    });
                }
                catch (InterruptedException ignored) {}
            }).start();
        }
        catch (IOException ex)
        {
            JOptionPane.showMessageDialog(parent,
                    "Could not start SVN:\n" + ex.getMessage()
                    + "\n\nMake sure 'svn' is installed and on your PATH.",
                    "Launcher Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void deleteDirectory(File dir) throws IOException
    {
        if (!dir.exists()) return;
        if (dir.isDirectory())
        {
            File[] children = dir.listFiles();
            if (children != null)
                for (File child : children) deleteDirectory(child);
        }
        if (!dir.delete())
            throw new IOException("Failed to delete: " + dir.getAbsolutePath());
    }
}

