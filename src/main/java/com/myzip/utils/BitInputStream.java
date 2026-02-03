package com.myzip.utils;

import java.io.*;

/**
 * BitInputStream allows reading individual bits from an input stream.
 * Essential for decompression algorithms like LZW and RLE.
 */
public class BitInputStream implements AutoCloseable {
    private InputStream input;
    private int currentByte;
    private int numBitsRemaining;
    
    public BitInputStream(InputStream in) {
        this.input = in;
        this.currentByte = 0;
        this.numBitsRemaining = 0;
    }
    
    /**
     * Read a single bit (0 or 1)
     */
    public int readBit() throws IOException {
        if (numBitsRemaining == 0) {
            currentByte = input.read();
            if (currentByte == -1) {
                return -1;
            }
            numBitsRemaining = 8;
        }
        numBitsRemaining--;
        return (currentByte >>> numBitsRemaining) & 1;
    }
    
    /**
     * Read multiple bits as an integer
     */
    public int readBits(int numBits) throws IOException {
        if (numBits < 0 || numBits > 32) {
            throw new IllegalArgumentException("Number of bits must be between 0 and 32");
        }
        
        int result = 0;
        for (int i = 0; i < numBits; i++) {
            int bit = readBit();
            if (bit == -1) {
                return -1;
            }
            result = (result << 1) | bit;
        }
        return result;
    }
    
    /**
     * Read a byte (8 bits)
     */
    public int readByte() throws IOException {
        return readBits(8);
    }
    
    @Override
    public void close() throws IOException {
        input.close();
    }
}
