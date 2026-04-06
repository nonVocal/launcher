@echo off
::
:: Maven build script for the Launcher project.
:: Sets JAVA_HOME to JDK 26 (required for jpackage) and runs "mvn package".
::
:: Output after a successful build:
::   target\launcher-0.0.3.jar          <- fat / uber-JAR (requires JRE)
::   target\dist\Launcher\Launcher.exe  <- self-contained exe (JRE bundled)
::   target\dist\Launcher\runtime\      <- bundled JRE
::
:: Distribute the entire  target\dist\Launcher\  folder;
:: users can run Launcher.exe without installing Java.
::

:: ── JDK 26 ────────────────────────────────────────────────────────────────
set "JAVA_HOME=C:\Users\benda\.jdks\openjdk-26"
set "PATH=%JAVA_HOME%\bin;%PATH%"

:: ── Maven  (IntelliJ-bundled) ──────────────────────────────────────────────
set "MVN=C:\Program Files\JetBrains\IntelliJ IDEA 261.20362.25\plugins\maven\lib\maven3\bin\mvn.cmd"

:: ── project root (one level above scripts\) ───────────────────────────────
set "PROJECT_DIR=%~dp0.."
cd /d "%PROJECT_DIR%"

echo.
echo ========================================================
echo  Building Launcher with JDK 26 + jpackage
echo ========================================================
echo.

call "%MVN%" package -DskipTests

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================================
    echo  Build successful!
    echo  Self-contained app: target\dist\Launcher\Launcher.exe
    echo ========================================================
) else (
    echo.
    echo Build FAILED.
    pause
)
