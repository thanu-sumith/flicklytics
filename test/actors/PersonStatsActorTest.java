package actors;

import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestProbe;
import org.apache.pekko.actor.typed.ActorRef;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import Services.TmdbServices;
import models.PersonStatsSummary;

import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;

/**
 * Unit test for PersonStatsActor using Pekko TestKit.
 *
 * @author Varshain Gopichandar Sreedevi
 */
public class PersonStatsActorTest {

    static ActorTestKit testKit;

    /**
     * Initializes the ActorTestKit before running tests.
     *
     * @author Varshain Gopichandar Sreedevi
     */
    @BeforeClass
    public static void setup() {
        testKit = ActorTestKit.create();
    }

    /**
     * Shuts down the ActorTestKit after all tests are completed.
     *
     * @author Varshain Gopichandar Sreedevi
     */
    @AfterClass
    public static void teardown() {
        testKit.shutdownTestKit();
    }

    // =========================================================
    // Helper
    // =========================================================

    /**
     * Builds a fake JSON response representing a person and their movie credits.
     *
     * @param personName name of the person
     * @param movieTitle title of the movie
     * @param popularity popularity score
     * @param voteAvg average vote
     * @param voteCount number of votes
     * @return constructed ObjectNode for testing
     * @author Varshain Gopichandar Sreedevi
     */
    private ObjectNode buildFakePersonJson(String personName, String movieTitle,
                                           double popularity, double voteAvg, long voteCount) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode personJson = mapper.createObjectNode();
        personJson.put("name", personName);

        ObjectNode combinedCredits = mapper.createObjectNode();
        ArrayNode cast = mapper.createArrayNode();

        ObjectNode movie = mapper.createObjectNode();
        movie.put("title", movieTitle);
        movie.put("media_type", "movie");
        movie.put("popularity", popularity);
        movie.put("vote_average", voteAvg);
        movie.put("vote_count", voteCount);
        cast.add(movie);

