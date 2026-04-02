package dev.nonvocal.launcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Immutable configuration record with three-level override support.
 * <ol>
 *   <li>Global:   {@code %APPDATA%\nvLauncher\config.json}</li>
 *   <li>Instance: {@code %APPDATA%\nvLauncher\{launcherId}\config.json}</li>
 *   <li>Explicit: path supplied via {@code --config=<path>}</li>
 * </ol>
 * Each level overrides only the fields that are explicitly present.
 * All fields are nullable; {@code null} means "not set at this level".
 * Call {@link #withDefaults()} on the final merged config to fill gaps.
 */
record LauncherConfig(
        String              rootFolder,
        Boolean             startMinimized,
        Integer             windowWidth,
        Integer             windowHeight,
        List<String>        priorityList,
        String              explorer,
        String              editor,
        List<String>        actionOrder,
        String              entryButtonStyle,
        Boolean             showContextMenu,
        List<String>        toolbarActions,
        List<CustomAction>  customActions,
        List<AppType>       appTypes,
        Map<String, String> appTypeAssignments,   // folderName → appType id
        String              theme,                // "light", "dark", null/"system"
        String              accentColor)          // hex string e.g. "#0078D7", null = default
{
    // ── Static paths ───────────────────────────────────────────────────────────

    /** Base config directory: {@code %APPDATA%\nvLauncher} */
    static final File CONFIG_DIR;
    static
    {
        String appData = System.getenv("APPDATA");
        if (appData == null) appData = System.getProperty("user.home");
        CONFIG_DIR = new File(appData, "nvLauncher");
    }

    static File globalConfigFile()
    {
        return new File(CONFIG_DIR, "config.json");
    }

    static File instanceConfigFile(String launcherId)
    {
        return new File(new File(CONFIG_DIR, launcherId), "config.json");
    }

    // ── Factory methods ────────────────────────────────────────────────────────

    /** All fields null – represents "nothing set at this level". */
    static LauncherConfig empty()
    {
        return new LauncherConfig(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    /** Hardcoded application defaults (all fields non-null). */
    static LauncherConfig defaults()
    {
        return new LauncherConfig(null, false, 560, 680, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Loads a config from {@code file}.
     * Returns {@link #empty()} if the file does not exist or cannot be read.
     * Fields missing in the file are left null.
     */
    static LauncherConfig loadFile(File file)
    {
        if (!file.exists()) return empty();
        try
        {
            return parse(Files.readString(file.toPath()));
        }
        catch (IOException e)
        {
            return empty();
        }
    }

    // ── Merging ────────────────────────────────────────────────────────────────

    /**
     * Returns a new config in which every non-null field of {@code this}
     * overrides the corresponding field from {@code base}.
     * Use as: {@code override.mergeOver(base)}.
     */
    LauncherConfig mergeOver(LauncherConfig base)
    {
        return new LauncherConfig(
                rootFolder          != null ? rootFolder          : base.rootFolder,
                startMinimized      != null ? startMinimized      : base.startMinimized,
                windowWidth         != null ? windowWidth         : base.windowWidth,
                windowHeight        != null ? windowHeight        : base.windowHeight,
                priorityList        != null ? priorityList        : base.priorityList,
                explorer            != null ? explorer            : base.explorer,
                editor              != null ? editor              : base.editor,
                actionOrder         != null ? actionOrder         : base.actionOrder,
                entryButtonStyle    != null ? entryButtonStyle    : base.entryButtonStyle,
                showContextMenu     != null ? showContextMenu     : base.showContextMenu,
                toolbarActions      != null ? toolbarActions      : base.toolbarActions,
                customActions       != null ? customActions       : base.customActions,
                appTypes            != null ? appTypes            : base.appTypes,
                appTypeAssignments  != null ? appTypeAssignments  : base.appTypeAssignments,
                theme               != null ? theme               : base.theme,
                accentColor         != null ? accentColor         : base.accentColor);
    }

    /**
     * Returns a new config where every field that is still null is filled
     * with the hardcoded application default.
     */
    LauncherConfig withDefaults()
    {
        return new LauncherConfig(
                rootFolder,
                startMinimized != null ? startMinimized : false,
                windowWidth    != null ? windowWidth    : 560,
                windowHeight   != null ? windowHeight   : 680,
                priorityList,
                explorer,
                editor,
                actionOrder,
                entryButtonStyle,
                showContextMenu,
                toolbarActions,
                customActions,
                appTypes,
                appTypeAssignments,
                theme,
                accentColor);
    }

    // ── Persistence ────────────────────────────────────────────────────────────

    /**
     * Saves this config to {@code file}, creating parent directories as needed.
     * Only non-null fields are written; silently ignores I/O errors.
     */
    void save(File file)
    {
        try
        {
            file.getParentFile().mkdirs();
            Files.writeString(file.toPath(), toJson());
        }
        catch (IOException ignored) {}
    }

    // ── Serialisation ──────────────────────────────────────────────────────────

    private String toJson()
    {
        List<String> lines = new ArrayList<>();
        if (rootFolder       != null) lines.add("  \"rootFolder\": "       + jsonStr(rootFolder));
        if (startMinimized   != null) lines.add("  \"startMinimized\": "   + startMinimized);
        if (windowWidth      != null) lines.add("  \"windowWidth\": "      + windowWidth);
        if (windowHeight     != null) lines.add("  \"windowHeight\": "     + windowHeight);
        if (explorer         != null) lines.add("  \"explorer\": "         + jsonStr(explorer));
        if (editor           != null) lines.add("  \"editor\": "           + jsonStr(editor));
        if (entryButtonStyle != null) lines.add("  \"entryButtonStyle\": " + jsonStr(entryButtonStyle));
        if (showContextMenu  != null) lines.add("  \"showContextMenu\": "  + showContextMenu);
        if (theme            != null) lines.add("  \"theme\": "            + jsonStr(theme));
        if (accentColor      != null) lines.add("  \"accentColor\": "      + jsonStr(accentColor));
        if (priorityList != null && !priorityList.isEmpty())
        {
            lines.add(jsonStrList("priorityList", priorityList));
        }
        if (actionOrder != null && !actionOrder.isEmpty())
        {
            lines.add(jsonStrList("actionOrder", actionOrder));
        }
        if (toolbarActions != null && !toolbarActions.isEmpty())
        {
            lines.add(jsonStrList("toolbarActions", toolbarActions));
        }
        if (customActions != null && !customActions.isEmpty())
        {
            lines.add(jsonCustomActions(customActions));
        }
        if (appTypes != null && !appTypes.isEmpty())
        {
            lines.add(jsonAppTypes(appTypes));
        }
        if (appTypeAssignments != null && !appTypeAssignments.isEmpty())
        {
            lines.add(jsonAppTypeAssignments(appTypeAssignments));
        }
        return "{\n" + String.join(",\n", lines) + "\n}";
    }

    private static String jsonCustomActions(List<CustomAction> actions)
    {
        StringBuilder sb = new StringBuilder("  \"customActions\": [\n");
        for (int i = 0; i < actions.size(); i++)
        {
            CustomAction a = actions.get(i);
            sb.append("    {");
            sb.append("\"id\": ").append(jsonStr(a.id()));
            sb.append(", \"scope\": ").append(jsonStr(a.scope() != null ? a.scope() : CustomAction.SCOPE_BOTH));
            sb.append(", \"icon\": ").append(jsonStr(a.iconPath()));
            sb.append(", \"script\": ").append(jsonStr(a.scriptPath()));
            if (a.label()   != null) sb.append(", \"label\": ")   .append(jsonStr(a.label()));
            if (a.tooltip() != null) sb.append(", \"tooltip\": ") .append(jsonStr(a.tooltip()));
            sb.append("}");
            if (i < actions.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]");
        return sb.toString();
    }

    private static String jsonAppTypes(List<AppType> types)
    {
        StringBuilder sb = new StringBuilder("  \"appTypes\": [\n");
        for (int i = 0; i < types.size(); i++)
        {
            AppType t = types.get(i);
            sb.append("    {");
            sb.append("\"id\": ").append(jsonStr(t.id()));
            if (t.iconPath() != null)
                sb.append(", \"iconPath\": ").append(jsonStr(t.iconPath()));
            if (t.executablePaths() != null && !t.executablePaths().isEmpty())
            {
                sb.append(", \"executablePaths\": [");
                for (int j = 0; j < t.executablePaths().size(); j++)
                {
                    sb.append(jsonStr(t.executablePaths().get(j)));
                    if (j < t.executablePaths().size() - 1) sb.append(", ");
                }
                sb.append("]");
            }
            if (t.executableNames() != null && !t.executableNames().isEmpty())
            {
                sb.append(", \"executableNames\": [");
                for (int j = 0; j < t.executableNames().size(); j++)
                {
                    sb.append(jsonStr(t.executableNames().get(j)));
                    if (j < t.executableNames().size() - 1) sb.append(", ");
                }
                sb.append("]");
            }
            sb.append("}");
            if (i < types.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]");
        return sb.toString();
    }

    private static String jsonAppTypeAssignments(Map<String, String> assignments)
    {
        StringBuilder sb = new StringBuilder("  \"appTypeAssignments\": [\n");
        int i = 0;
        for (Map.Entry<String, String> e : assignments.entrySet())
        {
            sb.append("    {\"folder\": ").append(jsonStr(e.getKey()))
              .append(", \"type\": ").append(jsonStr(e.getValue())).append("}");
            if (i < assignments.size() - 1) sb.append(",");
            sb.append("\n");
            i++;
        }
        sb.append("  ]");
        return sb.toString();
    }

    private static String jsonStrList(String key, List<String> values)
    {
        StringBuilder sb = new StringBuilder("  \"" + key + "\": [\n");
        for (int i = 0; i < values.size(); i++)
        {
            sb.append("    ").append(jsonStr(values.get(i)));
            if (i < values.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]");
        return sb.toString();
    }

    private static String jsonStr(String s)
    {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    // ── Deserialisation (no external library) ─────────────────────────────────

    private static LauncherConfig parse(String json)
    {
        return new LauncherConfig(
                parseStr              (json, "rootFolder"),
                parseBool             (json, "startMinimized"),
                parseInt              (json, "windowWidth"),
                parseInt              (json, "windowHeight"),
                parseStrList          (json, "priorityList"),
                parseStr              (json, "explorer"),
                parseStr              (json, "editor"),
                parseStrList          (json, "actionOrder"),
                parseStr              (json, "entryButtonStyle"),
                parseBool             (json, "showContextMenu"),
                parseStrList          (json, "toolbarActions"),
                parseCustomActions    (json),
                parseAppTypes         (json),
                parseAppTypeAssignments(json),
                parseStr              (json, "theme"),
                parseStr              (json, "accentColor"));
    }

    // ── CustomAction deserialisation ──────────────────────────────────────────

    private static List<CustomAction> parseCustomActions(String json)
    {
        // Locate the customActions array
        int keyIdx = json.indexOf("\"customActions\"");
        if (keyIdx < 0) return null;
        int arrStart = json.indexOf('[', keyIdx);
        if (arrStart < 0) return null;

        // Find matching ] counting brackets
        int depth = 0, arrEnd = arrStart;
        for (int i = arrStart; i < json.length(); i++)
        {
            char c = json.charAt(i);
            if      (c == '[') depth++;
            else if (c == ']') { if (--depth == 0) { arrEnd = i; break; } }
        }
        String inner = json.substring(arrStart + 1, arrEnd).trim();
        if (inner.isEmpty()) return new ArrayList<>();

        List<CustomAction> result = new ArrayList<>();
        for (int i = 0; i < inner.length(); )
        {
            int start = inner.indexOf('{', i);
            if (start < 0) break;
            int end = findObjectEnd(inner, start);
            if (end < 0) break;
            String obj = inner.substring(start + 1, end);
            String id  = parseStr(obj, "id");
            if (id != null && !id.isBlank())
            {
                result.add(new CustomAction(id,
                        parseStr(obj, "scope"),
                        parseStr(obj, "icon"),
                        parseStr(obj, "script"),
                        parseStr(obj, "label"),
                        parseStr(obj, "tooltip")));
            }
            i = end + 1;
        }
        return result;
    }

    /** Finds the index of the closing {@code '}'} for the {@code '{'} at {@code start}. */
    private static int findObjectEnd(String s, int start)
    {
        int depth = 0;
        boolean inString = false;
        for (int i = start; i < s.length(); i++)
        {
            char c = s.charAt(i);
            if (inString) { if (c == '\\') { i++; } else if (c == '"') inString = false; }
            else if (c == '"') inString = true;
            else if (c == '{') depth++;
            else if (c == '}') { if (--depth == 0) return i; }
        }
        return -1;
    }

    private static List<AppType> parseAppTypes(String json)
    {
        int keyIdx = json.indexOf("\"appTypes\"");
        if (keyIdx < 0) return null;
        int arrStart = json.indexOf('[', keyIdx);
        if (arrStart < 0) return null;

        int depth = 0, arrEnd = arrStart;
        for (int i = arrStart; i < json.length(); i++)
        {
            char c = json.charAt(i);
            if      (c == '[') depth++;
            else if (c == ']') { if (--depth == 0) { arrEnd = i; break; } }
        }
        String inner = json.substring(arrStart + 1, arrEnd).trim();
        if (inner.isEmpty()) return new ArrayList<>();

        List<AppType> result = new ArrayList<>();
        for (int i = 0; i < inner.length(); )
        {
            int start = inner.indexOf('{', i);
            if (start < 0) break;
            int end = findObjectEnd(inner, start);
            if (end < 0) break;
            String obj = inner.substring(start + 1, end);
            String id = parseStr(obj, "id");
            if (id != null && !id.isBlank())
            {
                result.add(new AppType(id,
                        parseStr    (obj, "iconPath"),
                        parseStrList(obj, "executablePaths"),
                        parseStrList(obj, "executableNames")));
            }
            i = end + 1;
        }
        return result;
    }

    private static Map<String, String> parseAppTypeAssignments(String json)
    {
        int keyIdx = json.indexOf("\"appTypeAssignments\"");
        if (keyIdx < 0) return null;
        int arrStart = json.indexOf('[', keyIdx);
        if (arrStart < 0) return null;

        int depth = 0, arrEnd = arrStart;
        for (int i = arrStart; i < json.length(); i++)
        {
            char c = json.charAt(i);
            if      (c == '[') depth++;
            else if (c == ']') { if (--depth == 0) { arrEnd = i; break; } }
        }
        String inner = json.substring(arrStart + 1, arrEnd).trim();
        if (inner.isEmpty()) return new LinkedHashMap<>();

        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < inner.length(); )
        {
            int start = inner.indexOf('{', i);
            if (start < 0) break;
            int end = findObjectEnd(inner, start);
            if (end < 0) break;
            String obj    = inner.substring(start + 1, end);
            String folder = parseStr(obj, "folder");
            String type   = parseStr(obj, "type");
            if (folder != null && !folder.isBlank() && type != null && !type.isBlank())
                result.put(folder, type);
            i = end + 1;
        }
        return result.isEmpty() ? null : result;
    }

    private static String parseStr(String json, String key)
    {
        Matcher m = Pattern.compile(
                "\"" + key + "\"\\s*:\\s*(?:null|\"((?:[^\"\\\\]|\\\\.)*)\")").matcher(json);
        if (!m.find()) return null;
        String g = m.group(1);
        return g == null ? null : g.replace("\\\\", "\\").replace("\\\"", "\"");
    }

    private static Boolean parseBool(String json, String key)
    {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(true|false)").matcher(json);
        return m.find() ? Boolean.parseBoolean(m.group(1)) : null;
    }

    private static Integer parseInt(String json, String key)
    {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?\\d+)").matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    private static List<String> parseStrList(String json, String key)
    {
        Matcher m = Pattern.compile(
                "\"" + key + "\"\\s*:\\s*\\[([^]]*?)\\]",
                Pattern.DOTALL).matcher(json);
        if (!m.find()) return null;
        String inner = m.group(1).trim();
        if (inner.isEmpty()) return new ArrayList<>();
        List<String> result = new ArrayList<>();
        Matcher em = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(inner);
        while (em.find())
        {
            result.add(em.group(1).replace("\\\\", "\\").replace("\\\"", "\""));
        }
        return result;
    }
}

