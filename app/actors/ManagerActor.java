package actors;

import org.apache.pekko.actor.typed.SupervisorStrategy;
import java.time.Duration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;
import play.libs.Json;

import actors.FinancialActor.GetFinancialStats;
import actors.FinancialActor.FinancialStatsResponse;
import actors.SearchActor.PerformSearch;
import actors.SearchActor.SearchResponse;
import actors.PersonStatsActor.PersonStatsFetched;
import actors.GlobalDiversityActor.DiversityStatsResponse;
import actors.ManagerActor.WrappedPersonStatsResponse;
import actors.ReviewActor.ReviewResponse;

import models.SearchRecord;
import Services.TmdbServices;

import java.util.*;

/**
 * ManagerActor: One per WebSocket connection
 * 
 * @author Varshain Gopichandar Sreedevi
 * @author Thanugundla Sumith Reddy
 * @author Honey Sharma
 * @author Aman Agnihotri
 * @author Harshavardhini Eluri
 */
public class ManagerActor extends AbstractBehavior<ManagerActor.Command> {

    public interface Command {
    }

    public static final class ChildFailed implements Command {
        public final String actorName;
        public final Throwable cause;

        public ChildFailed(String actorName, Throwable cause) {
            this.actorName = actorName;
            this.cause = cause;
        }
    }

    // UPDATED: Added for Live Updates
    public static final class WrappedReadabilityUpdate implements Command {
        public final String movieId;
        public final double ease;
        public final String grade;

        public WrappedReadabilityUpdate(String movieId, double ease, String grade) {
            this.movieId = movieId;
            this.ease = ease;
            this.grade = grade;
        }
    }

    public static final class InboundMessage implements Command {
        public final JsonNode json;

        /**
         * Constructs an inbound message from the client.
         * 
         * @author Varshain Gopichandar Sreedevi
         * @author Thanugundla Sumith Reddy
         * @author Honey Sharma
         * @author Aman Agnihotri
         * @author Harshavardhini Eluri
         * @param json the JSON payload received via WebSocket
         */
        public InboundMessage(JsonNode json) {
            this.json = json;
        }
    }

    public static final class WrappedFinancialResponse implements Command {
        public final FinancialStatsResponse response;

        /**
         * Wraps a financial response for internal handling.
         * 
         * @author Thanugundla Sumith Reddy
         * @param response the financial stats response
         */
        public WrappedFinancialResponse(FinancialStatsResponse response) {
            this.response = response;
        }
    }

    public static final class WrappedSearchResponse implements Command {
        public final SearchResponse response;

        /**
         * Wraps a search response for internal handling.
         * 
         * @author Harshavardhini Eluri
         * @param response the search response
         */
        public WrappedSearchResponse(SearchResponse response) {
            this.response = response;
        }
    }

    public static final class WrappedSearchAppendNotice implements Command {
        public final SearchResponse response;
        public final int appendedCount;

        public WrappedSearchAppendNotice(SearchResponse response, int appendedCount) {
            this.response = response;
            this.appendedCount = appendedCount;
        }
    }

    public static final class WrappedPersonStatsResponse implements Command {
        public final PersonStatsFetched response;

        /**
         * Wraps a person stats response for internal handling.
         * 
         * @author Varshain Gopichandar Sreedevi
         * @param response the fetched person stats
         */
        public WrappedPersonStatsResponse(PersonStatsFetched response) {
            this.response = response;
        }
    }

    public static final class WrappedDiversityResponse implements Command {
        public final DiversityStatsResponse response;

        /**
         * Wraps a diversity response for internal handling.
         * 
         * @author Aman Agnihotri
         * @param response the diversity stats response
         */
        public WrappedDiversityResponse(DiversityStatsResponse response) {
            this.response = response;
        }
    }

    public static final class WrappedReviewResponse implements Command {
        public final ReviewResponse response;

        /**
         * Wraps a review response for internal handling.
         * 
         * @author Honey Sharma
         * @param response the review response
         */
        public WrappedReviewResponse(ReviewResponse response) {
            this.response = response;
        }
    }

    private final ActorRef<JsonNode> clientActor;
    private final TmdbServices tmdbService;
    private final List<SearchRecord> searchHistory = new ArrayList<>();

    private final ActorRef<FinancialActor.Command> financialActor;
    private final ActorRef<SearchActor.Command> searchActor;
    private final ActorRef<PersonStatsActor.Command> personStatsActor;
    private final ActorRef<GlobalDiversityActor.Command> diversityActor;
    private final ActorRef<ReviewActor.Command> reviewActor;

