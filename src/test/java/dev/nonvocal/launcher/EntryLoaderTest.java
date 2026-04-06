package dev.nonvocal.launcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link EntryLoader} – entry classification, default sort order,
 * and priority-list reordering. Uses {@code @TempDir} to build real (temporary) file structures.
 */
class EntryLoaderTest
{
    // ── isScript() ────────────────────────────────────────────────────────────

    @Test void isScript_bat(@TempDir File d) throws IOException { assertTrue(EntryLoader.isScript(touch(d, "x.bat"))); }
    @Test void isScript_cmd(@TempDir File d) throws IOException { assertTrue(EntryLoader.isScript(touch(d, "x.cmd"))); }
    @Test void isScript_ps1(@TempDir File d) throws IOException { assertTrue(EntryLoader.isScript(touch(d, "x.ps1"))); }
    @Test void isScript_vbs(@TempDir File d) throws IOException { assertTrue(EntryLoader.isScript(touch(d, "x.vbs"))); }
    @Test void isScript_sh (@TempDir File d) throws IOException { assertTrue(EntryLoader.isScript(touch(d, "x.sh"))); }
    @Test void isScript_js (@TempDir File d) throws IOException { assertTrue(EntryLoader.isScript(touch(d, "x.js"))); }

    @Test
    void isScript_caseInsensitive(@TempDir File d) throws IOException
    {
        assertTrue(EntryLoader.isScript(touch(d, "SETUP.BAT")));
        assertTrue(EntryLoader.isScript(touch(d, "Deploy.PS1")));
    }

    @Test void isScript_exe_false(@TempDir File d) throws IOException { assertFalse(EntryLoader.isScript(touch(d, "app.exe"))); }
    @Test void isScript_txt_false(@TempDir File d) throws IOException { assertFalse(EntryLoader.isScript(touch(d, "readme.txt"))); }
    @Test void isScript_lnk_false(@TempDir File d) throws IOException { assertFalse(EntryLoader.isScript(touch(d, "link.lnk"))); }
    @Test void isScript_noExtension_false(@TempDir File d) throws IOException { assertFalse(EntryLoader.isScript(touch(d, "Makefile"))); }

    // ── load() – empty folder ─────────────────────────────────────────────────

    @Test
    void load_emptyFolder_returnsEmptyList(@TempDir File dir)
    {
        assertTrue(EntryLoader.load(dir, LauncherConfig.empty()).isEmpty());
    }

    // ── load() – classification ───────────────────────────────────────────────

    @Test
    void load_classifiesScript(@TempDir File dir) throws IOException
    {
        touch(dir, "setup.bat");

        List<LaunchEntry> entries = EntryLoader.load(dir, LauncherConfig.empty());

        assertEquals(1, entries.size());
        LaunchEntry entry = entries.getFirst();
        assertEquals(EntryType.SCRIPT, entry.type());
        assertEquals("setup.bat", entry.file().getName());
        assertNull(entry.iconFile());
    }

    @Test
    void load_classifiesPlainFolder(@TempDir File dir)
    {
        new File(dir, "myFolder").mkdir();

        List<LaunchEntry> entries = EntryLoader.load(dir, LauncherConfig.empty());

        assertEquals(1, entries.size());
        LaunchEntry entry = entries.getFirst();
        assertEquals(EntryType.PLAIN_FOLDER, entry.type());
        assertNull(entry.iconFile());
    }

    @Test
    void load_classifiesAppFolder_withLnk(@TempDir File dir) throws IOException
    {
        File appDir = mkdir(dir, "MyApp");
        File lnk    = touch(appDir, "MyApp.lnk");

        List<LaunchEntry> entries = EntryLoader.load(dir, LauncherConfig.empty());

        assertEquals(1, entries.size());
        LaunchEntry entry = entries.getFirst();
        assertEquals(EntryType.APP_FOLDER, entry.type());
        assertEquals(lnk, entry.iconFile());
    }

