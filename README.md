# launcher

A lightweight Java Swing application that lets you browse and launch scripts and application folders from a single root directory.

## Features

- Lists scripts and sub-folders found at the **top level** of a chosen root folder
- **Double-click / Enter** to run a script or launch an application
- **Right-click** an app folder for quick actions: *Open in File Explorer* or *Open in VS Code*
- Optional **system tray** support - start minimized with `--minimized`
- Folder-chooser dialog when no path is supplied

## Prerequisites

- **Java JDK** (version 8 or later) with `javac` and `javaw` available on your `PATH`
  - Tested with [SapMachine 26](https://sap.github.io/SapMachine/), but any standard JDK will work

## Setup

### 1. Install a JDK

Download and install a JDK, then make sure `javac` and `javaw` are on your system `PATH`.

```bat
javac -version   # should print something like: javac 21.0.x
```

### 2. Compile

Run the build script from the project folder:

```bat
build.bat
```

This compiles `Launcher.java` into `Launcher.class` in the same directory.

### 3. Run

**With a folder-chooser dialog** (prompts you to pick a folder):

```bat
run.bat
```

**With a specific folder** (opens that folder directly):

```bat
run.bat  C:\path\to\your\folder
```

**Minimized to the system tray** (edit `example_start_at_logon_in_apps_folder.bat` with your JDK path and target folder, then run it):

```bat
example_start_at_logon_in_apps_folder.bat
```

## File Overview

| File | Description |
|---|---|
| `Launcher.java` | Main application source |
| `build.bat` | Compiles the source with `javac` |
| `run.bat` | Runs the app with an optional folder argument |
| `example_start_at_logon_in_apps_folder.bat` | Example launcher with a configurable folder and `--minimized` flag |

## Auto-start on Logon

There are two common ways to make Launcher start automatically on Windows and open in the tray for a specific folder.

### Option A - Startup Folder (simplest)

1. Press **Win + R**, type `shell:startup`, and press **Enter** - this opens your personal Startup folder.
2. Create a shortcut to `example_start_at_logon_in_apps_folder.bat` (or any `.bat` launcher) inside that folder:
   - Right-click inside the Startup folder -> **New -> Shortcut**
   - Set the target to the full path of the batch file, for example:
     ```
     C:\path\to\launcher\example_start_at_logon_in_apps_folder.bat
     ```
   - Give the shortcut a name (e.g. *Launcher*) and click **Finish**.
3. Log off and back on - Launcher will start automatically.

> **Tip:** To start minimized to the tray, make sure `example_start_at_logon_in_apps_folder.bat` includes the `--minimized` flag (see the customisation section below). You can also set the shortcut's *Run* property to **Minimised** so the console window doesn't flash up.

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
| Program/script | `C:\path\to\launcher\example_start_at_logon_in_apps_folder.bat` |
| Start in | `C:\path\to\launcher` |

4. Click **Finish**.
5. Find the newly created task in the list, right-click it -> **Properties**:
   - On the **General** tab, check **Run only when user is logged on**.
   - *(Optional)* Check **Run with highest privileges** if the app needs elevation.
6. Click **OK** - the task will run automatically at your next logon.

To test it immediately without logging off, right-click the task and choose **Run**.

---

## Customising `example_start_at_logon_in_apps_folder.bat`

Open the file and update the two values to match your environment:

```bat
start "" C:\path\to\your\jdk\bin\javaw Launcher "C:\path\to\your\folder" --minimized
```

- Replace `C:\path\to\your\jdk\bin\javaw` with the full path to `javaw.exe` in your JDK
- Replace `C:\path\to\your\folder` with the root folder you want to launch from
