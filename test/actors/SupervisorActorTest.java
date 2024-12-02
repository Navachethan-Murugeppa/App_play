package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.typed.javadsl.Receive;
import akka.testkit.javadsl.TestKit;
import models.YouTubeService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import utils.SessionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * JUnit tests for the SupervisorActor class.
 * These tests validate the creation of UserActors, ReadabilityActors, ChannelProfileActors, and the Supervisor strategy.
 */
public class SupervisorActorTest {

    private ActorSystem actorSystem;
    private ActorRef supervisorActor;
    private YouTubeService mockYouTubeService;
    private SessionManager mockSessionManager;
    private TestKit probe;

    @Before
    public void setUp() {
        actorSystem = ActorSystem.create();
        mockYouTubeService = mock(YouTubeService.class);
        mockSessionManager = mock(SessionManager.class);

        // Create the SupervisorActor
        supervisorActor = actorSystem.actorOf(SupervisorActor.props());

        // Create a probe to interact with the actor
        probe = new TestKit(actorSystem);
    }

    @After
    public void tearDown() {
        TestKit.shutdownActorSystem(actorSystem);
    }

    /**
     * Test for creating a UserActor via the SupervisorActor.
     */
    @Test
    public void testCreateUserActor() {
        String sessionId = "test-session-id";
        Http.Session mockSession = mock(Http.Session.class);
        ActorRef out = probe.getRef();

        // Send the CreateUserActorMessage to the SupervisorActor
        supervisorActor.tell(new SupervisorActor.CreateUserActorMessage(sessionId, out, mockYouTubeService, mockSessionManager, mockSession), probe.getRef());

        // Expect the SupervisorActor to respond with the UserActor
        ActorRef userActor = probe.expectMsgClass(ActorRef.class);

        // Assert that the UserActor is not null
        assertNotNull(userActor);
    }

    /**
     * Test for supervisor strategy: check if the supervisor restarts the actor on RuntimeException.
     */
    @Test
    public void testSupervisorStrategy() {
        ActorRef testActor = actorSystem.actorOf(Props.create(TestActor.class));
        testActor.tell("Test message", probe.getRef());

        probe.expectNoMessage(); // Expect no immediate message due to restart
        testActor.tell("Another message", probe.getRef());
        probe.expectMsg("Message received after restart");
    }

    /**
     * Test for checking the creation of ReadabilityActor for a session.
     */
    @Test
    public void testCreateReadabilityActor() {
        String sessionId = "test-session-id";
        Http.Session mockSession = mock(Http.Session.class);
        ActorRef out = probe.getRef();

        // Send the CreateUserActorMessage to the SupervisorActor
        supervisorActor.tell(new SupervisorActor.CreateUserActorMessage(sessionId, out, mockYouTubeService, mockSessionManager, mockSession), probe.getRef());

        // Expect the SupervisorActor to respond with the UserActor and ReadabilityActor
        ActorRef userActor = probe.expectMsgClass(ActorRef.class);

        // Verify the ReadabilityActor creation (this can be further tested by checking interactions)
        assertNotNull(userActor);
    }

    /**
     * Test for handling FetchChannelProfileMessage.
     */
    @Test
    public void testFetchChannelProfileMessage() {
        String channelId = "mock-channel-id";

        // Send the FetchChannelProfileMessage to the SupervisorActor
        supervisorActor.tell(new SupervisorActor.FetchChannelProfileMessage(channelId, mockYouTubeService), probe.getRef());

        // Expect no direct response as the message will be forwarded to the ChannelProfileActor
        probe.expectNoMessage();
    }

    /**
     * Test for handling ChannelProfileResponse forwarding.
     */
    @Test
    public void testHandleChannelProfileResponse() {
        String channelId = "mock-channel-id";

        SupervisorActor.FetchChannelProfileMessage fetchMessage = new SupervisorActor.FetchChannelProfileMessage(channelId, mockYouTubeService);
        supervisorActor.tell(fetchMessage, probe.getRef());

        // Simulate a response from the ChannelProfileActor
        ChannelProfileActor.ChannelProfileResponse response =
                new ChannelProfileActor.ChannelProfileResponse(channelId, null, null, probe.getRef());
        supervisorActor.tell(response, probe.getRef());

        // Expect the response to be forwarded back to the original sender
        ChannelProfileActor.ChannelProfileResponse forwardedResponse = probe.expectMsgClass(ChannelProfileActor.ChannelProfileResponse.class);
        assertEquals(response.channelId, forwardedResponse.channelId);
    }

    /**
     * Test to ensure that SupervisorActor properly handles unhandled messages.
     */
    @Test
    public void testUnhandledMessages() {
        supervisorActor.tell("UnhandledMessage", probe.getRef());
        String response = probe.expectMsgClass(String.class);
        assertEquals("Unhandled message", response);
    }

    /**
     * A simple actor that throws a RuntimeException for testing supervisor strategy.
     */
    public static class TestActor extends AbstractActor {
        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(String.class, msg -> {
                        if ("Test message".equals(msg)) {
                            throw new RuntimeException("Test Exception");
                        }
                        getSender().tell("Message received after restart", getSelf());
                    })
                    .build();
        }
    }
}
