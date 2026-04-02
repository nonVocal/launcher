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
  - **SVN Checkout** (left) – check out a repository into the selected folder
  - **Settings ⚙** (right) – open the settings dialog
- **Inline action icons** (right side of each folder row) for one-click access to common actions — no right-click required:
  - 📁 – Open in File Explorer
  - 📝 – Open in Editor
  - 📋 – Copy with Robocopy
  - 🗑 – Delete
  - Which actions are shown, and their **order**, are fully **configurable** via the Settings dialog or the config file
  - **Button style** is configurable: individual **icons** (default) or a single **hamburger menu** (☰) that opens a popup
  - The cursor changes to a **hand pointer** when hovering over an icon
- **Right-click context menu** (folders and app folders) with the same configurable quick actions plus **SVN Checkout**
  - The context menu can be **disabled** via the Settings dialog or the `showContextMenu` config field
- **Real-time output windows** for robocopy and SVN operations
- **Three-level configuration** stored in `%APPDATA%\nvLauncher\` — global defaults, per-instance overrides, and an optional explicit config file
- **Settings dialog** to inspect active config paths, toggle startup options, configure commands (`EXPLORER`, `EDITOR`), and customize the action button bar
- Optional **system tray** support – start minimized with `--minimized`; **single-click** the tray icon to show/hide
- **Folder-chooser dialog** when no path is supplied on startup
- **Color-coded list** for scripts, application folders, and plain folders

## Prerequisites

- **Java JDK 16 or later** with `javac` and `javaw` available on your `PATH`
  - Records (used internally) require Java 16+
  - Tested with [SapMachine 26](https://sap.github.io/SapMachine/), but any JDK 16+ will work
- *(Optional)* An **editor** on your `PATH` – only needed for the *Open in Editor* action. Defaults to `code` (VS Code); configurable via `EDITOR` in the config file or Settings dialog
- *(Optional)* A custom **file explorer** – defaults to the system default; configurable via `EXPLORER` in the config file or Settings dialog
- *(Optional)* **SVN client** on your `PATH` – only needed for the *SVN Checkout* action
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
    "DELETE_ACTION"
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
| `actionOrder` | string array | Ordered list of **action keys** that determines which action buttons are shown and in what order. Omit to show all four actions in the default order. |

### Action Keys

The `actionOrder` field uses the following keys:

| Key | Action |
|---|---|
| `EXPLORE_ACTION` | Open in File Explorer |
| `EDITOR_ACTION` | Open in Editor |
| `COPY_ACTION` | Copy with Robocopy |
| `DELETE_ACTION` | Delete |

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
| **SVN Checkout** (apps-add icon) | Left | Opens the SVN Checkout dialog for the currently selected folder. If no folder entry is selected, the root launcher folder is used as the checkout target. |
| **Settings** (gear icon) | Right | Opens the Settings dialog to inspect config-file paths and toggle startup options. |

### Settings Dialog

Open by clicking the **⚙ gear icon** on the right side of the toolbar.

| Section | Field | Description |
|---|---|---|
| Configuration files | Launcher ID | The active instance identifier (read-only) |
| Configuration files | Global config | Path to `%APPDATA%\nvLauncher\config.json` with 📂 button to open the folder |
| Configuration files | Instance config | Path to `%APPDATA%\nvLauncher\{id}\config.json` with 📂 button to open the folder |
| Startup | Start minimized | Checkbox – saves to instance config; takes effect on next launch |
| Commands | EXPLORER | File explorer command. Leave blank to use the system default. |
| Commands | EDITOR | Editor command (e.g. `code`, `notepad++`). Leave blank to default to `code`. |
| Action Buttons | Action list | Checkboxes to show/hide each action; **↑ / ↓** buttons to reorder. Changes take effect immediately without a restart. |
| Button Style | Style radio buttons | Choose **Inline icons** (one button per action, default) or **Hamburger menu** (single ☰ button that opens a popup). Takes effect immediately. |
| Button Style | Show context menu | Checkbox – uncheck to disable the right-click context menu on folder entries. |

Click **Save** to persist changes or **Cancel** to discard.

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

#### **SVN Checkout...**
- Prompts you to enter an SVN repository URL
- Automatically derives the checkout folder name from the URL
- Checks out into a new subfolder inside the selected folder
- Shows real-time command output in a dedicated window that stays open
- After the checkout finishes, a prompt asks whether to refresh the file list
- Requires `svn` command on your `PATH`

## How Application Launching Works

When you double-click a list item, the behaviour depends on its category:

### Scripts
Executed in the appropriate interpreter — see *Supported Script Types* for details.

### Application Folders
Launcher uses the following two-stage strategy:

1. **Search for Windows shortcuts** – Looks for the first `.lnk` shortcut at the folder's top level
   - If found, executes it via the Windows shell
2. **Fallback executable** – If no shortcut is found, looks for:
   - `basis\sys\win\bin\dsc_StartPlm.exe` inside the application folder
   - If found, executes it directly
3. **Error handling** – If neither is present the folder will be shown as a *plain folder* rather than an application folder (it would not have been classified as one to begin with)

### Plain Folders
Double-clicking a plain folder opens it in **Windows File Explorer**. All right-click actions (copy, delete, SVN checkout, open in editor) are still available.

### Folder Classification

A sub-folder is classified as an **Application folder** when it meets at least one of these criteria:
- Contains a `.lnk` shortcut file at its **top level**
- Contains `basis\sys\win\bin\dsc_StartPlm.exe`

All other sub-folders are treated as **plain folders**.

## File Overview

### Source files

| File | Description |
|---|---|
| `src/main/java/dev/nonvocal/launcher/Launcher.java` | Application entry point and UI shell – constructs the window, wires all helper classes together, manages config state (~600 lines) |
| `src/main/java/dev/nonvocal/launcher/LauncherConfig.java` | Config record – JSON load/save/merge, three-level override logic (`empty` → `defaults` → `mergeOver` → `withDefaults`) |
| `src/main/java/dev/nonvocal/launcher/EntryType.java` | Enum with three values: `SCRIPT`, `APP_FOLDER`, `PLAIN_FOLDER` |
| `src/main/java/dev/nonvocal/launcher/LaunchEntry.java` | Immutable record representing one list row: `file()`, `type()`, `iconFile()` |
| `src/main/java/dev/nonvocal/launcher/EntryLoader.java` | Scans the root folder, classifies entries into the three types, and applies priority-list sorting |
| `src/main/java/dev/nonvocal/launcher/EntryLauncher.java` | Launches scripts (bat, cmd, ps1, …) and application folders (via `.lnk` or fallback executable) |
| `src/main/java/dev/nonvocal/launcher/FolderActions.java` | Folder-level operations: open in File Explorer, open in Editor, copy with Robocopy, delete, SVN checkout |
| `src/main/java/dev/nonvocal/launcher/EntryCellRenderer.java` | Swing list-cell renderer – draws entry rows with inline icon buttons (`ICONS`) or a single hamburger button (`HAMBURGER`) |
| `src/main/java/dev/nonvocal/launcher/ListMouseHandler.java` | `MouseAdapter` – handles single-click on action buttons, double-click to launch, hover cursor changes, and right-click context menu |
| `src/main/java/dev/nonvocal/launcher/EntryListTransferHandler.java` | `TransferHandler` for drag-and-drop reordering of list entries (disabled while a search filter is active) |
| `src/main/java/dev/nonvocal/launcher/SettingsDialog.java` | Modal settings `JDialog` – config-file paths, startup options, EXPLORER/EDITOR commands, action-button order/visibility, button style, context-menu toggle |
| `src/main/java/dev/nonvocal/launcher/ProcessOutputWindow.java` | Utility that streams real-time process output (robocopy, SVN) into a dedicated, auto-closing window |

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
| SVN Checkout | **Toolbar button** (left) or right-click → *SVN Checkout…* |
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
  - Open **Settings ⚙** → **EDITOR** field and enter the full path to the executable, e.g. `C:\Program Files\Microsoft VS Code\bin\code.cmd`
  - Or add the editor's folder to your PATH environment variable
  - Or install VS Code from [code.visualstudio.com](https://code.visualstudio.com/) and check "Add to PATH" during installation

### SVN Checkout Fails
- **Cause:** SVN client is not installed or not on `PATH`
- **Fix:**
  - Download and install TortoiseSVN or Apache Subversion
  - Ensure the `svn` command is available in your PATH
  - Verify with `svn --version` in Command Prompt

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
- Open **Settings ⚙** → **Action Buttons** section to toggle visibility and reorder using the ↑ / ↓ buttons
- If `actionOrder` is omitted entirely, all four buttons are shown in the default order

### Context Menu Not Appearing
- The context menu may be disabled: open **Settings ⚙** → uncheck **Show right-click context menu**, or set `"showContextMenu": true` in the config file

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

