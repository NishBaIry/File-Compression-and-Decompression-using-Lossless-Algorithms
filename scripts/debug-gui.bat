@echo off
REM Navigate to project root
cd /d "%~dp0\.."

REM Clean and build first
call scripts\build.bat

echo.
echo ===== DIAGNOSTIC INFORMATION =====
echo.

REM Check if resources were copied
echo Checking for icons in build directory...
if exist "build\classes\icons\app-icon.png" (
    echo ✓ app-icon.png found
) else (
    echo ✗ app-icon.png NOT FOUND
)

if exist "build\classes\icons\compress.png" (
    echo ✓ compress.png found
) else (
    echo ✗ compress.png NOT FOUND
)

if exist "build\classes\icons\extract.png" (
    echo ✓ extract.png found
) else (
    echo ✗ extract.png NOT FOUND
)

echo.
echo Current directory: %cd%
echo Classpath will be: build\classes;lib\*
echo.

REM List what's actually in the icons directory
echo Contents of build\classes\icons\:
dir /b "build\classes\icons\" 2>nul
if errorlevel 1 (
    echo ERROR: Icons directory does not exist!
    pause
    exit /b 1
)

echo.
echo Launching GUI with verbose output...
echo (Watch for "Icon not found" messages)
echo.

REM Run with same classpath but don't suppress stderr
java -cp "build\classes;lib\*" com.myzip.gui.CompressionGUI

echo.
pause