    private ManagerActor(ActorContext<Command> context,
            ActorRef<JsonNode> clientActor,
            TmdbServices tmdbService) {
        super(context);
        this.clientActor = clientActor;
        this.tmdbService = tmdbService;
        // Custom supervision strategy
        /**
         * Spawns all child actors with explicit supervision strategies.
         * Each child actor is supervised with a restart strategy that allows up to
         * 3 restarts within a 30-second window before the actor is permanently stopped.
         * If a child exceeds this limit, a {@code Terminated} signal is sent to this
         * actor.
         */

        this.financialActor = context.spawn(
                Behaviors.supervise(FinancialActor.create(tmdbService))
                        .onFailure(Exception.class, SupervisorStrategy.restart().withLimit(3, Duration.ofSeconds(30))),
                "financialActor");

        this.searchActor = context.spawn(
                Behaviors.supervise(SearchActor.create(tmdbService))
                        .onFailure(Exception.class, SupervisorStrategy.restart().withLimit(3, Duration.ofSeconds(30))),
                "searchActor");

        this.personStatsActor = context.spawn(
                Behaviors.supervise(PersonStatsActor.create(tmdbService))
                        .onFailure(Exception.class, SupervisorStrategy.restart().withLimit(3, Duration.ofSeconds(30))),
                "personStatsActor");

        this.diversityActor = context.spawn(
                Behaviors.supervise(GlobalDiversityActor.create(tmdbService))
                        .onFailure(Exception.class, SupervisorStrategy.restart().withLimit(3, Duration.ofSeconds(30))),
                "diversityActor");

        this.reviewActor = context.spawn(
                Behaviors.supervise(ReviewActor.create(tmdbService))
                        .onFailure(Exception.class, SupervisorStrategy.restart().withLimit(3, Duration.ofSeconds(30))),
                "reviewActor");

        context.watch(this.financialActor);
        context.watch(this.searchActor);
        context.watch(this.personStatsActor);
        context.watch(this.diversityActor);
        context.watch(this.reviewActor);
    }

    public static Behavior<Command> create(ActorRef<JsonNode> clientActor,
            TmdbServices tmdbService) {
        return Behaviors.setup(ctx -> new ManagerActor(ctx, clientActor, tmdbService));
    }

    /**
     * Defines how this actor handles incoming messages.
     *
     * @return the receive behavior
     */

