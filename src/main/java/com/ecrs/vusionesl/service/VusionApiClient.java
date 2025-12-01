package com.ecrs.vusionesl.service;

import com.ecrs.vusionesl.config.AppConfig;
import com.ecrs.vusionesl.model.VusionItem;
import com.ecrs.vusionesl.util.JsonUtil;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * HTTP client for interacting with the Vusion ESL API.
 * Handles item POST/DELETE operations with batching and retry logic.
 */
public class VusionApiClient implements Closeable {
    
    private static final Logger logger = LoggerFactory.getLogger(VusionApiClient.class);
    
    // Batch limits
    private static final int MAX_ITEMS_PER_BATCH = 999;
    private static final int MAX_BYTES_PER_BATCH = 10 * 1024 * 1024;  // 10MB
    
    // Retry configuration
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;  // 1 second
    private static final double BACKOFF_MULTIPLIER = 2.0;
    
    // HTTP timeouts
    private static final int CONNECT_TIMEOUT_SECONDS = 30;
    private static final int RESPONSE_TIMEOUT_SECONDS = 60;
    
    // API headers
    private static final String HEADER_SUBSCRIPTION_KEY = "Ocp-Apim-Subscription-Key";
    private static final String HEADER_CACHE_CONTROL = "Cache-Control";
    
    private final String baseUrl;
    private final String subscriptionKey;
    private final CloseableHttpClient httpClient;
    
    /**
     * Creates a new Vusion API client.
     *
     * @param config Application configuration
     */
    public VusionApiClient(AppConfig config) {
        this.baseUrl = config.getVusionBaseUrl();
        this.subscriptionKey = config.getVusionSubscriptionKey();
        
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(Timeout.of(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS))
            .setResponseTimeout(Timeout.of(RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS))
            .build();
        
        this.httpClient = HttpClients.custom()
            .setDefaultRequestConfig(requestConfig)
            .build();
        
        logger.info("VusionApiClient initialized: baseUrl={}", baseUrl);
    }
    
    /**
     * Posts items to a Vusion store, automatically batching if necessary.
     *
     * @param vusionStoreId The Vusion store ID
     * @param items         The items to post
     * @return true if all items were posted successfully
     * @throws VusionApiException if any batch fails after retries
     */
    public boolean postItems(String vusionStoreId, List<VusionItem> items) throws VusionApiException {
        if (items == null || items.isEmpty()) {
            logger.debug("No items to post to store {}", vusionStoreId);
            return true;
        }
        
        logger.info("Posting {} items to store {}", items.size(), vusionStoreId);
        
        // Split into batches
        List<List<VusionItem>> batches = createBatches(items);
        logger.debug("Split into {} batches", batches.size());
        
        // Post each batch
        for (int i = 0; i < batches.size(); i++) {
            List<VusionItem> batch = batches.get(i);
            logger.info("Posting batch {}/{} ({} items) to store {}", 
                i + 1, batches.size(), batch.size(), vusionStoreId);
            
            postBatchWithRetry(vusionStoreId, batch);
        }
        
        logger.info("Successfully posted all {} items to store {}", items.size(), vusionStoreId);
        return true;
    }
    
    /**
     * Deletes items from a Vusion store.
     *
     * @param vusionStoreId The Vusion store ID
     * @param itemIds       The item IDs (UPCs) to delete
     * @return true if all items were deleted successfully
     * @throws VusionApiException if deletion fails after retries
     */
    public boolean deleteItems(String vusionStoreId, List<String> itemIds) throws VusionApiException {
        if (itemIds == null || itemIds.isEmpty()) {
            logger.debug("No items to delete from store {}", vusionStoreId);
            return true;
        }
        
        logger.info("Deleting {} items from store {}", itemIds.size(), vusionStoreId);
        
        // Split into batches if needed
        List<List<String>> batches = createIdBatches(itemIds);
        
        for (int i = 0; i < batches.size(); i++) {
            List<String> batch = batches.get(i);
            logger.info("Deleting batch {}/{} ({} items) from store {}", 
                i + 1, batches.size(), batch.size(), vusionStoreId);
            
            deleteBatchWithRetry(vusionStoreId, batch);
        }
        
        logger.info("Successfully deleted all {} items from store {}", itemIds.size(), vusionStoreId);
        return true;
    }
    
