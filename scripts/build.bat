@echo off
REM Navigate to project root
cd /d "%~dp0\.."

echo Building File Compression System...
echo.

REM Clean previous builds
call scripts\clean.bat

echo.
REM Ensure build directory exists
if not exist "build\classes" mkdir "build\classes"

REM Compile all Java files with FlatLaf in classpath
echo Compiling Java files...
dir /s /b src\main\java\*.java > sources.txt
javac -encoding UTF-8 -cp "lib\*" -d build\classes @sources.txt
del sources.txt

REM Copy resources (icons, etc.) to build directory
echo Copying resources...
if exist "src\main\resources" (
    xcopy /E /I /Y "src\main\resources\*" "build\classes\"
    if errorlevel 1 (
        echo ERROR: Failed to copy resources
        pause
        exit /b 1
    )
    echo ✓ Resources copied successfully
) else (
    echo WARNING: src\main\resources directory not found
)

echo.
echo ✓ Build successful!
echo.
echo To run the GUI:  scripts\run-gui.bat
echo.
pause
