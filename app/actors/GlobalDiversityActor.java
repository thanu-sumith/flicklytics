package actors;

import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.SupervisorStrategy;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;

import models.GlobalDiversity;
import Services.TmdbServices;
import actors.ManagerActor.WrappedDiversityResponse;

import java.util.concurrent.CompletionStage;

/**
 * Worker Actor responsible for calculating Global Diversity and Translations.
 * This actor interacts with external services and manages asynchronous
 * responses
 * within the Pekko Actor System.
 * * @author Aman Agnihotri
 */
public class GlobalDiversityActor extends AbstractBehavior<GlobalDiversityActor.Command> {

    // =========================================================================
    // 1. PROTOCOL
    // =========================================================================
    /**
     * Base interface for all commands handled by the GlobalDiversityActor.
     */
    public interface Command {
    }

    /**
     * Message sent to the actor to request diversity statistics for a specific
     * movie.
     */
    public static final class GetDiversityStats implements Command {
        public final int movieId;
        public final ActorRef<DiversityStatsResponse> replyTo;

        /**
         * Constructs a request message to fetch diversity statistics.
         *
         * @param movieId the ID of the movie to query
         * @param replyTo the actor reference to send the final response to
         */
        public GetDiversityStats(int movieId, ActorRef<DiversityStatsResponse> replyTo) {
            this.movieId = movieId;
            this.replyTo = replyTo;
        }
    }

    /**
     * Constructs a response containing global diversity data.
     *
     * @param diversity the computed diversity result model
     */

    public static final class DiversityStatsResponse {
        public final GlobalDiversity diversity;

        /**
         * Internal message used to wrap asynchronous CompletionStage results
         * to safely bring them back into the actor's single-threaded context.
         */
        public DiversityStatsResponse(GlobalDiversity diversity) {
            this.diversity = diversity;
        }
    }

    /**
     * Internal message used to wrap asynchronous CompletionStage results
     * to safely bring them back into the actor's single-threaded context.
     */
    private static final class WrappedDiversityResponse implements Command {
        public final GlobalDiversity diversity;
        public final ActorRef<DiversityStatsResponse> replyTo;

        /**
         * Internal constructor used to wrap asynchronous results safely.
         *
         * @param diversity the diversity data returned from the service
         * @param replyTo   the original requester to reply to
         */
        public WrappedDiversityResponse(GlobalDiversity diversity, ActorRef<DiversityStatsResponse> replyTo) {
            this.diversity = diversity;
            this.replyTo = replyTo;
        }
    }

    // =========================================================================
    // 2. STATE & SETUP
    // =========================================================================
    private final TmdbServices tmdbService;

    /**
     * Private constructor for initializing the actor instance.
     *
     * @param context     the actor context providing access to actor lifecycle and
     *                    tools
     * @param tmdbService the TMDb service dependency for API interaction
     */
    private GlobalDiversityActor(ActorContext<Command> context, TmdbServices tmdbService) {
        super(context);
        this.tmdbService = tmdbService;
    }

    /**
     * Factory method to create the actor behavior.
     * Includes a Supervisor Strategy that restarts the actor upon encountering any
     * Exception,
     * ensuring system resilience.
     *
     * @param tmdbService the TMDb service dependency
     * @return the configured actor behavior with supervision
     */
    public static Behavior<Command> create(TmdbServices tmdbService) {
        return Behaviors.supervise(
                Behaviors.setup((ActorContext<Command> context) -> new GlobalDiversityActor(context, tmdbService)))
                .onFailure(Exception.class, SupervisorStrategy.restart());
    }

    // =========================================================================
    // 3. MESSAGE HANDLING LOGIC
    // =========================================================================

    /**
     * Sets up the message handlers for the actor.
     *
     * @return the receive behavior mapping commands to their respective methods
     */
    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(GetDiversityStats.class, this::onGetDiversityStats)
                .onMessage(WrappedDiversityResponse.class, this::onWrappedResponse)
                .build();
    }

    /**
     * Processes the request for diversity statistics.
     * Triggers an asynchronous service call and pipes the resulting future to the
     * actor's mailbox.
     *
     * @param command the request containing the movieId and reply address
     * @return the current behavior (this)
     */
    private Behavior<Command> onGetDiversityStats(GetDiversityStats command) {
        CompletionStage<GlobalDiversity> futureDiversity = tmdbService.getGlobalDiversity(command.movieId);

        getContext().pipeToSelf(futureDiversity, (diversity, failure) -> {
            if (failure != null) {
                throw new RuntimeException("TMDb API Failure during Diversity fetch", failure);
            }
            return new WrappedDiversityResponse(diversity, command.replyTo);
        });

        return this;
    }

    /**
     * Handles the internal wrapped result once the asynchronous call completes.
     * Sends the formatted DiversityStatsResponse to the original requester.
     *
     * @param wrapped the internal message containing the fetched data
     * @return the current behavior (this)
     */
    private Behavior<Command> onWrappedResponse(WrappedDiversityResponse wrapped) {
        wrapped.replyTo.tell(new DiversityStatsResponse(wrapped.diversity));
        return this;
    }
}