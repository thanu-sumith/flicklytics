package models;
import com.fasterxml.jackson.annotation.JsonProperty;
/**
 * Represents a single "known_for" item for a person, including its stats.
 * 
 * @author Varshain Gopichandar Sreedevi
 */
public class PersonStat {

    private final String title;
    private final String mediaType;
    private final double popularity;
    private final double voteAverage;
    private final long voteCount;

    /**
     * Constructor for PersonStat.
     * @param title the title or name of the known_for item
     * @param mediaType "movie" or "tv"
     * @param popularity popularity score
     * @param voteAverage average vote score
     * @param voteCount total number of votes
     * @author Varshain Gopichandar Sreedevi
     */
    public PersonStat(String title, String mediaType, double popularity, double voteAverage, long voteCount) {
        this.title = title;
        this.mediaType = mediaType;
        this.popularity = popularity;
        this.voteAverage = voteAverage;
        this.voteCount = voteCount;
    }

    /** @return the title of the item */
    /**@author Varshain Gopichandar Sreedevi */
    @JsonProperty("title")
    public String getTitle() { return title; }

    /** @return the media type (movie or tv) */
    /**@author Varshain Gopichandar Sreedevi */
    @JsonProperty("mediaType")
    public String getMediaType() { return mediaType; }

    /** @return the popularity score */
    /**@author Varshain Gopichandar Sreedevi */
    @JsonProperty("popularity")
    public double getPopularity() { return popularity; }

    /** @return the vote average */
    /**@author Varshain Gopichandar Sreedevi */
    @JsonProperty("voteAverage")
    public double getVoteAverage() { return voteAverage; }

    /** @return the vote count */
    /**@author Varshain Gopichandar Sreedevi */
    @JsonProperty("voteCount")
    public long getVoteCount() { return voteCount; }
}