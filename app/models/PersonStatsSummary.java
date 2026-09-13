package models;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import com.fasterxml.jackson.databind.JsonNode;
import Services.TmdbServices;
import com.fasterxml.jackson.annotation.JsonProperty;
/**
 * Holds the full result for the Person Stats page:
 * the list of known_for items plus computed summary statistics.
 * @author Varshain Gopichandar Sreedevi
 */
public class PersonStatsSummary {

    private final String personName;
    private final List<PersonStat> items;

    private final double popularityMin;
    private final double popularityMax;
    private final double popularityAvg;
    private final long popularityCount;

    private final double voteAvgMin;
    private final double voteAvgMax;
    private final double voteAvgAvg;

    private final long voteCountMin;
    private final long voteCountMax;
    private final double voteCountAvg;

    /**
     * Full constructor for PersonStatsSummary.
     * @param personName name of the person
     * @param items list of known_for PersonStat items
     * @param popularityMin minimum popularity
     * @param popularityMax maximum popularity
     * @param popularityAvg average popularity
     * @param popularityCount total number of items
     * @param voteAvgMin minimum vote average
     * @param voteAvgMax maximum vote average
     * @param voteAvgAvg average of vote averages
     * @param voteCountMin minimum vote count
     * @param voteCountMax maximum vote count
     * @param voteCountAvg average vote count
     * @author Varshain Gopichandar Sreedevi
     */
    public PersonStatsSummary(String personName, List<PersonStat> items,
                               double popularityMin, double popularityMax, double popularityAvg, long popularityCount,
                               double voteAvgMin, double voteAvgMax, double voteAvgAvg,
                               long voteCountMin, long voteCountMax, double voteCountAvg) {
        this.personName = personName;
        this.items = items;
        this.popularityMin = popularityMin;
        this.popularityMax = popularityMax;
        this.popularityAvg = popularityAvg;
        this.popularityCount = popularityCount;
        this.voteAvgMin = voteAvgMin;
        this.voteAvgMax = voteAvgMax;
        this.voteAvgAvg = voteAvgAvg;
        this.voteCountMin = voteCountMin;
        this.voteCountMax = voteCountMax;
        this.voteCountAvg = voteCountAvg;
    }

    /**
     * Factory method: fetches person details from TMDb and computes all statistics.
     * All business logic is here in the model, keeping the controller clean.
     * @param id the TMDb person ID
     * @param tmdbService the TMDb service instance
     * @return CompletionStage of PersonStatsSummary with all stats computed
     * @author Varshain Gopichandar Sreedevi
     */
    public static CompletionStage<PersonStatsSummary> fetch(String id, TmdbServices tmdbService) {
        return tmdbService.getDetails("person", id).thenApply(personJson -> {

            // Extract person name
            String personName = personJson.has("name") ? personJson.get("name").asText() : "Unknown";

            // Extract combined_credits -> cast array (up to 50 items)
            List<PersonStat> items = new ArrayList<>();
            JsonNode castArray = null;

            if (personJson.has("combined_credits") && personJson.get("combined_credits").has("cast")) {
                castArray = personJson.get("combined_credits").get("cast");
            }

            if (castArray != null && castArray.isArray()) {
                items = StreamSupport.stream(castArray.spliterator(), false)
                        .limit(50)
                        .map(node -> {
                            String title = node.has("title") ? node.get("title").asText()
                                    : (node.has("name") ? node.get("name").asText() : "Unknown");
                            String mediaType = node.has("media_type") ? node.get("media_type").asText() : "N/A";
                            double popularity  = node.has("popularity")   ? node.get("popularity").asDouble()   : 0.0;
                            double voteAverage = node.has("vote_average") ? node.get("vote_average").asDouble() : 0.0;
                            long   voteCount   = node.has("vote_count")   ? node.get("vote_count").asLong()     : 0L;
                            return new PersonStat(title, mediaType, popularity, voteAverage, voteCount);
                        })
                        .collect(Collectors.toList());
            }

            // Compute all statistics using Streams
            long count     = items.size();
            double popMin  = items.stream().mapToDouble(PersonStat::getPopularity).min().orElse(0.0);
            double popMax  = items.stream().mapToDouble(PersonStat::getPopularity).max().orElse(0.0);
            double popAvg  = items.stream().mapToDouble(PersonStat::getPopularity).average().orElse(0.0);
            double vAvgMin = items.stream().mapToDouble(PersonStat::getVoteAverage).min().orElse(0.0);
            double vAvgMax = items.stream().mapToDouble(PersonStat::getVoteAverage).max().orElse(0.0);
            double vAvgAvg = items.stream().mapToDouble(PersonStat::getVoteAverage).average().orElse(0.0);
            long   vCntMin = items.stream().mapToLong(PersonStat::getVoteCount).min().orElse(0L);
            long   vCntMax = items.stream().mapToLong(PersonStat::getVoteCount).max().orElse(0L);
            double vCntAvg = items.stream().mapToLong(PersonStat::getVoteCount).average().orElse(0.0);

            return new PersonStatsSummary(personName, items,
                    popMin, popMax, popAvg, count,
                    vAvgMin, vAvgMax, vAvgAvg,
                    vCntMin, vCntMax, vCntAvg);
        });
    }

    /**
     * @return the person's name
     * @author Varshain Gopichandar Sreedevi 
     */
    @JsonProperty("personName")
    public String getPersonName() { return personName; }

    /**
     * @return list of known_for items
     * @author Varshain Gopichandar Sreedevi
     */
    @JsonProperty("items")
    public List<PersonStat> getItems() { return items; }

    /**
     * @return minimum popularity value
     * @author Varshain Gopichandar Sreedevi
     */
    @JsonProperty("popularityMin")
    public double getPopularityMin() { return popularityMin; }

    /**
     * @return maximum popularity value
     * @author Varshain Gopichandar Sreedevi
     */
    @JsonProperty("popularityMax")
    public double getPopularityMax() { return popularityMax; }

    /**
     * @return average popularity value
     * @author Varshain Gopichandar Sreedevi
     */
    @JsonProperty("popularityAvg")
    public double getPopularityAvg() { return popularityAvg; }

    /**
     * @return count of items
     * @author Varshain Gopichandar Sreedevi
     */
    @JsonProperty("popularityCount")
    public long getPopularityCount() { return popularityCount; }

    /**
     * @return minimum vote average
     * @author Varshain Gopichandar Sreedevi
     */
    @JsonProperty("voteAvgMin")
    public double getVoteAvgMin() { return voteAvgMin; }

    /**
     * @return maximum vote average
     * @author Varshain Gopichandar Sreedevi
     */
    @JsonProperty("voteAvgMax")
    public double getVoteAvgMax() { return voteAvgMax; }

    /**
     * @author Varshain Gopichandar Sreedevi
     * @return average of vote averages
     */
    @JsonProperty("voteAvgAvg")
    public double getVoteAvgAvg() { return voteAvgAvg; }

    /**
     * @author Varshain Gopichandar Sreedevi
     * @return minimum vote count
     */
    @JsonProperty("voteCountMin")
    public long getVoteCountMin() { return voteCountMin; }

    /**
     * @author Varshain Gopichandar Sreedevi
     * @return maximum vote count
     */
    @JsonProperty("voteCountMax")
    public long getVoteCountMax() { return voteCountMax; }

    /**
     * @author Varshain Gopichandar Sreedevi
     * @return average vote count
     */
    @JsonProperty("voteCountAvg")
    public double getVoteCountAvg() { return voteCountAvg; }

}