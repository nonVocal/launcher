# launcher

A lightweight Java Swing application that lets you browse and launch scripts and application folders from a single root directory.

## Features

- **Lists** scripts and sub-folders found at the **top level** of a chosen root folder, divided into three categories:
  - **Scripts** – recognized script files (.bat, .cmd, .ps1, etc.)
  - **Application folders** – sub-folders that contain a `.lnk` shortcut or a known fallback executable; shown with the **application's own icon**
  - **Folders** – all other sub-folders (double-click opens in File Explorer)
- **Double-click / Enter** to run a script or launch an application
- **Configurable entry order** – drag any row to a new position with the mouse; the order is saved automatically to the instance config as a `priorityList` and restored on the next launch. Entries not in the list follow the default sort (scripts A–Z → app folders A–Z → plain folders A–Z)
- **Dark mode / Light mode** – choose between **Light**, **Dark**, or **System default** (follows the OS preference). The full UI – list rows, action buttons, toolbar, footer, and all dialogs – adapts to the active theme instantly when you save settings.
- **Custom accent color** – pick any colour with the built-in colour picker to tint the header bar, the Settings dialog section labels, and FlatLaf's system-wide highlights (selection, focused borders, scroll bars, …). Reset to the default Windows blue at any time.
- **Toolbar** below the header bar with quick-access buttons:
  - **SVN Checkout** (left-1) – classic command-line checkout: prompts for a URL, checks out directly into the target folder using the `svn` CLI, and shows real-time output
  - **SVN Repository Browser** (left-2) – opens the TortoiseSVN repository browser so you can navigate the repo and check out any sub-path; the application list refreshes **automatically** when a new directory appears in the launcher folder
  - **Settings ⚙** (right) – open the settings dialog
- **Inline action icons** (right side of each folder row) for one-click access to common actions — no right-click required:
  - 📁 – Open in File Explorer
  - 📝 – Open in Editor
  - 📋 – Copy with Robocopy
  - 🗑 – Delete
  - Which actions are shown, and their **order**, are fully **configurable** via the Settings dialog or the config file
  - **Button style** is configurable: individual **icons** (default) or a single **hamburger menu** (☰) that opens a popup
  - The cursor changes to a **hand pointer** when hovering over an icon
- **Right-click context menu** (folders and app folders) with the same configurable quick actions
  - The context menu can be **disabled** via the Settings dialog or the `showContextMenu` config field
