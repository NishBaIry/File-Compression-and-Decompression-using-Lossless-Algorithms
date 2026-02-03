package com.myzip.algorithms.huffman;

/**
 * HuffmanNode represents a node in the Huffman tree
 * Uses Binary Tree data structure
 */
public class HuffmanNode implements Comparable<HuffmanNode> {
    public int frequency;
    public byte value;
    public HuffmanNode left;
    public HuffmanNode right;
    public boolean isLeaf;
    
    public HuffmanNode(byte value, int frequency) {
        this.value = value;
        this.frequency = frequency;
        this.isLeaf = true;
        this.left = null;
        this.right = null;
    }
    
    public HuffmanNode(HuffmanNode left, HuffmanNode right) {
        this.frequency = left.frequency + right.frequency;
        this.left = left;
        this.right = right;
        this.isLeaf = false;
        this.value = 0;
    }
    
    @Override
    public int compareTo(HuffmanNode other) {
        return Integer.compare(this.frequency, other.frequency);
    }
}
