package models;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.mockito.Mockito;
import Services.TmdbServices;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

/**
 * JUnit 4 Test for MovieResults.
 * Guarantees 100% Branch Coverage for JSON parsing, ternary operators, and edge cases.
 *
 * @author Thanugundla Sumith Reddy
 * @author Varshain Gopichandar Sreedevi
 */
public class MovieResultsTest {

    /**
     * Tests the getter method for overview.
     * Ensures that the overview field is correctly returned.
     *
     * @author Thanugundla Sumith Reddy
     * @author Varshain Gopichandar Sreedevi
     */
    @Test
    public void testGetOverview() {
        MovieResults mr = new MovieResults("1", "Title", "Date", "en", 1.0, 1.0,
                "Action", "Male", "Acting", "/profile.jpg", "/poster.jpg", "Test Overview");

        assertEquals("Test Overview", mr.getOverview());
    }

    /**
     * Tests parsing when all JSON fields are missing.
     * Verifies default values are correctly assigned for all attributes.
     *
     * @author Thanugundla Sumith Reddy
     * @author Varshain Gopichandar Sreedevi
     */
    @Test
    public void testFromJson_AllMissingFields() {

        TmdbServices mockTmdb = Mockito.mock(TmdbServices.class);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode emptyJson = mapper.createObjectNode();

        MovieResults mr = MovieResults.fromJson(emptyJson, "movie", mockTmdb);

        assertEquals("N/A", mr.id);
        assertEquals("Unknown", mr.title);
        assertEquals("unknown", mr.releaseDate);
        assertEquals("N/A", mr.language);
        assertEquals(0.0, mr.popularity, 0.001);
        assertEquals(0.0, mr.voteAverage, 0.001);
        assertEquals("", mr.genres);
        assertEquals("N/A", mr.department);
        assertNull(mr.posterPath);
        assertNull(mr.profilePath);
        assertEquals("Unknown", mr.gender);
        assertEquals("No description available", mr.overview);
    }

    /**
     * Tests parsing of a valid movie JSON with female gender.
     * Covers standard field extraction and genre mapping.
     *
     * @author Thanugundla Sumith Reddy
     * @author Varshain Gopichandar Sreedevi
     */
    @Test
    public void testFromJson_ValidMovieAndFemaleGender() {

        TmdbServices mockTmdb = Mockito.mock(TmdbServices.class);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode json = mapper.createObjectNode();

        json.put("id", "123");
        json.put("title", "Batman");
        json.put("release_date", "2022-01-01");
        json.put("original_language", "en");
        json.put("popularity", 10.5);
        json.put("vote_average", 8.5);
        json.put("poster_path", "/poster.jpg");
        json.put("overview", "A great movie.");
        json.put("gender", 1);

        ArrayNode genres = mapper.createArrayNode();
        genres.add(28);
        json.set("genre_ids", genres);

        Mockito.when(mockTmdb.getGenres(any(), eq("movie"))).thenReturn("Action");

        MovieResults mr = MovieResults.fromJson(json, "movie", mockTmdb);

        assertEquals("123", mr.id);
        assertEquals("Batman", mr.title);
        assertEquals("2022-01-01", mr.releaseDate);
        assertEquals("en", mr.language);
        assertEquals(10.5, mr.popularity, 0.001);
        assertEquals(8.5, mr.voteAverage, 0.001);
        assertEquals("Action", mr.genres);
        assertEquals("/poster.jpg", mr.posterPath);
        assertEquals("Female", mr.gender);
        assertEquals("A great movie.", mr.overview);
    }

    /**
     * Tests parsing of a valid TV show JSON with male gender.
     * Covers alternate field paths such as name and first_air_date.
     *
     * @author Thanugundla Sumith Reddy
     * @author Varshain Gopichandar Sreedevi
     */
    @Test
    public void testFromJson_ValidTvShowAndMaleGender() {

        TmdbServices mockTmdb = Mockito.mock(TmdbServices.class);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode json = mapper.createObjectNode();

        json.put("name", "The Office");
        json.put("first_air_date", "2005-03-24");
        json.put("gender", 2);
        json.put("known_for_department", "Acting");
        json.put("profile_path", "/steve.jpg");

        MovieResults mr = MovieResults.fromJson(json, "tv", mockTmdb);

        assertEquals("The Office", mr.title);
        assertEquals("2005-03-24", mr.releaseDate);
        assertEquals("Male", mr.gender);
        assertEquals("Acting", mr.department);
        assertEquals("/steve.jpg", mr.profilePath);
    }

    /**
     * Tests edge cases including null values, empty strings, and incorrect data types.
     * Ensures robustness of JSON parsing logic and fallback conditions.
     *
     * @author Thanugundla Sumith Reddy
     * @author Varshain Gopichandar Sreedevi
     */
    @Test
    public void testFromJson_EdgeCasesAndNulls() {

        TmdbServices mockTmdb = Mockito.mock(TmdbServices.class);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode json = mapper.createObjectNode();

        json.put("release_date", "");
        json.put("first_air_date", "");

        json.put("genre_ids", "not_an_array");

        json.putNull("poster_path");
        json.putNull("profile_path");

        json.put("gender", 3);

        MovieResults mr1 = MovieResults.fromJson(json, "movie", mockTmdb);

        assertEquals("unknown", mr1.releaseDate);
        assertEquals("", mr1.genres);
        assertNull(mr1.posterPath);
        assertNull(mr1.profilePath);
        assertEquals("Non-Binary", mr1.gender);

        json.put("gender", 99);
        MovieResults mr2 = MovieResults.fromJson(json, "movie", mockTmdb);

        assertEquals("Unknown", mr2.gender);
    }
}