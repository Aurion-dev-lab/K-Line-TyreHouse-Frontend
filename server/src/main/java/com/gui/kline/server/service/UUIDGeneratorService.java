package com.gui.kline.server.service;

import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for generating unique IDs and API keys.
 */
@Service
public class UUIDGeneratorService {
    
    /**
     * Generate a unique ID
     */
    public String generateId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Generate a unique ID without hyphens
     */
    public String generateIdWithoutHyphens() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * Generate a short unique ID (8 characters)
     */
    public String generateShortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * Generate an API key
     */
    public String generateApiKey() {
        // Generate a 32-character API key
        return UUID.randomUUID().toString().replace("-", "") + 
               UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * Generate a short API key (16 characters)
     */
    public String generateShortApiKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * Generate a numeric ID
     */
    public String generateNumericId() {
        // Use timestamp + random number for numeric ID
        long timestamp = System.currentTimeMillis();
        int random = (int) (Math.random() * 10000);
        return String.valueOf(timestamp) + String.valueOf(random);
    }
    
    /**
     * Generate a prefixed ID
     */
    public String generatePrefixedId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString();
    }
    
    /**
     * Generate an invoice number
     */
    public String generateInvoiceNumber(String prefix) {
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(8);
        return prefix + timestamp;
    }
    
    /**
     * Generate a receipt number
     */
    public String generateReceiptNumber() {
        return "REC-" + System.currentTimeMillis();
    }
}