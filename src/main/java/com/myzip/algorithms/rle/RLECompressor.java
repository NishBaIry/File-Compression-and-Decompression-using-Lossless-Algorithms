package com.myzip.algorithms.rle;

import com.myzip.algorithms.Compressor;
import com.myzip.utils.BitOutputStream;

import java.io.ByteArrayOutputStream;

/**
 * RLE (Run-Length Encoding) Compression Algorithm
 * Uses Array data structure
 * Best for raw images (BMP, PPM) with repeated pixel values
 */
public class RLECompressor implements Compressor {
    
    private static final int MAX_RUN_LENGTH = 255;
    
    @Override
    public byte[] compress(byte[] data) throws Exception {
        if (data == null || data.length == 0) {
            return new byte[0];
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BitOutputStream bitOut = new BitOutputStream(baos);
        
        int i = 0;
        while (i < data.length) {
            byte currentByte = data[i];
            int runLength = 1;
            
            // Count consecutive identical bytes
            while (i + runLength < data.length && 
                   data[i + runLength] == currentByte && 
                   runLength < MAX_RUN_LENGTH) {
                runLength++;
            }
            
            // Write run length and byte value
            bitOut.writeByte(runLength);
            bitOut.writeByte(currentByte & 0xFF);
            
            i += runLength;
        }
        
        bitOut.close();
        return baos.toByteArray();
    }
    
    @Override
    public byte[] decompress(byte[] compressedData) throws Exception {
        // Decompression is handled by RLEDecompressor
        return new RLEDecompressor().decompress(compressedData);
    }
    
    @Override
    public String getAlgorithmName() {
        return "RLE";
    }
}
