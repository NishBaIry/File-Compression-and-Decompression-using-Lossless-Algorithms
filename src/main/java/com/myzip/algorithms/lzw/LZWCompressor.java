package com.myzip.algorithms.lzw;

import com.myzip.algorithms.Compressor;
import java.io.*;
import java.util.*;

public class LZWCompressor implements Compressor {
    @Override
    public byte[] compress(byte[] data) throws IOException {
        if (data == null || data.length == 0) {
            return new byte[0];
        }

        // Initialize dictionary with single-byte entries
        Map<String, Integer> dictionary = new HashMap<>();
        for (int i = 0; i < 256; i++) {
            dictionary.put("" + (char) i, i);
        }
        
        int dictSize = 256;
        String current = "";
        List<Integer> result = new ArrayList<>();
        
        for (byte b : data) {
            String next = current + (char) (b & 0xFF);
            if (dictionary.containsKey(next)) {
                current = next;
            } else {
                result.add(dictionary.get(current));
                if (dictSize < 4096) { // Limit dictionary size
                    dictionary.put(next, dictSize++);
                }
                current = "" + (char) (b & 0xFF);
            }
        }
        
        if (!current.isEmpty()) {
            result.add(dictionary.get(current));
        }
        
        // Write compressed data
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        dos.writeInt(result.size());
        for (int code : result) {
            dos.writeShort(code);
        }
        
        dos.close();
        return baos.toByteArray();
    }
    
    @Override
    public byte[] decompress(byte[] data) throws Exception {
        return new LZWDecompressor().decompress(data);
    }
    
    @Override
    public String getAlgorithmName() {
        return "LZW";
    }
}
