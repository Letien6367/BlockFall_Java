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
| Pause / resume | P / Escape |
| Restart | R |

Pause, game-over, and menu screens also provide buttons for resume, restart,
returning to the main menu, and exit.

## Tests

Run the dependency-free model, gameplay, persistence, and UI tests with:

```powershell
powershell -ExecutionPolicy Bypass -File .\test.ps1
```

The test runner compiles the production sources and tests with Java 21 language
and API compatibility, then runs the Swing panel tests in headless mode.

## High score

The best score is stored locally at
`%USERPROFILE%\.blockfall\high-score.txt`. The game can still run if the file is
missing or unavailable; file errors are reported in the application process
output.
