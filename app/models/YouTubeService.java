package models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.typesafe.config.Config;
import utils.QueryCache;

import javax.inject.Inject;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionStage;

/**
 * YouTubeService: Handles interactions with the YouTube API and provides utility methods for fetching data.
 * It fetches video details from the YouTube API and caches the results for repeated queries.
 *
 * <p>Author: Priyadarshine Kumar 40293041</p>
 * <p>Author: Saranraj Sivakumar 40306771</p>
 */
public class YouTubeService {

    private static final String BASE_URL = "https://www.googleapis.com/youtube/v3/";
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final QueryCache queryCache;

    /**
     * Constructor to initialize the YouTubeService with required dependencies.
     *
     * @param config The configuration object for API key and other settings
     * @param queryCache The cache to store previously fetched query results
     */
    @Inject
    public YouTubeService(Config config, QueryCache queryCache) {
        this.apiKey = config.getString("youtube.api.key");
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.queryCache = queryCache;
    }

    /**
     * Fetches video details from the YouTube API based on the query string.
     * If the query has been previously fetched, it returns the cached result.
     *
     * @param query The search query string
     * @param maxResults The maximum number of results to fetch
     * @return A CompletionStage containing the JSON response with video details
     */
    public CompletionStage<JsonNode> fetchVideos(String query, int maxResults) {
        return queryCache.getOrElseUpdate(query, () -> {
            String url = BASE_URL + "search?part=snippet&maxResults=" + maxResults + "&q=" + encodeQuery(query) + "&key=" + apiKey;
            return sendRequest(url);
        });
    }

    /**
     * Encodes the query string to ensure it is safe for URLs by converting it to ASCII string format.
     *
     * @param query The query string to encode
     * @return The encoded query string
     */
    public String encodeQuery(String query) {
        return URLEncoder.encode(query, StandardCharsets.UTF_8);
    }

    /**
     * Sends an HTTP GET request to the specified URL and returns the response as a JSON object.
     *
     * @param url The API endpoint URL
     * @return A CompletionStage containing the JSON response with the results of the request
     */
    public CompletionStage<JsonNode> sendRequest(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            return objectMapper.readTree(response.body());
                        } catch (Exception e) {
                            return JsonNodeFactory.instance.objectNode().put("error", "Error parsing API response.");
                        }
                    } else {
                        return JsonNodeFactory.instance.objectNode().put("error", "API returned error code: " + response.statusCode());
                    }
                })
                .exceptionally(e -> JsonNodeFactory.instance.objectNode().put("error", "Error during YouTube API request."));
    }

    /**
     * Fetches channel details using the YouTube API.
     * <p>Author: Saranraj Sivakumar 40306771</p>
     * @param channelId The ID of the YouTube channel
     * @return A CompletionStage containing the JSON response with channel details
     */
    public CompletionStage<JsonNode> fetchChannelDetails(String channelId) {
        String url = BASE_URL + "channels?part=snippet,statistics&id=" + encodeQuery(channelId) + "&key=" + apiKey;

        return sendRequest(url).thenApply(response -> {
            if (response.has("error")) {
                return JsonNodeFactory.instance.objectNode().put("error", "Failed to fetch channel details.");
            }
            return response;
        });
    }

    /**
     * Fetches videos from a channel using the YouTube API.
     * <p>Author: Saranraj Sivakumar 40306771</p>
     * @param channelId The ID of the YouTube channel
     * @param maxResults The maximum number of videos to fetch
     * @return A CompletionStage containing the JSON response with the videos
     */
    public CompletionStage<JsonNode> fetchChannelVideos(String channelId, int maxResults) {
        String url = BASE_URL + "search?part=snippet&channelId=" + encodeQuery(channelId) + "&maxResults=" + maxResults + "&order=date&type=video&key=" + apiKey;

        return sendRequest(url).thenApply(response -> {
            if (response.has("error")) {
                return JsonNodeFactory.instance.objectNode().put("error", "Failed to fetch channel videos.");
            }
            return response;
        });
    }
}
