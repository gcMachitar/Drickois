$ErrorActionPreference = "Stop"

function Show-Error([string] $message) {
  try {
    Add-Type -AssemblyName System.Windows.Forms | Out-Null
    [System.Windows.Forms.MessageBox]::Show($message, "DrickSysApp Setup", "OK", "Error") | Out-Null
  } catch {
    Write-Error $message
  }
}

function Show-Info([string] $message) {
  try {
    Add-Type -AssemblyName System.Windows.Forms | Out-Null
    [System.Windows.Forms.MessageBox]::Show($message, "DrickSysApp Setup", "OK", "Information") | Out-Null
  } catch {
    Write-Host $message
  }
}

$javaCmd = Get-Command javaw -ErrorAction SilentlyContinue
if (-not $javaCmd) { $javaCmd = Get-Command java -ErrorAction SilentlyContinue }
if (-not $javaCmd) {
  Show-Error "Java was not found on this PC. Install Java (JRE/JDK) first, then run the installer again."
  exit 1
}

$srcDir = Split-Path -Parent $PSCommandPath
$jarSrc = Join-Path $srcDir "DrickSysApp.jar"
$runSrc = Join-Path $srcDir "RunDrickSysApp.cmd"

if (-not (Test-Path $jarSrc)) { Show-Error "Missing payload file: DrickSysApp.jar"; exit 1 }
if (-not (Test-Path $runSrc)) { Show-Error "Missing payload file: RunDrickSysApp.cmd"; exit 1 }

$targetRoot = Join-Path $env:LOCALAPPDATA "DrickSysApp"
$appDir = Join-Path $targetRoot "app"
New-Item -ItemType Directory -Force -Path $appDir | Out-Null

Copy-Item -Force $jarSrc (Join-Path $appDir "DrickSysApp.jar")
Copy-Item -Force $runSrc (Join-Path $appDir "RunDrickSysApp.cmd")

$exampleSrc = Join-Path $srcDir "supabase.properties.example"
if (Test-Path $exampleSrc) {
  Copy-Item -Force $exampleSrc (Join-Path $targetRoot "supabase.properties.example")
}

try {
  $shell = New-Object -ComObject WScript.Shell

  $desktop = [Environment]::GetFolderPath("Desktop")
  $shortcutPath = Join-Path $desktop "DrickSysApp.lnk"
  $shortcut = $shell.CreateShortcut($shortcutPath)
  $shortcut.TargetPath = (Join-Path $appDir "RunDrickSysApp.cmd")
  $shortcut.WorkingDirectory = $appDir
  $shortcut.Save()

  $programs = [Environment]::GetFolderPath("Programs")
  $startMenuDir = Join-Path $programs "DrickSysApp"
  New-Item -ItemType Directory -Force -Path $startMenuDir | Out-Null
  $startShortcutPath = Join-Path $startMenuDir "DrickSysApp.lnk"
  $startShortcut = $shell.CreateShortcut($startShortcutPath)
  $startShortcut.TargetPath = (Join-Path $appDir "RunDrickSysApp.cmd")
  $startShortcut.WorkingDirectory = $appDir
  $startShortcut.Save()
} catch {
  Show-Info "Installed files, but shortcut creation failed:`n$($_.Exception.Message)"
}

Show-Info "Installed to:`n$appDir`n`nA shortcut was added to Desktop and Start Menu."

try {
  Start-Process -FilePath (Join-Path $appDir "RunDrickSysApp.cmd") -WorkingDirectory $appDir
} catch {
  Show-Info "Install complete, but auto-launch failed:`n$($_.Exception.Message)`n`nYou can start the app from the shortcut."
}

