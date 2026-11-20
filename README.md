# BlockFall Desktop

A desktop falling-block game built with Java 21, Swing, and AWT. No third-party
libraries or network services are required.

## Requirements

- JDK 21 or newer to build and run the game.
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

The script compiles every Java source with `--release 21` into `build\classes`
and creates `BlockFall.jar` using `manifest.mf`.

To build manually:

```powershell
$classes = Join-Path $PWD 'build\classes'
New-Item -ItemType Directory -Force -Path $classes | Out-Null
$sources = Get-ChildItem -Path .\src -Recurse -Filter *.java | ForEach-Object FullName
javac --release 21 -d $classes $sources
jar --create --file .\BlockFall.jar --manifest .\manifest.mf -C $classes .
```

## Run

Run the packaged game from PowerShell:

```powershell
java -jar .\BlockFall.jar
```

To run from VS Code, open `src\Main.java` and choose **Run Java**. The project
can also be compiled and launched directly:

```powershell
$classes = Join-Path $PWD 'build\classes'
New-Item -ItemType Directory -Force -Path $classes | Out-Null
$sources = Get-ChildItem -Path .\src -Recurse -Filter *.java | ForEach-Object FullName
javac --release 21 -d $classes $sources
java -cp $classes Main
```

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

## High score

The best score is stored locally at
`%USERPROFILE%\.blockfall\high-score.txt`. The game can still run if the file is
missing or unavailable; file errors are reported in the application process
output.

## Player leaderboard

Enter a player name on the main menu before starting. Names are trimmed and
repeated whitespace is collapsed; matching is case-sensitive, so `An` and
`an` are separate players. Names can be up to 24 characters. Returning players
reuse their saved player ID, and each player's personal best score is retained.
The menu shows the ten highest scores. Player records are stored locally at
`%USERPROFILE%\.blockfall\leaderboard.txt`.

## Combo effect

Each piece lock that clears one or more lines counts as one combo step and
displays an animated `COMBO xN!` banner starting at x1. Consecutive clear
events increase the count; clearing multiple lines with one piece still counts
as one step. The combo resets when a piece locks without clearing a line. This
visual effect does not add bonus points.
