package actors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestProbe;
import org.apache.pekko.actor.typed.ActorRef;
import org.junit.AfterClass;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import Services.TmdbServices;

import java.util.concurrent.CompletableFuture;

/**
 * Unit test for the FinancialActor using Pekko TestKit.
 * @author Thanugundla Sumith Reddy
 */
public class FinancialActorTest {

    private static final ActorTestKit testKit = ActorTestKit.create();

    /**
     * Cleans up the ActorTestKit after all tests are executed.
     * Ensures proper shutdown of actor system resources.
     * @author Thanugundla Sumith Reddy
     */
    @AfterClass
    public static void cleanup() {
        testKit.shutdownTestKit();
    }

    /**
     * Tests the GetFinancialStats message handling of FinancialActor.
     * <p>
     * This test:
     * <ul>
     *     <li>Mocks TMDb service responses</li>
     *     <li>Simulates search and details API calls</li>
     *     <li>Verifies that the actor returns a valid response</li>
     * </ul>
     *
     * @author Thanugundla Sumith Reddy
     */
    @Test
    public void testGetFinancialStats() {

        TmdbServices mockTmdbService = Mockito.mock(TmdbServices.class);
        ObjectMapper mapper = new ObjectMapper();

        // 1. Fake JSON for the initial Search API call
        ObjectNode fakeSearchJson = mapper.createObjectNode();
        ArrayNode results = mapper.createArrayNode();
        ObjectNode movie = mapper.createObjectNode();
        movie.put("id", 123);
        movie.put("title", "Batman");
        results.add(movie);
        fakeSearchJson.set("results", results);

        // 2. Fake JSON for the Details API call
        ObjectNode fakeDetailsJson = mapper.createObjectNode();
        fakeDetailsJson.put("title", "Batman");
        fakeDetailsJson.put("budget", 150000000);
        fakeDetailsJson.put("revenue", 770000000);

        // 3. Setup the mocks so FinancialStat doesn't crash
        Mockito.when(mockTmdbService.search(eq("movie"), anyString()))
                .thenReturn(CompletableFuture.completedFuture(fakeSearchJson));
        Mockito.when(mockTmdbService.getDetails(eq("movie"), anyString()))
                .thenReturn(CompletableFuture.completedFuture(fakeDetailsJson));

        // 4. Spawn Actor and Probe
        ActorRef<FinancialActor.Command> financialActor =
                testKit.spawn(FinancialActor.create(mockTmdbService));
        TestProbe<FinancialActor.FinancialStatsResponse> probe =
                testKit.createTestProbe();

        // 5. Send message
        financialActor.tell(
                new FinancialActor.GetFinancialStats("Batman", probe.getRef())
        );

        // 6. Verify response
        FinancialActor.FinancialStatsResponse response = probe.receiveMessage();

        assertNotNull("Response should not be null", response);
        assertEquals("Should have calculated stats for 1 movie", 1, response.stats.size());
    }
}