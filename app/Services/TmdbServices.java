package Services;

import play.libs.ws.*;
import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.CompletionStage;
import com.fasterxml.jackson.databind.JsonNode;
import com.typesafe.config.Config;
import models.GlobalDiversity;

/**
 * Service class for communicating with the TMDb REST API.
 * Provides asynchronous, non-blocking methods for searching items,
 * fetching item details, loading genre mappings, retrieving reviews,
 * and performing sentiment analysis on review content.
 * All API methods return CompletionStage to ensure reactive, non-blocking
 * behavior.
 * 
 * @author Varshain Gopichandar Sreedevi
 * @author Thanugundla Sumith Reddy
 * @author Honey Sharma
 * @author Aman Agnihotri
 * @author Harshavardhini Eluri
 */

public class TmdbServices {

    /** The Play Framework HTTP client used for making API requests. */
    private final WSClient ws;

    /** The TMDb API key loaded from application.conf. */
    private final String apiKey;
    // private Map<Integer, String> movieGenresCache = new HashMap<>();
    // private Map<Integer, String> tvGenresCache = new HashMap<>();

    /**
     * In-memory cache mapping genre IDs to genre names for movies.
     * Populated once on startup via {@link #loadMovieGenres()}.
     */
    private Map<Integer, String> movieGenresCache = new HashMap<>();

    /**
     * In-memory cache mapping genre IDs to genre names for TV shows.
     * Populated once on startup via {@link #loadTvGenres()}.
     */
    private Map<Integer, String> tvGenresCache = new HashMap<>();

    /**
     * Constructs TmdbServices with injected WSClient and Config.
     * Automatically loads movie and TV genre mappings from TMDb on startup
     * to avoid repeated API calls during search result processing.
     * 
     * @param ws     the Play Framework HTTP client
     * @param config the application configuration containing the TMDb API key
     * @author Thanugundla Sumith Reddy
     */
    @Inject
    public TmdbServices(WSClient ws, Config config) {
        this.ws = ws;
        this.apiKey = config.getString("tmdb.apikey");
        // this.apiKey= config.getString("tmdb.apikey");
        loadMovieGenres();
        loadTvGenres();
    }

    /**
     * Searches TMDb for items matching the given query in the specified category.
     * Returns a CompletionStage of JSON containing the search results array
     * and total_results count. This method is fully asynchronous and non-blocking.
     * 
     * @param category the search category: "movie", "tv", or "person"
     * @param query    the search keyword entered by the user
     * @return CompletionStage of JsonNode containing the TMDb search response
     * @author Varshain Gopichandar Sreedevi
     */
    public CompletionStage<JsonNode> search(String category, String query) {
        return search(category, query, 1);
    }

    public CompletionStage<JsonNode> search(String category, String query, int page) {
        String url = "https://api.themoviedb.org/3/search/" + category;
        return ws.url(url)
                .addQueryParameter("api_key", apiKey)
                .addQueryParameter("query", query)
                .addQueryParameter("page", String.valueOf(page))
                .get()
                .thenApply(WSResponse::asJson);
    }

    /**
     * Fetches detailed information for a specific item by its TMDb ID.
     * For persons, appends combined_credits to retrieve known_for data
     * in a single API call, reducing the total number of requests made.
     * This method is fully asynchronous and non-blocking.
     * 
     * @param category the item category: "movie", "tv", or "person"
     * @param id       the TMDb unique identifier of the item
     * @return CompletionStage of JsonNode containing the item's full details
     * @author Harshavardhini Eluri
     */
    public CompletionStage<JsonNode> getDetails(String category, String id) {
        String url = "https://api.themoviedb.org/3/" + category + "/" + id;

        WSRequest request = ws.url(url)
                .addQueryParameter("api_key", apiKey);

        if (category.equals("person")) {
            request = request.addQueryParameter("append_to_response", "combined_credits");
        }

        return request.get().thenApply(WSResponse::asJson);
    }

    /**
     * Asynchronously loads the full list of movie genre ID-to-name mappings from
     * TMDb.
     * Results are stored in {@link #movieGenresCache} for use during search result
     * processing.
     * Called once during application startup to avoid repeated API calls.
     * 
     * @return CompletionStage of Void that completes when the cache is populated
     * @author Varshain Gopichandar Sreedevi
     */
    public CompletionStage<Void> loadMovieGenres() {
        String url = "https://api.themoviedb.org/3/genre/movie/list";
        return ws.url(url)
                .addQueryParameter("api_key", apiKey)
                .get()
                .thenAccept(response -> {
                    JsonNode genres = response.asJson().get("genres");
                    for (JsonNode g : genres) {
                        movieGenresCache.put(
                                g.get("id").asInt(),
                                g.get("name").asText());
                    }
                });
    }

    /**
     * Asynchronously loads the full list of TV show genre ID-to-name mappings from
     * TMDb.
     * Results are stored in {@link #tvGenresCache} for use during search result
     * processing.
     * Called once during application launch to prevent making repetitive API calls.
     * 
     * @return CompletionStage of Void that completes when the cache is populated
     * @author Honey Sharma
     */
    public CompletionStage<Void> loadTvGenres() {
        String url = "https://api.themoviedb.org/3/genre/tv/list";
        return ws.url(url)
                .addQueryParameter("api_key", apiKey)
                .get()
                .thenAccept(response -> {
                    JsonNode genres = response.asJson().get("genres");
                    for (JsonNode g : genres) {
                        tvGenresCache.put(
                                g.get("id").asInt(),
                                g.get("name").asText());
                    }
                });
    }

