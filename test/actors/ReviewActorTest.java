package actors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.pekko.actor.testkit.typed.javadsl.TestKitJunitResource;
import org.apache.pekko.actor.testkit.typed.javadsl.TestProbe;
import org.apache.pekko.actor.typed.ActorRef;
import org.junit.ClassRule;
import org.junit.Test;

import Services.TmdbServices;

/**
 * Unit tests for {@link ReviewActor}.
 *
 * This test class uses Mockito to simulate external dependencies
 * and Pekko TestKit to validate the ReviewActor's behavior.
 * @author Honey Sharma
 */
public class ReviewActorTest {

    /**
     * Shared TestKit resource used to spawn actors and probes.
     */
    @ClassRule
    public static final TestKitJunitResource testKit = new TestKitJunitResource();

    // =========================================================================
    // 1. TEST: SUCCESSFUL REVIEW FETCH + SENTIMENT
    // =========================================================================
    /**
     * Verifies that the ReviewActor executed successfully
     * when the TMDB service delivered valid review data.
     *
     * @throws Exception if JSON parsing fails
     * @author Honey Sharma
     */
    @Test
    public void testReviewActorSuccess() throws Exception {
        TmdbServices mockTmdb = mock(TmdbServices.class);
        String jsonString = "{ \"results\": [" +
                "{\"author\":\"A\",\"content\":\"good amazing excellent\"}," +
                "{\"author\":\"B\",\"content\":\"bad terrible\"}" +
                "]}";
        JsonNode fakeJson = new ObjectMapper().readTree(jsonString);
        when(mockTmdb.getReviews(anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(fakeJson));

        TestProbe<ReviewActor.ReviewResponse> probe = testKit.createTestProbe();
        ActorRef<ReviewActor.Command> actor = testKit.spawn(ReviewActor.create(mockTmdb));
        actor.tell(new ReviewActor.GetReviews("1", "movie", "TestMovie", probe.getRef()));
        ReviewActor.ReviewResponse response = probe.receiveMessage();
        assertNotNull(response);
        assertNotNull(response.payload);
        assertNotNull(response.payload.reviews);
        assertEquals(2, response.payload.reviews.size());
        assertNotNull(response.payload.globalSentiment);
    }

    // =========================================================================
    // 2. TEST: EMPTY REVIEWS
    // =========================================================================
    /**
     * Tests a scenario when the TMDB API provides an empty list of reviews.
     *
     * @throws Exception if JSON parsing fails
     * @author Honey Sharma
     */
    @Test
    public void testEmptyReviews() throws Exception {
        TmdbServices mockTmdb = mock(TmdbServices.class);
        String jsonString = "{ \"results\": [] }";
        JsonNode fakeJson = new ObjectMapper().readTree(jsonString);
        when(mockTmdb.getReviews(anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(fakeJson));

        TestProbe<ReviewActor.ReviewResponse> probe = testKit.createTestProbe();
        ActorRef<ReviewActor.Command> actor = testKit.spawn(ReviewActor.create(mockTmdb));
        actor.tell(new ReviewActor.GetReviews("1", "movie", "TestMovie", probe.getRef()));
        ReviewActor.ReviewResponse response = probe.receiveMessage();
        assertEquals(0, response.payload.reviews.size());
        assertNotNull(response.payload.globalSentiment); // should be "N/A" or neutral
    }

    // =========================================================================
    // 3. TEST: NEGATIVE SENTIMENT
    // =========================================================================
    /**
     * Tests if the actor accurately recognizes negative sentiment.
     *
     * @throws Exception if JSON parsing fails
     * @author Honey Sharma
     */
    @Test
    public void testNegativeSentiment() throws Exception {
        TmdbServices mockTmdb = mock(TmdbServices.class);
        String jsonString = "{ \"results\": [" +
                "{\"author\":\"A\",\"content\":\"bad terrible awful\"}" +
                "]}";
        JsonNode fakeJson = new ObjectMapper().readTree(jsonString);
        when(mockTmdb.getReviews(anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(fakeJson));

        TestProbe<ReviewActor.ReviewResponse> probe = testKit.createTestProbe();
        ActorRef<ReviewActor.Command> actor = testKit.spawn(ReviewActor.create(mockTmdb));
        actor.tell(new ReviewActor.GetReviews("1", "movie", "TestMovie", probe.getRef()));
        ReviewActor.ReviewResponse response = probe.receiveMessage();
        assertEquals(":-(", response.payload.globalSentiment);
    }

    // =========================================================================
    // 4. TEST: FAILURE (EXCEPTION PATH)
    // =========================================================================
    /**
     * Tests actor behavior in the case of TMDB service failure.
     * @author Honey Sharma
     */
    @Test
    public void testApiFailure() {
        TmdbServices mockTmdb = mock(TmdbServices.class);
        CompletableFuture<JsonNode> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("API error"));
        when(mockTmdb.getReviews(anyString(), anyString())).thenReturn(failedFuture);

        ActorRef<ReviewActor.Command> actor = testKit.spawn(ReviewActor.create(mockTmdb));
        TestProbe<ReviewActor.ReviewResponse> probe = testKit.createTestProbe();
        actor.tell(new ReviewActor.GetReviews("1", "movie", "TestMovie", probe.getRef()));
        probe.expectNoMessage();
    }
}