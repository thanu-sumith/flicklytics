package models;

import org.junit.Test;
import java.util.Arrays;
import java.util.ArrayList;
import static org.junit.Assert.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.mockito.Mockito;
import Services.TmdbServices;
import java.util.concurrent.CompletableFuture;

/**
 * Unit tests for the PersonStatsSummary model class.
 * Tests all statistics fields, boundary conditions, and the fetch() factory
 * method.
 * Uses Mockito to mock TmdbServices — never calls the live TMDb API.
 * 
 * @author Varshain Gopichandar Sreedevi
 */
public class PersonStatsSummaryTest {

    /**
     * Helper method to create a standard PersonStatsSummary for reuse across tests.
     * Uses Brad Pitt with two known_for items: Fight Club and Se7en.
     * 
     * @return a pre-populated PersonStatsSummary instance
     * @author Varshain Gopichandar Sreedevi
     */
    private PersonStatsSummary buildSummary() {
        return new PersonStatsSummary(
                "Brad Pitt",
                Arrays.asList(
                        new PersonStat("Fight Club", "movie", 80.0, 8.8, 1500000L),
                        new PersonStat("Se7en", "movie", 60.0, 8.6, 1200000L)),
                60.0, 80.0, 70.0, 2L,
                8.6, 8.8, 8.7,
                1200000L, 1500000L, 1350000.0);
    }

    /**
     * Tests that the person name is stored and retrieved correctly.
     * Partition: normal non-empty name string.
     * 
     * @author Varshain Gopichandar Sreedevi
     */
    @Test
    public void testPersonName() {
        PersonStatsSummary summary = buildSummary();
        assertEquals("Brad Pitt", summary.getPersonName());
    }

    /**
     * Tests that popularity statistics (min, max, avg, count) are stored correctly.
     * Partition: normal numeric popularity values with two items.
     * 
     * @author Varshain Gopichandar Sreedevi
     */
    @Test
    public void testPopularityStats() {
        PersonStatsSummary summary = buildSummary();
        assertEquals(60.0, summary.getPopularityMin(), 0.001);
        assertEquals(80.0, summary.getPopularityMax(), 0.001);
        assertEquals(70.0, summary.getPopularityAvg(), 0.001);
        assertEquals(2L, summary.getPopularityCount());
    }

    /**
     * Tests that vote average statistics (min, max, avg) are stored correctly.
     * Partition: normal vote average values between 0 and 10.
     * 
     * @author Varshain Gopichandar Sreedevi
     */
    @Test
    public void testVoteAvgStats() {
        PersonStatsSummary summary = buildSummary();
        assertEquals(8.6, summary.getVoteAvgMin(), 0.001);
        assertEquals(8.8, summary.getVoteAvgMax(), 0.001);
        assertEquals(8.7, summary.getVoteAvgAvg(), 0.001);
    }

    /**
     * Tests that vote count statistics (min, max, avg) are stored correctly.
     * Partition: large vote count values typical of popular movies.
     * 
     * @author Varshain Gopichandar Sreedevi
     */
    @Test
    public void testVoteCountStats() {
        PersonStatsSummary summary = buildSummary();
        assertEquals(1200000L, summary.getVoteCountMin());
        assertEquals(1500000L, summary.getVoteCountMax());
        assertEquals(1350000.0, summary.getVoteCountAvg(), 0.001);
    }

    /**
     * Tests that the items list is stored correctly and accessible by index.
     * Partition: list with exactly 2 items in correct order.
     * 
     * @author Varshain Gopichandar Sreedevi
     */
    @Test
    public void testItemsList() {
        PersonStatsSummary summary = buildSummary();
        assertEquals(2, summary.getItems().size());
        assertEquals("Fight Club", summary.getItems().get(0).getTitle());
        assertEquals("Se7en", summary.getItems().get(1).getTitle());
    }

    /**
     * Tests PersonStatsSummary correctly handles an empty known_for items list.
     * Partition: boundary — person with no known_for items (count = 0).
     * 
     * @author Varshain Gopichandar Sreedevi
     */
    @Test
    public void testEmptyItemsList() {
        PersonStatsSummary summary = new PersonStatsSummary(
                "Unknown Person", new ArrayList<>(),
                0.0, 0.0, 0.0, 0L,
                0.0, 0.0, 0.0,
                0L, 0L, 0.0);
        assertEquals(0, summary.getItems().size());
        assertEquals(0L, summary.getPopularityCount());
    }

    /**
     * Tests the fetch() factory method builds a correct PersonStatsSummary
     * from a mocked TMDb API response containing one known_for movie item.
     * Partition: person with one known_for movie item — verifies all computed
     * stats.
     * Uses Mockito to avoid calling the live TMDb API.
     * 
     * @throws Exception if the CompletableFuture is interrupted
     * @author Varshain Gopichandar Sreedevi
     */
    @Test
    public void testFetchBuildsCorrectSummary() throws Exception {
        // Build mock person JSON with combined_credits
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode personJson = mapper.createObjectNode();
        personJson.put("name", "Brad Pitt");

        ObjectNode combinedCredits = mapper.createObjectNode();
        ArrayNode cast = mapper.createArrayNode();
        ObjectNode movie = mapper.createObjectNode();
        movie.put("title", "Fight Club");
        movie.put("media_type", "movie");
        movie.put("popularity", 80.0);
        movie.put("vote_average", 8.8);
        movie.put("vote_count", 1500000);
        cast.add(movie);
        combinedCredits.set("cast", cast);
        personJson.set("combined_credits", combinedCredits);

        // Mock TmdbServices — never calls live API
        TmdbServices mockTmdb = Mockito.mock(TmdbServices.class);
        Mockito.when(mockTmdb.getDetails("person", "287"))
                .thenReturn(CompletableFuture.completedFuture(personJson));

        // Call fetch() and verify all computed values
        PersonStatsSummary summary = PersonStatsSummary.fetch("287", mockTmdb)
                .toCompletableFuture().get();

        assertEquals("Brad Pitt", summary.getPersonName());
        assertEquals(1, summary.getItems().size());
        assertEquals("Fight Club", summary.getItems().get(0).getTitle());
        assertEquals(1L, summary.getPopularityCount());
        assertEquals(80.0, summary.getPopularityMin(), 0.001);
        assertEquals(80.0, summary.getPopularityMax(), 0.001);
        assertEquals(80.0, summary.getPopularityAvg(), 0.001);
        assertEquals(8.8, summary.getVoteAvgMin(), 0.001);
        assertEquals(1500000L, summary.getVoteCountMin());
    }
}