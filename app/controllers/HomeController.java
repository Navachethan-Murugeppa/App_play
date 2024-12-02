package controllers;

import actors.ChannelProfileActor;
import actors.WordStatsActor;
import akka.actor.ActorSystem;
import akka.stream.Materializer;
import akka.pattern.Patterns;
import akka.actor.ActorRef;
import play.libs.streams.ActorFlow;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import play.mvc.Http;
import com.fasterxml.jackson.databind.JsonNode;
import actors.SupervisorActor;
import actors.UserActor;
import models.YouTubeService;
import utils.SessionManager;
import javax.inject.Named;
import javax.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * HomeController is responsible for handling requests related to YouTube search through WebSockets.
 * It interacts with SupervisorActor to manage user sessions and the creation of UserActor instances.
 *
 * <p>Author: Priyadarshine Kumar 40293041</p>
 * <p>Author: Saranraj Sivakumar 40306771</p>
 *
 */

public class HomeController extends Controller {

    final ActorSystem actorSystem;
    final Materializer materializer;
    private final YouTubeService youTubeService;
    private final SessionManager sessionManager;
    final ActorRef supervisorActor;

    /**
     * Constructor to initialize the HomeController with the required dependencies.
     *
     * @param actorSystem The ActorSystem instance
     * @param materializer The Materializer instance
     * @param youTubeService The YouTubeService instance
     * @param sessionManager The SessionManager instance
     * @param supervisorActor The ActorRef for SupervisorActor
     */

    @Inject
    public HomeController(
            ActorSystem actorSystem,
            Materializer materializer,
            YouTubeService youTubeService,
            SessionManager sessionManager,
            @Named("supervisorActor") ActorRef supervisorActor) {
        this.actorSystem = actorSystem;
        this.materializer = materializer;
        this.youTubeService = youTubeService;
        this.sessionManager = sessionManager;
        this.supervisorActor = supervisorActor;
    }


    /**
     * WebSocket endpoint for searching YouTube videos. It creates a WebSocket connection
     * where the client can send search queries and receive video results. It interacts with
     * the SupervisorActor to create a UserActor for managing the user's session.
     *
     * @return A WebSocket instance that handles the search communication
     */

    public WebSocket searchViaWebSocket() {
        return WebSocket.Json.acceptOrResult(request -> {
            Http.Session session = request.session();
            String sessionId = session.getOptional("id").orElse("anonymous-" + System.currentTimeMillis());

            return Patterns.ask(
                    supervisorActor,
                    new SupervisorActor.CreateUserActorMessage(sessionId, null, youTubeService, sessionManager, session),
                    Duration.ofSeconds(5)
            ).thenApply(response -> {
                if (response instanceof ActorRef) {
                    ActorRef userActorRef = (ActorRef) response;

                    akka.stream.javadsl.Flow<JsonNode, JsonNode, ?> flow = ActorFlow.actorRef(
                            out -> UserActor.props(out, youTubeService, sessionManager, session, userActorRef),
                            actorSystem,
                            materializer
                    );

                    return play.libs.F.Either.<Result, akka.stream.javadsl.Flow<JsonNode, JsonNode, ?>>Right(flow);
                } else {
                    return play.libs.F.Either.<Result, akka.stream.javadsl.Flow<JsonNode, JsonNode, ?>>Left(
                            forbidden("Supervisor failed to create a UserActor.")
                    );
                }
            }).exceptionally(e ->
                    play.libs.F.Either.<Result, akka.stream.javadsl.Flow<JsonNode, JsonNode, ?>>Left(
                            forbidden("WebSocket error: " + e.getMessage())
                    )
            );
        });
    }
    /**
     * Handles the channel profile request by fetching channel details and videos via the SupervisorActor.
     *
     * <p>Author: Saranraj Sivakumar 40306771</p>
     *
     * @param channelId The unique identifier of the YouTube channel whose details are to be fetched
     * @return A CompletionStage of Result that contains the channel profile data and video list or an error response
     */

    public CompletionStage<Result> channelProfile(String channelId) {
        return Patterns.ask(supervisorActor, new SupervisorActor.FetchChannelProfileMessage(channelId, youTubeService), Duration.ofSeconds(60))
                .thenApply(response -> {
                    if (response instanceof ChannelProfileActor.ChannelProfileResponse) {
                        ChannelProfileActor.ChannelProfileResponse channelResponse = (ChannelProfileActor.ChannelProfileResponse) response;

                        if (channelResponse.profile == null || !channelResponse.profile.has("items") || channelResponse.profile.path("items").isEmpty()) {
                            return internalServerError("Error: Unable to fetch valid channel profile data.");
                        }

                        if (channelResponse.videos == null || !channelResponse.videos.has("items") || channelResponse.videos.path("items").isEmpty()) {
                            return internalServerError("Error: Unable to fetch videos for the channel.");
                        }

                        JsonNode channelData = channelResponse.profile.path("items").get(0);
                        if (channelData == null || channelData.isMissingNode()) {
                            return internalServerError("Error: Missing channel data.");
                        }

                        List<JsonNode> videosList = new ArrayList<>();
                        channelResponse.videos.path("items").forEach(videosList::add);

                        return ok(views.html.channelprofile.render(channelData, videosList));
                    } else {
                        return internalServerError("Error: Unexpected response from SupervisorActor.");
                    }
                })
                .exceptionally(ex -> internalServerError("An unexpected error occurred while processing the channel profile."));
    }

    public CompletionStage<Result> showWordFrequency(String query) {
        return Patterns.ask(supervisorActor, new SupervisorActor.FetchWordStatsMessage(query, youTubeService), Duration.ofSeconds(60))
                .thenApply(response -> {
                    if (response instanceof WordStatsActor.WordStatsResponse) {
                        WordStatsActor.WordStatsResponse wordsResponse = (WordStatsActor.WordStatsResponse) response;

//                        if (wordsResponse.profile == null || !channelResponse.profile.has("items") || channelResponse.profile.path("items").isEmpty()) {
//                            return internalServerError("Error: Unable to fetch valid channel profile data.");
//                        }
//
//                        if (channelResponse.videos == null || !channelResponse.videos.has("items") || channelResponse.videos.path("items").isEmpty()) {
//                            return internalServerError("Error: Unable to fetch videos for the channel.");
//                        }

                        Map<String, Long> wordsData = wordsResponse.response;
                        if (wordsData.isEmpty()) {
                            return internalServerError("Error: Missing channel data.");
                        }



                        return ok(views.html.wordStats.render(wordsResponse.query, wordsData));
                    } else {
                        return internalServerError("Error: Unexpected response from SupervisorActor.");
                    }
                })
                .exceptionally(ex -> internalServerError("An unexpected error occurred while processing the channel profile."));
    }

    /**
     * Index page rendering. This method is responsible for rendering the home page
     * of the application.
     *
     * @return A CompletionStage that represents the result of rendering the index page
     */
    public CompletionStage<Result> index() {
        return CompletableFuture.supplyAsync(() -> ok(views.html.index.render("TubeLytics - YouTube Search")));
    }
}
