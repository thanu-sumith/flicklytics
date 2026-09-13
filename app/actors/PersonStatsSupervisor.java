package actors;

import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.SupervisorStrategy;
import org.apache.pekko.actor.typed.javadsl.*;

import Services.TmdbServices;

/**
 * Supervisor Actor for {@link PersonStatsActor}.
 * <p>
 * This actor is responsible for managing the lifecycle of a child
 * {@link PersonStatsActor} instance. It applies a supervision strategy
 * that restarts the child actor in case of failures and forwards all
 * incoming messages to the child.
 * </p>
 * 
 * @author Varshain Gopichandar Sreedevi
 */
public class PersonStatsSupervisor
        extends AbstractBehavior<PersonStatsActor.Command> {

    /** Reference to the supervised child actor */
    /** @author Varshain Gopichandar Sreedevi */
    private final ActorRef<PersonStatsActor.Command> personStatsActor;

    /**
     * Factory method to create the supervisor behavior.
     * 
     * @author Varshain Gopichandar Sreedevi
     * @param tmdbService the TMDb service dependency
     * @return the configured supervisor behavior
     */
    public static Behavior<PersonStatsActor.Command> create(TmdbServices tmdbService) {
        return Behaviors.setup(context -> new PersonStatsSupervisor(context, tmdbService));
    }

    /**
     * Private constructor that initializes the supervisor and spawns the child
     * actor.
     * <p>
     * Applies a restart supervision strategy to the child actor to ensure
     * fault tolerance.
     * </p>
     * 
     * @author Varshain Gopichandar Sreedevi
     * @param context     the actor context
     * @param tmdbService the TMDb service used by the child actor
     */
    private PersonStatsSupervisor(
            ActorContext<PersonStatsActor.Command> context,
            TmdbServices tmdbService) {

        super(context);

        this.personStatsActor = context.spawn(
                Behaviors.supervise(PersonStatsActor.create(tmdbService))
                        .onFailure(Exception.class, SupervisorStrategy.restart()),
                "personStatsChild");

        getContext().getLog().info("PersonStatsSupervisor started");
    }

    /**
     * Defines how the supervisor handles incoming messages.
     * <p>
     * All messages are forwarded to the child actor for processing.
     * </p>
     * 
     * @author Varshain Gopichandar Sreedevi
     * @return the receive behavior
     */
    @Override
    public Receive<PersonStatsActor.Command> createReceive() {
        return newReceiveBuilder()
                .onAnyMessage(this::forwardToChild)
                .build();
    }

    /**
     * Forwards incoming messages to the supervised child actor.
     * 
     * @author Varshain Gopichandar Sreedevi
     * @param message the message received by the supervisor
     * @return the current behavior
     */
    private Behavior<PersonStatsActor.Command> forwardToChild(PersonStatsActor.Command message) {

        getContext().getLog().debug("Forwarding message to child actor");

        personStatsActor.tell(message);

        return this;
    }
}