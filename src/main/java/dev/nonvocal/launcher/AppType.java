package dev.nonvocal.launcher;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Defines how to detect, display, and launch a particular category of application folder.
 *
 * <ul>
 *   <li>{@code id}              – unique identifier referenced in {@code appTypeAssignments}</li>
 *   <li>{@code iconPath}        – optional absolute path to an icon image (PNG/JPG/GIF);
 *                                 when set, used as the entry icon instead of the executable's system icon</li>
 *   <li>{@code executablePaths} – priority-ordered list of sub-folder paths inside the app folder
 *                                 to search; use an empty string {@code ""} for the folder root</li>
 *   <li>{@code executableNames} – priority-ordered list of file names to look for in each path</li>
 * </ul>
 *
 * <p>Detection: for each {@code executablePath} (in order), for each {@code executableName} (in order),
 * the combined file is checked for existence.  The first existing file wins.</p>
 */
record AppType(
        String       id,
        String       iconPath,
        List<String> executablePaths,
        List<String> executableNames)
{
    AppType
    {
        // Pre-filter once at construction so the search loop needs no null/empty guards
        executablePaths = executablePaths == null
                ? List.of()
                : executablePaths.stream().filter(n -> n != null && !n.isEmpty()).toList();

        executableNames = executableNames == null
                ? List.of()
                : executableNames.stream().filter(n -> n != null && !n.isEmpty()).toList();
    }

    /**
     * Searches {@code appFolder} for an executable using the priority lists.
     * Returns the first existing file, or {@code null} if none found.
     */
    File findExecutable(File appFolder)
    {
        if (executablePaths.isEmpty() || executableNames.isEmpty()) return null;

        for (String path : executablePaths)
        {
            File searchDir = path.isEmpty() ? appFolder : new File(appFolder, path);
            for (String name : executableNames)
            {
                File candidate = new File(searchDir, name);
                if (candidate.isFile()) return candidate;
            }
        }
        return null;
    }

    /** Returns {@code true} if this type's executable exists inside {@code appFolder}. */
    boolean matches(File appFolder)
    {
        return findExecutable(appFolder) != null;
    }

    /**
     * Loads the custom icon from {@link #iconPath} and scales it to {@code w × h} pixels.
     * Returns {@code null} if {@code iconPath} is blank or the image cannot be read.
     */
    ImageIcon loadIcon(int w, int h)
    {
        if (iconPath == null || iconPath.isBlank()) return null;
        try
        {
            BufferedImage raw = ImageIO.read(new File(iconPath));
            if (raw == null) return null;
            return new ImageIcon(raw.getScaledInstance(w, h, Image.SCALE_SMOOTH));
        }
        catch (IOException e) { return null; }
    }
}

