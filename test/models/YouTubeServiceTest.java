package models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.typesafe.config.Config;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import utils.QueryCache;

import java.net.http.HttpClient;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for YouTubeService class.
 */
public class YouTubeServiceTest {

    private YouTubeService youTubeService;
    private QueryCache mockCache;


    /**
     * Initializes mocks and dependencies before each test.
     */
    @Before
    public void setUp() {
        // Mock configuration
        Config mockConfig = mock(Config.class);
        when(mockConfig.hasPath("youtube.api.key")).thenReturn(true);
        when(mockConfig.getString("youtube.api.key")).thenReturn("mockApiKey");

        // Initialize mock QueryCache
        mockCache = mock(QueryCache.class);



        // Initialize YouTubeService with mocks
        youTubeService = new YouTubeService(mockConfig, mockCache);

    }

    /**
     * Test to ensure fetchVideos uses cache when data is available.
     */
    @Test
    public void testFetchVideosUsesCache() {
        String query = "test query";
        int maxResults = 5;
        JsonNode mockResponse = JsonNodeFactory.instance.objectNode().put("result", "cached");

        // Mock cache to return predefined response
        when(mockCache.getOrElseUpdate(eq(query), any()))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        // Call method under test
        CompletionStage<JsonNode> result = youTubeService.fetchVideos(query, maxResults);
        JsonNode actualResponse = result.toCompletableFuture().join();

        // Verify cached result is returned
        assertEquals("cached", actualResponse.get("result").asText());
    }

    /**
     * Test to ensure fetchVideos fetches data from API when cache is empty.
     */
    @Test
    public void testFetchVideosFetchesFromAPI() {
        String query = "new query";
        int maxResults = 5;
        JsonNode apiResponse = JsonNodeFactory.instance.objectNode().put("result", "apiResponse");

        // Mock cache to simulate a cache miss
        when(mockCache.getOrElseUpdate(eq(query), any()))
                .thenAnswer(invocation -> {
                    Callable<CompletionStage<JsonNode>> callable = invocation.getArgument(1);
                    return callable.call();
                });

        // Spy on YouTubeService and mock API call
        YouTubeService spyYouTubeService = Mockito.spy(youTubeService);
        doReturn(CompletableFuture.completedFuture(apiResponse))
                .when(spyYouTubeService)
                .sendRequest(anyString());

        // Call method under test
        CompletionStage<JsonNode> result = spyYouTubeService.fetchVideos(query, maxResults);
        JsonNode actualResponse = result.toCompletableFuture().join();

        // Verify API result matches mocked response
        assertEquals("apiResponse", actualResponse.get("result").asText());
    }

    /**
     * Test to ensure errors are handled gracefully.
     */
    @Test
    public void testFetchVideosHandlesError() {
        String query = "invalid query";
        int maxResults = 5;

        JsonNode errorResponse = JsonNodeFactory.instance.objectNode().put("error", "API returned error code: 403");

        // Mock cache to return error response
        when(mockCache.getOrElseUpdate(eq(query), any()))
                .thenReturn(CompletableFuture.completedFuture(errorResponse));

        // Call method under test
        CompletionStage<JsonNode> result = youTubeService.fetchVideos(query, maxResults);
        JsonNode actualResponse = result.toCompletableFuture().join();

        // Verify error response is returned
        assertEquals("API returned error code: 403", actualResponse.get("error").asText());
    }

    /**
     * Test to ensure encodeQuery encodes queries correctly.
     */
    @Test
    public void testEncodeQueryHandlesSpecialCharacters() {
        String query = "test query with special characters 🚀";
        String expectedEncodedQuery = "test+query+with+special+characters+%F0%9F%9A%80";

        // Verify encoded result
        assertEquals(expectedEncodedQuery, youTubeService.encodeQuery(query));
    }


    /**
     * Tests the fetchChannelDetails method.
     */
    @Test
    public void testFetchChannelDetails() {
        String channelId = "mockChannelId";

        JsonNode mockResponse = JsonNodeFactory.instance.objectNode().put("channel", "details");

        // Mock the QueryCache to simulate fetching from the API
        when(mockCache.getOrElseUpdate(eq(channelId), any()))
                .thenAnswer(invocation -> {
                    Callable<CompletionStage<JsonNode>> callable = invocation.getArgument(1);
                    return callable.call();
                });

        // Mock the sendRequest method
        YouTubeService spyYouTubeService = Mockito.spy(youTubeService);
        doReturn(CompletableFuture.completedFuture(mockResponse))
                .when(spyYouTubeService)
                .sendRequest(anyString());

        // Call the fetchChannelDetails method
        CompletionStage<JsonNode> result = spyYouTubeService.fetchChannelDetails(channelId);
        JsonNode actualResponse = result.toCompletableFuture().join();

        // Assert the response matches the mock
        assertEquals("details", actualResponse.get("channel").asText());
    }

    /**
     * Tests the fetchChannelVideos method.
     */
    @Test
    public void testFetchChannelVideos() {
        String channelId = "mockChannelId";
        int maxResults = 10;

        JsonNode mockResponse = JsonNodeFactory.instance.objectNode()
                .put("video1", "details1")
                .put("video2", "details2");

        // Mock the QueryCache to simulate fetching from the API
        when(mockCache.getOrElseUpdate(eq(channelId), any()))
                .thenAnswer(invocation -> {
                    Callable<CompletionStage<JsonNode>> callable = invocation.getArgument(1);
                    return callable.call();
                });

        // Mock the sendRequest method
        YouTubeService spyYouTubeService = Mockito.spy(youTubeService);
        doReturn(CompletableFuture.completedFuture(mockResponse))
                .when(spyYouTubeService)
                .sendRequest(anyString());

        // Call the fetchChannelVideos method
        CompletionStage<JsonNode> result = spyYouTubeService.fetchChannelVideos(channelId, maxResults);
        JsonNode actualResponse = result.toCompletableFuture().join();

        // Assert the response matches the mock
        assertEquals("details1", actualResponse.get("video1").asText());
        assertEquals("details2", actualResponse.get("video2").asText());
    }

    /**
     * Tests the fetchChannelVideos method when the API returns an error.
     */
    @Test
    public void testFetchChannelVideosHandlesError() {
        String channelId = "mockChannelId";
        int maxResults = 10;

        JsonNode errorResponse = JsonNodeFactory.instance.objectNode().put("error", "API error");

        // Mock the sendRequest method to simulate an error
        YouTubeService spyYouTubeService = Mockito.spy(youTubeService);
        doReturn(CompletableFuture.completedFuture(errorResponse))
                .when(spyYouTubeService)
                .sendRequest(anyString());

        // Call the fetchChannelVideos method
        CompletionStage<JsonNode> result = spyYouTubeService.fetchChannelVideos(channelId, maxResults);
        JsonNode actualResponse = result.toCompletableFuture().join();

        // Assert the error response matches the expected output
        assertEquals("Failed to fetch channel videos.", actualResponse.get("error").asText());
    }


}








