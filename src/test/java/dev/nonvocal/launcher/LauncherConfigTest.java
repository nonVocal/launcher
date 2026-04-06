package dev.nonvocal.launcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LauncherConfig} – merging, defaults, JSON round-trip, and edge cases.
 */
class LauncherConfigTest
{
    // ── empty() ──────────────────────────────────────────────────────────────

    @Test
    void empty_allFieldsNull()
    {
        LauncherConfig c = LauncherConfig.empty();
        assertNull(c.rootFolder());
        assertNull(c.startMinimized());
        assertNull(c.windowWidth());
        assertNull(c.windowHeight());
        assertNull(c.priorityList());
        assertNull(c.explorer());
        assertNull(c.editor());
        assertNull(c.actionOrder());
        assertNull(c.entryButtonStyle());
        assertNull(c.showContextMenu());
    }

    // ── defaults() ───────────────────────────────────────────────────────────

    @Test
    void defaults_hasCoreFieldsSet()
    {
        LauncherConfig d = LauncherConfig.defaults();
        assertEquals(false, d.startMinimized());
        assertEquals(560,   d.windowWidth());
        assertEquals(680,   d.windowHeight());
    }

    @Test
    void defaults_optionalFieldsRemainNull()
    {
        LauncherConfig d = LauncherConfig.defaults();
        assertNull(d.rootFolder());
        assertNull(d.explorer());
        assertNull(d.editor());
        assertNull(d.actionOrder());
        assertNull(d.entryButtonStyle());
        assertNull(d.showContextMenu());
    }

    // ── mergeOver() ──────────────────────────────────────────────────────────

