param()
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$pidFile = Join-Path $projectRoot 'data\app.pid'
if (-not (Test-Path $pidFile)) { Write-Host 'No background VelisBank process was recorded.'; exit }
$appProcessId = [int](Get-Content $pidFile -Raw)
$appProcess = Get-CimInstance Win32_Process -Filter "ProcessId = $appProcessId"
$expectedJar = Join-Path $projectRoot 'target\velisbank-1.0.0.jar'
if ($appProcess -and $appProcess.Name -eq 'java.exe' -and $appProcess.CommandLine.Contains($expectedJar)) {
    Stop-Process -Id $appProcessId
    Remove-Item -LiteralPath $pidFile
    Write-Host 'VelisBank stopped. The project database is still running.'
} elseif ($appProcess) { throw 'The recorded process no longer belongs to VelisBank; no process was stopped.' }
else { Write-Host 'VelisBank is not running.' }
