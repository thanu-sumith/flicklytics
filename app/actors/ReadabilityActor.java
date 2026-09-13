package actors;

import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;
import models.ReadabilityResult;

/**
 * Actor responsible for calculating readability scores for Movie/TV
 * descriptions.
 * 
 * @author Harshavardhini Eluri
 */
public class ReadabilityActor extends AbstractBehavior<ReadabilityActor.Command> {

    public interface Command {
    }

    public static final class CalculateReadability implements Command {
        public final String text;
        public final ActorRef<ReadabilityResponse> replyTo;

        /**
         * Constructs a CalculateReadability command.
         *
         * @param text    the text to analyze for readability
         * @param replyTo the actor to send the readability response to
         * @author Harshavardhini Eluri
         */
        public CalculateReadability(String text, ActorRef<ReadabilityResponse> replyTo) {
            this.text = text;
            this.replyTo = replyTo;
        }
    }

    public static final class ReadabilityResponse {
        public final double ease;
        public final String grade;

        /**
         * Constructs a ReadabilityResponse with the computed scores.
         *
         * @param ease  the Flesch Reading Ease score
         * @param grade the Flesch-Kincaid Grade Level as a string
         * @author Harshavardhini Eluri
         */
        public ReadabilityResponse(double ease, String grade) {
            this.ease = ease;
            this.grade = grade;
        }
    }

    /**
     * Factory method to create the ReadabilityActor behavior.
     *
     * @return the initial behavior of the ReadabilityActor
     * @author Harshavardhini Eluri
     */
    public static Behavior<Command> create() {
        return Behaviors.setup(ReadabilityActor::new);
    }

    private ReadabilityActor(ActorContext<Command> context) {
        super(context);
    }

    /**
     * Defines the message handling behavior for this actor.
     *
     * @return the receive builder configured for this actor's commands
     * @author Harshavardhini Eluri
     */
    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(CalculateReadability.class, this::onCalculate)
                .build();
    }

    /**
     * Handles a CalculateReadability command by computing the Flesch Reading
     * Ease score and Flesch-Kincaid Grade Level for the given text, then
     * sending a ReadabilityResponse back to the requester.
     *
     * @param cmd the command containing the text to analyze and the reply target
     * @return the same behavior (this actor remains unchanged after handling)
     * @author Harshavardhini Eluri
     */
    private Behavior<Command> onCalculate(CalculateReadability cmd) {
        double ease = ReadabilityResult.calculateEase(cmd.text);
        double gradeVal = ReadabilityResult.calculateGrade(cmd.text);

        String gradeStr = String.valueOf(gradeVal);

        cmd.replyTo.tell(new ReadabilityResponse(ease, gradeStr));
        return this;
    }
}