package models;

import Services.TmdbServices;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * Unit tests for the {@link FinancialStat} model.
 * This test suite ensures 100% instruction and branch coverage for financial calculations
 * and asynchronous data fetching logic. It validates that movie budget, revenue, and
 * ROI (Return on Investment) are correctly processed.
 * @author Thanugundla Sumith Reddy
 */
public class FinancialStatTest {

    /**
     * Tests the core mathematical calculations for ROI and Profit.
     * Validates three major equivalence classes:
     * <ul>
     * <li><b>Blockbuster:</b> High ROI (> 200%) results in a "Blockbuster Success" rating.</li>
     * <li><b>Loss:</b> Revenue less than budget results in a negative ROI percentage.</li>
     * <li><b>N/A:</b> A budget of 0 avoids division errors and returns "Data Unavailable".</li>
     * </ul>
     *  * @author Thanugundla Sumith Reddy
     */
    @Test
    public void testMathCalculations() {
        // Test 1: Blockbuster Success
        FinancialStat hit = new FinancialStat("Hit Movie", 100000000L, 1000000000L);
        assertEquals("900.00%", hit.formattedRoi);
        assertEquals("Blockbuster Success", hit.financialRating);

        // Test 2: Financial Loss
        FinancialStat loss = new FinancialStat("Flop Movie", 100L, 50L);
        assertEquals("-50.00%", loss.formattedRoi);

        // Test 3: Data Unavailable (Edge case for 0 budget)
        FinancialStat empty = new FinancialStat("No Data", 0L, 0L);
        assertEquals("N/A", empty.formattedRoi);
        assertEquals("Data Unavailable", empty.financialRating);
    }

    /**
     * Tests the fetchFinancials logic using Mockito to bypass live API calls.
     * Verifies that the service:
     * 1. Correctly searches for a movie.
     * 2. Uses the {@code .limit(1)} stream logic to pick only the primary result.
     * 3. Calls the details API for that specific ID and maps the financial data.
     *  @throws Exception if the CompletableFuture join or get fails.
     *  @author Thanugundla Sumith Reddy
     */
    @Test
    public void testFetchFinancialsLogic() throws Exception {
        // 1. Setup Mockito and ObjectMapper
        TmdbServices mockTmdb = Mockito.mock(TmdbServices.class);
        ObjectMapper mapper = new ObjectMapper();

        // 2. Create a fake Search Result with multiple items
        // Our code .limit(1) should only pick the first one (ID 101)
        String searchJson = "{ \"results\": [ " +
                "{\"id\": 101, \"title\": \"The Batman\"}, " +
                "{\"id\": 102, \"title\": \"Lego Batman\"} " +
                "] }";
        JsonNode searchNode = mapper.readTree(searchJson);

        // 3. Create fake Details for that specific movie
        String detailsJson = "{ \"budget\": 185000000, \"revenue\": 772000000 }";
        JsonNode detailsNode = mapper.readTree(detailsJson);

        // 4. Stub the methods with flexible matchers to avoid Argument Mismatch errors
        Mockito.when(mockTmdb.search(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(searchNode));

        Mockito.when(mockTmdb.getDetails(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(detailsNode));

        // 5. Execute the actual method
        List<FinancialStat> results = FinancialStat.fetchFinancials("The Batman", mockTmdb)
                .toCompletableFuture()
                .get();

        // 6. Verify the outcome
        assertNotNull("Results should not be null", results);
        assertEquals("Should return exactly 1 result due to .limit(1)", 1, results.size());
        assertEquals("The Batman", results.get(0).title);
        assertEquals(185000000L, results.get(0).budget);

        // Profit check: 772M - 185M = 587M
        assertEquals(587000000L, results.get(0).profit);
    }
    /**
     * Tests the branch coverage for "Profitable" status.
     * Partition: ROI is positive (>= 0%) but below the 200% threshold for "Blockbuster".
     * @author Thanugundla Sumith Reddy
     */
    @Test
    public void testProfitableBucketCoverage() {
        // Budget: 100M, Revenue: 150M
        // Profit: 50M -> ROI: 50%
        // This triggers the (roi >= 0.0) branch on line 57
        FinancialStat okayMovie = new FinancialStat("Average Movie", 100000000L, 150000000L);

        assertEquals("50.00%", okayMovie.formattedRoi);
        assertEquals("Profitable", okayMovie.financialRating);
    }
}