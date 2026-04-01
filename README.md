# launcher

A lightweight Java Swing application that lets you browse and launch scripts and application folders from a single root directory.

## Features

- **Lists** scripts and sub-folders found at the **top level** of a chosen root folder
- **Double-click / Enter** to run a script or launch an application
- **Right-click context menu** (app folders only) with quick actions:
  - *Open in File Explorer* – browse the folder in Windows Explorer
  - *Open in VS Code* – open the folder in Visual Studio Code
  - *Copy with Robocopy* – duplicate the folder within the active launcher directory (with validation)
  - *Delete* – permanently delete the folder (with confirmation)
  - *SVN Checkout* – check out a repository into the folder
- **Real-time output windows** for robocopy and SVN operations
- Optional **system tray** support – start minimized with `--minimized`
- **Folder-chooser dialog** when no path is supplied on startup
- **Syntax highlighting** for scripts vs. application folders
- **Keyboard shortcuts** for common operations

## Prerequisites

- **Java JDK** (version 8 or later) with `javac` and `javaw` available on your `PATH`
  - Tested with [SapMachine 26](https://sap.github.io/SapMachine/), but any standard JDK will work
- *(Optional)* **`code`** command (from VS Code) on your `PATH` – only needed for the *Open in VS Code* action
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

**Minimized to the system tray**:

Create a batch file (see *Creating a Batch File to Start Launcher in a Folder at Logon* section) and run it:

```bat
example_start_at_logon_in_apps_folder.bat
```

## Usage Guide

### Launching Scripts and Applications

- **Double-click** or press **Enter** on a list item to launch it:
  - **Scripts** (.bat, .cmd, .ps1, .vbs, .sh, .js) open in their respective interpreter/shell
  - **Application folders** search for and launch `.lnk` shortcuts or the fallback executable

### Right-Click Context Menu (App Folders Only)

Right-click any application folder to access:

#### **Open in File Explorer**
Opens the folder in Windows File Explorer for browsing and file management.

#### **Open in VS Code**
Opens the folder in Visual Studio Code. Requires `code` command on your `PATH`.

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
- Checks out into a new subfolder inside the selected app folder
- Shows real-time command output in a dedicated window that stays open
- After the checkout finishes, a prompt asks whether to refresh the file list
- Requires `svn` command on your `PATH`

## How Application Launching Works

When you double-click an application folder, Launcher uses the following search strategy:

1. **Search for Windows shortcuts** – Looks for the first `.lnk` shortcut at the folder's top level
   - If found, executes it via the Windows shell
2. **Fallback executable** – If no shortcut is found, looks for:
   - `basis\sys\win\bin\dsc_StartPlm.exe` inside the application folder
   - If found, executes it directly
3. **Error handling** – If neither method succeeds, shows a warning dialog with diagnostic information

This two-stage approach gives you flexibility in how applications are structured.

## File Overview

| File | Description |
|---|---|
| `src/main/java/dev/nonvocal/launcher/Launcher.java` | Main application source (771 lines) |
| `pom.xml` | Maven configuration (JDK 26, JUnit 5) |
| `scripts/build.bat` | Compiles the source with `javac` |
| `scripts/run.bat` | Runs the app with an optional folder argument |
| `example_start_at_logon_in_apps_folder.bat` | Example launcher batch file for starting minimized to the tray |
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

## Keyboard Shortcuts & Tips

| Action | Keyboard / Mouse |
|---|---|
| Launch selected item | **Enter** or **Double-click** |
| Open context menu | **Right-click** (app folders only) |
| Navigate list | **↑ ↓** Arrow keys |
| Minimize to tray | Close window (if started with `--minimized`) |
| Show/Hide from tray | **Double-click** tray icon |

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

start "" C:\path\to\your\jdk\bin\javaw Launcher "C:\path\to\your\apps\folder" --minimized
```

### Customising the Batch File

Update the following values to match your environment:

- **`C:\path\to\your\jdk\bin\javaw`** – Replace with the full path to `javaw.exe` in your JDK installation
- **`C:\path\to\your\apps\folder`** – Replace with the root folder you want Launcher to browse

### Example

If you have:
- Java installed at `C:\Program Files\SapMachine\jdk-26\bin\javaw.exe`
- Launcher class in `D:\Launcher\Launcher.class`
- Apps folder at `D:\MyApps`

Your batch file would be:

```bat
@echo off
start "" "C:\Program Files\SapMachine\jdk-26\bin\javaw" -cp "D:\Launcher" Launcher "D:\MyApps" --minimized
```

---

## Troubleshooting

### "Could not open VS Code" Error
- **Cause:** `code` command is not on your `PATH`
- **Fix:** 
  - Install VS Code from [code.visualstudio.com](https://code.visualstudio.com/)
  - During installation, check "Add to PATH"
  - Or manually add VS Code's bin folder to your PATH environment variable

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

---

## Tips & Best Practices

### Organizing Your Apps Folder

For best results, structure your launcher folder like this:

```
C:\MyApps\
├── app1\
│   ├── app1.lnk         (shortcut to executable)
│   └── ... (app files)
├── app2\
│   ├── basis\
│   │   └── sys\win\bin\
│   │       └── dsc_StartPlm.exe
│   └── ... (app files)
├── setup.bat
├── deploy.ps1
└── build.cmd
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
- List items are sorted alphabetically: scripts first, then app folders
- Robocopy operations can be slow for large folders — the output window closes automatically when done and the list updates immediately

---

## License

See the `LICENSE` file in this project.

