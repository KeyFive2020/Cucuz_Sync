[CmdletBinding()]
param(
    [Parameter(Mandatory, Position = 0)]
    [ValidateSet('forge-1.16.5', 'forge-1.18.2', 'forge-1.19.2', 'forge-1.20.1', 'neoforge-1.21.1', 'neoforge-26.2')]
    [string] $Target
)

$ErrorActionPreference = 'Stop'
$workspaceRoot = Split-Path -Parent $PSScriptRoot
$projectDir = Join-Path $workspaceRoot "versions/$Target"
$gradleCache = Join-Path $workspaceRoot '.gradle-user-home'
$previousGradleHome = $env:GRADLE_USER_HOME
$env:GRADLE_USER_HOME = $gradleCache

try {
    Push-Location $projectDir
    try {
        & .\gradlew.bat runClient --no-daemon
        if ($LASTEXITCODE -ne 0) {
            throw "O cliente de $Target encerrou com codigo $LASTEXITCODE."
        }
    }
    finally {
        Pop-Location
    }
}
finally {
    $env:GRADLE_USER_HOME = $previousGradleHome
}
