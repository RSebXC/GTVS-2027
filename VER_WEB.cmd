@echo off
setlocal
cd /d "%~dp0"
set "PATH=%~dp0..\.tools\node-v26.13.0-win-x64;%PATH%"
set "VITE_API_URL=https://br-morning-resonance-belnuka7-gtvsapi.compute.c-9.us-east-2.aws.neon.tech/api"
set "PUBLIC_BASE=/"
echo Abre http://127.0.0.1:5173 cuando Vite indique que esta listo.
echo La API y la base de demostracion estan en Neon. Necesitas Internet.
call npm.cmd run dev
pause
