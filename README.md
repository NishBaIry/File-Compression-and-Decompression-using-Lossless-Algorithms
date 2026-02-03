# MyZip - File Compression & Archiving System

An academic project demonstrating Data Structures & Algorithms concepts through a practical file compression utility implementing multiple compression algorithms.

## 📋 Project Overview

MyZip is a Java-based compression system that implements **five different compression algorithms**, each optimized for specific file types. The system automatically selects the best algorithm based on file characteristics, featuring hash-based deduplication and integrity verification.

---

## 🔬 Compression Algorithms

The system implements five compression algorithms, each using different data structures:

| Algorithm | Data Structure | Used For | Performance |
|-----------|---------------|----------|-------------|
| **LZW** | HashMap (Dictionary) | `.txt`, `.log`, `.csv`, `.json`, `.xml`, `.java`, `.py`, `.c`, `.cpp`, `.html`, `.css`, `.js` | 25-50% compression |
| **RLE** | Array/Byte Stream | `.ppm`, `.bmp`, `.tiff`, `.pbm`, `.pgm` | 10-50% compression |
| **Huffman** | Binary Tree | General files, part of DEFLATE | 20-35% compression |
| **LZ77** | Sliding Window | Binary files, executables, base for DEFLATE | 30-45% compression |
| **DEFLATE** | LZ77 + Huffman | `.zip`, `.jar`, `.tar`, `.gz`, default for unknown | 35-60% compression |

### Algorithm Selection

```
File Extension          → Algorithm
────────────────────────────────────
.txt, .log, .csv       → LZW
.java, .py, .c, .cpp   → LZW
.html, .css, .js       → LZW
.json, .xml, .yaml     → LZW

.ppm, .bmp, .tiff      → RLE
.pbm, .pgm             → RLE

.zip, .jar, .tar, .gz  → DEFLATE
Unknown/Default        → DEFLATE
```

### Additional Features

- **Hash-Based Deduplication**: Uses SHA-256 with HashMap for O(1) duplicate detection. Stores identical files once, saving 30-50% additional space.
- **Integrity Verification**: SHA-256 checksums verify data integrity during decompression.

---

## 🖥️ User Interface

The application features a desktop GUI built using:
- **Java Swing** for UI components
- **FlatLaf** for modern look and feel
- WinZip/WinRAR style interface with drag-and-drop support

---

## 🚀 Quick Start

### Prerequisites

- **Java Development Kit (JDK) 8 or higher**
- Command line interface (Terminal/Command Prompt)

### Linux / macOS

```bash
# Clone the repository
git clone <your-repo-url>
cd dsa-el

# Make scripts executable
chmod +x scripts/*.sh

# Run the GUI
./scripts/run-gui.sh
```

### Windows

```cmd
# Clone the repository
git clone <your-repo-url>
cd dsa-el

# Run the GUI
scripts\run-gui.bat
```

The script will automatically clean, compile, and launch the GUI.

---

## 📖 Usage

1. Launch GUI using the script above
2. **Compress**: Click "Compress" or drag files → Select output location → System auto-selects algorithm per file
3. **Extract**: Click "Extract" or drag `.myzip` archive → Choose destination → Auto-verifies integrity

---

## 📊 Performance Comparison

| Algorithm | Text Files | Source Code | Images | Binary | Speed    |
|-----------|-----------|-------------|---------|---------|----------|
| LZW       | 25-40%    | 30-50%      | 10-20%  | 15-25%  | Fast     |
| RLE       | 5-15%     | 5-10%       | 10-50%  | 5-15%   | Very Fast|
| Huffman   | 20-35%    | 25-40%      | 15-30%  | 20-35%  | Fast     |
| LZ77      | 30-45%    | 35-50%      | 20-35%  | 25-40%  | Medium   |
| DEFLATE   | 35-60%    | 40-65%      | 25-45%  | 30-50%  | Medium   |

*Values show typical compression ratios (space saved)*

---

## 🎓 Academic Context

**Data Structures**: Hash Tables (deduplication), Binary Trees (Huffman), Sliding Window (LZ77), Dictionaries (LZW)

**Algorithms**: Greedy (Huffman tree), Dynamic Programming (LZ77 matching), String Matching, Hashing (SHA-256)

**Complexity Analysis**: Time/space tradeoffs, algorithm selection optimization

---

## 🔧 Manual Build (Alternative)

### Linux / macOS

```bash
mkdir -p build/classes
find src/main/java -name "*.java" > sources.txt
javac -encoding UTF-8 -cp "lib/*" -d build/classes @sources.txt
rm sources.txt
java -cp "build/classes:lib/*" com.myzip.gui.CompressionGUI
```

### Windows

```cmd
mkdir build\classes
dir /s /b src\main\java\*.java > sources.txt
javac -encoding UTF-8 -cp "lib/*" -d build/classes @sources.txt
del sources.txt
java -cp "build/classes;lib/*" com.myzip.gui.CompressionGUI
```

---

## 🏗️ Project Structure

```
dsa-el/
├── src/main/java/com/myzip/
│   ├── algorithms/         # LZW, RLE, Huffman, LZ77, DEFLATE
│   ├── core/               # ArchiveManager, DeduplicationManager, FileTypeDetector
│   ├── hashing/            # SHA-256 utilities
│   ├── utils/              # Bit I/O streams
│   └── gui/                # Swing UI
├── scripts/                # run-gui.sh, run-gui.bat, build.sh, clean.sh
├── lib/                    # flatlaf-3.4.1.jar
└── docs/                   # Additional documentation
```

---

## 🐛 Troubleshooting

- **`javac: command not found`**: Install JDK 8+ and add to PATH
- **Compilation errors**: Run clean script first (`./scripts/clean.sh` or `scripts\clean.bat`)
- **GUI doesn't launch**: Ensure desktop environment available (not SSH without X11)

---

## 📚 Additional Documentation

See `docs/` directory for detailed algorithm analysis and technical documentation.

---

## 📄 License

Created for academic purposes. Free to use for educational and non-commercial purposes.

---

**Note**: This is an academic demonstration. For production use, prefer `java.util.zip` or Apache Commons Compress.

**Created as part of a Data Structures & Algorithms course project.**
