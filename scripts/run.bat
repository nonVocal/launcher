@echo off
:: Run the Launcher application.
:: Usage:
::   run.bat                       – opens a folder-chooser dialog
::   run.bat  C:\path\to\folder   – opens that folder directly

if "%~1"=="" (
    java Launcher
) else (
    java Launcher "%~1"
)
