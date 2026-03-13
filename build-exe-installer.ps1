$ErrorActionPreference = "Stop"

function Assert-Command($name) {
  $cmd = Get-Command $name -ErrorAction SilentlyContinue
  if (-not $cmd) {
    throw "Required command not found on PATH: $name"
  }
}

Assert-Command "javac"
Assert-Command "java"
Assert-Command "iexpress.exe"

Write-Host "Compiling..." -ForegroundColor Cyan
javac -encoding UTF-8 *.java

Write-Host "Building jar..." -ForegroundColor Cyan
& .\BuildDrickSysApp.cmd | Out-Host

$jarPath = Join-Path $PSScriptRoot "dist\DrickSysApp.jar"
if (-not (Test-Path $jarPath)) {
  throw "Jar not found at $jarPath"
}

$payloadDir = Join-Path $PSScriptRoot "installer-iexpress\payload"
New-Item -ItemType Directory -Force -Path $payloadDir | Out-Null

Copy-Item -Force $jarPath (Join-Path $payloadDir "DrickSysApp.jar")
Copy-Item -Force (Join-Path $PSScriptRoot "RunDrickSysApp.cmd") (Join-Path $payloadDir "RunDrickSysApp.cmd")
if (Test-Path (Join-Path $PSScriptRoot "supabase.properties.example")) {
  Copy-Item -Force (Join-Path $PSScriptRoot "supabase.properties.example") (Join-Path $payloadDir "supabase.properties.example")
}
Copy-Item -Force (Join-Path $PSScriptRoot "installer-iexpress\install.cmd") (Join-Path $payloadDir "install.cmd")
Copy-Item -Force (Join-Path $PSScriptRoot "installer-iexpress\install.ps1") (Join-Path $payloadDir "install.ps1")

$outputExe = Join-Path $PSScriptRoot "dist\DrickSysApp-Setup.exe"
$sedPath = Join-Path $PSScriptRoot "installer-iexpress\DrickSysApp-Setup.sed"

$payloadAbs = [IO.Path]::GetFullPath($payloadDir)
$outputAbs = [IO.Path]::GetFullPath($outputExe)

$sed = @"
[Version]
Class=IEXPRESS
SEDVersion=3

[Options]
PackagePurpose=InstallApp
ShowInstallProgramWindow=0
HideExtractAnimation=1
UseLongFileName=1
InsideCompressed=1
CAB_FixedSize=0
CAB_ResvCodeSigning=0
RebootMode=N
InstallPrompt=
DisplayLicense=
FinishMessage=
TargetName=$outputAbs
FriendlyName=DrickSysApp Setup
AppLaunched=install.cmd
PostInstallCmd=
AdminQuietInstCmd=
UserQuietInstCmd=
SourceFiles=SourceFiles

[Strings]
InstallPromptTitle=DrickSysApp

[SourceFiles]
SourceFiles0=$payloadAbs

[SourceFiles0]
%FILE0%=DrickSysApp.jar
%FILE1%=RunDrickSysApp.cmd
%FILE2%=install.cmd
%FILE3%=install.ps1
%FILE4%=supabase.properties.example
"@

Set-Content -Encoding ASCII -Path $sedPath -Value $sed

Write-Host "Building wizard EXE installer..." -ForegroundColor Cyan
& iexpress.exe /N /Q $sedPath | Out-Host

if (-not (Test-Path $outputExe)) {
  throw "Installer EXE was not created: $outputExe"
}

Write-Host "Done." -ForegroundColor Green
Write-Host "Jar: $jarPath"
Write-Host "Installer: $outputExe"

