package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import com.fasterxml.jackson.databind.JsonNode;
import play.libs.Json;

import java.util.concurrent.CompletableFuture;

/**
 * Actor responsible for calculating readability scores for a given description.
 * It computes the Flesch-Kincaid Grade Level and Flesch Reading Ease Score.
 *
 * <p>Author: Priyadarshine Kumar 40293041</p>
 */
public class ReadabilityActor extends AbstractActor {

    public final ActorRef userActor; // Reference to the UserActor

    /**
     * Constructor that accepts an ActorRef to the UserActor.
     *
     * @param userActor The ActorRef to the UserActor
     */
    public ReadabilityActor(ActorRef userActor) {
        this.userActor = userActor;
    }

    /**
     * Creates a Props instance for the ReadabilityActor, used for creating the actor.
     *
     * @param userActor The ActorRef to the UserActor
     * @return The Props instance for the ReadabilityActor
     */
    public static Props props(ActorRef userActor) {
        return Props.create(ReadabilityActor.class, () -> new ReadabilityActor(userActor));
    }

    /**
     * Defines the behavior of the ReadabilityActor. It listens for messages of type JsonNode.
     * If the message contains a "description", it processes it to calculate readability scores.
     *
     * @return The receive builder defining the actor's behavior
     */
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(JsonNode.class, message -> {
                    if (message.has("description")) {
                        String description = message.get("description").asText();
                        processDescription(description);
                    } else {
                        // Send error message if "description" is missing
                        userActor.tell(Json.newObject().put("error", "Error calculating readability: Missing description"), self());
                    }
                })
                .build();
    }


    /**
     * Processes the given description asynchronously to calculate readability scores.
     * The results are sent back to the sender (UserActor).
     *
     * @param description The description text to process
     */
    public void processDescription(String description) {
        CompletableFuture.supplyAsync(() -> calculateReadabilityScores(description))
                .thenAccept(scores -> {
                    JsonNode result = Json.newObject()
                            .put("fkGrade", scores[0])
                            .put("readingEase", scores[1]);
                    userActor.tell(result, self());
                })
                .exceptionally(e -> {
                    // Handle exception and send an error response
                    JsonNode errorResponse = Json.newObject()
                            .put("error", "Error calculating readability: " + e.getMessage());
                    userActor.tell(errorResponse, self());
                    return null;
                });
    }


    /**
     * Calculates the Flesch-Kincaid Grade Level and Flesch Reading Ease score for the given text.
     *
     * @param text The text to analyze
     * @return An array containing the Flesch-Kincaid Grade Level and Flesch Reading Ease Score
     */
    public double[] calculateReadabilityScores(String text) {
        int sentenceCount = countSentences(text);
        int wordCount = countWords(text);
        int syllableCount = countSyllables(text);

        double fkGrade = 0.39 * (wordCount / sentenceCount) + 11.8 * (syllableCount / wordCount) - 15.59;
        double readingEase = 206.835 - (1.015 * (wordCount / sentenceCount)) - (84.6 * (syllableCount / wordCount));

        return new double[]{fkGrade, readingEase};
    }

    /**
     * Counts the number of sentences in the given text by splitting on sentence-ending punctuation.
     *
     * @param text The text to analyze
     * @return The number of sentences
     */
    public int countSentences(String text) {
        // Split text by sentence-ending punctuation marks
        return text.split("[.!?]").length;
    }

    /**
     * Counts the number of words in the given text by splitting on spaces.
     *
     * @param text The text to analyze
     * @return The number of words
     */
    public int countWords(String text) {
        // Split text by spaces to count words
        return text.split("\\s+").length;
    }

    /**
     * Counts the number of syllables in the given text by counting syllables in each word.
     * A basic vowel-counting method is used for estimation.
     *
     * @param text The text to analyze
     * @return The total number of syllables
     */
    public int countSyllables(String text) {
        int syllables = 0;
        for (String word : text.split("\\s+")) {
            syllables += countSyllablesInWord(word);
        }
        return syllables;
    }

    /**
     * Counts the syllables in a word by detecting vowels.
     *
     * @param word The word to analyze
     * @return The number of syllables in the word
     */
    public int countSyllablesInWord(String word) {
        // Simple vowel counting to estimate syllables
        word = word.toLowerCase();
        int count = 0;
        boolean isVowel = false;
        for (char c : word.toCharArray()) {
            if (isVowelChar(c)) {
                if (!isVowel) {
                    count++;
                    isVowel = true;
                }
            } else {
                isVowel = false;
            }
        }
        if (count > 0) {
            return count;
        }else{
            return 1;
        }
    }

    /**
     * Checks if the character is a vowel (a, e, i, o, u, y).
     *
     * @param c The character to check
     * @return True if the character is a vowel, false otherwise
     */
    public boolean isVowelChar(char c) {
        return "aeiouy".indexOf(c) != -1;
    }
}
