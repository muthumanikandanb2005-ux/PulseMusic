#define MyAppName "Pulse Music"
#define MyAppVersion "2.2.6"
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

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "..\desktopApp\build\compose\binaries\main\app\Pulse\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "..\desktopApp\PulseMusic.ico"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{autoprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; IconFilename: "{app}\PulseMusic.ico"; AppUserModelID: "{#MyAppAppUserModelId}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; IconFilename: "{app}\PulseMusic.ico"; AppUserModelID: "{#MyAppAppUserModelId}"; Tasks: desktopicon

[Registry]
Root: HKCU; Subkey: "Software\Classes\pulsemusic"; ValueType: string; ValueName: ""; ValueData: "URL:Pulse Music Protocol"; Flags: uninsdeletekey
Root: HKCU; Subkey: "Software\Classes\pulsemusic"; ValueType: string; ValueName: "URL Protocol"; ValueData: ""; Flags: uninsdeletekey
Root: HKCU; Subkey: "Software\Classes\pulsemusic\DefaultIcon"; ValueType: string; ValueName: ""; ValueData: "{app}\PulseMusic.ico"; Flags: uninsdeletekey
Root: HKCU; Subkey: "Software\Classes\pulsemusic\shell\open\command"; ValueType: string; ValueName: ""; ValueData: """{app}\{#MyAppExeName}"" ""%1"""; Flags: uninsdeletekey

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent
