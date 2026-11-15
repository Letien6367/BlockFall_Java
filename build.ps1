$ErrorActionPreference = 'Stop'

$projectRoot = $PSScriptRoot
$classesDirectory = Join-Path $projectRoot 'build\classes'
$jarPath = Join-Path $projectRoot 'BlockFall.jar'
$manifestPath = Join-Path $projectRoot 'manifest.mf'
$sourceFiles = @(Get-ChildItem -Path (Join-Path $projectRoot 'src') -Recurse -Filter *.java)
$jarCommand = Get-Command jar -ErrorAction SilentlyContinue

if ($null -ne $jarCommand) {
    $jarExecutable = $jarCommand.Source
} else {
    $javaProperties = & $env:ComSpec /c 'java -XshowSettings:properties -version 2>&1'
    $javaHomeLine = $javaProperties | Where-Object { $_ -match '^\s*java\.home\s*=' } |
        Select-Object -First 1
    if ($null -eq $javaHomeLine) {
        throw 'Unable to locate the JDK home directory needed to run jar.exe.'
    }
    $javaHome = ($javaHomeLine -split '=', 2)[1].Trim()
    $jarExecutable = Join-Path $javaHome 'bin\jar.exe'
    if (-not (Test-Path -LiteralPath $jarExecutable)) {
        throw "The JDK archive tool was not found at $jarExecutable."
    }
}

if ($sourceFiles.Count -eq 0) {
    throw 'No Java source files were found under src.'
}

New-Item -ItemType Directory -Force -Path $classesDirectory | Out-Null
& javac --release 21 -d $classesDirectory $sourceFiles.FullName
if ($LASTEXITCODE -ne 0) {
    throw "javac failed with exit code $LASTEXITCODE."
}

& $jarExecutable --create --file $jarPath --manifest $manifestPath -C $classesDirectory .
if ($LASTEXITCODE -ne 0) {
    throw "jar failed with exit code $LASTEXITCODE."
}

Write-Output "Created $jarPath"
