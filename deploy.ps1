#Requires -Version 7
param(
	[string]$Device = $env:ANDROID_SERIAL,
	[switch]$SkipBuild
)
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
if (-not (Test-Path "$root\local.properties")) {
	$sdk = $env:ANDROID_HOME ?? $env:ANDROID_SDK_ROOT ?? $env:ANDROID_SDK
	if (-not $sdk) { throw 'Set ANDROID_HOME to your Android SDK path.' }
	"sdk.dir=$($sdk -replace '\\', '/')" | Set-Content "$root\local.properties"
}
if (-not $SkipBuild) {
	& "$root\gradlew.bat" assemblePhoneDebug --console=plain
	if ($LASTEXITCODE) { exit $LASTEXITCODE }
}
$apk = Get-ChildItem "$root\build\outputs\apk\phone\debug\*.apk" | Sort-Object LastWriteTime | Select-Object -Last 1
if (-not $apk) { throw 'No APK found.' }
$adbArgs = if ($Device) { @('-s', $Device) } else { @() }
Write-Host "Installing $($apk.Name)"
adb @adbArgs install -r -d $apk.FullName
if ($LASTEXITCODE) { exit $LASTEXITCODE }
