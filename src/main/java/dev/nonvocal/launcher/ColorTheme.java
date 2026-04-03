package dev.nonvocal.launcher;

import javax.swing.*;
import java.awt.*;

/**
 * Holds all theme-aware colours for the launcher's cell renderer.
 * <p>
 * Two static instances ({@link #dark} and {@link #light}) carry pure hardcoded
 * colour values. Call {@link #forCurrentLaf()} at renderer-construction time
 * to obtain an instance that also incorporates any UIManager overrides
 * ({@code List.background}, {@code List.selectionBackground}).
 */
final class ColorTheme
{
    final Color rowEven, rowOdd;
    final Color fgScript, fgFolder, fgPlain;
    final Color selBg;
    final Color actFg, actDel, actBg, actBord;
    final Color selActBg, selActBord;
    final Color sepColor;   // separator / border colour (fallback for "Separator.foreground")
    final Color searchFg;   // search-label foreground

    // ── Static instances ──────────────────────────────────────────────────────

    /** Hardcoded dark-theme palette. */
    static final ColorTheme dark  = buildDark();
    /** Hardcoded light-theme palette. */
    static final ColorTheme light = buildLight();

    // ── Factory ───────────────────────────────────────────────────────────────

    /**
     * Returns a {@link ColorTheme} resolved against the active Look-and-Feel.
     * UIManager colours take precedence; hardcoded values serve as fallbacks.
     */
    static ColorTheme forCurrentLaf()
    {
        boolean isDark = isDark();
        ColorTheme base = isDark ? dark : light;

        Color listBg   = UIManager.getColor("List.background");
        Color selBgRaw = UIManager.getColor("List.selectionBackground");
        Color sepRaw   = UIManager.getColor("Separator.foreground");

        if (listBg == null && selBgRaw == null && sepRaw == null) return base;

        Color rowOdd  = listBg != null ? listBg   : base.rowOdd;
        Color rowEven = isDark  ? blend(rowOdd, Color.WHITE, 0.04f) : base.rowEven;
        Color selBg   = selBgRaw != null ? selBgRaw : base.selBg;
        Color sepColor = sepRaw  != null ? sepRaw   : base.sepColor;

        return new ColorTheme(
                rowEven, rowOdd,
                base.fgScript, base.fgFolder, base.fgPlain,
                selBg,
                base.actFg, base.actDel, base.actBg, base.actBord,
                base.selActBg, base.selActBord,
                sepColor, base.searchFg);
    }

    // ── Theme detection ───────────────────────────────────────────────────────

    /**
     * Returns {@code true} when the active Look-and-Feel is a dark theme.
     * Uses FlatLaf's API; falls back to {@code false} if FlatLaf is not on the classpath.
     */
    static boolean isDark()
    {
        try { return com.formdev.flatlaf.FlatLaf.isLafDark(); }
        catch (Throwable ignored) { return false; }
    }

    // ── Colour helper ─────────────────────────────────────────────────────────

    /**
     * Linear colour blend: 0.0 → pure {@code base}, 1.0 → pure {@code overlay}.
     */
    static Color blend(Color baseColor, Color overlayColor, float ratio)
    {
        int base    = baseColor.getRGB();
        int overlay = overlayColor.getRGB();

        // Fixed-point ratio: 0..256 (use 256 so we can shift by 8 instead of dividing by 255)
        int t = (int)(ratio * 256f);  // 0..256
        int s = 256 - t;              // complement

        // Pack RG channels into the high/low halves of a long (with guard bits)
        // Layout: [00 R 00 G] — each channel in its own 16-bit lane, value in low 8 bits
        long baseRG    = ((long)(base    >> 16 & 0xFF) << 32) | (base    >> 8 & 0xFF);
        long overlayRG = ((long)(overlay >> 16 & 0xFF) << 32) | (overlay >> 8 & 0xFF);

        long rg   = (baseRG * s + overlayRG * t) >> 8;  // >> 8 undoes the *256 scale

        // Blue channel — plain int, no packing needed
        int  blue = ((base & 0xFF) * s + (overlay & 0xFF) * t) >> 8;

        return new Color(((int)(rg >> 32) & 0xFF) << 16   // R
                | ((int) rg        & 0xFF) << 8    // G
                |  blue);                           // B
    }

    // ── Builders ──────────────────────────────────────────────────────────────

    private static ColorTheme buildDark()
    {
        Color rowOdd = new Color(0x2B, 0x2D, 0x30);
        return new ColorTheme(
                blend(rowOdd, Color.WHITE, 0.04f), rowOdd,
                new Color(0x4F, 0xC1, 0xDA),   // fgScript
                new Color(0x85, 0xBE, 0x6C),   // fgFolder
                new Color(0xC8, 0xB8, 0xA6),   // fgPlain
                new Color(0x00, 0x78, 0xD7),   // selBg   (fallback)
                new Color(0x88, 0xBB, 0xFF),   // actFg
                new Color(0xFF, 0x70, 0x70),   // actDel
                new Color(0x3A, 0x3C, 0x47),   // actBg
                new Color(0x50, 0x52, 0x60),   // actBord
                new Color(0x2D, 0x6A, 0xB4),   // selActBg
                new Color(0x4D, 0x8A, 0xD4),   // selActBord
                new Color(0x4A, 0x4A, 0x4A),   // sepColor
                new Color(0x66, 0xAA, 0xFF));   // searchFg
    }

    private static ColorTheme buildLight()
    {
        return new ColorTheme(
                new Color(0xF4, 0xF6, 0xF8), Color.WHITE,
                new Color(0x1A, 0x5F, 0x7A),   // fgScript
                new Color(0x2E, 0x6B, 0x2E),   // fgFolder
                new Color(0x66, 0x55, 0x44),   // fgPlain
                new Color(0x00, 0x78, 0xD7),   // selBg   (fallback)
                new Color(0x33, 0x55, 0x99),   // actFg
                new Color(0xAA, 0x22, 0x22),   // actDel
                new Color(0xE8, 0xEA, 0xF4),   // actBg
                new Color(0xBB, 0xBB, 0xCC),   // actBord
                new Color(0x40, 0x90, 0xD7),   // selActBg
                new Color(0x80, 0xB8, 0xFF),   // selActBord
                new Color(0xCC, 0xCC, 0xCC),   // sepColor
                new Color(0x00, 0x50, 0xA0));  // searchFg
    }

    // ── Constructor ───────────────────────────────────────────────────────────

    private ColorTheme(
            Color rowEven,  Color rowOdd,
            Color fgScript, Color fgFolder, Color fgPlain,
            Color selBg,
            Color actFg,    Color actDel,   Color actBg,    Color actBord,
            Color selActBg, Color selActBord,
            Color sepColor, Color searchFg)
    {
        this.rowEven    = rowEven;
        this.rowOdd     = rowOdd;
        this.fgScript   = fgScript;
        this.fgFolder   = fgFolder;
        this.fgPlain    = fgPlain;
        this.selBg      = selBg;
        this.actFg      = actFg;
        this.actDel     = actDel;
        this.actBg      = actBg;
        this.actBord    = actBord;
        this.selActBg   = selActBg;
        this.selActBord = selActBord;
        this.sepColor   = sepColor;
        this.searchFg   = searchFg;
    }
}

