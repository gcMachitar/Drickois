$ErrorActionPreference = "Stop"

function Resolve-JPackagePath {
  $candidates = @()
  if ($env:JAVA_HOME) {
    $candidates += (Join-Path $env:JAVA_HOME "bin\\jpackage.exe")
  }
  $candidates += "C:\\Program Files\\Java\\jdk-24\\bin\\jpackage.exe"
  $candidates += "C:\\Program Files\\Java\\jdk-23\\bin\\jpackage.exe"
  $candidates += "C:\\Program Files\\Java\\jdk-22\\bin\\jpackage.exe"

  foreach ($path in $candidates) {
    if ($path -and (Test-Path $path)) {
      return $path
    }
  }
  throw "jpackage.exe not found. Set JAVA_HOME to a JDK, or update build-release-jpackage.ps1 with your JDK path."
}

Write-Host "Building jar..." -ForegroundColor Cyan
& .\\BuildDrickSysApp.cmd | Out-Host

$jarPath = Join-Path $PSScriptRoot "dist\\DrickSysApp.jar"
if (-not (Test-Path $jarPath)) {
  throw "Jar not found at $jarPath"
}

$iconPath = Join-Path $PSScriptRoot "resources\\myicon.ico"
if (-not (Test-Path $iconPath)) {
  throw "Icon not found at $iconPath"
}

$destDir = Join-Path $PSScriptRoot "release"
New-Item -ItemType Directory -Force -Path $destDir | Out-Null

$jpackage = Resolve-JPackagePath
Write-Host "Packaging EXE installer with jpackage..." -ForegroundColor Cyan

& $jpackage `
  --type exe `
  --name "DrickSys" `
  --app-version "1.0.0" `
  --input (Join-Path $PSScriptRoot "dist") `
  --main-jar "DrickSysApp.jar" `
  --main-class "DrickSysApp" `
  --java-options "-Djava.net.preferIPv4Stack=true" `
  --java-options "-Djava.net.preferIPv6Addresses=false" `
  --icon $iconPath `
  --dest $destDir `
  --win-menu `
  --win-shortcut `
  --win-dir-chooser | Out-Host

Write-Host "Done." -ForegroundColor Green
Write-Host "Jar: $jarPath"
Write-Host "Installer output: $destDir"
