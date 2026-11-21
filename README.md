# BlockFall Desktop

A desktop falling-block game built with Java 21, Swing, and AWT. Player records
are stored in a local SQL Server Express/LocalDB database; no online service is
used.

## Requirements

- JDK 21 or newer to build and run the game.
- SQL Server Express or LocalDB installed on the same computer.
- A graphical desktop environment for the Swing window.
- VS Code with the Extension Pack for Java is optional.

Confirm Java is available in PowerShell:

```powershell
java -version
javac -version
```

The delivered JAR targets Java 21 bytecode. A compatible Java 21+ runtime must
be installed on any computer that runs it.

## Build

From the project directory, run:

```powershell
powershell -ExecutionPolicy Bypass -File .\build.ps1
```

The script compiles every Java source with `--release 21` and packages the
Microsoft SQL Server JDBC driver into `BlockFall.jar`. If the pinned driver is
not already in the local Maven cache, the build downloads it from Maven Central.
An alternate output path can be supplied for validation:

```powershell
powershell -ExecutionPolicy Bypass -File .\build.ps1 -OutputJarPath .\build\BlockFall.jar
```

## Run

Create the application database once using a Windows-authenticated SQL Server
administrative session:

```powershell
sqlcmd -S .\SQLEXPRESS -E -i .\database\setup.sql
```

The script creates the `BlockFall` database if it does not exist. Create a
dedicated SQL Server login for the game in SQL Server Management Studio (or
another trusted admin tool) named `BlockFallApp`, enable SQL Server
Authentication if needed, then grant the app permissions:

```powershell
sqlcmd -S .\SQLEXPRESS -E -i .\database\grant-app-permissions.sql
```

The application creates its tables and imports the legacy files on first
start.

Configure the connection in the process environment; do not put credentials in
source files or commit them. The default URL targets the local `SQLEXPRESS`
instance. Enter the SQL login and password in the current PowerShell session
without placing the password in shell history:

```powershell
$env:BLOCKFALL_DB_URL = 'jdbc:sqlserver://localhost;instanceName=SQLEXPRESS;databaseName=BlockFall;encrypt=true;trustServerCertificate=true'
$env:BLOCKFALL_DB_USER = Read-Host 'SQL Server application login'
$securePassword = Read-Host 'SQL Server application password' -AsSecureString
$passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
try {
    $env:BLOCKFALL_DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
}
```

To use LocalDB or a different Express instance, set `BLOCKFALL_DB_URL` to that
instance's JDBC URL. Keep these environment variables available in the shell
used to launch the game.

Run the packaged game from the same PowerShell session:

```powershell
java -jar .\BlockFall.jar
```

The runtime does not need internet access. Database connection and persistence
errors are shown in the application and reported to its process output.

## Controls

| Action | Keys |
|---|---|
| Move left / right | Left / A, Right / D |
| Soft drop | Down / S |
| Rotate clockwise | Up / X |
| Rotate counterclockwise | Z |
| Hard drop | Space |
| Hold / swap piece | C (once per piece) |
| Pause / resume | Pause button or P / Escape |
| Restart | R |

The game shows a ghost piece at the active piece's landing position. Use Hold
to store the active piece or swap it with the stored piece; Hold becomes
available again after the current piece locks.
Pieces are generated in shuffled groups of seven, so each group contains every
tetromino type.

Use the Pause button while playing to stop the game without losing progress.
Choose Resume to continue the same game. P / Escape also toggles pause/resume.
Pause, game-over, and menu screens also provide buttons for restart, returning
to the main menu, and exit.

## Player records and game history

Enter a player name on the main menu before starting. Names are trimmed and
repeated whitespace is collapsed; matching is case-sensitive, so `An` and
`an` are separate players. Names can be up to 24 characters. Returning players
reuse their saved player ID, and each player's personal best score is retained.
The menu shows the ten highest scores. At game over, the score, level, lines
cleared, and UTC completion time are saved. Choose **Lịch sử ván** on the menu
to browse the latest 100 games.

Player records and game history are stored in the local SQL Server database
`BlockFall`. On first startup, existing files in `%USERPROFILE%\.blockfall`
(`leaderboard.txt`, `high-score.txt`, and `high-level.txt`) are imported once.
The original files are kept; the database is the source of truth afterward.
The import is transactional and will not be marked complete if a read or
database error prevents it.

The Microsoft SQL Server JDBC Driver is distributed under the MIT License.
The notice is included in `BlockFall.jar` and in
`licenses\Microsoft-SQL-Server-JDBC.txt`; see
[Microsoft's JDBC Driver repository](https://github.com/microsoft/mssql-jdbc).

## Combo effect

Each piece lock that clears one or more lines counts as one combo step and
displays an animated `COMBO xN!` banner starting at x1. Consecutive clear
events increase the count; clearing multiple lines with one piece still counts
as one step. The combo resets when a piece locks without clearing a line. This
visual effect does not add bonus points.
