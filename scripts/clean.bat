@echo off
REM Navigate to project root
cd /d "%~dp0\.."

echo Cleaning build artifacts...

REM Remove compiled classes from build directory
if exist "build\classes" (
    del /s /q "build\classes\*" >nul 2>&1
    for /d %%p in ("build\classes\*") do rmdir "%%p" /s /q >nul 2>&1
)

REM Remove any stray .class files in src directory
echo Removing any stray .class files...
for /r "src" %%f in (*.class) do del "%%f" >nul 2>&1

REM Remove Windows-specific files
for /r %%f in (Thumbs.db desktop.ini) do del "%%f" >nul 2>&1

REM Remove temporary files
if exist "sources.txt" del "sources.txt" >nul 2>&1
for /r %%f in (*.tmp) do del "%%f" >nul 2>&1

REM Remove temporary upload directory
if exist "uploads" rmdir /s /q "uploads" >nul 2>&1

REM Remove archive files (but keep directory)
if exist "archives" (
    del /q "archives\*.myzip" >nul 2>&1
)

echo Clean complete!
