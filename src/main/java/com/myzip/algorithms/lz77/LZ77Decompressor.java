package com.myzip.algorithms.lz77;

import com.myzip.utils.BitInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * LZ77 Decompression Algorithm
 * Reconstructs data from (offset, length, literal) tuples
 */
public class LZ77Decompressor {
    
    public byte[] decompress(byte[] compressedData) throws Exception {
        if (compressedData == null || compressedData.length == 0) {
            return new byte[0];
        }
        
        ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
        BitInputStream bitIn = new BitInputStream(bais);
        
        // Read original length
        int originalLength = bitIn.readBits(32);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        while (baos.size() < originalLength) {
            int flag = bitIn.readBit();
            if (flag == -1) break;
            
            if (flag == 1) {
                // Match: read offset and length
                int offset = bitIn.readBits(12);
                int length = bitIn.readBits(5);
                
                // Skip invalid matches (should never happen with correct compressor)
                if (offset <= 0 || length <= 0) {
                    // This is a bug in the compressed data - skip this match
                    continue;
                }
                
                // Validate offset
                byte[] current = baos.toByteArray();
                if (offset > current.length) {
                    throw new Exception("Invalid LZ77 offset: " + offset + 
                                      " exceeds buffer size: " + current.length);
                }
                
                // Copy from sliding window (byte-by-byte to handle overlapping matches)
                for (int i = 0; i < length; i++) {
                    int pos = current.length - offset;
                    baos.write(current[pos]);
                    current = baos.toByteArray(); // Update for overlapping matches
                }
            } else {
                // Literal: read byte
                int literal = bitIn.readByte();
                if (literal == -1) break;
                baos.write(literal);
            }
        }
        
        bitIn.close();
        return baos.toByteArray();
    }
}
