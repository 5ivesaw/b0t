[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

function Read-JavaMajor([string] $JavaExecutable) {
    $output = & $JavaExecutable -version 2>&1 | Out-String
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to execute $JavaExecutable"
    }
    if ($output -match 'version "1\.(\d+)') {
        return [int] $Matches[1]
    }
    if ($output -match 'version "(\d+)') {
        return [int] $Matches[1]
    }
    throw "Could not parse Java version from: $output"
}

Write-Host 'SawBotV1 local build preflight'

$GradleJava = (Get-Command java -ErrorAction Stop).Source
$GradleMajor = Read-JavaMajor $GradleJava
if ($GradleMajor -lt 17) {
    throw "The Java on PATH must be JDK 17 or newer for Gradle. Found Java $GradleMajor at $GradleJava."
}
Write-Host "PASS Gradle JVM: Java $GradleMajor ($GradleJava)"

if (-not $env:JAVA8_HOME) {
    throw 'Set JAVA8_HOME to a JDK 8 directory before building.'
}
$Java8 = Join-Path $env:JAVA8_HOME 'bin\java.exe'
if (-not (Test-Path $Java8)) {
    throw "JAVA8_HOME does not contain bin\java.exe: $env:JAVA8_HOME"
}
$Java8Major = Read-JavaMajor $Java8
if ($Java8Major -ne 8) {
    throw "JAVA8_HOME must point to JDK 8. Found Java $Java8Major."
}
Write-Host "PASS Minecraft toolchain: Java 8 ($Java8)"

$GradleUserHome = if ($env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME } else { Join-Path $env:USERPROFILE '.gradle' }
New-Item -ItemType Directory -Force -Path $GradleUserHome | Out-Null
$UserProperties = Join-Path $GradleUserHome 'gradle.properties'
$NormalizedPath = $env:JAVA8_HOME -replace '\\', '/'
$Property = "org.gradle.java.installations.paths=$NormalizedPath"

$Existing = if (Test-Path $UserProperties) { Get-Content $UserProperties } else { @() }
$Filtered = $Existing | Where-Object { $_ -notmatch '^org\.gradle\.java\.installations\.paths=' }
@($Filtered) + $Property | Set-Content -Encoding UTF8 $UserProperties
Write-Host "PASS Registered JDK 8 in $UserProperties"

Write-Host 'Next commands:'
Write-Host '  .\gradlew.bat phase0Info'
Write-Host '  .\gradlew.bat clean ciBuild'
Write-Host '  .\gradlew.bat runClient'
