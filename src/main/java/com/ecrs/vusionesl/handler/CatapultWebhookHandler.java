package com.ecrs.vusionesl.handler;

import com.ecrs.vusionesl.config.AppConfig;
import com.ecrs.vusionesl.model.CatapultItem;
import com.ecrs.vusionesl.model.CatapultStoreData;
import com.ecrs.vusionesl.model.VusionItem;
import com.ecrs.vusionesl.service.ItemTransformer;
import com.ecrs.vusionesl.service.VusionApiClient;
import com.ecrs.vusionesl.service.VusionApiClient.VusionApiException;
import com.ecrs.vusionesl.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HTTP handler for receiving item data from Catapult system.
 * Processes incoming POST requests and forwards to Vusion API.
 */
public class CatapultWebhookHandler implements HttpHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(CatapultWebhookHandler.class);
    
    private final AppConfig config;
    private final VusionApiClient vusionClient;
    private final ItemTransformer transformer;
    
    /**
     * Creates a new webhook handler.
     *
     * @param config       Application configuration
     * @param vusionClient Vusion API client
     */
    public CatapultWebhookHandler(AppConfig config, VusionApiClient vusionClient) {
        this.config = config;
        this.vusionClient = vusionClient;
        this.transformer = new ItemTransformer();
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        
        logger.info("Received {} request to {}", method, path);
        
        try {
            if (!"POST".equalsIgnoreCase(method)) {
                sendResponse(exchange, 405, "Method Not Allowed. Use POST.");
                return;
            }
            
            // Read request body
            String requestBody = readRequestBody(exchange);
            logger.debug("Request body length: {} bytes", requestBody.length());
            
            if (requestBody.isEmpty()) {
                sendResponse(exchange, 400, "Empty request body");
                return;
            }
            
            // Parse Catapult items
            List<CatapultItem> catapultItems;
            try {
                catapultItems = JsonUtil.fromJsonList(requestBody, CatapultItem.class);
            } catch (Exception e) {
                logger.error("Failed to parse request JSON: {}", e.getMessage());
                sendResponse(exchange, 400, "Invalid JSON: " + e.getMessage());
                return;
            }
            
            if (catapultItems == null || catapultItems.isEmpty()) {
                sendResponse(exchange, 200, "No items to process");
                return;
            }
            
            logger.info("Received {} items from Catapult", catapultItems.size());
            
            // Process items
            ProcessingResult result = processItems(catapultItems);
            
            // Send response based on result
            if (result.hasErrors()) {
                String errorMessage = String.format(
                    "Processed with errors: %d items updated, %d items deleted, %d errors. First error: %s",
                    result.getItemsUpdated(), result.getItemsDeleted(), result.getErrors().size(),
                    result.getErrors().get(0)
                );
                logger.warn(errorMessage);
                sendResponse(exchange, 500, errorMessage);
            } else {
                String successMessage = String.format(
                    "Success: %d items updated, %d items deleted, %d items skipped (unmapped stores)",
                    result.getItemsUpdated(), result.getItemsDeleted(), result.getItemsSkipped()
                );
                logger.info(successMessage);
                sendResponse(exchange, 200, successMessage);
            }
            
        } catch (Exception e) {
            logger.error("Unexpected error handling request", e);
            sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * Processes a list of Catapult items.
     */
    private ProcessingResult processItems(List<CatapultItem> catapultItems) {
        ProcessingResult result = new ProcessingResult();
        
        // Group items by store for efficient batching
        Map<String, List<VusionItem>> itemsToPost = new HashMap<>();      // storeId -> items
        Map<String, List<String>> itemsToDelete = new HashMap<>();        // storeId -> itemIds
        
        for (CatapultItem item : catapultItems) {
            if (item.getStores() == null || item.getStores().isEmpty()) {
                logger.debug("Skipping item {} - no store data", item.getItemId());
                result.incrementSkipped();
                continue;
            }
            
            for (CatapultStoreData storeData : item.getStores()) {
                String catapultStore = storeData.getStoreNumber();
                
                // Check if this store is configured
                if (!config.isStoreConfigured(catapultStore)) {
                    logger.trace("Skipping store {} for item {} - not configured", 
                        catapultStore, item.getItemId());
                    result.incrementSkipped();
                    continue;
                }
                
                String vusionStoreId = config.getVusionStoreId(catapultStore);
                
                // Check if item should be deleted or updated
                if (storeData.shouldDelete()) {
                    logger.debug("Item {} marked for deletion in store {} (deleted={}, discontinued={})",
                        item.getItemId(), catapultStore, storeData.isDeleted(), storeData.isDiscontinued());
                    
                    itemsToDelete.computeIfAbsent(vusionStoreId, k -> new ArrayList<>())
                                 .add(item.getItemId());
                } else {
                    // Transform and queue for posting
                    try {
                        VusionItem vusionItem = transformer.transform(item, storeData);
                        itemsToPost.computeIfAbsent(vusionStoreId, k -> new ArrayList<>())
                                   .add(vusionItem);
                    } catch (Exception e) {
                        logger.error("Failed to transform item {}: {}", item.getItemId(), e.getMessage());
                        result.addError("Transform failed for " + item.getItemId() + ": " + e.getMessage());
                    }
                }
            }
        }
        
        // Post items to each store
        for (Map.Entry<String, List<VusionItem>> entry : itemsToPost.entrySet()) {
            String storeId = entry.getKey();
            List<VusionItem> items = entry.getValue();
            
            try {
                vusionClient.postItems(storeId, items);
                result.addUpdated(items.size());
                logger.info("Successfully posted {} items to store {}", items.size(), storeId);
            } catch (VusionApiException e) {
                logger.error("Failed to post {} items to store {}: {}", 
                    items.size(), storeId, e.getMessage());
                result.addError("POST failed for store " + storeId + ": " + e.getMessage());
            }
        }
        
        // Delete items from each store
        for (Map.Entry<String, List<String>> entry : itemsToDelete.entrySet()) {
            String storeId = entry.getKey();
            List<String> itemIds = entry.getValue();
            
            try {
                vusionClient.deleteItems(storeId, itemIds);
                result.addDeleted(itemIds.size());
                logger.info("Successfully deleted {} items from store {}", itemIds.size(), storeId);
            } catch (VusionApiException e) {
                logger.error("Failed to delete {} items from store {}: {}", 
                    itemIds.size(), storeId, e.getMessage());
                result.addError("DELETE failed for store " + storeId + ": " + e.getMessage());
            }
        }
        
        return result;
    }
    
    /**
     * Reads the request body as a string.
     */
    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
    
    /**
     * Sends an HTTP response.
     */
    private void sendResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        byte[] responseBytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
        
        logger.debug("Sent response: {} - {}", statusCode, message);
    }
    
    /**
     * Tracks the result of processing Catapult items.
     */
    private static class ProcessingResult {
        private int itemsUpdated = 0;
        private int itemsDeleted = 0;
        private int itemsSkipped = 0;
        private final List<String> errors = new ArrayList<>();
        
        void addUpdated(int count) {
            itemsUpdated += count;
        }
        
        void addDeleted(int count) {
            itemsDeleted += count;
        }
        
        void incrementSkipped() {
            itemsSkipped++;
        }
        
        void addError(String error) {
            errors.add(error);
        }
        
        int getItemsUpdated() {
            return itemsUpdated;
        }
        
        int getItemsDeleted() {
            return itemsDeleted;
        }
        
        int getItemsSkipped() {
            return itemsSkipped;
        }
        
        List<String> getErrors() {
            return errors;
        }
        
        boolean hasErrors() {
            return !errors.isEmpty();
        }
    }
}
