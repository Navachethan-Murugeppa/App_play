package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import com.fasterxml.jackson.databind.JsonNode;
import models.YouTubeService;
import play.libs.Json;
import play.mvc.Http;
import utils.SessionManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * UserActor is responsible for handling user-specific operations, such as searching for videos on YouTube,
 * managing session data, and processing readability scores for the search results.
 * It communicates with YouTubeService to fetch video details and ReadabilityActor to calculate readability scores.
 *
 * <p>Author: Priyadarshine Kumar 40293041</p>
 */
public class UserActor extends AbstractActor {

    private final Http.Session session;
    private final ActorRef out;
    private final YouTubeService youTubeService;
    private final SessionManager sessionManager;
    private final Set<String> fetchedVideoIds = new HashSet<>();
    private boolean isFirstSearch = true;

    // Add ReadabilityActor to be passed as part of UserActor props
    private final ActorRef readabilityActor;
    private final ActorRef sentimentActor;

    /**
     * Factory method to create the UserActor with the required parameters.
     *
     * @param out The ActorRef to send messages back to the client
     * @param youTubeService The YouTubeService instance for interacting with YouTube
     * @param sessionManager The SessionManager instance for managing session data
     * @param session The HTTP session for the user
     * @param readabilityActor The ReadabilityActor instance for processing video descriptions
     * @return A Props instance for creating the UserActor
     */
    public static Props props(ActorRef out, YouTubeService youTubeService, SessionManager sessionManager, Http.Session session, ActorRef readabilityActor) {
        return Props.create(UserActor.class, () -> new UserActor(out, youTubeService, sessionManager, session, readabilityActor));
    }

    /**
     * Constructor to initialize the UserActor with the required parameters.
     *
     * @param out The ActorRef to send messages back to the client
     * @param youTubeService The YouTubeService instance for interacting with YouTube
     * @param sessionManager The SessionManager instance for managing session data
     * @param session The HTTP session for the user
     * @param readabilityActor The ReadabilityActor instance for processing video descriptions
     */
    private UserActor(ActorRef out, YouTubeService youTubeService, SessionManager sessionManager, Http.Session session, ActorRef readabilityActor) {
        this.out = out;
        this.youTubeService = youTubeService;
        this.sessionManager = sessionManager;
        this.session = session;
        this.readabilityActor = readabilityActor; // Initialize ReadabilityActor
        this.sentimentActor = getContext().actorOf(Props.create(SentimentActor.class), "sentimentActor");
    }


    /**
     * Defines the behavior of the UserActor. It listens for messages of type JsonNode.
     * If the message is a "search" request, it performs a YouTube search.
     * It also processes readability scores received from the ReadabilityActor.
     *
     * @return The receive builder defining the actor's behavior
     */
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(JsonNode.class, message -> {
                    if (message.has("type") && "search".equals(message.get("type").asText())) {
                        String query = message.get("query").asText();
                        handleSearch(query);
                    }
                    // Handle readability response
                    if (message.has("fkGrade") && message.has("readingEase")) {
                        // Update the item with readability scores
                        handleReadabilityScores(message);
                    }
                })
                .build();
    }

    /**
     * Handles the search request by performing a YouTube search using the provided query.
     * It processes the results, sends descriptions to the ReadabilityActor, and responds back to the client.
     *
     * @param query The search query to be used for fetching videos from YouTube
     */
    private void handleSearch(String query) {
        // Update session data using SessionManager
        String updatedSessionData = sessionManager.prepareSessionData(session, query);
        System.out.println("Updated session data: " + updatedSessionData + " | Added query: " + query);

        // Perform YouTube search
        youTubeService.fetchVideos(query, 10).thenAccept(results -> {
            if (results != null && results.has("items")) {
                JsonNode items = results.get("items");

                // Extract descriptions from video items
                List<String> descriptions = items.findValuesAsText("description");

                // Send descriptions to ReadabilityActor for processing
                readabilityActor.tell(descriptions, self());
                sentimentActor.tell(descriptions, self());
                // Filter for unique video results
                items.forEach(item -> {
                    String videoId = item.path("id").path("videoId").asText();
                    if (!fetchedVideoIds.contains(videoId)) {
                        fetchedVideoIds.add(videoId);
                    }
                });

                // Send the initial response back to the client
                JsonNode response = Json.newObject()
                        .put("firstResponse", isFirstSearch)
                        .put("query", query)
                        .set("items", items);

                isFirstSearch = false;
                out.tell(response, self());
            }
        }).exceptionally(e -> {
            out.tell(Json.newObject().put("error", e.getMessage()), self());
            return null;
        });
    }

    /**
     * Handles the readability scores received from the ReadabilityActor and includes them in the response.
     *
     * @param scores The JsonNode containing the readability scores (fkGrade and readingEase)
     */
    private void handleReadabilityScores(JsonNode scores) {
        // Assuming the score contains the readability data for a particular item
        JsonNode response = Json.newObject()
                .put("fkGrade", scores.get("fkGrade").asDouble())
                .put("readingEase", scores.get("readingEase").asDouble());

        // Add readability scores to the video items (can loop over the items to add to each)
        out.tell(response, self());
    }

    /**
            * Handles the sentiment analysis scores and adds the sentiment result to the response.
     *
             * @param sentiment The JsonNode containing the sentiment analysis result
     */
    private void handleSentimentScores(JsonNode sentiment) {
        System.out.println("Sentiment Scores: " + sentiment);

        // Create a combined response that includes both readability and sentiment data
        JsonNode combinedResponse = Json.newObject()
                .put("sentiment", sentiment.get("sentiment").asDouble()); // Assuming sentiment is a numeric value

        // Send the combined response to the client
        out.tell(combinedResponse, self());
    }
}
