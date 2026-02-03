package com.myzip.algorithms.lz77;

import com.myzip.algorithms.Compressor;
import com.myzip.utils.BitOutputStream;

import java.io.ByteArrayOutputStream;

/**
 * LZ77 Compression Algorithm
 * Uses Sliding Window technique with lookback buffer
 * Finds repeated sequences and encodes as (offset, length, next_byte)
 */
public class LZ77Compressor implements Compressor {
    
    private static final int WINDOW_SIZE = 4096;  // Lookback window (sliding window)
    private static final int LOOKAHEAD_SIZE = 18; // Max match length
    private static final int MIN_MATCH = 3;       // Minimum match length
    
    @Override
    public byte[] compress(byte[] data) throws Exception {
        if (data == null || data.length == 0) {
            return new byte[0];
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BitOutputStream bitOut = new BitOutputStream(baos);
        
        // Write original length
        bitOut.writeBits(data.length, 32);
        
        int position = 0;
        
        while (position < data.length) {
            Match match = findLongestMatch(data, position);
            
            if (match.length >= MIN_MATCH && match.offset > 0) {
                // Found a match: write (1, offset, length)
                bitOut.writeBit(1); // Flag: this is a match
                bitOut.writeBits(match.offset, 12);  // 12 bits for offset (0-4095)
                bitOut.writeBits(match.length, 5);   // 5 bits for length (0-31)
                position += match.length;
            } else {
                // No match: write (0, literal byte)
                bitOut.writeBit(0); // Flag: this is a literal
                bitOut.writeByte(data[position] & 0xFF);
                position++;
            }
        }
        
        bitOut.close();
        return baos.toByteArray();
    }
    
    @Override
    public byte[] decompress(byte[] compressedData) throws Exception {
        return new LZ77Decompressor().decompress(compressedData);
    }
    
    /**
     * Find longest match in sliding window
     * Uses sliding window algorithm
     */
    private Match findLongestMatch(byte[] data, int position) {
        int bestOffset = 0;
        int bestLength = 0;
        
        // Calculate window boundaries
        int windowStart = Math.max(0, position - WINDOW_SIZE);
        int lookaheadEnd = Math.min(data.length, position + LOOKAHEAD_SIZE);
        
        // Search in window for matches
        for (int i = windowStart; i < position; i++) {
            int length = 0;
            
            // Count matching bytes
            while (position + length < lookaheadEnd &&
                   data[i + length] == data[position + length]) {
                length++;
            }
            
            // Update best match
            if (length > bestLength) {
                bestLength = length;
                bestOffset = position - i;
            }
        }
        
        return new Match(bestOffset, bestLength);
    }
    
    @Override
    public String getAlgorithmName() {
        return "LZ77";
    }
    
    /**
     * Helper class for match representation
     */
    private static class Match {
        int offset;
        int length;
        
        Match(int offset, int length) {
            this.offset = offset;
            this.length = length;
        }
    }
}
