@echo off
REM Navigate to project root
cd /d "%~dp0\.."

REM Clean previous builds
call scripts\clean.bat

echo.
echo Compiling Java files...

REM Ensure build directory exists
if not exist "build\classes" mkdir "build\classes"

REM Compile all dependencies with FlatLaf in classpath
dir /s /b src\main\java\*.java > sources.txt
javac -encoding UTF-8 -cp "lib/*" -d build/classes @sources.txt
del sources.txt

REM Copy resources (icons, etc.) to build directory
if exist "src\main\resources" (
    xcopy /E /I /Y "src\main\resources\*" "build\classes\" >nul
)

echo Compilation successful!
echo Launching GUI...
echo.
java -cp "build/classes;lib/*" com.myzip.gui.CompressionGUI
