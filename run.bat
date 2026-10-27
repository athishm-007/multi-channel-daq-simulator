@echo off
cd /d "%~dp0"

rem Usage: run.bat [path_to_javafx_lib]
set FX=%1
if "%FX%"=="" if exist "C:\javafx-sdk-21.0.13\lib" set FX=C:\javafx-sdk-21.0.13\lib
if "%FX%"=="" if exist "C:\javafx-sdk-21\lib" set FX=C:\javafx-sdk-21\lib
if "%FX%"=="" for /d %%D in (C:\javafx-sdk-21*\lib) do if exist "%%D" set FX=%%D

if "%FX%"=="" (
    echo [ERROR] JavaFX SDK lib folder not found.
    echo Please run: run.bat C:\path\to\javafx-sdk\lib
    pause
    exit /b 1
)

echo [INFO] Using JavaFX: %FX%
if not exist out mkdir out

echo [INFO] Compiling classes...
javac --module-path "%FX%" --add-modules javafx.controls -d out src\main\java\com\athish\daq\model\*.java src\main\java\com\athish\daq\core\*.java src\main\java\com\athish\daq\channels\*.java src\main\java\com\athish\daq\persist\*.java src\main\java\com\athish\daq\ui\*.java
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Compilation failed.
    pause
    exit /b 1
)

echo [INFO] Launching Multi-Channel DAQ Simulator...
java --enable-native-access=javafx.graphics --sun-misc-unsafe-memory-access=allow --module-path "%FX%" --add-modules javafx.controls -cp out com.athish.daq.ui.MainApp
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Application exited with an error.
    pause
)
