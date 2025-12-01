package com.ecrs.vusionesl.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Application configuration loaded from config.properties file.
 * Handles server settings, Vusion API configuration, and store mappings.
 */
public class AppConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
    
    // Default values
    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_LISTEN_ADDRESS = "";  // Empty = localhost
    private static final String DEFAULT_VUSION_BASE_URL = "https://api-us.vusion.io/vlink-pro/v1";
    
    // Configuration keys
    private static final String KEY_SERVER_PORT = "server.port";
    private static final String KEY_SERVER_ADDRESS = "server.address";
    private static final String KEY_VUSION_BASE_URL = "vusion.api.baseUrl";
    private static final String KEY_VUSION_SUBSCRIPTION_KEY = "vusion.api.subscriptionKey";
    private static final String STORE_PREFIX = "store.";
    
    private final int serverPort;
    private final String serverAddress;
    private final String vusionBaseUrl;
    private final String vusionSubscriptionKey;
    private final Map<String, String> storeMappings; // Catapult storeNumber -> Vusion storeId
    
    /**
     * Creates configuration from the specified properties file.
     *
     * @param configFilePath Path to the config.properties file
     * @throws IOException if the file cannot be read
     * @throws IllegalArgumentException if required configuration is missing
     */
    public AppConfig(String configFilePath) throws IOException {
        Properties props = new Properties();
        
        try (InputStream input = new FileInputStream(configFilePath)) {
            props.load(input);
            logger.info("Loaded configuration from: {}", configFilePath);
        }
        
        // Parse server settings
        this.serverPort = Integer.parseInt(
            props.getProperty(KEY_SERVER_PORT, String.valueOf(DEFAULT_PORT))
        );
        this.serverAddress = props.getProperty(KEY_SERVER_ADDRESS, DEFAULT_LISTEN_ADDRESS).trim();
        
        // Parse Vusion API settings
        this.vusionBaseUrl = props.getProperty(KEY_VUSION_BASE_URL, DEFAULT_VUSION_BASE_URL);
        this.vusionSubscriptionKey = props.getProperty(KEY_VUSION_SUBSCRIPTION_KEY);
        
        if (vusionSubscriptionKey == null || vusionSubscriptionKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing required configuration: " + KEY_VUSION_SUBSCRIPTION_KEY);
        }
        
        // Parse store mappings (store.RS1=okimoto_corp_us.waianae_store)
        this.storeMappings = new HashMap<>();
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith(STORE_PREFIX)) {
                String catapultStore = key.substring(STORE_PREFIX.length());
                String vusionStore = props.getProperty(key);
                if (vusionStore != null && !vusionStore.trim().isEmpty()) {
                    storeMappings.put(catapultStore, vusionStore.trim());
                    logger.info("Store mapping: {} -> {}", catapultStore, vusionStore.trim());
                }
            }
        }
        
        if (storeMappings.isEmpty()) {
            logger.warn("No store mappings configured. All items will be ignored.");
        }
        
        logger.info("Configuration loaded: address={}, port={}, vusionBaseUrl={}, stores={}",
            serverAddress.isEmpty() ? "localhost" : serverAddress, serverPort, vusionBaseUrl, storeMappings.keySet());
    }
    
    /**
     * Gets the server port to listen on.
     */
    public int getServerPort() {
        return serverPort;
    }
    
    /**
     * Gets the server listen address.
     * Empty string means localhost only (127.0.0.1).
     * Use "0.0.0.0" to listen on all interfaces.
     */
    public String getServerAddress() {
        return serverAddress;
    }
    
    /**
     * Checks if the server should bind to localhost only.
     */
    public boolean isLocalhostOnly() {
        return serverAddress.isEmpty() || "localhost".equalsIgnoreCase(serverAddress) || "127.0.0.1".equals(serverAddress);
    }
    
    /**
     * Gets the Vusion API base URL.
     */
    public String getVusionBaseUrl() {
        return vusionBaseUrl;
    }
    
    /**
     * Gets the Vusion API subscription key (Ocp-Apim-Subscription-Key header).
     */
    public String getVusionSubscriptionKey() {
        return vusionSubscriptionKey;
    }
    
    /**
     * Gets all configured store mappings.
     *
     * @return Map of Catapult store numbers to Vusion store IDs
     */
    public Map<String, String> getStoreMappings() {
        return new HashMap<>(storeMappings);
    }
    
    /**
     * Gets the Vusion store ID for a Catapult store number.
     *
     * @param catapultStoreNumber The Catapult store number (e.g., "RS1")
     * @return The Vusion store ID, or null if not mapped
     */
    public String getVusionStoreId(String catapultStoreNumber) {
        return storeMappings.get(catapultStoreNumber);
    }
    
    /**
     * Checks if a Catapult store is configured for syncing.
     *
     * @param catapultStoreNumber The Catapult store number
     * @return true if the store is mapped, false otherwise
     */
    public boolean isStoreConfigured(String catapultStoreNumber) {
        return storeMappings.containsKey(catapultStoreNumber);
    }
}