    /**
     * Creates batches of items respecting size limits.
     */
    private List<List<VusionItem>> createBatches(List<VusionItem> items) {
        List<List<VusionItem>> batches = new ArrayList<>();
        List<VusionItem> currentBatch = new ArrayList<>();
        int currentBatchSize = 2;  // Account for JSON array brackets "[]"
        
        for (VusionItem item : items) {
            String itemJson = JsonUtil.toJson(item);
            int itemSize = JsonUtil.getJsonSizeBytes(itemJson);
            
            // Check if adding this item would exceed limits
            boolean wouldExceedItemLimit = currentBatch.size() >= MAX_ITEMS_PER_BATCH;
            boolean wouldExceedSizeLimit = (currentBatchSize + itemSize + 1) > MAX_BYTES_PER_BATCH;  // +1 for comma
            
            if (!currentBatch.isEmpty() && (wouldExceedItemLimit || wouldExceedSizeLimit)) {
                // Start a new batch
                batches.add(currentBatch);
                currentBatch = new ArrayList<>();
                currentBatchSize = 2;
            }
            
            currentBatch.add(item);
            currentBatchSize += itemSize + (currentBatch.size() > 1 ? 1 : 0);  // +1 for comma between items
        }
        
        if (!currentBatch.isEmpty()) {
            batches.add(currentBatch);
        }
        
        return batches;
    }
    
    /**
     * Creates batches of item IDs for deletion.
     */
    private List<List<String>> createIdBatches(List<String> itemIds) {
        List<List<String>> batches = new ArrayList<>();
        
        for (int i = 0; i < itemIds.size(); i += MAX_ITEMS_PER_BATCH) {
            int end = Math.min(i + MAX_ITEMS_PER_BATCH, itemIds.size());
            batches.add(new ArrayList<>(itemIds.subList(i, end)));
        }
        
        return batches;
    }
    
    /**
     * Posts a batch of items with retry logic.
     */
    private void postBatchWithRetry(String vusionStoreId, List<VusionItem> batch) throws VusionApiException {
        String url = baseUrl + "/stores/" + vusionStoreId + "/items";
        String jsonBody = JsonUtil.toJson(batch);
        
        logger.debug("POST {} - {} bytes", url, JsonUtil.getJsonSizeBytes(jsonBody));
        
        VusionApiException lastException = null;
        
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                int statusCode = executePost(url, jsonBody);
                
                if (statusCode >= 200 && statusCode < 300) {
                    logger.debug("POST successful (attempt {}): status={}", attempt, statusCode);
                    return;
                } else {
                    lastException = new VusionApiException("POST failed with status " + statusCode, statusCode);
                    logger.warn("POST failed (attempt {}): status={}", attempt, statusCode);
                }
            } catch (IOException e) {
                lastException = new VusionApiException("POST failed: " + e.getMessage(), e);
                logger.warn("POST failed (attempt {}): {}", attempt, e.getMessage());
            }
            
