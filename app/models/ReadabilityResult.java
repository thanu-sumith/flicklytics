package models;

import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
/**
 * Model to hold full item details and readability scores.
 * @author Harshavardhini Eluri
 */
public class ReadabilityResult {
    public String title;
    public String overview;
    public double ease;
    public double grade;

    // Appendix A movie Fields
    public String releaseDate;
    public List<String> genres;
    public String homepage;
    public double popularity;
    public String posterPath;
    public List<String> productionCompanies;
    public long revenue;
    public int runtime;
    public List<String> spokenlanguages;
    public String status;
    public String tagline;
    public double voteAverage;
    public int voteCount;

    // tv fields
    public String firstAirDate;
    public String lastAirDate;
    public int numberOfEpisodes;
    public int numberOfSeasons;
    public List<String> networks;
    public String itemType;

    /**
     * Constructs a new ReadabilityResult with core identification and score fields.
     *
     * @param title The title or name of the media.
     * @param overview The description text used for readability analysis.
     * @param ease The initial Flesch Reading Ease score.
     * @param grade The initial Flesch-Kincaid Grade Level.
     * @author Harshavardhini Eluri
     */
    public ReadabilityResult(String title, String overview) {
        this.title = title;
        this.overview = overview;
    
    }

     /**
     * Factory method to create and populate a ReadabilityResult object from a JSON response.
     * It handles the logic for both Movie and TV categories.
     * @author Harshavardhini Eluri
     * @param json The JsonNode containing raw API data.
     * @param category The media category ("movie" or "tv").
     * @return A fully populated ReadabilityResult instance.
     */
    public static ReadabilityResult fromJson(com.fasterxml.jackson.databind.JsonNode json, String category) {
        //String title = json.has("title") ? json.get("title").asText() : json.get("name").asText("");
        String title = json.has("title") ? json.path("title").asText() : json.path("name").asText("N/A");
        String overview = json.path("overview").asText("");

        // Create the object
        ReadabilityResult result = new ReadabilityResult(title, overview);

         result.ease = calculateEase(overview);
             result.grade = calculateGrade(overview);

        // Map Common Fields
        result.popularity = json.path("popularity").asDouble();
        result.posterPath = json.path("poster_path").isMissingNode() || json.path("poster_path").isNull()
                ? "" : "https://image.tmdb.org/t/p/w500" + json.path("poster_path").asText();
        result.homepage = json.path("homepage").asText("");
        result.status = json.path("status").asText("");
        result.tagline = json.path("tagline").asText("");
        result.voteAverage = json.path("vote_average").asDouble();
        result.voteCount = json.path("vote_count").asInt();
        result.genres = extractList(json.get("genres"), "name");

        // Map Specific Fields
        if ("movie".equalsIgnoreCase(category)) {
            result.releaseDate = json.path("release_date").asText("");
            result.revenue = json.path("revenue").asLong();
            result.runtime = json.path("runtime").asInt();
            result.productionCompanies = extractList(json.get("production_companies"), "name");
            result.spokenlanguages = extractList(json.get("spoken_languages"), "english_name");
        } else {
            result.firstAirDate = json.path("first_air_date").asText("");
            result.lastAirDate = json.path("last_air_date").asText("");
            result.numberOfEpisodes = json.path("number_of_episodes").asInt();
            result.numberOfSeasons = json.path("number_of_seasons").asInt();
            result.networks = extractList(json.get("networks"), "name");
            result.itemType = json.path("type").asText("");
        }
        return result;
    }

