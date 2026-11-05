param([string]$GradleInicial = 'C:\Users\rodri\.gradle\wrapper\dists\gradle-9.6.0-bin\42k10rwplmzkhuboz9kdazi7s\gradle-9.6.0\bin\gradle.bat')
$ErrorActionPreference = 'Stop'
$JavaLocalGTVS = Join-Path $PSScriptRoot '../.tools/java17/jdk-17.0.20.1+1'
if (Test-Path -LiteralPath $JavaLocalGTVS) { $env:JAVA_HOME = (Resolve-Path -LiteralPath $JavaLocalGTVS).Path }
if (-not $env:JAVA_HOME) { throw 'Configura JAVA_HOME con Java 17. Alternativa: COMPILAR_ANDROID.cmd.' }
$env:ANDROID_HOME = 'C:\Users\rodri\AppData\Local\Android\Sdk'
$AndroidGTVS = Join-Path $PSScriptRoot 'android'
$NodeGTVS = Join-Path $PSScriptRoot '../.tools/node-v26.13.0-win-x64/node.exe'
Push-Location -LiteralPath $PSScriptRoot
try {
    if (Test-Path -LiteralPath $NodeGTVS) { & $NodeGTVS 'scripts/prepare-android.mjs' }
    else { & node 'scripts/prepare-android.mjs' }
    if ($LASTEXITCODE -ne 0) { throw 'No se preparó el mapa Android.' }
    if (-not (Test-Path -LiteralPath (Join-Path $AndroidGTVS 'gradlew.bat'))) {
        $BootstrapGTVS = Join-Path $PSScriptRoot '.gradle-bootstrap'
        New-Item -ItemType Directory -Force -Path $BootstrapGTVS | Out-Null
        Set-Content -LiteralPath (Join-Path $BootstrapGTVS 'settings.gradle') -Value "rootProject.name = 'gtvs-wrapper-bootstrap'"
        & $GradleInicial -p $BootstrapGTVS wrapper --gradle-version 8.9 --distribution-type bin --no-daemon
        if ($LASTEXITCODE -ne 0) { throw 'No fue posible crear el wrapper Gradle.' }
        Copy-Item -LiteralPath (Join-Path $BootstrapGTVS 'gradlew'),(Join-Path $BootstrapGTVS 'gradlew.bat') -Destination $AndroidGTVS
        Copy-Item -LiteralPath (Join-Path $BootstrapGTVS 'gradle') -Destination $AndroidGTVS -Recurse
    }
    Push-Location -LiteralPath $AndroidGTVS
    try { & './gradlew.bat' assembleDebug --no-daemon; if ($LASTEXITCODE -ne 0) { throw 'Compilación Android no completada.' } }
    finally { Pop-Location }
} finally { Pop-Location }
