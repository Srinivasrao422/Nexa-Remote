[Setup]
AppName=Nexa Remote
AppVersion=1.0.1
AppPublisher=Nexa Remote
DefaultDirName={autopf}\Nexa Remote
DefaultGroupName=Nexa Remote
UninstallDisplayIcon={app}\RemoteServer.exe
OutputBaseFilename=Nexa Remote Setup
OutputDir=Nexa Remote V1.0.1 Release\Windows
Compression=lzma2/ultra
SolidCompression=yes
WizardStyle=modern
PrivilegesRequired=admin

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked
Name: "autostart"; Description: "Automatically start Nexa Remote Server on Windows startup"; GroupDescription: "Startup options:"

[Files]
Source: "C:\Users\saina\AndroidStudioProjects\Remote\dist\RemoteServer\RemoteServer.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\saina\AndroidStudioProjects\Remote\dist\RemoteServer\_internal\*"; DestDir: "{app}\_internal"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\Nexa Remote Server"; Filename: "{app}\RemoteServer.exe"
Name: "{group}\{cm:UninstallProgram,Nexa Remote}"; Filename: "{uninstallexe}"
Name: "{autodesktop}\Nexa Remote Server"; Filename: "{app}\RemoteServer.exe"; Tasks: desktopicon

[Registry]
Root: HKCU; Subkey: "Software\Microsoft\Windows\CurrentVersion\Run"; ValueType: string; ValueName: "NexaRemoteServer"; ValueData: """{app}\RemoteServer.exe"""; Flags: uninsdeletevalue; Tasks: autostart

[Run]
Filename: "netsh"; Parameters: "advfirewall firewall add rule name=""Nexa Remote Server (Port 5000)"" dir=in action=allow protocol=TCP localport=5000"; Flags: runhidden
Filename: "netsh"; Parameters: "advfirewall firewall add rule name=""Nexa Remote Server (Port 5001)"" dir=in action=allow protocol=TCP localport=5001"; Flags: runhidden
Filename: "netsh"; Parameters: "advfirewall firewall add rule name=""Nexa Remote Server (Port 5002)"" dir=in action=allow protocol=TCP localport=5002"; Flags: runhidden
Filename: "{app}\RemoteServer.exe"; Description: "Launch Nexa Remote Server"; Flags: nowait postinstall skipifsilent

[UninstallRun]
Filename: "netsh"; Parameters: "advfirewall firewall delete rule name=""Nexa Remote Server (Port 5000)"""; Flags: runhidden
Filename: "netsh"; Parameters: "advfirewall firewall delete rule name=""Nexa Remote Server (Port 5001)"""; Flags: runhidden
Filename: "netsh"; Parameters: "advfirewall firewall delete rule name=""Nexa Remote Server (Port 5002)"""; Flags: runhidden
