param(
    [string]$MinecraftDirectory = 'C:\Users\fivesaw\AppData\Roaming\PrismLauncher\instances\1.8.9(1)\minecraft'
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$Repository = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$TelemetryDirectory = Join-Path $MinecraftDirectory 'sawbotv1\telemetry'
$Validator = Join-Path $Repository 'sawbot-tools\dataset-validator\validate_telemetry.py'
$Replay = Join-Path $Repository 'sawbot-tools\replay-inspector\inspect_telemetry.py'

function Invoke-Python {
    param([string[]]$Arguments)

    if (Get-Command py -ErrorAction SilentlyContinue) {
        & py -3 @Arguments
    }
    elseif (Get-Command python -ErrorAction SilentlyContinue) {
        & python @Arguments
    }
    else {
        throw 'Python 3 was not found in PATH.'
    }

    if ($LASTEXITCODE -ne 0) {
        throw "Python command failed with exit code $LASTEXITCODE."
    }
}

if (-not (Test-Path -LiteralPath $TelemetryDirectory -PathType Container)) {
    throw "Telemetry directory does not exist yet: $TelemetryDirectory"
}

$Latest = Get-ChildItem -LiteralPath $TelemetryDirectory -Filter '*.sbt' -File |
    Where-Object { $_.Name -notlike '*.recovered.sbt' } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if (-not $Latest) {
    throw 'No clean .sbt trajectory exists. Start telemetry with K, play briefly, then stop with K.'
}

$TempRoot = Join-Path $env:TEMP ('SawBotV1_TelemetryTest_' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $TempRoot -Force | Out-Null

try {
    Write-Host "Testing: $($Latest.FullName)" -ForegroundColor Cyan

    $ReportPath = Join-Path $TempRoot 'report.json'
    if (Get-Command py -ErrorAction SilentlyContinue) {
        & py -3 $Validator $Latest.FullName --json | Set-Content -LiteralPath $ReportPath -Encoding UTF8
    }
    else {
        & python $Validator $Latest.FullName --json | Set-Content -LiteralPath $ReportPath -Encoding UTF8
    }
    if ($LASTEXITCODE -ne 0) { throw 'Complete-file validation failed.' }

    $Report = Get-Content -LiteralPath $ReportPath -Raw | ConvertFrom-Json
    if (-not $Report.complete) { throw 'Validator did not report COMPLETE.' }
    if ($Report.telemetry_schema -ne 'sawbot.telemetry/0.1') { throw "Unexpected telemetry schema: $($Report.telemetry_schema)" }
    if ($Report.observation_schema -ne 'sawbot.observation/0.3') { throw "Unexpected observation schema: $($Report.observation_schema)" }
    if ([int64]$Report.step_count -lt 1) { throw 'Trajectory contains no steps.' }

    Write-Host "PASS complete trajectory: $($Report.step_count) steps, $($Report.file_size) bytes" -ForegroundColor Green
    Write-Host "Schemas: $($Report.telemetry_schema) / $($Report.observation_schema)"

    $Steps = @($Report.steps)
    $HasKeys = @($Steps | Where-Object { [int]$_.key_or -ne 0 }).Count -gt 0
    $HasMouse = @($Steps | Where-Object { [int]$_.mouse_delta_x -ne 0 -or [int]$_.mouse_delta_y -ne 0 }).Count -gt 0
    $Slots = @($Steps | ForEach-Object { [int]$_.first_selected_slot } | Sort-Object -Unique)
    $HasGui = @($Steps | Where-Object { [int]$_.gui_samples -gt 0 }).Count -gt 0
    Write-Host ("Capture evidence: keys={0} mouse={1} slots={2} gui={3}" -f $HasKeys, $HasMouse, ($Slots -join ','), $HasGui)
    if (-not $HasKeys) { Write-Host 'WARNING: no pressed movement/combat/use keys appeared in this recording.' -ForegroundColor Yellow }
    if (-not $HasMouse) { Write-Host 'WARNING: no mouse movement appeared in this recording.' -ForegroundColor Yellow }
    if ($Slots.Count -lt 2) { Write-Host 'WARNING: only one selected hotbar slot appeared in this recording.' -ForegroundColor Yellow }
    if (-not $HasGui) { Write-Host 'WARNING: no GUI-open tick appeared; open inventory during the next recording to test it.' -ForegroundColor Yellow }

    Write-Host ''
    Write-Host 'Replay sample:' -ForegroundColor Cyan
    Invoke-Python -Arguments @($Replay, $Latest.FullName, '--limit', '20')

    $Bytes = [System.IO.File]::ReadAllBytes($Latest.FullName)
    if ($Bytes.Length -le 64) { throw 'Trajectory is too small for the recovery test.' }
    $Truncated = Join-Path $TempRoot 'interrupted-copy.sbt.partial'
    $TruncatedBytes = New-Object byte[] ($Bytes.Length - 20)
    [Array]::Copy($Bytes, $TruncatedBytes, $TruncatedBytes.Length)
    [System.IO.File]::WriteAllBytes($Truncated, $TruncatedBytes)

    Write-Host ''
    Write-Host 'Testing recovery from a deliberately truncated temporary copy...' -ForegroundColor Cyan
    Invoke-Python -Arguments @($Validator, $Truncated, '--recover', '--summary-only')

    $Recovered = Join-Path $TempRoot 'interrupted-copy.sbt.recovered.sbt'
    if (-not (Test-Path -LiteralPath $Recovered -PathType Leaf)) {
        throw 'Recovery tool did not create the expected recovered file.'
    }
    Invoke-Python -Arguments @($Validator, $Recovered, '--summary-only')

    Write-Host ''
    Write-Host 'PASS: latest telemetry is complete, readable, checksum-valid, replayable, and recoverable.' -ForegroundColor Green
    Write-Host 'The deliberate corruption/recovery test used temporary copies only.'
}
finally {
    Remove-Item -LiteralPath $TempRoot -Recurse -Force -ErrorAction SilentlyContinue
}