    // Handles child failures explicitly
    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(InboundMessage.class, this::onInboundMessage)
                .onMessage(WrappedSearchResponse.class, this::onSearchResponse)
                .onMessage(WrappedSearchAppendNotice.class, this::onSearchAppendNotice)
                .onMessage(WrappedReadabilityUpdate.class, this::onReadabilityUpdate)
                .onMessage(WrappedFinancialResponse.class, this::onFinancialResponse)
                .onMessage(WrappedPersonStatsResponse.class, this::onPersonStatsResponse)
                .onMessage(WrappedDiversityResponse.class, this::onDiversityResponse)
                .onMessage(WrappedReviewResponse.class, this::onReviewResponse)
                /**
                 * Handles the {@code Terminated} signal sent by Pekko when a watched child
                 * actor
                 * permanently stops after exhausting its restart limit defined in the
                 * supervision strategy.
                 *
                 * When triggered, this handler sends an error message to the connected
                 * WebSocket
                 * client identifying which actor failed, so the client can display an
                 * appropriate
                 * error message and prompt the user to retry the action.
                 */
                .onSignal(org.apache.pekko.actor.typed.Terminated.class, sig -> {
                    ObjectNode json = Json.newObject();
                    json.put("messageType", "actorError");
                    json.put("actor", sig.getRef().path().name());
                    json.put("error", "Actor stopped after max restarts. Please retry.");
                    clientActor.tell(json);
                    return this;
                })
                .build(); // <-- build() goes at the very END
    }

    // =========================================================================
    // INBOUND MESSAGE HANDLING
    // =========================================================================
    private Behavior<Command> onInboundMessage(InboundMessage msg) {
        String action = msg.json.path("action").asText("");
        if ("search".equals(action)) {
            String query = msg.json.path("query").asText("");
            String category = msg.json.path("category").asText("movie");

            // UPDATED: Using getSelf() so SearchActor knows who the Manager is
            searchActor.tell(new PerformSearch(query, category, getContext().getSelf()));
        } else if ("financial".equals(action)) {
            String query = msg.json.path("query").asText("");
            ActorRef<FinancialStatsResponse> adapter = getContext().messageAdapter(FinancialStatsResponse.class,
                    WrappedFinancialResponse::new);
            financialActor.tell(new GetFinancialStats(query, adapter));
        } else if ("personStats".equals(action)) {
            String personId = msg.json.path("personId").asText("");
            ActorRef<PersonStatsActor.Command> adapter = getContext().messageAdapter(PersonStatsActor.Command.class,
                    cmd -> cmd instanceof PersonStatsFetched ? new WrappedPersonStatsResponse((PersonStatsFetched) cmd)
                            : new WrappedPersonStatsResponse(null));
            personStatsActor.tell(new PersonStatsActor.FetchPersonStats(personId, adapter));
        } else if ("diversity".equals(action)) {
            int movieId = msg.json.path("movieId").asInt();
            ActorRef<DiversityStatsResponse> adapter = getContext().messageAdapter(DiversityStatsResponse.class,
                    WrappedDiversityResponse::new);
            diversityActor.tell(new GlobalDiversityActor.GetDiversityStats(movieId, adapter));
        } else if ("reviews".equals(action)) {
            String id = msg.json.path("id").asText("");
            String category = msg.json.path("category").asText("");
            String name = msg.json.path("name").asText("");
            ActorRef<ReviewResponse> adapter = getContext().messageAdapter(ReviewResponse.class,
                    WrappedReviewResponse::new);
            reviewActor.tell(new ReviewActor.GetReviews(id, category, name, adapter));
        }
        return this;
    }

    // =========================================================================
    // RESPONSES
    // =========================================================================

    /**
     * Handles live readability updates and sends them to the client via WebSocket.
     */
    // --- START OF READABILITY UPDATE LOGIC ---
    private Behavior<Command> onReadabilityUpdate(WrappedReadabilityUpdate update) {
        ObjectNode json = Json.newObject();
        json.put("messageType", "readabilityUpdate");
        json.put("movieId", update.movieId);
        json.put("ease", update.ease);
        json.put("grade", update.grade);
        clientActor.tell(json);
        return this;
    }
    // --- END OF READABILITY UPDATE LOGIC ---

    /**
     * Handles search responses, maintains search history, and sends results to the
     * client.
     * 
     * @author Harshavardhini Eluri
     * @param wrapped the wrapped search response
     * @return the current behavior
     */
    private Behavior<Command> onSearchResponse(WrappedSearchResponse wrapped) {
        SearchRecord newRecord = wrapped.response.record;

        // ✅ REMOVE duplicate history entry
        searchHistory.removeIf(r -> r.query.equalsIgnoreCase(newRecord.query) &&
                r.category.equalsIgnoreCase(newRecord.category));

        searchHistory.add(0, newRecord);

        if (searchHistory.size() > 10) {
            searchHistory.remove(searchHistory.size() - 1);
        }

        // ✅ D2 BONUS: FILTER duplicate RESULTS inside record
        if (newRecord.results != null) {
            Set<String> seenIds = new HashSet<>();
            newRecord.results.removeIf(item -> !seenIds.add(item.id));
        }

        ObjectNode json = Json.newObject();
        json.put("messageType", "searchResults");
        json.set("data", Json.toJson(searchHistory));
        clientActor.tell(json);
        return this;
    }

    private Behavior<Command> onSearchAppendNotice(WrappedSearchAppendNotice wrapped) {
        ObjectNode json = Json.newObject();
        json.put("messageType", "searchAppendNotice");
        json.put("appendedCount", wrapped.appendedCount);
        json.set("data", Json.toJson(wrapped.response.record));
        clientActor.tell(json);
        return this;
    }

    /**
     * Handles Financial result responses and sends them to the client.
     * 
     * @author Thanugundla Sumith Reddy
     * @param wrapped the wrapped person stats response
     * @return the current behavior
     */
    private Behavior<Command> onFinancialResponse(WrappedFinancialResponse wrapped) {
        ObjectNode json = Json.newObject();
        json.put("messageType", "financialResults");
        json.set("data", Json.toJson(wrapped.response.stats));
        clientActor.tell(json);
        return this;
    }

    /**
     * Handles person statistics responses and sends them to the client.
     * 
     * @author Varshain Gopichandar Sreedevi
     * @param wrapped the wrapped person stats response
     * @return the current behavior
     */
    private Behavior<Command> onPersonStatsResponse(WrappedPersonStatsResponse wrapped) {
        ObjectNode json = Json.newObject();
        if (wrapped.response == null) {
            json.put("messageType", "personStatsError");
        } else {
            json.put("messageType", "personStatsResults");
            json.set("data", Json.toJson(wrapped.response.summary()));
        }
        clientActor.tell(json);
        return this;
    }

    /**
     * Handles diversity responses and sends them to the client.
     * 
     * @author Aman Agnihotri
     * @param wrapped the wrapped diversity response
     * @return the current behavior
     */
    private Behavior<Command> onDiversityResponse(WrappedDiversityResponse wrapped) {
        ObjectNode json = Json.newObject();
        json.put("messageType", "diversityResults");
        json.set("data", Json.toJson(wrapped.response.diversity));
        clientActor.tell(json);
        return this;
    }

    /**
     * Handles review responses and sends them to the client.
     * 
     * @author Honey Sharma
     * @param wrapped the wrapped review response
     * @return the current behavior
     */
    private Behavior<Command> onReviewResponse(WrappedReviewResponse wrapped) {
        ObjectNode json = Json.newObject();
        json.put("messageType", "reviewResults");
        json.set("data", Json.toJson(wrapped.response.payload));
        clientActor.tell(json);
        return this;
    }
}