    @Test
    void mergeOver_nonNullFieldsInOverrideWin()
    {
        LauncherConfig base = new LauncherConfig("base", false, 400, 400, null, null, null, null, null, null, null, null, null, null, null, null, null);
        LauncherConfig over = new LauncherConfig("over", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        LauncherConfig merged = over.mergeOver(base);

        assertEquals("over", merged.rootFolder());   // override wins
        assertEquals(false,  merged.startMinimized()); // falls back to base
        assertEquals(400,    merged.windowWidth());    // falls back to base
    }

    @Test
    void mergeOver_nullFieldsFallBackToBase()
    {
        LauncherConfig base = new LauncherConfig(
                "baseRoot", true, 800, 600, null, "exp.exe", "nano", null, "ICONS", true, null, null, null, null, null, null, null);
        LauncherConfig merged = LauncherConfig.empty().mergeOver(base);

        assertEquals("baseRoot",  merged.rootFolder());
        assertEquals(true,        merged.startMinimized());
        assertEquals(800,         merged.windowWidth());
        assertEquals(600,         merged.windowHeight());
        assertEquals("exp.exe",   merged.explorer());
        assertEquals("nano",      merged.editor());
        assertEquals("ICONS",     merged.entryButtonStyle());
        assertEquals(true,        merged.showContextMenu());
    }

    @Test
    void mergeOver_allFieldsOverridden()
    {
        LauncherConfig base = new LauncherConfig("base", false, 400, 400,
                List.of("a"), "e1", "ed1", List.of("X"), "ICONS", true, null, null, null, null, null, null, null);
        LauncherConfig over = new LauncherConfig("over", true, 800, 900,
                List.of("b"), "e2", "ed2", List.of("Y"), "HAMBURGER", false, null, null, null, null, null, null, null);
        LauncherConfig merged = over.mergeOver(base);

        assertEquals("over",                  merged.rootFolder());
        assertEquals(true,                    merged.startMinimized());
        assertEquals(800,                     merged.windowWidth());
        assertEquals(900,                     merged.windowHeight());
        assertEquals(List.of("b"),            merged.priorityList());
        assertEquals("e2",                    merged.explorer());
        assertEquals("ed2",                   merged.editor());
        assertEquals(List.of("Y"),            merged.actionOrder());
        assertEquals("HAMBURGER",             merged.entryButtonStyle());
        assertEquals(false,                   merged.showContextMenu());
    }

    @Test
    void mergeOver_threeLevels_explicit_overrides_instance_overrides_global()
    {
        LauncherConfig global   = new LauncherConfig("globalRoot", false, 400, 400, null, null, null, null, null, null, null, null, null, null, null, null, null);
        LauncherConfig instance = new LauncherConfig(null, null, 600, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        LauncherConfig explicit = new LauncherConfig("explicitRoot", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        LauncherConfig merged = explicit.mergeOver(instance.mergeOver(global)).withDefaults();

        assertEquals("explicitRoot", merged.rootFolder());  // from explicit
        assertEquals(600,            merged.windowWidth());   // from instance
        assertEquals(400,            merged.windowHeight());  // from global
        assertEquals(false,          merged.startMinimized());// from global
    }

    // ── withDefaults() ───────────────────────────────────────────────────────

    @Test
    void withDefaults_fillsMissingWindowDimensions()
    {
        LauncherConfig c = LauncherConfig.empty().withDefaults();
        assertEquals(false, c.startMinimized());
        assertEquals(560,   c.windowWidth());
        assertEquals(680,   c.windowHeight());
    }

    @Test
    void withDefaults_preservesExplicitValues()
    {
        LauncherConfig c = new LauncherConfig(
                null, true, 1024, 768, null, null, null, null, null, null, null, null, null, null, null, null, null).withDefaults();
        assertEquals(true, c.startMinimized());
        assertEquals(1024, c.windowWidth());
        assertEquals(768,  c.windowHeight());
    }

    @Test
    void withDefaults_doesNotOverwriteNullOptionals()
    {
        LauncherConfig c = LauncherConfig.empty().withDefaults();
        // Optional fields have no hardcoded default – remain null
        assertNull(c.rootFolder());
        assertNull(c.explorer());
        assertNull(c.editor());
        assertNull(c.actionOrder());
        assertNull(c.entryButtonStyle());
        assertNull(c.showContextMenu());
        assertNull(c.toolbarActions());
    }

    // ── save() + loadFile() round-trips ──────────────────────────────────────

    @Test
    void saveAndLoad_simpleScalarFields(@TempDir File tmp) throws IOException
    {
        File f = new File(tmp, "config.json");
        LauncherConfig original = new LauncherConfig(
                "C:\\Apps", true, 700, 500, null, "explorer.exe", "code", null, "HAMBURGER", false, null, null, null, null, null, null, null);
        original.save(f);

        LauncherConfig loaded = LauncherConfig.loadFile(f);
        assertEquals("C:\\Apps",      loaded.rootFolder());
        assertEquals(true,             loaded.startMinimized());
        assertEquals(700,              loaded.windowWidth());
        assertEquals(500,              loaded.windowHeight());
        assertEquals("explorer.exe",   loaded.explorer());
        assertEquals("code",           loaded.editor());
        assertEquals("HAMBURGER",      loaded.entryButtonStyle());
        assertEquals(false,            loaded.showContextMenu());
    }

    @Test
    void saveAndLoad_priorityList(@TempDir File tmp)
    {
        File f = new File(tmp, "config.json");
        List<String> priority = List.of("app-one", "app-two", "daily-script.bat");
        new LauncherConfig(null, null, null, null, priority, null, null, null, null, null, null, null, null, null, null, null, null).save(f);

        assertEquals(priority, LauncherConfig.loadFile(f).priorityList());
    }

    @Test
    void saveAndLoad_actionOrder(@TempDir File tmp)
    {
        File f = new File(tmp, "config.json");
        List<String> order = List.of("EXPLORE_ACTION", "DELETE_ACTION");
        new LauncherConfig(null, null, null, null, null, null, null, order, null, null, null, null, null, null, null, null, null).save(f);

        assertEquals(order, LauncherConfig.loadFile(f).actionOrder());
    }

    @Test
    void saveAndLoad_pathWithBackslashes(@TempDir File tmp)
    {
        File f = new File(tmp, "config.json");
        String path = "C:\\My Apps\\Sub Folder";
        new LauncherConfig(path, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null).save(f);

        assertEquals(path, LauncherConfig.loadFile(f).rootFolder());
    }

    @Test
    void saveAndLoad_stringWithEmbeddedQuotes(@TempDir File tmp)
    {
        File f = new File(tmp, "config.json");
        String editor = "C:\\Program Files\\\"My Editor\"\\edit.exe";
        new LauncherConfig(null, null, null, null, null, null, editor, null, null, null, null, null, null, null, null, null, null).save(f);

        assertEquals(editor, LauncherConfig.loadFile(f).editor());
    }

    @Test
    void save_createsParentDirectoriesAutomatically(@TempDir File tmp)
    {
        // Several levels deep – parent does not exist yet
        File f = new File(tmp, "deep/nested/dir/config.json");
        LauncherConfig.defaults().save(f);

        assertTrue(f.exists(), "Config file should have been created along with parent dirs");
    }

    // ── empty actionOrder is NOT serialized (design behaviour) ───────────────

    @Test
    void save_emptyActionOrder_isNotWrittenToJson(@TempDir File tmp) throws IOException
    {
        File f = new File(tmp, "config.json");
        new LauncherConfig(null, null, null, null, null, null, null, List.of(), null, null, null, null, null, null, null, null, null).save(f);

        // An empty array is not serialized; on load the field will be absent → null
        assertNull(LauncherConfig.loadFile(f).actionOrder(),
                "Empty actionOrder list must not be serialized (treated same as 'not set')");
    }

    // ── loadFile() edge cases ─────────────────────────────────────────────────

    @Test
    void loadFile_nonExistentFile_returnsEmpty()
    {
        File f = new File("absolutely_does_not_exist_xyz987.json");
        LauncherConfig c = LauncherConfig.loadFile(f);
        assertNull(c.rootFolder());
        assertNull(c.windowWidth());
    }

    @Test
    void loadFile_malformedJson_returnsEmptyWithoutThrowing(@TempDir File tmp) throws IOException
    {
        File f = new File(tmp, "bad.json");
        Files.writeString(f.toPath(), "{ this is [not valid json }");

        LauncherConfig c = LauncherConfig.loadFile(f);
        // No exception – all fields will be null (regex found nothing)
        assertNull(c.rootFolder());
        assertNull(c.startMinimized());
    }

    @Test
    void loadFile_partialJson_loadsOnlyPresentFields(@TempDir File tmp) throws IOException
    {
        File f = new File(tmp, "partial.json");
        Files.writeString(f.toPath(), "{ \"windowWidth\": 999, \"startMinimized\": true }");

        LauncherConfig c = LauncherConfig.loadFile(f);
        assertEquals(999,  c.windowWidth());
        assertEquals(true, c.startMinimized());
        assertNull(c.rootFolder());
        assertNull(c.windowHeight());
        assertNull(c.editor());
    }

    @Test
    void loadFile_showContextMenu_false(@TempDir File tmp) throws IOException
    {
        File f = new File(tmp, "ctx.json");
        Files.writeString(f.toPath(), "{ \"showContextMenu\": false }");

        assertEquals(false, LauncherConfig.loadFile(f).showContextMenu());
    }

    @Test
    void loadFile_emptyArrayForActionOrder_parsedAsEmptyList(@TempDir File tmp) throws IOException
    {
        File f = new File(tmp, "empty.json");
        Files.writeString(f.toPath(), "{ \"actionOrder\": [] }");

        List<String> order = LauncherConfig.loadFile(f).actionOrder();
        assertNotNull(order);
        assertTrue(order.isEmpty());
    }

    @Test
    void loadFile_nullStringValue_treatedAsMissing(@TempDir File tmp) throws IOException
    {
        File f = new File(tmp, "nullval.json");
        Files.writeString(f.toPath(), "{ \"rootFolder\": null, \"windowWidth\": 500 }");

        LauncherConfig c = LauncherConfig.loadFile(f);
        assertNull(c.rootFolder(), "Explicit JSON null should be treated as not-set (null)");
        assertEquals(500, c.windowWidth());
    }
}

