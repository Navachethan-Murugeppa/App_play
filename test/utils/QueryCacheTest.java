package utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
import java.util.concurrent.Executors;
import play.libs.Json;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledExecutorService;


/**
 * JUnit tests for the QueryCache class.
 * These tests validate the caching functionality, expiry behavior, and retrieval of cached data.
 */
public class QueryCacheTest {

    private QueryCache queryCache;
    private ScheduledExecutorService mockCleaner;

    @Before
    public void setUp() {
        // Initialize the QueryCache before each test
        queryCache = new QueryCache();

    }

    @After
    public void tearDown() {
        // Clean up the cache after each test
        queryCache.shutdown();
    }

    /**
     * Test that the cache correctly stores and retrieves an item.
     */
    @Test
    public void testPutAndGet() {
        JsonNode mockValue = JsonNodeFactory.instance.objectNode().put("key", "value");

        String key = "testKey";

        // Store the value in the cache
        queryCache.put(key, mockValue);

        // Retrieve the value from the cache
        JsonNode retrievedValue = queryCache.get(key);

        // Assert that the retrieved value matches the stored value
        assertNotNull(retrievedValue);
        assertEquals("value", retrievedValue.get("key").asText());
    }

    /**
     * Test that the cache correctly removes expired items.
     */
    @Test
    public void testCacheExpiry() throws InterruptedException {
        JsonNode mockValue = JsonNodeFactory.instance.objectNode().put("key", "value");

        String key = "testKey";

        // Store the value in the cache with a short expiry time (1 millisecond)
        queryCache.put(key, mockValue);

        // Wait for the cache to expire (10 seconds)
        TimeUnit.MILLISECONDS.sleep(4000);

        // Try to retrieve the value after expiry
        JsonNode retrievedValue = queryCache.get(key);

        // Assert that the value is no longer available in the cache
        assertNull(retrievedValue);
    }

    /**
     * Test that the cache correctly updates the value when the cache is empty.
     */
    @Test
    public void testGetOrElseUpdate() throws Exception {
        String key = "testKey";
        JsonNode mockValue = JsonNodeFactory.instance.objectNode().put("key", "value");

        // Mock the callable to return a new value
        Callable<CompletionStage<JsonNode>> mockBlock = mock(Callable.class);
        when(mockBlock.call()).thenReturn(CompletableFuture.completedFuture(mockValue));

        // Fetch the value, it should be computed and cached
        CompletionStage<JsonNode> result = queryCache.getOrElseUpdate(key, mockBlock);

        // Wait for the result and verify it is correct
        JsonNode resultValue = result.toCompletableFuture().join();

        // Assert that the retrieved value matches the expected value
        assertNotNull(resultValue);
        assertEquals("value", resultValue.get("key").asText());

        // Verify that the block was called once
        verify(mockBlock, times(1)).call();
    }

    @Test
    public void testGetOrElseUpdateWithException() throws Exception {
        QueryCache queryCache = new QueryCache();
        String key = "testKey";

        // Callable block that throws an exception
        CompletionStage<JsonNode> result = queryCache.getOrElseUpdate(key, () -> {
            throw new RuntimeException("Simulated exception");
        });

        // Verify that the result is an empty JSON object
        JsonNode actualResult = result.toCompletableFuture().join();
        assertEquals(Json.newObject(), actualResult);
    }


    /**
     * Test that the cache returns the cached value if it exists.
     */
    @Test
    public void testGetFromCache() throws Exception {
        String key = "testKey";
        JsonNode mockValue = JsonNodeFactory.instance.objectNode().put("key", "value");

        // Store the value in the cache
        queryCache.put(key, mockValue);

        // Mock the callable to return a different value
        Callable<CompletionStage<JsonNode>> mockBlock = mock(Callable.class);
        when(mockBlock.call()).thenReturn(CompletableFuture.completedFuture(JsonNodeFactory.instance.objectNode().put("key", "new value")));

        // Fetch the value, which should come from the cache
        CompletionStage<JsonNode> result = queryCache.getOrElseUpdate(key, mockBlock);

        // Wait for the result and verify it is correct
        JsonNode resultValue = result.toCompletableFuture().join();

        // Assert that the cached value is returned
        assertNotNull(resultValue);
        assertEquals("value", resultValue.get("key").asText());

        // Verify that the block was not called (since the value was cached)
        verify(mockBlock, times(0)).call();
    }

    @Test
    public void testShutdown() {
        QueryCache queryCache = new QueryCache();

        // Shutdown the cache cleaner and ensure it terminates
        queryCache.shutdown();
        assertTrue(queryCache.cleaner.isShutdown());

        // Ensure forced shutdown works
        try {
            queryCache.shutdown();
        } catch (Exception e) {
            fail("Forced shutdown failed: " + e.getMessage());
        }
    }


    @Test
    public void testCleanUpRemovesExpiredItems() throws Exception {
        QueryCache queryCache = new QueryCache();
        String key = "testKey";
        JsonNode value = Json.newObject().put("test", "value");

        // Add an item to the cache
        queryCache.put(key, value);

        // Simulate time passing (wait for expiry)
        Thread.sleep(3100);

        // Trigger clean-up
        queryCache.cleanUp();

        // Verify the item has been removed
        assertNull(queryCache.get(key));
    }


}