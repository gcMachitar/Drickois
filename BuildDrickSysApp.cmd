@echo off
setlocal
cd /d "%~dp0"

set "JAR_EXE="

if defined JAVA_HOME if exist "%JAVA_HOME%\bin\jar.exe" set "JAR_EXE=%JAVA_HOME%\bin\jar.exe"
if not defined JAR_EXE for /f "delims=" %%I in ('where.exe jar 2^>nul') do (
  set "JAR_EXE=%%I"
  goto :build
)
if not defined JAR_EXE if exist "C:\Program Files\Java\jdk-24\bin\jar.exe" set "JAR_EXE=C:\Program Files\Java\jdk-24\bin\jar.exe"
if not defined JAR_EXE if exist "C:\Program Files\Android\jdk\jdk-8.0.302.8-hotspot\jdk8u302-b08\bin\jar.exe" set "JAR_EXE=C:\Program Files\Android\jdk\jdk-8.0.302.8-hotspot\jdk8u302-b08\bin\jar.exe"

:build
if not defined JAR_EXE (
  echo jar.exe was not found.
  echo Set JAVA_HOME to your JDK folder or add the JDK bin directory to PATH.
  exit /b 1
)

if not exist "dist" mkdir "dist"

"%JAR_EXE%" cfm "dist\DrickSysApp.jar" "META-INF\MANIFEST.MF" *.class resources inventory.csv products.csv product_recipes.csv item_suppliers.csv
