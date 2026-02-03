package com.myzip.algorithms;

/**
 * Common interface for all compression algorithms
 */
public interface Compressor {
    
    /**
     * Compress the input data
     * @param data Input data to compress
     * @return Compressed data
     */
    byte[] compress(byte[] data) throws Exception;
    
    /**
     * Decompress the compressed data
     * @param compressedData Compressed data
     * @return Original data
     */
    byte[] decompress(byte[] compressedData) throws Exception;
    
    /**
     * Get the name of the compression algorithm
     * @return Algorithm name
     */
    String getAlgorithmName();
}
