package controllers;

import actors.ChannelProfileActor;
import actors.SupervisorActor;
import actors.WordStatsActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.stream.Materializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import models.YouTubeService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import play.mvc.Http;
import play.mvc.Result;
import utils.SessionManager;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;
import static org.junit.Assert.assertEquals;
import static play.mvc.Results.ok;
import static play.mvc.Results.internalServerError;

public class HomeControllerTest {

    private ActorSystem actorSystem;
    private Materializer materializer;
    private YouTubeService mockYouTubeService;
    private SessionManager mockSessionManager;
    private ActorRef mockSupervisorActor;
    private HomeController homeController;

    @Before
    public void setUp() {
        actorSystem = ActorSystem.create();
        materializer = mock(Materializer.class);
        mockYouTubeService = mock(YouTubeService.class);
        mockSessionManager = mock(SessionManager.class);
        mockSupervisorActor = mock(ActorRef.class);

        homeController = new HomeController(actorSystem, materializer, mockYouTubeService, mockSessionManager, mockSupervisorActor);
    }

    @Test
    public void testSearchViaWebSocket() {
        Http.Request mockRequest = mock(Http.Request.class);
        Http.Session mockSession = mock(Http.Session.class);

        when(mockRequest.session()).thenReturn(mockSession);
        when(mockSession.getOptional("id")).thenReturn(java.util.Optional.of("test-session-id"));

        ActorRef mockUserActor = mock(ActorRef.class);
        Mockito.doReturn(CompletableFuture.completedFuture(mockUserActor))
                .when(mockSupervisorActor).tell(any(), any());

        // Validate the WebSocket creation
        assertNotNull(homeController.searchViaWebSocket());
    }

    @Test
    public void testChannelProfile_Success() {
        String channelId = "mockChannelId";

        JsonNode mockProfile = JsonNodeFactory.instance.objectNode().put("items", "profileData");
        JsonNode mockVideos = JsonNodeFactory.instance.objectNode().put("items", "videoData");

        ChannelProfileActor.ChannelProfileResponse mockResponse =
                new ChannelProfileActor.ChannelProfileResponse(channelId, mockProfile, mockVideos, null);

        Mockito.doReturn(CompletableFuture.completedFuture(mockResponse))
                .when(mockSupervisorActor).tell(any(), any());

        CompletionStage<Result> result = homeController.channelProfile(channelId);
        Result actualResult = result.toCompletableFuture().join();

        assertEquals(200, actualResult.status());
    }

    @Test
    public void testChannelProfile_Error() {
        String channelId = "mockChannelId";

        Mockito.doReturn(CompletableFuture.failedFuture(new RuntimeException("Mock error")))
                .when(mockSupervisorActor).tell(any(), any());

        CompletionStage<Result> result = homeController.channelProfile(channelId);
        Result actualResult = result.toCompletableFuture().join();

        assertEquals(500, actualResult.status());
    }

    @Test
    public void testShowWordFrequency_Success() {
        String query = "test query";

        Map<String, Long> mockWordStats = Map.of("word1", 5L, "word2", 3L);
        ActorRef mockActorRef = mock(ActorRef.class); // Mock the ActorRef
        WordStatsActor.WordStatsResponse mockResponse =
                new WordStatsActor.WordStatsResponse(query, mockWordStats, mockActorRef);

        Mockito.doReturn(CompletableFuture.completedFuture(mockResponse))
                .when(mockSupervisorActor).tell(any(), any());

        CompletionStage<Result> result = homeController.showWordFrequency(query);
        Result actualResult = result.toCompletableFuture().join();

        assertEquals(200, actualResult.status());
    }

    @Test
    public void testShowWordFrequency_Error() {
        String query = "test query";

        Mockito.doReturn(CompletableFuture.failedFuture(new RuntimeException("Mock error")))
                .when(mockSupervisorActor).tell(any(), any());

        CompletionStage<Result> result = homeController.showWordFrequency(query);
        Result actualResult = result.toCompletableFuture().join();

        assertEquals(500, actualResult.status());
    }

    @Test
    public void testIndex() {
        CompletionStage<Result> result = homeController.index();
        Result actualResult = result.toCompletableFuture().join();

        assertEquals(200, actualResult.status());
        assertEquals("text/html", actualResult.contentType().orElse(""));
    }
}
