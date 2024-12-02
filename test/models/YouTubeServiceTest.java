package models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.typesafe.config.Config;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import utils.QueryCache;
import java.util.concurrent.Callable;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.junit.runner.RunWith;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.net.URLEncoder;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@RunWith(PowerMockRunner.class)
@PrepareForTest({URLEncoder.class})

public class YouTubeServiceTest {

    private YouTubeService youTubeService;
    private QueryCache mockCache;
    private Config mockConfig;

    @Before
    public void setUp() {
        // Mock Config
        mockConfig = mock(Config.class);
        when(mockConfig.hasPath("youtube.api.key")).thenReturn(true);
        when(mockConfig.getString("youtube.api.key")).thenReturn("mockApiKey");

        // Mock QueryCache
        mockCache = mock(QueryCache.class);

        // Create YouTubeService with mocked dependencies
        youTubeService = new YouTubeService(mockConfig, mockCache);
    }

    /**
     * Test that fetchVideos uses the cache when a result is available.
     */
    @Test
    public void testFetchVideosUsesCache() throws Exception {
        String query = "test query";
        int maxResults = 5;
        JsonNode mockResponse = JsonNodeFactory.instance.objectNode().put("result", "cached");

        // Mock cache behavior to return a cached response
        when(mockCache.getOrElseUpdate(eq(query), any()))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        // Fetch videos
        CompletionStage<JsonNode> result = youTubeService.fetchVideos(query, maxResults);
        JsonNode actualResponse = result.toCompletableFuture().join();

        // Verify that the cache was used and the result matches
        assertEquals("cached", actualResponse.get("result").asText());
        verify(mockCache, times(1)).getOrElseUpdate(eq(query), any());
    }

    /**
     * Test that fetchVideos fetches from the API when the cache is empty.
     */
    @Test
    public void testFetchVideosFetchesFromAPI() throws Exception {
        String query = "new query";
        int maxResults = 5;

        // Mock the API response
        JsonNode apiResponse = JsonNodeFactory.instance.objectNode().put("result", "apiResponse");

        // Mock the QueryCache behavior
        when(mockCache.getOrElseUpdate(eq(query), any()))
                .thenAnswer(invocation -> {
                    // Simulate the block call and return a CompletionStage
                    Callable<CompletionStage<JsonNode>> callable = invocation.getArgument(1);
                    return callable.call();
                });

        // Mock the sendRequest method in YouTubeService to return the API response
        YouTubeService spyYouTubeService = Mockito.spy(youTubeService);
        doReturn(CompletableFuture.completedFuture(apiResponse))
                .when(spyYouTubeService)
                .sendRequest(anyString());

        // Call fetchVideos
        CompletionStage<JsonNode> result = spyYouTubeService.fetchVideos(query, maxResults);
        JsonNode actualResponse = result.toCompletableFuture().join();

        // Verify the result matches the mocked API response
        assertEquals("apiResponse", actualResponse.get("result").asText());
    }

    /**
     * Test that an invalid API key or query returns an appropriate error message.
     */
    @Test
    public void testFetchVideosHandlesError() throws Exception {
        String query = "invalid query";
        int maxResults = 5;

        JsonNode errorResponse = JsonNodeFactory.instance.objectNode().put("error", "API returned error code: 403");

        // Mock cache behavior to simulate an error
        when(mockCache.getOrElseUpdate(eq(query), any()))
                .thenReturn(CompletableFuture.completedFuture(errorResponse));

        // Fetch videos
        CompletionStage<JsonNode> result = youTubeService.fetchVideos(query, maxResults);
        JsonNode actualResponse = result.toCompletableFuture().join();

        // Verify that the error response is returned
        assertEquals("API returned error code: 403", actualResponse.get("error").asText());
    }

    /**
     * Test that encodeQuery falls back to the original query on exception.
     */
    @Test
    public void testEncodeQueryFallbackOnException() throws Exception {
        // Mock the static URLEncoder class
        PowerMockito.mockStatic(URLEncoder.class);

        // Configure the mock to throw an exception when called
        PowerMockito.when(URLEncoder.encode(Mockito.anyString(), Mockito.anyString()))
                .thenThrow(new IllegalArgumentException("Mocked exception"));

        // Create an instance of YouTubeService with mocked dependencies
        Config mockConfig = mock(Config.class);
        when(mockConfig.hasPath("youtube.api.key")).thenReturn(true);
        when(mockConfig.getString("youtube.api.key")).thenReturn("mockApiKey");

        QueryCache mockQueryCache = mock(QueryCache.class);
        YouTubeService youTubeService = new YouTubeService(mockConfig, mockQueryCache);

        // Provide an input query that will trigger the exception
        String query = "test query";

        // Call the encodeQuery method and verify the fallback
        String result = youTubeService.encodeQuery(query);

        // Verify that the fallback logic was triggered
        assertEquals("test+query",result); // The result should fallback to the original query
    }

    @Test
    public void testEncodeQuery() {
        YouTubeService youTubeService = new YouTubeService(mockConfig, mockCache);

        // Test encoding with special characters
        String query = "test query with special characters \uD83D\uDE80";
        String expectedEncodedQuery = "test+query+with+special+characters+%F0%9F%9A%80";

        // Verify the encoded result
        assertEquals(expectedEncodedQuery, youTubeService.encodeQuery(query));
    }



    /**
     * Test that encodeQuery correctly handles special characters.
     */
    @Test
    public void testEncodeQueryHandlesSpecialCharacters() {
        // Use a string with special characters
        String query = "invalid query string with \uD83D\uDE80";

        // Invoke encodeQuery
        String result = youTubeService.encodeQuery(query);

        // Verify the encoded result
        String expectedEncodedQuery = "invalid+query+string+with+%F0%9F%9A%80";
        assertEquals(expectedEncodedQuery, result);
    }


}
