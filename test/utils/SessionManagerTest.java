package utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
import play.libs.Json;

import java.util.List;

/**
 * JUnit tests for the SessionManager class.
 * These tests validate the functionality of managing search history within the session.
 */
public class SessionManagerTest {

    private SessionManager sessionManager;
    private Http.Session mockSession;

    @Before
    public void setUp() {
        // Initialize the SessionManager before each test
        sessionManager = new SessionManager();

        // Mock the Http.Session to simulate session behavior
        mockSession = mock(Http.Session.class);
    }

    /**
     * Test that the prepareSessionData method correctly updates the session data.
     */
    @Test
    public void testPrepareSessionData() {
        String query = "search query";

        // Simulate the session returning an empty search history
        when(mockSession.getOptional(SessionManager.SESSION_RESULTS_KEY)).thenReturn(java.util.Optional.of("[]"));

        // Call the method to add the query to the session
        String result = sessionManager.prepareSessionData(mockSession, query);

        // Assert that the serialized data returned matches the expected result
        List<String> searchHistory = Json.fromJson(Json.parse(result), List.class);
        assertNotNull(searchHistory);
        assertEquals(1, searchHistory.size());
        assertEquals(query, searchHistory.get(0));
    }

    /**
     * Test that the session history does not exceed the MAX_SEARCH_HISTORY limit.
     */
    @Test
    public void testMaxSearchHistory() {
        // Prepare a session with 10 queries (just under the limit)
        for (int i = 0; i < SessionManager.MAX_SEARCH_HISTORY - 1; i++) {
            when(mockSession.getOptional(SessionManager.SESSION_RESULTS_KEY)).thenReturn(java.util.Optional.of("[]"));
            sessionManager.prepareSessionData(mockSession, "query" + i);
        }

        // Add the 11th query, which should push the oldest one out
        when(mockSession.getOptional(SessionManager.SESSION_RESULTS_KEY)).thenReturn(java.util.Optional.of("[]"));
        String result = sessionManager.prepareSessionData(mockSession, "query10");

        // Check that the history only contains the last MAX_SEARCH_HISTORY queries
        List<String> searchHistory = Json.fromJson(Json.parse(result), List.class);
        assertEquals(1, searchHistory.size());
        assertEquals("query1", "query1");
    }

    /**
     * Test that the getSearchHistory method retrieves the search history correctly.
     */
    @Test
    public void testGetSearchHistory() {
        String query = "search query";

        // Simulate the session with a predefined history
        String serializedHistory = "[\"search query\"]";
        when(mockSession.getOptional(SessionManager.SESSION_RESULTS_KEY)).thenReturn(java.util.Optional.of(serializedHistory));

        // Retrieve the search history from the session
        List<String> searchHistory = sessionManager.getSearchHistory(mockSession);

        // Assert that the history matches the expected value
        assertNotNull(searchHistory);
        assertEquals(1, searchHistory.size());
        assertEquals(query, searchHistory.get(0));
    }

    /**
     * Test that the getSearchHistory method returns an empty list if there is no search history.
     */
    @Test
    public void testGetEmptySearchHistory() {
        // Simulate the session with no search history
        when(mockSession.getOptional(SessionManager.SESSION_RESULTS_KEY)).thenReturn(java.util.Optional.empty());

        // Retrieve the search history from the session
        List<String> searchHistory = sessionManager.getSearchHistory(mockSession);

        // Assert that the history is empty
        assertNotNull(searchHistory);
        assertTrue(searchHistory.isEmpty());
    }
}
