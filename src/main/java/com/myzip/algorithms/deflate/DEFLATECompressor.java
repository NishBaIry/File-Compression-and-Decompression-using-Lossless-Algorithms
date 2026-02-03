package com.myzip.algorithms.deflate;

import com.myzip.algorithms.Compressor;
import com.myzip.algorithms.lz77.LZ77Compressor;
import com.myzip.algorithms.huffman.HuffmanCompressor;

/**
 * DEFLATE-like Compression Algorithm
 * Combines LZ77 (sliding window) + Huffman (optimal codes)
 * Similar to ZIP/GZIP compression
 * 
 * Data Structures Used:
 * - Sliding Window (Array/Buffer) - from LZ77
 * - Binary Tree (Huffman Tree) - from Huffman
 * - Priority Queue (Min Heap) - for building Huffman tree
 * - Hash Table/Dictionary - for encoding
 */
public class DEFLATECompressor implements Compressor {
    
    @Override
    public byte[] compress(byte[] data) throws Exception {
        if (data == null || data.length == 0) {
            return new byte[0];
        }
        
        // Step 1: LZ77 compression (find repeated sequences)
        LZ77Compressor lz77 = new LZ77Compressor();
        byte[] lz77Compressed = lz77.compress(data);
        
        // Step 2: Huffman coding (optimal bit encoding)
        HuffmanCompressor huffman = new HuffmanCompressor();
        byte[] finalCompressed = huffman.compress(lz77Compressed);
        
        return finalCompressed;
    }
    
    @Override
    public byte[] decompress(byte[] compressedData) throws Exception {
        return new DEFLATEDecompressor().decompress(compressedData);
    }
    
    @Override
    public String getAlgorithmName() {
        return "DEFLATE";
    }
}
