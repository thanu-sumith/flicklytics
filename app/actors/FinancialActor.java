package actors;

import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.SupervisorStrategy;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;

import models.FinancialStat;
import Services.TmdbServices;
import actors.ManagerActor.WrappedFinancialResponse;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * Actor responsible for managing and fetching financial performance data for
 * movies.
 * <p>
 * This actor acts as a bridge between the asynchronous TMDb service and the
 * Pekko Actor System.
 * It utilizes a supervisor strategy to ensure resilience by restarting on
 * failures.
 * </p>
 * * @author Thanugundla Sumith Reddy
 * 
 * @version 1.0
 */
public class FinancialActor extends AbstractBehavior<FinancialActor.Command> {

    // =========================================================================
    // 1. PROTOCOL (The messages this actor can understand)
    // =========================================================================

    /**
     * Base interface for all messages that the FinancialActor can process.
     * * @author Thanugundla Sumith Reddy
     */
    public interface Command {
    }

    /**
     * Message class representing a request to fetch financial statistics for a
     * specific query.
     * * @author Thanugundla Sumith Reddy
     */
    public static final class GetFinancialStats implements Command {
        public final String query;
        public final ActorRef<FinancialStatsResponse> replyTo;

        /**
         * Constructs a new GetFinancialStats message.
         * * @param query The search query for which to fetch financial stats.
         * 
         * @param replyTo The actor reference where the response should be sent.
         * @author Thanugundla Sumith Reddy
         */
        public GetFinancialStats(String query, ActorRef<FinancialStatsResponse> replyTo) {
            this.query = query;
            this.replyTo = replyTo;
        }
    }

    /**
     * Message class representing the final response containing a list of financial
     * statistics.
     * * @author Thanugundla Sumith Reddy
     */
    public static final class FinancialStatsResponse {
        public final List<FinancialStat> stats;

        /**
         * Constructs the financial response message.
         * * @param stats The list of financial statistics results.
         * 
         * @author Thanugundla Sumith Reddy
         */
        public FinancialStatsResponse(List<FinancialStat> stats) {
            this.stats = stats;
        }
    }

    /**
     * Internal wrapper message used to safely return asynchronous data into the
     * actor's thread context.
     * * @author Thanugundla Sumith Reddy
     */
    private static final class WrappedFinancialResponse implements Command {
        final List<FinancialStat> stats;
        final ActorRef<FinancialStatsResponse> replyTo;

        /**
         * Constructs the wrapped response.
         * * @param stats The list of statistics fetched.
         * 
         * @param replyTo The original requester.
         * @author Thanugundla Sumith Reddy
         */
        WrappedFinancialResponse(List<FinancialStat> stats, ActorRef<FinancialStatsResponse> replyTo) {
            this.stats = stats;
            this.replyTo = replyTo;
        }
    }

    // =========================================================================
    // 2. ACTOR STATE & SETUP
    // =========================================================================

    private final TmdbServices tmdbService;

    /**
     * Private constructor for the FinancialActor.
     * * @param context The actor context.
     * 
     * @param tmdbService The service used to interact with the TMDb API.
     * @author Thanugundla Sumith Reddy
     */
    private FinancialActor(ActorContext<Command> context, TmdbServices tmdbService) {
        super(context);
        this.tmdbService = tmdbService;
    }

    /**
     * Factory method to create a new Behavior for this actor.
     * Implements a Supervision Strategy that restarts the actor upon encountering
     * an Exception.
     * * @param tmdbService The TMDb service dependency.
     * 
     * @return A Behavior configured with setup and supervision.
     * @author Thanugundla Sumith Reddy
     */
    public static Behavior<Command> create(TmdbServices tmdbService) {
        return Behaviors.supervise(
                Behaviors.setup((ActorContext<Command> context) -> new FinancialActor(context, tmdbService)))
                .onFailure(Exception.class, SupervisorStrategy.restart());
    }

    // =========================================================================
    // 3. MESSAGE HANDLING LOGIC
    // =========================================================================

    /**
     * Configures how the actor responds to different message types.
     * * @return A Receive object defining message handlers.
     * 
     * @author Thanugundla Sumith Reddy
     */
    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(GetFinancialStats.class, this::onGetFinancialStats)
                .onMessage(WrappedFinancialResponse.class, this::onWrappedResponse)
                .build();
    }

    /**
     * Handles the initial request for financial statistics. Triggers an
     * asynchronous fetch
     * and pipes the result back to the actor as a WrappedFinancialResponse.
     * * @param command The GetFinancialStats command containing the query details.
     * 
     * @return The updated behavior (this).
     * @author Thanugundla Sumith Reddy
     */
    private Behavior<Command> onGetFinancialStats(GetFinancialStats command) {
        // 1. Trigger the asynchronous logic you already wrote in your model
        CompletionStage<List<FinancialStat>> futureStats = FinancialStat.fetchFinancials(command.query, tmdbService);

        // 2. Pipe the result of that Future back to this actor safely
        getContext().pipeToSelf(futureStats, (stats, failure) -> {
            if (failure != null) {
                // If the TMDb API crashes, throw an exception so the Supervisor catches it and
                // restarts the actor
                throw new RuntimeException("TMDb API Failure during Financial Stats fetch", failure);
            }
            return new WrappedFinancialResponse(stats, command.replyTo);
        });

        return this;
    }

    /**
     * Handles the internal wrapped message once the asynchronous call completes.
     * Sends the final results to the original requester.
     * * @param wrapped The internal message containing stats and the return
     * address.
     * 
     * @return The updated behavior (this).
     * @author Thanugundla Sumith Reddy
     */
    private Behavior<Command> onWrappedResponse(WrappedFinancialResponse wrapped) {
        // 3. Send the final compiled list back to whoever requested it (the Controller
        // or UserActor)
        wrapped.replyTo.tell(new FinancialStatsResponse(wrapped.stats));
        return this;
    }
}