package models;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JUnit 4 Test for ReadabilityResult.
 * Engineered to achieve 100% Line and Branch coverage by targeting all edge cases,
 * including complex syllable rules and malformed JSON payloads.
 *
 * @author Harshavardhini Eluri
 */
public class ReadabilityResultTest {

    @Test
    public void testTextEdgeCases() {
        // Fixes the yellow line: if (text == null || text.isEmpty())
        // Hits the left side of the OR operator (null)
        assertEquals(0.0, ReadabilityResult.calculateEase(null), 0.001);
        assertEquals(0.0, ReadabilityResult.calculateGrade(null), 0.001);
        ReadabilityResult.calculateEase("!!! ???");

        // Hits the right side of the OR operator (empty)
        assertEquals(0.0, ReadabilityResult.calculateEase(""), 0.001);
        assertEquals(0.0, ReadabilityResult.calculateGrade(""), 0.001);

        // Text with irregular spacing, double hyphens, and isolated punctuation
        // Hits empty split tokens: s.trim().isEmpty() and word.isEmpty()
        String weirdText = "  Hello . ! World well--known sh  ";
        assertTrue(ReadabilityResult.calculateEase(weirdText) > 0);
        assertTrue(ReadabilityResult.calculateGrade(weirdText) > 0);

    }

    @Test
    public void testSyllableCountingBranches() {
        // This string is mathematically crafted to hit EVERY true/false branch in countSyllables:
        // "Action" -> ends with "ion"
        // "media" -> ends with "ia"
        // "audio" -> ends with "io"
        // "outside" -> ends with 'e' AND syllables > 1 (silent e removed)
        // "the" -> ends with 'e' AND syllables == 1 (silent e kept)
        // "good" -> consecutive vowels (isVowel && prevVowel)
        // "rhythm" -> no vowels at all (hits Math.max(1, syllables))
        String text = "Action media audio outside the good rhythm.";

        // Execute both ease and grade to ensure full path coverage
        assertTrue(ReadabilityResult.calculateEase(text) > 0);
        assertTrue(ReadabilityResult.calculateGrade(text) > 0);
    }

    @Test
    public void testFromJson_Movie_ValidAndEdgeCases() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode json = mapper.createObjectNode();

        // Missing 'title', forces fallback to 'name'
        json.put("name", "Batman Movie");
        json.put("overview", "A great movie.");

        // poster_path is explicitly a JSON null (Hits isNull() == true branch)
        json.putNull("poster_path");

        // ExtractList branch: node is NOT an array (Hits isArray() == false)
        // Passes an ObjectNode instead of an ArrayNode to test safe skipping
        ObjectNode prodComp = mapper.createObjectNode();
        prodComp.put("name", "DC");
        json.set("production_companies", prodComp);

        // ExtractList branch: node contains empty values (Hits !value.isEmpty() == false)
        ArrayNode langs = mapper.createArrayNode();

        ObjectNode validLang = mapper.createObjectNode();
        validLang.put("english_name", "English");

        ObjectNode emptyLang = mapper.createObjectNode();
        emptyLang.put("english_name", ""); // Forces value.isEmpty() to be true

        langs.add(validLang);
        langs.add(emptyLang);
        json.set("spoken_languages", langs);

        // NOTE: "genres" is missing entirely (Hits node != null == false)

        // Execute the parser
        ReadabilityResult result = ReadabilityResult.fromJson(json, "movie");

        assertNotNull(result);
        assertEquals("Batman Movie", result.title);
        assertEquals("", result.posterPath); // Safely handled the null poster
        assertTrue(result.productionCompanies.isEmpty()); // Safely ignored the non-array
        assertEquals(1, result.spokenlanguages.size()); // Safely ignored the empty string
        assertTrue(result.genres.isEmpty()); // Safely handled the missing node
    }

    @Test
    public void testFromJson_TV_ValidPath() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode json = mapper.createObjectNode();

        // Has 'title' natively
        json.put("title", "Batman TV");
        json.put("overview", "TV show.");

        // Valid poster path (Hits FALSE for isMissingNode() || isNull())
        json.put("poster_path", "/batman.jpg");

        // Valid Networks array
        ArrayNode networks = mapper.createArrayNode();
        ObjectNode net = mapper.createObjectNode();
        net.put("name", "HBO");
        networks.add(net);
        json.set("networks", networks);

        ReadabilityResult result = ReadabilityResult.fromJson(json, "tv");

        assertNotNull(result);
        assertEquals("Batman TV", result.title);
        assertEquals("https://image.tmdb.org/t/p/w500/batman.jpg", result.posterPath);
        assertEquals(1, result.networks.size());
        assertEquals("HBO", result.networks.get(0));
    }

    @Test
    public void testFromJson_MissingPosterPathEntirely() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode json = mapper.createObjectNode();

        // Completely missing poster_path (Hits isMissingNode() == true)
        json.put("title", "Missing Poster");

        ReadabilityResult result = ReadabilityResult.fromJson(json, "movie");
        assertEquals("", result.posterPath);
    }
}