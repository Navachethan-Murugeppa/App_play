package utils;

import play.libs.Json;
import play.mvc.Http;

import java.util.LinkedList;
import java.util.List;

/**
 * SessionManager: Manages the search history within a user's session.
 * It allows adding new queries to the session history, retrieving the search history,
 * and ensuring that the history does not exceed a predefined limit.
 *
 * <p>Author: Priyadarshine Kumar 40293041</p>
 * <p>Author: Saranraj Sivakumar 40306771</p>
 */
public class SessionManager {

    public static final String SESSION_RESULTS_KEY = "searchResults";
    public static final int MAX_SEARCH_HISTORY = 10;

    /**
     * Adds a new query to the session history and prepares updated session data.
     * If the search history exceeds the maximum limit, the oldest entry is removed.
     *
     * @param session Current user session
     * @param query The new search query to add to the history
     * @return Serialized session data as a string
     */
    public String prepareSessionData(Http.Session session, String query) {
        List<String> searchHistory = getSearchHistory(session);

        // Log current session data before modification
        System.out.println("Current session data: " + searchHistory);
        searchHistory.add(0, query);

//        // Ensure the history does not exceed the maximum size
//        if (searchHistory.size() > MAX_SEARCH_HISTORY) {
//            searchHistory.remove(searchHistory.size() - 1);
//        }

        // Serialize the updated search history to a string
        String serializedData = Json.stringify(Json.toJson(searchHistory));

        // Log the updated session data

        // Update the session with the new serialized data
        session = session.adding(SESSION_RESULTS_KEY, serializedData);

        return serializedData;
    }

    /**
     * Retrieves the user's search history from the session.
     * If the session does not contain any history, an empty list is returned.
     *
     * @param session Current user session
     * @return A list of search history queries
     */
    public List<String> getSearchHistory(Http.Session session) {
        // Retrieve the serialized results from the session, or default to an empty list
        String serializedResults = session.getOptional(SESSION_RESULTS_KEY).orElse("[]");
        return Json.fromJson(Json.parse(serializedResults), List.class);
    }
}
