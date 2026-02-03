package com.myzip.utils;

import java.io.Serializable;
import java.util.*;

/**
 * Metadata class stores information about compressed files
 * Used in archive creation and restoration
 */
public class Metadata implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String relativePath;
    private String originalName;
    private long originalSize;
    private long compressedSize;
    private String compressionAlgorithm;
    private String fileType;
    private String hash;
    private boolean isDuplicate;
    private String duplicateOf;
    private long timestamp;
    
    public Metadata() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public Metadata(String relativePath, String originalName, long originalSize) {
        this();
        this.relativePath = relativePath;
        this.originalName = originalName;
        this.originalSize = originalSize;
    }
    
    // Getters and Setters
    public String getRelativePath() {
        return relativePath;
    }
    
    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }
    
    public String getOriginalName() {
        return originalName;
    }
    
    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }
    
    public long getOriginalSize() {
        return originalSize;
    }
    
    public void setOriginalSize(long originalSize) {
        this.originalSize = originalSize;
    }
    
    public long getCompressedSize() {
        return compressedSize;
    }
    
    public void setCompressedSize(long compressedSize) {
        this.compressedSize = compressedSize;
    }
    
    public String getCompressionAlgorithm() {
        return compressionAlgorithm;
    }
    
    public void setCompressionAlgorithm(String compressionAlgorithm) {
        this.compressionAlgorithm = compressionAlgorithm;
    }
    
    public String getFileType() {
        return fileType;
    }
    
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
    
    public String getHash() {
        return hash;
    }
    
    public void setHash(String hash) {
        this.hash = hash;
    }
    
    public boolean isDuplicate() {
        return isDuplicate;
    }
    
    public void setDuplicate(boolean duplicate) {
        isDuplicate = duplicate;
    }
    
    public String getDuplicateOf() {
        return duplicateOf;
    }
    
    public void setDuplicateOf(String duplicateOf) {
        this.duplicateOf = duplicateOf;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public double getCompressionRatio() {
        if (originalSize == 0) return 0;
        return (1.0 - (double) compressedSize / originalSize) * 100;
    }
    
    @Override
    public String toString() {
        return String.format("Metadata{path='%s', size=%d->%d, algo='%s', type='%s', ratio=%.2f%%}",
                relativePath, originalSize, compressedSize, compressionAlgorithm, fileType, getCompressionRatio());
    }
}
