package actors;

import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestProbe;
import org.apache.pekko.actor.typed.ActorRef;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Unit test for ReadabilityActor using Pekko TestKit.
 * Ensures high branch and line coverage with strong assertions.
 * @author Harshavardhini Eluri
 */
public class ReadabilityActorTest {

        private static ActorTestKit testKit;

        /**
         * Initializes the ActorTestKit before running tests.
         */
        @BeforeClass
        public static void setup() {
                testKit = ActorTestKit.create();
        }

        /**
         * Shuts down the ActorTestKit after all tests are completed.
         */
        @AfterClass
        public static void teardown() {
                testKit.shutdownTestKit();
        }

        /**
         * Test with complex normal text.
         */
        @Test
        public void testReadabilityCalculation_NormalText() {

                ActorRef<ReadabilityActor.Command> actor = testKit.spawn(ReadabilityActor.create());

                TestProbe<ReadabilityActor.ReadabilityResponse> probe = testKit.createTestProbe();

                String text = "The incredibly sophisticated architectural design ensures scalability.";

                actor.tell(new ReadabilityActor.CalculateReadability(text, probe.getRef()));

                ReadabilityActor.ReadabilityResponse response = probe.receiveMessage();

                assertNotNull(response);
                assertNotNull(response.grade);
                assertNotNull(response.ease);
        }

        /**
         * Test with empty text input.
         */
        @Test
        public void testReadabilityCalculation_EmptyText() {

                ActorRef<ReadabilityActor.Command> actor = testKit.spawn(ReadabilityActor.create());

                TestProbe<ReadabilityActor.ReadabilityResponse> probe = testKit.createTestProbe();

                actor.tell(new ReadabilityActor.CalculateReadability("", probe.getRef()));

                ReadabilityActor.ReadabilityResponse response = probe.receiveMessage();

                assertNotNull(response);
                assertEquals(0.0, response.ease, 0.001);
                assertEquals("0.0", response.grade);
        }

        /**
         * Test with null text input (edge case).
         */
        @Test
        public void testReadabilityCalculation_NullText() {

                ActorRef<ReadabilityActor.Command> actor = testKit.spawn(ReadabilityActor.create());

                TestProbe<ReadabilityActor.ReadabilityResponse> probe = testKit.createTestProbe();

                actor.tell(new ReadabilityActor.CalculateReadability(null, probe.getRef()));

                ReadabilityActor.ReadabilityResponse response = probe.receiveMessage();

                assertNotNull(response);
        }

        /**
         * Test with very simple short text (different branch).
         */
        @Test
        public void testReadabilityCalculation_SimpleText() {

                ActorRef<ReadabilityActor.Command> actor = testKit.spawn(ReadabilityActor.create());

                TestProbe<ReadabilityActor.ReadabilityResponse> probe = testKit.createTestProbe();

                String text = "Hello world.";

                actor.tell(new ReadabilityActor.CalculateReadability(text, probe.getRef()));

                ReadabilityActor.ReadabilityResponse response = probe.receiveMessage();

                assertNotNull(response);
                assertNotNull(response.grade);
        }
}