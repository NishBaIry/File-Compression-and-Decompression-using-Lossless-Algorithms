package com.myzip.algorithms.huffman;

import com.myzip.utils.BitInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;

/**
 * Huffman Decompression Algorithm
 * Reconstructs data by traversing Huffman tree
 */
public class HuffmanDecompressor {
    
    public byte[] decompress(byte[] compressedData) throws Exception {
        if (compressedData == null || compressedData.length == 0) {
            return new byte[0];
        }
        
        ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
        BitInputStream bitIn = new BitInputStream(bais);
        
        // Read header: number of unique bytes
        int uniqueBytes = bitIn.readBits(16);
        
        // Read frequency table
        Map<Byte, Integer> frequencyMap = new HashMap<>();
        for (int i = 0; i < uniqueBytes; i++) {
            byte value = (byte) bitIn.readByte();
            int frequency = bitIn.readBits(32);
            frequencyMap.put(value, frequency);
        }
        
        // Read original data length
        int originalLength = bitIn.readBits(32);
        
        // Rebuild Huffman tree
        HuffmanNode root = buildHuffmanTree(frequencyMap);
        
        // Decompress data
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        HuffmanNode current = root;
        int decodedBytes = 0;
        
        // Handle single-byte case
        if (root.isLeaf) {
            for (int i = 0; i < originalLength; i++) {
                baos.write(root.value);
            }
            bitIn.close();
            return baos.toByteArray();
        }
        
        while (decodedBytes < originalLength) {
            int bit = bitIn.readBit();
            if (bit == -1) break;
            
            current = (bit == 0) ? current.left : current.right;
            
            if (current.isLeaf) {
                baos.write(current.value);
                decodedBytes++;
                current = root;
            }
        }
        
        bitIn.close();
        return baos.toByteArray();
    }
    
    /**
     * Rebuild Huffman tree from frequency table
     */
    private HuffmanNode buildHuffmanTree(Map<Byte, Integer> frequencyMap) {
        PriorityQueue<HuffmanNode> pq = new PriorityQueue<>();
        
        for (Map.Entry<Byte, Integer> entry : frequencyMap.entrySet()) {
            pq.offer(new HuffmanNode(entry.getKey(), entry.getValue()));
        }
        
        while (pq.size() > 1) {
            HuffmanNode left = pq.poll();
            HuffmanNode right = pq.poll();
            HuffmanNode parent = new HuffmanNode(left, right);
            pq.offer(parent);
        }
        
        return pq.poll();
    }
}
