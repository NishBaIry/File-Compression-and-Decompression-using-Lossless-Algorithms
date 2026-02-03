package com.myzip.core;

import com.myzip.algorithms.Compressor;
import com.myzip.algorithms.lzw.LZWCompressor;
import com.myzip.algorithms.lzw.LZWDecompressor;
import com.myzip.algorithms.rle.RLECompressor;
import com.myzip.algorithms.rle.RLEDecompressor;
import com.myzip.utils.Metadata;
import com.myzip.hashing.HashUtil;

import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * ArchiveManager handles creation and extraction of .myzip archives
 * Manages compression, metadata, and file storage
 */
public class ArchiveManager {
    
    private DeduplicationManager deduplicationManager;
    
    public ArchiveManager() {
        this.deduplicationManager = new DeduplicationManager();
    }
    
    /**
     * Create archive from files
     * @param files List of files to archive
     * @param outputFile Output archive file
     * @return Compression statistics
     */
    public CompressionResult createArchive(List<FileEntry> files, File outputFile) throws Exception {
        List<Metadata> metadataList = new ArrayList<>();
        Map<String, byte[]> compressedData = new HashMap<>();
        
        long totalOriginalSize = 0;
        long totalCompressedSize = 0;
        int duplicateCount = 0;
        
        // Process each file
        for (FileEntry entry : files) {
            byte[] originalData = entry.getData();
            totalOriginalSize += originalData.length;
            
            Metadata metadata = new Metadata();
            metadata.setRelativePath(entry.getRelativePath());
            metadata.setOriginalName(entry.getFileName());
            metadata.setOriginalSize(originalData.length);
            
            // Compute hash for deduplication
            String hash = deduplicationManager.processFile(originalData, entry.getRelativePath());
            metadata.setHash(hash);
            
            // Check if duplicate
            if (deduplicationManager.isDuplicate(hash) && 
                !deduplicationManager.getOriginalFile(hash).equals(entry.getRelativePath())) {
                metadata.setDuplicate(true);
                metadata.setDuplicateOf(deduplicationManager.getOriginalFile(hash));
                metadata.setCompressionAlgorithm("DUPLICATE");
                metadata.setCompressedSize(0);
                duplicateCount++;
            } else {
                // Detect file type
                FileTypeDetector.FileType fileType = FileTypeDetector.detectFileType(entry.getFileName());
                metadata.setFileType(FileTypeDetector.getFileTypeString(fileType));
                
                // Compress or store
                byte[] processedData;
                String algorithm = FileTypeDetector.getCompressionAlgorithm(fileType);
                metadata.setCompressionAlgorithm(algorithm);
                
                if (FileTypeDetector.shouldCompress(fileType)) {
                    processedData = compressData(originalData, fileType);
                } else {
                    processedData = originalData;
                }
                
                metadata.setCompressedSize(processedData.length);
                totalCompressedSize += processedData.length;
                
                // Store compressed data
                compressedData.put(hash, processedData);
            }
            
            metadataList.add(metadata);
        }
        
        // Write archive
        writeArchive(outputFile, metadataList, compressedData);
        
        // Create result
        CompressionResult result = new CompressionResult();
        result.setMetadataList(metadataList);
        result.setOriginalSize(totalOriginalSize);
        result.setCompressedSize(totalCompressedSize);
        result.setDuplicateCount(duplicateCount);
        
        return result;
    }
    
    /**
     * Compress data using appropriate algorithm
     */
    private byte[] compressData(byte[] data, FileTypeDetector.FileType fileType) throws Exception {
        Compressor compressor = null;
        
        switch (fileType) {
            case TEXT:
                compressor = new LZWCompressor();
                break;
            case RAW_IMAGE:
                compressor = new RLECompressor();
                break;
            default:
                return data;
        }
        
        return compressor.compress(data);
    }
    
