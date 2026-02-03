package com.myzip.hashing;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * HashUtil provides hashing functionality for file deduplication and integrity verification
 * Uses Hash Table data structure concept
 */
public class HashUtil {
    
    /**
     * Compute SHA-256 hash of byte array
     * Returns hex string representation
     */
    public static String computeSHA256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data);
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    /**
     * Compute MD5 hash of byte array (faster but less secure)
     * Returns hex string representation
     */
    public static String computeMD5(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hashBytes = digest.digest(data);
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }
    
    /**
     * Convert byte array to hexadecimal string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    /**
     * Verify if data matches given hash
     */
    public static boolean verifySHA256(byte[] data, String expectedHash) {
        String actualHash = computeSHA256(data);
        return actualHash.equals(expectedHash);
    }
    
    /**
     * Verify if data matches given MD5 hash
     */
    public static boolean verifyMD5(byte[] data, String expectedHash) {
        String actualHash = computeMD5(data);
        return actualHash.equals(expectedHash);
    }
}
