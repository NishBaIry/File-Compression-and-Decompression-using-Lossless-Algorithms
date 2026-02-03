package com.myzip.utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * FileUtils provides common file operations
 */
public class FileUtils {
    
    /**
     * Read entire file into byte array
     */
    public static byte[] readAllBytes(File file) throws IOException {
        return Files.readAllBytes(file.toPath());
    }
    
    /**
     * Write byte array to file
     */
    public static void writeAllBytes(File file, byte[] data) throws IOException {
        Files.createDirectories(file.toPath().getParent());
        Files.write(file.toPath(), data);
    }
    
    /**
     * Get file extension
     */
    public static String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) {
            return "";
        }
        return filename.substring(lastDot + 1).toLowerCase();
    }
    
    /**
     * Get filename without extension
     */
    public static String getNameWithoutExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) {
            return filename;
        }
        return filename.substring(0, lastDot);
    }
    
    /**
     * Recursively get all files in directory
     * Uses Tree data structure (directory traversal)
     */
    public static List<File> getAllFiles(File directory) {
        List<File> fileList = new ArrayList<>();
        getAllFilesRecursive(directory, fileList);
        return fileList;
    }
    
    private static void getAllFilesRecursive(File directory, List<File> fileList) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    fileList.add(file);
                } else if (file.isDirectory()) {
                    getAllFilesRecursive(file, fileList);
                }
            }
        }
    }
    
    /**
     * Get relative path from base directory
     */
    public static String getRelativePath(File base, File file) {
        return base.toPath().relativize(file.toPath()).toString();
    }
    
    /**
     * Format bytes to human readable format
     */
    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    /**
     * Create directory if it doesn't exist
     */
    public static void ensureDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            Files.createDirectories(directory.toPath());
        }
    }
    
    /**
     * Copy input stream to output stream
     */
    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
    }
}
