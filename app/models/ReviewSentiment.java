package models;

import com.fasterxml.jackson.databind.JsonNode;
import Services.TmdbServices;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
/**
 * Represents a single user review with its sentiment analysis result.
 * This package includes static utility methods for generating a list of reviews
 * from TMDb JSON and calculating overall sentiment across all reviews.
 * @author Honey Sharma
 */
public class ReviewSentiment {

    /** The username or display name of the review author. */
    public String author;

    /** The full text content of the review. */
    public String content;

    /** The sentiment emoticon: ":-)" for happy, ":-(" for sad or ":-|" for neutral. */
    public String sentiment;

    /**
     * Constructs a ReviewSentiment object with the given author, content, and sentiment.
     *
     * @param author    the username of the review author
     * @param content   the full text content of the review
     * @param sentiment the sentiment emoticon result: ":-)", ":-(" or ":-|"
     * @author Honey Sharma
     */
    public ReviewSentiment(String author, String content, String sentiment) {
        this.author = author;
        this.content = content;
        this.sentiment = sentiment;
    }

    /**
     * Builds a list of ReviewSentiment objects from a TMDb reviews JSON response.
     * Uses the TmdbServices sentiment analyzer to process up to 50 reviews,
     * extracting the author and text, and analyzing each review's sentiment.
     *
     * @param json        the TMDb JSON response containing a "results" array of reviews
     * @param tmdbService the TMDb service used to perform sentiment analysis on each review
     * @return a List of up to 50 ReviewSentiment objects with computed sentiment values
     * @author Honey Sharma
     */
    public static List<ReviewSentiment> buildReviews(JsonNode json, TmdbServices tmdbService) {
        JsonNode results = json.get("results");
        return StreamSupport.stream(results.spliterator(), false)
                .limit(50)
                .map(node -> {
                    String author = node.get("author").asText();
                    String content = node.get("content").asText();
                    String sentiment = tmdbService.analyzeSentiment(content);
                    return new ReviewSentiment(author, content, sentiment);
                })
                .collect(Collectors.toList());
    }

    /**
     * Determines the global sentiment across the list of reviews.
     * Returns the sentiment emoticon that occurs most frequently after grouping
     * reviews according to their sentiment value and counting each group.
     * Returns "N/A" if the reviews list is empty.
     *
     * @param reviews the list of ReviewSentiment objects to analyze
     * @return the most frequently occurring sentiment: ":-)", ":-(", ":-|" or "N/A"
     * @author Honey Sharma
     */
    public static String getGlobalSentiment(List<ReviewSentiment> reviews) {
        return reviews.stream()
                .collect(Collectors.groupingBy(r -> r.sentiment, Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
    }
}