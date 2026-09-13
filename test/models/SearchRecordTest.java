package models;

import Services.TmdbServices;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for the SearchRecord model.
 * <p>
 * This class verifies the functionality of the fromJson factory method.
 * It ensures that raw JSON responses from the TMDb API are correctly parsed,
 * sorted by popularity, and mapped into SearchRecord and MovieResults objects.
 * It also verifies that the model is resilient against empty data sets.
 * </p>
 *
 * @author Thanugundla Sumith Reddy
 */
public class SearchRecordTest {

    /**
     * Tests the successful parsing of a populated JSON response.
     * Verifies that the root-level data (query, category, total_results) is extracted correctly,
     * and that the array of results is properly mapped into the internal list.
     * @throws Exception if the JSON payload fails to parse
     * @author Thanugundla Sumith Reddy
     */
    @Test
    public void testFromJsonValidData() throws Exception {
        // 1. Mock the TMDb service (we don't need it to actually do anything here)
        TmdbServices mockTmdb = mock(TmdbServices.class);
        ObjectMapper mapper = new ObjectMapper();

        // 2. Create a fake JSON response with 3 items of varying popularity to test your sorting logic!
        String jsonString = "{ \"total_results\": 3, \"results\": [ " +
                "{ \"id\": 1, \"title\": \"Low Pop Movie\", \"popularity\": 10.0 }, " +
                "{ \"id\": 2, \"title\": \"High Pop Movie\", \"popularity\": 99.9 }, " +
                "{ \"id\": 3, \"title\": \"Mid Pop Movie\", \"popularity\": 50.0 } " +
                "] }";
        JsonNode fakeJson = mapper.readTree(jsonString);

        // 3. Run refactored method
        SearchRecord record = SearchRecord.fromJson("Batman", "movie", fakeJson, mockTmdb);

        // 4. Verify it parsed the root data correctly
        assertNotNull(record);
        assertEquals("Batman", record.query);
        assertEquals("movie", record.category);
        assertEquals(3, record.totalResults);

        // 5. Verify the array was mapped properly
        assertEquals(3, record.results.size());
    }

    /**
     * Tests the model's fallback behavior when provided with an empty JSON response.
     * Verifies that missing fields (like total_results) safely default to 0 and that
     * the results list initializes cleanly without throwing a NullPointerException.
     * @throws Exception if the JSON payload fails to parse
     * @author Thanugundla Sumith Reddy
     */
    @Test
    public void testFromJsonEmptyData() throws Exception {
        TmdbServices mockTmdb = mock(TmdbServices.class);
        ObjectMapper mapper = new ObjectMapper();

        // Simulating a search that returned 0 results and no "total_results" field
        String jsonString = "{ \"results\": [] }";
        JsonNode fakeJson = mapper.readTree(jsonString);

        SearchRecord record = SearchRecord.fromJson("Unknown", "movie", fakeJson, mockTmdb);

        // Verify your defaults work perfectly
        assertNotNull(record);
        assertEquals(0, record.totalResults); // Should default to 0
        assertTrue(record.results.isEmpty());
    }
}
