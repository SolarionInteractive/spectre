@echo off
setlocal

set "HYTALE_HOME=%APPDATA%\Hytale"
set "BUILD_SERVER_HOST=http://localhost:8080"
set "LOCAL_DEV=true"

set "SCRIPT_DIR=%~dp0"
if "%SCRIPT_DIR:~-1%"=="\" set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"

set "RUN_DIR=%SCRIPT_DIR%\run"
set "JAR_FILE=%HYTALE_HOME%\install\release\package\game\latest\server\HytaleServer.jar"
set "ASSETS_FILE=%HYTALE_HOME%\install\release\package\game\latest\Assets.zip"
set "MODS_DIR=%SCRIPT_DIR%\build\libs"

echo %HYTALE_HOME%\install\release\package\game\latest
echo DIR: %RUN_DIR%

if not exist "%RUN_DIR%" mkdir "%RUN_DIR%"
cd /d "%RUN_DIR%" || exit /b 1

java -jar "%JAR_FILE%" --disable-sentry --mods="%MODS_DIR%" --assets="%ASSETS_FILE%"
