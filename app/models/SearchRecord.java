package models;

import com.fasterxml.jackson.databind.JsonNode;
import Services.TmdbServices;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
/**
 * Represents a single search query and its results.
 * @author Thanugundla Sumith Reddy
 */
public class SearchRecord {
    public final String query;
    public final String category;
    public final List<MovieResults> results;
    public final int totalResults;

    

 /**
 * Constructs a SearchRecord with the given query, category, results and total count.
 *
 * @param query        the search keyword entered by the user
 * @param category     the search category: "movie", "tv", or "person"
 * @param results      the list of up to 10 matching MovieResults items
 * @param totalResults the total number of results returned by the TMDb API
 * @author Thanugundla Sumith Reddy
 */
    public SearchRecord(String query, String category, List<MovieResults> results, int totalResults) {
        this.query = query;
        this.category = category;
        this.results = results;
        this.totalResults = totalResults;
    }

    public List<MovieResults> getResults() {
        return this.results;
    }
    /**
     * Factory method to process raw TMDb JSON into a clean SearchRecord object.
     * Handles extracting the total count, sorting the results array by popularity,
     * limiting to top 10, and mapping them to MovieResults objects.
     * @author Thanugundla Sumith Reddy
     */
    public static SearchRecord fromJson(String query, String category, JsonNode jsonResponse, TmdbServices tmdbService) {
        return fromJson(query, category, jsonResponse, tmdbService, 10);
    }

    public static SearchRecord fromJson(String query, String category, JsonNode jsonResponse, TmdbServices tmdbService, int maxItems) {

        JsonNode resultsArray = jsonResponse.get("results");
        int totalResults = jsonResponse.has("total_results") ? jsonResponse.get("total_results").asInt() : 0;

        List<MovieResults> movies = StreamSupport.stream(resultsArray.spliterator(), false)
                // Added a safety check for popularity just in case the API omits it!
                .sorted((a, b) -> Double.compare(
                        b.has("popularity") ? b.get("popularity").asDouble() : 0.0,
                        a.has("popularity") ? a.get("popularity").asDouble() : 0.0))
                .limit(Math.max(1, maxItems))
                .map(node -> MovieResults.fromJson(node, category, tmdbService))
                .collect(Collectors.toList());

        return new SearchRecord(query, category, movies, totalResults);
    }
}