- **Real-time output windows** for robocopy and SVN Checkout operations
- **Three-level configuration** stored in `%APPDATA%\nvLauncher\` — global defaults, per-instance overrides, and an optional explicit config file
- **User-defined custom actions** – define your own actions (script/executable + icon + label) and assign them to the entry action bar and/or the toolbar; fully manageable from the Settings dialog
- **Configurable application types** – define your own app type categories with custom executable detection paths, executable names (priority lists), and optional icons; assign specific folders to a type via the Settings dialog or config file
- **Settings dialog** – organized in **four tabs** for easy navigation:
  - **General** – config-file paths, startup options, **appearance (theme + accent colour)**, `EXPLORER`/`EDITOR` commands, button style, and context-menu toggle
  - **Custom Actions** – add, edit, and remove user-defined actions
  - **Action Buttons** – show/hide and reorder toolbar buttons and per-entry action buttons
  - **App Types** – manage application type definitions and folder assignments
- Optional **system tray** support – start minimized with `--minimized`; **single-click** the tray icon to show/hide
- **Folder-chooser dialog** when no path is supplied on startup
- **Color-coded list** for scripts, application folders, and plain folders – colours automatically adapt to the current light/dark theme

## Prerequisites

- **Java JDK 16 or later** with `javaw` available on your `PATH`
  - Records (used internally) require Java 16+
  - Tested with [SapMachine 26](https://sap.github.io/SapMachine/), but any JDK 16+ will work
  - **FlatLaf** (modern Swing Look & Feel library) is bundled in the uber-JAR – no separate installation needed at runtime
- *(Optional)* An **editor** on your `PATH` – only needed for the *Open in Editor* action. Defaults to `code` (VS Code); configurable via `EDITOR` in the config file or Settings dialog
- *(Optional)* A custom **file explorer** – defaults to the system default; configurable via `EXPLORER` in the config file or Settings dialog
- *(Optional)* **SVN command-line client** on your `PATH` – only needed for the *SVN Checkout* toolbar button. Verify with `svn --version` in Command Prompt.
- *(Optional)* **TortoiseSVN** – only needed for the *SVN Repository Browser* toolbar button. Install from [tortoisesvn.net](https://tortoisesvn.net/). The application uses `TortoiseProc.exe` from its standard installation path.
  - `robocopy` is built-in to Windows, so *Copy with Robocopy* works out of the box

## Setup

### 1. Install a JDK

Download and install a JDK, then make sure `javaw` is on your system `PATH`.

```
java -version   # should print something like: openjdk 26
```

### 2. Build

```bat
mvn clean package
```

This compiles the project and creates a **self-contained uber-JAR** in the `target/` folder that includes FlatLaf and all other runtime dependencies. No separate classpath setup is needed at runtime.

### 3. Run

**With a folder-chooser dialog** (prompts you to pick a folder):

```bat
javaw -jar target\launcher-0.0.1.jar
```

**With a specific folder** (opens that folder directly):

```bat
javaw -jar target\launcher-0.0.1.jar  C:\path\to\your\folder
```

**Minimized to the system tray**:

```bat
javaw -jar target\launcher-0.0.1.jar  C:\path\to\your\folder  --minimized
```

## Command-Line Options

| Option | Description |
|---|---|
| `<rootFolder>` | Path to the root folder to browse. If omitted, the last-used folder from config is opened; if none exists, a folder-chooser dialog is shown. |
| `--minimized` | Start hidden in the system tray. Single-click the tray icon to show/hide; use the tray menu to exit. |
| `--launcherId=<id>` | Explicitly set the launcher instance ID. Used to locate the instance-specific config. Defaults to an 8-character hex hash of the root folder path. |
| `--config=<path>` | Load an additional JSON config file. Its values override both the global and instance configs (highest priority among config files). |

## Configuration

Launcher uses a **three-level configuration** hierarchy. Each level can override individual fields of the level below it; missing fields fall back to the lower level.

```
Level 1 (lowest priority)  %APPDATA%\nvLauncher\config.json
Level 2                    %APPDATA%\nvLauncher\{launcherId}\config.json
Level 3 (highest priority) <path supplied via --config=…>

