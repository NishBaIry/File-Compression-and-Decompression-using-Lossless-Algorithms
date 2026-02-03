#!/usr/bin/env bash
set -e

# Navigate to project root
cd "$(dirname "$0")/.."

echo "🧹 Cleaning previous build..."
./scripts/clean.sh

echo ""
echo "🔨 Compiling Java files..."

# Ensure build directory exists
mkdir -p build/classes

# Compile with FlatLaf library in classpath
find src/main/java -name "*.java" > sources.txt
javac -encoding UTF-8 -cp "lib/*" -d build/classes @sources.txt
rm sources.txt

# Copy resources (icons, etc.) to build directory
if [ -d "src/main/resources" ]; then
    cp -r src/main/resources/* build/classes/
fi

echo "✓ Compilation successful!"
echo "🚀 Launching GUI..."
echo ""
java -cp "build/classes:lib/*" com.myzip.gui.CompressionGUI
