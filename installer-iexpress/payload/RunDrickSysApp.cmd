@echo off
cd /d "%~dp0"

where javaw >nul 2>nul
if %errorlevel% neq 0 (
  where java >nul 2>nul
  if %errorlevel% neq 0 (
    powershell -NoProfile -Command "Add-Type -AssemblyName System.Windows.Forms; [System.Windows.Forms.MessageBox]::Show('Java was not found on this PC. Please install Java (JRE/JDK) and try again.','DrickSysApp', 'OK', 'Error') | Out-Null" 2>nul
    echo Java was not found on this PC. Please install Java (JRE/JDK) and try again.
    exit /b 1
  )
)

if exist "dist\DrickSysApp.jar" (
  java -jar "dist\DrickSysApp.jar"
  exit /b %errorlevel%
)

if exist "DrickSysApp.jar" (
  java -jar "DrickSysApp.jar"
  exit /b %errorlevel%
)

echo DrickSysApp.jar was not found. Run BuildDrickSysApp.cmd first.
exit /b 1