CLI arguments override all config files.
```

### Config file format

```json
{
  "rootFolder": "C:\\MyApps",
  "startMinimized": false,
  "windowWidth": 560,
  "windowHeight": 680,
  "explorer": "explorer.exe",
  "editor": "code",
  "entryButtonStyle": "ICONS",
  "showContextMenu": true,
  "theme": "system",
  "accentColor": "#0078D7",
  "priorityList": [
    "my-favourite-app",
    "another-app",
    "daily-script.bat"
  ],
  "actionOrder": [
    "EXPLORE_ACTION",
    "EDITOR_ACTION",
    "COPY_ACTION",
    "DELETE_ACTION",
    "my-deploy-action"
  ],
  "toolbarActions": [
    "SVN_CHECKOUT_ACTION",
    "SVN_BROWSER_ACTION",
    "my-build-action"
  ],
  "customActions": [
    {
      "id": "my-deploy-action",
      "scope": "ENTRY",
      "iconPath": "C:\\icons\\deploy.png",
      "scriptPath": "C:\\scripts\\deploy.bat",
      "label": "Deploy",
      "tooltip": "Deploy this application"
    },
    {
      "id": "my-build-action",
      "scope": "TOOLBAR",
      "iconPath": "C:\\icons\\build.png",
      "scriptPath": "C:\\scripts\\build.bat",
      "label": "Build All"
    }
  ]
}
```

Any field can be omitted; omitted fields are inherited from the level below.

| Field | Type | Description |
|---|---|---|
| `rootFolder` | string | Absolute path to the root folder to browse |
| `startMinimized` | boolean | `true` to start hidden in the system tray |
| `windowWidth` | integer | Initial window width in pixels |
| `windowHeight` | integer | Initial window height in pixels |
| `explorer` | string | File explorer executable. Leave blank or omit to use the system default (`Desktop.open`). Example: `"explorer.exe"` |
| `editor` | string | Editor executable. Leave blank or omit to use `code` (VS Code). Example: `"notepad++"`, `"idea64.exe"` |
| `entryButtonStyle` | string | Controls how entry action buttons appear. `"ICONS"` (default) shows one small button per action. `"HAMBURGER"` shows a single ☰ button that opens a popup. |
| `showContextMenu` | boolean | `true` (default) to show the right-click context menu on folder entries. Set to `false` to disable it. |
| `theme` | string | UI colour scheme. `"light"` forces light mode, `"dark"` forces dark mode, `"system"` (default, or omit) follows the OS dark/light preference. Configurable in **Settings → General → Appearance**. |
| `accentColor` | string | Accent colour as a CSS hex string, e.g. `"#0078D7"`. Tints the header bar, the Settings dialog section labels, and FlatLaf's system-wide highlights (selection, focused controls, …). Omit or set to `null` to use the FlatLaf default. Configurable in **Settings → General → Appearance**. |
| `priorityList` | string array | Ordered list of entry names. Entries in this list appear at the top in the given order; all remaining entries follow in the default sort (scripts A–Z → app folders A–Z → plain folders A–Z). Updated automatically when you drag and drop entries in the UI. |
| `actionOrder` | string array | Ordered list of **action keys** that determines which action buttons are shown and in what order. May include built-in keys and custom action IDs. Omit to show all four built-in actions in the default order. |
| `toolbarActions` | string array | Ordered list of **toolbar button keys** that determines which toolbar buttons are shown and in what order. May include built-in keys and custom action IDs. Omit to show both SVN buttons in the default order. |
| `customActions` | object array | List of user-defined custom actions. Each object defines an action that can be referenced in `actionOrder` (entry bar) and/or `toolbarActions` (toolbar). See [Custom Actions](#custom-actions) below. |
| `appTypes` | object array | List of user-defined application type definitions. Each type defines how to detect and display a category of application folder. See [Application Types](#application-types) below. |
| `appTypeAssignments` | object array | Explicit assignments of folder names to application type IDs, overriding auto-detection. See [Application Types](#application-types) below. |

### Action Keys

The `actionOrder` field accepts both **built-in action keys** and **custom action IDs**:

| Key | Action |
|---|---|
| `EXPLORE_ACTION` | Open in File Explorer |
| `EDITOR_ACTION` | Open in Editor |
| `COPY_ACTION` | Copy with Robocopy |
| `DELETE_ACTION` | Delete |
| `<custom action id>` | Any ID defined in `customActions` |

### Toolbar Button Keys

The `toolbarActions` field accepts both **built-in toolbar keys** and **custom action IDs**:

| Key | Button |
|---|---|
| `SVN_CHECKOUT_ACTION` | SVN Checkout (CLI) |
| `SVN_BROWSER_ACTION` | SVN Repository Browser (TortoiseSVN) |
| `<custom action id>` | Any ID defined in `customActions` |

**Examples:**

Show only the SVN Repository Browser button:
```json
{ "toolbarActions": ["SVN_BROWSER_ACTION"] }
```

Hide all toolbar SVN buttons:
```json
{ "toolbarActions": [] }
```

### Custom Actions

Custom actions let you hook your own scripts or executables into the launcher UI. Define them once in `customActions`, then reference their IDs in `actionOrder` (to show them in the per-entry action bar) and/or in `toolbarActions` (to show them in the toolbar).

#### Custom action fields

| Field | Required | Description |
|---|---|---|
| `id` | ✅ yes | Unique identifier. Used as the key in `actionOrder` and `toolbarActions`. Must not clash with built-in keys. |
| `scope` | ✅ yes | Where the action may appear: `"ENTRY"` (entry action bar only), `"TOOLBAR"` (toolbar only), or `"BOTH"` (entry bar and toolbar). Only actions whose scope matches will appear in the respective list in the Settings dialog. |
| `scriptPath` | ✅ yes | Full path to the script or executable to run. When triggered from the **entry action bar**, the selected folder's absolute path is passed as the first argument. When triggered from the **toolbar**, the launcher root folder path is passed instead. |
| `iconPath` | ❌ optional | Full path to a PNG or JPEG image file used as the button icon. If omitted, a short text label is shown instead. |
| `label` | ❌ optional | Display label for the button and context menu item. Falls back to the `id` if omitted. |
| `tooltip` | ❌ optional | Tooltip text shown on hover. Falls back to `label` (or `id`) if omitted. |

#### Example – one custom action in entry bar and toolbar

```json
{
  "customActions": [
    {
      "id": "my-deploy",
      "scope": "BOTH",
      "scriptPath": "C:\\scripts\\deploy.bat",
      "iconPath": "C:\\icons\\deploy.png",
      "label": "Deploy",
      "tooltip": "Deploy the selected application"
    }
  ],
  "actionOrder": ["EXPLORE_ACTION", "my-deploy"],
  "toolbarActions": ["SVN_BROWSER_ACTION", "my-deploy"]
}
```

**Examples:**

Show only Explorer and Editor buttons (in that order):
```json
{ "actionOrder": ["EXPLORE_ACTION", "EDITOR_ACTION"] }
```

Hide the Delete button, reverse the order of the remaining three:
```json
{ "actionOrder": ["COPY_ACTION", "EDITOR_ACTION", "EXPLORE_ACTION"] }
```

Hide all action buttons:
```json
{ "actionOrder": [] }
```

### Application Types

Application types let you define categories of application folders with custom detection rules, icons, and launch behaviour.

#### Detection priority

When classifying a sub-folder, Launcher checks in this order:

1. **Explicit assignment** (`appTypeAssignments`) – the folder is immediately treated as the assigned type  
2. **Auto-detection** – all defined `appTypes` are scanned in order; first match wins  
3. **Built-in `.lnk` detection** – first shortcut found at the folder root  
4. **Built-in fallback exe** – `basis\sys\win\bin\dsc_StartPlm.exe`  
5. **Plain folder** – no executable found

#### App type fields

| Field | Required | Description |
|---|---|---|
| `id` | ✅ yes | Unique identifier. Referenced in `appTypeAssignments`. |
| `executablePaths` | ✅ yes | Priority-ordered list of sub-folder paths inside the app folder to search. Use `""` (empty string) to search the folder root. |
| `executableNames` | ✅ yes | Priority-ordered list of file names to look for in each path. May include `.lnk` shortcuts and `.exe` files. |
| `iconPath` | ❌ optional | Absolute path to a PNG/JPG/GIF image used as the entry icon. If omitted, the system icon of the found executable is extracted instead. |

#### Assignment fields (inside `appTypeAssignments`)

| Field | Description |
|---|---|
| `folder` | Exact name of the sub-folder to assign (case-sensitive, as it appears on disk) |
| `type` | The `id` of the app type to assign to this folder |

#### Config example

```json
{
  "appTypes": [
    {
      "id": "sap-app",
      "iconPath": "C:\\icons\\sap.png",
      "executablePaths": ["basis\\sys\\win\\bin", ""],
      "executableNames": ["dsc_StartPlm.exe", "saplogon.exe"]
    },
    {
      "id": "generic-win",
      "executablePaths": ["bin", ""],
      "executableNames": ["app.lnk", "start.exe", "run.bat"]
    }
  ],
  "appTypeAssignments": [
    {"folder": "MyLegacySapApp", "type": "sap-app"},
    {"folder": "SomeOtherTool",  "type": "generic-win"}
  ]
}
```

### Launcher ID

The launcher ID identifies a specific instance and determines which instance config folder is used.

- Provide it explicitly with `--launcherId=<id>` (e.g. `--launcherId=myapps`)
- If not provided, it is computed as an 8-character hex hash of the absolute root folder path (e.g. `3a7f2c01`)

The **Settings dialog** (⚙ button in the toolbar) shows the active launcher ID and the full paths to both config files.

### First run

On the very first launch, `%APPDATA%\nvLauncher\config.json` is created automatically with the application defaults. The instance config is created/updated each time the launcher starts and on window close (capturing the current window size).

## Usage Guide

### Launching Scripts and Applications

- **Double-click** or press **Enter** on a list item to launch it:
  - **Scripts** (.bat, .cmd, .ps1, .vbs, .sh, .js) open in their respective interpreter/shell
  - **Application folders** search for and launch `.lnk` shortcuts or the fallback executable
  - **Plain folders** open in Windows File Explorer

### Toolbar

A toolbar sits between the header and the entry list.

| Button | Position | Action |
|---|---|---|
| **SVN Checkout** (apps-add icon) | Left-1 | Prompts for an SVN repository URL, derives a folder name from it, and runs `svn checkout` into the selected (or root) folder. Shows real-time command output. After the process finishes, asks whether to refresh the list. Requires `svn` on your `PATH`. |
| **SVN Repository Browser** (apps-add icon) | Left-2 | Opens the TortoiseSVN repository browser directly (no URL prompt). While the browser is open the launcher folder is watched for new directories — the list refreshes automatically when a checkout completes. Requires TortoiseSVN. |
| **Settings** (gear icon) | Right | Opens the Settings dialog. |

### Settings Dialog

Open by clicking the **⚙ gear icon** on the right side of the toolbar. The dialog is organized into **four tabs**. The **Save** and **Cancel** buttons are always visible at the bottom, regardless of the active tab.

#### Tab: General

| Section | Field | Description |
|---|---|---|
| Configuration files | Launcher ID | The active instance identifier (read-only) |
| Configuration files | Global config | Path to `%APPDATA%\nvLauncher\config.json` with 📂 button to open the folder |
| Configuration files | Instance config | Path to `%APPDATA%\nvLauncher\{id}\config.json` with 📂 button to open the folder |
| Startup | Start minimized | Checkbox – saves to instance config; takes effect on next launch |
| Appearance | Theme | Choose **System default** (follows OS dark/light preference), **Light mode**, or **Dark mode**. Applied immediately when you click Save. |
| Appearance | Accent color | Colour swatch showing the current accent. Click **Choose…** to open the colour picker; click **Reset to default** to restore the built-in blue. The accent tints the header bar, the Settings dialog section labels, and FlatLaf's system highlights (selection, focused borders, …). Applied immediately when you click Save. |
| Commands | EXPLORER | File explorer command. Leave blank to use the system default. |
| Commands | EDITOR | Editor command (e.g. `code`, `notepad++`). Leave blank to default to `code`. |
| Button Style | Style radio buttons | Choose **Inline icons** (one button per action, default) or **Hamburger menu** (single ☰ button that opens a popup). |
| Button Style | Show context menu | Checkbox – uncheck to disable the right-click context menu on folder entries. |

#### Tab: Custom Actions

Add, edit, or remove user-defined custom actions. Each action has an ID, a mandatory **scope** (`Entry`, `Toolbar`, or `Both`), script path, optional icon, label, and tooltip.

#### Tab: Action Buttons

| Section | Description |
|---|---|
| Toolbar | Checkboxes to show/hide each toolbar button; **↑ / ↓** buttons to reorder. Changes take effect immediately without a restart. |
| Action Buttons | Checkboxes to show/hide each per-entry action button; **↑ / ↓** buttons to reorder. Changes take effect immediately without a restart. |

#### Tab: App Types

| Section | Description |
|---|---|
| Application Types | Add, edit, or remove application type definitions. Each type has an ID, priority-ordered **executable paths** and **executable names**, and an optional icon path. |
| Application Type Assignments | Manually assign a specific folder to an application type (overrides auto-detection). |

Click **Save** to persist all changes across all tabs, or **Cancel** to discard.

### Theme and Appearance

Launcher supports three theme modes, selectable in **Settings → General → Appearance**:

| Mode | Config value | Behaviour |
|---|---|---|
| System default | `"system"` (or omit) | Follows the OS dark/light preference (Windows registry on Windows, `AppleInterfaceStyle` on macOS) |
| Light mode | `"light"` | Always light, regardless of OS setting |
| Dark mode | `"dark"` | Always dark, regardless of OS setting |

The theme is applied via [FlatLaf](https://www.formdev.com/flatlaf/). The following UI areas adapt automatically when the theme changes:

- **List rows** – alternating row colours, entry-type foreground colours, action button colours
- **Action buttons** – background, border, icon foreground, delete-button red
- **Toolbar and footer** – background follows the system panel colour; borders use the theme separator colour
- **All standard Swing controls** – text fields, buttons, checkboxes, radio buttons, tabs, dialogs, …

The **accent colour** is used for:
- The header bar background
- Section labels in the Settings dialog (lightened in dark mode, darkened in light mode)
- FlatLaf's system-wide selection highlight, focused component borders, and similar controls

### Inline Action Icons / Hamburger Menu

Every **application folder** and **plain folder** row shows action button(s) on the **right-hand side** of the row. Scripts do not have action buttons.

**Button styles:**

| Style | Config value | Behaviour |
|---|---|---|
| Inline icons | `"ICONS"` (default) | One small icon button per action; each click fires that action immediately |
| Hamburger menu | `"HAMBURGER"` | A single ☰ button; click it to open a popup listing all configured actions |

| Icon | Action key | Action | Description |
|---|---|---|---|
| 📁 | `EXPLORE_ACTION` | Open in File Explorer | Browse the folder in the configured file explorer (or system default) |
| 📝 | `EDITOR_ACTION` | Open in Editor | Open the folder in the configured editor (default: `code`) |
| 📋 | `COPY_ACTION` | Copy with Robocopy… | Duplicate the folder within the active launcher directory |
| 🗑 | `DELETE_ACTION` | Delete | Permanently delete the folder (requires confirmation) |

### Type-to-Search

Just start typing while the Launcher window is focused — no need to click a search box first:

- The list is filtered **instantly** as you type
- The current filter is shown in the **footer** (`Filter: <query>▌`)
- The entry count updates to show **N of M** (filtered vs. total)
- **Backspace** removes the last character; **Escape** clears the filter
- **Drag-and-drop reordering is disabled** while a filter is active

### Entry Order & Favorites

Entries can be sorted into any order using drag-and-drop:

1. **Drag** any row to a new position (grab and move while the list is unfiltered)
2. **Drop** it between two other entries
3. The new order is **saved immediately** to the instance config (`priorityList`)

#### How ordering works

| Priority | Rule |
|---|---|
| 1st | Entries whose name appears in `priorityList`, in the order they appear there |
| 2nd | All remaining entries: scripts A–Z → app folders A–Z → plain folders A–Z |

### Right-Click Context Menu (Folders Only)

Right-click any **application folder** or **plain folder** to access the context menu. The menu shows only the actions currently enabled via `actionOrder`.

> **Note:** The context menu can be disabled entirely via **Settings ⚙ → Show right-click context menu** or by setting `"showContextMenu": false` in the config file.

#### **Copy with Robocopy...**
- Prompts you to enter a new name for the copy
- Uses Windows `robocopy` with `/MIR` flag (mirror mode — copies recursively)
- Shows real-time output; closes automatically when done; list refreshes automatically

#### **Delete**
- Requires confirmation; permanently deletes the folder and all its contents
- List refreshes automatically

## How Application Launching Works

### Scripts
Executed in the appropriate interpreter — see *Supported Script Types*.

### Plain Folders
Opens in **Windows File Explorer**.

### Application Folders
Launcher uses the following strategy (in priority order):

1. **Custom app type** – `executablePaths`/`executableNames` priority lists are searched; first found is launched
2. **Built-in: `.lnk` shortcut** – first shortcut at the folder root via the Windows shell
3. **Built-in: fallback executable** – `basis\sys\win\bin\dsc_StartPlm.exe`
4. **Error** – warning dialog shown if nothing is found

## File Overview

### Source files

| File | Description |
|---|---|
| `Launcher.java` | Application entry point and UI shell. Includes `applyTheme(theme, accentColor)` (applies FlatLaf L&F + accent colour via `setGlobalExtraDefaults`), `refreshThemeColors()` (updates header, borders, legend labels on theme/accent change), and `parseHexColor()` utility. |
| `LauncherConfig.java` | Config record – JSON load/save/merge, three-level override logic; **16 fields** including `theme`, `accentColor`, `customActions`, `appTypes`, `appTypeAssignments` |
| `CustomAction.java` | Immutable record for user-defined actions: `id`, `scope`, `iconPath`, `scriptPath`, `label`, `tooltip` |
| `AppType.java` | Immutable record for custom application types: `id`, `iconPath`, `executablePaths`, `executableNames` |
| `EntryType.java` | Enum: `SCRIPT`, `APP_FOLDER`, `PLAIN_FOLDER` |
| `LaunchEntry.java` | Immutable record for one list row: `file()`, `type()`, `iconFile()`, `appType()` |
| `EntryLoader.java` | Scans the root folder, classifies entries, applies priority-list sorting |
| `EntryLauncher.java` | Launches scripts and application folders |
| `FolderActions.java` | Folder-level operations: explorer, editor, robocopy, delete, SVN Checkout, SVN Browser, custom actions |
| `EntryCellRenderer.java` | Swing list-cell renderer. All colours are computed from the active Look-and-Feel at construction time via `isDark()` (`FlatLaf.isLafDark()`), so they adapt automatically to light/dark themes. The renderer is recreated on every theme/settings change. |
| `ListMouseHandler.java` | `MouseAdapter` – action button clicks, double-click launch, hover cursor, right-click menu |
| `EntryListTransferHandler.java` | Drag-and-drop reordering (disabled while a search filter is active) |
| `SettingsDialog.java` | Modal settings `JDialog` – four tabs. *General* now includes the **Appearance section** (theme radio buttons + accent colour swatch/picker/reset). Section labels derive their colour from the configured accent (lightened in dark mode, darkened in light mode). |
| `ProcessOutputWindow.java` | Streams real-time process output into a dedicated, auto-closing window |

### Test files

Tests are written with **JUnit 5** (`mvn test`):

| File | Tests | What is covered |
|---|---|---|
| `LauncherConfigTest.java` | 23 | `empty()`, `defaults()`, `mergeOver()`, `withDefaults()`, three-level merge chain, JSON round-trips, missing/malformed files |
| `EntryLoaderTest.java` | 26 | Script detection, folder classification, sort order, priority-list reordering |
| `LaunchEntryTest.java` | 9 | Constructors, `toString()`, equality, hash code, `EntryType` values |

> **UI classes not covered by unit tests:** `FolderActions`, `ListMouseHandler`, `SettingsDialog`, `EntryLauncher`, and `ProcessOutputWindow` require a UI-testing framework such as AssertJ Swing.

### Other files

| File | Description |
|---|---|
| `src/main/resources/` | PNG icon files used for action buttons, window icon, and system tray |
| `pom.xml` | Maven configuration (JDK 26, JUnit 5, **FlatLaf 3.5.4**, **maven-shade-plugin** for self-contained uber-JAR) |
| `scripts/build.bat` | Legacy `javac` compile script (Maven recommended) |
| `scripts/run.bat` | Legacy run script (use `javaw -jar target\launcher-0.0.1.jar` instead) |
| `example_start_at_logon_in_apps_folder.bat` | Example batch file for starting Launcher minimized to the tray at logon |
| `icon-attribution.md` | Icon licence credits (Flaticon) |
| `README.md` | This file |
| `LICENSE` | License information |

## Supported Script Types

Launcher recognizes and launches the following script file types:

| Extension | Interpreter | Notes |
|---|---|---|
| `.bat` | Command Prompt | Opens in new console window |
| `.cmd` | Command Prompt | Opens in new console window |
| `.ps1` | PowerShell | Keeps window open after execution (`-NoExit`) |
| `.vbs` | Windows Script Host | VBScript |
| `.js` | Windows Script Host | JavaScript |
| `.sh` | Bash | Requires WSL or Git Bash on PATH |

## Color Legend

All colours adapt automatically to the active light/dark theme.

| Color (light mode) | Color (dark mode) | Category | Double-click behaviour |
|---|---|---|---|
| 🟦 Dark teal | 🩵 Bright cyan | Scripts | Execute in appropriate interpreter/shell |
| 🟩 Dark green | 🟢 Bright green | Application folders | Launch via `.lnk` shortcut or fallback executable |
| 🟫 Warm dark gray | 🌫️ Warm light gray | Plain folders | Open in Windows File Explorer |

## Keyboard Shortcuts & Tips

| Action | Keyboard / Mouse |
|---|---|
| Launch selected item | **Enter** or **Double-click** |
| Reorder entry | **Drag & drop** row (unfiltered list only) |
| Open Settings | **Toolbar ⚙ button** (right) |
| Open in File Explorer | **Click folder icon** or right-click → *Open in File Explorer* |
| Open in Editor | **Click document icon** or right-click → *Open in Editor* |
| Copy with Robocopy | **Click copy icon** or right-click → *Copy with Robocopy…* |
| Delete folder | **Click bin icon** or right-click → *Delete* |
| Navigate list | **↑ ↓** Arrow keys |
| Start filtering | **Type any character** |
| Delete last filter character | **Backspace** |
| Clear filter | **Escape** |
| Minimize to tray | Close window (if started with `--minimized`) |
| Show/Hide from tray | **Single-click** tray icon |

## Auto-start on Logon

### Option A - Startup Folder (simplest)

1. Press **Win + R**, type `shell:startup`, press **Enter**.
2. Create a shortcut to your launcher batch file inside that folder.
3. Log off and back on – Launcher will start automatically.

### Option B - Task Scheduler (recommended for tray/minimized use)

1. Open **Task Scheduler** → **Create Basic Task…**
2. Set **Trigger** to *When I log on*, **Action** to *Start a program*, point to your batch file.
3. In **Properties → General**, check *Run only when user is logged on*.

---

## Creating a Batch File to Start Launcher at Logon

```bat
@echo off
REM Start Launcher minimized in the system tray for a specific folder

