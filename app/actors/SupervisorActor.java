package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.OneForOneStrategy;
import akka.actor.SupervisorStrategy;
import akka.actor.Status;
import scala.concurrent.duration.Duration;
import models.YouTubeService;
import utils.SessionManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * SupervisorActor is responsible for managing child actors such as UserActor, ReadabilityActor, and ChannelProfileActor.
 * It handles the creation of actors and forwards messages to the appropriate child actors.
 *
 * <p>Author: Saranraj Sivakumar 40306771</p>
 * <p>Author: Priyadarshine Kumar 40293041</p>
 */
public class SupervisorActor extends AbstractActor {

    private final Map<String, ActorRef> userActors = new HashMap<>();
    private final Map<String, ActorRef> readabilityActors = new HashMap<>();
    private final Map<String, ActorRef> channelProfileActors = new HashMap<>();
    private final Map<String, ActorRef> wordStatsActor = new HashMap<>();

    /**
     * Factory method to create Props for SupervisorActor.
     *
     * @return A Props instance for SupervisorActor
     */
    public static Props props() {
        return Props.create(SupervisorActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(CreateUserActorMessage.class, this::handleCreateUserActorMessage)
                .match(FetchChannelProfileMessage.class, this::handleFetchChannelProfileMessage)
                .match(FetchWordStatsMessage.class, this::handleFetchWordStatsMessage)
                .match(ChannelProfileActor.ChannelProfileResponse.class, this::handleChannelProfileResponse)

                .match(WordStatsActor.WordStatsResponse.class, this::handleWordStatsResponse)
                .matchAny(message -> getSender().tell("Unhandled message", getSelf()))
                .build();
    }

    /**
     * Handles the creation of a UserActor for the given session.
     *
     * @param message The message containing session information and dependencies
     */
    private void handleCreateUserActorMessage(CreateUserActorMessage message) {
        try {
            String sessionId = message.sessionId;

            // Create or retrieve the ReadabilityActor for this session
            ActorRef readabilityActor = readabilityActors.computeIfAbsent(sessionId, id ->
                    getContext().actorOf(ReadabilityActor.props(getSelf()), "readabilityActor-" + id)
            );

            // Create or retrieve the UserActor for this session
            ActorRef userActor = userActors.computeIfAbsent(sessionId, id ->
                    getContext().actorOf(
                            UserActor.props(message.out, message.youTubeService, message.sessionManager, message.session, readabilityActor),
                            "userActor-" + id
                    )
            );

            // Send the UserActor reference back to the sender
            getSender().tell(userActor, getSelf());
        } catch (Exception e) {
            getSender().tell(new Status.Failure(e), getSelf());
        }
    }

    /**
     * Handles a request to fetch channel profile information by delegating the task to ChannelProfileActor.
     *
     * @param message The message containing the channelId and YouTubeService instance
     */
    private void handleFetchChannelProfileMessage(FetchChannelProfileMessage message) {
        try {
            String channelId = message.channelId;

            // Create or retrieve the ChannelProfileActor for this channel
            ActorRef channelProfileActor = channelProfileActors.computeIfAbsent(channelId, id ->
                    getContext().actorOf(ChannelProfileActor.props(message.youTubeService), "channelProfileActor-" + id)
            );

            // Forward the FetchChannelProfile message with the original sender
            channelProfileActor.tell(new ChannelProfileActor.FetchChannelProfile(channelId, getSender()), getSelf());
        } catch (Exception e) {
            getSender().tell(new Status.Failure(e), getSelf());
        }
    }

    private void handleFetchWordStatsMessage(FetchWordStatsMessage message) {
        try {
            String query = message.query;

            // Create or retrieve the ChannelProfileActor for this channel
            ActorRef WordStatsActor = wordStatsActor.computeIfAbsent(query, id ->
                    getContext().actorOf(actors.WordStatsActor.props(message.youTubeService), "wordStatsActor-" + id)
            );

            // Forward the FetchChannelProfile message with the original sender
            WordStatsActor.tell(new WordStatsActor.FetchWordStatsResponse(query, getSender()), getSelf());
        } catch (Exception e) {
            getSender().tell(new Status.Failure(e), getSelf());
        }
    }

    /**
     * Handles the response from a ChannelProfileActor and forwards it to the original requester.
     *
     * @param response The response containing channel profile and video details
     */
    private void handleChannelProfileResponse(ChannelProfileActor.ChannelProfileResponse response) {
        try {
            response.originalSender.tell(response, getSelf());
        } catch (Exception e) {
            getSender().tell(new Status.Failure(e), getSelf());
        }
    }

    private void handleWordStatsResponse(WordStatsActor.WordStatsResponse response) {
        try {
            response.originalSender.tell(response, getSelf());
        } catch (Exception e) {
            getSender().tell(new Status.Failure(e), getSelf());
        }
    }

    /**
     * Defines the supervision strategy for managing child actor failures.
     *
     * @return The SupervisorStrategy instance for managing child actor failures
     */
    @Override
    public SupervisorStrategy supervisorStrategy() {
        return new OneForOneStrategy(
                10,
                Duration.create(1, TimeUnit.MINUTES),
                throwable -> {
                    if (throwable instanceof RuntimeException) {
                        return SupervisorStrategy.restart();
                    } else {
                        return SupervisorStrategy.stop();
                    }
                }
        );
    }

    @Override
    public void preStart() {
        // Lifecycle log for preStart
    }

    @Override
    public void postStop() {
        // Lifecycle log for postStop
    }

    /**
     * Message class for creating a UserActor.
     */
    public static class CreateUserActorMessage {
        public final String sessionId;
        public final ActorRef out;
        public final YouTubeService youTubeService;
        public final SessionManager sessionManager;
        public final play.mvc.Http.Session session;

        /**
         * @param sessionId The unique session identifier
         * @param out The ActorRef for sending WebSocket responses
         * @param youTubeService The YouTubeService instance for API interaction
         * @param sessionManager The SessionManager instance for session handling
         * @param session The HTTP session associated with the user
         */
        public CreateUserActorMessage(String sessionId, ActorRef out, YouTubeService youTubeService, SessionManager sessionManager, play.mvc.Http.Session session) {
            this.sessionId = sessionId;
            this.out = out;
            this.youTubeService = youTubeService;
            this.sessionManager = sessionManager;
            this.session = session;
        }
    }

    /**
     * Message class for requesting a channel profile.
     */
    public static class FetchChannelProfileMessage {
        public final String channelId;
        public final YouTubeService youTubeService;

        /**
         *
         * @param channelId The unique identifier for the YouTube channel
         * @param youTubeService The YouTubeService instance for API interaction
         */
        public FetchChannelProfileMessage(String channelId, YouTubeService youTubeService) {
            this.channelId = channelId;
            this.youTubeService = youTubeService;
        }
    }


    public static class FetchWordStatsMessage {
        public final String query;
        public final YouTubeService youTubeService;

        /**
         *
         * @param query The query term to search the videos
         * @param youTubeService The YouTubeService instance for API interaction
         */
        public FetchWordStatsMessage(String query, YouTubeService youTubeService) {
            this.query = query;
            this.youTubeService = youTubeService;
        }
    }
}
