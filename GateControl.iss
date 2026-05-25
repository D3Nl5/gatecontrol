#define AppName      "GateControl"
#define AppVersion   "1.0.0"
#define AppPublisher "Hellenic Military"
#define AppExeName   "GateControl.exe"
#define AppId        "{{A1B2C3D4-E5F6-7890-ABCD-EF1234567890}"
#define MainJar      "gatecontrol-1.0.0.jar"

[Setup]
AppId={#AppId}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher={#AppPublisher}
DefaultDirName={autopf}\{#AppName}
DefaultGroupName={#AppName}
OutputDir=installer-output
OutputBaseFilename=GateControl-Setup-{#AppVersion}
SetupIconFile=src\main\resources\app.ico
Compression=lzma2/max
SolidCompression=yes
WizardStyle=modern
PrivilegesRequired=admin
UninstallDisplayIcon={app}\{#AppExeName}
MinVersion=10.0

[Tasks]
Name: "desktopicon"; Description: "Create a desktop shortcut"; GroupDescription: "Additional icons:"

[Files]
; jpackage app image (bundled JRE + JAR)
Source: "jpackage-image\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
; Placeholder db.properties - overwritten after install by [Code] below
Source: "src\main\resources\db.properties"; DestDir: "{app}\app"; Flags: ignoreversion

[Icons]
Name: "{group}\{#AppName}";         Filename: "{app}\{#AppExeName}"
Name: "{commondesktop}\{#AppName}"; Filename: "{app}\{#AppExeName}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#AppExeName}"; Description: "Launch GateControl"; Flags: nowait postinstall skipifsilent

[UninstallDelete]
Type: files; Name: "{app}\app\db.properties"

[Code]

var
  DbPage:       TWizardPage;
  LblHost:      TLabel;
  EdHost:       TEdit;
  LblPort:      TLabel;
  EdPort:       TEdit;
  LblDbName:    TLabel;
  EdDbName:     TEdit;
  LblUsername:  TLabel;
  EdUsername:   TEdit;
  LblPassword:  TLabel;
  EdPassword:   TEdit;
  LblGateName:  TLabel;
  EdGateName:   TEdit;
  ChkEncrypt:   TCheckBox;
  ChkTrustCert: TCheckBox;

procedure CreateDbPage;
var
  Y: Integer;
begin
  DbPage := CreateCustomPage(
    wpSelectDir,
    'Database & Application Configuration',
    'Enter the SQL Server connection details and application settings for GateControl.'
  );

  Y := 8;

  { --- SQL Server Host / Port --- }
  LblHost := TLabel.Create(DbPage);
  LblHost.Parent  := DbPage.Surface;
  LblHost.Caption := 'SQL Server Host / IP:';
  LblHost.Left    := 0;
  LblHost.Top     := Y;
  LblHost.Width   := 200;

  EdHost := TEdit.Create(DbPage);
  EdHost.Parent := DbPage.Surface;
  EdHost.Left   := 0;
  EdHost.Top    := Y + 18;
  EdHost.Width  := 220;
  EdHost.Text   := 'localhost';

  LblPort := TLabel.Create(DbPage);
  LblPort.Parent  := DbPage.Surface;
  LblPort.Caption := 'Port:';
  LblPort.Left    := 230;
  LblPort.Top     := Y;
  LblPort.Width   := 60;

  EdPort := TEdit.Create(DbPage);
  EdPort.Parent := DbPage.Surface;
  EdPort.Left   := 230;
  EdPort.Top    := Y + 18;
  EdPort.Width  := 80;
  EdPort.Text   := '1433';

  Y := Y + 50;

  { --- Database Name --- }
  LblDbName := TLabel.Create(DbPage);
  LblDbName.Parent  := DbPage.Surface;
  LblDbName.Caption := 'Database Name:';
  LblDbName.Left    := 0;
  LblDbName.Top     := Y;
  LblDbName.Width   := 200;

  EdDbName := TEdit.Create(DbPage);
  EdDbName.Parent := DbPage.Surface;
  EdDbName.Left   := 0;
  EdDbName.Top    := Y + 18;
  EdDbName.Width  := 310;
  EdDbName.Text   := 'GateControl';

  Y := Y + 50;

  { --- Username / Password --- }
  LblUsername := TLabel.Create(DbPage);
  LblUsername.Parent  := DbPage.Surface;
  LblUsername.Caption := 'Username:';
  LblUsername.Left    := 0;
  LblUsername.Top     := Y;
  LblUsername.Width   := 150;

  EdUsername := TEdit.Create(DbPage);
  EdUsername.Parent := DbPage.Surface;
  EdUsername.Left   := 0;
  EdUsername.Top    := Y + 18;
  EdUsername.Width  := 150;
  EdUsername.Text   := 'sa';

  LblPassword := TLabel.Create(DbPage);
  LblPassword.Parent  := DbPage.Surface;
  LblPassword.Caption := 'Password:';
  LblPassword.Left    := 160;
  LblPassword.Top     := Y;
  LblPassword.Width   := 150;

  EdPassword := TEdit.Create(DbPage);
  EdPassword.Parent       := DbPage.Surface;
  EdPassword.Left         := 160;
  EdPassword.Top          := Y + 18;
  EdPassword.Width        := 150;
  EdPassword.PasswordChar := '*';

  Y := Y + 54;

  { --- Gate Name --- }
  LblGateName := TLabel.Create(DbPage);
  LblGateName.Parent  := DbPage.Surface;
  LblGateName.Caption := 'Gate Name (shown in movement log):';
  LblGateName.Left    := 0;
  LblGateName.Top     := Y;
  LblGateName.Width   := 310;

  EdGateName := TEdit.Create(DbPage);
  EdGateName.Parent := DbPage.Surface;
  EdGateName.Left   := 0;
  EdGateName.Top    := Y + 18;
  EdGateName.Width  := 310;
  EdGateName.Text   := 'Main Gate';

  Y := Y + 50;

  { --- Connection options --- }
  ChkEncrypt := TCheckBox.Create(DbPage);
  ChkEncrypt.Parent  := DbPage.Surface;
  ChkEncrypt.Caption := 'Encrypt connection';
  ChkEncrypt.Left    := 0;
  ChkEncrypt.Top     := Y;
  ChkEncrypt.Width   := 200;
  ChkEncrypt.Checked := True;

  ChkTrustCert := TCheckBox.Create(DbPage);
  ChkTrustCert.Parent  := DbPage.Surface;
  ChkTrustCert.Caption := 'Trust server certificate (self-signed)';
  ChkTrustCert.Left    := 0;
  ChkTrustCert.Top     := Y + 22;
  ChkTrustCert.Width   := 280;
  ChkTrustCert.Checked := True;
end;

function BuildJdbcUrl: String;
var
  Encrypt:   String;
  TrustCert: String;
begin
  if ChkEncrypt.Checked   then Encrypt   := 'true' else Encrypt   := 'false';
  if ChkTrustCert.Checked then TrustCert := 'true' else TrustCert := 'false';
  Result :=
    'jdbc:sqlserver://' + EdHost.Text + ':' + EdPort.Text +
    ';databaseName='    + EdDbName.Text +
    ';encrypt='         + Encrypt +
    ';trustServerCertificate=' + TrustCert;
end;

function NextButtonClick(CurPageID: Integer): Boolean;
begin
  Result := True;
  if CurPageID = DbPage.ID then
  begin
    if Trim(EdHost.Text) = '' then
    begin
      MsgBox('Please enter the SQL Server host or IP address.', mbError, MB_OK);
      Result := False;
      Exit;
    end;
    if Trim(EdPort.Text) = '' then
    begin
      MsgBox('Please enter the SQL Server port.', mbError, MB_OK);
      Result := False;
      Exit;
    end;
    if Trim(EdDbName.Text) = '' then
    begin
      MsgBox('Please enter the database name.', mbError, MB_OK);
      Result := False;
      Exit;
    end;
    if Trim(EdUsername.Text) = '' then
    begin
      MsgBox('Please enter the database username.', mbError, MB_OK);
      Result := False;
      Exit;
    end;
    if Trim(EdGateName.Text) = '' then
    begin
      MsgBox('Please enter the gate name.', mbError, MB_OK);
      Result := False;
      Exit;
    end;
  end;
end;

procedure InitializeWizard;
begin
  CreateDbPage;
end;

procedure CurStepChanged(CurStep: TSetupStep);
var
  PropertiesPath: String;
  Lines:          TArrayOfString;
  ResultCode:     Integer;
begin
  if CurStep = ssPostInstall then
  begin
    PropertiesPath := ExpandConstant('{app}\app\db.properties');

    { Write db.properties with plain-text password.
      The app will auto-encrypt db.password -> db.password.enc on first startup
      using an AES-256-GCM key derived from this machine's hardware fingerprint. }
    SetArrayLength(Lines, 7);
    Lines[0] := '# GateControl - Database Connection Settings';
    Lines[1] := '# Generated by the installer. Edit to change the connection.';
    Lines[2] := 'db.url=' + BuildJdbcUrl;
    Lines[3] := 'db.username=' + EdUsername.Text;
    Lines[4] := 'db.password=' + EdPassword.Text;
    Lines[5] := 'admin.password.hash=03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4';
    Lines[6] := 'gate.name=' + EdGateName.Text;
    if not SaveStringsToFile(PropertiesPath, Lines, False) then
      MsgBox('Warning: could not write db.properties to ' + PropertiesPath, mbError, MB_OK);

    { ── Step A: encrypt db.password right now, while we have admin rights ──
      Uses the machine's hardware fingerprint (motherboard serial + CPU ID)
      so the ciphertext is bound to this specific machine.
      PropertiesLauncher (layout=ZIP) lets us run any class from the fat JAR
      without starting the Spring Boot application context. }
    Exec(ExpandConstant('{app}\runtime\bin\java.exe'),
         '-Dloader.main=gr.military.gatecontrol.auth.CredentialCrypto'
         + ' -jar "' + ExpandConstant('{app}\app\{#MainJar}') + '"'
         + ' --setup-db "' + PropertiesPath + '"',
         ExpandConstant('{app}'),
         SW_HIDE, ewWaitUntilTerminated, ResultCode);
    if ResultCode <> 0 then
      MsgBox(
        'Warning: installer could not encrypt the database password (exit code '
        + IntToStr(ResultCode) + ').'
        + #13#10 + 'The application will encrypt it automatically on the first run'
        + ' (requires launching as Administrator once).',
        mbInformation, MB_OK);

    { ── Step B: grant Users group Modify on db.properties ───────────────────
      Uses the locale-independent SID *S-1-5-32-545 (BUILTIN\Users) so this
      works on every Windows language.
      /inheritance:d  — disables ACL inheritance from Program Files (which
                         would otherwise override our explicit grant).
      /grant:r        — replaces any existing grants for these principals. }
    Exec(ExpandConstant('{sys}\icacls.exe'),
         '"' + PropertiesPath + '"'
         + ' /inheritance:d'
         + ' /grant:r "*S-1-5-32-544:(F)"'
         + ' /grant:r "*S-1-5-32-545:(M)"'
         + ' /Q',
         '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
  end;
end;
