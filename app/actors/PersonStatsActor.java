package actors;

import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.*;

import models.PersonStatsSummary;
import Services.TmdbServices;

/**
 * Pekko Typed Actor responsible for fetching and processing
 * person-related statistics (Part D).
 * <p>
 * This actor receives requests for person statistics, performs asynchronous
 * API calls via {@link TmdbServices}, and sends results back to the requester.
 * </p>
 * 
 * @author Varshain Gopichandar Sreedevi
 */
public class PersonStatsActor extends AbstractBehavior<PersonStatsActor.Command> {

    // =========================================================================
    // MESSAGES
    // =========================================================================

    /**
     * Marker interface for all commands supported by this actor.
     * 
     * @author Varshain Gopichandar Sreedevi
     */
    public sealed interface Command permits FetchPersonStats, PersonStatsFetched, FetchFailed {
    }

    /**
     * Command message to request statistics for a specific person.
     * 
     * @author Varshain Gopichandar Sreedevi
     * @param personId the unique identifier of the person
     * @param replyTo  the actor to send the response back to
     */
    public record FetchPersonStats(
            String personId,
            ActorRef<Command> replyTo) implements Command {
    }

    /**
     * Message indicating successful retrieval of person statistics.
     * 
     * @author Varshain Gopichandar Sreedevi
     * @param personId the ID of the person
     * @param summary  the computed statistics summary
     */
    public record PersonStatsFetched(
            String personId,
            PersonStatsSummary summary) implements Command {
    }

    /**
     * Message indicating failure while fetching person statistics.
     * 
     * @author Varshain Gopichandar Sreedevi
     * @param personId the ID of the person
     * @param reason   the error/exception that occurred
     */
    public record FetchFailed(
            String personId,
            Throwable reason) implements Command {
    }

    // =========================================================================
    // STATE
    // =========================================================================

    /** Service used to interact with the TMDb API */
    /** @author Varshain Gopichandar Sreedevi */
    private final TmdbServices tmdbService;

    /**
     * Factory method to create the actor behavior.
     * 
     * @author Varshain Gopichandar Sreedevi
     * @param tmdbService the TMDb service dependency
     * @return the configured actor behavior
     */
    public static Behavior<Command> create(TmdbServices tmdbService) {
        return Behaviors.setup(context -> new PersonStatsActor(context, tmdbService));
    }

    /**
     * Private constructor to initialize the actor.
     * 
     * @author Varshain Gopichandar Sreedevi
     * @param context     the actor context
     * @param tmdbService the TMDb service used for API calls
     */
    private PersonStatsActor(ActorContext<Command> context, TmdbServices tmdbService) {
        super(context);
        this.tmdbService = tmdbService;
    }

    // =========================================================================
    // RECEIVE
    // =========================================================================

    /**
     * Defines how this actor handles incoming messages.
     * 
     * @author Varshain Gopichandar Sreedevi
     * @return the receive behavior
     */
    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(FetchPersonStats.class, this::onFetchPersonStats)
                .build();
    }

    // =========================================================================
    // HANDLER
    // =========================================================================

    /**
     * Handles requests to fetch person statistics.
     * <p>
     * Initiates an asynchronous operation using
     * {@link PersonStatsSummary#fetch(String, TmdbServices)}
     * and sends either a success or failure message back to the requesting actor.
     * </p>
     * 
     * @author Varshain Gopichandar Sreedevi
     * @param command the incoming fetch request
     * @return the current actor behavior
     */
    private Behavior<Command> onFetchPersonStats(FetchPersonStats command) {

        getContext().getLog().info("Fetching person stats for ID: {}", command.personId());

        // Asynchronous fetch operation
        PersonStatsSummary.fetch(command.personId(), tmdbService)
                .thenAccept(summary -> {
                    command.replyTo().tell(
                            new PersonStatsFetched(command.personId(), summary));
                })
                .exceptionally(error -> {
                    command.replyTo().tell(
                            new FetchFailed(command.personId(), error));
                    return null;
                });

        return this;
    }
}