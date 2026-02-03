package com.myzip.utils;

import java.io.*;

/**
 * BitOutputStream allows writing individual bits to an output stream.
 * Essential for compression algorithms like LZW and RLE.
 */
public class BitOutputStream implements AutoCloseable {
    private OutputStream output;
    private int currentByte;
    private int numBitsFilled;
    
    public BitOutputStream(OutputStream out) {
        this.output = out;
        this.currentByte = 0;
        this.numBitsFilled = 0;
    }
    
    /**
     * Write a single bit (0 or 1)
     */
    public void writeBit(int bit) throws IOException {
        if (bit != 0 && bit != 1) {
            throw new IllegalArgumentException("Bit must be 0 or 1");
        }
        
        currentByte = (currentByte << 1) | bit;
        numBitsFilled++;
        
        if (numBitsFilled == 8) {
            output.write(currentByte);
            currentByte = 0;
            numBitsFilled = 0;
        }
    }
    
    /**
     * Write multiple bits from an integer
     */
    public void writeBits(int value, int numBits) throws IOException {
        if (numBits < 0 || numBits > 32) {
            throw new IllegalArgumentException("Number of bits must be between 0 and 32");
        }
        
        for (int i = numBits - 1; i >= 0; i--) {
            int bit = (value >>> i) & 1;
            writeBit(bit);
        }
    }
    
    /**
     * Write a byte (8 bits)
     */
    public void writeByte(int b) throws IOException {
        writeBits(b, 8);
    }
    
    /**
     * Flush remaining bits (pad with zeros)
     */
    public void flush() throws IOException {
        while (numBitsFilled != 0) {
            writeBit(0);
        }
        output.flush();
    }
    
    @Override
    public void close() throws IOException {
        flush();
        output.close();
    }
}
