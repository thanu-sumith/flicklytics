package models;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the PersonStat model class.
 * Tests all fields and boundary conditions using partition testing.
 * @author Varshain Gopichandar Sreedevi
 */
public class PersonStatTest {

    /**
     * Tests all fields of PersonStat are stored correctly.
     * Partition: normal movie item with all fields present and non-zero.
     * @author Varshain Gopichandar Sreedevi
     */
    @Test
    public void testPersonStatMovieItem() {
        PersonStat stat = new PersonStat("Inception", "movie", 85.5, 8.8, 2000000L);
        assertEquals("Inception", stat.getTitle());
        assertEquals("movie", stat.getMediaType());
        assertEquals(85.5, stat.getPopularity(), 0.001);
        assertEquals(8.8, stat.getVoteAverage(), 0.001);
        assertEquals(2000000L, stat.getVoteCount());
    }

    /**
     * Tests PersonStat correctly stores TV show media type and fields.
     * Partition: TV show media type instead of movie.
     * @author Varshain Gopichandar Sreedevi
     */
    @Test
    public void testPersonStatTVItem() {
        PersonStat stat = new PersonStat("Breaking Bad", "tv", 120.0, 9.5, 500000L);
        assertEquals("Breaking Bad", stat.getTitle());
        assertEquals("tv", stat.getMediaType());
        assertEquals(9.5, stat.getVoteAverage(), 0.001);
    }

    /**
     * Tests PersonStat correctly handles zero values for all numeric fields.
     * Partition: boundary — all numeric fields are zero.
     * @author Varshain Gopichandar Sreedevi
     */
    @Test
    public void testPersonStatZeroValues() {
        PersonStat stat = new PersonStat("Unknown", "N/A", 0.0, 0.0, 0L);
        assertEquals(0.0, stat.getPopularity(), 0.001);
        assertEquals(0.0, stat.getVoteAverage(), 0.001);
        assertEquals(0L, stat.getVoteCount());
    }
}