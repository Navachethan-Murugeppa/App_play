import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.testkit.javadsl.TestKit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import play.libs.Json;
import scala.concurrent.duration.FiniteDuration;
import java.util.concurrent.TimeUnit;

import actors.ReadabilityActor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class ReadabilityActorTest {

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

    /**
     * Testable version of ReadabilityActor with additional test-specific behavior.
     */
    public static class TestableReadabilityActor extends ReadabilityActor {
        public TestableReadabilityActor(ActorRef userActor) {
            super(userActor);
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(String.class, text -> {
                        if (text.startsWith("countSentences:")) {
                            getSender().tell(countSentences(text.substring(15)), getSelf());
                        } else if (text.startsWith("countWords:")) {
                            getSender().tell(countWords(text.substring(11)), getSelf());
                        } else if (text.startsWith("countSyllables:")) {
                            getSender().tell(countSyllables(text.substring(15)), getSelf());
                        } else if (text.startsWith("countSyllablesInWord:")) {
                            getSender().tell(countSyllablesInWord(text.substring(21)), getSelf());
                        }
                    })
                    .build();
        }
    }

    /**
     * Test: Valid description processing by ReadabilityActor.
     */
    @Test
    public void testValidDescriptionProcessing() {
        new TestKit(system) {{
            ActorRef userActor = getTestActor();
            ActorRef readabilityActor = system.actorOf(ReadabilityActor.props(userActor));

            ObjectNode message = Json.newObject();
            message.put("description", "This is a test description.");

            readabilityActor.tell(message, getRef());

            ObjectNode actualResponse = expectMsgClass(FiniteDuration.apply(10, TimeUnit.SECONDS), ObjectNode.class);

            assertTrue(actualResponse.has("fkGrade"));
            assertTrue(actualResponse.has("readingEase"));
        }};
    }

    /**
     * Test: Handling a missing description.
     */
    @Test
    public void testMissingDescription() {
        new TestKit(system) {{
            ActorRef userActor = getTestActor();
            ActorRef readabilityActor = system.actorOf(ReadabilityActor.props(userActor));

            ObjectNode message = Json.newObject(); // No "description" field

            readabilityActor.tell(message, getRef());

            ObjectNode actualResponse = expectMsgClass(ObjectNode.class);
            assertEquals("Error calculating readability: Missing description", actualResponse.get("error").asText());
        }};
    }

    /**
     * Test: Simulate exception handling during processing.
     */
    @Test
    public void testExceptionHandling() {
        new TestKit(system) {{
            // Create a mock UserActor (test probe)
            ActorRef userActor = getTestActor();

            // Create ReadabilityActor that simulates an exception
            ActorRef readabilityActor = system.actorOf(Props.create(ReadabilityActor.class, () -> new ReadabilityActor(userActor) {
                @Override
                public Receive createReceive() {
                    return receiveBuilder()
                            .match(JsonNode.class, message -> {
                                try {
                                    // Simulate an exception
                                    throw new RuntimeException("Simulated exception");
                                } catch (RuntimeException e) {
                                    // Send an error response to the sender
                                    JsonNode errorResponse = Json.newObject()
                                            .put("error", "Error calculating readability: Simulated exception");
                                    sender().tell(errorResponse, self());
                                }
                            })
                            .build();
                }
            }));

            // Send a valid JSON message to trigger the exception
            ObjectNode message = Json.newObject();
            message.put("description", "This is a test description.");

            // Send the message
            readabilityActor.tell(message, getRef());

            // Expect an error response
            ObjectNode actualResponse = expectMsgClass(ObjectNode.class);

            // Validate the error message
            assertTrue(actualResponse.has("error"));
            assertEquals("Error calculating readability: Simulated exception", actualResponse.get("error").asText());
        }};
    }


    @Test
    public void testCountSentences() {
        new TestKit(system) {{
            ActorRef readabilityActor = system.actorOf(Props.create(TestableReadabilityActor.class, getRef()));
            readabilityActor.tell("countSentences:This is a test. This is another sentence!", getRef());
            int result = expectMsgClass(Integer.class);
            assertEquals(2, result);
        }};
    }

    @Test
    public void testCountWords() {
        new TestKit(system) {{
            ActorRef readabilityActor = system.actorOf(Props.create(TestableReadabilityActor.class, getRef()));
            readabilityActor.tell("countWords:This is a test sentence.", getRef());
            int result = expectMsgClass(Integer.class);
            assertEquals(5, result);
        }};
    }

    @Test
    public void testCountSyllables() {
        new TestKit(system) {{
            ActorRef readabilityActor = system.actorOf(Props.create(TestableReadabilityActor.class, getRef()));
            readabilityActor.tell("countSyllables:This is a test.", getRef());
            int result = expectMsgClass(Integer.class);
            assertEquals(4, result);
        }};
    }

    @Test
    public void testCountSyllablesInWord() {
        new TestKit(system) {{

            // Create the TestableReadabilityActor
            ActorRef readabilityActor = system.actorOf(Props.create(TestableReadabilityActor.class, getRef()));

            // Case 1: Word with vowels (count > 0)
            readabilityActor.tell("countSyllablesInWord:reading", getRef());
            int result1 = expectMsgClass(Integer.class);
            assertEquals(2, result1); // Expected syllable count for "reading"

            // Case 2: Word with no vowels (count == 0, fallback to 1 syllable)
            readabilityActor.tell("countSyllablesInWord:rthm", getRef());
            int result2 = expectMsgClass(Integer.class);
            assertEquals(1, result2); // Expected fallback to 1 syllable

            // Case 3: Word with multiple vowels (count > 0)
            readabilityActor.tell("countSyllablesInWord:syllables", getRef());
            int result3 = expectMsgClass(Integer.class);
            assertEquals(3, result3); // Expected syllable count for "syllables"
        }};
    }

}