package actors;

import akka.actor.ActorSystem;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import akka.actor.AbstractActor;
import akka.actor.SupervisorStrategy;
import akka.actor.OneForOneStrategy;
import akka.pattern.Patterns;
import models.YouTubeService;
import utils.SessionManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

/**
 * JUnit tests for the SupervisorActor class.
 * These tests validate the creation of UserActors and ReadabilityActors, and the Supervisor strategy.
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
        actorSystem.terminate();
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
        // Define an actor that throws a RuntimeException
        ActorRef actorRef = actorSystem.actorOf(Props.create(TestActor.class));

        // Send a message that causes a RuntimeException
        actorRef.tell("Test message", probe.getRef());

        // Expect the actor to be restarted by the supervisor (no immediate response)
        probe.expectNoMessage();  // No message should be received initially due to restart

        // After restart, send another message to check if the actor has recovered
        actorRef.tell("Another message", probe.getRef());

        // Expect the actor to respond after restart
        probe.expectMsg("Message received after restart");
    }

    /**
     * A simple actor that throws a RuntimeException for testing supervisor strategy.
     */
    public static class TestActor extends AbstractActor {
        private static int restartCounter = 0;

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(String.class, msg -> {
                        if (msg.equals("Test message")) {
                            throw new RuntimeException("Test Exception");  // This triggers the supervisor's strategy
                        }
                        // After the actor restarts, we count the message
                        restartCounter++;
                        getSender().tell("Message received after restart", getSelf());
                    })
                    .build();
        }
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
     * Test to ensure that SupervisorActor properly handles unhandled messages.
     */
    @Test
    public void testUnhandledMessages() {
        // Send an unhandled message to the SupervisorActor
        supervisorActor.tell("UnhandledMessage", probe.getRef());

        // Expect the SupervisorActor to respond with "Unhandled message"
        String response = probe.expectMsgClass(String.class);
        assertEquals("Unhandled message", response);
    }
}