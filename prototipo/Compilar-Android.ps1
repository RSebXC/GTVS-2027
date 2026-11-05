param([string]$GradleInicial = 'C:\Users\rodri\.gradle\wrapper\dists\gradle-9.6.0-bin\42k10rwplmzkhuboz9kdazi7s\gradle-9.6.0\bin\gradle.bat')
$ErrorActionPreference = 'Stop'
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = 'C:\Users\rodri\AppData\Local\Android\Sdk'
$ProyectoAndroid = Join-Path $PSScriptRoot 'android'
if (-not (Test-Path -LiteralPath (Join-Path $ProyectoAndroid 'gradlew.bat'))) {
    $BootstrapGradle = Join-Path $PSScriptRoot '.gradle-bootstrap'
    New-Item -ItemType Directory -Force -Path $BootstrapGradle | Out-Null
    Set-Content -LiteralPath (Join-Path $BootstrapGradle 'settings.gradle') -Value "rootProject.name = 'wrapper-bootstrap'"
    & $GradleInicial -p $BootstrapGradle wrapper --gradle-version 8.9 --distribution-type bin --no-daemon
    if ($LASTEXITCODE -ne 0) { throw 'No fue posible generar el wrapper.' }
    Copy-Item -LiteralPath (Join-Path $BootstrapGradle 'gradlew'),(Join-Path $BootstrapGradle 'gradlew.bat') -Destination $ProyectoAndroid
    Copy-Item -LiteralPath (Join-Path $BootstrapGradle 'gradle') -Destination $ProyectoAndroid -Recurse
}
Push-Location -LiteralPath $ProyectoAndroid
try {
    & '.\gradlew.bat' assembleDebug --no-daemon
    if ($LASTEXITCODE -ne 0) { throw 'La compilación Android falló. Revisar el registro.' }
} finally { Pop-Location }
