Param(
  [string]$AppName = "Church Administration Platform",
  [string]$AppVersion = "1.0.0",
  [string]$AppVendor = "Church Administration Platform",
  [string]$AppDescription = "Church Administration Platform desktop app"
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Split-Path -Parent $ScriptDir
$TargetDir = Join-Path $RootDir "target"
$DistDir = Join-Path $RootDir "packaging\dist"
$PackageInputDir = Join-Path $RootDir "package-input"
$MainJar = Join-Path $TargetDir "ministryadmin-web-$AppVersion.jar"
$PackageJar = $null
$IconPath = Join-Path $RootDir "src\main\resources\static\img\ministryadmin-icon.ico"

function Require-Command {
  Param([string]$Name)
  if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
    throw "Lipseste comanda '$Name'. Instaleaza JDK 21+ (cu jpackage) si Maven."
  }
}

Require-Command "mvn"
Require-Command "jpackage"
if (-not (Get-Command "candle.exe" -ErrorAction SilentlyContinue) -or -not (Get-Command "light.exe" -ErrorAction SilentlyContinue)) {
  throw "Nu gasesc WiX Toolset in PATH (candle.exe si light.exe). Instaleaza WiX Toolset 3.x si adauga folderul bin in PATH, apoi ruleaza din nou scriptul."
}

function Resolve-MainJar {
  if (Test-Path $MainJar) {
    return $MainJar
  }

  $jar = Get-ChildItem -Path $TargetDir -Filter "ministryadmin-web-*.jar" -File -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notlike "*.original" } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

  if ($null -ne $jar) {
    return $jar.FullName
  }

  return $MainJar
}

Write-Host "Rulez build Maven pentru a include ultimele modificari..."
Push-Location $RootDir
try {
  & mvn -DskipTests package | Out-Host
}
finally {
  Pop-Location
}

$MainJar = Resolve-MainJar
if (-not (Test-Path $MainJar)) {
  throw "Jar-ul principal nu exista: $MainJar"
}
if (-not (Test-Path $IconPath)) {
  throw "Iconita Windows nu exista: $IconPath"
}

if (Test-Path $PackageInputDir) {
  Remove-Item -Recurse -Force $PackageInputDir
}
New-Item -ItemType Directory -Force -Path $PackageInputDir | Out-Null
$PackageJar = Join-Path $PackageInputDir ([System.IO.Path]::GetFileName($MainJar))
Copy-Item $MainJar $PackageJar
New-Item -ItemType Directory -Force -Path $DistDir | Out-Null

Write-Host "Generez installer EXE..."
& jpackage `
  --name $AppName `
  --type exe `
  --dest $DistDir `
  --input $PackageInputDir `
  --main-jar ([System.IO.Path]::GetFileName($MainJar)) `
  --main-class "org.springframework.boot.loader.launch.JarLauncher" `
  --icon $IconPath `
  --app-version $AppVersion `
  --vendor $AppVendor `
  --description $AppDescription `
  --win-menu `
  --win-shortcut `
  --java-options "-Dserver.port=8080" `
  --java-options "-Dfile.encoding=UTF-8"

Write-Host ""
Write-Host "EXE generat in: $DistDir"
Get-ChildItem -Path $DistDir -Filter "*.exe" | Select-Object FullName
