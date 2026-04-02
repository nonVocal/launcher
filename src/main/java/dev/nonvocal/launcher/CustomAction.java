package dev.nonvocal.launcher;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * A user-defined action that can appear in the entry action bar or the toolbar.
 *
 * <ul>
 *   <li>{@code id}         – unique key; referenced in {@code actionOrder} / {@code toolbarActions}</li>
 *   <li>{@code iconPath}   – absolute path to a PNG/JPG/GIF icon file (may be {@code null})</li>
 *   <li>{@code scriptPath} – absolute path to the script or executable to run</li>
 *   <li>{@code label}      – display name; falls back to {@code id} when blank or {@code null}</li>
 *   <li>{@code tooltip}    – tooltip text; falls back to the effective label when blank or {@code null}</li>
 * </ul>
 *
 * When used as a <em>list-entry action</em> the selected folder's absolute path is passed
 * as the first argument to the script.  When used as a <em>toolbar action</em> the
 * launcher root folder path is passed instead.
 */
record CustomAction(
        String id,
        String iconPath,
        String scriptPath,
        String label,
        String tooltip)
{
    /** Display name – falls back to {@code id} when {@code label} is blank or {@code null}. */
    String effectiveLabel()
    {
        return (label != null && !label.isBlank()) ? label : id;
    }

    /** Tooltip text – falls back to {@link #effectiveLabel()} when {@code tooltip} is blank or {@code null}. */
    String effectiveTooltip()
    {
        return (tooltip != null && !tooltip.isBlank()) ? tooltip : effectiveLabel();
    }

    /**
     * Loads the icon from {@link #iconPath} and scales it to {@code w × h} pixels.
     * Returns {@code null} when the path is blank or the image cannot be read.
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

