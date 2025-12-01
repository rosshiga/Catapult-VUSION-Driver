package com.ecrs.vusionesl;

import com.ecrs.vusionesl.config.AppConfig;
import com.ecrs.vusionesl.handler.CatapultWebhookHandler;
import com.ecrs.vusionesl.service.VusionApiClient;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Main application entry point.
 * Starts an HTTP server to receive Catapult item data and forward to Vusion API.
 */
public class VusionEslDriverApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(VusionEslDriverApplication.class);
    
    // Default config file path
    private static final String DEFAULT_CONFIG_PATH = "config.properties";
    
    // Endpoint path for Catapult webhook
    private static final String WEBHOOK_PATH = "/catapult";
    
    private final AppConfig config;
    private final VusionApiClient vusionClient;
    private HttpServer server;
    
    /**
     * Creates the application with the specified configuration.
     *
     * @param config Application configuration
     */
    public VusionEslDriverApplication(AppConfig config) {
        this.config = config;
        this.vusionClient = new VusionApiClient(config);
    }
    
    /**
     * Starts the HTTP server.
     *
     * @throws IOException if the server cannot be started
     */
    public void start() throws IOException {
        // Determine bind address based on configuration
        InetSocketAddress address;
        String displayAddress;
        
        if (config.isLocalhostOnly()) {
            // Bind to localhost only (default, secure)
            address = new InetSocketAddress(InetAddress.getLoopbackAddress(), config.getServerPort());
            displayAddress = "localhost";
        } else {
            // Bind to configured address (e.g., 0.0.0.0 for all interfaces)
            address = new InetSocketAddress(config.getServerAddress(), config.getServerPort());
            displayAddress = config.getServerAddress();
        }
        
        server = HttpServer.create(address, 0);
        
        // Register webhook handler
        CatapultWebhookHandler webhookHandler = new CatapultWebhookHandler(config, vusionClient);
        server.createContext(WEBHOOK_PATH, webhookHandler);
        
        // Use a thread pool for handling requests
        server.setExecutor(Executors.newFixedThreadPool(10));
        
        // Start the server
        server.start();
        
        logger.info("=================================================");
        logger.info("  Catapult VUSION ESL Driver started");
        logger.info("  Listening on: http://{}:{}{}", displayAddress, config.getServerPort(), WEBHOOK_PATH);
        logger.info("  Vusion API: {}", config.getVusionBaseUrl());
        logger.info("  Configured stores: {}", config.getStoreMappings().keySet());
        logger.info("=================================================");
    }
    
    /**
     * Stops the HTTP server and releases resources.
     */
    public void stop() {
        if (server != null) {
            logger.info("Stopping server...");
            server.stop(5);  // Wait up to 5 seconds for connections to finish
            server = null;
        }
        
        try {
            vusionClient.close();
        } catch (IOException e) {
            logger.warn("Error closing Vusion client: {}", e.getMessage());
        }
        
        logger.info("Server stopped");
    }
    
    /**
     * Main entry point.
     *
     * @param args Command line arguments. Optional: path to config.properties
     */
    public static void main(String[] args) {
        // Determine config file path
        String configPath = args.length > 0 ? args[0] : DEFAULT_CONFIG_PATH;
        
        logger.info("Starting Catapult VUSION ESL Driver...");
        logger.info("Loading configuration from: {}", configPath);
        
        try {
            // Load configuration
            AppConfig config = new AppConfig(configPath);
            
            // Create and start application
            VusionEslDriverApplication app = new VusionEslDriverApplication(config);
            
            // Add shutdown hook for graceful termination
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutdown signal received");
                app.stop();
            }));
            
            // Start the server
            app.start();
            
            // Keep main thread alive
            logger.info("Press Ctrl+C to stop the server");
            
        } catch (IOException e) {
            logger.error("Failed to start application: {}", e.getMessage(), e);
            System.exit(1);
        } catch (IllegalArgumentException e) {
            logger.error("Configuration error: {}", e.getMessage());
            System.exit(1);
        }
    }
}
