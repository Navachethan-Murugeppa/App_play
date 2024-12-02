package actors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import models.YouTubeService;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class WordStatsActorTest {

    private static ActorSystem system;

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
    public void testHandleFetchWordStats_Success() {
        new TestKit(system) {{
            YouTubeService mockYouTubeService = mock(YouTubeService.class);

            JsonNodeFactory factory = JsonNodeFactory.instance;
            JsonNode mockVideos = factory.objectNode()
                    .putArray("items")
                    .add(factory.objectNode().putObject("snippet").put("title", "Hello World").put("description", "Akka is awesome"))
                    .add(factory.objectNode().putObject("snippet").put("title", "Java Programming").put("description", "Learn Java and Akka"));

            when(mockYouTubeService.fetchVideos("mockQuery", 50))
                    .thenReturn(CompletableFuture.completedFuture(mockVideos));

            ActorRef wordStatsActor = system.actorOf(WordStatsActor.props(mockYouTubeService));

            wordStatsActor.tell(
                    new WordStatsActor.FetchWordStatsResponse("mockQuery", getRef()),
                    getRef()
            );

            WordStatsActor.WordStatsResponse response = expectMsgClass(WordStatsActor.WordStatsResponse.class);
            assertEquals("mockQuery", response.query);
            assertTrue(response.response.containsKey("akka"));
            assertEquals(2L, (long) response.response.get("akka"));

            verify(mockYouTubeService, times(1)).fetchVideos("mockQuery", 50);
        }};
    }

    @Test
    public void testHandleFetchWordStats_NullResponse() {
        new TestKit(system) {{
            YouTubeService mockYouTubeService = mock(YouTubeService.class);

            when(mockYouTubeService.fetchVideos("mockQuery", 50))
                    .thenReturn(CompletableFuture.completedFuture(null));

            ActorRef wordStatsActor = system.actorOf(WordStatsActor.props(mockYouTubeService));

            wordStatsActor.tell(
                    new WordStatsActor.FetchWordStatsResponse("mockQuery", getRef()),
                    getRef()
            );

            WordStatsActor.WordStatsResponse response = expectMsgClass(WordStatsActor.WordStatsResponse.class);
            assertEquals("mockQuery", response.query);
            assertTrue(response.response.isEmpty());

            verify(mockYouTubeService, times(1)).fetchVideos("mockQuery", 50);
        }};
    }

    @Test
    public void testHandleFetchWordStats_Exception() {
        new TestKit(system) {{
            YouTubeService mockYouTubeService = mock(YouTubeService.class);

            when(mockYouTubeService.fetchVideos("mockQuery", 50))
                    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Mock Exception")));

            ActorRef wordStatsActor = system.actorOf(WordStatsActor.props(mockYouTubeService));

            wordStatsActor.tell(
                    new WordStatsActor.FetchWordStatsResponse("mockQuery", getRef()),
                    getRef()
            );

            WordStatsActor.WordStatsResponse response = expectMsgClass(WordStatsActor.WordStatsResponse.class);
            assertEquals("mockQuery", response.query);
            assertTrue(response.response.isEmpty());

            verify(mockYouTubeService, times(1)).fetchVideos("mockQuery", 50);
        }};
    }
}