package actors;

import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.SupervisorStrategy;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;
import models.MovieResults;
import models.SearchRecord;
import Services.TmdbServices;
import java.util.concurrent.CompletionStage;
import java.util.Timer;
import java.util.TimerTask;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

/**
 * Actor responsible for performing TMDb search operations and computing
 * readability scores for each result item.
 * <p>
 * For each search, this actor fetches results from the TMDb API, forwards
 * them to the ManagerActor, and then delegates readability calculation
 * for each result to a child {@link ReadabilityActor}.
 * </p>
 * 
 * @author Varshain Gopichandar Sreedevi
 * @author Thanugundla Sumith Reddy
 * @author Honey Sharma
 * @author Aman Agnihotri
 * @author Harshavardhini Eluri
 */
public class SearchActor extends AbstractBehavior<SearchActor.Command> {

    /**
     * Marker interface for all messages handled by {@link SearchActor}.
     * 
     * @author Harshavardhini Eluri
     */
    public interface Command {
    }

    /**
     * Command to trigger a TMDb search for the given query and category.
     * 
     * @author Harshavardhini Eluri
     */
    public static final class PerformSearch implements Command {
        public final String query;
        public final String category;
        public final ActorRef<ManagerActor.Command> replyTo;

        /**
         * Constructs a PerformSearch command.
         *
         * @param query    the search keyword(s) entered by the user
         * @param category the content category (movie, tv, or person)
         * @param replyTo  the ManagerActor to send the search response to
         * @author Thanugundla Sumith Reddy
         */
        public PerformSearch(String query, String category, ActorRef<ManagerActor.Command> replyTo) {
            this.query = query;
            this.category = category;
            this.replyTo = replyTo;
        }
    }

    /**
     * Response message containing the search results as a {@link SearchRecord}.
     * 
     * @author Thanugundla Sumith Reddy
     */
    public static final class SearchResponse {
        public final SearchRecord record;

        /**
         * Constructs a SearchResponse.
         *
         * @param record the search record containing results for the query
         * @author Harshavardhini Eluri
         */
        public SearchResponse(SearchRecord record) {
            this.record = record;
        }
    }

    /**
     * Internal message wrapping a readability response from
     * {@link ReadabilityActor},
     * along with the movie ID and the manager actor reference.
     * 
     * @author Harshavardhini Eluri
     */
    private static final class WrappedReadabilityResponse implements Command {
        final ReadabilityActor.ReadabilityResponse response;
        final String movieId;
        final ActorRef<ManagerActor.Command> manager;

        /**
         * Constructs a WrappedReadabilityResponse.
         *
         * @param response the readability response from the ReadabilityActor
         * @param movieId  the ID of the movie whose overview was analyzed
         * @param manager  the ManagerActor to forward the readability update to
         * @author Harshavardhini Eluri
         */
        WrappedReadabilityResponse(ReadabilityActor.ReadabilityResponse response, String movieId,
                ActorRef<ManagerActor.Command> manager) {
            this.response = response;
            this.movieId = movieId;
            this.manager = manager;
        }
    }

    /**
     * Internal message wrapping a completed search result and its reply target.
     * 
     * @author Harshavardhini Eluri
     */
    private static final class WrappedSearchResponse implements Command {
        final SearchRecord record;
        final ActorRef<ManagerActor.Command> replyTo;

        /**
         * Constructs a WrappedSearchResponse.
         *
         * @param record  the completed search record
         * @param replyTo the ManagerActor to forward the result to
         * @author Varshain Gopichandar Sreedevi
         */
        WrappedSearchResponse(SearchRecord record, ActorRef<ManagerActor.Command> replyTo) {
            this.record = record;
            this.replyTo = replyTo;
        }
    }

    private static final class CheckForMoreResults implements Command {
        final String query;
        final String category;
        final ActorRef<ManagerActor.Command> replyTo;

        CheckForMoreResults(String query, String category, ActorRef<ManagerActor.Command> replyTo) {
            this.query = query;
            this.category = category;
            this.replyTo = replyTo;
        }
    }

    private final TmdbServices tmdbService;
    private final ActorRef<ReadabilityActor.Command> readabilityActor;
    private Timer pollingTimer;
    private String activeQuery = "";
    private String activeCategory = "movie";
    private final Set<String> seenIds = new LinkedHashSet<>();
    private int appendedCount = 0;
    private int nextPageToFetch = 1;
    private SearchRecord activeRecord;

    /**
     * Private constructor that initializes the SearchActor and spawns
     * a child {@link ReadabilityActor}.
     *
     * @param context     the actor context
     * @param tmdbService the TMDb service used for API calls
     * @author Harshavardhini Eluri
     */
    private SearchActor(ActorContext<Command> context, TmdbServices tmdbService) {
        super(context);
        this.tmdbService = tmdbService;
        this.readabilityActor = context.spawn(ReadabilityActor.create(), "ReadabilityWorker");
    }

    /**
     * Factory method to create the SearchActor behavior with a restart
     * supervision strategy.
     *
     * @param tmdbService the TMDb service dependency
     * @return the supervised behavior for this actor
     * @author Harshavardhini Eluri
     */
    public static Behavior<Command> create(TmdbServices tmdbService) {
        return Behaviors.supervise(
                Behaviors.setup((ActorContext<Command> context) -> new SearchActor(context, tmdbService)))
                .onFailure(Exception.class, SupervisorStrategy.restart());
    }

