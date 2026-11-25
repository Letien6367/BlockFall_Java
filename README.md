# BlockFall Desktop

A desktop falling-block game built with Java 21, Swing, and AWT. Player
leaderboards are stored locally; the game does not use a database or require an
internet connection.

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

The script compiles every Java source with `--release 21` and creates
`BlockFall.jar`. To choose another output path:

```powershell
powershell -ExecutionPolicy Bypass -File .\build.ps1 -OutputJarPath .\build\BlockFall.jar
```

## Run

```powershell
java -jar .\BlockFall.jar
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

## Player records

Enter a player name on the main menu before starting. Names are trimmed and
repeated whitespace is collapsed; matching is case-sensitive, so `An` and
`an` are separate players. Names can be up to 24 characters. Returning players
reuse their saved player ID, and each player's personal best score is retained.
The menu shows the ten highest scores.

Player records are saved in
`%USERPROFILE%\.blockfall\leaderboard.txt`. The game keeps the existing text
format and also updates `high-score.txt` for the overall best score. Existing
leaderboard files remain in place and continue to be used; no database setup is
needed. Each completed game is also recorded in `game-history.txt`; choose
**Lịch sử ván** in the menu to browse the latest 100 games.

## Combo effect

Each piece lock that clears one or more lines counts as one combo step and
displays an animated `COMBO xN!` banner starting at x1. Consecutive clear
events increase the count; clearing multiple lines with one piece still counts
as one step. The combo resets when a piece locks without clearing a line. This
visual effect does not add bonus points.
