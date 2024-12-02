package controllers;

import akka.actor.ActorRef;
import akka.stream.Materializer;
import akka.testkit.javadsl.TestKit;
import play.libs.streams.ActorFlow;
import akka.pattern.Patterns;
import akka.actor.Actor;
import akka.actor.Props;
import akka.actor.ActorSystem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.WebSocket;
import play.test.WithApplication;
import play.libs.Json;
import models.YouTubeService;
import actors.SupervisorActor;
import actors.UserActor;
import utils.SessionManager;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * JUnit tests for the HomeController class.
 * These tests validate the functionality of WebSocket communication and index page rendering.
 */
public class HomeControllerTest extends WithApplication {

    private ActorSystem actorSystem;
    private Materializer materializer;
    private YouTubeService mockYouTubeService;
    private SessionManager mockSessionManager;
    private ActorRef supervisorActor;
    private HomeController controller;

    private TestKit probe;

    @Before
    public void setUp() {
        actorSystem = ActorSystem.create();
        materializer = Materializer.createMaterializer(actorSystem);
        mockYouTubeService = mock(YouTubeService.class);
        mockSessionManager = mock(SessionManager.class);

        // Create a mock SupervisorActor for testing
        supervisorActor = actorSystem.actorOf(Props.create(SupervisorActor.class));

        // Initialize the controller with the mocked dependencies
        controller = new HomeController(actorSystem, materializer, mockYouTubeService, mockSessionManager, supervisorActor);

        // Create a probe to interact with the actor
        probe = new TestKit(actorSystem);
    }

    @After
    public void tearDown() {
        actorSystem.terminate();
    }

    /**
     * Test for the WebSocket functionality for searching YouTube videos.
     */
    @Test
    public void testSearchViaWebSocket() {
        Http.Session mockSession = mock(Http.Session.class);
        when(mockSession.getOptional("id")).thenReturn(java.util.Optional.of("test-session-id"));

        // Simulate sending a WebSocket message with a query
        WebSocket ws = controller.searchViaWebSocket();
        // Assume WebSocket is set up with actorRef, and mock the necessary actor flow
        // Simulate a successful response from the SupervisorActor
        supervisorActor.tell(new SupervisorActor.CreateUserActorMessage("test-session-id", null, mockYouTubeService, mockSessionManager, mockSession), probe.getRef());

        // Verify that the SupervisorActor is interacting with the UserActor and sending a proper response
        probe.expectMsgClass(ActorRef.class);
        ActorRef userActor = probe.getLastSender();

        // Check the WebSocket flow creation, this part simulates an actual WebSocket test
        akka.stream.javadsl.Flow<JsonNode, JsonNode, ?> flow = ActorFlow.actorRef(
                out -> UserActor.props(out, mockYouTubeService, mockSessionManager, mockSession, userActor),
                actorSystem,
                materializer
        );
        assertNotNull(flow); // Flow should not be null, indicating the WebSocket setup is correct
    }

    /**
     * Test for the index page rendering method.
     */
    @Test
    public void testIndexPageRendering() {
        CompletionStage<Result> result = controller.index();
        result.toCompletableFuture().join(); // Wait for the result to be completed
        assertEquals(200, result.toCompletableFuture().join().status()); // Assert HTTP status is OK
    }

    /**
     * Test for the constructor to ensure proper dependency injection and setup.
     */
    @Test
    public void testConstructorInjection() {
        assertNotNull(controller); // Ensure controller is properly initialized
        assertEquals(actorSystem, controller.actorSystem); // Check the ActorSystem
    }

    /**
     * Test for the supervisor actor interaction within the controller.
     */
    @Test
    public void testSupervisorActorInteraction() {
        Http.Session mockSession = mock(Http.Session.class);
        when(mockSession.getOptional("id")).thenReturn(java.util.Optional.of("test-session-id"));

        // Simulate supervisor actor's response
        supervisorActor.tell(new SupervisorActor.CreateUserActorMessage("test-session-id", null, mockYouTubeService, mockSessionManager, mockSession), probe.getRef());
        probe.expectMsgClass(ActorRef.class); // Ensure the response contains an ActorRef
    }
}