    /**
     * Write archive to file
     */
    private void writeArchive(File outputFile, List<Metadata> metadataList, Map<String, byte[]> compressedData) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputFile))) {
            // Write metadata
            ZipEntry metadataEntry = new ZipEntry("metadata.dat");
            zos.putNextEntry(metadataEntry);
            ObjectOutputStream oos = new ObjectOutputStream(zos);
            oos.writeObject(metadataList);
            oos.flush();
            zos.closeEntry();
            
            // Write compressed data
            for (Map.Entry<String, byte[]> entry : compressedData.entrySet()) {
                ZipEntry dataEntry = new ZipEntry("data/" + entry.getKey());
                zos.putNextEntry(dataEntry);
                zos.write(entry.getValue());
                zos.closeEntry();
            }
        }
    }
    
    /**
     * Extract archive
     */
    public DecompressionResult extractArchive(File archiveFile) throws Exception {
        List<Metadata> metadataList = new ArrayList<>();
        Map<String, byte[]> compressedData = new HashMap<>();
        
        // Read archive
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(archiveFile))) {
            ZipEntry entry;
            
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals("metadata.dat")) {
                    ObjectInputStream ois = new ObjectInputStream(zis);
                    metadataList = (List<Metadata>) ois.readObject();
                } else if (entry.getName().startsWith("data/")) {
                    String hash = entry.getName().substring(5);
                    byte[] data = readAllBytes(zis);
                    compressedData.put(hash, data);
                }
            }
        }
        
        // Decompress files
        List<RestoredFile> restoredFiles = new ArrayList<>();
        Map<String, byte[]> decompressedCache = new HashMap<>();
        
        for (Metadata metadata : metadataList) {
            RestoredFile restoredFile = new RestoredFile();
            restoredFile.setPath(metadata.getRelativePath());
            restoredFile.setHash(metadata.getHash());
            
            byte[] decompressedData;
            
            // Check if we already decompressed this hash
            if (decompressedCache.containsKey(metadata.getHash())) {
                decompressedData = decompressedCache.get(metadata.getHash());
            } else {
                // Decompress for the first time
                byte[] compData = compressedData.get(metadata.getHash());
                if (compData != null) {
                    String algorithm = metadata.isDuplicate() ? 
                        getOriginalAlgorithm(metadataList, metadata.getHash()) : 
                        metadata.getCompressionAlgorithm();
                    decompressedData = decompressFile(compData, algorithm);
                    decompressedCache.put(metadata.getHash(), decompressedData);
                } else {
                    throw new IllegalStateException("No compressed data found for hash: " + metadata.getHash());
                }
            }
            
            // Verify integrity
            String computedHash = HashUtil.computeSHA256(decompressedData);
            boolean verified = computedHash.equals(metadata.getHash());
            
            restoredFile.setData(decompressedData);
            restoredFile.setSize(decompressedData.length);
            restoredFile.setVerified(verified);
            
            restoredFiles.add(restoredFile);
        }
        
        DecompressionResult result = new DecompressionResult();
        result.setRestoredFiles(restoredFiles);
        
        return result;
    }
    
    /**
     * Get the algorithm used for the original file (for duplicates)
     */
    private String getOriginalAlgorithm(List<Metadata> metadataList, String hash) {
        for (Metadata m : metadataList) {
            if (m.getHash().equals(hash) && !m.isDuplicate()) {
                return m.getCompressionAlgorithm();
            }
        }
        return "STORE";
    }
    
    /**
     * Decompress file data
     */
    private byte[] decompressFile(byte[] data, String algorithm) throws Exception {
        switch (algorithm) {
            case "LZW":
                return new LZWDecompressor().decompress(data);
            case "RLE":
                return new RLEDecompressor().decompress(data);
            case "STORE":
            case "DUPLICATE":
            default:
                return data;
        }
    }
    
    /**
     * Read all bytes from input stream
     */
    private byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        return baos.toByteArray();
    }
    
    // Helper classes
    public static class FileEntry {
        private String relativePath;
        private String fileName;
        private byte[] data;
        
        public FileEntry(String relativePath, String fileName, byte[] data) {
            this.relativePath = relativePath;
            this.fileName = fileName;
            this.data = data;
        }
        
        public String getRelativePath() { return relativePath; }
        public String getFileName() { return fileName; }
        public byte[] getData() { return data; }
    }
    
    public static class CompressionResult {
        private List<Metadata> metadataList;
        private long originalSize;
        private long compressedSize;
        private int duplicateCount;
        
        public List<Metadata> getMetadataList() { return metadataList; }
        public void setMetadataList(List<Metadata> metadataList) { this.metadataList = metadataList; }
        
        public long getOriginalSize() { return originalSize; }
        public void setOriginalSize(long originalSize) { this.originalSize = originalSize; }
        
        public long getCompressedSize() { return compressedSize; }
        public void setCompressedSize(long compressedSize) { this.compressedSize = compressedSize; }
        
        public int getDuplicateCount() { return duplicateCount; }
        public void setDuplicateCount(int duplicateCount) { this.duplicateCount = duplicateCount; }
        
        public double getCompressionRatio() {
            if (originalSize == 0) return 0;
            return (1.0 - (double) compressedSize / originalSize) * 100;
        }
    }
    
    public static class RestoredFile {
        private String path;
        private byte[] data;
        private long size;
        private String hash;
        private boolean verified;
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public byte[] getData() { return data; }
        public void setData(byte[] data) { this.data = data; }
        
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        
        public String getHash() { return hash; }
        public void setHash(String hash) { this.hash = hash; }
        
        public boolean isVerified() { return verified; }
        public void setVerified(boolean verified) { this.verified = verified; }
    }
    
    public static class DecompressionResult {
        private List<RestoredFile> restoredFiles;
        
        public List<RestoredFile> getRestoredFiles() { return restoredFiles; }
        public void setRestoredFiles(List<RestoredFile> restoredFiles) { this.restoredFiles = restoredFiles; }
    }
}
