# GateControl

Desktop application for military gate access control. Manages personnel entry/exit using RFID cards or NFC-enabled phones, logs all movements, and provides real-time presence monitoring.

Built with Java 21, Spring Boot 3, JavaFX 21, and SQL Server.

---

## Features

- Real-time RFID / NFC scanning — automatic entry/exit detection
- Personnel roster with rank, name, unit, and photo
- Live statistics: total strength, personnel inside / outside
- Movement history with date range filters and color-coded entries
- Admin functions protected by encrypted password
- Manual movement entry (e.g. reader failure, lost card)
- Excel export of movement history and inside-personnel list
- Webcam photo capture for personnel registration
- Database backup with timestamp
- Compact / normal display modes, full-screen support
- Windows installer (`GateControl-Setup.exe`)
- NFC phone support — Android phones can act as access cards

---

## Access Levels

| Role | Permissions |
|------|-------------|
| Operator | RFID scan, view personnel list, view movement history |
| Administrator | All of the above + manage personnel, manual movement, Excel export, database backup, change admin password |

---

## System Requirements

| Component | Requirement |
|-----------|-------------|
| OS | Windows 10 / Windows 11 (64-bit) |
| CPU | Intel Core i3 or better |
| RAM | 4 GB minimum |
| Disk | 500 MB free |
| Database | Microsoft SQL Server 2019 or later (Express edition is sufficient) |
| RFID / NFC device | USB reader |
| Camera | USB Webcam (optional — for personnel photos) |

---

## Installation

### 1. Install SQL Server

Download **SQL Server Express** (free): https://www.microsoft.com/en-us/sql-server/sql-server-downloads

Run the installer as Administrator → select **Basic** installation.  
Note the instance name created (e.g. `SQLEXPRESS` or `MSSQLSERVER`).

### 2. Configure SQL Server — enable TCP/IP on port 1433

Open **SQL Server Configuration Manager**:

1. Go to: `SQL Server Network Configuration → Protocols for [INSTANCE]`
2. Right-click **TCP/IP** → **Enable**
3. Double-click **TCP/IP** → tab **IP Addresses** → scroll to **IPAll**
4. Set `TCP Port = 1433` — clear any value in `TCP Dynamic Ports`
5. Click OK → right-click the SQL Server service → **Restart**

> If Windows Firewall is active, add an inbound rule for TCP port 1433.

### 3. Create the database

Using SSMS or `sqlcmd`:

```sql
CREATE DATABASE GateControl;
GO
```

Make sure the `sa` account is enabled:  
SSMS → Security → Logins → sa → Properties → Status → Login: **Enabled**

> Table structure is created automatically on first GateControl startup (Hibernate DDL auto-update). Only the empty database is needed.

### 4. Install GateControl

Run `GateControl-Setup.exe` as Administrator and follow the setup wizard.

When prompted for connection settings, enter:

```
Server:    localhost,1433
Database:  GateControl
Username:  sa
Password:  [your sa password]
```

### 5. First startup

1. Verify SQL Server service is running (`services.msc → SQL Server → Running`)
2. Launch GateControl from the desktop shortcut
3. Tables are created automatically on first run
4. Log in as administrator with the default password **1234** and change it immediately
5. Register personnel and their RFID / NFC card IDs

---

## Configuration

Copy the example config and fill in your values:

```
copy src\main\resources\db.properties.example src\main\resources\db.properties
```

Edit `db.properties`:

```properties
db.url=jdbc:sqlserver://localhost:1433;databaseName=GateControl;...
db.username=sa
db.password=your_password
```

> `db.properties` is excluded from git — it contains credentials.

---

## Building from Source

```
mvnw clean package -DskipTests
```

Run:
```
java -jar target/gatecontrol-1.0.0.jar
```

Build the Windows installer (requires Inno Setup + jpackage):
```
build-installer.bat
```

Output: `app\GateControl-Setup.exe`

---

## Project Structure

```
src/main/java/gr/military/gatecontrol/
├── auth/           Admin authentication & password crypto
├── config/         Spring context + database config
├── controller/     REST controllers (Person, Movement)
├── entity/         JPA entities (Person, Movement)
├── reader/         RFID keyboard listener, NFC reader
├── repository/     Spring Data JPA repositories
├── service/        Business logic (access control, movements)
└── ui/             JavaFX screens
```

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| App won't start | Check SQL Server service is running (`services.msc`). Check `db.properties` connection settings. |
| RFID reader not detected | Unplug and replug USB. Restart the app with reader connected. |
| Card not recognised | Verify the RFID UID is registered in the personnel record. |
| Camera not shown | Ensure camera is connected and recognised by Windows. |
| Excel export error | Close any open Excel file with the same name and retry. |
| Data loss | Restore from the latest backup file (`.bak`) created via the Backup button. |