    /**
     * Calculates the Flesch Reading Ease score for a given text.
     * Higher scores indicate material that is easier to read; lower scores indicate
     * material that is more difficult to read.
     * * Formula: 206.835 - 1.015 * (total words / total sentences) - 84.6 * (total syllables / total words)
     * @author Harshavardhini Eluri
     * @param text The string content to analyze.
     * @return The Flesch Reading Ease score as a double.
     */
    public static double calculateEase(String text) {
        if (text == null || text.isEmpty()) return 0;
        double w = countWords(text);
        double s = countSentences(text);
        double l = countSyllables(text);
    
        // Formula: 206.835 - 1.015 * (words/sentences) - 84.6 * (syllables/words)
        double score = 206.835 - Math.round((1.015 * (w / s))) - Math.round((84.6 * (l / w)));
        return score ;
        //return Math.round(score * 100.0) / 100.0;
    }
 /**
     * Calculates the Flesch-Kincaid Grade Level for a given text.
     * The result indicates the U.S. school grade level needed to understand the text.
     * * Formula: 0.39 * (total words / total sentences) + 11.8 * (total syllables / total words) - 15.59
     *  @author Harshavardhini Eluri
     * @param text The string content to analyze.
     * @return The estimated grade level as a double.
     */
    public static double calculateGrade(String text) {
        if (text == null || text.isEmpty()) return 0;
        double w = countWords(text);
        double s = countSentences(text);
        double l = countSyllables(text);
        // Formula: 0.39 * (words/sentences) + 11.8 * (syllables/words) - 15.59
        double score = (0.39 * (w / s)) + (11.8 * (l / w)) - 15.59;
        return score;
        //return Math.round(score * 100.0) / 100.0;
    }

 /**
     * Counts the number of sentences in the text based on punctuation marks (. ! ?).
     * Ensures that empty strings between multiple punctuations are not counted.
     *  @author Harshavardhini Eluri
     * @param text The string to process.
     * @return Total count of sentences, minimum 1 if text is present.
     */

    private static int countSentences(String text) {
        String[] sentences = text.split("[.!?]+");
        int count = 0;

        for (String s : sentences) {
            if (!s.trim().isEmpty()) {
                count++;
            }
        }

        return Math.max(1, count);
    }
   /**
     * Counts the number of words in the text.
     * It removes non-alphabetic characters but preserves hyphens to correctly
     * identify compound words.
     * @author Harshavardhini Eluri
     * @param text The string to process.
     * @return Total count of words.
     */

    private static int countWords(String text) {

        // keep hyphenated words
        text = text.replaceAll("[^a-zA-Z\\s-]", "");

        String[] words = text.trim().split("\\s+");

        int count = 0;

        for (String word : words) {
            if (!word.isEmpty()) count++;
        }

        return count;
    }

     /**
     * Estimates the total number of syllables in a block of text.
     * The logic processes words individually, handling vowel groups, silent 'e's,
     * hyphenated words, and specific suffix rules (e.g., -ion, -ia, -io).
     * @author Harshavardhini Eluri
     * @param text The string to process.
     * @return Total estimated syllable count.
     */
    private static int countSyllables(String text) {

        int total = 0;

        text = text.replaceAll("[^a-zA-Z\\s-]", "");
        String[] words = text.toLowerCase().split("\\s+");

        for (String word : words) {

            if (word.isEmpty()) continue;

            String[] parts = word.split("-");

            for (String part : parts) {

                int syllables = 0;
                boolean prevVowel = false;

                for (int i = 0; i < part.length(); i++) {

                    char c = part.charAt(i);
                     boolean isVowel = isVowel(c);
                  //  boolean isVowel = "aeiouy".indexOf(c) >= 0;
           
                    if (isVowel && !prevVowel) {
                        syllables++;
                    }

                    prevVowel = isVowel;
                }

                // silent e
                if (part.endsWith("e") && syllables > 1) {
                    syllables--;
                }

                // special endings that add syllables
                if (part.endsWith("ia") || part.endsWith("io")) {
                    syllables++;
                }

                // -ion words
                if (part.endsWith("ion")) {
                    syllables++;
                }

                total += Math.max(1, syllables);
            }
        }

        return total;
    }
    private static boolean isVowel(char c) {
        return "aeiouy".indexOf(c) != -1;
    }

    /**
     * Extracts a list of strings from a JSON array based on a specific field name.
     * @author Harshavardhini Eluri
     * @param node The JSON array node to process.
     * @param fieldName The name of the field to extract from each object in the array.
     * @return A List of strings containing the extracted values.
     */
    // // Move the helper here too
    private static java.util.List<String> extractList(com.fasterxml.jackson.databind.JsonNode node, String fieldName) {
        java.util.List<String> list = new java.util.ArrayList<>();
         if (node != null && node.isArray()) {
             for (com.fasterxml.jackson.databind.JsonNode item : node) {
                // list.add(item.get(fieldName).asText());
                String value = item.path(fieldName).asText("");
             if (!value.isEmpty()) {
                 list.add(value);
             }
             }
         }
         return list;
     }


}