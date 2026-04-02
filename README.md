# launcher

A lightweight Java Swing application that lets you browse and launch scripts and application folders from a single root directory.

## Features

- **Lists** scripts and sub-folders found at the **top level** of a chosen root folder, divided into three categories:
  - **Scripts** – recognized script files (.bat, .cmd, .ps1, etc.)
  - **Application folders** – sub-folders that contain a `.lnk` shortcut or a known fallback executable; shown with the **application's own icon**
  - **Folders** – all other sub-folders (displayed in a distinct color; double-click opens in File Explorer)
- **Double-click / Enter** to run a script or launch an application
- **Configurable entry order** – drag any row to a new position with the mouse; the order is saved automatically to the instance config as a `priorityList` and restored on the next launch. Entries not in the list follow the default sort (scripts A–Z → app folders A–Z → plain folders A–Z)
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
  - **General** – config-file paths, startup options, `EXPLORER`/`EDITOR` commands, button style, and context-menu toggle
  - **Custom Actions** – add, edit, and remove user-defined actions
  - **Action Buttons** – show/hide and reorder toolbar buttons and per-entry action buttons
  - **App Types** – manage application type definitions and folder assignments
- Optional **system tray** support – start minimized with `--minimized`; **single-click** the tray icon to show/hide
- **Folder-chooser dialog** when no path is supplied on startup
- **Color-coded list** for scripts, application folders, and plain folders

## Prerequisites

