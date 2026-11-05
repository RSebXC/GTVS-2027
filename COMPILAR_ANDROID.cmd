@echo off
setlocal
cd /d "%~dp0"
set "PATH=%~dp0..\.tools\node-v26.13.0-win-x64;%PATH%"
if exist "%~dp0..\.tools\java17\jdk-17.0.20.1+1" set "JAVA_HOME=%~dp0..\.tools\java17\jdk-17.0.20.1+1"
if not defined ANDROID_HOME set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
call node scripts/prepare-android.mjs
if errorlevel 1 exit /b 1
call android\gradlew.bat -p android assembleDebug --no-daemon --console=plain
if errorlevel 1 exit /b 1
echo APK: android\app\build\outputs\apk\debug\app-debug.apk
pause
