import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.YouTubeService;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import play.libs.Json;
import play.mvc.Http;
import utils.SessionManager;
import actors.UserActor;
import java.util.concurrent.CompletableFuture;
import java.util.Collections;


import static org.mockito.Mockito.*;

public class UserActorTest {

    static ActorSystem system;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create();
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void testHandleSearchRequest() {
        new TestKit(system) {{
            // Mock dependencies
            YouTubeService youTubeService = mock(YouTubeService.class);
            SessionManager sessionManager = mock(SessionManager.class);
            ActorRef readabilityActor = getTestActor(); // Test probe as readability actor
            Http.Session session = mock(Http.Session.class);

            // Mock YouTubeService to return a fake response
            ObjectNode mockYouTubeResponse = Json.newObject();
            ObjectNode videoIdNode = Json.newObject();
            ((ObjectNode) videoIdNode).put("videoId", "12345");

            ObjectNode videoItemNode = Json.newObject();
            ((ObjectNode) videoItemNode).set("id", videoIdNode);
            ((ObjectNode) videoItemNode).put("description", "Sample description");

            mockYouTubeResponse.set("items", Json.newArray().add(videoItemNode));

            when(youTubeService.fetchVideos(anyString(), anyInt()))
                    .thenReturn(CompletableFuture.completedFuture(mockYouTubeResponse));

            // Create UserActor
            ActorRef userActor = system.actorOf(UserActor.props(getTestActor(), youTubeService, sessionManager, session, readabilityActor));

            // Send a search request
            ObjectNode searchRequest = Json.newObject();
            ((ObjectNode) searchRequest).put("type", "search");
            ((ObjectNode) searchRequest).put("query", "test query");
            userActor.tell(searchRequest, getRef());

            // Expect descriptions to be sent to ReadabilityActor as plain text
            expectMsgEquals(Collections.singletonList("Sample description"));

            // Expect initial response to be sent to WebSocket
            ObjectNode expectedResponse = Json.newObject();
            ((ObjectNode) expectedResponse).put("firstResponse", true);
            ((ObjectNode) expectedResponse).put("query", "test query");
            expectedResponse.set("items", mockYouTubeResponse.get("items"));
            expectMsgEquals(expectedResponse);

            // Verify session data preparation
            verify(sessionManager, times(1)).prepareSessionData(any(Http.Session.class), eq("test query"));
        }};
    }

    @Test
    public void testHandleReadabilityScores() {
        new TestKit(system) {{
            // Mock dependencies
            YouTubeService youTubeService = mock(YouTubeService.class);
            SessionManager sessionManager = mock(SessionManager.class);
            ActorRef readabilityActor = getTestActor(); // Test probe as readability actor
            Http.Session session = mock(Http.Session.class);

            // Create UserActor
            ActorRef userActor = system.actorOf(UserActor.props(getTestActor(), youTubeService, sessionManager, session, readabilityActor));

            // Send readability scores to the actor
            ObjectNode readabilityScores = Json.newObject();
            ((ObjectNode) readabilityScores).put("fkGrade", 8.5);
            ((ObjectNode) readabilityScores).put("readingEase", 70.2);
            userActor.tell(readabilityScores, getRef());

            // Expect the enriched response to be sent to the WebSocket
            ObjectNode expectedResponse = Json.newObject();
            ((ObjectNode) expectedResponse).put("fkGrade", 8.5);
            ((ObjectNode) expectedResponse).put("readingEase", 70.2);
            expectMsgEquals(expectedResponse);
        }};
    }
}