        combinedCredits.set("cast", cast);
        personJson.set("combined_credits", combinedCredits);
        return personJson;
    }

    // =========================================================
    // Tests
    // =========================================================

    /**
     * Tests successful fetching of person statistics.
     * Verifies correct parsing and response handling.
     *
     * @author Varshain Gopichandar Sreedevi
     */
    @Test
    public void testFetchPersonStatsSuccess() {
        TmdbServices mockTmdb = Mockito.mock(TmdbServices.class);
        ObjectNode fakeJson = buildFakePersonJson("Brad Pitt", "Fight Club", 80.0, 8.8, 1500000L);

        Mockito.when(mockTmdb.getDetails("person", "287"))
                .thenReturn(CompletableFuture.completedFuture(fakeJson));

        ActorRef<PersonStatsActor.Command> actor =
                testKit.spawn(PersonStatsActor.create(mockTmdb));

        TestProbe<PersonStatsActor.Command> probe = testKit.createTestProbe();

        actor.tell(new PersonStatsActor.FetchPersonStats("287", probe.getRef()));

        PersonStatsActor.PersonStatsFetched response =
                probe.expectMessageClass(PersonStatsActor.PersonStatsFetched.class);

        assertEquals("287", response.personId());
        assertEquals("Brad Pitt", response.summary().getPersonName());
    }

    /**
     * Tests failure scenario when TMDb service returns an error.
     * Ensures FetchFailed message is handled correctly.
     *
     * @author Varshain Gopichandar Sreedevi
     */
    @Test
    public void testFetchPersonStatsFailure() {
        TmdbServices mockTmdb = Mockito.mock(TmdbServices.class);

        Mockito.when(mockTmdb.getDetails("person", "999"))
                .thenReturn(CompletableFuture.failedFuture(
                        new RuntimeException("API error")));

        ActorRef<PersonStatsActor.Command> actor =
                testKit.spawn(PersonStatsActor.create(mockTmdb));

        TestProbe<PersonStatsActor.Command> probe = testKit.createTestProbe();

        actor.tell(new PersonStatsActor.FetchPersonStats("999", probe.getRef()));

        PersonStatsActor.FetchFailed response =
                probe.expectMessageClass(PersonStatsActor.FetchFailed.class);

        assertEquals("999", response.personId());
        assertNotNull(response.reason());
    }

    /**
     * Tests behavior when a person has no associated credits.
     * Ensures empty results are handled properly.
     *
     * @author Varshain Gopichandar Sreedevi
     */
    @Test
    public void testFetchPersonStatsEmptyCredits() {
        TmdbServices mockTmdb = Mockito.mock(TmdbServices.class);

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode personJson = mapper.createObjectNode();
        personJson.put("name", "Empty");

        ObjectNode combinedCredits = mapper.createObjectNode();
        combinedCredits.set("cast", mapper.createArrayNode());
        personJson.set("combined_credits", combinedCredits);

        Mockito.when(mockTmdb.getDetails("person", "000"))
                .thenReturn(CompletableFuture.completedFuture(personJson));

        ActorRef<PersonStatsActor.Command> actor =
                testKit.spawn(PersonStatsActor.create(mockTmdb));

        TestProbe<PersonStatsActor.Command> probe = testKit.createTestProbe();

        actor.tell(new PersonStatsActor.FetchPersonStats("000", probe.getRef()));

        PersonStatsActor.PersonStatsFetched response =
                probe.expectMessageClass(PersonStatsActor.PersonStatsFetched.class);

        assertEquals(0, response.summary().getItems().size());
    }

    /**
     * Tests actor stability when receiving a PersonStatsFetched message directly.
     * Ensures actor continues functioning after handling such messages.
     *
     * @throws Exception if async computation fails
     * @author Varshain Gopichandar Sreedevi
     */
    @Test
    public void testActorHandlesFetchedMessage() throws Exception {
        TmdbServices mockTmdb = Mockito.mock(TmdbServices.class);

        ObjectNode fakeJson = buildFakePersonJson("Tom Hanks", "Forrest Gump", 90.0, 8.8, 5000L);

        Mockito.when(mockTmdb.getDetails("person", "31"))
                .thenReturn(CompletableFuture.completedFuture(fakeJson));

        PersonStatsSummary summary =
                PersonStatsSummary.fetch("31", mockTmdb).toCompletableFuture().get();

        ActorRef<PersonStatsActor.Command> actor =
                testKit.spawn(PersonStatsActor.create(mockTmdb));

        actor.tell(new PersonStatsActor.PersonStatsFetched("31", summary));

        TestProbe<PersonStatsActor.Command> probe = testKit.createTestProbe();
        actor.tell(new PersonStatsActor.FetchPersonStats("31", probe.getRef()));
        probe.expectMessageClass(PersonStatsActor.PersonStatsFetched.class);
    }

    /**
     * Tests actor behavior when a failure message is received.
     * Ensures actor remains operational after failure handling.
     *
     * @author Varshain Gopichandar Sreedevi
     */
    @Test
    public void testActorHandlesFailureMessage() {
        TmdbServices mockTmdb = Mockito.mock(TmdbServices.class);

        ActorRef<PersonStatsActor.Command> actor =
                testKit.spawn(PersonStatsActor.create(mockTmdb));

        actor.tell(new PersonStatsActor.FetchFailed("123",
                new RuntimeException("fail")));

        ObjectNode fakeJson = buildFakePersonJson("Actor", "Movie", 50.0, 7.0, 1000L);

        Mockito.when(mockTmdb.getDetails("person", "123"))
                .thenReturn(CompletableFuture.completedFuture(fakeJson));

        TestProbe<PersonStatsActor.Command> probe = testKit.createTestProbe();

        actor.tell(new PersonStatsActor.FetchPersonStats("123", probe.getRef()));
        probe.expectMessageClass(PersonStatsActor.PersonStatsFetched.class);
    }
}