    @Test
    void load_classifiesAppFolder_withFallbackExe(@TempDir File dir) throws IOException
    {
        File appDir  = mkdir(dir, "SapApp");
        File fallback = new File(appDir, EntryLoader.FALLBACK_EXE);
        fallback.getParentFile().mkdirs();
        fallback.createNewFile();

        List<LaunchEntry> entries = EntryLoader.load(dir, LauncherConfig.empty());

        assertEquals(1, entries.size());
        LaunchEntry entry = entries.getFirst();
        assertEquals(EntryType.APP_FOLDER, entry.type());
        assertEquals(fallback, entry.iconFile());
    }

    @Test
    void load_lnkTakesPrecedenceOverFallbackExe(@TempDir File dir) throws IOException
    {
        File appDir  = mkdir(dir, "HybridApp");
        File lnk     = touch(appDir, "App.lnk");
        File fallback = new File(appDir, EntryLoader.FALLBACK_EXE);
        fallback.getParentFile().mkdirs();
        fallback.createNewFile();

        List<LaunchEntry> entries = EntryLoader.load(dir, LauncherConfig.empty());

        LaunchEntry entry = entries.getFirst();
        assertEquals(EntryType.APP_FOLDER, entry.type());
        assertEquals(lnk, entry.iconFile(), ".lnk must take precedence over fallback exe");
    }

    @Test
    void load_nonScriptFilesAtRoot_areIgnored(@TempDir File dir) throws IOException
    {
        touch(dir, "readme.txt");
        touch(dir, "data.xml");
        touch(dir, "binary.exe");

        assertTrue(EntryLoader.load(dir, LauncherConfig.empty()).isEmpty(),
                "Non-script files at the root level should be ignored");
    }

    // ── load() – default sort order ───────────────────────────────────────────

    @Test
    void load_defaultSortOrder_scripts_then_apps_then_plain(@TempDir File dir) throws IOException
    {
        new File(dir, "zFolder").mkdir();            // plain

        File appDir = mkdir(dir, "aApp");            // app folder
        touch(appDir, "app.lnk");

        touch(dir, "mScript.bat");                   // script

        List<LaunchEntry> entries = EntryLoader.load(dir, LauncherConfig.empty());

        assertEquals(3, entries.size());
        assertEquals(EntryType.SCRIPT,       entries.getFirst().type());
        assertEquals(EntryType.APP_FOLDER,   entries.get(1).type());
        assertEquals(EntryType.PLAIN_FOLDER, entries.get(2).type());
    }

    @Test
    void load_alphabeticalWithinCategory(@TempDir File dir) throws IOException
    {
        touch(dir, "c.bat");
        touch(dir, "a.bat");
        touch(dir, "b.bat");

        List<LaunchEntry> entries = EntryLoader.load(dir, LauncherConfig.empty());

        assertEquals("a.bat", entries.getFirst().file().getName());
        assertEquals("b.bat", entries.get(1).file().getName());
        assertEquals("c.bat", entries.get(2).file().getName());
    }

    @Test
    void load_alphabeticalIsCaseInsensitive(@TempDir File dir) throws IOException
    {
        touch(dir, "Bravo.bat");
        touch(dir, "alpha.bat");
        touch(dir, "CHARLIE.bat");

        List<LaunchEntry> entries = EntryLoader.load(dir, LauncherConfig.empty());

        assertEquals("alpha.bat",   entries.getFirst().file().getName());
        assertEquals("Bravo.bat",   entries.get(1).file().getName());
        assertEquals("CHARLIE.bat", entries.get(2).file().getName());
    }

    // ── load() – priority list ────────────────────────────────────────────────

    @Test
    void load_priorityList_reordersEntries(@TempDir File dir) throws IOException
    {
        touch(dir, "alpha.bat");
        touch(dir, "beta.bat");
        touch(dir, "gamma.bat");

        LauncherConfig cfg = withPriority("gamma.bat", "alpha.bat");

        List<LaunchEntry> entries = EntryLoader.load(dir, cfg);

        assertEquals("gamma.bat", entries.getFirst().file().getName());
        assertEquals("alpha.bat", entries.get(1).file().getName());
        assertEquals("beta.bat",  entries.get(2).file().getName()); // remainder in default alpha order
    }

    @Test
    void load_priorityList_unknownNamesAreSkipped(@TempDir File dir) throws IOException
    {
        touch(dir, "real.bat");

        LauncherConfig cfg = withPriority("ghost.bat", "real.bat", "phantom.bat");

        List<LaunchEntry> entries = EntryLoader.load(dir, cfg);

        assertEquals(1, entries.size());
        assertEquals("real.bat", entries.getFirst().file().getName());
    }

