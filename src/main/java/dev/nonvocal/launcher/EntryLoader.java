package dev.nonvocal.launcher;

import java.io.File;
import java.util.*;

/**
 * Scans the root folder and builds an ordered list of {@link LaunchEntry} objects.
 * Pure logic – no UI dependencies.
 */
class EntryLoader
{
    /** Relative path of the built-in fallback executable inside an application folder. */
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
        // Build fast-lookup structures from config
        Map<String, AppType> typeById = LinkedHashMap.newLinkedHashMap(
                config.appTypes() != null ? config.appTypes().size() : 0);
        if (config.appTypes() != null)
            for (AppType t : config.appTypes()) typeById.put(t.id(), t);

        Map<String, String> assignments = config.appTypeAssignments() != null
                ? config.appTypeAssignments() : Collections.emptyMap();

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
                    LaunchEntry entry = classifyDirectory(f, assignments, typeById);
                    if (entry.type() == EntryType.APP_FOLDER) apps.add(entry);
                    else                                       plain.add(entry);
                }
                else if (isScript(f))
                {
                    scripts.add(new LaunchEntry(f, EntryType.SCRIPT));
                }
            }
        }

        List<LaunchEntry> natural = new ArrayList<>(scripts.size() + apps.size() + plain.size());
        natural.addAll(scripts);
        natural.addAll(apps);
        natural.addAll(plain);

        List<String> priority = config.priorityList();
        if (priority == null || priority.isEmpty()) return natural;

        Map<String, LaunchEntry> byName = LinkedHashMap.newLinkedHashMap(natural.size());
        for (LaunchEntry e : natural) byName.put(e.file().getName(), e);

        List<LaunchEntry> result = new ArrayList<>(natural.size());
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
     * Classifies a directory using (in priority order):
     * <ol>
     *   <li>Explicit app-type assignment from config</li>
     *   <li>Auto-detection: scan all defined app types for a matching executable</li>
     *   <li>Built-in: first {@code .lnk} file found at the folder root</li>
     *   <li>Built-in: fallback {@code basis\sys\win\bin\dsc_StartPlm.exe}</li>
     *   <li>Plain folder (no executable found)</li>
     * </ol>
     */
    private static LaunchEntry classifyDirectory(
            File dir,
            Map<String, String> assignments,
            Map<String, AppType> typeById)
    {
        // 1. Explicit assignment
        String assignedId = assignments.get(dir.getName());
        if (assignedId != null)
        {
            AppType appType = typeById.get(assignedId);
            if (appType != null)
            {
                // iconFile = found executable (for system-icon extraction when no custom iconPath)
                File iconFile = appType.findExecutable(dir);
                return new LaunchEntry(dir, EntryType.APP_FOLDER, iconFile, appType);
            }
        }

        // 2. Auto-detection via defined app types
        for (AppType appType : typeById.values())
        {
            File exe = appType.findExecutable(dir);
            if (exe != null)
                return new LaunchEntry(dir, EntryType.APP_FOLDER, exe, appType);
        }

        // 3 & 4. Built-in detection
        File iconSrc = findAppIconSource(dir);
        if (iconSrc != null)
            return new LaunchEntry(dir, EntryType.APP_FOLDER, iconSrc, null);

        return new LaunchEntry(dir, EntryType.PLAIN_FOLDER);
    }

    /**
     * Built-in detection: returns the first {@code .lnk} at the folder root,
     * or the fallback exe, or {@code null} (→ plain folder).
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
