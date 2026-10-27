#!/bin/sh
# Usage: ./run.sh /path/to/javafx-sdk/lib      (or set FX to the lib folder)
FX="${1:-${FX:-/usr/share/openjfx/lib}}"
mkdir -p out
javac --module-path "$FX" --add-modules javafx.controls -d out $(find src/main -name "*.java") || exit 1
java  --module-path "$FX" --add-modules javafx.controls -cp out com.athish.daq.ui.MainApp
