package dev.nonvocal.launcher;

import java.io.File;
import java.util.*;

/**
 * Scans the root folder and builds an ordered list of {@link LaunchEntry} objects.
 * Pure logic – no UI dependencies.
 */
class EntryLoader
{
    /** Relative path of the fallback executable inside an application folder. */
    static final String FALLBACK_EXE = "basis\\sys\\win\\bin\\dsc_StartPlm.exe";

    private static final Set<String> SCRIPT_EXTENSIONS =
            Set.of(".bat", ".cmd", ".ps1", ".vbs", ".sh", ".js");

    private EntryLoader() {}

    /**
     * Loads and sorts all launch entries from {@code baseFolder}.
     * Entries in {@code config.priorityList()} are placed first (in priority order);
     * all remaining entries follow in the default sort (scripts → app folders → plain folders, A–Z).
     */
    static List<LaunchEntry> load(File baseFolder, LauncherConfig config)
    {
        List<LaunchEntry> scripts = new ArrayList<>();
        List<LaunchEntry> apps    = new ArrayList<>();
        List<LaunchEntry> plain   = new ArrayList<>();

        File[] children = baseFolder.listFiles();
        if (children != null)
        {
            Arrays.sort(children, Comparator.comparing(f -> f.getName().toLowerCase(Locale.ROOT)));
            for (File f : children)
            {
                if (f.isDirectory())
                {
                    File iconSrc = findAppIconSource(f);
                    if (iconSrc != null) apps.add(new LaunchEntry(f, EntryType.APP_FOLDER, iconSrc));
                    else                 plain.add(new LaunchEntry(f, EntryType.PLAIN_FOLDER));
                }
                else if (isScript(f))
                {
                    scripts.add(new LaunchEntry(f, EntryType.SCRIPT));
                }
            }
        }

        List<LaunchEntry> natural = new ArrayList<>(scripts);
        natural.addAll(apps);
        natural.addAll(plain);

        List<String> priority = config.priorityList();
        if (priority == null || priority.isEmpty()) return natural;

        Map<String, LaunchEntry> byName = new LinkedHashMap<>();
        for (LaunchEntry e : natural) byName.put(e.file().getName(), e);

        List<LaunchEntry> result = new ArrayList<>();
        for (String name : priority)
        {
            LaunchEntry e = byName.remove(name);
            if (e != null) result.add(e);
        }
        result.addAll(byName.values());
        return result;
    }

    /** Returns {@code true} if {@code f} has a recognised script file extension. */
    static boolean isScript(File f)
    {
        String name = f.getName().toLowerCase(Locale.ROOT);
        return SCRIPT_EXTENSIONS.stream().anyMatch(name::endsWith);
    }

    /**
     * Returns the first {@code .lnk} file found in {@code dir}, or the fallback exe,
     * or {@code null} if neither exists (→ plain folder).
     */
    private static File findAppIconSource(File dir)
    {
        File[] children = dir.listFiles();
        if (children != null)
        {
            for (File f : children)
                if (f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".lnk")) return f;
        }
        File fallback = new File(dir, FALLBACK_EXE);
        return fallback.isFile() ? fallback : null;
    }
}

