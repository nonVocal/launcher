package dev.nonvocal.launcher;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
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

    void deleteFolder(File folder)    {
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

    /**
     * Executes a user-defined {@link CustomAction} with {@code targetFolder} as the
     * first command-line argument.  The process output is streamed in a dedicated window.
     * <p>
     * The following environment variables are always set for the child process:
     * <ul>
     *   <li>{@code NV_LAUNCHER_FOLDER} – absolute path of the launcher root folder</li>
     *   <li>{@code NV_ENTRY_PATH}      – absolute path passed as the first CLI argument</li>
     *   <li>{@code NV_ENTRY_NAME}      – file/folder name of the entry (or root folder)</li>
     *   <li>{@code NV_ENTRY_TYPE}      – {@code SCRIPT}, {@code APP_FOLDER}, or {@code PLAIN_FOLDER}
     *                                    (empty when no entry context is available)</li>
     *   <li>{@code NV_APP_TYPE_ID}     – ID of the matched {@link AppType}, or empty string</li>
     *   <li>{@code NV_ICON_FILE}       – absolute path of the icon file, or empty string</li>
     * </ul>
     *
     * @param action       the custom action to execute
     * @param targetFolder the folder passed as the first CLI argument
     * @param entry        the list entry that triggered the action, or {@code null} when
     *                     triggered from the toolbar without a selected entry
     */
    void executeCustomAction(CustomAction action, File targetFolder, LaunchEntry entry)
    {
        if (action.scriptPath() == null || action.scriptPath().isBlank())
        {
            JOptionPane.showMessageDialog(parent,
                    "Action \"" + action.effectiveLabel() + "\" has no script path configured.",
                    "Launcher \u2013 Custom Action", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try
        {
            ProcessBuilder pb = new ProcessBuilder(
                    action.scriptPath(), targetFolder.getAbsolutePath());
            pb.redirectErrorStream(true);

            // ── Expose entry metadata as environment variables ────────────────
            java.util.Map<String, String> env = pb.environment();
            env.put("NV_LAUNCHER_FOLDER", baseFolder.getAbsolutePath());
            env.put("NV_ENTRY_PATH",      targetFolder.getAbsolutePath());
            env.put("NV_ENTRY_NAME",      targetFolder.getName());
            if (entry != null)
            {
                env.put("NV_ENTRY_TYPE",  entry.type().name());
                env.put("NV_APP_TYPE_ID", entry.appType()  != null ? entry.appType().id()                      : "");
                env.put("NV_ICON_FILE",   entry.iconFile() != null ? entry.iconFile().getAbsolutePath()         : "");
            }
            else
            {
                env.put("NV_ENTRY_TYPE",  "");
                env.put("NV_APP_TYPE_ID", "");
                env.put("NV_ICON_FILE",   "");
            }

            ProcessOutputWindow.show(pb.start(), action.effectiveLabel(), null);
        }
        catch (IOException ex)
        {
            JOptionPane.showMessageDialog(parent,
                    "Could not run \"" + action.effectiveLabel() + "\":\n" + ex.getMessage()
                    + "\n\nCheck that the script path is correct and the file is executable.",
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

    /**
     * Opens the TortoiseSVN repository browser at the given URL so the user can
     * navigate the repository and check out any sub-path they choose.
     * <p>
     * While the repository browser is open (and for a short grace period
     * afterwards) the {@code baseFolder} is watched for newly created
     * directories.  As soon as one appears the application list is refreshed
     * automatically.
     */
    void svnCheckoutWithRepoBrowser(File targetFolder)
    {
        File tortoiseProc = findTortoiseSVN();
        if (tortoiseProc == null)
        {
            JOptionPane.showMessageDialog(parent,
                    "<html>TortoiseSVN was not found on this system.<br><br>"
                    + "Please install TortoiseSVN to use the repository browser.<br>"
                    + "Common installation path:<br>"
                    + "<tt>C:\\Program Files\\TortoiseSVN\\bin\\TortoiseProc.exe</tt></html>",
                    "TortoiseSVN Not Found", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(tortoiseProc.getAbsolutePath());
        cmd.add("/command:repobrowser");

        Process repoBrowserProcess;
        try
        {
            repoBrowserProcess = new ProcessBuilder(cmd).start();
        }
        catch (IOException ex)
        {
            JOptionPane.showMessageDialog(parent,
                    "Could not open SVN repository browser:\n" + ex.getMessage(),
                    "Launcher Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        startCheckoutWatcher(repoBrowserProcess);
    }

    // ── SVN helpers ───────────────────────────────────────────────────────────

    /**
     * Searches common installation directories for {@code TortoiseProc.exe}.
     * Returns the first match, or {@code null} if TortoiseSVN is not installed.
     */
    private static File findTortoiseSVN()
    {
        String pf   = System.getenv("ProgramFiles");
        String pf86 = System.getenv("ProgramFiles(x86)");
        String[] candidates = {
            pf   != null ? pf   + "\\TortoiseSVN\\bin\\TortoiseProc.exe" : null,
            pf86 != null ? pf86 + "\\TortoiseSVN\\bin\\TortoiseProc.exe" : null,
        };
        for (String p : candidates)
        {
            if (p == null) continue;
            File f = new File(p);
            if (f.exists()) return f;
        }
        return null;
    }

    /**
     * Watches {@code baseFolder} for newly created sub-directories.
     * <ul>
     *   <li>While {@code repoBrowserProcess} is alive the watcher stays active.</li>
     *   <li>After the browser is closed a 2-minute grace window keeps the watcher
     *       running so that a checkout already in progress can still finish.</li>
     * </ul>
     * Every time a new directory is detected the application list is refreshed on
     * the Event Dispatch Thread.
     */
    private void startCheckoutWatcher(Process repoBrowserProcess)
    {
        Thread watcher = new Thread(() ->
        {
            try (WatchService ws = FileSystems.getDefault().newWatchService())
            {
                baseFolder.toPath().register(ws, StandardWatchEventKinds.ENTRY_CREATE);

                // Phase 1 – watch while the repository browser is open
                while (repoBrowserProcess.isAlive())
                {
                    WatchKey key = ws.poll(3, TimeUnit.SECONDS);
                    if (key != null)
                    {
                        processWatchKey(key);
                    }
                }

                // Phase 2 – 2-minute grace period after the browser window closes
                long graceEnd = System.currentTimeMillis() + 2L * 60 * 1000;
                while (System.currentTimeMillis() < graceEnd)
                {
                    WatchKey key = ws.poll(5, TimeUnit.SECONDS);
                    if (key == null) continue;
                    processWatchKey(key);
                }
            }
            catch (IOException | InterruptedException ignored) {}
        }, "svn-checkout-watcher");
        watcher.setDaemon(true);
        watcher.start();
    }

    /** Processes one {@link WatchKey}: refreshes the list for every new directory found. */
    private void processWatchKey(WatchKey key)
    {
        for (WatchEvent<?> event : key.pollEvents())
        {
            if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE)
            {
                @SuppressWarnings("unchecked")
                Path created = baseFolder.toPath().resolve(((WatchEvent<Path>) event).context());
                if (created.toFile().isDirectory())
                    SwingUtilities.invokeLater(onRefresh);
            }
        }
        key.reset();
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