            // Wait before retry (exponential backoff)
            if (attempt < MAX_RETRIES) {
                long backoffMs = (long) (INITIAL_BACKOFF_MS * Math.pow(BACKOFF_MULTIPLIER, attempt - 1));
                logger.debug("Waiting {}ms before retry", backoffMs);
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new VusionApiException("Interrupted during retry backoff", e);
                }
            }
        }
        
        // All retries exhausted
        logger.error("POST failed after {} retries to {}", MAX_RETRIES, url);
        throw lastException;
    }
    
    /**
     * Deletes a batch of items with retry logic.
     */
    private void deleteBatchWithRetry(String vusionStoreId, List<String> itemIds) throws VusionApiException {
        String url = baseUrl + "/stores/" + vusionStoreId + "/items";
        String jsonBody = JsonUtil.toJson(itemIds);
        
        logger.debug("DELETE {} - {} items", url, itemIds.size());
        
        VusionApiException lastException = null;
        
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                int statusCode = executeDelete(url, jsonBody);
                
                if (statusCode >= 200 && statusCode < 300) {
                    logger.debug("DELETE successful (attempt {}): status={}", attempt, statusCode);
                    return;
                } else {
                    lastException = new VusionApiException("DELETE failed with status " + statusCode, statusCode);
                    logger.warn("DELETE failed (attempt {}): status={}", attempt, statusCode);
                }
            } catch (IOException e) {
                lastException = new VusionApiException("DELETE failed: " + e.getMessage(), e);
                logger.warn("DELETE failed (attempt {}): {}", attempt, e.getMessage());
            }
            
            // Wait before retry (exponential backoff)
            if (attempt < MAX_RETRIES) {
                long backoffMs = (long) (INITIAL_BACKOFF_MS * Math.pow(BACKOFF_MULTIPLIER, attempt - 1));
                logger.debug("Waiting {}ms before retry", backoffMs);
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new VusionApiException("Interrupted during retry backoff", e);
                }
            }
        }
        
        // All retries exhausted
        logger.error("DELETE failed after {} retries to {}", MAX_RETRIES, url);
        throw lastException;
    }
    
    /**
     * Executes an HTTP POST request.
     */
    private int executePost(String url, String jsonBody) throws IOException {
        HttpPost request = new HttpPost(url);
        request.setHeader(HEADER_SUBSCRIPTION_KEY, subscriptionKey);
        request.setHeader(HEADER_CACHE_CONTROL, "no-cache");
        request.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
        
        return httpClient.execute(request, response -> {
            int statusCode = response.getCode();
            String responseBody = EntityUtils.toString(response.getEntity());
            
            if (statusCode >= 200 && statusCode < 300) {
                logger.trace("POST response: {}", responseBody);
            } else {
                logger.warn("POST error response ({}): {}", statusCode, responseBody);
            }
            
            return statusCode;
        });
    }
    
    /**
     * Executes an HTTP DELETE request with a JSON body.
     */
    private int executeDelete(String url, String jsonBody) throws IOException {
        // HttpDelete with body requires a custom approach
        HttpDeleteWithBody request = new HttpDeleteWithBody(url);
        request.setHeader(HEADER_SUBSCRIPTION_KEY, subscriptionKey);
        request.setHeader(HEADER_CACHE_CONTROL, "no-cache");
        request.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
        
        return httpClient.execute(request, response -> {
            int statusCode = response.getCode();
            String responseBody = EntityUtils.toString(response.getEntity());
            
            if (statusCode >= 200 && statusCode < 300) {
                logger.trace("DELETE response: {}", responseBody);
            } else {
                logger.warn("DELETE error response ({}): {}", statusCode, responseBody);
            }
            
            return statusCode;
        });
    }
    
    @Override
    public void close() throws IOException {
        httpClient.close();
        logger.debug("VusionApiClient closed");
    }
    
    /**
     * Custom HttpDelete class that supports a request body.
     * Standard HttpDelete does not support body in Apache HttpClient 5.
     */
    private static class HttpDeleteWithBody extends HttpPost {
        public HttpDeleteWithBody(String uri) {
            super(uri);
        }
        
        @Override
        public String getMethod() {
            return "DELETE";
        }
    }
    
    /**
     * Exception thrown when Vusion API calls fail.
     */
    public static class VusionApiException extends Exception {
        private final Integer statusCode;
        
        public VusionApiException(String message) {
            super(message);
            this.statusCode = null;
        }
        
        public VusionApiException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }
        
        public VusionApiException(String message, Throwable cause) {
            super(message, cause);
            this.statusCode = null;
        }
        
        public Integer getStatusCode() {
            return statusCode;
        }
    }
}