    @Test
    void load_priorityList_null_usesDefaultOrder(@TempDir File dir) throws IOException
    {
        touch(dir, "b.bat");
        touch(dir, "a.bat");

        // empty() has priorityList = null
        List<LaunchEntry> entries = EntryLoader.load(dir, LauncherConfig.empty());

        assertEquals("a.bat", entries.getFirst().file().getName());
        assertEquals("b.bat", entries.get(1).file().getName());
    }

    @Test
    void load_priorityList_empty_usesDefaultOrder(@TempDir File dir) throws IOException
    {
        touch(dir, "b.bat");
        touch(dir, "a.bat");

        LauncherConfig cfg = new LauncherConfig(
                null, null, null, null, List.of(), null, null, null, null, null, null, null, null, null, null, null, null, null);

        List<LaunchEntry> entries = EntryLoader.load(dir, cfg);

        assertEquals("a.bat", entries.getFirst().file().getName());
        assertEquals("b.bat", entries.get(1).file().getName());
    }

    @Test
    void load_priorityList_partialCoverage_remainderInDefaultOrder(@TempDir File dir) throws IOException
    {
        touch(dir, "c.bat");
        touch(dir, "a.bat");
        touch(dir, "b.bat");

        // Only pin "c" at top; a and b should follow in alpha order
        LauncherConfig cfg = withPriority("c.bat");

        List<LaunchEntry> entries = EntryLoader.load(dir, cfg);

        assertEquals("c.bat", entries.getFirst().file().getName());
        assertEquals("a.bat", entries.get(1).file().getName());
        assertEquals("b.bat", entries.get(2).file().getName());
    }

    // ── load() – hidden entries ───────────────────────────────────────────────

    @Test
    void load_hiddenEntries_excludesMatchingFiles(@TempDir File dir) throws IOException
    {
        touch(dir, "visible.bat");
        touch(dir, "hidden.bat");

        LauncherConfig cfg = withHidden("hidden.bat");
        List<LaunchEntry> entries = EntryLoader.load(dir, cfg);

        assertEquals(1, entries.size());
        assertEquals("visible.bat", entries.getFirst().file().getName());
    }

    @Test
    void load_hiddenEntries_excludesMatchingFolders(@TempDir File dir)
    {
        new File(dir, "visible").mkdir();
        new File(dir, "hidden").mkdir();

        LauncherConfig cfg = withHidden("hidden");
        List<LaunchEntry> entries = EntryLoader.load(dir, cfg);

        assertEquals(1, entries.size());
        assertEquals("visible", entries.getFirst().file().getName());
    }

    @Test
    void load_hiddenEntries_null_showsAll(@TempDir File dir) throws IOException
    {
        touch(dir, "a.bat");
        touch(dir, "b.bat");

        // hiddenEntries = null → nothing filtered
        List<LaunchEntry> entries = EntryLoader.load(dir, LauncherConfig.empty());
        assertEquals(2, entries.size());
    }

    @Test
    void load_hiddenEntries_unknownName_isIgnored(@TempDir File dir) throws IOException
    {
        touch(dir, "real.bat");

        LauncherConfig cfg = withHidden("ghost.bat");
        List<LaunchEntry> entries = EntryLoader.load(dir, cfg);

        assertEquals(1, entries.size());
        assertEquals("real.bat", entries.getFirst().file().getName());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Creates an empty file in {@code parent} and returns it. */
    private static File touch(File parent, String name) throws IOException
    {
        File f = new File(parent, name);
        f.createNewFile();
        return f;
    }

    /** Creates a directory in {@code parent} and returns it. */
    private static File mkdir(File parent, String name)
    {
        File d = new File(parent, name);
        d.mkdir();
        return d;
    }

    /** Builds a config that has only the given priority list set. */
    private static LauncherConfig withPriority(String... names)
    {
        return new LauncherConfig(null, null, null, null, List.of(names), null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    /** Builds a config that has only the given hidden entries set. */
    private static LauncherConfig withHidden(String... names)
    {
        return new LauncherConfig(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, List.of(names), null);
    }
}

