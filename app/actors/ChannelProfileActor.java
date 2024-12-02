package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import com.fasterxml.jackson.databind.JsonNode;
import models.YouTubeService;

import java.util.concurrent.CompletionStage;

/**
 * Actor responsible for managing channel profile details and videos from YouTube API.
 *
 * <p>Author: Saranraj Sivakumar 40306771</p>
 */


public class ChannelProfileActor extends AbstractActor {

    // Instance of YouTubeService for API calls
    private final YouTubeService youTubeService;

    /**
     * Creates Props for ChannelProfileActor.
     *
     * @param youTubeService The YouTubeService instance for API interactions
     * @return Props for creating an instance of ChannelProfileActor
     */
    public static Props props(YouTubeService youTubeService) {
        return Props.create(ChannelProfileActor.class, youTubeService);
    }

    /**
     * Constructor to initialize the ChannelProfileActor with a YouTubeService instance.
     *
     * @param youTubeService The YouTubeService instance for API interactions
     */
    public ChannelProfileActor(YouTubeService youTubeService) {
        this.youTubeService = youTubeService;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(FetchChannelProfile.class, this::handleFetchChannelProfile)
                .matchAny(message -> getSender().tell("Unhandled message", getSelf()))
                .build();
    }

    /**
     * Handles FetchChannelProfile messages to fetch channel details and videos.
     *
     * @param message The FetchChannelProfile message containing the channel ID and sender reference
     */
    private void handleFetchChannelProfile(FetchChannelProfile message) {
        String channelId = message.channelId;
        ActorRef originalSender = message.originalSender;

        CompletionStage<JsonNode> channelDetailsStage = youTubeService.fetchChannelDetails(channelId);
        CompletionStage<JsonNode> videosStage = youTubeService.fetchChannelVideos(channelId, 10);

        channelDetailsStage.thenCombine(videosStage, (channelDetails, videos) -> {
                    if (channelDetails == null || videos == null) {
                        return new ChannelProfileResponse(channelId, null, null, originalSender);
                    }
                    return new ChannelProfileResponse(channelId, channelDetails, videos, originalSender);
                }).thenAccept(response -> originalSender.tell(response, getSelf()))
                .exceptionally(ex -> {
                    originalSender.tell(new ChannelProfileResponse(channelId, null, null, originalSender), getSelf());
                    return null;
                });
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
     * Message class to request fetching channel profile and videos.
     */
    public static class FetchChannelProfile {
        public final String channelId;
        public final ActorRef originalSender;

        /**
         * Constructor for FetchChannelProfile message.
         *
         * @param channelId      The ID of the YouTube channel to fetch
         * @param originalSender The sender of the request
         */
        public FetchChannelProfile(String channelId, ActorRef originalSender) {
            this.channelId = channelId;
            this.originalSender = originalSender;
        }
    }

    /**
     * Response class containing channel details and video list.
     */
    public static class ChannelProfileResponse {
        public final String channelId;
        public final JsonNode profile; // Channel details
        public final JsonNode videos; // Video list
        public final ActorRef originalSender;

        /**
         * Constructor for ChannelProfileResponse.
         *
         * @param channelId      The ID of the YouTube channel
         * @param profile        The details of the channel
         * @param videos         The list of videos from the channel
         * @param originalSender The original sender of the request
         */
        public ChannelProfileResponse(String channelId, JsonNode profile, JsonNode videos, ActorRef originalSender) {
            this.channelId = channelId;
            this.profile = profile;
            this.videos = videos;
            this.originalSender = originalSender;
        }
    }
}
