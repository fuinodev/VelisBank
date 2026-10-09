param()
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$pidFile = Join-Path $projectRoot 'data\app.pid'
$expectedJar = Join-Path $projectRoot 'target\velisbank-1.0.0.jar'
# Oracle's java launcher can spawn a second JVM; stop both matching processes.
$runtimeRoot = Join-Path $projectRoot 'data\runtime'
$jarArgument = '(?i)(?:^|\s)-jar\s+"?(?:' + [regex]::Escape($expectedJar) + '|' + [regex]::Escape($runtimeRoot) + '[\\/]velisbank-[0-9]+\.jar)"?(?=\s|$)'
$appProcesses = @(Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" | Where-Object { $_.CommandLine -match $jarArgument })
foreach ($appProcess in $appProcesses) {
    $current = Get-CimInstance Win32_Process -Filter "ProcessId = $($appProcess.ProcessId)"
    if ($current -and $current.Name -eq 'java.exe' -and $current.CommandLine -match $jarArgument) {
        Stop-Process -Id $current.ProcessId
        Wait-Process -Id $current.ProcessId -Timeout 15 -ErrorAction SilentlyContinue
        if (Get-Process -Id $current.ProcessId -ErrorAction SilentlyContinue) { throw 'App did not stop. Rebuild cancelled.' }
    }
}
if (Test-Path -LiteralPath $pidFile) { Remove-Item -LiteralPath $pidFile }
Write-Host 'VelisBank is stopped. The project database is still running.'
