@echo off
cd /d "%~dp0"
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
