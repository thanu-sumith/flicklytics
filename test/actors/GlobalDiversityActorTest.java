package actors;

import Services.TmdbServices;
import models.GlobalDiversity;
import org.apache.pekko.actor.testkit.typed.javadsl.TestKitJunitResource;
import org.apache.pekko.actor.testkit.typed.javadsl.TestProbe;
import org.apache.pekko.actor.typed.ActorRef;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.mockito.Mockito.*;

/**
 * JUnit test class for {@link GlobalDiversityActor}.
 * 
 * This test suite ensures:
 * - Correct handling of successful TMDb responses
 * - Proper message passing using Pekko TestKit
 * - Correct behavior on API failure (exception path)
 * 
 * Dependency Injection is achieved using Mockito for TmdbServices.
 * 
 * @author Aman Agnihotri
 */
public class GlobalDiversityActorTest {

    @ClassRule
    public static final TestKitJunitResource testKit = new TestKitJunitResource();

    /**
     * Test successful flow:
     * Actor receives request → calls TMDb → returns diversity response
     */
    @Test
    public void testGetDiversityStatsSuccess() {
        // Arrange
        TmdbServices mockService = Mockito.mock(TmdbServices.class);
        GlobalDiversity mockDiversity = Mockito.mock(GlobalDiversity.class);

        CompletionStage<GlobalDiversity> future = CompletableFuture.completedFuture(mockDiversity);

        when(mockService.getGlobalDiversity(1)).thenReturn(future);

        ActorRef<GlobalDiversityActor.Command> actor = testKit.spawn(GlobalDiversityActor.create(mockService));

        TestProbe<GlobalDiversityActor.DiversityStatsResponse> probe = testKit.createTestProbe();

        // Act
        actor.tell(new GlobalDiversityActor.GetDiversityStats(1, probe.getRef()));

        // Assert
        GlobalDiversityActor.DiversityStatsResponse response = probe.receiveMessage();

        assert response.diversity == mockDiversity;

        verify(mockService, times(1)).getGlobalDiversity(1);
    }

    /**
     * Test failure flow:
     * If TMDb service fails, actor should throw exception (restart strategy)
     */
    @Test
    public void testGetDiversityStatsFailure() {
        // Arrange
        TmdbServices mockService = Mockito.mock(TmdbServices.class);

        CompletableFuture<GlobalDiversity> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("API Failure"));

        when(mockService.getGlobalDiversity(2)).thenReturn(failedFuture);

        ActorRef<GlobalDiversityActor.Command> actor = testKit.spawn(GlobalDiversityActor.create(mockService));

        TestProbe<GlobalDiversityActor.DiversityStatsResponse> probe = testKit.createTestProbe();

        // Act
        actor.tell(new GlobalDiversityActor.GetDiversityStats(2, probe.getRef()));

        // Assert
        // No message should be received due to failure
        probe.expectNoMessage();

        verify(mockService, times(1)).getGlobalDiversity(2);
    }
}