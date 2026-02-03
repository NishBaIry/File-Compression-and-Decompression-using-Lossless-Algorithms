package com.myzip.core;

import com.myzip.utils.FileUtils;

import java.io.File;
import java.util.*;

/**
 * FileTypeDetector detects file types based on extension
 * and determines appropriate compression strategy
 */
public class FileTypeDetector {
    
    // File type categories
    public enum FileType {
        TEXT,           // .txt, .csv, .log, .md
        RAW_IMAGE,      // .bmp, .ppm
        COMPRESSED,     // .jpg, .jpeg, .png, .gif, .zip, .mp3, .mp4, .avi
        UNKNOWN
    }
    
    private static final Set<String> TEXT_EXTENSIONS = new HashSet<>(Arrays.asList(
        "txt", "csv", "log", "md", "markdown", "json", "xml", "html", "css", "js", 
        "java", "py", "c", "cpp", "h", "hpp", "go", "rs", "rb", "php", "sh", "bash"
    ));
    
    private static final Set<String> RAW_IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList(
        "bmp", "ppm", "pgm", "pbm"
    ));
    
    private static final Set<String> COMPRESSED_EXTENSIONS = new HashSet<>(Arrays.asList(
        // Images
        "jpg", "jpeg", "png", "gif", "webp", "svg", "ico", "tiff", "tif", "heic", "heif",
        // Archives
        "zip", "rar", "7z", "gz", "tar", "bz2", "xz", "tgz", "tbz", "jar", "war", "ear",
        // Audio
        "mp3", "aac", "ogg", "flac", "wav", "m4a", "wma", "opus", "alac",
        // Video
        "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "mpg", "mpeg", "3gp", "ts",
        // Documents
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp",
        // Executables & Compiled
        "exe", "dll", "so", "dylib", "app", "dmg", "pkg", "deb", "rpm", "apk", "class",
        // Databases
        "db", "sqlite", "sqlite3", "mdb"
    ));
    
    /**
     * Detect file type based on extension
     */
    public static FileType detectFileType(File file) {
        String extension = FileUtils.getExtension(file.getName());
        return detectFileType(extension);
    }
    
    /**
     * Detect file type based on filename
     */
    public static FileType detectFileType(String filename) {
        String extension = FileUtils.getExtension(filename).toLowerCase();
        
        if (TEXT_EXTENSIONS.contains(extension)) {
            return FileType.TEXT;
        } else if (RAW_IMAGE_EXTENSIONS.contains(extension)) {
            return FileType.RAW_IMAGE;
        } else if (COMPRESSED_EXTENSIONS.contains(extension)) {
            return FileType.COMPRESSED;
        } else {
            // Treat unknown files as text for better compression
            return FileType.TEXT;
        }
    }
    
    /**
     * Get recommended compression algorithm for file type
     */
    public static String getCompressionAlgorithm(FileType fileType) {
        switch (fileType) {
            case TEXT:
                return "LZW";
            case RAW_IMAGE:
                return "RLE";
            case COMPRESSED:
            case UNKNOWN:
                return "STORE";
            default:
                return "STORE";
        }
    }
    
    /**
     * Should this file be compressed?
     */
    public static boolean shouldCompress(FileType fileType) {
        return fileType == FileType.TEXT || fileType == FileType.RAW_IMAGE;
    }
    
    /**
     * Get file type as string
     */
    public static String getFileTypeString(FileType fileType) {
        switch (fileType) {
            case TEXT:
                return "Text";
            case RAW_IMAGE:
                return "Raw Image";
            case COMPRESSED:
                return "Pre-compressed";
            case UNKNOWN:
                return "Unknown";
            default:
                return "Unknown";
        }
    }
}
