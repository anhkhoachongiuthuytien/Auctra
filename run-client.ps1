param([string]$ServerHost = 'localhost', [int]$Port = 9999, [switch]$Local)

$ErrorActionPreference = 'Stop'
$compiler = Get-Command javac -ErrorAction Stop
$jdkDirectory = Split-Path -Parent (Split-Path -Parent $compiler.Source)
$previousJavaHome = $env:JAVA_HOME
$previousPath = $env:Path
Push-Location $PSScriptRoot
try {
    $env:JAVA_HOME = $jdkDirectory
    $env:Path = "$jdkDirectory\bin;$previousPath"
    $mavenArguments = @('-pl', 'auction-client', 'javafx:run')
    if (-not $Local) { $mavenArguments += "-Djavafx.args=--socket $ServerHost $Port" }
    & mvn @mavenArguments
    if ($LASTEXITCODE -ne 0) { throw "Client exited with code $LASTEXITCODE" }
} finally {
    $env:JAVA_HOME = $previousJavaHome
    $env:Path = $previousPath
    Pop-Location
}
