package controllers;

import play.mvc.*;
import play.libs.streams.ActorFlow;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.stream.Materializer;
import actors.ManagerActor;

import Services.TmdbServices;
import javax.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import com.fasterxml.jackson.databind.JsonNode;
import play.cache.SyncCacheApi;

import org.apache.pekko.actor.typed.javadsl.Adapter;
import org.apache.pekko.stream.javadsl.Flow;

/**
 * Main Controller for FlickLytics Application.
 * Handles HTTP routes, WebSocket connections, and delegates processing
 * to actors and services.
 * @author Varshain Gopichandar Sreedevi
 * @author Thanugundla Sumith Reddy
 * @author Honey Sharma
 * @author Aman Agnihotri
 * @author Harsha Vardhini Eluri
 */
public class HomeController extends Controller {

    private final TmdbServices tmdbService;
    private final SyncCacheApi cache;
    private final ActorSystem actorSystem;
    private final Materializer materializer;

    /**
     * Constructs the HomeController with required dependencies.
     *
     * @param tmdbService  service for TMDb API calls
     * @param cache        cache API for storing session data
     * @param actorSystem  Pekko actor system
     * @param materializer stream materializer
     * @author Varshain G S
     */
    @Inject
    public HomeController(TmdbServices tmdbService,
                          SyncCacheApi cache,
                          ActorSystem actorSystem,
                          Materializer materializer) {
        this.tmdbService = tmdbService;
        this.cache = cache;
        this.actorSystem = actorSystem;
        this.materializer = materializer;
    }

    // =========================================================================
    // INDEX
    // =========================================================================

    /**
     * Renders the home page of the application.
     *
     * @return a CompletionStage containing the HTTP response
     * @author Varshain G S
     */
    public CompletionStage<Result> index() {
        return java.util.concurrent.CompletableFuture.completedFuture(
                ok(views.html.index.render()));
    }

    // =========================================================================
    // WEBSOCKET
    // =========================================================================

    /**
     * Establishes a WebSocket connection between client and server.
     * <p>
     * Uses ActorFlow to connect WebSocket messages to {@link ManagerActor}.
     * </p>
     *
     * @return a WebSocket handler for JSON messages
     * @author Thanugundla Sumith Reddy
     */
    public WebSocket ws() {
        return WebSocket.Json.accept(request -> {

            Flow<ManagerActor.Command, JsonNode, ?> actorFlow =
                    ActorFlow.actorRef(
                            out -> Adapter.props(
                                    () -> ManagerActor.create(Adapter.toTyped(out), tmdbService)),
                            actorSystem,
                            materializer);

            return Flow.<JsonNode>create()
                    .map(json -> (ManagerActor.Command) new ManagerActor.InboundMessage(json))
                    .via(actorFlow);
        });
    }

    // =========================================================================
    // SEARCH
    // =========================================================================

    /**
     * Handles search requests and maintains user search history.
     *
     * @param request  the HTTP request containing session data
     * @param query    the search query string
     * @param category the search category (movie, TV, etc.)
     * @return a CompletionStage containing the rendered search results page
     * @author Thanugundla Sumith Reddy
     */
    public CompletionStage<Result> search(Http.Request request, String query, String category) {

        String sessionId = request.session().getOptional("sessionId")
                .orElse(UUID.randomUUID().toString());

        List<String> history = (List<String>) cache.getOptional(sessionId)
                .orElse(new ArrayList<>());

        String currentSearch = query + "|" + category;

        history.remove(currentSearch);
        history.add(0, currentSearch);

        if (history.size() > 10) {
            history = new ArrayList<>(history.subList(0, 10));
        }

        cache.set(sessionId, history);

        return java.util.concurrent.CompletableFuture.completedFuture(
                ok(views.html.searchresult.render(query, category))
                        .addingToSession(request, "sessionId", sessionId));
    }

    // =========================================================================
    // ITEM DETAILS
    // =========================================================================

    /**
     * Retrieves item details and computes readability metrics.
     *
     * @param category the content category (movie or TV)
     * @param id       the item ID
     * @return a CompletionStage containing the rendered details page
     * @author Harsha Vardhini Eluri
     */
    public CompletionStage<Result> itemDetails(String category, String id) {
        return tmdbService.getDetails(category, id).thenApply(json -> {

            models.ReadabilityResult result =
                    models.ReadabilityResult.fromJson(json, category);

            result.ease = models.ReadabilityResult.calculateEase(result.overview);
            result.grade = models.ReadabilityResult.calculateGrade(result.overview);

            return ok(views.html.Movies_TV_Series_Description_Readability.render(result, category));
        });
    }

    // =========================================================================
    // GLOBAL DIVERSITY
    // =========================================================================

    /**
     * Renders the global diversity page.
     *
     * @param movieId the movie ID
     * @return the HTTP response containing the rendered page
     * @author Aman Agnihotri
     */
    public CompletionStage<Result> globalDiversity(int movieId) {
        return java.util.concurrent.CompletableFuture.completedFuture(
            ok(views.html.globalDiversity.render(movieId)));
    }

    // =========================================================================
    // FINANCIAL PERFORMANCE
    // =========================================================================

    /**
     * Renders the financial performance page.
     *
     * @param query the encoded search query
     * @return a CompletionStage containing the rendered page
     * @author Thanugundla Sumith Reddy
     */
    public CompletionStage<Result> financialPerformance(String query) {
        String cleanQuery = java.net.URLDecoder.decode(
                query,
                java.nio.charset.StandardCharsets.UTF_8
        );

        return java.util.concurrent.CompletableFuture.completedFuture(
                ok(views.html.financialPerformance.render(cleanQuery)));
    }

    // =========================================================================
    // PERSON STATS
    // =========================================================================

    /**
     * Renders the person statistics page.
     *
     * @param id the person ID
     * @return a CompletionStage containing the rendered page
     * @author Varshain Gopichandar Sreedevi
     */
    public CompletionStage<Result> personStats(String id) {
        return java.util.concurrent.CompletableFuture.completedFuture(
                ok(views.html.personStats.render(id)));
    }

    // =========================================================================
    // REVIEWS
    // =========================================================================

    /**
     * Renders the reviews page for a given item.
     *
     * @param id       the item ID
     * @param category the content category
     * @param name     the encoded name of the item
     * @return the HTTP response containing the rendered page
     * @author Honey Sharma
     */
    public CompletionStage<Result> reviews(String id, String category, String name) {
        String cleanName = java.net.URLDecoder.decode(
                name,
                java.nio.charset.StandardCharsets.UTF_8
        );

        return java.util.concurrent.CompletableFuture.completedFuture(
            ok(views.html.reviews.render(id, category, cleanName)));
    }
}