    /**
     * Converts a JSON array of genre IDs into a string of genre names separated by commas.
     * Uses the appropriate genre cache (movie or TV) based on the given category.
     * Genre IDs not found in the cache are silently skipped.
     * 
     * @param genreIds a JsonNode array of integer genre IDs from a TMDb search
     *                 result
     * @param category the category to choose from the "movie" and "tv" caches
     * @return a comma-separated string of genre names or an empty string if none
     *         found
     * @author Honey Sharma
     */
    public String getGenres(JsonNode genreIds, String category) {
        Map<Integer, String> genreMap = category.equals("movie") ? movieGenresCache : tvGenresCache;
        List<String> genres = new ArrayList<>();

        for (JsonNode idNode : genreIds) {
            int id = idNode.asInt();
            String genre = genreMap.get(id);
            if (genre != null) {
                genres.add(genre);
            }
        }
        return String.join(", ", genres);
    }

    /**
     * Retrieves the list of user reviews from TMDb for a certain movie or TV show.
     * Returns a CompletionStage of JSON containing a results array of review objects,
     * each with an author and content field. This method is fully asynchronous.
     * 
     * @param id       the TMDb unique identifier of the movie or TV show
     * @param category the item category: "movie" or "tv"
     * @return CompletionStage of JsonNode containing the reviews response
     * @author Honey Sharma
     */
    public CompletionStage<JsonNode> getReviews(String id, String category) {
        String url = "https://api.themoviedb.org/3/" + category + "/" + id + "/reviews";
        return ws.url(url)
                .addQueryParameter("api_key", apiKey)
                .get()
                .thenApply(WSResponse::asJson);
    }

    /**
     * Set of words associated with positive/happy sentiment used in review
     * analysis.
     * A review is classified as happy if happy words exceed 70% of total sentiment
     * words.
     */
    private static final Set<String> happyWords = Set.of(
            "good", "great", "amazing", "awesome", "love", "excellent", "fun", "fantastic",
            "nice", "happy", "best", "wonderful", "perfect", "enjoyed");

    /**
     * Set of words associated with negative/sad sentiment used in review analysis.
     * A review is classified as sad if sad words exceed 70% of total sentiment
     * words.
     */
    private static final Set<String> sadWords = Set.of(
            "bad", "terrible", "awful", "worst", "boring", "hate", "poor",
            "sad", "disappointing", "waste", "annoying");

    /**
     * Analyzes the sentiment of the text in a review.
     * Counts occurrences of happy and sad words from predefined word lists.
     * Returns a happy emoticon if happy words exceed 70% of total sentiment words,
     * a sad emoticon if sad words exceed 70%, and a neutral emoticon otherwise.
     * If no sentiment words are found, returns neutral.
     * 
     * @param review the full text content of a user review
     * @return ":-)" for happy sentiment, ":-(" for sad, or ":-|" for neutral
     * @author Honey Sharma
     */
    public static String analyzeSentiment(String review) {
        String[] words = review.toLowerCase().split("\\W+");

        int happy = 0;
        int sad = 0;
        for (String w : words) {
            if (happyWords.contains(w))
                happy++;
            if (sadWords.contains(w))
                sad++;
        }

        int total = happy + sad;
        if (total == 0)
            return ":-|";

        double happyRatio = (double) happy / total;
        double sadRatio = (double) sad / total;

        if (happyRatio > 0.7)
            return ":-)";
        if (sadRatio > 0.7)
            return ":-(";

        return ":-|";
    }

    // PART B : GLOBAL DIVERSITY

    /**
     * Computes global diversity metrics for a movie using its translated overviews.
     * 
     * Fetches the movie's original overview, its translations, and the list of
     * supported languages from TMDb. Calculates translation density (based on
     * available translations) and localization index (based on relative overview
     * lengths). Language count is determined dynamically. This method is fully
     * asynchronous and non-blocking.
     * 
     * @param movieId the TMDb unique identifier of the movie
     * @return CompletionStage resolving to a GlobalDiversity object containing
     *         translation density, localization index, and translation count
     * @author Aman Agnihotri
     */
    public CompletionStage<GlobalDiversity> getGlobalDiversity(int movieId) {

        String movieUrl = "https://api.themoviedb.org/3/movie/"
                + movieId
                + "?api_key="
                + apiKey
                + "&append_to_response=translations";

        String languagesUrl = "https://api.themoviedb.org/3/configuration/languages?api_key=" + apiKey;

        CompletionStage<JsonNode> movieFuture = ws.url(movieUrl).get().thenApply(WSResponse::asJson);

        CompletionStage<JsonNode> languagesFuture = ws.url(languagesUrl).get().thenApply(WSResponse::asJson);

        return movieFuture.thenCombine(languagesFuture, (movieJson, languagesJson) -> {

            String originalOverview = movieJson.get("overview").asText();

            JsonNode translations = movieJson
                    .get("translations")
                    .get("translations");

            List<String> translatedOverviews = new ArrayList<>();

            for (JsonNode t : translations) {

                JsonNode data = t.get("data");

                if (data.has("overview")) {
                    String overview = data.get("overview").asText();

                    translatedOverviews.add(overview);
                }
            }

            int targetLanguages = languagesJson.size(); // fetched dynamically & not hardcoded

            return GlobalDiversity.calculate(
                    translatedOverviews,
                    originalOverview,
                    targetLanguages);
        });
    }

}
