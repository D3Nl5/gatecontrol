# GateControl

Desktop application for military gate access control. Manages personnel entry/exit using RFID cards or NFC-enabled phones, logs all movements, and provides real-time presence monitoring.

Built with Java 21, Spring Boot 3, JavaFX 21. Runs out of the box with a built-in H2 database — no SQL Server installation required unless you need it.

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
- English / Greek interface — toggle in the top bar
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
| Database | Built-in H2 (default) or Microsoft SQL Server 2019+ |
| RFID / NFC device | USB reader |
| Camera | USB Webcam (optional — for personnel photos) |

---

## Installation

### Quick start (H2 — no database setup needed)

Just run the app. The database file (`gatecontrol-data.mv.db`) is created automatically in the same folder as the JAR on first startup.

```
java -jar target/gatecontrol-1.0.0.jar
```

That's it. Log in with the default admin password **1234** and change it immediately.

---

### Using SQL Server instead

If you need SQL Server (shared server, existing infrastructure, etc.), follow the steps below.

#### 1. Install SQL Server

Download **SQL Server Express** (free): https://www.microsoft.com/en-us/sql-server/sql-server-downloads

Run the installer as Administrator → select **Basic** installation.

#### 2. Enable TCP/IP on port 1433

Open **SQL Server Configuration Manager**:

1. Go to: `SQL Server Network Configuration → Protocols for [INSTANCE]`
2. Right-click **TCP/IP** → **Enable**
3. Double-click **TCP/IP** → tab **IP Addresses** → scroll to **IPAll**
4. Set `TCP Port = 1433` — clear any value in `TCP Dynamic Ports`
5. Click OK → right-click the SQL Server service → **Restart**

> If Windows Firewall is active, add an inbound rule for TCP port 1433.

#### 3. Create the database

```sql
CREATE DATABASE GateControl;
GO
```

Make sure the `sa` account is enabled:
SSMS → Security → Logins → sa → Properties → Status → Login: **Enabled**

#### 4. Configure the connection

Copy the example config:

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

#### 5. Start with the mssql profile

```
java -jar target/gatecontrol-1.0.0.jar --spring.profiles.active=mssql
```

---

## Building from Source

```
mvnw clean package -DskipTests
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
| App won't start (H2) | Check that the working directory is writable — H2 creates its data file there. |
| App won't start (SQL Server) | Check SQL Server service is running (`services.msc`). Check `db.properties` connection settings. |
| RFID reader not detected | Unplug and replug USB. Restart the app with reader connected. |
| Card not recognised | Verify the RFID UID is registered in the personnel record. |
| Camera not shown | Ensure camera is connected and recognised by Windows. |
| Excel export error | Close any open Excel file with the same name and retry. |
| Data loss (H2) | Restore from the latest backup `.zip` created via the Backup button. |
| Data loss (SQL Server) | Restore from the latest backup `.bak` created via the Backup button. |
