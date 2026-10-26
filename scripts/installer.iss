#define MyAppName "Pulse Music"
#define MyAppVersion "3.2.7"
#define MyAppPublisher "Pulse Music Studio"
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
OutputBaseFilename=PulseMusic-v3.2.7-windows-x64-Setup
SetupIconFile=..\desktopApp\PulseMusic.ico
Compression=lzma2/ultra64
SolidCompression=yes
WizardStyle=modern
UninstallDisplayIcon={app}\PulseMusic.ico
ChangesAssociations=yes
CloseApplications=yes
RestartApplications=yes
CloseApplicationsFilter=Pulse.exe
SignTool=signtool
SignedUninstaller=yes
VersionInfoCompany=Pulse Music Studio
VersionInfoDescription=Pulse Music Desktop Setup
VersionInfoVersion=3.2.7.0
VersionInfoTextVersion=3.2.7
VersionInfoCopyright=Copyright (C) 2026 Pulse Music Studio
VersionInfoProductName=Pulse Music
VersionInfoProductVersion=3.2.7.0

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"

[InstallDelete]
Type: files; Name: "{app}\app\*.jar"
Type: files; Name: "{app}\app\Pulse.cfg"

[Files]
Source: "..\desktopApp\build\compose\binaries\main\app\Pulse\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "..\desktopApp\PulseMusic.ico"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\dist\PulseMusic.cer"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\scripts\trust_publisher.bat"; DestDir: "{app}"; Flags: ignoreversion

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
Filename: "certutil.exe"; Parameters: "-user -addstore -f ""TrustedPublisher"" ""{app}\PulseMusic.cer"""; Flags: runhidden; StatusMsg: "Configuring publisher trust..."
Filename: "certutil.exe"; Parameters: "-user -addstore -f ""TrustedPeople"" ""{app}\PulseMusic.cer"""; Flags: runhidden; StatusMsg: "Configuring publisher trust..."
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent

[Code]
function InitializeSetup(): Boolean;
var
  ErrorCode: Integer;
begin
  ShellExec('open', 'taskkill.exe', '/F /IM Pulse.exe', '', SW_HIDE, ewWaitUntilTerminated, ErrorCode);
  Result := True;
end;

function PrepareToInstall(var NeedsRestart: Boolean): String;
var
  ErrorCode: Integer;
begin
  ShellExec('open', 'taskkill.exe', '/F /IM Pulse.exe', '', SW_HIDE, ewWaitUntilTerminated, ErrorCode);
  Result := '';
end;

procedure CurStepChanged(CurStep: TSetupStep);
var
  ErrorCode: Integer;
begin
  if CurStep = ssPostInstall then
  begin
    if IsAdmin then
    begin
      ShellExec('open', 'certutil.exe', '-addstore -f "Root" "' + ExpandConstant('{app}\PulseMusic.cer') + '"', '', SW_HIDE, ewWaitUntilTerminated, ErrorCode);
      ShellExec('open', 'certutil.exe', '-addstore -f "TrustedPublisher" "' + ExpandConstant('{app}\PulseMusic.cer') + '"', '', SW_HIDE, ewWaitUntilTerminated, ErrorCode);
    end;
  end;
end;

function InitializeUninstall(): Boolean;
var
  ErrorCode: Integer;
begin
  ShellExec('open', 'taskkill.exe', '/F /IM Pulse.exe', '', SW_HIDE, ewWaitUntilTerminated, ErrorCode);
  Result := True;
end;
