#!/usr/bin/env bash

# Navigate to project root
cd "$(dirname "$0")/.."

echo "🧹 Cleaning build artifacts..."

# Remove compiled classes from build directory
rm -rf build/classes/*

# Remove any stray .class files in src directory (shouldn't be there!)
echo "Removing any stray .class files..."
find src -name "*.class" -type f -delete 2>/dev/null || true

# Remove Mac-specific files
find . -name ".DS_Store" -type f -delete 2>/dev/null || true
find . -name "._*" -type f -delete 2>/dev/null || true

# Remove temporary upload and archive directories
rm -rf uploads/ 2>/dev/null || true
rm -rf archives/*.myzip 2>/dev/null || true

echo "✓ Clean complete!"
