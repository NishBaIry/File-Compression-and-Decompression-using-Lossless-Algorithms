package com.myzip.algorithms.huffman;

import com.myzip.algorithms.Compressor;
import com.myzip.utils.BitOutputStream;

import java.io.ByteArrayOutputStream;
import java.util.*;

/**
 * Huffman Coding Compression Algorithm
 * Uses Priority Queue and Binary Tree data structures
 * Optimal prefix-free codes based on frequency
 */
public class HuffmanCompressor implements Compressor {
    
    @Override
    public byte[] compress(byte[] data) throws Exception {
        if (data == null || data.length == 0) {
            return new byte[0];
        }
        
        // Build frequency table (Hash Table)
        Map<Byte, Integer> frequencyMap = new HashMap<>();
        for (byte b : data) {
            frequencyMap.put(b, frequencyMap.getOrDefault(b, 0) + 1);
        }
        
        // Build Huffman tree using Priority Queue
        HuffmanNode root = buildHuffmanTree(frequencyMap);
        
        // Generate codes (Dictionary/Map)
        Map<Byte, String> codeTable = new HashMap<>();
        generateCodes(root, "", codeTable);
        
        // Compress data
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BitOutputStream bitOut = new BitOutputStream(baos);
        
        // Write header: number of unique bytes
        bitOut.writeBits(frequencyMap.size(), 16);
        
        // Write frequency table
        for (Map.Entry<Byte, Integer> entry : frequencyMap.entrySet()) {
            bitOut.writeByte(entry.getKey() & 0xFF);
            bitOut.writeBits(entry.getValue(), 32);
        }
        
        // Write original data length
        bitOut.writeBits(data.length, 32);
        
        // Write compressed data
        for (byte b : data) {
            String code = codeTable.get(b);
            for (char c : code.toCharArray()) {
                bitOut.writeBit(c == '1' ? 1 : 0);
            }
        }
        
        bitOut.close();
        return baos.toByteArray();
    }
    
    @Override
    public byte[] decompress(byte[] compressedData) throws Exception {
        return new HuffmanDecompressor().decompress(compressedData);
    }
    
    /**
     * Build Huffman tree using Priority Queue
     */
    private HuffmanNode buildHuffmanTree(Map<Byte, Integer> frequencyMap) {
        // Priority Queue (Min Heap) - sorts by frequency
        PriorityQueue<HuffmanNode> pq = new PriorityQueue<>();
        
        // Add all bytes as leaf nodes
        for (Map.Entry<Byte, Integer> entry : frequencyMap.entrySet()) {
            pq.offer(new HuffmanNode(entry.getKey(), entry.getValue()));
        }
        
        // Build tree bottom-up
        while (pq.size() > 1) {
            HuffmanNode left = pq.poll();
            HuffmanNode right = pq.poll();
            HuffmanNode parent = new HuffmanNode(left, right);
            pq.offer(parent);
        }
        
        return pq.poll();
    }
    
    /**
     * Generate Huffman codes by traversing tree
     * Uses recursive tree traversal
     */
    private void generateCodes(HuffmanNode node, String code, Map<Byte, String> codeTable) {
        if (node == null) return;
        
        if (node.isLeaf) {
            codeTable.put(node.value, code.isEmpty() ? "0" : code);
            return;
        }
        
        generateCodes(node.left, code + "0", codeTable);
        generateCodes(node.right, code + "1", codeTable);
    }
    
    @Override
    public String getAlgorithmName() {
        return "HUFFMAN";
    }
}