start "" "C:\path\to\your\jdk\bin\javaw" -jar "C:\path\to\launcher-0.0.1.jar" ^
    "C:\path\to\your\apps\folder" --minimized --launcherId=myapps
```

### Example

```bat
@echo off
start "" "C:\Program Files\SapMachine\jdk-26\bin\javaw" -jar "D:\Launcher\launcher-0.0.1.jar" ^
    "D:\MyApps" --minimized --launcherId=myapps
```

The instance config will be stored at `%APPDATA%\nvLauncher\myapps\config.json`.

---

## Troubleshooting

### Theme Does Not Apply Correctly
- Click **Save** in the Settings dialog – the theme and accent are applied immediately. No restart is needed.

### "Could not open editor" Error
- Enter the full path in **Settings ⚙ → General → EDITOR**, e.g. `C:\Program Files\Microsoft VS Code\bin\code.cmd`

### SVN Checkout Fails
- Install an SVN command-line client and verify with `svn --version` in Command Prompt.
- Alternatively, use the **SVN Repository Browser** toolbar button (TortoiseSVN GUI).

### TortoiseSVN Not Found
- Install TortoiseSVN from [tortoisesvn.net](https://tortoisesvn.net/) to the default path (`C:\Program Files\TortoiseSVN`).

### Robocopy Shows "Access Denied"
- Run Launcher as Administrator or check folder permissions.

### Application Fails to Launch
- Add a `.lnk` shortcut to the app folder's top level, or define an App Type with the correct executable paths.

### Action Buttons Not Showing / Wrong Order
- Open **Settings ⚙ → Action Buttons** tab to toggle visibility and reorder. Check that `actionOrder` is not an empty array `[]`.

### Context Menu Not Appearing
- Re-enable it in **Settings ⚙ → General → Show right-click context menu** or set `"showContextMenu": true` in the config.

### Config Not Being Picked Up
- Verify the JSON is valid (no trailing commas, all strings quoted).
- Use the **Settings** dialog to confirm the exact config file paths for the current instance.

---

## Tips & Best Practices

### Running Multiple Launcher Instances

```bat
javaw -jar launcher-0.0.1.jar "D:\Apps"     --launcherId=apps
javaw -jar launcher-0.0.1.jar "D:\Projects" --launcherId=projects
```

Each instance uses its own `%APPDATA%\nvLauncher\{launcherId}\config.json`.

### Sharing a Common Base Config

Edit `%APPDATA%\nvLauncher\config.json` to set defaults for **all** instances, e.g.:

```json
{ "theme": "dark", "accentColor": "#1A73E8", "startMinimized": false }
```

Instance configs override only the fields they explicitly set.

### Performance Notes

- Launcher scans **only the top level** of your chosen folder (no recursive scanning)
- Robocopy operations show real-time output; the list refreshes automatically when done

---

## License

See the `LICENSE` file in this project.

---

## Icon Attribution

Icons used in this application are provided by [Flaticon](https://www.flaticon.com/).
See [`icon-attribution.md`](icon-attribution.md) for the full list of credits.