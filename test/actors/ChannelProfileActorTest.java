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
import org.mockito.Mockito;

import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

public class ChannelProfileActorTest {

    private static ActorSystem system;

    /**
     * Sets up the ActorSystem for testing.
     */
    @BeforeClass
    public static void setup() {
        system = ActorSystem.create();
    }

    /**
     * Shuts down the ActorSystem after tests are completed.
     */
    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    /**
     * Tests the successful handling of a FetchChannelProfile message.
     * Verifies that valid channel details and videos are fetched and returned in the response.
     */
    @Test
    public void testHandleFetchChannelProfile_Success() {
        new TestKit(system) {{
            // Arrange
            YouTubeService mockYouTubeService = mock(YouTubeService.class);

            // Mock API responses
            JsonNode mockChannelDetails = JsonNodeFactory.instance.objectNode().put("title", "Mock Channel");
            JsonNode mockVideos = JsonNodeFactory.instance.objectNode().putArray("items").add("Video1").add("Video2");

            when(mockYouTubeService.fetchChannelDetails("mockChannelId"))
                    .thenReturn(CompletableFuture.completedFuture(mockChannelDetails));

            when(mockYouTubeService.fetchChannelVideos("mockChannelId", 10))
                    .thenReturn(CompletableFuture.completedFuture(mockVideos));

            ActorRef channelProfileActor = system.actorOf(ChannelProfileActor.props(mockYouTubeService));

            // Act
            channelProfileActor.tell(
                    new ChannelProfileActor.FetchChannelProfile("mockChannelId", getRef()),
                    getRef()
            );

            // Assert
            ChannelProfileActor.ChannelProfileResponse response = expectMsgClass(ChannelProfileActor.ChannelProfileResponse.class);
            assertEquals("mockChannelId", response.channelId);
            assertEquals(mockChannelDetails, response.profile);
            assertEquals(mockVideos, response.videos);

            // Verify mocks
            verify(mockYouTubeService, times(1)).fetchChannelDetails("mockChannelId");
            verify(mockYouTubeService, times(1)).fetchChannelVideos("mockChannelId", 10);
        }};
    }

    /**
     * Tests the handling of a FetchChannelProfile message when the API returns null responses.
     * Verifies that null values are returned in the response for both channel details and videos.
     */
    @Test
    public void testHandleFetchChannelProfile_Failure() {
        new TestKit(system) {{
            // Arrange
            YouTubeService mockYouTubeService = mock(YouTubeService.class);

            // Mock failed API responses
            when(mockYouTubeService.fetchChannelDetails("mockChannelId"))
                    .thenReturn(CompletableFuture.completedFuture(null));

            when(mockYouTubeService.fetchChannelVideos("mockChannelId", 10))
                    .thenReturn(CompletableFuture.completedFuture(null));

            ActorRef channelProfileActor = system.actorOf(ChannelProfileActor.props(mockYouTubeService));

            // Act
            channelProfileActor.tell(
                    new ChannelProfileActor.FetchChannelProfile("mockChannelId", getRef()),
                    getRef()
            );

            // Assert
            ChannelProfileActor.ChannelProfileResponse response = expectMsgClass(ChannelProfileActor.ChannelProfileResponse.class);
            assertEquals("mockChannelId", response.channelId);
            assertNull(response.profile);
            assertNull(response.videos);

            // Verify mocks
            verify(mockYouTubeService, times(1)).fetchChannelDetails("mockChannelId");
            verify(mockYouTubeService, times(1)).fetchChannelVideos("mockChannelId", 10);
        }};
    }

    /**
     * Tests the handling of a FetchChannelProfile message when exceptions occur during API calls.
     * Verifies that null values are returned in the response for both channel details and videos.
     */
    @Test
    public void testHandleFetchChannelProfile_Exception() {
        new TestKit(system) {{
            // Arrange
            YouTubeService mockYouTubeService = mock(YouTubeService.class);

            // Mock exceptions in API calls
            when(mockYouTubeService.fetchChannelDetails("mockChannelId"))
                    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Mock Exception")));

            when(mockYouTubeService.fetchChannelVideos("mockChannelId", 10))
                    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Mock Exception")));

            ActorRef channelProfileActor = system.actorOf(ChannelProfileActor.props(mockYouTubeService));

            // Act
            channelProfileActor.tell(
                    new ChannelProfileActor.FetchChannelProfile("mockChannelId", getRef()),
                    getRef()
            );

            // Assert
            ChannelProfileActor.ChannelProfileResponse response = expectMsgClass(ChannelProfileActor.ChannelProfileResponse.class);
            assertEquals("mockChannelId", response.channelId);
            assertNull(response.profile);
            assertNull(response.videos);

            // Verify mocks
            verify(mockYouTubeService, times(1)).fetchChannelDetails("mockChannelId");
            verify(mockYouTubeService, times(1)).fetchChannelVideos("mockChannelId", 10);
        }};
    }
}
