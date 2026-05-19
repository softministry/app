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
$AppImageDir = Join-Path $DistDir $AppName
$MainJar = Join-Path $TargetDir "ministryadmin-web-$AppVersion.jar"
$IconPath = Join-Path $RootDir "src\main\resources\static\img\ministryadmin-icon.ico"
$JPackage = (Get-Command "jpackage" -ErrorAction Stop).Source

function Require-Command {
  Param([string]$Name)
  if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
    throw "Lipseste comanda '$Name'. Instaleaza Maven si un JDK 21+."
  }
}

Require-Command "mvn"

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

$MainJar = Resolve-MainJar

if (-not (Test-Path $MainJar)) {
  Write-Host "Jar-ul nu exista, rulez build Maven..."
  Push-Location $RootDir
  try {
    & mvn -DskipTests package | Out-Host
  }
  finally {
    Pop-Location
  }
  $MainJar = Resolve-MainJar
}

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
Copy-Item $MainJar (Join-Path $PackageInputDir ([System.IO.Path]::GetFileName($MainJar)))

if (Test-Path $DistDir) {
  Remove-Item -Recurse -Force $DistDir
}
New-Item -ItemType Directory -Force -Path $DistDir | Out-Null

Write-Host "Generez app-image self-contained..."
& $JPackage `
  --name $AppName `
  --type app-image `
  --dest $DistDir `
  --input $PackageInputDir `
  --main-jar ([System.IO.Path]::GetFileName($MainJar)) `
  --main-class "org.springframework.boot.loader.launch.JarLauncher" `
  --icon $IconPath `
  --app-version $AppVersion `
  --vendor $AppVendor `
  --description $AppDescription `
  --java-options "-Dspring.profiles.active=desktop" `
  --java-options "-Dfile.encoding=UTF-8"

if (-not (Test-Path $AppImageDir)) {
  throw "App-image-ul nu a fost creat: $AppImageDir"
}

$ZipPath = Join-Path $DistDir "$AppName-portable.zip"
if (Test-Path $ZipPath) {
  Remove-Item -Force $ZipPath
}

Compress-Archive -Path (Join-Path $AppImageDir "*") -DestinationPath $ZipPath -Force

Write-Host ""
Write-Host "Pachet self-contained creat in:"
Write-Host $AppImageDir
Write-Host $ZipPath
