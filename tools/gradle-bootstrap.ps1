[CmdletBinding()]
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $GradleArguments
)

$ErrorActionPreference = 'Stop'
$GradleVersion = '8.8'
$ExpectedSha256 = 'a4b4158601f8636cdeeab09bd76afb640030bb5b144aafe261a5e8af027dc612'
$Root = Split-Path -Parent $PSScriptRoot
$BootstrapRoot = if ($env:GRADLE_USER_HOME) {
    Join-Path $env:GRADLE_USER_HOME 'sawbot-bootstrap'
} else {
    Join-Path $env:USERPROFILE '.gradle\sawbot-bootstrap'
}
$GradleHome = Join-Path $BootstrapRoot "gradle-$GradleVersion"
$Archive = Join-Path $BootstrapRoot "gradle-$GradleVersion-bin.zip"
$GradleBat = Join-Path $GradleHome 'bin\gradle.bat'
$Url = "https://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip"

if (-not (Test-Path $GradleBat)) {
    New-Item -ItemType Directory -Force -Path $BootstrapRoot | Out-Null
    if (-not (Test-Path $Archive)) {
        Write-Host "Downloading Gradle $GradleVersion..."
        Invoke-WebRequest -UseBasicParsing -Uri $Url -OutFile $Archive
    }

    $ActualSha256 = (Get-FileHash -Algorithm SHA256 -Path $Archive).Hash.ToLowerInvariant()
    if ($ActualSha256 -ne $ExpectedSha256) {
        Remove-Item -Force $Archive -ErrorAction SilentlyContinue
        throw "Gradle distribution checksum mismatch. Expected $ExpectedSha256 but got $ActualSha256."
    }

    if (Test-Path $GradleHome) {
        Remove-Item -Recurse -Force $GradleHome
    }
    Expand-Archive -Path $Archive -DestinationPath $BootstrapRoot -Force
}

& $GradleBat -p $Root @GradleArguments
exit $LASTEXITCODE
