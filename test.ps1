$ErrorActionPreference = 'Stop'

$projectRoot = $PSScriptRoot
& (Join-Path $projectRoot 'build.ps1')
if ($LASTEXITCODE -ne 0) {
    throw "Production build failed with exit code $LASTEXITCODE."
}

$classesDirectory = Join-Path $projectRoot 'build\classes'
$testClassesDirectory = Join-Path $projectRoot 'build\test-classes'
$testFiles = @(Get-ChildItem -Path (Join-Path $projectRoot 'test') -Recurse -Filter *.java)
New-Item -ItemType Directory -Force -Path $testClassesDirectory | Out-Null

& javac --release 21 -cp $classesDirectory -d $testClassesDirectory $testFiles.FullName
if ($LASTEXITCODE -ne 0) {
    throw "Test compilation failed with exit code $LASTEXITCODE."
}

$classPath = $classesDirectory + [IO.Path]::PathSeparator + $testClassesDirectory
$testClasses = @(
    'tetris.model.TetrominoModelTest',
    'tetris.model.BoardMovementTest',
    'tetris.model.TetrominoRotationTest',
    'tetris.model.BoardLineClearTest',
    'tetris.service.ScoreManagerTest',
    'tetris.service.HighScoreManagerTest',
    'tetris.core.GamePanelControlsTest',
    'tetris.core.GamePanelLifecycleTest',
    'tetris.core.GamePanelGameplayTest',
    'tetris.ui.MenuPanelTest'
)

foreach ($testClass in $testClasses) {
    & java '-Djava.awt.headless=true' -cp $classPath $testClass
    if ($LASTEXITCODE -ne 0) {
        throw "Test $testClass failed with exit code $LASTEXITCODE."
    }
}

Write-Output 'All BlockFall tests passed.'
