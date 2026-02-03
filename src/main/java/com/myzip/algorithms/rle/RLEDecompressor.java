package com.myzip.algorithms.rle;

import com.myzip.utils.BitInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * RLE Decompression Algorithm
 * Reconstructs original data from RLE compressed format
 */
public class RLEDecompressor {
    
    public byte[] decompress(byte[] compressedData) throws Exception {
        if (compressedData == null || compressedData.length == 0) {
            return new byte[0];
        }
        
        ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
        BitInputStream bitIn = new BitInputStream(bais);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        while (true) {
            int runLength = bitIn.readByte();
            if (runLength == -1) {
                break;
            }
            
            int byteValue = bitIn.readByte();
            if (byteValue == -1) {
                throw new IllegalArgumentException("Invalid RLE format: incomplete run");
            }
            
            // Write the byte value runLength times
            for (int i = 0; i < runLength; i++) {
                baos.write(byteValue);
            }
        }
        
        bitIn.close();
        return baos.toByteArray();
    }
}
