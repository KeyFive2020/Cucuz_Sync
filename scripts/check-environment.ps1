[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$workspaceRoot = Split-Path -Parent $PSScriptRoot
$targets = @(
    @{ Name = 'forge-1.16.5'; Java = 8 },
    @{ Name = 'forge-1.18.2'; Java = 17 },
    @{ Name = 'forge-1.19.2'; Java = 17 },
    @{ Name = 'forge-1.20.1'; Java = 17 },
    @{ Name = 'neoforge-1.21.1'; Java = 21 },
    @{ Name = 'neoforge-26.2'; Java = 25 }
)

Write-Host 'Java que inicia o Gradle:' -ForegroundColor Cyan
& java -version
if ($LASTEXITCODE -ne 0) {
    throw 'Java nao foi encontrado no PATH.'
}

Write-Host "`nProjetos:" -ForegroundColor Cyan
foreach ($target in $targets) {
    $wrapper = Join-Path $workspaceRoot "versions/$($target.Name)/gradlew.bat"
    [pscustomobject]@{
        Target = $target.Name
        JavaToolchain = $target.Java
        Wrapper = if (Test-Path $wrapper) { 'OK' } else { 'AUSENTE' }
    }
}
