package actors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import play.libs.Json;

import static org.junit.Assert.assertEquals;

/**
 * Test class for the SentimentActor.
 * This class tests the sentiment analysis functionality for various cases of input.
 *
 * <p>Author: Lokesh Kommalapati (40301947)</p>
 */
public class SentimentActorTest {

    private static ActorSystem system;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create("SentimentActorTestSystem");
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    /**
     * Test case for a predominantly happy sentiment input.
     */
    @Test
    public void testHappySentiment() {
        new TestKit(system) {{
            ActorRef sentimentActor = system.actorOf(SentimentActor.props());

            JsonNode input = Json.newArray()
                    .add("This is a fantastic day! I'm so happy and excited!")
                    .add("Amazing experience with lots of joy and smiles!");
            sentimentActor.tell(input, getRef());

            JsonNode response = expectMsgClass(JsonNode.class);
            assertEquals(":-)", response.get("finalSentiment").asText());
        }};
    }

    /**
     * Test case for a predominantly sad sentiment input.
     */
    @Test
    public void testSadSentiment() {
        new TestKit(system) {{
            ActorRef sentimentActor = system.actorOf(SentimentActor.props());

            JsonNode input = Json.newArray()
                    .add("This is such a terrible day, I'm feeling so down and lonely.")
                    .add("The experience was awful, full of grief and sorrow.");
            sentimentActor.tell(input, getRef());

            JsonNode response = expectMsgClass(JsonNode.class);
            assertEquals(":-(", response.get("finalSentiment").asText());
        }};
    }

    /**
     * Test case for neutral sentiment input.
     */
    @Test
    public void testNeutralSentiment() {
        new TestKit(system) {{
            ActorRef sentimentActor = system.actorOf(SentimentActor.props());

            JsonNode input = Json.newArray()
                    .add("This is a normal day, nothing special.")
                    .add("Just an average experience, not too bad or good.");
            sentimentActor.tell(input, getRef());

            JsonNode response = expectMsgClass(JsonNode.class);
            assertEquals(":-|", response.get("finalSentiment").asText());
        }};
    }

    /**
     * Test case for mixed sentiment input.
     */
    @Test
    public void testMixedSentiment() {
        new TestKit(system) {{
            ActorRef sentimentActor = system.actorOf(SentimentActor.props());

            JsonNode input = Json.newArray()
                    .add("This is a fantastic day, but it ended on a bad note.")
                    .add("I was so happy earlier, but now I'm feeling down.");
            sentimentActor.tell(input, getRef());

            JsonNode response = expectMsgClass(JsonNode.class);
            assertEquals(":-|", response.get("finalSentiment").asText());
        }};
    }
}
