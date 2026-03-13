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
TargetName=C:\Users\Admin\Downloads\Compressed\Drickoi's\Drickois\dist\DrickSysApp-Setup.exe
FriendlyName=DrickSysApp Setup
AppLaunched=install.cmd
PostInstallCmd=
AdminQuietInstCmd=
UserQuietInstCmd=
SourceFiles=SourceFiles

[Strings]
InstallPromptTitle=DrickSysApp

[SourceFiles]
SourceFiles0=C:\Users\Admin\Downloads\Compressed\Drickoi's\Drickois\installer-iexpress\payload

[SourceFiles0]
%FILE0%=DrickSysApp.jar
%FILE1%=RunDrickSysApp.cmd
%FILE2%=install.cmd
%FILE3%=install.ps1
%FILE4%=supabase.properties.example
