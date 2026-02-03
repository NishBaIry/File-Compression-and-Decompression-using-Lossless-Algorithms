#!/usr/bin/env bash
set -e

# Navigate to project root
cd "$(dirname "$0")/.."

echo "Building File Compression System..."
echo ""

# Ensure build directory exists
mkdir -p build/classes

# Compile all Java files with FlatLaf in classpath
echo "Compiling Java files..."
find src/main/java -name "*.java" > sources.txt
javac -encoding UTF-8 -cp "lib/*" -d build/classes @sources.txt
rm sources.txt

# Copy resources (icons, etc.) to build directory
echo "Copying resources..."
if [ -d "src/main/resources" ]; then
    cp -r src/main/resources/* build/classes/
    echo "✓ Resources copied"
fi

echo ""
echo "✓ Build successful!"
echo ""
echo "To run the GUI:  ./scripts/run-gui.sh"
echo ""
