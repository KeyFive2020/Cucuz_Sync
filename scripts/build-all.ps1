[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$workspaceRoot = Split-Path -Parent $PSScriptRoot
$gradleCache = Join-Path $workspaceRoot '.gradle-user-home'
$distributionDir = Join-Path $workspaceRoot 'dist'
$targets = @(
    'forge-1.16.5',
    'forge-1.18.2',
    'forge-1.19.2',
    'forge-1.20.1',
    'neoforge-1.21.1',
    'neoforge-26.2'
)

New-Item -ItemType Directory -Force -Path $gradleCache, $distributionDir | Out-Null
$previousGradleHome = $env:GRADLE_USER_HOME
$env:GRADLE_USER_HOME = $gradleCache

try {
    foreach ($target in $targets) {
        $projectDir = Join-Path $workspaceRoot "versions/$target"
        Write-Host "`n==> Compilando $target" -ForegroundColor Cyan

        Push-Location $projectDir
        try {
            & .\gradlew.bat build --no-daemon
            if ($LASTEXITCODE -ne 0) {
                throw "A compilacao de $target falhou com codigo $LASTEXITCODE."
            }
        }
        finally {
            Pop-Location
        }

        Get-ChildItem (Join-Path $projectDir 'build/libs') -Filter '*.jar' |
            Where-Object { $_.Name -notmatch '(sources|javadoc|dev|slim)' } |
            Copy-Item -Destination $distributionDir -Force
    }
}
finally {
    $env:GRADLE_USER_HOME = $previousGradleHome
}

Write-Host "`nCompilacao concluida. Artefatos em: $distributionDir" -ForegroundColor Green
