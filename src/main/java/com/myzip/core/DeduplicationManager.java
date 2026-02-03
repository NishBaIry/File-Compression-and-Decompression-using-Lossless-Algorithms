package com.myzip.core;

import com.myzip.hashing.HashUtil;
import com.myzip.utils.FileUtils;

import java.io.File;
import java.util.*;

/**
 * DeduplicationManager handles file deduplication using hash-based approach
 * Uses Hash Table data structure (HashMap)
 */
public class DeduplicationManager {
    
    // Hash table: hash -> file path
    private Map<String, String> hashToFile;
    
    // Hash table: hash -> file data
    private Map<String, byte[]> hashToData;
    
    // Track duplicate files
    private Set<String> duplicateFiles;
    
    public DeduplicationManager() {
        this.hashToFile = new HashMap<>();
        this.hashToData = new HashMap<>();
        this.duplicateFiles = new HashSet<>();
    }
    
    /**
     * Check if file data is duplicate
     * @param data File data
     * @param filePath File path
     * @return Hash of the file
     */
    public String processFile(byte[] data, String filePath) {
        String hash = HashUtil.computeSHA256(data);
        
        if (hashToFile.containsKey(hash)) {
            // Duplicate found
            duplicateFiles.add(filePath);
            return hash;
        } else {
            // New file
            hashToFile.put(hash, filePath);
            hashToData.put(hash, data);
            return hash;
        }
    }
    
    /**
     * Check if file is a duplicate
     */
    public boolean isDuplicate(String hash) {
        return hashToFile.containsKey(hash) && hashToFile.get(hash) != null;
    }
    
    /**
     * Get original file path for a duplicate
     */
    public String getOriginalFile(String hash) {
        return hashToFile.get(hash);
    }
    
    /**
     * Get file data by hash
     */
    public byte[] getDataByHash(String hash) {
        return hashToData.get(hash);
    }
    
    /**
     * Get number of duplicate files found
     */
    public int getDuplicateCount() {
        return duplicateFiles.size();
    }
    
    /**
     * Get all duplicate file paths
     */
    public Set<String> getDuplicateFiles() {
        return new HashSet<>(duplicateFiles);
    }
    
    /**
     * Get all unique hashes
     */
    public Set<String> getAllHashes() {
        return new HashSet<>(hashToFile.keySet());
    }
    
    /**
     * Check if hash exists
     */
    public boolean hasHash(String hash) {
        return hashToFile.containsKey(hash);
    }
    
    /**
     * Store data with hash (for decompression)
     */
    public void storeData(String hash, byte[] data) {
        hashToData.put(hash, data);
    }
    
    /**
     * Clear all data
     */
    public void clear() {
        hashToFile.clear();
        hashToData.clear();
        duplicateFiles.clear();
    }
    
    /**
     * Get statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalHashes", hashToFile.size());
        stats.put("duplicateCount", duplicateFiles.size());
        stats.put("uniqueFiles", hashToFile.size());
        return stats;
    }
}
