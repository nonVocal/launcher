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
- **Custom theme colors** – override individual launcher-specific colours (row backgrounds, foreground colours for each entry type, action button colours, separator colour, search label colour) independently of the base theme.
- **LAF color overrides** – override any FlatLaf `@variable` (e.g. `@background`, `@foreground`, `@selectionBackground`, `@textBackground`, `@buttonBackground`, `@menuBackground`) or UIManager property to create a fully cohesive custom theme that extends beyond the launcher list to **all** Swing components.
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
- **Hidden entries** – exclude specific folder or file names from the launcher list via the Settings dialog or `hiddenEntries` config field
- **Settings dialog** – organized in **six tabs** for easy navigation:
  - **General** – config-file paths, startup options, **appearance (theme + accent colour)**, `EXPLORER`/`EDITOR` commands, button style, and context-menu toggle
  - **Custom Actions** – add, edit, and remove user-defined actions
  - **Action Buttons** – show/hide and reorder toolbar buttons and per-entry action buttons
  - **App Types** – manage application type definitions and folder assignments
  - **Hidden Entries** – manage entries excluded from the list
  - **Colors** – override individual launcher colours and FlatLaf LAF variables to craft a cohesive custom theme
- Optional **system tray** support – start minimized with `--minimized`; **single-click** the tray icon to show/hide
- **Folder-chooser dialog** when no path is supplied on startup
- **Color-coded list** for scripts, application folders, and plain folders – colours automatically adapt to the current light/dark theme

## Prerequisites

### Running the self-contained application (recommended)

Download the `Launcher/` folder from the [Releases](../../releases) page and double-click **`Launcher.exe`** — no JDK or JRE installation needed.

### Running from the uber-JAR

