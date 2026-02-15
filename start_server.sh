#!/bin/bash

# Detect operating system and set Hytale home directory
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" || "$OSTYPE" == "cygwin" ]]; then
    # Windows
    hytale_home="$HOME/AppData/Roaming/Hytale"
elif [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    hytale_home="$HOME/Library/Application Support/Hytale"
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    # Linux
    flatpak_path="$HOME/.var/app/com.hypixel.HytaleLauncher/data/Hytale"
    local_path="$HOME/.local/share/Hytale"

    if [ -d "$flatpak_path" ]; then
        hytale_home="$flatpak_path"
    else
        hytale_home="$local_path"
    fi
else
    # Unsupported OS
    echo "Error: Unsupported OS '$OSTYPE'" >&2
    exit 1
fi

export BUILD_SERVER_HOST=http://localhost:8080
export LOCAL_DEV=true

JAR_FILE="$hytale_home/install/release/package/game/latest/server/HytaleServer.jar"
ASSETS_FILE="$hytale_home/install/release/package/game/latest/Assets.zip"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RUN_DIR="$SCRIPT_DIR/run"
MODS_DIR="$SCRIPT_DIR/build/libs"

echo "$hytale_home/install/release/package/game/latest"
echo "DIR: $RUN_DIR"

mkdir -p "$RUN_DIR"
cd "$RUN_DIR" || exit 1

java -jar "$JAR_FILE" --disable-sentry --mods="$MODS_DIR" --assets="$ASSETS_FILE"