    /**
     * Defines the message handling behavior for this actor.
     *
     * @return the receive builder configured for this actor's commands
     * @author Honey Sharma
     */
    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(PerformSearch.class, this::onPerformSearch)
                .onMessage(WrappedSearchResponse.class, this::onWrappedResponse)
                .onMessage(CheckForMoreResults.class, this::onCheckForMoreResults)
                .onMessage(WrappedSearchDelta.class, this::onWrappedSearchDelta)
                .onMessage(WrappedReadabilityResponse.class, this::onReadabilityResult)
                .build();
    }

    /**
     * Handles a {@link PerformSearch} command by asynchronously fetching
     * search results from the TMDb API and piping the result back to this actor.
     *
     * @param command the search command containing the query, category, and reply
     *                target
     * @return the same behavior (this actor remains unchanged after handling)
     * @author Varshain Gopichandar Sreedevi
     */
    private Behavior<Command> onPerformSearch(PerformSearch command) {
        cancelPolling();
        activeQuery = command.query;
        activeCategory = command.category;
        seenIds.clear();
        appendedCount = 0;
        nextPageToFetch = 1;

        CompletionStage<SearchRecord> futureRecord = tmdbService.search(command.category, command.query)
                .thenApply(json -> SearchRecord.fromJson(command.query, command.category, json, tmdbService));

        getContext().pipeToSelf(futureRecord, (record, failure) -> new WrappedSearchResponse(record, command.replyTo));
        return this;
    }

    /**
     * Handles a {@link WrappedSearchResponse} by forwarding the search results
     * to the ManagerActor and triggering readability calculation for each result
     * item.
     *
     * @param wrapped the wrapped search response containing the record and reply
     *                target
     * @return the same behavior (this actor remains unchanged after handling)
     * @author Aman Agnihotri
     */
    private Behavior<Command> onWrappedResponse(WrappedSearchResponse wrapped) {
        activeRecord = wrapped.record;
        if (wrapped.record != null && wrapped.record.getResults() != null) {
            for (MovieResults movie : wrapped.record.getResults()) {
                seenIds.add(movie.id);
            }
        }
        nextPageToFetch = 1;

        wrapped.replyTo.tell(new ManagerActor.WrappedSearchResponse(new SearchResponse(wrapped.record)));
        schedulePolling(wrapped.replyTo);

        if (wrapped.record != null && wrapped.record.getResults() != null) {
            for (MovieResults movie : wrapped.record.getResults()) {
                ActorRef<ReadabilityActor.ReadabilityResponse> adapter = getContext().messageAdapter(
                        ReadabilityActor.ReadabilityResponse.class,
                        res -> new WrappedReadabilityResponse(res, movie.id, wrapped.replyTo));

                readabilityActor.tell(new ReadabilityActor.CalculateReadability(movie.getOverview(), adapter));
            }
        }
        return this;
    }

    private Behavior<Command> onCheckForMoreResults(CheckForMoreResults command) {
        if (!command.query.equals(activeQuery) || !command.category.equals(activeCategory)) {
            return this;
        }
        if (appendedCount >= 10) {
            cancelPolling();
            return this;
        }

        int page = nextPageToFetch;
        nextPageToFetch++;

        CompletionStage<SearchRecord> futureRecord = tmdbService.search(command.category, command.query, page)
                .thenApply(json -> SearchRecord.fromJson(command.query, command.category, json, tmdbService, 20));

        getContext().pipeToSelf(futureRecord, (record, failure) -> new WrappedSearchDelta(record, command.replyTo));
        return this;
    }

    private static final class WrappedSearchDelta implements Command {
        final SearchRecord record;
        final ActorRef<ManagerActor.Command> replyTo;

        WrappedSearchDelta(SearchRecord record, ActorRef<ManagerActor.Command> replyTo) {
            this.record = record;
            this.replyTo = replyTo;
        }
    }

    private Behavior<Command> onWrappedSearchDelta(WrappedSearchDelta wrapped) {
        if (wrapped.record == null || wrapped.record.getResults() == null || activeRecord == null) {
            return this;
        }

        if (wrapped.record.getResults().isEmpty()) {
            cancelPolling();
            return this;
        }

        List<MovieResults> newlyAdded = new ArrayList<>();
        int remaining = 10 - appendedCount;
        for (MovieResults movie : wrapped.record.getResults()) {
            if (remaining <= 0) break;
            if (seenIds.add(movie.id)) {
                activeRecord.results.add(movie);
                newlyAdded.add(movie);
                appendedCount++;
                remaining--;
            }
        }

        if (!newlyAdded.isEmpty()) {
            wrapped.replyTo.tell(new ManagerActor.WrappedSearchAppendNotice(
                    new SearchResponse(activeRecord),
                    newlyAdded.size()));
        }

        if (appendedCount >= 10) {
            cancelPolling();
        }
        return this;
    }

    private void schedulePolling(ActorRef<ManagerActor.Command> replyTo) {
        if (pollingTimer != null) {
            return;
        }
        pollingTimer = new Timer(true);
        pollingTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                getContext().getSelf().tell(new CheckForMoreResults(activeQuery, activeCategory, replyTo));
            }
        }, 4000L, 4000L);
    }

    private void cancelPolling() {
        if (pollingTimer != null) {
            pollingTimer.cancel();
            pollingTimer = null;
        }
    }

    /**
     * Handles a {@link WrappedReadabilityResponse} by forwarding the computed
     * readability scores to the ManagerActor for the corresponding movie.
     *
     * @param wrapped the wrapped readability response containing scores and the
     *                manager reference
     * @return the same behavior (this actor remains unchanged after handling)
     * @author Harshavardhini Eluri
     */
    private Behavior<Command> onReadabilityResult(WrappedReadabilityResponse wrapped) {
        wrapped.manager.tell(new ManagerActor.WrappedReadabilityUpdate(
                wrapped.movieId,
                wrapped.response.ease,
                wrapped.response.grade));
        return this;
    }
}
