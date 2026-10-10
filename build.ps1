param([switch]$SkipTests)

$ErrorActionPreference = 'Stop'
$compiler = Get-Command javac -ErrorAction Stop
$jdkDirectory = Split-Path -Parent (Split-Path -Parent $compiler.Source)
$previousJavaHome = $env:JAVA_HOME
$previousPath = $env:Path
Push-Location $PSScriptRoot
try {
    $env:JAVA_HOME = $jdkDirectory
    $env:Path = "$jdkDirectory\bin;$previousPath"
    $mavenArguments = @('-B', 'install')
    if ($SkipTests) { $mavenArguments += '-DskipTests' }
    & mvn @mavenArguments
    if ($LASTEXITCODE -ne 0) { throw "Maven build failed (exit $LASTEXITCODE)" }
} finally {
    $env:JAVA_HOME = $previousJavaHome
    $env:Path = $previousPath
    Pop-Location
}