- **Java JDK 16 or later** with `javac` and `javaw` available on your `PATH`
  - Records (used internally) require Java 16+
  - Tested with [SapMachine 26](https://sap.github.io/SapMachine/), but any JDK 16+ will work
- *(Optional)* An **editor** on your `PATH` – only needed for the *Open in Editor* action. Defaults to `code` (VS Code); configurable via `EDITOR` in the config file or Settings dialog
- *(Optional)* A custom **file explorer** – defaults to the system default; configurable via `EXPLORER` in the config file or Settings dialog
- *(Optional)* **SVN command-line client** on your `PATH` – only needed for the *SVN Checkout* toolbar button. Verify with `svn --version` in Command Prompt.
- *(Optional)* **TortoiseSVN** – only needed for the *SVN Repository Browser* toolbar button. Install from [tortoisesvn.net](https://tortoisesvn.net/). The application uses `TortoiseProc.exe` from its standard installation path.
  - `robocopy` is built-in to Windows, so *Copy with Robocopy* works out of the box

## Setup

### 1. Install a JDK

Download and install a JDK, then make sure `javac` and `javaw` are on your system `PATH`.

```
javac -version   # should print something like: javac 26.x.x
```

### 2. Compile

**Option A – Using Maven:**

```bat
mvn clean package
```

This compiles the project and creates a JAR file in the `target/` folder.

**Option B – Using the build script (javac only):**

```bat
build.bat
```

This compiles all `.java` source files in the `launcher` package into `.class` files in the same directory. Note: Maven (`mvn clean package`) is recommended as it handles all source files automatically.

### 3. Run

**With a folder-chooser dialog** (prompts you to pick a folder):

```bat
run.bat
```

**With a specific folder** (opens that folder directly):

```bat
run.bat  C:\path\to\your\folder
```

**Minimized to the system tray**:

```bat
run.bat  C:\path\to\your\folder  --minimized
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

When a user clicks the *Deploy* button next to a folder row, `deploy.bat` is called with that folder's path as the first argument. When clicked from the toolbar, the launcher root folder path is passed.

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

Application types let you define categories of application folders with custom detection rules, icons, and launch behaviour. Launcher uses these types to decide whether a sub-folder should be shown as an **Application folder** (green) and how to launch it.

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

Each entry has two fields:

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

In this example:
- `MyLegacySapApp` is always shown as an Application folder of type `sap-app`, even if `dsc_StartPlm.exe` is not found
- Any other folder containing `dsc_StartPlm.exe` in `basis\sys\win\bin\` or `saplogon.exe` anywhere will be auto-detected as `sap-app`
- The `sap.png` file is shown as the entry icon for all `sap-app` folders

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

A toolbar sits between the blue header and the entry list.

| Button | Position | Action |
|---|---|---|
| **SVN Checkout** (apps-add icon) | Left-1 | Prompts for an SVN repository URL, derives a folder name from it, and runs `svn checkout` into the selected (or root) folder. Shows real-time command output. After the process finishes, asks whether to refresh the list. Requires `svn` on your `PATH`. |
| **SVN Repository Browser** (apps-add icon) | Left-2 | Opens the TortoiseSVN repository browser directly (no URL prompt). While the browser is open the launcher folder is watched for new directories — the list refreshes automatically when a checkout completes. Requires TortoiseSVN. |
| **Settings** (gear icon) | Right | Opens the Settings dialog to inspect config-file paths and toggle startup options. |

### Settings Dialog

Open by clicking the **⚙ gear icon** on the right side of the toolbar. The dialog is organized into **four tabs**. The **Save** and **Cancel** buttons are always visible at the bottom, regardless of the active tab.

#### Tab: General

| Section | Field | Description |
|---|---|---|
| Configuration files | Launcher ID | The active instance identifier (read-only) |
| Configuration files | Global config | Path to `%APPDATA%\nvLauncher\config.json` with 📂 button to open the folder |
| Configuration files | Instance config | Path to `%APPDATA%\nvLauncher\{id}\config.json` with 📂 button to open the folder |
| Startup | Start minimized | Checkbox – saves to instance config; takes effect on next launch |
| Commands | EXPLORER | File explorer command. Leave blank to use the system default. |
| Commands | EDITOR | Editor command (e.g. `code`, `notepad++`). Leave blank to default to `code`. |
| Button Style | Style radio buttons | Choose **Inline icons** (one button per action, default) or **Hamburger menu** (single ☰ button that opens a popup). |
| Button Style | Show context menu | Checkbox – uncheck to disable the right-click context menu on folder entries. |

#### Tab: Custom Actions

Add, edit, or remove user-defined custom actions. Each action has an ID, a mandatory **scope** (`Entry`, `Toolbar`, or `Both`), script path, optional icon, label, and tooltip. Only actions with a matching scope appear in the **Action Buttons** or **Toolbar** lists on the *Action Buttons* tab.

#### Tab: Action Buttons

| Section | Description |
|---|---|
| Toolbar | Checkboxes to show/hide each toolbar button (built-in SVN buttons and any custom actions with a toolbar scope); **↑ / ↓** buttons to reorder. Changes take effect immediately without a restart. |
| Action Buttons | Checkboxes to show/hide each per-entry action button (built-in actions and any custom actions with an entry scope); **↑ / ↓** buttons to reorder. Changes take effect immediately without a restart. |

#### Tab: App Types

| Section | Description |
|---|---|
| Application Types | Add, edit, or remove application type definitions. Each type has an ID, priority-ordered **executable paths** and **executable names** (for detection and launch), and an optional icon path. |
| Application Type Assignments | Manually assign a specific folder to an application type (overrides auto-detection). Each assignment maps an exact folder name to a type ID. The type dropdown is populated from the Application Types defined on this tab. |

Click **Save** to persist all changes across all tabs, or **Cancel** to discard.

### Inline Action Icons / Hamburger Menu

Every **application folder** and **plain folder** row shows action button(s) on the **right-hand side** of the row. Scripts do not have action buttons. The set of visible buttons, their order, and their display style are configurable (see **Settings** dialog or `actionOrder` / `entryButtonStyle` config fields).

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

- **Single-click** any icon (or the ☰ button) to trigger the action
- The **cursor changes to a hand pointer** when you hover over a button
- A double-click inside the button area does **not** accidentally launch the entry

### Type-to-Search

Just start typing while the Launcher window is focused — no need to click a search box first:

- The list is filtered **instantly** as you type
- The current filter is shown in the **footer** (`Filter: <query>▌`) in blue
- The entry count updates to show **N of M** (filtered vs. total)
- **Backspace** removes the last character
- **Escape** clears the filter and restores the full list
- The filter is **case-insensitive** and matches anywhere in the entry name
- **Drag-and-drop reordering is disabled** while a filter is active (the footer shows a note); clear the filter first to reorder

### Entry Order & Favorites

Entries can be sorted into any order you like using drag-and-drop:

1. **Drag** any row to a new position (grab and move while the list is unfiltered)
2. **Drop** it between two other entries — the insertion indicator shows where it will land
3. The new order is **saved immediately** to the instance config (`priorityList` field in `%APPDATA%\nvLauncher\{id}\config.json`) and survives restarts

#### How ordering works

| Priority | Rule |
|---|---|
| 1st | Entries whose name appears in `priorityList`, in the order they appear there |
| 2nd | All remaining entries in the default sort: scripts A–Z → app folders A–Z → plain folders A–Z |

- Only entries listed in `priorityList` are pinned to the top; newly added entries (not yet in the list) appear at the bottom in the default sort order automatically.
- Entries that were in `priorityList` but have since been deleted from disk are simply skipped.
- After each drag-and-drop the **entire current order** is written to `priorityList`, so every item's relative position is remembered.

#### Manually editing the priority list

You can also edit `priorityList` directly in the instance config file (see **Settings** dialog for the exact path). Use the exact folder/file names as they appear on disk.

```json
{
  "priorityList": [
    "PriorityApp",
    "DailyScript.bat",
    "AnotherApp"
  ]
}
```

### Right-Click Context Menu (Folders Only)

Right-click any **application folder** or **plain folder** to access the context menu (scripts are excluded). The menu shows only the actions that are currently enabled via `actionOrder`.

> **Note:** The context menu can be disabled entirely via **Settings ⚙ → Show right-click context menu** or by setting `"showContextMenu": false` in the config file. When disabled, folder entries can still be acted on via the inline action button(s) (icons or hamburger ☰).

#### **Open in File Explorer**
Opens the folder in the configured file explorer. If `EXPLORER` is not set, the system default is used.

#### **Open in Editor**
Opens the folder in the configured editor. If `EDITOR` is not set, defaults to `code` (VS Code). Requires the configured command to be on your `PATH`.

#### **Copy with Robocopy...**
- Prompts you to enter a new name for the copy
- Validates that the name doesn't already exist in the active launcher folder (prevents duplicates)
- Uses Windows `robocopy` with `/MIR` flag (mirror mode — copies recursively)
- Creates the copy in the **active launcher folder** (same parent directory as the source)
- Shows real-time output in a dedicated window
- **Output window closes automatically** when the copy is complete
- **List refreshes automatically** to show the new folder — no extra steps needed

#### **Delete**
- Requires confirmation (warning dialog before anything is deleted)
- Permanently deletes the folder and all its contents — cannot be undone
- **List refreshes automatically** as soon as the folder is gone


## How Application Launching Works

When you double-click a list item, the behaviour depends on its category:

### Scripts
Executed in the appropriate interpreter — see *Supported Script Types* for details.

### Plain Folders
Double-clicking a plain folder opens it in **Windows File Explorer**. All right-click actions (copy, delete, open in editor) are still available.

### Application Folders
Launcher uses the following strategy (in priority order):

1. **Custom app type** – if the folder has a matched or assigned `AppType`, its `executablePaths`/`executableNames` priority lists are searched; the first existing file is launched (`.lnk` via shell, `.exe` directly)
2. **Built-in: `.lnk` shortcut** – the first shortcut found at the folder's root is executed via the Windows shell
3. **Built-in: fallback executable** – `basis\sys\win\bin\dsc_StartPlm.exe` inside the application folder
4. **Error** – if nothing is found, a warning dialog is shown

### Folder Classification

A sub-folder is classified as an **Application folder** when at least one of the following is true:
- It is explicitly assigned an app type via `appTypeAssignments`
- It matches a defined `appType` (executable found at one of the type's paths)
- Contains a `.lnk` shortcut file at its **top level** (built-in)
- Contains `basis\sys\win\bin\dsc_StartPlm.exe` (built-in fallback)

All other sub-folders are treated as **plain folders**.

## File Overview

### Source files

| File | Description |
|---|---|
| `src/main/java/dev/nonvocal/launcher/Launcher.java` | Application entry point and UI shell – constructs the window, wires all helper classes together, manages config state (~700 lines) |
| `src/main/java/dev/nonvocal/launcher/LauncherConfig.java` | Config record – JSON load/save/merge, three-level override logic; 14 fields including `customActions`, `appTypes`, `appTypeAssignments` |
| `src/main/java/dev/nonvocal/launcher/CustomAction.java` | Immutable record representing a user-defined custom action: `id`, `scope`, `iconPath`, `scriptPath`, `label`, `tooltip`; helper methods `effectiveLabel()`, `effectiveTooltip()`, `loadIcon()`, `appliesToEntry()`, `appliesToToolbar()` |
| `src/main/java/dev/nonvocal/launcher/AppType.java` | Immutable record defining a custom application type: `id`, `iconPath`, `executablePaths`, `executableNames`; methods `findExecutable()`, `matches()`, `loadIcon()` |
| `src/main/java/dev/nonvocal/launcher/EntryType.java` | Enum with three values: `SCRIPT`, `APP_FOLDER`, `PLAIN_FOLDER` |
| `src/main/java/dev/nonvocal/launcher/LaunchEntry.java` | Immutable record representing one list row: `file()`, `type()`, `iconFile()`, `appType()` |
| `src/main/java/dev/nonvocal/launcher/EntryLoader.java` | Scans the root folder, classifies entries using app type assignments → auto-detection → built-in `.lnk` / fallback exe detection, and applies priority-list sorting |
| `src/main/java/dev/nonvocal/launcher/EntryLauncher.java` | Launches scripts and application folders; uses the entry's matched `AppType` executable paths/names when set, falls back to built-in `.lnk` / fallback exe logic |
| `src/main/java/dev/nonvocal/launcher/FolderActions.java` | Folder-level operations: open in File Explorer, open in Editor, copy with Robocopy, delete; **SVN Checkout** (CLI via `svn`); **SVN Repository Browser** (TortoiseSVN, auto-refresh); **custom action execution** |
| `src/main/java/dev/nonvocal/launcher/EntryCellRenderer.java` | Swing list-cell renderer – draws entry rows with inline icon buttons (`ICONS`) or hamburger button (`HAMBURGER`); supports custom action icons and app type icons |
| `src/main/java/dev/nonvocal/launcher/ListMouseHandler.java` | `MouseAdapter` – handles single-click on action buttons (built-in and custom), double-click to launch, hover cursor changes, and right-click context menu |
| `src/main/java/dev/nonvocal/launcher/EntryListTransferHandler.java` | `TransferHandler` for drag-and-drop reordering of list entries (disabled while a search filter is active) |
| `src/main/java/dev/nonvocal/launcher/SettingsDialog.java` | Modal settings `JDialog` organized in **four tabs**: *General* (config-file paths, startup, EXPLORER/EDITOR commands, button style, context-menu toggle), *Custom Actions* (add/edit/remove user-defined actions), *Action Buttons* (toolbar and per-entry button visibility and order), *App Types* (application type definitions and folder assignments). Save and Cancel buttons are permanently visible below the tabs. |
| `src/main/java/dev/nonvocal/launcher/ProcessOutputWindow.java` | Utility that streams real-time process output (robocopy, SVN, custom actions) into a dedicated, auto-closing window |

### Test files

Tests are written with **JUnit 5** and can be run via `mvn test`. The following classes are covered:

| File | Tests | What is covered |
|---|---|---|
| `src/test/…/LauncherConfigTest.java` | 23 | `empty()`, `defaults()`, `mergeOver()`, `withDefaults()`, three-level merge chain, JSON round-trips (all field types, backslashes, embedded quotes), empty `actionOrder` serialization behaviour, missing/malformed JSON files |
| `src/test/…/EntryLoaderTest.java` | 26 | `isScript()` for all six extensions and edge cases (uppercase, unknown extensions), folder classification (SCRIPT / APP_FOLDER via `.lnk` / APP_FOLDER via fallback exe / PLAIN_FOLDER), `.lnk` takes precedence over fallback exe, non-script files at root are ignored, default sort order (scripts → apps → plain, case-insensitive alphabetical), priority-list reordering, unknown/missing priority entries |
| `src/test/…/LaunchEntryTest.java` | 9 | Two- and three-argument constructors, `toString()`, record equality and hash code, `EntryType` enum values |

> **UI classes not covered by unit tests:** `FolderActions`, `ListMouseHandler`, `SettingsDialog`, `EntryLauncher`, and `ProcessOutputWindow` rely on Swing dialogs and the Event Dispatch Thread. Meaningful tests for these would require a UI-testing framework such as AssertJ Swing or Mockito.

### Other files

| File | Description |
|---|---|
| `src/main/resources/` | PNG icon files used for action buttons, window icon, and system tray |
| `pom.xml` | Maven configuration (JDK 26, JUnit 5) |
| `scripts/build.bat` | Compiles the source with `javac` |
| `scripts/run.bat` | Runs the app with an optional folder argument |
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

| Color | Category | Icon | Double-click behaviour |
|---|---|---|---|
| 🟦 Dark teal | Scripts | OS file icon | Execute in appropriate interpreter/shell |
| 🟩 Dark green | Application folders | **Application's own icon** (from `.lnk` or `.exe`) | Launch via `.lnk` shortcut or fallback executable |
| 🟫 Warm dark gray | Plain folders | OS folder icon | Open in Windows File Explorer |

## Keyboard Shortcuts & Tips

| Action | Keyboard / Mouse |
|---|---|
| Launch selected item | **Enter** or **Double-click** |
| Reorder entry | **Drag & drop** row to a new position (unfiltered list only) |
| SVN Checkout | **Toolbar button** (left-1) |
| SVN Repository Browser | **Toolbar button** (left-2) |
| Open Settings | **Toolbar ⚙ button** (right) |
| Open in File Explorer | **Click folder icon** (row right side) or right-click → *Open in File Explorer* |
| Open in Editor | **Click document icon** (row right side) or right-click → *Open in Editor* |
| Copy with Robocopy | **Click copy icon** (row right side) or right-click → *Copy with Robocopy…* |
| Delete folder | **Click bin icon** (row right side) or right-click → *Delete* |
| Open context menu | **Right-click** (all folders; scripts excluded; can be disabled in Settings) |
| Navigate list | **↑ ↓** Arrow keys |
| Start filtering | **Type any character** |
| Delete last filter character | **Backspace** |
| Clear filter | **Escape** |
| Minimize to tray | Close window (if started with `--minimized`) |
| Show/Hide from tray | **Single-click** tray icon |

## Auto-start on Logon

There are two common ways to make Launcher start automatically on Windows and open in the tray for a specific folder.

### Option A - Startup Folder (simplest)

1. Press **Win + R**, type `shell:startup`, and press **Enter** - this opens your personal Startup folder.
2. Create a shortcut to your launcher batch file inside that folder:
   - Right-click inside the Startup folder -> **New -> Shortcut**
   - Set the target to the full path of your batch file, for example:
     ```
     C:\path\to\example_start_at_logon_in_apps_folder.bat
     ```
   - Give the shortcut a name (e.g. *Launcher*) and click **Finish**.
3. Log off and back on - Launcher will start automatically.

> **Tip:** To start minimized to the tray, make sure your batch file includes the `--minimized` flag (see the customisation section below). You can also set the shortcut's *Run* property to **Minimised** so the console window doesn't flash up.

### Option B - Task Scheduler (recommended for tray/minimized use)

Task Scheduler gives you more control and avoids any console window flash.

1. Open **Task Scheduler** (search for it in the Start menu).
2. Click **Create Basic Task...** in the right-hand panel.
3. Fill in the wizard:

| Field | Value |
|---|---|
| Name | `Launcher` |
| Trigger | **When I log on** |
| Action | **Start a program** |
| Program/script | `C:\path\to\your\batch\file.bat` |
| Start in | `C:\path\to\your\batch\folder` |

4. Click **Finish**.
5. Find the newly created task in the list, right-click it -> **Properties**:
   - On the **General** tab, check **Run only when user is logged on**.
   - *(Optional)* Check **Run with highest privileges** if the app needs elevation.
6. Click **OK** - the task will run automatically at your next logon.

To test it immediately without logging off, right-click the task and choose **Run**.

---

## Creating a Batch File to Start Launcher in a Folder at Logon

Create a batch file (e.g. `example_start_at_logon_in_apps_folder.bat`) with the following content to launch Launcher minimized to the system tray for a specific folder:

```bat
@echo off
REM Example: Start Launcher minimized in the system tray for a specific folder

start "" "C:\path\to\your\jdk\bin\javaw" -jar "C:\path\to\launcher.jar" ^
    "C:\path\to\your\apps\folder" --minimized --launcherId=myapps
```

### Customising the Batch File

Update the following values to match your environment:

- **`C:\path\to\your\jdk\bin\javaw`** – Replace with the full path to `javaw.exe` in your JDK installation
- **`C:\path\to\launcher.jar`** – Replace with the full path to the compiled JAR (or use `-cp` with the class directory)
- **`C:\path\to\your\apps\folder`** – Replace with the root folder you want Launcher to browse
- **`--launcherId=myapps`** – Optional; give this instance a memorable name (used for its config folder)

### Example

If you have:
- Java installed at `C:\Program Files\SapMachine\jdk-26\bin\javaw.exe`
- Launcher JAR at `D:\Launcher\launcher-0.0.1.jar`
- Apps folder at `D:\MyApps`

Your batch file would be:

```bat
@echo off
start "" "C:\Program Files\SapMachine\jdk-26\bin\javaw" -jar "D:\Launcher\launcher-0.0.1.jar" ^
    "D:\MyApps" --minimized --launcherId=myapps
```

The instance config will be stored at `%APPDATA%\nvLauncher\myapps\config.json`.

---

## Troubleshooting

### "Could not open editor" Error
- **Cause:** The configured editor command is not on your `PATH`
- **Fix:**
  - Open **Settings ⚙ → General → EDITOR** field and enter the full path to the executable, e.g. `C:\Program Files\Microsoft VS Code\bin\code.cmd`
  - Or add the editor's folder to your PATH environment variable
  - Or install VS Code from [code.visualstudio.com](https://code.visualstudio.com/) and check "Add to PATH" during installation

### SVN Checkout Fails
- **Cause:** SVN command-line client is not installed or not on `PATH`
- **Fix:**
  - Install an SVN command-line client (e.g. [Apache Subversion](https://subversion.apache.org/packages.html) or the command-line tools bundled with TortoiseSVN)
  - Verify with `svn --version` in Command Prompt
  - Alternatively, use the **SVN Repository Browser** button (left-2) which uses TortoiseSVN's GUI instead

### TortoiseSVN Not Found
- **Cause:** TortoiseSVN is not installed or not in the expected location
- **Fix:**
  - Download and install TortoiseSVN from [tortoisesvn.net](https://tortoisesvn.net/)
  - Make sure it is installed to the default location (`C:\Program Files\TortoiseSVN`)
  - After installing, restart Launcher and try again

### Robocopy Shows "Access Denied"
- **Cause:** Permission issues on source or destination folder
- **Fix:**
  - Run Launcher as Administrator
  - Ensure you have read/write permissions on both folders
  - Check if files are locked by another application

### Application Fails to Launch
- **Cause:** Missing `.lnk` shortcut or fallback executable
- **Fix:**
  - Add a `.lnk` shortcut to the app folder's top level, or
  - Ensure `basis\sys\win\bin\dsc_StartPlm.exe` exists in the app folder
  - Check file permissions

### List Not Refreshing After Delete
- **Note:** The list refreshes automatically. If it doesn't, close and reopen Launcher as a workaround.

### "Name Already Exists" Error (Copy Operation)
- **Cause:** A folder with the chosen name already exists in the active launcher directory
- **Fix:** Choose a different name (e.g., add `_v2`, `_backup`, or a timestamp)

### Action Buttons Not Showing / Wrong Order
- **Check** the `actionOrder` field in your config file – if it is an empty array (`[]`) no buttons are shown
- Open **Settings ⚙ → Action Buttons** tab to toggle visibility and reorder using the ↑ / ↓ buttons
- If `actionOrder` is omitted entirely, all four buttons are shown in the default order

### Context Menu Not Appearing
- The context menu may be disabled: open **Settings ⚙ → General** → uncheck **Show right-click context menu**, or set `"showContextMenu": true` in the config file

### Config Not Being Picked Up
- **Check** that the JSON is valid (no trailing commas, all strings quoted)
- **Check** the correct config file is being edited – use the **Settings** dialog (⚙) to see the exact paths that are active for the current instance

---

## Tips & Best Practices

### Running Multiple Launcher Instances

You can run several Launcher windows side-by-side, each pointing to a different folder and carrying its own configuration:

```bat
REM Apps launcher
javaw -jar launcher.jar "D:\Apps" --launcherId=apps

REM Projects launcher
javaw -jar launcher.jar "D:\Projects" --launcherId=projects
```

Each instance reads/writes its own `%APPDATA%\nvLauncher\{launcherId}\config.json`.

### Sharing a Common Base Config

Edit `%APPDATA%\nvLauncher\config.json` to set defaults that apply to **all** instances (e.g. `startMinimized: true`). Instance configs will override only the fields they explicitly set.

### Organizing Your Apps Folder

For best results, structure your launcher folder like this:

```
C:\MyApps\
├── app1\                          ← Application folder (has app1.lnk)
│   ├── app1.lnk
│   └── ... (app files)
├── app2\                          ← Application folder (has fallback exe)
│   ├── basis\
│   │   └── sys\win\bin\
│   │       └── dsc_StartPlm.exe
│   └── ... (app files)
├── docs\                          ← Plain folder (no .lnk, no fallback exe)
│   └── ... (documents)
├── setup.bat                      ← Script
├── deploy.ps1                     ← Script
└── build.cmd                      ← Script
```

### Creating Shortcuts for Applications

To make applications launchable with a double-click:

1. Inside the app folder, create a shortcut (`.lnk`) to your application's executable
2. Name it something memorable (e.g., `app.lnk`)
3. Place it at the **top level** of the folder
4. Launcher will find and launch it automatically

Alternatively, ensure `basis\sys\win\bin\dsc_StartPlm.exe` exists in your application folder.

### Performance Notes

- Launcher scans **only the top level** of your chosen folder (no recursive scanning)
- The first time you open a large folder may take a moment
- List items are sorted by the **priority list** first, then alphabetically: scripts → app folders → plain folders
- Robocopy operations can be slow for large folders — the output window closes automatically when done and the list updates immediately

---

## License

See the `LICENSE` file in this project.

---

## Icon Attribution

Icons used in this application are provided by [Flaticon](https://www.flaticon.com/).
See [`icon-attribution.md`](icon-attribution.md) for the full list of credits.

