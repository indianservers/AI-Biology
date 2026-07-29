param(
    [string]$PackageName = "com.indianservers.biology",
    [string]$Serial = "",
    [string[]]$Models = @()
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$sourceDir = Join-Path $repoRoot "local-model-cache\biology\3d"
$localProperties = Join-Path $repoRoot "local.properties"

if (-not (Test-Path $sourceDir)) {
    throw "Model cache not found: $sourceDir"
}

$sdkDir = $null
if (Test-Path $localProperties) {
    $sdkLine = Get-Content $localProperties | Where-Object { $_ -match "^sdk\.dir=" } | Select-Object -First 1
    if ($sdkLine) {
        $sdkDir = ($sdkLine -replace "^sdk\.dir=", "") -replace "\\:", ":"
    }
}

$adb = if ($sdkDir) { Join-Path $sdkDir "platform-tools\adb.exe" } else { "adb" }
if (-not (Test-Path $adb)) {
    throw "adb not found. Add Android SDK platform-tools to PATH or check local.properties."
}

$adbArgs = @()
if ($Serial) {
    $adbArgs += @("-s", $Serial)
}

$targetDir = "files/biology/3d"
$tmpDir = "/data/local/tmp/biology-models"

& $adb @adbArgs shell "mkdir -p '$tmpDir'"
if ($LASTEXITCODE -ne 0) {
    throw "Unable to create temp folder on device."
}

& $adb @adbArgs shell "run-as $PackageName mkdir -p '$targetDir'"
if ($LASTEXITCODE -ne 0) {
    throw "Unable to create app model folder. Install/run the debug app once, then rerun this script."
}

$modelFiles = Get-ChildItem $sourceDir -Filter *.glb
if ($Models.Count -gt 0) {
    $wanted = @{}
    $Models | ForEach-Object { $wanted[$_] = $true }
    $modelFiles = $modelFiles | Where-Object { $wanted.ContainsKey($_.Name) }
}

$modelFiles | ForEach-Object {
    Write-Host "Pushing $($_.Name)"
    $tmpFile = "$tmpDir/$($_.Name)"
    & $adb @adbArgs push $_.FullName $tmpFile
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to push $($_.Name)"
    }

    & $adb @adbArgs shell "run-as $PackageName cp '$tmpFile' '$targetDir/$($_.Name)'"
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to copy $($_.Name) into app storage. The device may be out of space."
    }

    & $adb @adbArgs shell "rm '$tmpFile'"
}

Write-Host "Done. Models copied to app-owned storage: $targetDir"
