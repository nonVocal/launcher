package dev.nonvocal.launcher;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A user-defined action that can appear in the entry action bar, the toolbar, or both.
 *
 * <ul>
 *   <li>{@code id}         – unique key; referenced in {@code actionOrder} / {@code toolbarActions}</li>
 *   <li>{@code scope}      – where the action may appear: {@code "ENTRY"}, {@code "TOOLBAR"}, or {@code "BOTH"}</li>
 *   <li>{@code iconPath}   – absolute path to a PNG/JPG/GIF icon file (may be {@code null})</li>
 *   <li>{@code scriptPath} – absolute path to the script or executable to run</li>
 *   <li>{@code label}      – display name; falls back to {@code id} when blank or {@code null}</li>
 *   <li>{@code tooltip}    – tooltip text; falls back to the effective label when blank or {@code null}</li>
 * </ul>
 *
 * When used as a <em>list-entry action</em> the selected folder's absolute path is passed
 * as the first argument to the script.  When used as a <em>toolbar action</em> the
 * launcher root folder path is passed instead.
 * <p>
 * In both cases the following environment variables are additionally set so that scripts
 * can inspect the full entry context without needing to parse additional arguments:
 * <ul>
 *   <li>{@code NV_LAUNCHER_FOLDER} – absolute path of the launcher root folder</li>
 *   <li>{@code NV_ENTRY_PATH}      – same as the first CLI argument</li>
 *   <li>{@code NV_ENTRY_NAME}      – file/folder name of the entry</li>
 *   <li>{@code NV_ENTRY_TYPE}      – {@code SCRIPT}, {@code APP_FOLDER}, or {@code PLAIN_FOLDER}
 *                                    (empty when triggered from the toolbar with no selection)</li>
 *   <li>{@code NV_APP_TYPE_ID}     – ID of the matched application type, or empty string</li>
 *   <li>{@code NV_ICON_FILE}       – absolute path of the icon file, or empty string</li>
 * </ul>
 */
record CustomAction(
        String id,
        String scope,
        String iconPath,
        String scriptPath,
        String label,
        String tooltip,
        boolean appliesToEntry,
        boolean appliesToToolbar,
        String effectiveLabel,
        String effectiveTooltip)
{
    // ── Scope constants ───────────────────────────────────────────────────────

    /** Action appears only in the per-entry action bar. */
    static final String SCOPE_ENTRY   = "ENTRY";

    /** Action appears only in the toolbar. */
    static final String SCOPE_TOOLBAR = "TOOLBAR";

    /** Action appears in both the per-entry action bar and the toolbar. */
    static final String SCOPE_BOTH    = "BOTH";

    private static final Map<String, Icon> ICON_CACHE = new HashMap<>();

    CustomAction(
            String id,
            String scope,
            String iconPath,
            String scriptPath,
            String label,
            String tooltip)
    {
        this(id,scope,iconPath,scriptPath,label,tooltip, null, null, null, null);
    }

    CustomAction(
            String id,
            String scope,
            String iconPath,
            String scriptPath,
            String label,
            String tooltip,
            Boolean appliesToEntry,
            Boolean appliesToToolbar,
            String effectiveLabel,
            String effectiveTooltip)
    {
        this(id, scope, iconPath, scriptPath, label, tooltip,
                Objects.requireNonNullElseGet(appliesToEntry, () -> computeAppliesToEntry(scope)).booleanValue(),
                Objects.requireNonNullElseGet(appliesToToolbar, () -> computeAppliesToToolbar(scope)).booleanValue(),
                effectiveLabel, effectiveTooltip);
    }

    CustomAction
    {
        effectiveLabel = computeEffectiveLabel(effectiveLabel, id);
        effectiveTooltip = computeEffectiveTooltip(effectiveTooltip, effectiveLabel);
    }

    // ── Scope helpers ─────────────────────────────────────────────────────────

    /** {@code true} when this action may appear in the per-entry action bar. */
    static boolean computeAppliesToEntry(String scope)
    {
        return scope == null || SCOPE_ENTRY.equals(scope) || SCOPE_BOTH.equals(scope);
    }

    /** {@code true} when this action may appear in the toolbar. */
    static boolean computeAppliesToToolbar(String scope)
    {
        return scope == null || SCOPE_TOOLBAR.equals(scope) || SCOPE_BOTH.equals(scope);
    }

    // ── Label / tooltip helpers ───────────────────────────────────────────────

    /** Display name – falls back to {@code id} when {@code label} is blank or {@code null}. */
    static String computeEffectiveLabel(String label, String defaultValue)
    {
        return (label != null && !label.isBlank()) ? label : defaultValue;
    }

    /** Tooltip text – falls back to {@link #effectiveLabel()} when {@code tooltip} is blank or {@code null}. */
    static String computeEffectiveTooltip(String tooltip, String defaultValue)
    {
        return (tooltip != null && !tooltip.isBlank()) ? tooltip : defaultValue;
    }

    /**
     * Loads the icon from {@link #iconPath} and scales it to {@code w × h} pixels.
     * Returns {@code null} when the path is blank or the image cannot be read.
     */
    ImageIcon loadIcon(int w, int h)
    {
        if (iconPath == null || iconPath.isBlank()) return null;
        return ICON_CACHE.computeIfAbsent(iconPath, ip ->
        {
            try
            {
                BufferedImage raw = ImageIO.read(new File(iconPath));
                if (raw == null) return Icon.NULL;
                return new Icon(new ImageIcon(raw.getScaledInstance(w, h, Image.SCALE_SMOOTH)));
            } catch (IOException e)
            {
                return Icon.NULL;
            }
        }).icon();
    }

    record Icon(ImageIcon icon)
    {
        final static Icon NULL = new Icon(null);

    }
}
