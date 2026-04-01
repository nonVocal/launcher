@echo off
:: Compile Launcher.java
:: Requires Java JDK (javac) on PATH.

echo Compiling Launcher.java ...
javac Launcher.java

if %ERRORLEVEL% EQU 0 (
    echo.
    echo Build successful.  Run:  java Launcher [folder]
) else (
    echo.
    echo Build FAILED.
    pause
)
