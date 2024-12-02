package actors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import models.YouTubeService;
import org.junit.*;
import org.junit.Test;

import java.util.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class WordStatsActorTest {

    private static ActorSystem system;
    private ObjectMapper objectMapper;

    @Before
    public void setup() {
        system = ActorSystem.create();
        objectMapper = new ObjectMapper();
    }

    @After
    public  void teardown() {
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

    @Test
    public void testCalculateWordFrequency_EmptyJson() {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        JsonNode json = factory.objectNode().putArray("items");

        Map<String, Long> wordFrequency = WordStatsActor.calculateWordFrequency(json);

        assertTrue(wordFrequency.isEmpty());
    }

    @Test
    public void testCalculateWordFrequency_ItemsWithoutSnippets() {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        JsonNode json = factory.objectNode()
                .putArray("items")
                .add(factory.objectNode().put("id", "1"))
                .add(factory.objectNode().put("id", "2"));

        Map<String, Long> wordFrequency = WordStatsActor.calculateWordFrequency(json);

        assertTrue(wordFrequency.isEmpty());
    }


    @Test
    public void testCalculateWordFrequency_SortingByFrequency() throws Exception {
        // Updated JSON with specific frequencies to ensure sorting order
        String jsonString = "{\"items\": [" +
                "{\"snippet\": {\"title\": \"Python Python Python\", \"description\": \"Python basics\"}}, " +
                "{\"snippet\": {\"title\": \"Java Java\", \"description\": \"Java basics\"}}" +
                "]}";
        JsonNode json = new ObjectMapper().readTree(jsonString);

        // Calculate word frequency
        Map<String, Long> wordFrequency = WordStatsActor.calculateWordFrequency(json);

        // Verify that "python" appears first due to higher frequency
        List<String> wordsByFrequency = new ArrayList<>(wordFrequency.keySet());
        assertEquals("python", wordsByFrequency.get(0));  // Highest frequency word
        assertEquals("java", wordsByFrequency.get(1));    // Second highest
    }


}