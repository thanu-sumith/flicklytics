package models;

import com.fasterxml.jackson.databind.JsonNode;
import Services.TmdbServices;

/**
 * Represents a single search result item (Movie, TV Show, or Person).
 * Contains a static factory method to parse TMDb JSON directly.
 * @author Thanugundla Sumith Reddy, Varshain Gopichandar Sreedevi
 */
public class MovieResults {

    public final String id;
    public final String title;
    public final String releaseDate;
    public final String language;
    public final double popularity;
    public final double voteAverage;
    public final String genres;
    public final String gender;
    public final String department;
    public final String profilePath;
    public String posterPath;
    public final String overview;

    /**
     * Constructor for MovieResults.
     *
     * @param id          the TMDb item ID
     * @param title       the title or name of the item
     * @param releaseDate the release or air date
     * @param language    the original language
     * @param popularity  the popularity score
     * @param voteAverage the vote average score
     * @param genres      comma-separated genre names
     * @param gender      gender string for persons
     * @param department  known_for department for persons
     * @param profilePath profile photo path for persons
     * @param posterPath  poster image path for movies/TV
     * @param overview    description/overview text
     * @author Thanugundla Sumith Reddy, Varshain Gopichandar Sreedevi
     */
    public MovieResults(String id, String title, String releaseDate, String language,
                        double popularity, double voteAverage, String genres,
                        String gender, String department, String profilePath,
                        String posterPath, String overview) {
        this.id = id;
        this.title = title;
        this.releaseDate = releaseDate;
        this.language = language;
        this.popularity = popularity;
        this.voteAverage = voteAverage;
        this.genres = genres;
        this.gender = gender;
        this.department = department;
        this.profilePath = profilePath;
        this.posterPath = posterPath;
        this.overview = overview;
    }

    /**
     * Retrieves the overview/description of the item.
     *
     * @return the overview text
     * @author Thanugundla Sumith Reddy, Varshain Gopichandar Sreedevi
     */
    public String getOverview() {
        return this.overview;
    }

    /**
     * Factory method: parses a single TMDb JSON result node into a MovieResults object.
     * <p>
     * Handles all field extraction, null checks, date resolution logic,
     * and gender mapping. Keeps parsing/business logic inside the model.
     * </p>
     *
     * @param node        the JSON node representing one search result
     * @param category    the search category: "movie", "tv", or "person"
     * @param tmdbService the TMDb service used for genre name lookup
     * @return a fully populated MovieResults instance
     * @author Thanugundla Sumith Reddy, Varshain Gopichandar Sreedevi
     */
    public static MovieResults fromJson(JsonNode node, String category, TmdbServices tmdbService) {

        String id = node.has("id") ? node.get("id").asText() : "N/A";

        String title = node.has("title") ? node.get("title").asText()
                : (node.has("name") ? node.get("name").asText() : "Unknown");

        String date = "unknown";
        if (node.has("release_date") && !node.get("release_date").asText().isEmpty()) {
            date = node.get("release_date").asText();
        } else if (node.has("first_air_date") && !node.get("first_air_date").asText().isEmpty()) {
            date = node.get("first_air_date").asText();
        }

        String language = node.has("original_language") ? node.get("original_language").asText() : "N/A";

        double popularity = node.has("popularity") ? node.get("popularity").asDouble() : 0.0;

        double voteAverage = node.has("vote_average") ? node.get("vote_average").asDouble() : 0.0;

        String genres = "";
        if (node.has("genre_ids") && node.get("genre_ids").isArray()) {
            genres = tmdbService.getGenres(node.get("genre_ids"), category);
        }

        String department = node.has("known_for_department")
                ? node.get("known_for_department").asText()
                : "N/A";

        String posterPath = node.has("poster_path") && !node.get("poster_path").isNull()
                ? node.get("poster_path").asText()
                : null;

        String profilePath = node.has("profile_path") && !node.get("profile_path").isNull()
                ? node.get("profile_path").asText()
                : null;

        String gender = "Unknown";
        if (node.has("gender")) {
            int g = node.get("gender").asInt();
            if (g == 1)
                gender = "Female";
            else if (g == 2)
                gender = "Male";
            else if (g == 3)
                gender = "Non-Binary";
        }

        String overview = node.has("overview") ? node.get("overview").asText() : "No description available";

        return new MovieResults(id, title, date, language, popularity,
                voteAverage, genres, gender, department, profilePath, posterPath, overview);
    }
}