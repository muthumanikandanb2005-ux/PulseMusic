#define MyAppName "Pulse Music"
#define MyAppVersion "2.2.7"
#define MyAppPublisher "Pulse Music Studio (Muthumanikandan B)"
#define MyAppURL "https://github.com/muthumanikandanb2005-ux/PulseMusic"
#define MyAppExeName "Pulse.exe"
#define MyAppAppUserModelId "com.pulse.music"

[Setup]
AppId={{D4B386A2-15CE-4B19-8AE1-7DF29B96A820}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
DefaultDirName={localappdata}\Programs\{#MyAppName}
DefaultGroupName={#MyAppName}
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
OutputDir=..\dist
OutputBaseFilename=PulseMusic-Windows-x64-Setup
SetupIconFile=..\desktopApp\PulseMusic.ico
Compression=lzma2/ultra64
SolidCompression=yes
WizardStyle=modern
UninstallDisplayIcon={app}\PulseMusic.ico
ChangesAssociations=yes
VersionInfoCompany=Pulse Music Studio
VersionInfoDescription=Pulse Music Desktop Setup
VersionInfoVersion=2.2.7.0
VersionInfoTextVersion=2.2.7
VersionInfoCopyright=Copyright (C) 2026 Pulse Music Studio
VersionInfoProductName=Pulse Music
VersionInfoProductVersion=2.2.7.0

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "..\desktopApp\build\compose\binaries\main-release\app\Pulse\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "..\desktopApp\PulseMusic.ico"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{autoprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; IconFilename: "{app}\PulseMusic.ico"; AppUserModelID: "{#MyAppAppUserModelId}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; IconFilename: "{app}\PulseMusic.ico"; AppUserModelID: "{#MyAppAppUserModelId}"; Tasks: desktopicon

[Registry]
Root: HKCU; Subkey: "Software\Classes\pulsemusic"; ValueType: string; ValueName: ""; ValueData: "URL:Pulse Music Protocol"; Flags: uninsdeletekey
Root: HKCU; Subkey: "Software\Classes\pulsemusic"; ValueType: string; ValueName: "URL Protocol"; ValueData: ""; Flags: uninsdeletekey
Root: HKCU; Subkey: "Software\Classes\pulsemusic\DefaultIcon"; ValueType: string; ValueName: ""; ValueData: "{app}\PulseMusic.ico"; Flags: uninsdeletekey
Root: HKCU; Subkey: "Software\Classes\pulsemusic\shell\open\command"; ValueType: string; ValueName: ""; ValueData: """{app}\{#MyAppExeName}"" ""%1"""; Flags: uninsdeletekey
Root: HKCU; Subkey: "Software\Classes\Applications\{#MyAppExeName}"; ValueType: string; ValueName: ""; ValueData: "{#MyAppName}"; Flags: uninsdeletekey
Root: HKCU; Subkey: "Software\Classes\Applications\{#MyAppExeName}"; ValueType: string; ValueName: "FriendlyAppName"; ValueData: "{#MyAppName}"; Flags: uninsdeletekey
Root: HKCU; Subkey: "Software\Classes\Applications\{#MyAppExeName}"; ValueType: string; ValueName: "ApplicationCompany"; ValueData: "{#MyAppPublisher}"; Flags: uninsdeletekey
Root: HKCU; Subkey: "Software\Classes\Applications\{#MyAppExeName}\DefaultIcon"; ValueType: string; ValueName: ""; ValueData: "{app}\PulseMusic.ico"; Flags: uninsdeletekey
Root: HKCU; Subkey: "Software\Classes\AppUserModelId\{#MyAppAppUserModelId}"; ValueType: string; ValueName: "DisplayName"; ValueData: "{#MyAppName}"; Flags: uninsdeletekey
Root: HKCU; Subkey: "Software\Classes\AppUserModelId\{#MyAppAppUserModelId}"; ValueType: string; ValueName: "IconUri"; ValueData: "{app}\PulseMusic.ico"; Flags: uninsdeletekey
Root: HKCU; Subkey: "Software\Classes\AppUserModelId\{#MyAppAppUserModelId}"; ValueType: string; ValueName: "IconBackgroundColor"; ValueData: "0"; Flags: uninsdeletekey
Root: HKCU; Subkey: "Software\Microsoft\Windows\CurrentVersion\App Paths\{#MyAppExeName}"; ValueType: string; ValueName: ""; ValueData: "{app}\{#MyAppExeName}"; Flags: uninsdeletekey
Root: HKCU; Subkey: "Software\Microsoft\Windows\CurrentVersion\App Paths\{#MyAppExeName}"; ValueType: string; ValueName: "Path"; ValueData: "{app}"; Flags: uninsdeletekey

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent
