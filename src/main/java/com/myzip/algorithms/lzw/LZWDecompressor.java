package com.myzip.algorithms.lzw;

import java.io.*;
import java.util.*;

public class LZWDecompressor {
    public byte[] decompress(byte[] data) throws IOException {
        if (data == null || data.length == 0) {
            return new byte[0];
        }

        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
        
        int codeCount = dis.readInt();
        List<Integer> codes = new ArrayList<>();
        for (int i = 0; i < codeCount; i++) {
            codes.add((int) dis.readShort());
        }
        
        // Initialize dictionary
        Map<Integer, String> dictionary = new HashMap<>();
        for (int i = 0; i < 256; i++) {
            dictionary.put(i, "" + (char) i);
        }
        
        int dictSize = 256;
        StringBuilder result = new StringBuilder();
        
        String previous = dictionary.get(codes.get(0));
        result.append(previous);
        
        for (int i = 1; i < codes.size(); i++) {
            int code = codes.get(i);
            String entry;
            
            if (dictionary.containsKey(code)) {
                entry = dictionary.get(code);
            } else if (code == dictSize) {
                entry = previous + previous.charAt(0);
            } else {
                throw new IOException("Invalid LZW code: " + code);
            }
            
            result.append(entry);
            
            if (dictSize < 4096) {
                dictionary.put(dictSize++, previous + entry.charAt(0));
            }
            
            previous = entry;
        }
        
        // Convert to bytes
        byte[] output = new byte[result.length()];
        for (int i = 0; i < result.length(); i++) {
            output[i] = (byte) result.charAt(i);
        }
        
        return output;
    }
}
