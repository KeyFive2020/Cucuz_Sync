[CmdletBinding()]
param(
    [string] $ModId = 'cuscuz_multiversion',
    [string] $ModName = 'Cuscuz Multiversion Mod',
    [string] $ModVersion = '0.1.0',
    [string] $GroupId = 'br.com.cuscuz.multiversion',
    [string] $Authors = 'Equipe Cuscuz',
    [string] $Description = 'Base multi-versao do Cuscuz para as principais versoes de Minecraft com mods.'
)

$ErrorActionPreference = 'Stop'
if ($ModId -notmatch '^[a-z][a-z0-9_]{1,63}$') {
    throw 'ModId invalido. Use letras minusculas, numeros e sublinhado; comece por uma letra.'
}

$workspaceRoot = Split-Path -Parent $PSScriptRoot
$propertyFiles = Get-ChildItem (Join-Path $workspaceRoot 'versions') -Filter 'gradle.properties' -Recurse
$values = [ordered]@{
    mod_id = $ModId
    mod_name = $ModName
    mod_version = $ModVersion
    mod_group_id = $GroupId
    mod_authors = $Authors
    mod_description = $Description
}

foreach ($file in $propertyFiles) {
    $content = Get-Content -Raw $file.FullName
    foreach ($entry in $values.GetEnumerator()) {
        $escapedValue = [string]$entry.Value -replace "`r|`n", ' '
        $pattern = "(?m)^$([regex]::Escape($entry.Key))=.*$"
        if ($content -match $pattern) {
            $content = [regex]::Replace($content, $pattern, "$($entry.Key)=$escapedValue")
        }
        else {
            $content += "`r`n$($entry.Key)=$escapedValue"
        }
    }
    Set-Content -Path $file.FullName -Value $content -Encoding utf8NoBOM
}

Write-Host "Metadados atualizados em $($propertyFiles.Count) projetos." -ForegroundColor Green

