package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import com.fasterxml.jackson.databind.JsonNode;
import models.YouTubeService;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


import java.util.concurrent.CompletionStage;

/**
 * Actor responsible for word count details of the fetched videos from YouTube API.
 *
 * <p>Author:Navachethan Murugeppa  40306253</p>
 */


public class WordStatsActor extends AbstractActor {

    // Instance of YouTubeService for API calls
    private final YouTubeService youTubeService;

    /**
     * Creates Props for WordStatActor.
     *
     * @param youTubeService The YouTubeService instance for API interactions
     * @return Props for creating an instance of WordStatActor
     */
    public static Props props(YouTubeService youTubeService) {
        return Props.create(WordStatsActor.class, youTubeService);
    }

    /**
     * Constructor to initialize the WordStatActor with a YouTubeService instance.
     *
     * @param youTubeService The YouTubeService instance for API interactions
     */
    public WordStatsActor(YouTubeService youTubeService) {
        this.youTubeService = youTubeService;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(FetchWordStatsResponse.class, this::handleFetchWordStats)
                .matchAny(message -> getSender().tell("Unhandled message", getSelf()))
                .build();
    }

    /**
     * Handles perform word stat for  the videos fetched.
     *
     * @param message The FetchWordStatResponse message containing the query and sender reference
     */
    private void handleFetchWordStats(FetchWordStatsResponse message) {
        String query = message.query;
        ActorRef originalSender = message.originalSender;

        CompletionStage<JsonNode> videosStage = youTubeService.fetchVideos(query, 50);
        videosStage.thenApply(videosResponse -> {
                    if (videosResponse == null) {
                        return new WordStatsResponse(query, Collections.emptyMap(), originalSender);
                    }
                    Map<String, Long> wordCalculation = calculateWordFrequency(videosResponse);
                    return new WordStatsResponse(query, wordCalculation, originalSender);
                })
                .exceptionally(ex -> {
                    return new WordStatsResponse(query, Collections.emptyMap(), originalSender);
                })
                .thenAccept(response -> originalSender.tell(response, getSelf()));
    }

    @Override
    public void preStart() {
        // Hook for actions during actor start
    }

    @Override
    public void postStop() {
        // Hook for actions during actor stop
    }

    /**
     * Message class to request for word stat action on fetched videos.
     */
    public static class FetchWordStatsResponse {
        public final String query;
        public final ActorRef originalSender;

        /**
         * Constructor for FetchWordStatResponse message.
         *
         * @param query      The query for videos of the YouTube channel to fetch
         * @param originalSender The sender of the request
         */
        public FetchWordStatsResponse(String query, ActorRef originalSender) {
            this.query = query;
            this.originalSender = originalSender;
        }
    }

    /**
     * Response class containing Map of words and count.
     */
    public static class WordStatsResponse {
        public final String query;
        public final Map<String, Long> response; // Word and word count

        public final ActorRef originalSender;

        /**
         * Constructor for ChannelProfileResponse.
         *
         * @param query      The query search of the YouTube channel
         * @param response   The map response of the word count
         * @param originalSender The original sender of the request
         */
        public WordStatsResponse(String query, Map<String, Long> response, ActorRef originalSender) {
            this.query = query;
            this.response = response;
            this.originalSender = originalSender;
        }
    }

    public static Map<String, Long> calculateWordFrequency(JsonNode json) {
        Logger logger = Logger.getLogger(WordStatsActor.class.getName());

        if (json == null || !json.has("items")) {
            logger.warning("JsonNode is null or does not contain 'items'");
            return new LinkedHashMap<>();
        }

        return StreamSupport.stream(json.path("items").spliterator(), false)
                .map(item -> {
                    StringBuilder text = new StringBuilder();
                    if (item.has("snippet")) {
                        JsonNode snippet = item.get("snippet");
                        if (snippet != null) {
                            text.append(snippet.path("title").asText("")).append(" ")
                                    .append(snippet.path("description").asText(""));
                        } else {
                            logger.warning("Snippet is null for item: " + item);
                        }
                    } else {
                        logger.warning("Item does not contain 'snippet': " + item);
                    }
                    return text.toString();
                })
                .filter(text -> !text.trim().isEmpty())
                .flatMap(text -> Arrays.stream(text.toLowerCase()
                        .replaceAll("[^a-zA-Z\\s]", " ")
                        .split("\\s+")))
                .filter(word -> word.length() > 1)
                .collect(Collectors.groupingBy(
                        word -> word,
                        LinkedHashMap::new,
                        Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }
}
