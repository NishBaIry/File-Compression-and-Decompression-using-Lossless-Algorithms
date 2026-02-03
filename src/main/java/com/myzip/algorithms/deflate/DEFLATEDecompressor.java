package com.myzip.algorithms.deflate;

import com.myzip.algorithms.huffman.HuffmanDecompressor;
import com.myzip.algorithms.lz77.LZ77Decompressor;

/**
 * DEFLATE Decompression Algorithm
 * Reverses Huffman → LZ77 compression
 */
public class DEFLATEDecompressor {
    
    public byte[] decompress(byte[] compressedData) throws Exception {
        if (compressedData == null || compressedData.length == 0) {
            return new byte[0];
        }
        
        // Step 1: Huffman decompression
        HuffmanDecompressor huffman = new HuffmanDecompressor();
        byte[] huffmanDecompressed = huffman.decompress(compressedData);
        
        // Step 2: LZ77 decompression
        LZ77Decompressor lz77 = new LZ77Decompressor();
        byte[] finalDecompressed = lz77.decompress(huffmanDecompressed);
        
        return finalDecompressed;
    }
}
