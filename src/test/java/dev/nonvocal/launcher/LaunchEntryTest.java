package dev.nonvocal.launcher;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link LaunchEntry} record – constructors, accessors, equality, and toString.
 */
class LaunchEntryTest
{
    // ── Constructors ──────────────────────────────────────────────────────────

    @Test
    void threeArgConstructor_storesAllFields()
    {
        File file = new File("MyApp");
        File icon = new File("MyApp/App.lnk");
        LaunchEntry entry = new LaunchEntry(file, EntryType.APP_FOLDER, icon);

        assertEquals(file,              entry.file());
        assertEquals(EntryType.APP_FOLDER, entry.type());
        assertEquals(icon,              entry.iconFile());
    }

    @Test
    void twoArgConstructor_setsIconFileToNull()
    {
        File file  = new File("setup.bat");
        LaunchEntry entry = new LaunchEntry(file, EntryType.SCRIPT);

        assertEquals(file,          entry.file());
        assertEquals(EntryType.SCRIPT, entry.type());
        assertNull(entry.iconFile(), "Convenience constructor must set iconFile to null");
    }

    // ── toString() ────────────────────────────────────────────────────────────

    @Test
    void toString_returnsFileNameOnly()
    {
        LaunchEntry entry = new LaunchEntry(new File(new File("Apps"), "MyApp"), EntryType.PLAIN_FOLDER);
        assertEquals("MyApp", entry.toString());
    }

    @Test
    void toString_script_returnsFileNameWithExtension()
    {
        LaunchEntry entry = new LaunchEntry(new File(new File("Scripts"), "deploy.bat"), EntryType.SCRIPT);
        assertEquals("deploy.bat", entry.toString());
    }

    // ── Record equality & hash ────────────────────────────────────────────────

    @Test
    void recordEquality_sameArguments_areEqual()
    {
        File f = new File("app");
        LaunchEntry a = new LaunchEntry(f, EntryType.SCRIPT, null);
        LaunchEntry b = new LaunchEntry(f, EntryType.SCRIPT, null);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void recordEquality_differentType_notEqual()
    {
        File f = new File("x");
        assertNotEquals(new LaunchEntry(f, EntryType.SCRIPT),
                        new LaunchEntry(f, EntryType.PLAIN_FOLDER));
    }

    @Test
    void recordEquality_differentFile_notEqual()
    {
        assertNotEquals(new LaunchEntry(new File("a"), EntryType.SCRIPT),
                        new LaunchEntry(new File("b"), EntryType.SCRIPT));
    }

    @Test
    void recordEquality_differentIconFile_notEqual()
    {
        File f    = new File("MyApp");
        File icon1 = new File("MyApp/a.lnk");
        File icon2 = new File("MyApp/b.lnk");

        assertNotEquals(new LaunchEntry(f, EntryType.APP_FOLDER, icon1),
                        new LaunchEntry(f, EntryType.APP_FOLDER, icon2));
    }

    // ── EntryType enum sanity ─────────────────────────────────────────────────

    @Test
    void entryType_allValuesPresent()
    {
        EntryType[] values = EntryType.values();
        assertEquals(3, values.length);
        assertNotNull(EntryType.valueOf("SCRIPT"));
        assertNotNull(EntryType.valueOf("APP_FOLDER"));
        assertNotNull(EntryType.valueOf("PLAIN_FOLDER"));
    }
}

