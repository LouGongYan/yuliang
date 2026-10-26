#requires -Version 5.1
<#
  Build the "Yuliang" Android APK without Gradle.

  Requirements:
    * JDK 17 (javac, java, keytool)
    * Android SDK platform 35 and build-tools 35.x
      (or let this script download the SDK into .sdk)

  Usage:
    pwsh -File build.ps1 -Jdk "C:\Path\To\jdk-17" -Sdk "C:\Android\Sdk"
    pwsh -File build.ps1 -Jdk "C:\Path\To\jdk-17" -Sdk "C:\Android\Sdk" -SkipTests

  The APK is written to dist\yuliang-<version>.apk.
#>
[CmdletBinding()]
param(
    [string]$Jdk = "",
    [string]$Sdk = "",
    [string]$VersionName = "1.0.0",
    [int]$VersionCode = 1,
    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"
$root = $PSScriptRoot
Set-Location $root

function Fail([string]$message) {
    Write-Host ""
    Write-Host "BUILD FAILED: $message" -ForegroundColor Red
    exit 1
}

function Test-Jdk([string]$path) {
    return $path -and (Test-Path (Join-Path $path "bin\javac.exe"))
}

function Find-Jdk() {
    if (Test-Jdk $Jdk) { return (Resolve-Path $Jdk).Path }
    if (Test-Jdk $env:JAVA_HOME) { return (Resolve-Path $env:JAVA_HOME).Path }
    $cmd = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($cmd) {
        $parent = Split-Path (Split-Path $cmd.Source -Parent) -Parent
        if (Test-Jdk $parent) { return $parent }
    }
    $candidates = @(
        "C:\Program Files\Android\Android Studio\jbr",
        "$env:LOCALAPPDATA\Programs\Android Studio\jbr",
        "C:\Program Files\Java\jdk*",
        "C:\Program Files\Eclipse Adoptium\jdk*",
        "C:\Program Files\Microsoft\jdk*",
        "C:\Program Files\Amazon Corretto\jdk*",
        "$env:USERPROFILE\.jdks\*"
    )
    foreach ($pattern in $candidates) {
        foreach ($dir in @(Get-Item $pattern -ErrorAction SilentlyContinue)) {
            if (Test-Jdk $dir.FullName) { return $dir.FullName }
        }
    }
    return ""
}

function Find-Sdk {
    if ($Sdk -and (Test-Path $Sdk)) { return (Resolve-Path $Sdk).Path }
    if ($env:ANDROID_HOME -and (Test-Path $env:ANDROID_HOME)) { return (Resolve-Path $env:ANDROID_HOME).Path }
    if ($env:ANDROID_SDK_ROOT -and (Test-Path $env:ANDROID_SDK_ROOT)) { return (Resolve-Path $env:ANDROID_SDK_ROOT).Path }
    $local = Join-Path $root ".sdk"
    if (Test-Path $local) { return $local }
    return ""
}

function Download-File([string]$url, [string]$destination) {
    Write-Host "Downloading $url"
    $curl = Get-Command curl.exe -ErrorAction SilentlyContinue
    if ($curl) {
        & $curl.Source -L --fail --retry 3 --max-time 1800 -o $destination $url
        if ($LASTEXITCODE -ne 0) { Fail "download failed: $url" }
    } else {
        Invoke-WebRequest -Uri $url -OutFile $destination -UseBasicParsing
    }
}

function Ensure-Sdk([string]$found) {
    if ($found) { return $found }
    $local = Join-Path $root ".sdk"
    Write-Host "Android SDK not found. Downloading a minimal SDK into $local" -ForegroundColor Yellow
    New-Item -ItemType Directory -Force -Path $local | Out-Null
    $btZip = Join-Path $local "build-tools.zip"
    $pfZip = Join-Path $local "platform.zip"
    if (!(Test-Path $btZip)) {
        Download-File "http://dl.google.com/android/repository/build-tools_r35_windows.zip" $btZip
    }
    if (!(Test-Path $pfZip)) {
        Download-File "http://dl.google.com/android/repository/platform-35_r02.zip" $pfZip
    }
    New-Item -ItemType Directory -Force -Path (Join-Path $local "build-tools"),
        (Join-Path $local "platform") | Out-Null
    Expand-Archive -Path $btZip -DestinationPath (Join-Path $local "build-tools") -Force
    Expand-Archive -Path $pfZip -DestinationPath (Join-Path $local "platform") -Force
    return $local
}

function Find-BuildTools([string]$sdkRoot) {
    $dirs = @(Get-ChildItem (Join-Path $sdkRoot "build-tools") -Directory -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending)
    foreach ($dir in $dirs) {
        if ((Test-Path (Join-Path $dir.FullName "aapt2.exe")) -and
            (Test-Path (Join-Path $dir.FullName "lib\d8.jar"))) {
            return $dir.FullName
        }
    }
    return ""
}

function Find-AndroidJar([string]$sdkRoot) {
    $candidates = @(
        (Join-Path $sdkRoot "platforms\android-35\android.jar"),
        (Join-Path $sdkRoot "platform\android-35\android.jar"),
        (Join-Path $sdkRoot "platforms\android-36\android.jar"),
        (Join-Path $sdkRoot "platform\android-36\android.jar")
    )
    foreach ($file in $candidates) {
        if (Test-Path $file) { return $file }
    }
    return ""
}

function Compile-RepackUtility() {
    $repackDir = Join-Path $root "tools\repack-classes"
    New-Item -ItemType Directory -Force -Path $repackDir | Out-Null
    & $javac -nowarn -encoding UTF-8 -source 17 -target 17 -d $repackDir "tools\RepackApk.java"
    if ($LASTEXITCODE -ne 0) { Fail "repack utility compilation failed" }
    return $repackDir
}

# --------------------------------------------------------------------- tools
$jdkRoot = Find-Jdk
if (!$jdkRoot) {
    Fail "JDK not found. Install JDK 17 (or Android Studio) and pass -Jdk 'C:\path\to\jdk'."
}
$java = Join-Path $jdkRoot "bin\java.exe"
$javac = Join-Path $jdkRoot "bin\javac.exe"
$keytool = Join-Path $jdkRoot "bin\keytool.exe"

$sdkRoot = Ensure-Sdk (Find-Sdk)
$buildTools = Find-BuildTools $sdkRoot
$androidJar = Find-AndroidJar $sdkRoot
if (!$buildTools) { Fail "Android build-tools not found under $sdkRoot\build-tools (need 35.x)." }
if (!$androidJar) { Fail "android.jar not found under $sdkRoot (need platform 35)." }

$aapt2 = Join-Path $buildTools "aapt2.exe"
$zipalign = Join-Path $buildTools "zipalign.exe"
$d8Jar = Join-Path $buildTools "lib\d8.jar"
$apksignerJar = Join-Path $buildTools "lib\apksigner.jar"

Write-Host "JDK:          $jdkRoot"
Write-Host "SDK:          $sdkRoot"
Write-Host "Build tools:  $buildTools"
Write-Host "android.jar:  $androidJar"

# --------------------------------------------------------------------- build
$buildDir = Join-Path $root "app\build"
$genDir = Join-Path $buildDir "gen"
$classesDir = Join-Path $buildDir "classes"
$dexDir = Join-Path $buildDir "dex"
$localBuild = Join-Path $root ".build"
New-Item -ItemType Directory -Force -Path $buildDir, $genDir, $classesDir, $dexDir, $localBuild | Out-Null
Remove-Item (Join-Path $classesDir "*") -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item (Join-Path $dexDir "*") -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item (Join-Path $genDir "*") -Recurse -Force -ErrorAction SilentlyContinue

$localJar = Join-Path $localBuild "android.jar"
Copy-Item $androidJar $localJar -Force

# AAPT2 on Windows cannot open an absolute non-ASCII path in --dir, so all
# arguments below are intentionally relative to the project root.
Write-Host "1/7  Compiling resources..."
& $aapt2 compile --dir "app\src\main\res" -o "app\build\res.zip"
if ($LASTEXITCODE -ne 0) { Fail "aapt2 compile failed" }

Write-Host "2/7  Linking resources..."
& $aapt2 link -o "app\build\app-unsigned.apk" `
    -I ".build\android.jar" `
    --manifest "app\src\main\AndroidManifest.xml" `
    -R "app\build\res.zip" `
    --java "app\build\gen" `
    --auto-add-overlay `
    --min-sdk-version 26 `
    --target-sdk-version 35 `
    --version-code $VersionCode `
    --version-name $VersionName
if ($LASTEXITCODE -ne 0) { Fail "aapt2 link failed" }

Write-Host "3/7  Compiling Java..."
$sources = @()
$sources += Get-ChildItem "app\src\main\java" -Recurse -Filter "*.java" |
    ForEach-Object { $_.FullName.Substring($root.Length + 1) }
$sources += Get-ChildItem "app\build\gen" -Recurse -Filter "*.java" |
    ForEach-Object { $_.FullName.Substring($root.Length + 1) }
$utf8 = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllLines((Join-Path $buildDir "sources.txt"), $sources, $utf8)
& $javac -nowarn -encoding UTF-8 -source 17 -target 17 -classpath ".build\android.jar" `
    -d "app\build\classes" "@app\build\sources.txt"
if ($LASTEXITCODE -ne 0) { Fail "javac failed" }

Write-Host "4/7  Dexing..."
Add-Type -AssemblyName System.IO.Compression.FileSystem
$classesJar = Join-Path $buildDir "classes.jar"
if (Test-Path $classesJar) { Remove-Item $classesJar -Force }
[System.IO.Compression.ZipFile]::CreateFromDirectory(
    (Resolve-Path $classesDir).Path,
    $classesJar,
    [System.IO.Compression.CompressionLevel]::Optimal,
    $false)
& $java -cp $d8Jar com.android.tools.r8.D8 --min-api 26 --lib ".build\android.jar" `
    --output "app\build\dex" "app\build\classes.jar"
if ($LASTEXITCODE -ne 0) { Fail "d8 failed" }

Write-Host "5/7  Packaging..."
$repackDir = Compile-RepackUtility
& $java -cp $repackDir RepackApk `
    "app\build\app-unsigned.apk" `
    "app\build\app-with-dex.apk" `
    "app\build\dex\classes.dex"
if ($LASTEXITCODE -ne 0) { Fail "APK repack failed" }
& $zipalign -f 4 "app\build\app-with-dex.apk" "app\build\app-aligned.apk"
if ($LASTEXITCODE -ne 0) { Fail "zipalign failed" }

Write-Host "6/7  Signing..."
$keystoreDir = Join-Path $root "app\keystore"
$keystore = Join-Path $keystoreDir "yuliang-release.jks"
$storePass = "Yuliang2026!"
New-Item -ItemType Directory -Force -Path $keystoreDir | Out-Null
if (!(Test-Path $keystore)) {
    & $keytool -genkeypair -v -keystore $keystore -storetype PKCS12 -alias yuliang `
        -keyalg RSA -keysize 2048 -validity 12000 `
        -storepass $storePass -keypass $storePass `
        -dname "CN=Yuliang, OU=Personal, O=Yuliang, L=Shanghai, ST=Shanghai, C=CN"
    if ($LASTEXITCODE -ne 0) { Fail "keytool failed" }
}
& $java -jar $apksignerJar sign `
    --ks $keystore --ks-key-alias yuliang `
    --ks-pass "pass:$storePass" --key-pass "pass:$storePass" `
    --out "app\build\yuliang-$VersionName.apk" `
    "app\build\app-aligned.apk"
if ($LASTEXITCODE -ne 0) { Fail "apksigner failed" }
& $java -jar $apksignerJar verify --verbose "app\build\yuliang-$VersionName.apk"
if ($LASTEXITCODE -ne 0) { Fail "APK signature verification failed" }

Write-Host "7/7  Copying deliverable..."
New-Item -ItemType Directory -Force -Path "dist" | Out-Null
Copy-Item "app\build\yuliang-$VersionName.apk" "dist\yuliang-$VersionName.apk" -Force
$cnName = [string]::Concat([char]0x4F59, [char]0x91CF) + "-$VersionName.apk"
Copy-Item "app\build\yuliang-$VersionName.apk" (Join-Path "dist" $cnName) -Force

if (!$SkipTests) {
    Write-Host "Running budget engine checks..."
    $testOut = Join-Path $root "tools\test-classes"
    New-Item -ItemType Directory -Force -Path $testOut | Out-Null
    & $javac -nowarn -encoding UTF-8 -source 17 -target 17 -classpath "app\build\classes" `
        -d "tools\test-classes" "tools\TestEngine.java"
    if ($LASTEXITCODE -ne 0) { Fail "test compilation failed" }
    & $java -classpath "app\build\classes;tools\test-classes" TestEngine
    if ($LASTEXITCODE -ne 0) { Fail "budget engine tests failed" }
}

$size = (Get-Item (Join-Path "dist" $cnName)).Length
Write-Host ""
Write-Host "BUILD OK" -ForegroundColor Green
Write-Host "  dist\$cnName ($([math]::Round($size / 1KB, 1)) KiB)"
Write-Host "  package com.yuliang.ledger  v$VersionName ($VersionCode)"
