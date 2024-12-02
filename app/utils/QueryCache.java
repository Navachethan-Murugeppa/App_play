package utils;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.Json;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Custom cache implementation for storing query results with expiry.
 * It stores query results and automatically cleans up expired entries.
 *
 * <p>Author: Priyadarshine Kumar 40293041</p>
 * <p>Author: Saranraj Sivakumar 40306771</p>
 */
public class QueryCache {

    private final ConcurrentHashMap<String, CachedItem> cache;
    private final long expiryDurationMillis;
    public final ScheduledExecutorService cleaner;


    /**
     * Constructor to initialize the QueryCache with a default expiry time of 10 seconds.
     * It also starts a background task that periodically cleans expired cache items.
     */
    public QueryCache() {
        this.cache = new ConcurrentHashMap<>();
        this.expiryDurationMillis = 2800; // Cache expiry set to 10 seconds
        this.cleaner = Executors.newScheduledThreadPool(1);

        // Periodically clean expired entries
        cleaner.scheduleAtFixedRate(this::cleanUp, expiryDurationMillis, expiryDurationMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Store the result for a query in the cache.
     *
     * @param key The query string.
     * @param value The JSON result to store.
     */
    public void put(String key, JsonNode value) {
        long expiryTime = System.currentTimeMillis() + expiryDurationMillis;
        System.out.println("Adding item to cache with key: " + key );
        cache.put(key, new CachedItem(value, expiryTime));
    }

    /**
     * Retrieve a result from the cache if it hasn't expired.
     *
     * @param key The query string.
     * @return The cached JSON result, or null if not found or expired.
     */
    public JsonNode get(String key) {
        CachedItem item = cache.get(key);

        return item != null ? item.getValue() : null;
    }

    /**
     * Fetch a result from the cache or compute it using the provided Callable.
     * If the result is not cached or expired, it calls the block to compute it.
     *
     * @param key The query string.
     * @param block The computation block to execute if the result is not in the cache or expired.
     * @return A CompletionStage containing the result.
     */
    public CompletionStage<JsonNode> getOrElseUpdate(String key, Callable<CompletionStage<JsonNode>> block) {
        JsonNode cachedResult = this.get(key);

        if (cachedResult != null) {
            // If result is already cached, return it
            return CompletableFuture.supplyAsync(() -> cachedResult);
        } else {
            try {
                // Fetch the result and cache it
                return block.call().thenApplyAsync(result -> {
                    this.put(key, result);
                    return result;
                });
            } catch (Exception e) {
                e.printStackTrace();
                // Return an empty JSON object in case of failure
                return CompletableFuture.supplyAsync(Json::newObject);
            }
        }
    }

    /**
     * Periodically clean expired items from the cache.
     */
    public void cleanUp() {
        long currentTime = System.currentTimeMillis();
        cache.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().isExpired(currentTime);
            if (expired) {
                System.out.println("Cleaning up expired cache for key: " + entry.getKey());
            }
            return expired;
        });
    }

    /**
     * Shutdown the cache cleaner when no longer needed.
     */
    public void shutdown() {
        cleaner.shutdown();
    }

    /**
     * Inner class representing a cached item with an expiry timestamp.
     */
    public static class CachedItem {
        private final JsonNode value;
        private final long expiryTime;

        public CachedItem(JsonNode value, long expiryTime) {
            this.value = value;
            this.expiryTime = expiryTime;
        }

        public JsonNode getValue() {
            return value;
        }

        public boolean isExpired(long currentTime) {
            return currentTime > expiryTime;
        }
    }
}