- **Java JDK 26** (or a compatible JRE) with `javaw` on your `PATH`
  - Records (used internally) require Java 16+
  - Tested with [OpenJDK 26](https://openjdk.org/)
  - **FlatLaf** (modern Swing Look & Feel library) is bundled in the uber-JAR – no separate installation needed at runtime

### Building from source

- **Java JDK 26** (required by the compiler and `jpackage`)  
  `jpackage` (bundled with JDK 14+) is used to create the self-contained application image
- **Maven 3.6+** – or use the bundled Maven in IntelliJ IDEA

### Optional runtime tools

- **Editor** on your `PATH` – only needed for the *Open in Editor* action. Defaults to `code` (VS Code); configurable via `EDITOR` in the config file or Settings dialog
- **Custom file explorer** – defaults to the system default; configurable via `EXPLORER` in the config file or Settings dialog
- **SVN command-line client** on your `PATH` – only needed for the *SVN Checkout* toolbar button. Verify with `svn --version` in Command Prompt.
- **TortoiseSVN** – only needed for the *SVN Repository Browser* toolbar button. Install from [tortoisesvn.net](https://tortoisesvn.net/). The application uses `TortoiseProc.exe` from its standard installation path.
  - `robocopy` is built-in to Windows, so *Copy with Robocopy* works out of the box

## Setup

### 1. Build

```bat
scripts\build.bat
```

Or, if Maven is on your `PATH`:

```bat
mvn clean package
```

This compiles the project and produces **two outputs** in the `target/` folder:

| Output | Path | Notes |
|---|---|---|
| **Self-contained app** (recommended) | `target\dist\Launcher\Launcher.exe` | Bundled JRE – no Java install needed on target machine. Distribute the whole `Launcher\` folder. |
| **Uber-JAR** | `target\launcher-0.0.4.jar` | Fat JAR including FlatLaf. Requires JRE 26+ on the target machine. |

> **Note:** `jpackage` (JDK 14+) must be available in the JDK running Maven.  
> `scripts\build.bat` automatically sets `JAVA_HOME` to JDK 26 and calls IntelliJ's bundled Maven.

### 2. Run

#### Self-contained exe (no JRE needed)

Double-click `Launcher.exe` inside `target\dist\Launcher\`, or start it from a shortcut / batch file:

```bat
start "" "target\dist\Launcher\Launcher.exe" "C:\path\to\your\folder"
```

#### Uber-JAR (requires JRE 26+)

**With a folder-chooser dialog** (prompts you to pick a folder):

```bat
javaw -jar target\launcher-0.0.4.jar
```

**With a specific folder** (opens that folder directly):

```bat
javaw -jar target\launcher-0.0.4.jar  C:\path\to\your\folder
```

**Minimized to the system tray**:

```bat
javaw -jar target\launcher-0.0.4.jar  C:\path\to\your\folder  --minimized
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
  ],
  "hiddenEntries": [
    "temp-folder",
    "archive.bat"
  ],
  "customThemeColors": {
    "rowEven": "#1E2030",
    "rowOdd": "#1A1B2A",
    "fgScript": "#89DCEB",
    "fgFolder": "#A6E3A1",
    "fgPlain": "#CDD6F4",
    "selBg": "#313244",
    "actFg": "#89B4FA",
    "actDel": "#F38BA8",
    "actBg": "#292B3D",
    "actBord": "#45475A",
    "selActBg": "#45475A",
    "selActBord": "#6C7086",
    "sepColor": "#313244",
    "searchFg": "#89B4FA"
  },
  "customLafDefaults": {
    "@background": "#1E1E2E",
    "@foreground": "#CDD6F4",
    "@selectionBackground": "#313244",
    "@selectionForeground": "#CDD6F4",
    "@textBackground": "#181825",
    "@buttonBackground": "#292B3D",
    "@menuBackground": "#181825"
  }
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
| `hiddenEntries` | string array | Names of folders or files to exclude from the launcher list. The comparison is case-sensitive and matches the file/folder name exactly. |
| `customThemeColors` | object | Per-key hex colour overrides for the launcher's own colour palette. Keys are the palette slot names (see [Custom Theme Colors](#custom-theme-colors) below); values are CSS hex strings (e.g. `"#1E2030"`). Missing keys fall back to the theme default. |
| `customLafDefaults` | object | FlatLaf `@variable` overrides and/or direct UIManager property overrides. Applied globally via `FlatLaf.setGlobalExtraDefaults()` before the L&F is installed, so they cascade through **all** Swing components. See [LAF Color Overrides](#laf-color-overrides) below. |

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

#### Environment variables

In addition to the folder path passed as the first CLI argument, Launcher always sets the following environment variables for the child process so that scripts can inspect the full entry context:

| Variable | Description |
|---|---|
| `NV_LAUNCHER_FOLDER` | Absolute path of the launcher root folder |
| `NV_ENTRY_PATH` | Absolute path of the target entry (same as the first CLI argument) |
| `NV_ENTRY_NAME` | File/folder name of the target entry |
| `NV_ENTRY_TYPE` | `SCRIPT`, `APP_FOLDER`, or `PLAIN_FOLDER` — empty when triggered from the toolbar with no entry selected |
| `NV_APP_TYPE_ID` | ID of the matched application type (see [Application Types](#application-types)), or empty string if not an app-typed folder |
| `NV_ICON_FILE` | Absolute path to the icon file (`.lnk` or fallback `.exe`) used to display the entry's icon, or empty string if none |

**Example – batch script using entry metadata:**

```bat
@echo off
echo Entry name : %NV_ENTRY_NAME%
echo Entry type : %NV_ENTRY_TYPE%
echo App type   : %NV_APP_TYPE_ID%
echo Root folder: %NV_LAUNCHER_FOLDER%
echo Icon file  : %NV_ICON_FILE%
pause
```

**Example – PowerShell script using entry metadata:**

```powershell
Write-Host "Deploying: $env:NV_ENTRY_NAME (type=$env:NV_ENTRY_TYPE, appType=$env:NV_APP_TYPE_ID)"
Write-Host "Root: $env:NV_LAUNCHER_FOLDER"
# ... deployment logic using $args[0] (the entry folder path)
```

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

Open by clicking the **⚙ gear icon** on the right side of the toolbar. The dialog is organized into **six tabs**. The **Save** and **Cancel** buttons are always visible at the bottom, regardless of the active tab.

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

#### Tab: Hidden Entries

Manage the list of folder/file names excluded from the launcher. Add names from the known-folders dropdown (or type any name), remove existing entries. Changes take effect when you click Save.

#### Tab: Colors

| Section | Description |
|---|---|
| **Launcher Colors** | 14 colour slots covering row backgrounds, foreground colours for each entry type, action button colours, separator, and search label. Each slot shows a colour swatch (hatched = using theme default), a **Pick…** button, and a **Reset** button. A **Reset All to Theme Defaults** button clears all overrides. |
| **Look-and-Feel Color Overrides** | Seven pre-defined FlatLaf `@variable` rows (background, foreground, selectionBackground, selectionForeground, textBackground, buttonBackground, menuBackground) with colour pickers. A **Reset All LAF Overrides** button clears them. Below that, a free-form **Add / Edit / Remove** list for any additional FlatLaf variable or UIManager property key with a built-in colour picker. |

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

**Three layers of colour customisation** (applied in order, each overrides the previous):

| Layer | Config field | Scope |
|---|---|---|
| 1. Base theme | `theme` + FlatLaf L&F | Entire application |
| 2. Accent colour | `accentColor` → `@accentColor` | Header, selection highlights, focused borders |
| 3. LAF overrides | `customLafDefaults` → `setGlobalExtraDefaults()` | Any FlatLaf `@variable` or UIManager key — entire application |
| 4. Launcher palette | `customThemeColors` | Launcher list rows, action buttons, separator, search label |

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
| `Launcher.java` | Application entry point and UI shell. Includes `applyTheme(theme, accentColor, customLafDefaults)` (merges `customLafDefaults` with the accent colour into `FlatLaf.setGlobalExtraDefaults()`, then installs the L&F), `refreshThemeColors()` (updates header, borders, legend labels on theme/accent change), and `parseHexColor()` utility. |
| `LauncherConfig.java` | Config record – JSON load/save/merge, three-level override logic; **19 fields** including `theme`, `accentColor`, `customActions`, `appTypes`, `appTypeAssignments`, `hiddenEntries`, `customThemeColors`, and `customLafDefaults`. Shared `jsonStringMap` / `parseJsonStringMap` helpers for serializing both color-map fields. |
| `ColorTheme.java` | Colour palette for the launcher's cell renderer. `forCurrentLaf()` resolves colours from the active L&F; `forCurrentLaf(Map<String,String>)` additionally applies `customThemeColors` overrides on top. `applyCustomColors()` substitutes individual hex-colour values by palette-slot name. |
| `CustomAction.java` | Immutable record for user-defined actions: `id`, `scope`, `iconPath`, `scriptPath`, `label`, `tooltip` |
| `AppType.java` | Immutable record for custom application types: `id`, `iconPath`, `executablePaths`, `executableNames` |
| `EntryType.java` | Enum: `SCRIPT`, `APP_FOLDER`, `PLAIN_FOLDER` |
| `LaunchEntry.java` | Immutable record for one list row: `file()`, `type()`, `iconFile()`, `appType()` |
| `EntryLoader.java` | Scans the root folder, classifies entries, applies priority-list sorting and hidden-entry filtering |
| `EntryLauncher.java` | Launches scripts and application folders |
| `FolderActions.java` | Folder-level operations: explorer, editor, robocopy, delete, SVN Checkout, SVN Browser, custom actions |
| `EntryCellRenderer.java` | Swing list-cell renderer. Accepts `customThemeColors` in its constructor and calls `ColorTheme.forCurrentLaf(customThemeColors)` so the palette reflects both the active L&F and any user-defined overrides. Recreated on every theme/settings change. |
| `ListMouseHandler.java` | `MouseAdapter` – action button clicks, double-click launch, hover cursor, right-click menu |
| `EntryListTransferHandler.java` | Drag-and-drop reordering (disabled while a search filter is active) |
| `SettingsDialog.java` | Modal settings `JDialog` – **six tabs**: General (theme + accent), Custom Actions, Action Buttons, App Types, Hidden Entries, Colors (launcher palette + LAF overrides). `buildNewLafDefaults()` merges predefined `@variable` swatches with the free-form list into the `customLafDefaults` map. |
| `ProcessOutputWindow.java` | Streams real-time process output into a dedicated, auto-closing window |

### Test files

Tests are written with **JUnit 5** (`mvn test`):

| File | Tests | What is covered |
|---|---|---|
| `LauncherConfigTest.java` | 26 | `empty()`, `defaults()`, `mergeOver()`, `withDefaults()`, three-level merge chain, JSON round-trips for scalars / lists / `customThemeColors` / `customLafDefaults`, missing/malformed files |
| `EntryLoaderTest.java` | 26 | Script detection, folder classification, sort order, priority-list reordering, hidden-entry filtering |
| `LaunchEntryTest.java` | 9 | Constructors, `toString()`, equality, hash code, `EntryType` values |

> **UI classes not covered by unit tests:** `FolderActions`, `ListMouseHandler`, `SettingsDialog`, `EntryLauncher`, and `ProcessOutputWindow` require a UI-testing framework such as AssertJ Swing.

### Other files

| File | Description |
|---|---|
| `src/main/resources/` | PNG icon files used for action buttons, window icon, and system tray |
| `pom.xml` | Maven configuration (JDK 26, JUnit 5, **FlatLaf 3.5.4**, **maven-shade-plugin** for uber-JAR, **maven-antrun-plugin** + **jpackage-maven-plugin** for self-contained exe) |
| `scripts/build.bat` | Maven build script – sets `JAVA_HOME` to JDK 26, calls IntelliJ's bundled Maven, runs `mvn package`; outputs both the uber-JAR and the self-contained `target\dist\Launcher\Launcher.exe` |
| `scripts/run.bat` | Legacy run script (use `target\dist\Launcher\Launcher.exe` or `javaw -jar target\launcher-0.0.4.jar` instead) |
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
2. Create a shortcut to `Launcher.exe` (self-contained) or to your launcher batch file inside that folder.
3. Log off and back on – Launcher will start automatically.

### Option B - Task Scheduler (recommended for tray/minimized use)

1. Open **Task Scheduler** → **Create Basic Task…**
2. Set **Trigger** to *When I log on*, **Action** to *Start a program*, point to `Launcher.exe` or your batch file.
3. In **Properties → General**, check *Run only when user is logged on*.

---

## Creating a Batch File to Start Launcher at Logon

### Using the self-contained exe (no JRE needed)

```bat
@echo off
REM Start Launcher minimized in the system tray for a specific folder
start "" "C:\path\to\Launcher\Launcher.exe" "C:\path\to\your\apps\folder" --minimized --launcherId=myapps
```

### Using the uber-JAR (requires JRE 26+)

```bat
@echo off
REM Start Launcher minimized in the system tray for a specific folder

start "" "C:\path\to\your\jdk\bin\javaw" -jar "C:\path\to\launcher-0.0.4.jar" ^
    "C:\path\to\your\apps\folder" --minimized --launcherId=myapps
```

### Example (uber-JAR)

```bat
@echo off
start "" "C:\Users\benda\.jdks\openjdk-26\bin\javaw" -jar "D:\Launcher\launcher-0.0.4.jar" ^
    "D:\MyApps" --minimized --launcherId=myapps
```

The instance config will be stored at `%APPDATA%\nvLauncher\myapps\config.json`.

---

## Troubleshooting

### Theme Does Not Apply Correctly
- Click **Save** in the Settings dialog – the theme, accent, and all colour overrides are applied immediately. No restart is needed.
- If `customLafDefaults` entries have no visible effect, verify the key spelling (FlatLaf variables must start with `@`; UIManager keys are case-sensitive, e.g. `Panel.background`).

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

**Self-contained exe:**
```bat
start "" "D:\Launcher\Launcher.exe" "D:\Apps"     --launcherId=apps
start "" "D:\Launcher\Launcher.exe" "D:\Projects" --launcherId=projects
```

**Uber-JAR:**
```bat
javaw -jar launcher-0.0.4.jar "D:\Apps"     --launcherId=apps
javaw -jar launcher-0.0.4.jar "D:\Projects" --launcherId=projects
```

Each instance uses its own `%APPDATA%\nvLauncher\{launcherId}\config.json`.

### Sharing a Common Base Config

Edit `%APPDATA%\nvLauncher\config.json` to set defaults for **all** instances, e.g.:

```json
{
  "theme": "dark",
  "accentColor": "#1A73E8",
  "startMinimized": false,
  "customLafDefaults": {
    "@background": "#1E1E2E",
    "@foreground": "#CDD6F4"
  }
}
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