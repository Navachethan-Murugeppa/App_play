package actors;

import akka.actor.AbstractActor;
import akka.actor.Props;
import com.fasterxml.jackson.databind.JsonNode;
import play.libs.Json;

import java.util.List;

/**
 * SentimentActor class processes sentiment analysis based on video descriptions received as a JsonNode.
 * It classifies each description as happy, sad, or neutral based on predefined lists of sentiment-related words
 * and returns the final sentiment result after processing all descriptions.
 *
 * <p>Author: Lokesh Kommalapati (40301947)</p>
 */
public class SentimentActor extends AbstractActor {

    // Happy and Sad word lists for sentiment analysis
    private static final List<String> HAPPY_WORDS = List.of(
            "happy", "good", "joy", ":)", "😊", "love", "awesome", "fantastic", "great", "wonderful",
            "amazing", "cheerful", "delighted", "excited", "pleased", "blessed", "smile", "fun",
            "best", "fantabulous", "grateful", "victorious", "content", "elated", "positive", "optimistic",
            "thrilled", "bright", "sunny", "jolly", "merry", "peaceful", "euphoric", "hopeful", "blissful",
            "radiant", "overjoyed", "satisfied"
    );

    private static final List<String> SAD_WORDS = List.of(
            "sad", "bad", "angry", ":(", "☹️", "hate", "terrible", "awful", "depressed", "heartbroken",
            "mourn", "unhappy", "disappointed", "gloomy", "down", "sorrow", "grief", "misery", "angst",
            "pain", "regret", "distressed", "lost", "lonely", "blue", "melancholy", "despair", "downcast",
            "hopeless", "forlorn", "tragic", "hurt", "shattered", "anguish", "tears", "unfortunate",
            "dismal", "isolated", "cold", "disheartened", "empty", "sick"
    );

    /**
     * Factory method to create the SentimentActor.
     * This method returns the Props for creating the SentimentActor, which is used by Akka.
     * No arguments are required for the actor creation.
     *
     * @return The Props to create the SentimentActor instance.
     */
    public static Props props() {
        return Props.create(SentimentActor.class);
    }

    /**
     * Constructor for SentimentActor.
     * This constructor doesn't require any parameters and allows initialization if needed.
     */
    public SentimentActor() {
        // Initialization logic, if needed, goes here.
    }

    /**
     * Defines the behavior of the SentimentActor.
     * It listens for messages of type JsonNode and processes sentiment analysis based on video descriptions.
     *
     * @return The receive block that defines the actor's behavior.
     */
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(JsonNode.class, message -> {
                    // Handle incoming message containing descriptions
                    System.out.println("Received message: " + message);  // Print the received message
                    if (message.isArray()) {
                        String finalSentiment = analyzeSentiment(message);
                        JsonNode response = Json.newObject().put("finalSentiment", finalSentiment);
                        getSender().tell(response, getSelf()); // Send the result back to the sender (UserActor)
                    }
                })
                .build();
    }

    /**
     * Analyze sentiment based on the video descriptions.
     * It calculates sentiment for each description and returns the final sentiment emoji.
     *
     * @param descriptions The list of descriptions to analyze.
     * @return The final sentiment emoji (":-)", ":-(", ":-|").
     */
    private String analyzeSentiment(JsonNode descriptions) {
        int happyCount = 0;
        int sadCount = 0;
        int neutralCount = 0;

        System.out.println("Starting sentiment analysis for " + descriptions.size() + " descriptions");

        for (JsonNode description : descriptions) {
            String desc = description.asText().toLowerCase();
            int happyWordsCount = countSentimentWords(desc, HAPPY_WORDS);
            int sadWordsCount = countSentimentWords(desc, SAD_WORDS);
            int totalWords = happyWordsCount + sadWordsCount;

            // Print the word counts for each description
            System.out.println("Description: \"" + description.asText() + "\"");
            System.out.println("Happy words count: " + happyWordsCount);
            System.out.println("Sad words count: " + sadWordsCount);
            System.out.println("Total sentiment words: " + totalWords);

            // Classify the sentiment of the description
            if (totalWords > 0) {
                double happyPercentage = (double) happyWordsCount / totalWords;
                double sadPercentage = (double) sadWordsCount / totalWords;

                System.out.println("Happy percentage: " + happyPercentage);
                System.out.println("Sad percentage: " + sadPercentage);

                if (happyPercentage > 0.7) {
                    happyCount++;
                    System.out.println("Description classified as HAPPY");
                } else if (sadPercentage > 0.7) {
                    sadCount++;
                    System.out.println("Description classified as SAD");
                } else {
                    neutralCount++;
                    System.out.println("Description classified as NEUTRAL");
                }
            }
        }

        // Print the final sentiment counts
        System.out.println("Happy Count: " + happyCount);
        System.out.println("Sad Count: " + sadCount);
        System.out.println("Neutral Count: " + neutralCount);

        // Determine the final sentiment based on the counts
        if (happyCount >= sadCount && happyCount >= neutralCount) {
            System.out.println("Final Sentiment: :-)");
            return ":-)"; // Happy sentiment
        } else if (sadCount > happyCount && sadCount >= neutralCount) {
            System.out.println("Final Sentiment: :-(");
            return ":-("; // Sad sentiment
        } else {
            System.out.println("Final Sentiment: :-|");
            return ":-|"; // Neutral sentiment
        }
    }

    /**
     * Count the number of sentiment-related words in the description.
     *
     * @param description The description text.
     * @param sentimentWords List of words related to a particular sentiment (happy or sad).
     * @return The number of sentiment words found in the description.
     */
    private int countSentimentWords(String description, List<String> sentimentWords) {
        int count = 0;
        for (String word : sentimentWords) {
            if (description.contains(word)) {
                count++;
            }
        }
        return count;
    }
}
