package actors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestProbe;
import org.apache.pekko.actor.typed.ActorRef;
import org.junit.AfterClass;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;

import Services.TmdbServices;

import java.util.concurrent.CompletableFuture;

/**
 * Unit test for PersonStatsSupervisor using Pekko TestKit.
 *
 * @author Varshain Gopichandar Sreedevi
 */
public class PersonStatsSupervisorTest {

    private static final ActorTestKit testKit = ActorTestKit.create();

    /**
     * Cleans up the ActorTestKit after all tests are executed.
     * Ensures proper shutdown of actor system resources.
     *
     * @author Varshain Gopichandar Sreedevi
     */
    @AfterClass
    public static void cleanup() {
        testKit.shutdownTestKit();
    }

    /**
     * Tests the creation of PersonStatsSupervisor and verifies that it correctly
     * forwards messages to its child PersonStatsActor.
     * <p>
     * This test:
     * <ul>
     *     <li>Mocks TMDb service response</li>
     *     <li>Spawns the supervisor actor</li>
     *     <li>Sends a FetchPersonStats message</li>
     *     <li>Verifies that the response is received through the child actor</li>
     * </ul>
     *
     * @author Varshain Gopichandar Sreedevi
     */
    @Test
    public void testSupervisorCreationAndForwarding() {

        TmdbServices mockTmdbService = Mockito.mock(TmdbServices.class);
        ObjectMapper mapper = new ObjectMapper();

        // 1. Create a fake JSON response
        ObjectNode fakeJson = mapper.createObjectNode();
        fakeJson.put("name", "Brad Pitt");

        ObjectNode combinedCredits = mapper.createObjectNode();
        combinedCredits.set("cast", mapper.createArrayNode());
        fakeJson.set("combined_credits", combinedCredits);

        Mockito.when(mockTmdbService.getDetails(eq("person"), eq("123")))
                .thenReturn(CompletableFuture.completedFuture(fakeJson));

        // 2. Spawn supervisor actor
        ActorRef<PersonStatsActor.Command> supervisor =
                testKit.spawn(PersonStatsSupervisor.create(mockTmdbService));

        // 3. Create probe
        TestProbe<PersonStatsActor.Command> probe =
                testKit.createTestProbe();

        // 4. Send message
        supervisor.tell(
                new PersonStatsActor.FetchPersonStats("123", probe.getRef())
        );

        // 5. Verify response
        PersonStatsActor.PersonStatsFetched response =
                probe.expectMessageClass(PersonStatsActor.PersonStatsFetched.class);

        assertNotNull("Response should not be null", response);
        assertEquals("Brad Pitt", response.summary().getPersonName());
    }
}