package models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the ReviewSentiment model.
 * Verified to work with static sentiment analysis without requiring a TmdbServices instance.
 * @author Honey Sharma
 */
public class ReviewSentimentTest {

    /**
     * Tests the building of review objects from JSON.
     * Uses real sentiment words to trigger 70% happy ratio.
     * @author Honey Sharma
     */
    @Test
    public void testBuildReviews() throws Exception {
        String jsonString = "{ \"results\": [ " +
                "{ \"author\": \"Alice\", \"content\": \"good great amazing\" } " +
                "] }";

        ObjectMapper mapper = new ObjectMapper();
        JsonNode fakeJson = mapper.readTree(jsonString);
        List<ReviewSentiment> reviews = ReviewSentiment.buildReviews(fakeJson, null);

        assertEquals(1, reviews.size());
        assertEquals("Alice", reviews.get(0).author);
        assertEquals(":-)", reviews.get(0).sentiment);
    }

    /**
     * Tests the global sentiment calculator math.
     * @author Honey Sharma
     */
    @Test
    public void testGetGlobalSentiment() {
        List<ReviewSentiment> mockReviews = Arrays.asList(
                new ReviewSentiment("User1", "Great!", ":-)"),
                new ReviewSentiment("User2", "Amazing!", ":-)"),
                new ReviewSentiment("User3", "Bad.", ":-(")
        );
        String result = ReviewSentiment.getGlobalSentiment(mockReviews);
        assertEquals(":-)", result);
    }

    /**
     * Tests fallback for empty lists.
     * @author Honey Sharma
     */
    @Test
    public void testGetGlobalSentimentWithEmptyList() {
        List<ReviewSentiment> emptyReviews = new ArrayList<>();
        String result = ReviewSentiment.getGlobalSentiment(emptyReviews);
        assertEquals("N/A", result);
    }
}