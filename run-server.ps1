param([int]$Port = 9999)

$ErrorActionPreference = 'Stop'
$compiler = Get-Command javac -ErrorAction Stop
$jdkDirectory = Split-Path -Parent (Split-Path -Parent $compiler.Source)
$serverJar = Join-Path $PSScriptRoot 'auction-server\target\auction-server.jar'
if (-not (Test-Path -LiteralPath $serverJar)) { throw 'Run ./build.ps1 first.' }
Push-Location $PSScriptRoot
try {
    & "$jdkDirectory\bin\java.exe" -jar $serverJar $Port
    if ($LASTEXITCODE -ne 0) { throw "Server exited with code $LASTEXITCODE" }
} finally { Pop-Location }
