param(
    [Parameter(Mandatory = $true)]
    [string]$PlanPath
)

$ErrorActionPreference = 'Stop'
Start-Sleep -Seconds 4

function Get-NormalizedPath([string]$Path) {
    return [System.IO.Path]::GetFullPath($Path)
}

function Assert-ChildPath([string]$Path, [string]$Parent) {
    $normalizedPath = Get-NormalizedPath $Path
    $normalizedParent = (Get-NormalizedPath $Parent).TrimEnd([System.IO.Path]::DirectorySeparatorChar) + [System.IO.Path]::DirectorySeparatorChar
    if (-not $normalizedPath.StartsWith($normalizedParent, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Caminho fora da área permitida: $normalizedPath"
    }
    return $normalizedPath
}

try {
    $normalizedPlan = Get-NormalizedPath $PlanPath
    $plan = Get-Content -LiteralPath $normalizedPlan -Raw -Encoding UTF8 | ConvertFrom-Json
    $gameRoot = Get-NormalizedPath ([string]$plan.gameDirectory)
    $modsRoot = Join-Path $gameRoot 'mods'
    $syncRoot = Join-Path $gameRoot '.cuscuz-sync'
    $stagingRoot = Join-Path $syncRoot 'staging'
    $quarantineRoot = Join-Path $syncRoot 'quarantine'
    $historyRoot = Join-Path $syncRoot 'history'

    foreach ($operation in $plan.operations) {
        $kind = [string]$operation.kind
        if ($kind -eq 'QUARANTINE') {
            $source = Assert-ChildPath ([string]$operation.source) $modsRoot
            $target = Assert-ChildPath ([string]$operation.target) $quarantineRoot
        } elseif ($kind -eq 'INSTALL') {
            $source = Assert-ChildPath ([string]$operation.source) $stagingRoot
            $target = Assert-ChildPath ([string]$operation.target) $modsRoot
        } else {
            throw "Operação desconhecida: $kind"
        }

        $targetDirectory = Split-Path -Parent $target
        New-Item -ItemType Directory -Path $targetDirectory -Force | Out-Null
        if ($kind -eq 'INSTALL' -and (Test-Path -LiteralPath $target)) {
            $backupDirectory = Join-Path $quarantineRoot ('backup-' + [string]$plan.createdAt)
            New-Item -ItemType Directory -Path $backupDirectory -Force | Out-Null
            Move-Item -LiteralPath $target -Destination (Join-Path $backupDirectory ([System.IO.Path]::GetFileName($target))) -Force
        }
        if (Test-Path -LiteralPath $source) {
            Move-Item -LiteralPath $source -Destination $target -Force
        }
    }

    New-Item -ItemType Directory -Path $historyRoot -Force | Out-Null
    $historyPlan = Join-Path $historyRoot ('applied-' + [string]$plan.createdAt + '.json')
    Move-Item -LiteralPath $normalizedPlan -Destination $historyPlan -Force
} catch {
    $failurePath = Join-Path (Split-Path -Parent $PlanPath) 'last-apply-error.txt'
    ($_ | Out-String) | Set-Content -LiteralPath $failurePath -Encoding UTF8
    exit 1
}
