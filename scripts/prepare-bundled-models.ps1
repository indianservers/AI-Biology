param(
    [string]$SourceDirectory
)

$ErrorActionPreference = "Stop"
$repositoryRoot = Split-Path -Parent $PSScriptRoot

if ([string]::IsNullOrWhiteSpace($SourceDirectory)) {
    $SourceDirectory = Join-Path $repositoryRoot "local-model-cache\biology\3d"
}

$destinationDirectory = Join-Path $repositoryRoot "app\src\main\assets\biology\3d"
$requiredModels = @(
    "Bacteriacell.glb",
    "Cell Membrane.glb",
    "Chloroplast.glb",
    "epithelial microvilli.glb",
    "Lysosome.glb",
    "Mitochondrion.glb",
    "Neuron.glb",
    "plant cell wall.glb",
    "PlantCell.glb",
    "Ribosomes.glb",
    "Rough Endoplasmic Reticulum.glb",
    "Smooth Endoplasmic Reticulum.glb",
    "Vacuole.glb",
    "WhiteBloodCell.glb"
)

$missingModels = $requiredModels | Where-Object {
    -not (Test-Path -LiteralPath (Join-Path $SourceDirectory $_) -PathType Leaf)
}

if ($missingModels.Count -gt 0) {
    throw "Missing source models: $($missingModels -join ', ')"
}

New-Item -ItemType Directory -Path $destinationDirectory -Force | Out-Null

foreach ($modelName in $requiredModels) {
    Copy-Item `
        -LiteralPath (Join-Path $SourceDirectory $modelName) `
        -Destination (Join-Path $destinationDirectory $modelName) `
        -Force
}

$bundledModels = Get-ChildItem -LiteralPath $destinationDirectory -Filter *.glb -File
$totalMegabytes = [math]::Round(
    ($bundledModels | Measure-Object Length -Sum).Sum / 1MB,
    1
)

Write-Host "Bundled $($bundledModels.Count) biology models ($totalMegabytes MB)."
