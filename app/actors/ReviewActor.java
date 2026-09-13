package actors;

import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.SupervisorStrategy;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;

import models.ReviewSentiment;
import Services.TmdbServices;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * The actor responsible for retrieving reviews from TMDB and analyzing their
 * sentiment.
 *
 * @author Honey Sharma
 */
public class ReviewActor extends AbstractBehavior<ReviewActor.Command> {

    // =========================================================================
    // 1. PROTOCOL
    // =========================================================================
    /**
     * Marker interface for all commands handled by this actor.
     * 
     * @author Honey Sharma
     */
    public interface Command {
    }

    /**
     * Send a message requesting sentiment analysis and reviews.
     * 
     * @author Honey Sharma
     */
    public static final class GetReviews implements Command {
        public final String id;
        public final String category;
        public final String name;
        public final ActorRef<ReviewResponse> replyTo;

        public GetReviews(String id, String category, String name, ActorRef<ReviewResponse> replyTo) {
            this.id = id;
            this.category = category;
            this.name = name;
            this.replyTo = replyTo;
        }
    }

    /**
     * Payload containing processed reviews and global sentiment.
     * 
     * @author Honey Sharma
     */
    public static final class ReviewPayload {
        public final List<ReviewSentiment> reviews;
        public final String globalSentiment;

        public ReviewPayload(List<ReviewSentiment> reviews, String globalSentiment) {
            this.reviews = reviews;
            this.globalSentiment = globalSentiment;
        }
    }

    /**
     * Response message sent back to the requester.
     * 
     * @author Honey Sharma
     */
    public static final class ReviewResponse {
        public final ReviewPayload payload;

        public ReviewResponse(ReviewPayload payload) {
            this.payload = payload;
        }
    }

    /**
     * Internal message for wrapping asynchronous results from the TMDB API.
     * 
     * @author Honey Sharma
     */
    private static final class WrappedReviewResponse implements Command {
        public final ReviewPayload payload;
        public final ActorRef<ReviewResponse> replyTo;

        public WrappedReviewResponse(ReviewPayload payload, ActorRef<ReviewResponse> replyTo) {
            this.payload = payload;
            this.replyTo = replyTo;
        }
    }

    // =========================================================================
    // 2. STATE & SETUP
    // =========================================================================
    /**
     * Service used to fetch data from TMDB API.
     * 
     * @author Honey Sharma
     */
    private final TmdbServices tmdbService;

    /**
     * Constructor for ReviewActor.
     * 
     * @author Honey Sharma
     */
    private ReviewActor(ActorContext<Command> context, TmdbServices tmdbService) {
        super(context);
        this.tmdbService = tmdbService;
    }

    /**
     * Factory method for creating a ReviewActor using a supervision technique.
     * 
     * @author Honey Sharma
     */
    public static Behavior<Command> create(TmdbServices tmdbService) {
        return Behaviors.supervise(
                Behaviors.setup((ActorContext<Command> context) -> new ReviewActor(context, tmdbService)))
                .onFailure(Exception.class, SupervisorStrategy.restart());
    }

    // =========================================================================
    // 3. MESSAGE HANDLING LOGIC
    // =========================================================================
    /**
     * Defines how the actor processes incoming messages.
     * 
     * @author Honey Sharma
     */
    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(GetReviews.class, this::onGetReviews)
                .onMessage(WrappedReviewResponse.class, this::onWrappedResponse)
                .build();
    }

    /**
     * Collects reviews asynchronously from TMDB, analyzes sentiment
     * and returns the outcome to the actor.
     * 
     * @return updated behavior
     * @author Honey Sharma
     */
    private Behavior<Command> onGetReviews(GetReviews command) {
        CompletionStage<ReviewPayload> futureReviews = tmdbService.getReviews(command.id, command.category)
                .thenApply(jsonNode -> {
                    List<ReviewSentiment> reviews = ReviewSentiment.buildReviews(jsonNode, tmdbService);
                    String globalSentiment = ReviewSentiment.getGlobalSentiment(reviews);
                    return new ReviewPayload(reviews, globalSentiment);
                });

        getContext().pipeToSelf(futureReviews, (payload, failure) -> {
            if (failure != null) {
                throw new RuntimeException("TMDb API Failure during Reviews fetch", failure);
            }
            return new WrappedReviewResponse(payload, command.replyTo);
        });
        return this;
    }

    /**
     * Returns the processed review data to the actor that made the request.
     * 
     * @return updated behavior
     * @author Honey Sharma
     */
    private Behavior<Command> onWrappedResponse(WrappedReviewResponse wrapped) {
        wrapped.replyTo.tell(new ReviewResponse(wrapped.payload));
        return this;
    }
}